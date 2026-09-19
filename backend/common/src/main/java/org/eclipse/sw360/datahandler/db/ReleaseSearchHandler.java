/*
 * Copyright Siemens AG, 2018. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db;

import com.ibm.cloud.cloudant.v1.Cloudant;
import org.eclipse.sw360.datahandler.cloudantclient.BaseNouveauSearchHandler;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseSortColumn;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.eclipse.sw360.datahandler.common.SearchUtils.INDEX_ID_FIELD;
import static org.eclipse.sw360.datahandler.common.SearchUtils.INDEX_VERSION_SEGMENTS;
import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.SCORE_SORTING_FIELD;

/**
 * Nouveau search handler for Releases with paginated access control filtering.
 *
 * @author thomas.maier@evosoft.com
 */
public class ReleaseSearchHandler extends BaseNouveauSearchHandler<Release> {

    // -------------------------------------------------------------------------
    //  Field spec declarations
    // -------------------------------------------------------------------------

    private static final List<IndexField> RELEASE_FIELDS = List.of(
            IndexField.standard("name"),
            IndexField.standard("version", 1, BaseNouveauSearchHandler.EDGE_NGRAM_MAX_LENGTH),
            IndexField.simple("componentId", "keyword"),
            IndexField.simple("clearingState", "keyword"),
            IndexField.simple("mainlineState", "keyword"),
            IndexField.simple("createdBy", "email"),
            IndexField.simple("componentType", "keyword"),
            IndexField.date("createdOn")
    );

    /**
     * Release-specific JS for array-backed fields that should support text
     * and sort lookups via arrayToStringIndex helper.
     */
        private static final String RELEASE_CUSTOM_JS =
            "    arrayToStringIndex(doc.languages, 'languages');" +
            "    arrayToStringIndex(doc.operatingSystems, 'operatingSystems');" +
            "    arrayToStringIndex(doc.softwarePlatforms, 'softwarePlatforms');" +
            "    arrayToStringIndex(doc.mainLicenseIds, 'mainLicenseIds');" +
            "    arrayToStringIndex(doc.externalIds, 'externalIds');" +
            "    if(doc.eccInformation !== undefined && doc.eccInformation != null) {" +
            "      if(doc.eccInformation.eccStatus !== undefined && doc.eccInformation.eccStatus != null && typeof(doc.eccInformation.eccStatus) == 'string' && doc.eccInformation.eccStatus.length > 0) {" +
            "        index('text', 'eccStatus', doc.eccInformation.eccStatus);" +
            "        index('string', 'eccStatus_sort', doc.eccInformation.eccStatus.toLowerCase());" +
            "      }" +
            "      if(doc.eccInformation.assessorContactPerson !== undefined && doc.eccInformation.assessorContactPerson != null && typeof(doc.eccInformation.assessorContactPerson) == 'string' && doc.eccInformation.assessorContactPerson.length > 0) {" +
            "        index('text', 'eccAssessor', doc.eccInformation.assessorContactPerson);" +
            "        index('string', 'eccAssessor_sort', doc.eccInformation.assessorContactPerson.toLowerCase());" +
            "      }" +
            "      if(doc.eccInformation.assessorDepartment !== undefined && doc.eccInformation.assessorDepartment != null && typeof(doc.eccInformation.assessorDepartment) == 'string' && doc.eccInformation.assessorDepartment.length > 0) {" +
            "        index('text', 'eccAssessorGroup', doc.eccInformation.assessorDepartment);" +
            "        index('string', 'eccAssessorGroup_sort', doc.eccInformation.assessorDepartment.toLowerCase());" +
            "      }" +
            "      if(doc.eccInformation.eccn !== undefined && doc.eccInformation.eccn != null && typeof(doc.eccInformation.eccn) == 'string' && doc.eccInformation.eccn.length > 0) {" +
            "        index('text', 'eccn', doc.eccInformation.eccn);" +
            "        index('string', 'eccn_sort', doc.eccInformation.eccn.toLowerCase());" +
            "      }" +
            "    }" +
            INDEX_VERSION_SEGMENTS +
            INDEX_ID_FIELD;

    /**
     * Analyzer overrides for fields created by {@code arrayToStringIndex}.
     * The helper generates {@code <field>_sort} string indexes that require
     * the {@code keyword} analyzer for correct sorting behavior.
     */
    private static final Map<String, String> RELEASE_CUSTOM_ANALYZERS = Map.of(
            "languages_sort", "keyword",
            "operatingSystems_sort", "keyword",
            "softwarePlatforms_sort", "keyword",
            "mainLicenseIds_sort", "keyword",
            "externalIds_sort", "keyword",
            "eccStatus_sort", "keyword",
            "eccAssessor_sort", "keyword",
            "eccAssessorGroup_sort", "keyword",
            "eccn_sort", "keyword",
            "id", "keyword"
    );

    private static final BuiltIndexDefinition RELEASE_INDEX_DEFINITION = buildIndexFunction(
            "release",
            SW360Constants.PROJECT_SEARCH_EMPTY_TOKEN,
            RELEASE_FIELDS,
            RELEASE_CUSTOM_JS,
            RELEASE_CUSTOM_ANALYZERS,
            "standard"
    );

    // -------------------------------------------------------------------------
    //  Constructor
    // -------------------------------------------------------------------------

    private final NouveauLuceneAwareDatabaseConnector connector;

        private static final List<String> QUICK_FILTER_FIELDS = List.of(
            Release._Fields.ID.getFieldName(),
            Release._Fields.NAME.getFieldName(),
            Release._Fields.VERSION.getFieldName(),
            Release._Fields.EXTERNAL_IDS.getFieldName(),
            "eccStatus",
            "eccAssessor",
            "eccAssessorGroup",
            "eccn"
    );

    public ReleaseSearchHandler(Cloudant cClient, String dbName) throws IOException {
        super(Release.class, "releases", RELEASE_INDEX_DEFINITION);
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(cClient, dbName);
        connector = new NouveauLuceneAwareDatabaseConnector(db, DDOC_NAME, dbName, db.getInstance().getGson());
        setup(connector, db);
    }

    // -------------------------------------------------------------------------
    //  Public search API
    // -------------------------------------------------------------------------

    /**
     * Paginated search with permission filtering.
     */
    public Map<PaginationData, List<Release>> searchAccessibleReleases(
            final Map<String, Set<String>> subQueryRestrictions, @Nullable User user,
            PaginationData pageData
    ) {
        String visibilityQuery = buildVisibilityLuceneQuery(user);
        if (CommonUtils.isNullOrEmptyMap(subQueryRestrictions)) {
            String query = CommonUtils.isNotNullEmptyOrWhitespace(visibilityQuery) ? visibilityQuery : "*:*";
            return connector.searchView(Release.class, getIndexName(), query,
                    pageData, getSortColumns(pageData));
        }

        return baseSearch(connector, subQueryRestrictions, visibilityQuery, pageData);
    }

    /**
     * Search Releases with id, name, description or externalIds fields.
     */
    public Map<PaginationData, List<Release>> searchFilteredReleases(
            final String searchText, @Nullable User user, PaginationData pageData
    ) {
        Map<String, Set<String>> subQueryRestrictions = new HashMap<>();
        for (String fieldName : QUICK_FILTER_FIELDS) {
            subQueryRestrictions.put(fieldName, Collections.singleton(searchText));
        }
        String visibilityQuery = buildVisibilityLuceneQuery(user);
        return baseSearchWithOr(connector, subQueryRestrictions, visibilityQuery, pageData);
    }

    /**
     * Search Releases by quick-filter fields while applying additional AND restrictions
     * (e.g. project-scoped release id set).
     */
    public Map<PaginationData, List<Release>> searchFilteredReleasesWithAndRestrictions(
            final String searchText,
            final Map<String, Set<String>> andRestrictions,
            @Nullable User user,
            PaginationData pageData
    ) {
        String visibilityQuery = buildVisibilityLuceneQuery(user);
        if (CommonUtils.isNullEmptyOrWhitespace(searchText)) {
            return baseSearch(connector, andRestrictions, visibilityQuery, pageData);
        }

        Map<String, Set<String>> orRestrictions = new HashMap<>();
        for (String fieldName : QUICK_FILTER_FIELDS) {
            orRestrictions.put(fieldName, Collections.singleton(searchText));
        }

        Map<String, Map<String, Set<String>>> complexRestrictions = new LinkedHashMap<>();
        complexRestrictions.put("OR", orRestrictions);
        complexRestrictions.put("AND", andRestrictions);

        return complexBaseSearch(connector, complexRestrictions, AND, visibilityQuery, pageData);
    }

    public Map<PaginationData, List<Release>> searchAccessibleReleasesFromComponent(
            String componentId, String searchText, @Nullable User user, PaginationData pageData
    ) {
        String visibilityQuery = buildVisibilityLuceneQuery(user);
        Map<String, Set<String>> andRestrictions = new HashMap<>();
        andRestrictions.put(Release._Fields.COMPONENT_ID.getFieldName(), Collections.singleton(componentId));

        if (CommonUtils.isNullEmptyOrWhitespace(searchText)) {
            return baseSearch(connector, andRestrictions, visibilityQuery, pageData);
        }

        Map<String, Set<String>> orRestrictions = new HashMap<>();
        orRestrictions.put(Release._Fields.ID.getFieldName(), Collections.singleton(searchText));
        orRestrictions.put(Release._Fields.VERSION.getFieldName(), Collections.singleton(searchText));

        Map<String, Map<String, Set<String>>> complexRestrictions = new LinkedHashMap<>();
        complexRestrictions.put("OR", orRestrictions);
        complexRestrictions.put("AND", andRestrictions);

        return complexBaseSearch(connector, complexRestrictions, AND, visibilityQuery, pageData);
    }

    // -------------------------------------------------------------------------
    //  Visibility / permission Lucene query
    // -------------------------------------------------------------------------

    /**
     * Build a Lucene query string that enforces release visibility rules for the given user,
     * mirroring {@link org.eclipse.sw360.datahandler.permissions.ReleasePermissions#isVisible}.
     *
     * <p>Releases currently have no visibility restrictions; returns {@code null}.</p>
     *
     * @param user The requesting user, or {@code null}.
     * @return A Lucene query string, or {@code null} if no restriction should be applied.
     */
    @Nullable
    public static String buildVisibilityLuceneQuery(@Nullable User user) {
        return null;
    }

    // -------------------------------------------------------------------------
    //  Sort column mapping
    // -------------------------------------------------------------------------

    @Override
    protected @NonNull List<String> mapSortColumn(int sortColumnNumber) {
        String revDir = "-";
        return switch (ReleaseSortColumn.findByValue(sortColumnNumber)) {
            case ReleaseSortColumn.BY_NAME -> List.of("name_sort", revDir + "version_sort", revDir + "createdOn");
            case ReleaseSortColumn.BY_VERSION -> List.of("version_sort", "name_sort", revDir + "createdOn");
            case ReleaseSortColumn.BY_CLEARING_STATE -> List.of("clearingState_sort", SCORE_SORTING_FIELD, "name_sort", revDir + "createdOn");
            case ReleaseSortColumn.BY_MAINLINE_STATE -> List.of("mainlineState_sort", SCORE_SORTING_FIELD, "name_sort", revDir + "createdOn");
            case ReleaseSortColumn.BY_CREATEDON -> List.of("createdOn");
            case null, default -> List.of(SCORE_SORTING_FIELD);
        };
    }
}

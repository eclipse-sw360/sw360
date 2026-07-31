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
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseSortColumn;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.eclipse.sw360.datahandler.common.SearchUtils.INDEX_ID_FIELD;
import static org.eclipse.sw360.datahandler.permissions.PermissionUtils.makePermission;
import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.DEFAULT_DESIGN_PREFIX;
import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.SCORE_SORTING_FIELD;

/**
 * Nouveau search handler for Releases with paginated access control filtering.
 *
 * @author thomas.maier@evosoft.com
 */
public class ReleaseSearchHandler extends BaseNouveauSearchHandler<Release> {

    private static final String DDOC_NAME = DEFAULT_DESIGN_PREFIX + "lucene";

    // -------------------------------------------------------------------------
    //  Field spec declarations
    // -------------------------------------------------------------------------

    private static final List<IndexField> RELEASE_FIELDS = List.of(
            IndexField.string("id"),
            IndexField.standard("name"),
            IndexField.standard("version"),
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

    private static final List<Release._Fields> QUICK_FILTER_FIELDS = List.of(
            Release._Fields.ID,
            Release._Fields.NAME,
            Release._Fields.VERSION,
            Release._Fields.EXTERNAL_IDS
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
            final Map<String, Set<String>> subQueryRestrictions, User user, PaginationData pageData) {
        Map<PaginationData, List<Release>> resultReleaseList = baseSearch(connector, subQueryRestrictions, pageData);

        PaginationData respPageData = resultReleaseList.keySet().iterator().next();
        List<Release> releaseList = resultReleaseList.values().iterator().next();

        releaseList = releaseList.stream().filter(release ->
                makePermission(release, user).isActionAllowed(RequestedAction.READ))
                .toList();

        return Collections.singletonMap(respPageData, releaseList);
    }

    /**
     * Search Releases with id, name, description or externalIds fields.
     */
    public Map<PaginationData, List<Release>> searchFilteredReleases(
            final String searchText, User user, PaginationData pageData
    ) {
        Map<String, Set<String>> subQueryRestrictions = new HashMap<>();
        for (Release._Fields field : QUICK_FILTER_FIELDS) {
            subQueryRestrictions.put(field.getFieldName(), Collections.singleton(searchText));
        }
        Map<PaginationData, List<Release>> resultReleaseList = baseSearchWithOr(connector, subQueryRestrictions, pageData);

        PaginationData respPageData = resultReleaseList.keySet().iterator().next();
        List<Release> releaseList = resultReleaseList.values().iterator().next();

        releaseList = releaseList.stream().filter(release ->
                makePermission(release, user).isActionAllowed(RequestedAction.READ))
                .toList();

        return Collections.singletonMap(respPageData, releaseList);
    }

    /**
     * Non-paginated search (legacy callers).
     */
    public List<Release> search(String text, final Map<String, Set<String>> subQueryRestrictions) {
        return connector.searchViewWithRestrictionsWithAnd(Release.class, getIndexName(),
                text, subQueryRestrictions);
    }

    // -------------------------------------------------------------------------
    //  Sort column mapping
    // -------------------------------------------------------------------------

    @Override
    protected List<String> mapSortColumn(int sortColumnNumber) {
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

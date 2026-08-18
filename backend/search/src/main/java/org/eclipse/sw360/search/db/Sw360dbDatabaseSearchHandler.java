/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.search.db;

import com.ibm.cloud.cloudant.v1.Cloudant;
import org.eclipse.sw360.datahandler.cloudantclient.BaseNouveauSearchHandler;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.db.ComponentRepository;
import org.eclipse.sw360.datahandler.db.ProjectRepository;
import org.eclipse.sw360.datahandler.db.ReleaseRepository;
import org.eclipse.sw360.datahandler.db.VendorRepository;
import org.eclipse.sw360.datahandler.permissions.ComponentPermissions;
import org.eclipse.sw360.datahandler.permissions.ProjectPermissions;
import org.eclipse.sw360.datahandler.permissions.ReleasePermissions;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.search.SearchResult;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.nouveau.NouveauResult;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.SCORE_SORTING_FIELD;

/**
 * Full text search handler for the main SW360 database. It serves every document type except
 * users, which live in their own database.
 */
public class Sw360dbDatabaseSearchHandler extends BaseNouveauSearchHandler<SearchResult> {

    // -------------------------------------------------------------------------
    //  Field spec declarations
    // -------------------------------------------------------------------------

    private static final String INDEX_NAME = "all";

    private static final String TYPE_FIELD = "type";
    private static final String DEFAULT_FIELD = "default";
    private static final String NAME_FIELD = "name";
    private static final String FULLNAME_FIELD = "fullname";
    private static final String TITLE_FIELD = "title";

    /**
     * Type mask entry requesting the generic "search the whole document" mode. It is always the
     * last entry of the mask.
     */
    private static final String DOCUMENT_TYPE_MASK = SW360Constants.TYPE_DOCUMENT;

    /** Document types carrying their display name in {@code name}. */
    private static final Set<String> NAME_TYPES = Set.of(
            SW360Constants.TYPE_PROJECT, SW360Constants.TYPE_COMPONENT,
            SW360Constants.TYPE_RELEASE, SW360Constants.TYPE_PACKAGE
    );
    /** Document types carrying their display name in {@code fullname}. */
    private static final Set<String> FULLNAME_TYPES = Set.of(
            SW360Constants.TYPE_LICENSE, SW360Constants.TYPE_VENDOR
    );
    /** Document types carrying their display name in {@code title}. */
    private static final Set<String> TITLE_TYPES = Set.of(
            SW360Constants.TYPE_OBLIGATION, SW360Constants.TYPE_OBLIGATIONS
    );

    /**
     * Fields indexed for the global search, grouped by index category.
     *
     * <ul>
     *   <li><b>default</b>: the whole document flattened into a text blob, used by the
     *   unrestricted ({@code document}) search.</li>
     *   <li><b>string</b>: {@code type} - used to restrict a search to given document types.</li>
     *   <li><b>standard</b>: {@code name}, {@code fullname}, {@code title} - the display name of
     *   the different document types, with full prefix-search support.</li>
     * </ul>
     */
    private static final List<IndexField> SEARCH_FIELDS = List.of(
            IndexField.defaultIndex(),
            IndexField.string(TYPE_FIELD),
            IndexField.standard(NAME_FIELD),
            IndexField.standard(FULLNAME_FIELD),
            IndexField.standard(TITLE_FIELD)
    );

    // -------------------------------------------------------------------------
    //  Design document
    // -------------------------------------------------------------------------

    /**
     * The database holds many document types, so the index function must not be restricted to a
     * single {@code doc.type}.
     */
    private static final BuiltIndexDefinition SEARCH_INDEX_DEFINITION = buildIndexFunction(
            null,
            "",
            SEARCH_FIELDS,
            null,
            Map.of(),
            "standard"
    );

    private final NouveauLuceneAwareDatabaseConnector connector;

    private final ProjectRepository projectRepository;
    private final ComponentRepository componentRepository;
    private final ReleaseRepository releaseRepository;

    public Sw360dbDatabaseSearchHandler() throws IOException {
        this(DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_DATABASE);
    }

    public Sw360dbDatabaseSearchHandler(Cloudant client, String dbName) throws IOException {
        super(SearchResult.class, INDEX_NAME, SEARCH_INDEX_DEFINITION);
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(client, dbName);
        connector = new NouveauLuceneAwareDatabaseConnector(db, DDOC_NAME, dbName, db.getInstance().getGson());
        setup(connector, db);

        projectRepository = new ProjectRepository(db);
        VendorRepository vendorRepository = new VendorRepository(db);
        releaseRepository = new ReleaseRepository(db, vendorRepository);
        componentRepository = new ComponentRepository(db, releaseRepository, vendorRepository);
    }

    // -------------------------------------------------------------------------
    //  Search
    // -------------------------------------------------------------------------

    /**
     * Search the whole document for the given text, restricted to the requested document types.
     */
    public @NonNull List<SearchResult> search(String text, List<String> typeMask, User user) {
        if (!isDocumentSearch(typeMask)) {
            return restrictedSearch(text, typeMask, user);
        }
        String query = buildQueryFromRestrictionsWithOr(Map.of(DEFAULT_FIELD, Collections.singleton(text)));
        return getSearchResults(restrictToTypes(query, typesWithoutDocumentMask(typeMask)), user);
    }

    // -------------------------------------------------------------------------
    //  Sorting
    // -------------------------------------------------------------------------

    @Override
    protected @NonNull @Unmodifiable List<String> mapSortColumn(int sortColumnNumber) {
        // The global search has no sortable columns, results are ordered by relevance and
        // re-sorted by the caller.
        return List.of(SCORE_SORTING_FIELD);
    }

    // -------------------------------------------------------------------------
    //  Helpers
    // -------------------------------------------------------------------------

    /**
     * Search only the display name of the documents matching the given type mask. Without a type
     * mask, all three display name fields are searched.
     */
    private @NonNull List<SearchResult> restrictedSearch(String text, List<String> typeMask, User user) {
        Map<String, Set<String>> nameRestrictions = new LinkedHashMap<>();

        if (CommonUtils.isNullOrEmptyCollection(typeMask)) {
            nameRestrictions.put(NAME_FIELD, Collections.singleton(text));
            nameRestrictions.put(FULLNAME_FIELD, Collections.singleton(text));
            nameRestrictions.put(TITLE_FIELD, Collections.singleton(text));
            return getSearchResults(buildQueryFromRestrictionsWithOr(nameRestrictions), user);
        }

        if (containsAny(typeMask, NAME_TYPES)) {
            nameRestrictions.put(NAME_FIELD, Collections.singleton(text));
        }
        if (containsAny(typeMask, FULLNAME_TYPES)) {
            nameRestrictions.put(FULLNAME_FIELD, Collections.singleton(text));
        }
        if (containsAny(typeMask, TITLE_TYPES)) {
            nameRestrictions.put(TITLE_FIELD, Collections.singleton(text));
        }

        return getSearchResults(
                restrictToTypes(buildQueryFromRestrictionsWithOr(nameRestrictions), typeMask), user);
    }

    /**
     * Run the query and convert the raw hits. The documents are shipped with the hits because the
     * result set mixes document types and therefore cannot be deserialized into a single type.
     */
    private @NonNull List<SearchResult> getSearchResults(String queryString, User user) {
        NouveauResult queryResult = baseSearchRaw(connector, queryString,
                NouveauLuceneAwareDatabaseConnector.pageDataForAllRecords());
        return SearchResultConverter.convertAndFilter(queryResult, result -> isVisibleToUser(result, user));
    }

    /**
     * Restrict an already built query to the given document types. The query is returned unchanged
     * when no type restriction is requested.
     */
    private @NonNull String restrictToTypes(@NonNull String query, Collection<String> typeMask) {
        if (CommonUtils.isNullOrEmptyCollection(typeMask)) {
            return query;
        }
        return AND.join(List.of("( " + buildTypeQuery(typeMask) + " )", "( " + query + " )"));
    }

    private @NonNull String buildTypeQuery(@NonNull Collection<String> typeMask) {
        return buildQueryFromRestrictionsWithOr(Map.of(TYPE_FIELD, new LinkedHashSet<>(typeMask)));
    }

    private static boolean isDocumentSearch(List<String> typeMask) {
        return !CommonUtils.isNullOrEmptyCollection(typeMask)
                && DOCUMENT_TYPE_MASK.equals(typeMask.getLast());
    }

    private static @NonNull @Unmodifiable List<String> typesWithoutDocumentMask(@NonNull List<String> typeMask) {
        return typeMask.stream().filter(m -> !DOCUMENT_TYPE_MASK.equals(m)).toList();
    }

    private static boolean containsAny(@NonNull Collection<String> typeMask, Set<String> types) {
        return typeMask.stream().anyMatch(types::contains);
    }

    private boolean isVisibleToUser(@NonNull SearchResult result, User user) {
        if (SW360Constants.TYPE_PROJECT.equals(result.type)) {
            Project project = projectRepository.get(result.id);
            return ProjectPermissions.isVisible(user).test(project);
        } else if (SW360Constants.TYPE_COMPONENT.equals(result.type)) {
            Component component = componentRepository.get(result.id);
            return ComponentPermissions.isVisible(user).test(component);
        } else if (SW360Constants.TYPE_RELEASE.equals(result.type)) {
            Release release = releaseRepository.get(result.id);
            boolean isReleaseVisible = ReleasePermissions.isVisible(user).test(release);
            boolean isComponentVisible = false;
            String componentId = release.getComponentId();
            if (CommonUtils.isNotNullEmptyOrWhitespace(componentId)) {
                Component component = componentRepository.get(componentId);
                isComponentVisible = ComponentPermissions.isVisible(user).test(component);
            }
            return isReleaseVisible && isComponentVisible;
        }
        return true;
    }
}

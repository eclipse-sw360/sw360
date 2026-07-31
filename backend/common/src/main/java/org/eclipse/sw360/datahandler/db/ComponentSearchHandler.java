/*
 * Copyright Siemens AG, 2013-2016. Part of the SW360 Portal Project.
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
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.ComponentSortColumn;
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
 * Nouveau search handler for Components with paginated access control filtering.
 *
 * @author cedric.bodet@tngtech.com
 * @author alex.borodin@evosoft.com
 */
public class ComponentSearchHandler extends BaseNouveauSearchHandler<Component> {

    private static final String DDOC_NAME = DEFAULT_DESIGN_PREFIX + "lucene";

    // -------------------------------------------------------------------------
    //  Field spec declarations
    // -------------------------------------------------------------------------

    private static final List<IndexField> COMPONENT_FIELDS = List.of(
            IndexField.standard("name"),
            IndexField.simple("componentType", "keyword"),
            IndexField.simple("createdBy", "email"),
            IndexField.simple("businessUnit"),
            IndexField.simple("description"),
            IndexField.date("createdOn")
    );

    private static final Map<String, String> COMPONENT_CUSTOM_ANALYZERS = Map.of(
            "categories_sort", "email",
            "languages_sort", "keyword",
            "softwarePlatforms_sort", "keyword",
            "operatingSystems_sort", "keyword",
            "vendorNames_sort", "keyword",
            "mainLicenseIds_sort", "keyword",
            "externalIds_sort", "keyword",
            "id", "keyword"
    );

    /**
     * Component-specific JS for array-backed fields that should support text
     * and sort lookups via arrayToStringIndex helper.
     */
    private static final String COMPONENT_CUSTOM_JS =
            "    arrayToStringIndex(doc.categories, 'categories');" +
            "    arrayToStringIndex(doc.languages, 'languages');" +
            "    arrayToStringIndex(doc.softwarePlatforms, 'softwarePlatforms');" +
            "    arrayToStringIndex(doc.operatingSystems, 'operatingSystems');" +
            "    arrayToStringIndex(doc.vendorNames, 'vendorNames');" +
            "    arrayToStringIndex(doc.mainLicenseIds, 'mainLicenseIds');" +
            "    arrayToStringIndex(doc.externalIds, 'externalIds');" +
            INDEX_ID_FIELD;

    private static final BuiltIndexDefinition COMPONENT_INDEX_DEFINITION = buildIndexFunction(
            "component",
            SW360Constants.PROJECT_SEARCH_EMPTY_TOKEN,
            COMPONENT_FIELDS,
            COMPONENT_CUSTOM_JS,
            COMPONENT_CUSTOM_ANALYZERS,
            "standard"
    );

    // -------------------------------------------------------------------------
    //  Constructor
    // -------------------------------------------------------------------------

    private final NouveauLuceneAwareDatabaseConnector connector;

    private static final List<Component._Fields> QUICK_FILTER_FIELDS = List.of(
            Component._Fields.ID,
            Component._Fields.NAME,
            Component._Fields.DESCRIPTION,
            Component._Fields.EXTERNAL_IDS
    );

    public ComponentSearchHandler(Cloudant cClient, String dbName) throws IOException {
        super(Component.class, "components", COMPONENT_INDEX_DEFINITION);
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
    public Map<PaginationData, List<Component>> searchAccessibleComponents(
            final Map<String, Set<String>> subQueryRestrictions, User user, PaginationData pageData) {
        Map<PaginationData, List<Component>> resultComponentList = baseSearch(connector, subQueryRestrictions, pageData);

        PaginationData respPageData = resultComponentList.keySet().iterator().next();
        List<Component> componentList = resultComponentList.values().iterator().next();

        componentList = componentList.stream().filter(component ->
                makePermission(component, user).isActionAllowed(RequestedAction.READ))
                .toList();

        return Collections.singletonMap(respPageData, componentList);
    }

    /**
     * Search Components with id, name, description or externalIds fields.
     */
    public Map<PaginationData, List<Component>> searchFilteredComponents(
            String searchText, User user, PaginationData pageData
    ) {
        Map<String, Set<String>> subQueryRestrictions = new HashMap<>();
        for (Component._Fields field : QUICK_FILTER_FIELDS) {
            subQueryRestrictions.put(field.getFieldName(), Collections.singleton(searchText));
        }
        Map<PaginationData, List<Component>> resultComponentList = baseSearchWithOr(connector, subQueryRestrictions, pageData);
        PaginationData respPageData = resultComponentList.keySet().iterator().next();
        List<Component> componentList = resultComponentList.values().iterator().next();

        componentList = componentList.stream().filter(component ->
                        makePermission(component, user).isActionAllowed(RequestedAction.READ))
                .toList();

        return Collections.singletonMap(respPageData, componentList);
    }

    /**
     * Non-paginated search (legacy callers).
     */
    public List<Component> search(String text, final Map<String, Set<String>> subQueryRestrictions) {
        return connector.searchViewWithRestrictionsWithAnd(Component.class, getIndexName(),
                text, subQueryRestrictions);
    }

    /**
     * Non-paginated search with accessibility information filled.
     */
    public List<Component> searchWithAccessibility(String text, final Map<String, Set<String>> subQueryRestrictions,
                                                   User user) {
        List<Component> resultComponentList = connector.searchViewWithRestrictionsWithAnd(Component.class,
                getIndexName(), text, subQueryRestrictions);
        for (Component component : resultComponentList) {
            makePermission(component, user).fillPermissionsInOther(component);
        }
        return resultComponentList;
    }

    // -------------------------------------------------------------------------
    //  Sort column mapping
    // -------------------------------------------------------------------------

    @Override
    protected List<String> mapSortColumn(int sortColumnNumber) {
        String revDir = "-";
        return switch (ComponentSortColumn.findByValue(sortColumnNumber)) {
            case ComponentSortColumn.BY_NAME -> List.of("name_sort", revDir + "createdOn");
            case ComponentSortColumn.BY_VENDOR -> List.of("vendorNames_sort", SCORE_SORTING_FIELD, "name_sort", revDir + "createdOn");
            case ComponentSortColumn.BY_MAINLICENSE -> List.of("mainLicenseIds_sort", SCORE_SORTING_FIELD, "name_sort", revDir + "createdOn");
            case ComponentSortColumn.BY_TYPE -> List.of("componentType_sort", SCORE_SORTING_FIELD, "name_sort", revDir + "createdOn");
            case ComponentSortColumn.BY_CREATEDON -> List.of("createdOn");
            case null, default -> List.of(SCORE_SORTING_FIELD);
        };
    }
}

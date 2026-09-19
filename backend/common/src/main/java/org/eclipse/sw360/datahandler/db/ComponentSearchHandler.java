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
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.ComponentSortColumn;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.eclipse.sw360.datahandler.common.SW360ConfigKeys.IS_COMPONENT_VISIBILITY_RESTRICTION_ENABLED;
import static org.eclipse.sw360.datahandler.common.SearchUtils.INDEX_ID_FIELD;
import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.SCORE_SORTING_FIELD;

/**
 * Nouveau search handler for Components with paginated access control filtering.
 *
 * @author cedric.bodet@tngtech.com
 * @author alex.borodin@evosoft.com
 */
public class ComponentSearchHandler extends BaseNouveauSearchHandler<Component> {

    // -------------------------------------------------------------------------
    //  Field spec declarations
    // -------------------------------------------------------------------------

    private static final List<IndexField> COMPONENT_FIELDS = List.of(
            IndexField.standard("name"),
            IndexField.simple("componentType", "keyword"),
            IndexField.simple("createdBy", "email"),
            IndexField.simple("businessUnit"),
            IndexField.simple("description"),
            IndexField.simple("visbility", "keyword"),
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
            "moderators", "email",
            "id", "keyword"
    );

    /**
     * Component-specific JS for array-backed fields that should support text
     * and sort lookups via arrayToStringIndex helper, plus moderators indexing.
     */
    private static final String COMPONENT_CUSTOM_JS =
            "    arrayToStringIndex(doc.categories, 'categories');" +
            "    arrayToStringIndex(doc.languages, 'languages');" +
            "    arrayToStringIndex(doc.softwarePlatforms, 'softwarePlatforms');" +
            "    arrayToStringIndex(doc.operatingSystems, 'operatingSystems');" +
            "    arrayToStringIndex(doc.vendorNames, 'vendorNames');" +
            "    arrayToStringIndex(doc.mainLicenseIds, 'mainLicenseIds');" +
            "    arrayToStringIndex(doc.externalIds, 'externalIds');" +
            "    if(doc.moderators && doc.moderators.length > 0) {" +
            "      for(var i in doc.moderators) {" +
            "        index('text', 'moderators', doc.moderators[i]);" +
            "      }" +
            "    }" +
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
            final Map<String, Set<String>> subQueryRestrictions,
            @Nullable User user,
            PaginationData pageData
    ) {
        String visibilityQuery = buildVisibilityLuceneQuery(user);
        if (CommonUtils.isNullOrEmptyMap(subQueryRestrictions)) {
            String query = CommonUtils.isNotNullEmptyOrWhitespace(visibilityQuery) ? visibilityQuery : "*:*";
            return connector.searchView(Component.class, getIndexName(), query,
                    pageData, getSortColumns(pageData));
        }

        return baseSearch(connector, subQueryRestrictions, visibilityQuery, pageData);
    }

    /**
     * Search Components with id, name, description or externalIds fields.
     */
    public Map<PaginationData, List<Component>> searchFilteredComponents(
            String searchText, @Nullable User user, PaginationData pageData
    ) {
        Map<String, Set<String>> subQueryRestrictions = new HashMap<>();
        for (Component._Fields field : QUICK_FILTER_FIELDS) {
            subQueryRestrictions.put(field.getFieldName(), Collections.singleton(searchText));
        }
        String visibilityQuery = buildVisibilityLuceneQuery(user);
        return baseSearchWithOr(connector, subQueryRestrictions,
                visibilityQuery, pageData);
    }

    // -------------------------------------------------------------------------
    //  Visibility / permission Lucene query
    // -------------------------------------------------------------------------

    /**
     * Build a Lucene query string that enforces component visibility rules for the given user,
     * mirroring the logic in {@link org.eclipse.sw360.datahandler.permissions.ComponentPermissions#isVisible}.
     *
     * <p>When {@code IS_COMPONENT_VISIBILITY_RESTRICTION_ENABLED} is disabled (false / default),
     * no visibility filter is applied (returns {@code null}).</p>
     *
     * @param user The requesting user, or {@code null} (no filter applied when null).
     * @return A Lucene query string, or {@code null} if no restriction should be applied.
     */
    @Nullable
    public static String buildVisibilityLuceneQuery(@Nullable User user) {
        if (!SW360Utils.readConfig(IS_COMPONENT_VISIBILITY_RESTRICTION_ENABLED, false)) {
            return null;
        }

        if (user == null || PermissionUtils.isAdmin(user)) {
            return null;
        }

        boolean isClearingAdmin = PermissionUtils.isUserAtLeast(UserGroup.CLEARING_ADMIN, user);
        String email = user.getEmail();
        String primaryBU = SW360Utils.getBUFromOrganisation(user.getDepartment());

        Set<String> allBUs = new HashSet<>();
        if (CommonUtils.isNotNullEmptyOrWhitespace(primaryBU)) {
            allBUs.add(primaryBU);
        }
        Map<String, Set<UserGroup>> secondaryDepartmentsAndRoles = user.getSecondaryDepartmentsAndRoles();
        if (!CommonUtils.isNullOrEmptyMap(secondaryDepartmentsAndRoles)) {
            secondaryDepartmentsAndRoles.keySet().stream()
                    .map(SW360Utils::getBUFromOrganisation)
                    .filter(CommonUtils::isNotNullEmptyOrWhitespace)
                    .forEach(allBUs::add);
        }

        // Clause 1: PRIVATE components owned by the user
        String privateClause = "(visbility:\"PRIVATE\" AND createdBy:\"" + email + "\")";

        // Clause 2: visible to everyone
        String everyoneClause = "visbility:\"EVERYONE\"";

        // Clause 3: ME_AND_MODERATORS – user must be creator or moderator
        String memberCheck = "createdBy:\"" + email + "\" OR moderators:\"" + email + "\"";
        String meAndModeratorClause = "(visbility:\"ME_AND_MODERATORS\" AND (" + memberCheck + "))";

        // Clause 4: BUISNESSUNIT_AND_MODERATORS
        String buAndModeratorClause;
        if (isClearingAdmin) {
            buAndModeratorClause = "visbility:\"BUISNESSUNIT_AND_MODERATORS\"";
        } else {
            List<String> buOrMemberParts = new ArrayList<>();
            for (String bu : allBUs) {
                buOrMemberParts.add("businessUnit:\"" + bu + "\"");
            }
            buOrMemberParts.add(memberCheck);
            buAndModeratorClause = "(visbility:\"BUISNESSUNIT_AND_MODERATORS\" AND ("
                    + String.join(" OR ", buOrMemberParts) + "))";
        }

        return privateClause
                + " OR " + everyoneClause
                + " OR " + meAndModeratorClause
                + " OR " + buAndModeratorClause;
    }

    // -------------------------------------------------------------------------
    //  Sort column mapping
    // -------------------------------------------------------------------------

    @Override
    protected @NonNull List<String> mapSortColumn(int sortColumnNumber) {
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

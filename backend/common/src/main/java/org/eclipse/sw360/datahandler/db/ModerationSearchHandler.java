/*
 * Copyright Siemens AG, 2021. Part of the SW360 Portal Project.
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
import org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationSortColumn;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.jspecify.annotations.NonNull;

import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.SCORE_SORTING_FIELD;

public class ModerationSearchHandler extends BaseNouveauSearchHandler<ModerationRequest> {

    // -------------------------------------------------------------------------
    //  Field spec declarations
    // -------------------------------------------------------------------------

    /**
     * Fields common to all vulnerabilities, grouped by index category.
     *
     * <ul>
     *   <li><b>standard</b>: {@code documentName} - full prefix-search support.</li>
     *   <li><b>simple</b>: {@code documentType}, {@code componentType}, {@code requestingUser},
     *   {@code moderationState}, {@code requestingUserDepartment}</li>
     *   <li><b>date</b>: {@code timestamp} - stored as sortable yyyyMMdd double.</li>
     * </ul>
     */
    private static final List<IndexField> MR_FIELDS = List.of(
        IndexField.standard("documentName"),
        IndexField.simple("documentType", "keyword"),
        IndexField.simple("componentType", "keyword"),
        IndexField.simple("requestingUser", "email"),
        IndexField.simple("moderationState", "keyword"),
        IndexField.simple("requestingUserDepartment", "keyword"),
        IndexField.date("timestamp")
    );

    /**
     * Handler-specific JS: index {@code moderators} as a concatenated text blob
     */
    private static final String MR_CUSTOM_JS =
            "    arrayToStringIndex(doc.moderators, 'moderators');";
    /**
     * Analyzer overrides that are not auto-generated from {@link #MR_FIELDS}.
     * <ul>
     *   <li>{@code moderators} -> {@code email}</li>
     * </ul>
     */
    private static final Map<String, String> MR_CUSTOM_ANALYZERS = Map.of(
            "moderators", "email"
    );

    // -------------------------------------------------------------------------
    //  Design document
    // -------------------------------------------------------------------------

    private static final BuiltIndexDefinition MR_INDEX_DEFINITION = buildIndexFunction(
            "moderation",
            "",
            MR_FIELDS,
            MR_CUSTOM_JS,
            MR_CUSTOM_ANALYZERS,
            "standard"
    );

    private final NouveauLuceneAwareDatabaseConnector connector;

    public ModerationSearchHandler(Cloudant client, String dbName) throws IOException {
        super(ModerationRequest.class, "moderations", MR_INDEX_DEFINITION);
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(client, dbName);
        connector = new NouveauLuceneAwareDatabaseConnector(db, DDOC_NAME, dbName, db.getInstance().getGson());
        setup(connector, db);
    }

    public Map<PaginationData, List<ModerationRequest>> search(String text,
            final Map<String, Set<String>> subQueryRestrictions, PaginationData pageData, User sw360User) {
        String moderatorKey = ModerationRequest._Fields.MODERATORS.getFieldName();
        String requestingUserKey = ModerationRequest._Fields.REQUESTING_USER.getFieldName();
        if (!subQueryRestrictions.containsKey(moderatorKey) && !subQueryRestrictions.containsKey(requestingUserKey)) {
            subQueryRestrictions.put(moderatorKey, Collections.singleton(sw360User.getEmail()));
            subQueryRestrictions.put(requestingUserKey, Collections.singleton(sw360User.getEmail()));
        }

        Map<String, Map<String, Set<String>>> restrictions = new java.util.HashMap<>();
        Map<String, Set<String>> orRestrictions = new java.util.HashMap<>();
        Map<String, Set<String>> andRestrictions = new java.util.HashMap<>();

        if (subQueryRestrictions != null) {
            for (Map.Entry<String, Set<String>> entry : subQueryRestrictions.entrySet()) {
                if (entry.getValue() == null || entry.getValue().isEmpty()) {
                    continue;
                }

                String fieldName = entry.getKey();
                Set<String> values = entry.getValue();

                if (isModeratorOrRequestingUserField(fieldName)) {
                    orRestrictions.put(fieldName, values);
                } else {
                    andRestrictions.put(fieldName, values);
                }
            }
        }

        if (!orRestrictions.isEmpty()) {
            restrictions.put("OR", orRestrictions);
        }
        if (!andRestrictions.isEmpty()) {
            restrictions.put("AND", andRestrictions);
        }

        return complexBaseSearch(connector, restrictions, AND, pageData);
    }

    private static boolean isModeratorOrRequestingUserField(String fieldName) {
        return ModerationRequest._Fields.MODERATORS.getFieldName().equals(fieldName)
                || ModerationRequest._Fields.REQUESTING_USER.getFieldName().equals(fieldName);
    }

    @Override
    protected @NonNull List<String> mapSortColumn(int sortColumnNumber) {
        String revDir = "-";
        return switch (ModerationSortColumn.findByValue(sortColumnNumber)) {
            case ModerationSortColumn.BY_DOCUMENT_NAME -> List.of("documentName_sort", SCORE_SORTING_FIELD, revDir + "timestamp");
            case ModerationSortColumn.BY_DOCUMENT_TYPE -> List.of("documentType_sort", SCORE_SORTING_FIELD, revDir + "timestamp");
            case ModerationSortColumn.BY_COMPONENT_TYPE -> List.of("componentType_sort", SCORE_SORTING_FIELD, revDir + "timestamp");
            case ModerationSortColumn.BY_REQUESTING_USER -> List.of("requestingUser_sort", SCORE_SORTING_FIELD, revDir + "timestamp");
            case ModerationSortColumn.BY_MODERATION_STATE -> List.of("moderationState_sort", SCORE_SORTING_FIELD, revDir + "timestamp");
            case ModerationSortColumn.BY_REQUESTING_USER_DEPT -> List.of("requestingUserDepartment_sort", SCORE_SORTING_FIELD, revDir + "timestamp");
            case null, default -> List.of(SCORE_SORTING_FIELD, revDir + "timestamp");
        };
    }
}

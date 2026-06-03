/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseRepositoryCloudantClient;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.changelogs.ChangeLogs;

import com.google.common.collect.ImmutableSet;

import com.ibm.cloud.cloudant.v1.model.DesignDocumentViewsMapReduce;
import org.eclipse.sw360.datahandler.thrift.changelogs.Operation;

/**
 * CRUD access for the ChangeLogs class
 *
 * @author jaideep.palit@siemens.com
 */
public class ChangeLogsRepository extends DatabaseRepositoryCloudantClient<ChangeLogs> {

    public static final ImmutableSet<String> SET_OF_IGNORED_FIELD_VALUES = ImmutableSet.<String>builder()
            .add("\"\"")
            .add("[]")
            .add("{}").build();

    public static final ImmutableSet<String> SET_OF_IGNORED_FIELD_NAMES = ImmutableSet.<String>builder()
            .add("revision")
            .add("documentState").build();

    private static String getJsArray(Set<String> set) {
        return "[" + set.stream().map(s -> "'" + s.replace("'", "\\'") + "'").collect(Collectors.joining(",")) + "]";
    }

    private static final String IS_NOT_EMPTY_CHANGE_LOG =
            """
            function isNotEmptyChangeLog(doc) {
              if (doc.operation === '%s') {
                return true;
              }
              if (!doc.changes || doc.changes.length === 0) {
                return false;
              }
              var ignoredFieldValues = %s;
              var ignoredFieldNames = %s;
              var hasValidChanges = false;
              for (var i = 0; i < doc.changes.length; i++) {
                var ch = doc.changes[i];
                var fieldName = ch.fieldName;
                var oldFieldValue = ch.fieldValueOld;
                var newFieldValue = ch.fieldValueNew;
                if ((fieldName != null && ignoredFieldNames.indexOf(fieldName) !== -1)
                    || (oldFieldValue == null && newFieldValue == null)
                    || (oldFieldValue == null && ignoredFieldValues.indexOf(newFieldValue) !== -1)
                    || (newFieldValue == null && ignoredFieldValues.indexOf(oldFieldValue) !== -1)
                    || (oldFieldValue != null && newFieldValue != null && newFieldValue === oldFieldValue)) {
                  continue;
                }
                hasValidChanges = true;
                break;
              }
              return hasValidChanges;
            }""".formatted(Operation.CREATE.name(), getJsArray(SET_OF_IGNORED_FIELD_VALUES), getJsArray(SET_OF_IGNORED_FIELD_NAMES));

    private static final String ALL = "function(doc) { if (doc.type == 'changeLogs') emit(doc._id, null) }";
    private static final String BY_DOCUMENT_ID =
            "function(doc) {" +
                    "  if (doc.type == 'changeLogs') {" +
                    "    emit(doc.documentId, null);" +
                    "  }" +
                    "}";
    private static final String BY_PARENT_DOCUMENT_ID =
            "function(doc) {" +
                    "  if (doc.type == 'changeLogs') {" +
                    "    emit(doc.parentDocId, null);" +
                    "  }" +
                    "}";
    private static final String BY_USER_EDITED =
            "function(doc) {" +
                    "  if (doc.type == 'changeLogs') {" +
                    "    emit(doc.userEdited, null);" +
                    "  }" +
                    "}";
    private static final String BY_TIMESTAMP =
            "function(doc) {" +
                    "  if (doc.type == 'changeLogs') {" +
                    "    emit(doc.changeTimestamp, null);" +
                    "  }" +
                    "}";
    private static final String BY_DOCUMENT_ID_AND_TIMESTAMP = """
            function(doc) {
              %s
              if (doc.type === 'changeLogs' && isNotEmptyChangeLog(doc)) {
                if (doc.documentId) {
                  emit([doc.documentId, doc.changeTimestamp], null);
                }
                if (doc.parentDocId && doc.parentDocId !== doc.documentId) {
                  emit([doc.parentDocId, doc.changeTimestamp], null);
                }
              }
            }""".formatted(IS_NOT_EMPTY_CHANGE_LOG);

    public static final String PAGINATION_IDX = "PaginationIdx";

    public ChangeLogsRepository(DatabaseConnectorCloudant db) {
        super(db, ChangeLogs.class);
        Map<String, DesignDocumentViewsMapReduce> views = new HashMap<>();
        views.put("byDocumentId", createMapReduce(BY_DOCUMENT_ID, null));
        views.put("byParentDocumentId", createMapReduce(BY_PARENT_DOCUMENT_ID, null));
        views.put("byUserEdited", createMapReduce(BY_USER_EDITED, null));
        views.put("byTimestamp", createMapReduce(BY_TIMESTAMP, null));
        views.put("byDocumentIdAndTimestamp", createMapReduce(BY_DOCUMENT_ID_AND_TIMESTAMP, null));
        views.put("all", createMapReduce(ALL, null));
        initStandardDesignDocument(views, db);
    }

    public List<ChangeLogs> getChangeLogsByDocId(String docId) {
        return queryView("byDocumentId", docId).stream().collect(Collectors.toList());
    }

    public List<ChangeLogs> getChangeLogsByParentDocId(String docId) {
        return queryView("byParentDocumentId", docId).stream().collect(Collectors.toList());
    }

    public List<ChangeLogs> getChangeLogsByUserEdited(String userEdited) {
        return queryView("byUserEdited", userEdited).stream().collect(Collectors.toList());
    }

    public List<ChangeLogs> getChangeLogsByTimestamp(String timestamp) {
        return queryView("byTimestamp", timestamp).stream().collect(Collectors.toList());
    }

    public Map<PaginationData, List<ChangeLogs>> getChangeLogsByDocumentIdPaginated(String docId, PaginationData pageData) {
        List<ChangeLogs> results = queryViewWithComplexKeysPaginated("byDocumentIdAndTimestamp", docId, pageData);
        return Collections.singletonMap(pageData, results);
    }
}

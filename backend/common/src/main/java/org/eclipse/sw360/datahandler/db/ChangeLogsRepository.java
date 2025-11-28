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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseRepositoryCloudantClient;
import org.eclipse.sw360.datahandler.thrift.changelogs.ChangeLogs;

import com.ibm.cloud.cloudant.v1.model.DesignDocumentViewsMapReduce;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * CRUD access for the ChangeLogs class
 *
 * @author jaideep.palit@siemens.com
 */
@Component
public class ChangeLogsRepository extends DatabaseRepositoryCloudantClient<ChangeLogs> {

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

    public ChangeLogsRepository(
            @Qualifier("CLOUDANT_DB_CONNECTOR_DATABASE") DatabaseConnectorCloudant db
    ) {
        super(db, ChangeLogs.class);
        Map<String, DesignDocumentViewsMapReduce> views = new HashMap<>();
        views.put("byDocumentId", createMapReduce(BY_DOCUMENT_ID, null));
        views.put("byParentDocumentId", createMapReduce(BY_PARENT_DOCUMENT_ID, null));
        views.put("byUserEdited", createMapReduce(BY_USER_EDITED, null));
        views.put("byTimestamp", createMapReduce(BY_TIMESTAMP, null));
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
}

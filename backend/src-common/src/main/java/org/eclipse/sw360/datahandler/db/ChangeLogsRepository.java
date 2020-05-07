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

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.DatabaseRepository;
import org.eclipse.sw360.datahandler.thrift.changelogs.ChangeLogs;
import org.ektorp.support.View;

/**
 * CRUD access for the ChangeLogs class
 *
 * @author jaideep.palit@siemens.com
 */
@View(name = "all", map = "function(doc) { if (doc.type == 'changeLogs') emit(doc._id, doc) }")
public class ChangeLogsRepository extends DatabaseRepository<ChangeLogs> {

    private static final String BY_DOCUMENT_ID =
            "function(doc) {" +
                    "  if (doc.type == 'changeLogs') {" +
                    "    emit(doc.documentId, doc);" +
                    "  }" +
                    "}";
    private static final String BY_PARENT_DOCUMENT_ID =
            "function(doc) {" +
                    "  if (doc.type == 'changeLogs') {" +
                    "    emit(doc.parentDocId, doc);" +
                    "  }" +
                    "}";
    private static final String BY_USER_EDITED =
            "function(doc) {" +
                    "  if (doc.type == 'changeLogs') {" +
                    "    emit(doc.userEdited, doc);" +
                    "  }" +
                    "}";
    private static final String BY_TIMESTAMP =
            "function(doc) {" +
                    "  if (doc.type == 'changeLogs') {" +
                    "    emit(doc.changeTimestamp, doc);" +
                    "  }" +
                    "}";

    public ChangeLogsRepository(DatabaseConnector db) {
        super(ChangeLogs.class, db);
        initStandardDesignDocument();
    }

    @View(name = "byDocumentId", map = BY_DOCUMENT_ID)
    public List<ChangeLogs> getChangeLogsByDocId(String docId) {
        return queryView("byDocumentId", docId).stream().collect(Collectors.toList());
    }

    @View(name = "byParentDocumentId", map = BY_PARENT_DOCUMENT_ID)
    public List<ChangeLogs> getChangeLogsByParentDocId(String docId) {
        return queryView("byParentDocumentId", docId).stream().collect(Collectors.toList());
    }

    @View(name = "byUserEdited", map = BY_USER_EDITED)
    public List<ChangeLogs> getChangeLogsByUserEdited(String userEdited) {
        return queryView("byUserEdited", userEdited).stream().collect(Collectors.toList());
    }

    @View(name = "byTimestamp", map = BY_TIMESTAMP)
    public List<ChangeLogs> getChangeLogsByTimestamp(String timestamp) {
        return queryView("byTimestamp", timestamp).stream().collect(Collectors.toList());
    }
}

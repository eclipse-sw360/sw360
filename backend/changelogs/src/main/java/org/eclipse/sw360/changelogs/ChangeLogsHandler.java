/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.changelogs;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.db.ChangeLogsDatabaseHandler;
import org.eclipse.sw360.datahandler.services.changelogs.ChangeLogs;
import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.eclipse.sw360.datahandler.services.users.User;
import org.springframework.stereotype.Service;

import com.ibm.cloud.cloudant.v1.Cloudant;

@Service
public class ChangeLogsHandler {

    private final ChangeLogsDatabaseHandler handler;

    ChangeLogsHandler() throws IOException {
        this(DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_CHANGE_LOGS);
    }

    ChangeLogsHandler(Cloudant client, String dbName) throws IOException {
        handler = new ChangeLogsDatabaseHandler(client, dbName);
    }

    public List<ChangeLogs> getChangeLogsByDocumentId(User user, String docId) {
        assertNotEmpty(docId, "document id");
        assertUser(user);
        List<ChangeLogs> result = handler.getChangeLogsByDocumentId(user, docId);
        return result == null ? Collections.emptyList() : result;
    }

    public ChangeLogs getChangeLogsById(String id) {
        assertNotEmpty(id, "change log id");
        return handler.getChangeLogsById(id);
    }

    public Map<PaginationData, List<ChangeLogs>> getChangeLogsByDocumentIdPaginated(User user, String docId,
            PaginationData pageData) {
        assertNotEmpty(docId, "document id");
        assertUser(user);
        Map<PaginationData, List<ChangeLogs>> result =
                handler.getChangeLogsByDocumentIdPaginated(user, docId, pageData);
        return result == null ? Collections.emptyMap() : result;
    }

    public RequestStatus deleteChangeLogsByDocumentId(String docId, User user) {
        assertNotEmpty(docId, "document id");
        assertUser(user);
        return handler.deleteChangeLogsByDocumentId(docId, user);
    }

    private static void assertNotEmpty(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new SW360Exception(name + " must not be empty");
        }
    }

    private static void assertUser(User user) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            throw new SW360Exception("user email must not be empty");
        }
    }
}

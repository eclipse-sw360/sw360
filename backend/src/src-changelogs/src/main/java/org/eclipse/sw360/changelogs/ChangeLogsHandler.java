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

import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotEmpty;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotNull;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertUser;

import java.io.IOException;
import java.util.List;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.db.ChangeLogsDatabaseHandler;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.changelogs.ChangeLogs;
import org.eclipse.sw360.datahandler.thrift.changelogs.ChangeLogsService;
import org.eclipse.sw360.datahandler.thrift.users.User;

/**
 * Implementation of the Thrift service
 *
 * @author jaideep.palit@siemens.com
 */
public class ChangeLogsHandler implements ChangeLogsService.Iface {

    private final ChangeLogsDatabaseHandler handler;

    ChangeLogsHandler() throws IOException {
        handler = new ChangeLogsDatabaseHandler(DatabaseSettings.getConfiguredHttpClient(),
                DatabaseSettings.COUCH_DB_CHANGE_LOGS);
    }

    @Override
    public List<ChangeLogs> getChangeLogsByDocumentId(User user, String docId) throws SW360Exception {
        assertNotEmpty(docId);
        assertUser(user);
        return handler.getChangeLogsByDocumentId(user, docId);
    }

    @Override
    public ChangeLogs getChangeLogsById(String id) throws SW360Exception {
        assertNotEmpty(id);
        return handler.getChangeLogsById(id);
    }
}
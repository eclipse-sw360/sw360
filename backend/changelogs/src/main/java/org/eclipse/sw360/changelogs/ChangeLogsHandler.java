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
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertUser;

import java.util.List;

import org.eclipse.sw360.datahandler.db.ChangeLogsDatabaseHandler;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.changelogs.ChangeLogs;
import org.eclipse.sw360.datahandler.thrift.changelogs.ChangeLogsService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementation of the Thrift service
 *
 * @author jaideep.palit@siemens.com
 */
@Component
public class ChangeLogsHandler implements ChangeLogsService.Iface {

    @Autowired
    private ChangeLogsDatabaseHandler handler;

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

    @Override
    public RequestStatus deleteChangeLogsByDocumentId(String docId, User user) throws SW360Exception {
        assertNotEmpty(docId);
        assertUser(user);
        return handler.deleteChangeLogsByDocumentId(docId, user);
    }
}

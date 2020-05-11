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

import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotNull;

import java.net.MalformedURLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.changelogs.ChangeLogs;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.ektorp.http.HttpClient;

/**
 * Class for accessing the CouchDB database for Change logs objects
 *
 * @author: jaideep.palit@siemens.com
 */
public class ChangeLogsDatabaseHandler {
    private final DatabaseConnector db;
    private final ChangeLogsRepository changeLogsRepository;

    private static final Logger log = LogManager.getLogger(ChangeLogsDatabaseHandler.class);

    public ChangeLogsDatabaseHandler(Supplier<HttpClient> httpClient, String dbName) throws MalformedURLException {
        db = new DatabaseConnector(httpClient, dbName);
        changeLogsRepository = new ChangeLogsRepository(db);
    }

    public List<ChangeLogs> getChangeLogsByDocumentId(User user, String docId) {
        List<ChangeLogs> changeLogsByDocId = changeLogsRepository.getChangeLogsByDocId(docId);
        changeLogsByDocId.addAll(changeLogsRepository.getChangeLogsByParentDocId(docId));

        Collections.sort(changeLogsByDocId, Comparator.comparing(ChangeLogs::getChangeTimestamp).reversed());
        changeLogsByDocId.stream().forEach(cl -> cl.setChangeTimestamp(cl.getChangeTimestamp().split(" ")[0]));
        return changeLogsByDocId;
    }

    public ChangeLogs getChangeLogsById(String id) throws SW360Exception {
        ChangeLogs changeLogs = changeLogsRepository.get(id);
        assertNotNull(changeLogs);
        changeLogs.setChangeTimestamp(changeLogs.changeTimestamp.split(" ")[0]);
        return changeLogs;
    }
}

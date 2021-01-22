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
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.changelogs.ChangeLogs;
import org.eclipse.sw360.datahandler.thrift.changelogs.ChangedFields;
import org.eclipse.sw360.datahandler.thrift.changelogs.Operation;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.ektorp.http.HttpClient;

import com.google.common.collect.ImmutableSet;

/**
 * Class for accessing the CouchDB database for Change logs objects
 *
 * @author: jaideep.palit@siemens.com
 */
public class ChangeLogsDatabaseHandler {
    private final DatabaseConnector db;
    private final ChangeLogsRepository changeLogsRepository;
    private static final ImmutableSet<String> setOfIgnoredFieldValues = ImmutableSet.<String>builder()
            .add("\"\"")
            .add("[]")
            .add("{}").build();
    private static final ImmutableSet<String> setOfIgnoredFieldNames = ImmutableSet.<String>builder()
            .add("revision")
            .add("documentState").build();

    public ChangeLogsDatabaseHandler(Supplier<HttpClient> httpClient, String dbName) throws MalformedURLException {
        db = new DatabaseConnector(httpClient, dbName);
        changeLogsRepository = new ChangeLogsRepository(db);
    }

    public List<ChangeLogs> getChangeLogsByDocumentId(User user, String docId) {
        List<ChangeLogs> changeLogsByDocId = changeLogsRepository.getChangeLogsByDocId(docId);
        changeLogsByDocId.addAll(changeLogsRepository.getChangeLogsByParentDocId(docId));
        changeLogsByDocId = changeLogsByDocId.stream().filter(Objects::nonNull).filter(changeLog -> isNotEmptyChangeLog(changeLog))
                .collect(Collectors.toList());
        Collections.sort(changeLogsByDocId, Comparator.comparing(ChangeLogs::getChangeTimestamp).reversed());
        changeLogsByDocId.stream().forEach(cl -> cl.setChangeTimestamp(cl.getChangeTimestamp().split(" ")[0]));
        return changeLogsByDocId;
    }

    public ChangeLogs getChangeLogsById(String id) throws SW360Exception {
        ChangeLogs changeLogs = changeLogsRepository.get(id);
        assertNotNull(changeLogs);
        removeNullToEmtpyChanges(changeLogs);
        changeLogs.setChangeTimestamp(changeLogs.changeTimestamp.split(" ")[0]);
        return changeLogs;
    }

    private ChangeLogs removeNullToEmtpyChanges(ChangeLogs changeLog) {
        Set<ChangedFields> changes = changeLog.getChanges();
        if (CommonUtils.isNotEmpty(changes)) {
            Set<ChangedFields> collectFiltered = changes.stream().filter(ch -> {
                String fieldName = ch.getFieldName();
                String oldFieldValue = ch.getFieldValueOld();
                String newFieldValue = ch.getFieldValueNew();
                if ((fieldName != null && setOfIgnoredFieldNames.contains(fieldName))
                        || (oldFieldValue == null && newFieldValue == null)
                        || (oldFieldValue == null && setOfIgnoredFieldValues.contains(newFieldValue))
                        || (newFieldValue == null && setOfIgnoredFieldValues.contains(oldFieldValue))
                        || (oldFieldValue != null && newFieldValue != null && newFieldValue.equals(oldFieldValue))) {
                    return false;
                }
                return true;
            }).collect(Collectors.toSet());
            changeLog.setChanges(collectFiltered);
        }

        return changeLog;
    }

    private boolean isNotEmptyChangeLog(ChangeLogs changeLog) {
        return changeLog.getOperation() == Operation.CREATE
                || CommonUtils.isNotEmpty(removeNullToEmtpyChanges(changeLog).getChanges());
    }
}

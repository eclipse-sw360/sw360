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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.changelogs.ChangeLogs;
import org.eclipse.sw360.datahandler.thrift.changelogs.ChangedFields;
import org.eclipse.sw360.datahandler.thrift.changelogs.Operation;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;

import com.google.common.collect.ImmutableSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Class for accessing the CouchDB database for Change logs objects
 *
 * @author: jaideep.palit@siemens.com
 */
@Component
public class ChangeLogsDatabaseHandler {
    @Autowired
    private ChangeLogsRepository changeLogsRepository;
    private static final ImmutableSet<String> setOfIgnoredFieldValues = ImmutableSet.<String>builder()
            .add("\"\"")
            .add("[]")
            .add("{}").build();
    private static final ImmutableSet<String> setOfIgnoredFieldNames = ImmutableSet.<String>builder()
            .add("revision")
            .add("documentState").build();

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

    public RequestStatus deleteChangeLogsByDocumentId(String docId, User user) {
        try {
            for (ChangeLogs changeLog: getChangeLogsByDocumentId(user, docId)) {
                changeLogsRepository.remove(changeLog);
            }
            return RequestStatus.SUCCESS;
        } catch (Exception e) {
            return RequestStatus.FAILURE;
        }
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

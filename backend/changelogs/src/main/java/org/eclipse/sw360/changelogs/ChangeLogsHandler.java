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

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.sw360.common.converter.ThriftConverter;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.db.ChangeLogsDatabaseHandler;
import org.eclipse.sw360.datahandler.services.changelogs.ChangeLogs;
import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.users.User;
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

    public List<ChangeLogs> getChangeLogsByDocumentId(User user, String docId) throws SW360Exception {
        try{
            assertNotEmpty(docId);
            assertUser(user);

            List<org.eclipse.sw360.datahandler.thrift.changelogs.ChangeLogs> thriftList = handler.getChangeLogsByDocumentId(user, docId);
        
            if (thriftList == null || thriftList.isEmpty()) {
                return Collections.emptyList();
            }
            return thriftList.stream()
            .map(ThriftConverter::fromThriftChangeLogs)
            .collect(Collectors.toList());

        } catch(org.eclipse.sw360.datahandler.thrift.SW360Exception ex){
            throw ThriftConverter.fromThriftException(ex);
        }
        
    }

    public ChangeLogs getChangeLogsById(String id) throws SW360Exception {
        try{
            assertNotEmpty(id);
            return ThriftConverter.fromThriftChangeLogs( handler.getChangeLogsById(id));

        } catch(org.eclipse.sw360.datahandler.thrift.SW360Exception ex){
            throw ThriftConverter.fromThriftException(ex);
        }
    }

    public Map<PaginationData, List<ChangeLogs>> getChangeLogsByDocumentIdPaginated(User user, String docId, PaginationData pageData) throws SW360Exception{ 
        try{
            assertNotEmpty(docId);
            assertUser(user);
            org.eclipse.sw360.datahandler.thrift.PaginationData thriftPageData = ThriftConverter.toThriftPaginationData(pageData);
            Map<org.eclipse.sw360.datahandler.thrift.PaginationData,List<org.eclipse.sw360.datahandler.thrift.changelogs.ChangeLogs>> thriftMap = handler.getChangeLogsByDocumentIdPaginated(user, docId, thriftPageData);
            if (thriftMap == null || thriftMap.isEmpty()) {
                return Collections.emptyMap();
            }
            return thriftMap.entrySet()
            .stream()
            .collect(Collectors.toMap(
                entry -> ThriftConverter.fromThriftPaginationData(entry.getKey()),
                entry -> entry.getValue() == null? Collections.emptyList():
                            entry.getValue()
                        .stream()
                        .map(ThriftConverter::fromThriftChangeLogs)
                        .collect(Collectors.toList())
                    ));

        } catch(org.eclipse.sw360.datahandler.thrift.SW360Exception ex){
            throw ThriftConverter.fromThriftException(ex);
        }
    }

    public RequestStatus deleteChangeLogsByDocumentId(String docId, User user) throws SW360Exception {
        try{
            assertNotEmpty(docId);
            assertUser(user);
            return ThriftConverter.fromThriftRequestStatus( handler.deleteChangeLogsByDocumentId(docId, user));
        } catch(org.eclipse.sw360.datahandler.thrift.SW360Exception ex){
            throw ThriftConverter.fromThriftException(ex);
        }
        
    }
}

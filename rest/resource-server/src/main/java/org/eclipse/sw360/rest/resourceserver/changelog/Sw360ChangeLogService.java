/*
 * Copyright Siemens AG, 2020. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.changelog;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.sw360.datahandler.thrift.changelogs.ChangeLogs;
import org.eclipse.sw360.datahandler.thrift.changelogs.ChangeLogsService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class Sw360ChangeLogService {
    private static final Logger log = LogManager.getLogger(Sw360ChangeLogService.class);

    @Value("${sw360.thrift-server-url:http://localhost:8080}")
    private String thriftServerUrl;

    private ChangeLogsService.Iface getThriftChangeLogClient() throws TTransportException {
        THttpClient thriftClient = new THttpClient(thriftServerUrl + "/changelogs/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        return new ChangeLogsService.Client(protocol);
    }

    public List<ChangeLogs> getChangeLogsByDocumentId(String docId, User sw360User) throws TException {
        return getThriftChangeLogClient().getChangeLogsByDocumentId(sw360User, docId);
    }
}

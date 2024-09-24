/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.moderation;

import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;

import java.io.IOException;
import java.util.List;

/**
 * Small client for testing a service
 * author: Gerrit.Grenzebach@tngtech.com
 */
public class TestModerationClient {

    public static void main(String[] args) throws TException, IOException {
        THttpClient thriftClient = new THttpClient("http://127.0.0.1:8080//moderation/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        ModerationService.Iface client = new ModerationService.Client(protocol);

        List<ModerationRequest> requestsByModerator = client.getRequestsByModerator(new User().setId("58245y9845").setEmail("cedric.bodet@tngtech.com").setDepartment("BB"));


        System.out.println("Fetched " + requestsByModerator.size() + " moderation requests from moderation service");


    }

}

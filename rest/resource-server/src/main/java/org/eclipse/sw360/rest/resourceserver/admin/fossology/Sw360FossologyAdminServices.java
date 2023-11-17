/*
 * Copyright Siemens AG, 2023-2024.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.admin.fossology;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.ConfigContainer;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.fossology.FossologyService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class Sw360FossologyAdminServices {

    @Value("${sw360.thrift-server-url:http://localhost:8080}")
    private String thriftServerUrl;

    private static FossologyService.Iface fossologyClient;
    public static Sw360FossologyAdminServices instance;
    private boolean fossologyConnectionEnabled;

    public void saveConfig(User sw360User, String url, String folderId, String token) throws TException {
        FossologyService.Iface client = getThriftFossologyClient();
        ConfigContainer fossologyConfig = client.getFossologyConfig();

        if (PermissionUtils.isUserAtLeast(UserGroup.ADMIN, sw360User)) {
            Map<String, Set<String>> configKeyToValues = new HashMap<>();
            setConfigValues(configKeyToValues, "url", url);
            setConfigValues(configKeyToValues, "folderId", folderId);
            setConfigValues(configKeyToValues, "token", token);
            fossologyConfig.setConfigKeyToValues(configKeyToValues);
            if (client != null && fossologyConfig != null) {
                client.setFossologyConfig(fossologyConfig);
            } else {
                throw new HttpMessageNotReadableException("fossologyConfig value is null.");
            }
        } else {
            throw new HttpMessageNotReadableException("Unable to save the details. User is not admin");
        }
    }

    public FossologyService.Iface getThriftFossologyClient() throws TTransportException {
        if (fossologyClient == null) {
            if (thriftServerUrl == null || thriftServerUrl.isEmpty()) {
                throw new TTransportException("Invalid thriftServerUrl");
            }

            THttpClient thriftClient = new THttpClient(thriftServerUrl + "/fossology/thrift");
            TProtocol protocol = new TCompactProtocol(thriftClient);
            fossologyClient = new FossologyService.Client(protocol);
        }

        return fossologyClient;
    }

    private static void setConfigValues(Map<String, Set<String>> configKeyToValues, String key, String value) {
        configKeyToValues.putIfAbsent(key, new HashSet<>());
        Set<String> values = configKeyToValues.get(key);
        values.add(value);
    }

}

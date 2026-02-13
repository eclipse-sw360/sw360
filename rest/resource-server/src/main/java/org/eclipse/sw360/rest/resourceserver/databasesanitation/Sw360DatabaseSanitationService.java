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

package org.eclipse.sw360.rest.resourceserver.databasesanitation;

import static org.eclipse.sw360.datahandler.common.CommonUtils.allAreEmptyOrNull;
import static org.eclipse.sw360.datahandler.common.CommonUtils.oneIsNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class Sw360DatabaseSanitationService {

    @Value("${sw360.thrift-server-url:http://localhost:8080}")
    private String thriftServerUrl;

    public Map<String, Map<String, List<String>>> duplicateIdentifiers(User sw360User) throws TException, SW360Exception {
        try {
            if (!PermissionUtils.isUserAtLeast(UserGroup.ADMIN, sw360User)) {
                throw new AccessDeniedException("Access Denied");
            }

            Map<String, Map<String, List<String>>> responseMap = new HashMap<>();
            ComponentService.Iface componentClient = getThriftComponentClient();
            ProjectService.Iface projectClient = getThriftProjectClient();
            Map<String, List<String>> duplicateComponents = componentClient.getDuplicateComponents();
            Map<String, List<String>> duplicateReleases = componentClient.getDuplicateReleases();
            Map<String, List<String>> duplicateReleaseSources = componentClient.getDuplicateReleaseSources();
            Map<String, List<String>> duplicateProjects = projectClient.getDuplicateProjects();

            if (oneIsNull(duplicateComponents, duplicateReleases, duplicateProjects, duplicateReleaseSources)) {
                throw new SW360Exception();
            } else if (allAreEmptyOrNull(duplicateComponents, duplicateReleases, duplicateProjects,
                    duplicateReleaseSources)) {
                return responseMap;
            } else {
                responseMap.put("duplicateReleases", duplicateReleases);
                responseMap.put("duplicateReleaseSources", duplicateReleaseSources);
                responseMap.put("duplicateComponents", duplicateComponents);
                responseMap.put("duplicateProjects", duplicateProjects);
            }
            return responseMap;
        } catch (SW360Exception sw360Exp) {
            if (sw360Exp.getErrorCode() == 403) {
                throw new AccessDeniedException("User has not admin access");
            } else if (sw360Exp.getErrorCode() == 204) {
                throw sw360Exp;
            } else {
                log.error("No duplicate ids found: {}", sw360Exp.getMessage());
            }
            throw sw360Exp;
        }
    }

    public ComponentService.Iface getThriftComponentClient() throws TTransportException {
        THttpClient thriftClient = new THttpClient(thriftServerUrl + "/components/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        return new ComponentService.Client(protocol);
    }

    public ProjectService.Iface getThriftProjectClient() throws TTransportException {
        THttpClient thriftClient = new THttpClient(thriftServerUrl + "/projects/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        return new ProjectService.Client(protocol);
    }

}

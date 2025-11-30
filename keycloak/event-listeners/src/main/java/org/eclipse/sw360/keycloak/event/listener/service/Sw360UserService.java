/*
 * Copyright Siemens AG, 2024. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.keycloak.event.listener.service;

import java.util.List;

import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestStatus;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.datahandler.thrift.users.UserService;
import org.jboss.logging.Logger;

public class Sw360UserService {
    public static String thriftServerUrl = "http://localhost:8080";
    private static final Logger logger = Logger.getLogger(Sw360UserService.class);

    public List<User> getAllUsers() {
        try {
            UserService.Iface sw360UserClient = getThriftUserClient();
            return sw360UserClient.getAllUsers();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public User getUserByEmail(String email) {
        try {
            UserService.Iface sw360UserClient = getThriftUserClient();
            return sw360UserClient.getByEmail(email);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public User getUserByEmailOrExternalId(String userIdentifier) {
        try {
            UserService.Iface sw360UserClient = getThriftUserClient();
            return sw360UserClient.getByEmailOrExternalId(userIdentifier, userIdentifier);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public User getUser(String id) {
        try {
            UserService.Iface sw360UserClient = getThriftUserClient();
            return sw360UserClient.getUser(id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public User getUserByApiToken(String token) {
        try {
            UserService.Iface sw360UserClient = getThriftUserClient();
            return sw360UserClient.getByApiToken(token);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public User getUserFromClientId(String clientId) {
        try {
            UserService.Iface sw360UserClient = getThriftUserClient();
            return sw360UserClient.getByOidcClientId(clientId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public User addUser(User user) {
        try {
            UserService.Iface sw360UserClient = getThriftUserClient();
            if(user.getUserGroup() == null) {
                user.setUserGroup(UserGroup.USER);
            }
            AddDocumentRequestSummary documentRequestSummary = sw360UserClient.addUser(user);
            logger.info("Sw360UserService::addUser()::documentSummarry-->" + documentRequestSummary);
            if (documentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.SUCCESS) {
                user.setId(documentRequestSummary.getId());
                return user;
            } else if (documentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.DUPLICATE) {
                logger.warn("Duplicate User");
            } else if (documentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.INVALID_INPUT) {
                logger.warn("Invalid Input/Request");
            }
        } catch (Exception e) {
            logger.error("Error Creating the user in sw360 database");
            return null;
        }
        return null;
    }

    public RequestStatus updateUser(User user) throws Exception {
        UserService.Iface sw360UserClient = getThriftUserClient();
        RequestStatus requestStatus = sw360UserClient.updateUser(user);
        return requestStatus;
    }

    private UserService.Iface getThriftUserClient() throws Exception {
        THttpClient thriftClient = new THttpClient(thriftServerUrl + "/users/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        return new UserService.Client(protocol);
    }
}

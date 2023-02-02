/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.user;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestStatus;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.datahandler.thrift.users.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class Sw360UserService {
    @Value("${sw360.thrift-server-url:http://localhost:8080}")
    private String thriftServerUrl;

    public List<User> getAllUsers() {
        try {
            UserService.Iface sw360UserClient = getThriftUserClient();
            return sw360UserClient.getAllUsers();
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }

    public User getUserByEmail(String email) {
        try {
            UserService.Iface sw360UserClient = getThriftUserClient();
            return sw360UserClient.getByEmail(email);
        } catch (TException e) {
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
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }

    public User getUserByApiToken(String token) {
        try {
            UserService.Iface sw360UserClient = getThriftUserClient();
            return sw360UserClient.getByApiToken(token);
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }

    public User getUserFromClientId(String clientId) {
        try {
            UserService.Iface sw360UserClient = getThriftUserClient();
            return sw360UserClient.getByOidcClientId(clientId);
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }

    public User addUser(User user) throws TException{
        try {
            UserService.Iface sw360UserClient = getThriftUserClient();
            user.setUserGroup(UserGroup.USER);
            AddDocumentRequestSummary documentRequestSummary = sw360UserClient.addUser(user);
            if (documentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.SUCCESS) {
                user.setId(documentRequestSummary.getId());
                return user;
            } else if (documentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.DUPLICATE) {
                throw new DataIntegrityViolationException("sw360 user with name '" + user.getEmail()
                        + "' already exists, having database identifier " + documentRequestSummary.getId());
            } else if (documentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.INVALID_INPUT) {
                throw new HttpMessageNotReadableException(documentRequestSummary.getMessage());
            }
        } catch (SW360Exception sw360Exp) {
            throw new HttpMessageNotReadableException(sw360Exp.getMessage());
        } catch (TException e) {
            throw new HttpMessageNotReadableException(e.getMessage());
        }
        return null;
    }

    private UserService.Iface getThriftUserClient() throws TTransportException {
        THttpClient thriftClient = new THttpClient(thriftServerUrl + "/users/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        return new UserService.Client(protocol);
    }
}

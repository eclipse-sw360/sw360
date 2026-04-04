/*
 * SPDX-FileCopyrightText: 2026 Eclipse SW360 Contributors
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.migration.poc.service;

import org.eclipse.sw360.migration.poc.handler.Sw360UserHandler;
import org.eclipse.sw360.migration.poc.model.RequestStatus;
import org.eclipse.sw360.migration.poc.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * REST service layer after migration — mirrors {@code Sw360UserService} in
 * {@code rest/resource-server/src/main/java/.../user/Sw360UserService.java}.
 *
 * <h3>The exact before/after for each call site:</h3>
 * <pre>
 *   // BEFORE — Sw360UserService.java (current production code):
 *
 *   {@literal @}Value("${sw360.thrift-server-url:http://localhost:8080}")
 *   private String thriftServerUrl;
 *
 *   private UserService.Iface getThriftUserClient() throws TTransportException {
 *       THttpClient thriftClient = new THttpClient(thriftServerUrl + "/users/thrift");
 *       TProtocol protocol = new TCompactProtocol(thriftClient);
 *       return new UserService.Client(protocol);
 *   }
 *
 *   public List&lt;User&gt; getAllUsers() {
 *       try {
 *           return getThriftUserClient().getAllUsers();  // new THttpClient per request
 *       } catch (TException e) {
 *           throw new RuntimeException(e);
 *       }
 *   }
 *
 *
 *   // AFTER — this class:
 *
 *   private final Sw360UserHandler userHandler;  // Spring @Autowired singleton
 *
 *   public List&lt;User&gt; getAllUsers() {
 *       return userHandler.getAllUsers();         // direct in-process call
 *   }
 * </pre>
 *
 * <p>The REST controllers ({@code UserController}) call this service layer — they are
 * <b>not changed</b> by the migration. The change is entirely contained here.
 */
@Service
public class DirectInjectionUserService {

    // Replaces:
    //   @Value("${sw360.thrift-server-url:http://localhost:8080}") String thriftServerUrl
    //   + getThriftUserClient() factory method that creates a new THttpClient per call
    private final Sw360UserHandler userHandler;

    @Autowired
    public DirectInjectionUserService(Sw360UserHandler userHandler) {
        this.userHandler = userHandler;
    }

    public List<User> getAllUsers() {
        return userHandler.getAllUsers();
    }

    public User getUser(String id) {
        return userHandler.getUser(id);
    }

    public User getUserByEmail(String email) {
        return userHandler.getByEmail(email);
    }

    public RequestStatus createUser(User user) {
        return userHandler.addUser(user);
    }

    public RequestStatus updateUser(User user) {
        return userHandler.updateUser(user);
    }

    public RequestStatus deleteUser(User user, User adminUser) {
        return userHandler.deleteUser(user, adminUser);
    }
}

/*
 * Copyright Siemens AG, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.users.dto;

import org.eclipse.sw360.datahandler.thrift.users.User;

/**
 * Request body for the {@code DELETE /users} endpoint.
 * Both the user to delete and the acting admin user must be supplied.
 */
public class DeleteUserRequest {

    private User user;
    private User adminUser;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getAdminUser() {
        return adminUser;
    }

    public void setAdminUser(User adminUser) {
        this.adminUser = adminUser;
    }
}

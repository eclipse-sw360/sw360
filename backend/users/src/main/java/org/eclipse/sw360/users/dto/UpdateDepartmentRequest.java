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

import java.util.List;

/**
 * Request body for the department bulk-update endpoints:
 * {@code PUT /users/department} and {@code DELETE /users/secondaryDepartment}.
 */
public class UpdateDepartmentRequest {

    private List<User> users;
    private String department;

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}

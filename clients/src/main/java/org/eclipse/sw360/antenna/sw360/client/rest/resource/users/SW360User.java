/*
 * Copyright (c) Bosch Software Innovations GmbH 2017-2018.
 * Copyright (c) Bosch.IO GmbH 2020.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.antenna.sw360.client.rest.resource.users;

import org.eclipse.sw360.antenna.sw360.client.rest.resource.SW360SimpleHalResource;

import java.util.Objects;

public final class SW360User extends SW360SimpleHalResource {
    private String email;
    private String type;
    private SW360UserGroup userGroup;
    private String department;
    private String fullName;
    private String givenName;
    private String lastName;

    public String getType() {
        return this.type;
    }

    public SW360User setType(String type) {
        this.type = type;
        return this;
    }

    public String getEmail() {
        return this.email;
    }

    public SW360User setEmail(String email) {
        this.email = email;
        return this;
    }

    public SW360UserGroup getUserGroup() {
        return this.userGroup;
    }

    public SW360User setUserGroup(SW360UserGroup userGroup) {
        this.userGroup = userGroup;
        return this;
    }

    public String getDepartment() {
        return this.department;
    }

    public SW360User setDepartment(String department) {
        this.department = department;
        return this;
    }

    public String getFullName() {
        return this.fullName;
    }

    public SW360User setFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    public String getGivenName() {
        return this.givenName;
    }

    public SW360User setGivenName(String givenName) {
        this.givenName = givenName;
        return this;
    }

    public String getLastName() {
        return this.lastName;
    }

    public SW360User setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SW360User) || !super.equals(o)) return false;
        SW360User sw360User = (SW360User) o;
        return Objects.equals(email, sw360User.email) &&
                Objects.equals(type, sw360User.type) &&
                userGroup == sw360User.userGroup &&
                Objects.equals(department, sw360User.department) &&
                Objects.equals(fullName, sw360User.fullName) &&
                Objects.equals(givenName, sw360User.givenName) &&
                Objects.equals(lastName, sw360User.lastName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), email, type, userGroup, department, fullName, givenName, lastName);
    }

    @Override
    public boolean canEqual(Object o) {
        return o instanceof SW360User;
    }
}

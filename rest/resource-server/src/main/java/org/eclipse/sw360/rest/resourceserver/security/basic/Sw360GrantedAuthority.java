/*
SPDX-FileCopyrightText: © 2022 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.rest.resourceserver.security.basic;

import org.springframework.security.core.GrantedAuthority;

public enum Sw360GrantedAuthority implements GrantedAuthority {

    BASIC, READ, WRITE, ADMIN;

    @Override
    public String getAuthority() {
        return toString();
    }
}

/*
 * Copyright Siemens AG, 2017, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.authserver.security;

import org.springframework.security.core.GrantedAuthority;

public enum Sw360GrantedAuthority implements GrantedAuthority {

    /*
    * BASIC:
    *    only authorized with clientName/clientSecret secret (without valid sw360 user credentials)
    */
    BASIC,

    /*
    * READ:
    *    authorized with clientName/clientSecret and valid sw360 user (without rest api write privileges)
    */
    READ,

    /*
    * WRITE
    *    authorized with clientName/clientSecret and valid sw360 user with rest api write privileges
    */
    WRITE,

    /*
     * ADMIN
     *    authorized and valid sw360 user with auth server administration privileges
     */
    ADMIN;

    @Override
    public String getAuthority() {
        return toString();
    }

}

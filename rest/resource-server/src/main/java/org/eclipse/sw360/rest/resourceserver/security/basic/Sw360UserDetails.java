/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.security.basic;

import lombok.Getter;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.springframework.security.core.GrantedAuthority;

import java.io.Serial;
import java.util.Collection;

@Getter
public class Sw360UserDetails extends org.springframework.security.core.userdetails.User {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient User sw360User;

    public Sw360UserDetails(User sw360User, Collection<? extends GrantedAuthority> authorities) {
        super(sw360User.getEmail(), sw360User.getPassword(), authorities);
        this.sw360User = sw360User;
    }

}

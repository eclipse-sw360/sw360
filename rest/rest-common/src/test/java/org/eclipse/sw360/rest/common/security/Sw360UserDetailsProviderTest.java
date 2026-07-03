/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.common.security;

import org.eclipse.sw360.clients.users.UsersClient;
import org.eclipse.sw360.datahandler.services.users.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Sw360UserDetailsProviderTest {

    @Test
    void shouldReturnUserFromClientId_whenRestLookupSucceeds() {
        UsersClient usersClient = mock(UsersClient.class);
        User expected = new User().setEmail("oidc-user@sw360.org").setDepartment("oidc");
        when(usersClient.getByOidcClientId("trusted-client")).thenReturn(expected);

        Sw360UserDetailsProvider provider = new Sw360UserDetailsProvider(usersClient);

        User actual = provider.getUserFromClientId("trusted-client");

        assertThat(actual).isSameAs(expected);
    }

    @Test
    void shouldReturnNullFromClientId_whenRestLookupThrows() {
        UsersClient usersClient = mock(UsersClient.class);
        when(usersClient.getByOidcClientId("trusted-client")).thenThrow(new RuntimeException("boom"));

        Sw360UserDetailsProvider provider = new Sw360UserDetailsProvider(usersClient);

        User actual = provider.getUserFromClientId("trusted-client");

        assertThat(actual).isNull();
    }
}

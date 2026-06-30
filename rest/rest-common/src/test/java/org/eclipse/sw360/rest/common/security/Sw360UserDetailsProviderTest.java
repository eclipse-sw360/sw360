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

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Sw360UserDetailsProviderTest {

    private static Sw360UserDetailsProvider providerWithClient(UserService.Iface thriftClient) {
        return new Sw360UserDetailsProvider() {
            @Override
            public UserService.Iface getUserClient() {
                return thriftClient;
            }
        };
    }

    @Test
    void shouldReturnUserFromClientId_whenThriftLookupSucceeds() throws Exception {
        UserService.Iface thriftClient = mock(UserService.Iface.class);
        User expected = new User("oidc-user@sw360.org", "oidc");
        when(thriftClient.getByOidcClientId("trusted-client")).thenReturn(expected);

        Sw360UserDetailsProvider provider = providerWithClient(thriftClient);

        User actual = provider.getUserFromClientId("trusted-client");

        assertThat(actual).isSameAs(expected);
    }

    @Test
    void shouldReturnNullFromClientId_whenThriftLookupThrows() throws Exception {
        UserService.Iface thriftClient = mock(UserService.Iface.class);
        when(thriftClient.getByOidcClientId("trusted-client")).thenThrow(new TException("boom"));

        Sw360UserDetailsProvider provider = providerWithClient(thriftClient);

        User actual = provider.getUserFromClientId("trusted-client");

        assertThat(actual).isNull();
    }
}

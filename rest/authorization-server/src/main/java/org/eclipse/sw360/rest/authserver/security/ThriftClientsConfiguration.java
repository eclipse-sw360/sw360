/*
SPDX-FileCopyrightText: © 2026 Contributors to the SW360 Portal Project
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.rest.authserver.security;

import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Defines the ThriftClients in its own configuration so
 * {@link SecurityConfig} can inject the authentication provider without a circular
 * dependency: the provider needs a {@code ThriftClients} bean that must not be
 * declared on the same configuration class that injects the provider.
 */
@Configuration
public class ThriftClientsConfiguration {

    @Bean
    public ThriftClients thriftClients() {
        return new ThriftClients();
    }
}

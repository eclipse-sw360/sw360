/*
SPDX-FileCopyrightText: © 2026 Contributors to the SW360 Portal Project
SPDX-License-Identifier: EPL-2.0

*/
package org.eclipse.sw360.rest.authserver.security;

import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Defines the Thrift client factory in its own configuration so services such as
 * {@link Sw360UserDetailsProvider} can depend on {@link ThriftClients} without a circular
 * dependency: {@link SecurityConfig} must not declare this bean while also injecting the
 * authentication provider chain that ultimately needs {@link ThriftClients}.
 */
@Configuration
public class ThriftClientsConfiguration {

    @Bean
    public ThriftClients thriftClients() {
        return new ThriftClients();
    }
}

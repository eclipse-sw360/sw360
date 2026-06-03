/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.authserver.config;

import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Hosts the shared {@link ThriftClients} bean in a configuration class that
 * has <em>no other dependencies</em>.
 */
@Configuration
public class ThriftClientsConfig {

    @Bean
    public ThriftClients thriftClients() {
        return new ThriftClients();
    }
}

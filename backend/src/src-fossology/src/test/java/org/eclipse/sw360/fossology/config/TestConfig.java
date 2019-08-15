/*
 * Copyright Siemens AG, 2015, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.fossology.config;

import org.eclipse.sw360.datahandler.thrift.ThriftClients;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import static org.mockito.Mockito.mock;

@Configuration
@ComponentScan({"org.eclipse.sw360.fossology"})
public class TestConfig {

    @Bean
    public ThriftClients thriftClients() {
        return mock(ThriftClients.class);
    }

}

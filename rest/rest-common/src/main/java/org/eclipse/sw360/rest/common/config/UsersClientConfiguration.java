/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.common.config;

import org.eclipse.sw360.datahandler.rest.BackendRestClients;
import org.eclipse.sw360.datahandler.users.UsersClient;
import org.eclipse.sw360.datahandler.users.UsersServiceRestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring bean for {@link UsersClient} shared by authorization-server and resource-server.
 * Uses the same pooled {@link BackendRestClients#shared()} as static {@code UsersClients.get()}.
 */
@Configuration
public class UsersClientConfiguration {

    @Bean
    public UsersClient usersClient() {
        return new UsersServiceRestClient(BackendRestClients.shared());
    }
}

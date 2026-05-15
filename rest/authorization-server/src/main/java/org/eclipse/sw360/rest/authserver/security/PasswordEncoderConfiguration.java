/*
SPDX-FileCopyrightText: © 2026 Contributors to the SW360 Portal Project
SPDX-License-Identifier: EPL-2.0

*/
package org.eclipse.sw360.rest.authserver.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Defines the application password encoder in its own configuration so
 * {@link SecurityConfig} can inject the authentication provider without a circular
 * dependency: the provider needs a {@code PasswordEncoder} bean that must not be
 * declared on the same configuration class that injects the provider.
 *
 * <p>The encoder is a {@link Sw360ClientSecretEncoder} which produces BCrypt
 * hashes for new secrets but also accepts legacy raw-UUID client secrets stored
 * by the older Liferay-based SW360 portal in the same {@code sw360oauthclients}
 * CouchDB. Legacy values are upgraded to BCrypt synchronously on first
 * successful authentication by {@link LegacyClientSecretUpgrader}.</p>
 */
@Configuration
public class PasswordEncoderConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Sw360ClientSecretEncoder();
    }
}

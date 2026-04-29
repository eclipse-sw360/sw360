/*
SPDX-FileCopyrightText: © 2026 Contributors to the SW360 Portal Project
SPDX-License-Identifier: EPL-2.0

*/
package org.eclipse.sw360.rest.authserver.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Defines the application password encoder in its own configuration so
 * {@link SecurityConfig} can inject the authentication provider without a circular
 * dependency: the provider needs a {@code PasswordEncoder} bean that must not be
 * declared on the same configuration class that injects the provider.
 */
@Configuration
public class PasswordEncoderConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

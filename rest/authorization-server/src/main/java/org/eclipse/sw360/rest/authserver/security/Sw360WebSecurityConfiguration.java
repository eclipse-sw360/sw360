/*
 * Copyright Siemens AG, 2017, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.authserver.security;

import org.eclipse.sw360.rest.authserver.security.basicauth.Sw360LiferayAuthenticationProvider;
import org.eclipse.sw360.rest.authserver.security.customheaderauth.Sw360CustomHeaderAuthenticationProvider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.AuthenticationManagerConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * This class configures the standard spring security for this server. Only
 * exception is that the {@link AuthenticationManager} is shared with the oauth2
 * security and this one is configured with the oauth2
 * {@link AuthenticationProvider}s from
 * {@link Sw360AuthorizationServerConfiguration}.
 */
@Configuration
@EnableWebSecurity
public class Sw360WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Autowired
    private Sw360LiferayAuthenticationProvider sw360LiferayAuthenticationProvider;

    @Autowired
    private Sw360CustomHeaderAuthenticationProvider sw360CustomHeaderAuthenticationProvider;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                .antMatchers(HttpMethod.OPTIONS)
                    .permitAll() // some JS frameworks make HTTP OPTIONS requests
                .anyRequest()
                    .authenticated()
                .and()
                    .httpBasic()
                .and()
                    .csrf().disable();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder authenticationManagerBuilder) {
        authenticationManagerBuilder
                .authenticationProvider(sw360LiferayAuthenticationProvider)
                .authenticationProvider(sw360CustomHeaderAuthenticationProvider);
    }

    /**
     * We have to publish our configured authentication manager because otherwise
     * some boot default manager will be populated from
     * {@link AuthenticationManagerConfiguration}.
     */
    @Bean(name = "authenticationManager")
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

}

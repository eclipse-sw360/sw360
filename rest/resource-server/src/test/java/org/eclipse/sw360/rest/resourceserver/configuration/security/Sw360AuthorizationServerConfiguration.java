/*
 * Copyright Siemens AG, 2017,2024. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.configuration.security;

import lombok.RequiredArgsConstructor;
import org.eclipse.sw360.rest.resourceserver.core.SimpleAuthenticationEntryPoint;
import org.eclipse.sw360.rest.resourceserver.security.basic.Sw360UserAuthenticationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Profile("SECURITY_MOCK")
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class Sw360AuthorizationServerConfiguration {

	private final Sw360UserAuthenticationProvider sw360UserAuthenticationProvider;

	@Order(1)
	@Bean
	public SecurityFilterChain appSecurity(HttpSecurity httpSecurity) throws Exception {
		SimpleAuthenticationEntryPoint saep = new SimpleAuthenticationEntryPoint();
		httpSecurity.authorizeHttpRequests(
				authz -> authz
						.requestMatchers(HttpMethod.GET, "/api/health").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/version").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/info").permitAll()
						.anyRequest().authenticated()
		).authenticationProvider(sw360UserAuthenticationProvider)
				.httpBasic(Customizer.withDefaults()).formLogin(Customizer.withDefaults())
				.exceptionHandling(x -> x.authenticationEntryPoint(saep));
		return httpSecurity.csrf(csrf -> csrf.disable()).build();
	}

}

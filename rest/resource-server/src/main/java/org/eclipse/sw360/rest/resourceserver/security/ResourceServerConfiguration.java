/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.resourceserver.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.http.HttpServletRequest;

@Configuration
@EnableResourceServer
@EnableGlobalMethodSecurity(prePostEnabled = true, proxyTargetClass = true)
public class ResourceServerConfiguration extends WebSecurityConfigurerAdapter implements ResourceServerConfigurer {

    @Value("${security.oauth2.resource.id:sw360-REST-API}")
    private String resourceId;

    @Override
    public void configure(ResourceServerSecurityConfigurer resources) {
        resources.resourceId(resourceId);
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/", "/**/*.html", "/**/*.css", "/**/*.js", "/**/*.json", "/**/*.png", "/**/*.gif");
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        // TODO Thomas Maier 15-12-2017
        // Use Sw360GrantedAuthority from authorization server
        http
                .httpBasic().and()
                .authorizeRequests()
                .antMatchers(HttpMethod.GET, "/api").permitAll()
                .antMatchers(HttpMethod.GET, "/api/**").hasAuthority("READ")
                .antMatchers(HttpMethod.POST, "/api/**").hasAuthority("WRITE")
                .antMatchers(HttpMethod.PUT, "/api/**").hasAuthority("WRITE")
                .antMatchers(HttpMethod.PATCH, "/api/**").hasAuthority("WRITE").and()
                .csrf().disable();
    }

    private static class OAuthRequestedMatcher implements RequestMatcher {
        public boolean matches(HttpServletRequest httpServletRequest) {
            String authorizationHeaderValue = httpServletRequest.getHeader("Authorization");
            return (authorizationHeaderValue != null) && authorizationHeaderValue.startsWith("Bearer");
        }
    }
}

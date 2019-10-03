/*
 * Copyright Siemens AG, 2017-2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.rest.authserver.security;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.rest.authserver.client.service.Sw360ClientDetailsService;
import org.eclipse.sw360.rest.authserver.security.customheaderauth.Sw360CustomHeaderAuthenticationFilter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory;

import static org.eclipse.sw360.rest.authserver.security.Sw360GrantedAuthority.BASIC;

import java.io.File;

/**
 * This class configures the oauth2 authorization server specialties for the
 * authorization parts of this server.
 */
@Configuration
@EnableAuthorizationServer
public class Sw360AuthorizationServerConfiguration extends AuthorizationServerConfigurerAdapter {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private Sw360CustomHeaderAuthenticationFilter sw360CustomHeaderAuthenticationFilter;

    @Autowired
    private Sw360UserDetailsProvider sw360UserDetailsProvider;

    @Value("${jwt.secretkey:sw360SecretKey}")
    private String secretKey;

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        endpoints
                .allowedTokenEndpointRequestMethods(HttpMethod.GET, HttpMethod.POST)
                .tokenStore(tokenStore())
                .tokenEnhancer(jwtAccessTokenConverter())
                .authenticationManager(authenticationManager)
                .userDetailsService(userDetailsService());
    }

    @Override
    public void configure(AuthorizationServerSecurityConfigurer oauthServer) throws Exception {
        String serverAuthority = BASIC.getAuthority();
        oauthServer.tokenKeyAccess("isAnonymous() || hasAuthority('" + serverAuthority + "')")
                .checkTokenAccess("hasAuthority('" + serverAuthority + "')")
                .addTokenEndpointAuthenticationFilter(sw360CustomHeaderAuthenticationFilter);
    }

    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        clients.withClientDetails(sw360ClientDetailsService());
    }

    @Bean
    public Sw360ClientDetailsService sw360ClientDetailsService() {
        return new Sw360ClientDetailsService();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new Sw360UserDetailsService(sw360UserDetailsProvider, sw360ClientDetailsService(),
                sw360UserAndClientAuthoritiesCalculator());
    }

    @Bean
    public Sw360GrantedAuthoritiesCalculator sw360UserAndClientAuthoritiesCalculator() {
        return new Sw360GrantedAuthoritiesCalculator();
    }

    @Bean
    public TokenStore tokenStore() {
        return new JwtTokenStore(jwtAccessTokenConverter());
    }

    @Bean
    protected JwtAccessTokenConverter jwtAccessTokenConverter() {
        String keystore = "/jwt-keystore.jks";
        Resource resource = new FileSystemResource(new File(CommonUtils.SYSTEM_CONFIGURATION_PATH + keystore));
        if (!resource.exists()) {
            resource = new ClassPathResource(keystore);
        }
        KeyStoreKeyFactory keyStoreKeyFactory =
                new KeyStoreKeyFactory(resource, secretKey.toCharArray());
        JwtAccessTokenConverter jwtAccessTokenConverter = new JwtAccessTokenConverter();
        jwtAccessTokenConverter.setKeyPair(keyStoreKeyFactory.getKeyPair("jwt"));
        return jwtAccessTokenConverter;
    }

}

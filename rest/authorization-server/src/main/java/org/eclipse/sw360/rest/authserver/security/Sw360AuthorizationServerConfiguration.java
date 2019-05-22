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

import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.rest.authserver.security.basicauth.Sw360LiferayAuthenticationProvider;
import org.eclipse.sw360.rest.authserver.security.customheaderauth.Sw360CustomHeaderAuthenticationFilter;
import org.eclipse.sw360.rest.authserver.security.customheaderauth.Sw360CustomHeaderAuthenticationProvider;
import org.eclipse.sw360.rest.authserver.security.customheaderauth.Sw360CustomHeaderUserDetailsProvider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory;

import javax.annotation.PostConstruct;
import javax.servlet.Filter;

import java.io.IOException;

import static org.eclipse.sw360.rest.authserver.Sw360AuthorizationServer.CONFIG_ACCESS_TOKEN_VALIDITY_SECONDS;
import static org.eclipse.sw360.rest.authserver.Sw360AuthorizationServer.CONFIG_CLIENT_ID;
import static org.eclipse.sw360.rest.authserver.Sw360AuthorizationServer.CONFIG_CLIENT_SECRET;
import static org.eclipse.sw360.rest.authserver.security.Sw360GrantedAuthority.BASIC;
import static org.eclipse.sw360.rest.authserver.security.Sw360SecurityEncryptor.decrypt;

/**
 * This class configures the oauth2 authorization server specialities for the
 * authorization parts of this server. Only exception is that the
 * {@link AuthenticationManager} is shared with the standard security and this
 * one is configured with our oauth2 {@link AuthenticationProvider}s in
 * {@link Sw360WebSecurityConfiguration}.
 */
@Configuration
@EnableAuthorizationServer
public class Sw360AuthorizationServerConfiguration extends AuthorizationServerConfigurerAdapter {

    @Value("${security.oauth2.client.client-id}")
    private String clientId;

    @Value("${security.oauth2.client.client-secret}")
    private String clientSecret;

    @Value("${security.oauth2.client.scope}")
    private String[] scopes;

    @Value("${security.oauth2.client.authorized-grant-types}")
    private String[] authorizedGrantTypes;

    @Value("${security.oauth2.client.resource-ids}")
    private String resourceIds;

    @Value("${security.oauth2.client.access-token-validity-seconds}")
    private Integer accessTokenValiditySeconds;

    @Autowired
    private AuthenticationManager authenticationManager;

    @PostConstruct
    public void postSw360AuthorizationServerConfiguration() throws IOException {
        if (CONFIG_CLIENT_ID != null) {
            clientId = CONFIG_CLIENT_ID;
        }
        if (CONFIG_CLIENT_SECRET != null) {
            clientSecret = decrypt(CONFIG_CLIENT_SECRET);
        }
        if (CONFIG_ACCESS_TOKEN_VALIDITY_SECONDS != null) {
            accessTokenValiditySeconds = Integer.parseInt(CONFIG_ACCESS_TOKEN_VALIDITY_SECONDS);
        }
    }

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        endpoints
                .allowedTokenEndpointRequestMethods(HttpMethod.GET, HttpMethod.POST)
                .tokenStore(tokenStore())
                .tokenEnhancer(jwtAccessTokenConverter())
                .authenticationManager(authenticationManager);
    }

    @Override
    public void configure(AuthorizationServerSecurityConfigurer oauthServer) throws Exception {
        String serverAuthority = BASIC.getAuthority();
        oauthServer.tokenKeyAccess("isAnonymous() || hasAuthority('" + serverAuthority + "')")
                .checkTokenAccess("hasAuthority('" + serverAuthority + "')")
                .addTokenEndpointAuthenticationFilter(sw360CustomHeaderAuthenticationFilter());
    }

    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        clients.inMemory()
                .withClient(clientId)
                .authorizedGrantTypes(authorizedGrantTypes)
                .authorities(BASIC.getAuthority())
                .scopes(scopes)
                .resourceIds(resourceIds)
                .accessTokenValiditySeconds(accessTokenValiditySeconds)
                .secret(clientSecret);
    }

    @Bean
    public TokenStore tokenStore() {
        return new JwtTokenStore(jwtAccessTokenConverter());
    }

    @Bean
    protected JwtAccessTokenConverter jwtAccessTokenConverter() {
        KeyStoreKeyFactory keyStoreKeyFactory =
                new KeyStoreKeyFactory(new ClassPathResource("jwt-keystore.jks"), "sw360SecretKey".toCharArray());
        JwtAccessTokenConverter jwtAccessTokenConverter = new JwtAccessTokenConverter();
        jwtAccessTokenConverter.setKeyPair(keyStoreKeyFactory.getKeyPair("jwt"));
        return jwtAccessTokenConverter;
    }

    @Bean
    protected Filter sw360CustomHeaderAuthenticationFilter() {
        return new Sw360CustomHeaderAuthenticationFilter();
    }

    @Bean
    protected Sw360LiferayAuthenticationProvider sw360LiferayAuthenticationProvider() {
        return new Sw360LiferayAuthenticationProvider();
    }

    @Bean
    protected Sw360CustomHeaderAuthenticationProvider sw360CustomHeaderAuthenticationProvider() {
        return new Sw360CustomHeaderAuthenticationProvider();
    }

    @Bean
    protected Sw360CustomHeaderUserDetailsProvider principalProvider() {
        return new Sw360CustomHeaderUserDetailsProvider();
    }

    @Bean
    protected ThriftClients thriftClients() {
        return new ThriftClients();
    }
}

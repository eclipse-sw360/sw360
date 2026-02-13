/*
SPDX-FileCopyrightText: © 2024 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.rest.authserver.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.rest.authserver.client.service.Sw360ClientDetailsService;
import org.eclipse.sw360.rest.authserver.client.service.Sw360OidcUserInfoService;
import org.eclipse.sw360.rest.authserver.security.authproviders.Sw360UserAuthenticationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

/**
 * Configures the security settings for the authorization server
 *
 * @author smruti.sahoo@siemens.com
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final Sw360UserAuthenticationProvider sw360UserAuthenticationProvider;
    @SuppressWarnings("unused")
    private final Sw360ClientDetailsService sw360ClientDetailsService;
    @SuppressWarnings("unused")
    private final Sw360OidcUserInfoService sw360OidcUserInfoService;

    public SecurityConfig(
            Sw360UserAuthenticationProvider sw360UserAuthenticationProvider,
            Sw360ClientDetailsService sw360ClientDetailsService,
            Sw360OidcUserInfoService sw360OidcUserInfoService
    ) {
        this.sw360UserAuthenticationProvider = sw360UserAuthenticationProvider;
        this.sw360ClientDetailsService = sw360ClientDetailsService;
        this.sw360OidcUserInfoService = sw360OidcUserInfoService;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain webFilterChainForOauth(HttpSecurity httpSecurity) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                new OAuth2AuthorizationServerConfigurer();
        RequestMatcher endpointsMatcher = authorizationServerConfigurer.getEndpointsMatcher();

        httpSecurity
                .securityMatcher(endpointsMatcher)
                .authenticationProvider(sw360UserAuthenticationProvider)
                .with(authorizationServerConfigurer, Customizer.withDefaults())
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")))
                .csrf(csrf -> csrf.ignoringRequestMatchers(endpointsMatcher))
                .getConfigurer(OAuth2AuthorizationServerConfigurer.class).oidc(Customizer.withDefaults());

        return httpSecurity.build();
    }

    @Order(2)
    @Bean
    public SecurityFilterChain appSecurity(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .authenticationProvider(sw360UserAuthenticationProvider)
                .authorizeHttpRequests(
                authz -> authz
                .requestMatchers("/client-management/**").hasAuthority("ADMIN")
                .anyRequest().authenticated()
        ).httpBasic(Customizer.withDefaults()).formLogin(Customizer.withDefaults());
        return httpSecurity.csrf(csrf -> csrf.disable()).build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().build();
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);

        var keys = keyPairGenerator.generateKeyPair();
        var publicKey = (RSAPublicKey) keys.getPublic();
        var privateKey = keys.getPrivate();
        var rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    public ThriftClients thriftClients() {
        return new ThriftClients();
    }

}

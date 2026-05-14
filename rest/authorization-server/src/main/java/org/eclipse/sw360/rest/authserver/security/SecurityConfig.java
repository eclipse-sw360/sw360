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
import lombok.RequiredArgsConstructor;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.rest.authserver.security.authproviders.Sw360UserAuthenticationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

/**
 * Configures the security settings for the authorization server.
 *
 * <p>Two filter chains are wired:</p>
 * <ol>
 *   <li>{@link #webFilterChainForOauth(HttpSecurity)} (order 1) - the OAuth2 /
 *       OIDC endpoints managed by Spring Authorization Server.</li>
 *   <li>{@link #appSecurity(HttpSecurity)} (order 2) - everything else.
 *       Anonymous browser traffic is sent to the form login page, but requests
 *       to {@code /client-management/**} are challenged with HTTP Basic so
 *       that admin automation (token-generator-bot, the install scripts,
 *       etc.) can authenticate without holding a JWT.</li>
 * </ol>
 *
 * <p>The {@link org.springframework.security.web.authentication.www.BasicAuthenticationFilter}
 * remains installed globally - it is a passive filter that only acts when an
 * {@code Authorization: Basic ...} header is present - but the
 * {@code WWW-Authenticate: Basic} challenge is only emitted for the
 * {@code /client-management/**} path so browsers never see a Basic-Auth popup
 * on other endpoints.</p>
 *
 * @author smruti.sahoo@siemens.com
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private static final String CLIENT_MGMT_PATTERN = "/client-management/**";

    private static final String BASIC_REALM = "sw360-client-management";

    private final Sw360UserAuthenticationProvider sw360UserAuthenticationProvider;

    @Bean
    @Order(1)
    public SecurityFilterChain webFilterChainForOauth(HttpSecurity httpSecurity) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                new OAuth2AuthorizationServerConfigurer();
        RequestMatcher endpointsMatcher = authorizationServerConfigurer.getEndpointsMatcher();

        httpSecurity
                .securityMatcher(endpointsMatcher)
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
        RequestMatcher clientManagementMatcher = PathPatternRequestMatcher.pathPattern(CLIENT_MGMT_PATTERN);
        // Custom entry point that writes 401 directly without triggering servlet
        // container error dispatch (which would forward to /error and lose the status).
        var basicEntryPoint = new org.springframework.security.web.AuthenticationEntryPoint() {
            @Override
            public void commence(jakarta.servlet.http.HttpServletRequest request,
                    jakarta.servlet.http.HttpServletResponse response,
                    org.springframework.security.core.AuthenticationException authException) throws java.io.IOException {
                response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                response.setHeader("WWW-Authenticate", "Basic realm=\"" + BASIC_REALM + "\"");
                response.getWriter().write("Unauthorized");
                response.getWriter().flush();
            }
        };

        httpSecurity
                .authenticationProvider(sw360UserAuthenticationProvider)
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(CLIENT_MGMT_PATTERN).hasAuthority("ADMIN")
                        .anyRequest().authenticated())
                // HTTP Basic is always active (passive filter). The custom authenticationEntryPoint
                // avoids servlet error dispatch so callers get a clean 401 + WWW-Authenticate.
                .httpBasic(basic -> basic
                        .realmName(BASIC_REALM)
                        .authenticationEntryPoint(basicEntryPoint))
                .formLogin(Customizer.withDefaults())
                .exceptionHandling(eh -> eh
                        .defaultAuthenticationEntryPointFor(basicEntryPoint, clientManagementMatcher)
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                PathPatternRequestMatcher.pathPattern("/**")))
                .csrf(csrf -> csrf.ignoringRequestMatchers(CLIENT_MGMT_PATTERN));

        return httpSecurity.build();
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
}

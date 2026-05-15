/*
SPDX-FileCopyrightText: © 2024 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.rest.authserver.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.eclipse.sw360.rest.authserver.security.authproviders.Sw360UserAuthenticationProvider;
import org.eclipse.sw360.rest.authserver.security.key.KeyManager;
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
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

/**
 * Configures the security settings for the authorization server.
 *
 * <p>Two filter chains are wired:</p>
 * <ol>
 *   <li>{@link #webFilterChainForOauth(HttpSecurity)} (order 1) - the OAuth2 /
 *       OIDC endpoints managed by Spring Authorization Server.</li>
 *   <li>{@link #appSecurity(HttpSecurity)} (order 2) - everything else.
 *       Requests to {@code /client-management/**} accept <em>both</em> HTTP
 *       Basic <em>and</em> Bearer JWT. The JWT's {@code scope} claim is mapped
 *       to Spring authorities <strong>without</strong> the default
 *       {@code SCOPE_} prefix so that {@code hasAuthority("ADMIN")} matches
 *       tokens carrying {@code "ADMIN"} in their scope set. Anonymous browser
 *       traffic is sent to the form login page.</li>
 * </ol>
 *
 * @author smruti.sahoo@siemens.com
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private static final String CLIENT_MGMT_PATTERN = "/client-management/**";

    private static final String BASIC_REALM = "sw360-client-management";

    @NonNull
    private final Sw360UserAuthenticationProvider sw360UserAuthenticationProvider;

    @NonNull
    private final KeyManager keyManager;

    @Bean
    @Order(1)
    public SecurityFilterChain webFilterChainForOauth(HttpSecurity httpSecurity) {
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
                // HTTP Basic stays active for admin automation / curl / scripts.
                .httpBasic(basic -> basic
                        .realmName(BASIC_REALM)
                        .authenticationEntryPoint(basicEntryPoint))
                // JWT Bearer support - the React frontend (sw360oauth provider)
                // authenticates via authorization_code+PKCE and then calls
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(sw360JwtAuthConverter())))
                .formLogin(Customizer.withDefaults())
                .exceptionHandling(eh -> eh
                        .defaultAuthenticationEntryPointFor(basicEntryPoint, clientManagementMatcher)
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                PathPatternRequestMatcher.pathPattern("/**")))
                .csrf(csrf -> csrf.ignoringRequestMatchers(CLIENT_MGMT_PATTERN));

        return httpSecurity.build();
    }

    /**
     * Maps JWT {@code scope} claim values (e.g. {@code READ}, {@code WRITE},
     * {@code ADMIN}) to Spring Security authorities <em>without</em> the
     * default {@code SCOPE_} prefix. This ensures
     * {@code hasAuthority("ADMIN")} matches tokens minted by this
     * authorization server, whose {@code Sw360TokenCustomizerConfig} emits
     * plain {@code scope} values.
     *
     * <p>The {@code user_name} claim (set by the token customizer instead of
     * the standard {@code sub}) is used as the principal name.</p>
     */
    private static JwtAuthenticationConverter sw360JwtAuthConverter() {
        var scopesConverter = new JwtGrantedAuthoritiesConverter();
        scopesConverter.setAuthorityPrefix("");
        scopesConverter.setAuthoritiesClaimName("scope");
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(scopesConverter);
        converter.setPrincipalClaimName("user_name");
        return converter;
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().build();
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() throws NoSuchAlgorithmException,
            UnrecoverableKeyException, CertificateException, KeyStoreException, IOException, JOSEException {
        // The signing key is loaded from the persistent JKS keystore via
        // KeyManager so the JWK set (and its 'kid') is stable across
        // authorization-server restarts. Resource-server JWKS caches will
        // continue to validate previously-issued JWTs without a cache flush.
        RSAKey rsaKey = keyManager.rsaKey();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }
}

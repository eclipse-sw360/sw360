/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.security.jwt;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class Sw360HybridAuthenticationManagerResolverTest {

    @Mock
    private AuthenticationManagerResolver<HttpServletRequest> issuerResolver;

    @Mock
    private AuthenticationManager issuerAuthenticationManager;

    @Mock
    private AuthenticationManager legacyAuthenticationManager;

    @Test
    public void shouldDispatchToIssuerResolver_whenTokenHasIssClaim() throws Exception {
        String token = serializePlainJwt(new JWTClaimsSet.Builder()
                .issuer("https://keycloak.example.com/realms/sw360")
                .subject("alice@example.com")
                .build());
        MockHttpServletRequest request = bearerRequest(token);
        when(issuerResolver.resolve(request)).thenReturn(issuerAuthenticationManager);

        Sw360HybridAuthenticationManagerResolver resolver =
                new Sw360HybridAuthenticationManagerResolver(issuerResolver, legacyAuthenticationManager);

        AuthenticationManager resolved = resolver.resolve(request);

        assertThat(resolved).isSameAs(issuerAuthenticationManager);
        verify(issuerResolver).resolve(request);
    }

    @Test
    public void shouldDispatchToLegacyManager_whenTokenHasNoIssClaim() throws Exception {
        String token = serializePlainJwt(new JWTClaimsSet.Builder()
                .claim("user_name", "alice@example.com")
                .audience("sw360-REST-API")
                .build());
        MockHttpServletRequest request = bearerRequest(token);

        Sw360HybridAuthenticationManagerResolver resolver =
                new Sw360HybridAuthenticationManagerResolver(issuerResolver, legacyAuthenticationManager);

        AuthenticationManager resolved = resolver.resolve(request);

        assertThat(resolved).isSameAs(legacyAuthenticationManager);
        verify(issuerResolver, never()).resolve(any());
    }

    @Test
    public void shouldThrowMissingIssuer_whenTokenHasNoIssAndNoLegacyManagerConfigured() throws Exception {
        String token = serializePlainJwt(new JWTClaimsSet.Builder()
                .claim("user_name", "alice@example.com")
                .build());
        MockHttpServletRequest request = bearerRequest(token);

        Sw360HybridAuthenticationManagerResolver resolver =
                new Sw360HybridAuthenticationManagerResolver(issuerResolver, null);

        assertThatThrownBy(() -> resolver.resolve(request))
                .isInstanceOf(InvalidBearerTokenException.class)
                .hasMessageContaining("Missing issuer");
        verify(issuerResolver, never()).resolve(any());
    }

    @Test
    public void shouldDelegateToIssuerResolver_whenTokenIsMalformed() {
        MockHttpServletRequest request = bearerRequest("not-a-real-jwt");
        when(issuerResolver.resolve(request)).thenReturn(issuerAuthenticationManager);

        Sw360HybridAuthenticationManagerResolver resolver =
                new Sw360HybridAuthenticationManagerResolver(issuerResolver, legacyAuthenticationManager);

        AuthenticationManager resolved = resolver.resolve(request);

        assertThat(resolved).isSameAs(issuerAuthenticationManager);
    }

    @Test
    public void shouldDelegateToIssuerResolver_whenNoBearerTokenIsPresent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(issuerResolver.resolve(request)).thenReturn(issuerAuthenticationManager);

        Sw360HybridAuthenticationManagerResolver resolver =
                new Sw360HybridAuthenticationManagerResolver(issuerResolver, legacyAuthenticationManager);

        AuthenticationManager resolved = resolver.resolve(request);

        assertThat(resolved).isSameAs(issuerAuthenticationManager);
    }

    private static MockHttpServletRequest bearerRequest(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    private static String serializePlainJwt(JWTClaimsSet claims) {
        return new PlainJWT(claims).serialize();
    }
}

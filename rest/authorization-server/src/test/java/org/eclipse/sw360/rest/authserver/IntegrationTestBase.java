/*
 * Copyright Siemens AG, 2017, 2019, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.authserver;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import com.nimbusds.jwt.SignedJWT;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.users.User;
import org.eclipse.sw360.datahandler.services.users.UserGroup;
import org.eclipse.sw360.clients.users.UsersClient;
import org.eclipse.sw360.rest.authserver.client.persistence.OAuthClientRepository;
import org.eclipse.sw360.rest.authserver.client.service.Sw360ClientDetailsService;
import org.eclipse.sw360.rest.common.client.service.Sw360UserDetailsService;
import org.eclipse.sw360.rest.authserver.client.service.Sw360UserMirrorService;
import org.eclipse.sw360.rest.common.security.Sw360GrantedAuthority;
import org.eclipse.sw360.rest.common.security.Sw360UserDetailsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(classes = Sw360AuthorizationServer.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles({"dev", "test"})
public abstract class IntegrationTestBase {
    private static final Logger log = LogManager.getLogger(IntegrationTestBase.class);

    @Value("${local.server.port}")
    protected int port;

    @MockitoBean
    protected UsersClient usersClient;

    @MockitoSpyBean
    protected Sw360UserDetailsProvider sw360UserDetailsProvider;

    @MockitoSpyBean
    protected Sw360UserMirrorService sw360UserMirrorService;

    @MockitoBean
    protected Sw360ClientDetailsService sw360ClientDetailsService;

    @MockitoBean
    protected OAuthClientRepository clientRepo;

    protected User adminTestUser;

    protected User normalTestUser;

    protected RegisteredClient testClient;

    protected ResponseEntity<String> responseEntity;

    @MockitoBean
    Sw360UserDetailsService sw360UserDetailsService;

    @Autowired
    protected PasswordEncoder encoder;

    @BeforeEach
    public void setup() throws TException {
        setupTestUser();
        when(usersClient.getByEmailOrExternalId(eq(adminTestUser.getEmail()), anyString())).thenReturn(adminTestUser);
        when(usersClient.getByEmailOrExternalId(eq(normalTestUser.getEmail()), anyString()))
                .thenReturn(normalTestUser);
        when(usersClient.getByEmail(eq(adminTestUser.getEmail()))).thenReturn(adminTestUser);
        when(usersClient.getByEmail(eq(normalTestUser.getEmail()))).thenReturn(normalTestUser);
        when(usersClient.updateUser(org.mockito.ArgumentMatchers.any(User.class)))
                .thenReturn(RequestStatus.SUCCESS);

        // Default: any unknown user gets UsernameNotFoundException from the mock.
        // Use doThrow so specific stubs below can override without triggering the exception.
        org.mockito.Mockito.doThrow(new org.springframework.security.core.userdetails.UsernameNotFoundException("unknown user"))
                .when(sw360UserDetailsService).loadUserByUsername(org.mockito.ArgumentMatchers.anyString());
        // Known test users: override the default behaviour (last stub wins in Mockito)
        org.mockito.Mockito.doReturn(new org.springframework.security.core.userdetails.User(adminTestUser.getEmail(), encoder.encode(adminTestUser.getPassword()), List.of(new SimpleGrantedAuthority(Sw360GrantedAuthority.ADMIN.getAuthority()))))
                .when(sw360UserDetailsService).loadUserByUsername(adminTestUser.getEmail());
        org.mockito.Mockito.doReturn(new org.springframework.security.core.userdetails.User(normalTestUser.getEmail(), encoder.encode(normalTestUser.getPassword()), List.of(new SimpleGrantedAuthority(Sw360GrantedAuthority.READ.getAuthority()))))
                .when(sw360UserDetailsService).loadUserByUsername(normalTestUser.getEmail());

        setupTestClient();
        when(sw360ClientDetailsService.findByClientId(anyString())).thenReturn(testClient);
    }

    private void setupTestUser() {
        adminTestUser = new User()
                .setEmail("mockedserviceadminuser@sw360.org")
                .setDepartment("qa-admin")
                .setExternalid("service-mocked-by-mockito-admin")
                .setFullname("Mocked Service Admin User")
                .setGivenname("Mocked")
                .setLastname("Service Admin User")
                .setPassword("12345")
                .setUserGroup(UserGroup.ADMIN);

        normalTestUser = new User()
                .setEmail("mockedservicenormaluser@sw360.org")
                .setDepartment("qa-normal")
                .setExternalid("service-mocked-by-mockito-normal")
                .setFullname("Mocked Service Normal User")
                .setGivenname("Mocked")
                .setLastname("Service Normal User")
                .setPassword("12345")
                .setUserGroup(UserGroup.USER);
    }

    private void setupTestClient() {
        testClient = RegisteredClient.withId("trusted-sw360-client").clientId("trusted-sw360-client")
                .clientSecret(encoder.encode("sw360-secret")).authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .authorizationGrantType(new AuthorizationGrantType("password"))
                .scope(Sw360GrantedAuthority.READ.getAuthority())
                .scope(Sw360GrantedAuthority.WRITE.getAuthority())
                .scope(Sw360GrantedAuthority.ADMIN.getAuthority())
                .build();
    }

    protected void checkResponseBody() throws IOException {
        String responseBody = responseEntity.getBody();

        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));

        JsonNode responseBodyJsonNode = new ObjectMapper().readTree(responseBody);

        assertThat(responseBodyJsonNode.get("token_type").asText(), is("bearer"));
        assertThat(responseBodyJsonNode.get("scope").asText(), is(Sw360GrantedAuthority.READ.toString()));
        assertThat(responseBodyJsonNode.has("access_token"), is(true));
        assertThat(responseBodyJsonNode.has("expires_in"), is(true));
        assertThat(responseBodyJsonNode.has("jti"), is(true));
    }

    protected JsonNode checkJwtClaims(String... expectedAuthority) throws IOException, ParseException {
        String responseBody = responseEntity.getBody();

        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));

        JsonNode responseBodyJsonNode = new ObjectMapper().readTree(responseBody);
        assertThat(responseBodyJsonNode.has("access_token"), is(true));

        String accessToken = responseBodyJsonNode.get("access_token").asText();
        SignedJWT signedJWT = SignedJWT.parse(accessToken);
        String jwtClaims = signedJWT.getPayload().toString();
        JsonNode jwtClaimsJsonNode = new ObjectMapper().readTree(jwtClaims);
        assertThat(jwtClaimsJsonNode.get("aud").get(0).asText(), is("sw360-REST-API"));
        assertThat(jwtClaimsJsonNode.get("client_id").asText(), is("trusted-sw360-client"));

        JsonNode scopesNode = jwtClaimsJsonNode.get("scope");
        List<String> actualScopes = new ArrayList<>();
        if (scopesNode.isArray()) {
            for (final JsonNode scopeNode : scopesNode) {
                actualScopes.add(scopeNode.asText());
            }
        } else {
            actualScopes.add(scopesNode.asText());
        }
        assertThat(actualScopes, containsInAnyOrder(Sw360GrantedAuthority.READ.toString()));

        JsonNode authoritiesJsonNode = jwtClaimsJsonNode.get("authorities");
        List<String> actualAuthorities = new ArrayList<>();
        if (authoritiesJsonNode.isArray()) {
            for (final JsonNode authorityTextNode : authoritiesJsonNode) {
                actualAuthorities.add(authorityTextNode.asText());
            }
        } else {
            actualAuthorities.add(authoritiesJsonNode.asText());
        }
        log.info("ACTUAL: {}", actualAuthorities);
        log.info("EXPECTED: {}", StringUtils.join(expectedAuthority, ", "));
        assertThat(actualAuthorities, containsInAnyOrder(expectedAuthority));

        return jwtClaimsJsonNode;
    }
}

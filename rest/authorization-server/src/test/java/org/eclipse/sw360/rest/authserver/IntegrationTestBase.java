/*
 * Copyright Siemens AG, 2017, 2019. Part of the SW360 Portal Project.
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.datahandler.thrift.users.UserService;
import org.eclipse.sw360.rest.authserver.client.persistence.OAuthClientRepository;
import org.eclipse.sw360.rest.authserver.client.service.Sw360ClientDetailsService;
import org.eclipse.sw360.rest.authserver.client.service.Sw360UserDetailsService;
import org.eclipse.sw360.rest.authserver.security.Sw360GrantedAuthority;
import org.eclipse.sw360.rest.authserver.security.Sw360UserDetailsProvider;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Sw360AuthorizationServer.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"dev", "test"})
public abstract class IntegrationTestBase {

    @Value("${local.server.port}")
    protected int port;

    @MockBean
    protected ThriftClients thriftClients;

    @MockBean
    protected Sw360ClientDetailsService sw360ClientDetailsService;

    @MockBean
    protected OAuthClientRepository clientRepo;

    @MockBean
    Sw360UserDetailsProvider sw360UserDetailsProvider;

    @SpyBean
    protected RestTemplateBuilder restTemplateBuilder;

    protected User adminTestUser;

    protected User normalTestUser;

    protected RegisteredClient testClient;

    protected ResponseEntity<String> responseEntity;

    @MockBean
    Sw360UserDetailsService sw360UserDetailsService;

    @Autowired
    protected BCryptPasswordEncoder encoder;

    @Before
    public void setup() throws TException {
        setupTestUser();
        UserService.Client mockedUserService = mock(UserService.Client.class);
        when(mockedUserService.getByEmailOrExternalId(eq(adminTestUser.email), anyString())).thenReturn(adminTestUser);
        when(mockedUserService.getByEmailOrExternalId(eq(normalTestUser.email), anyString()))
                .thenReturn(normalTestUser);
        when(thriftClients.makeUserClient()).thenReturn(mockedUserService);

        when(sw360UserDetailsService.loadUserByUsername(adminTestUser.email)).thenReturn(new org.springframework.security.core.userdetails.User(adminTestUser.email, encoder.encode(adminTestUser.password), List.of(new SimpleGrantedAuthority(Sw360GrantedAuthority.ADMIN.getAuthority()))));
        when(sw360UserDetailsService.loadUserByUsername(normalTestUser.email)).thenReturn(new org.springframework.security.core.userdetails.User(normalTestUser.email, encoder.encode(normalTestUser.password), List.of(new SimpleGrantedAuthority(Sw360GrantedAuthority.READ.getAuthority()))));

        setupTestClient();
        when(sw360ClientDetailsService.findByClientId(anyString())).thenReturn(testClient);
    }

    private void setupTestUser() {
        adminTestUser = new User("mockedserviceadminuser@sw360.org", "qa-admin");
        adminTestUser.externalid = "service-mocked-by-mockito-admin";
        adminTestUser.fullname = "Mocked Service Admin User";
        adminTestUser.givenname = "Mocked";
        adminTestUser.lastname = "Service Admin User";
        adminTestUser.password = "12345";
        adminTestUser.userGroup = UserGroup.ADMIN;

        normalTestUser = new User("mockedservicenormaluser@sw360.org", "qa-normal");
        normalTestUser.externalid = "service-mocked-by-mockito-normal";
        normalTestUser.fullname = "Mocked Service Normal User";
        normalTestUser.givenname = "Mocked";
        normalTestUser.lastname = "Service Normal User";
        normalTestUser.password = "12345";
        normalTestUser.userGroup = UserGroup.USER;
    }

    private void setupTestClient() {
        testClient = RegisteredClient.withId("trusted-sw360-client").clientId("trusted-sw360-client")
                .clientSecret(encoder.encode("sw360-secret")).authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.PASSWORD)
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

    protected JsonNode checkJwtClaims(String... expectedAuthority) throws IOException {
        String responseBody = responseEntity.getBody();

        assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));

        JsonNode responseBodyJsonNode = new ObjectMapper().readTree(responseBody);
        assertThat(responseBodyJsonNode.has("access_token"), is(true));

        String accessToken = responseBodyJsonNode.get("access_token").asText();
        Jwt jwt = JwtHelper.decode(accessToken);
        String jwtClaims = jwt.getClaims();
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
        System.out.println("ACTUAL: " + actualAuthorities);
        System.out.println("EXPECTED: " + StringUtils.join(expectedAuthority, ", "));
        assertThat(actualAuthorities, containsInAnyOrder(expectedAuthority));

        return jwtClaimsJsonNode;
    }
}

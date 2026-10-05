/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.user;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.SW360ConfigKeys;
import org.eclipse.sw360.datahandler.thrift.ConfigFor;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.configurations.SW360ConfigsService;
import org.eclipse.sw360.datahandler.thrift.users.RestApiToken;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.eclipse.sw360.rest.resourceserver.Sw360ResourceServer.API_TOKEN_MAX_VALIDITY_IN_DAYS;

/**
 * Unit tests for the fully DB-config-driven API write-token generator enforcement in
 * {@link Sw360UserService#convertToRestApiToken}.
 * <p>
 * Regression coverage for the removal of the legacy, properties-based
 * {@code rest.apitoken.write.generator.enable} flag (and the corresponding
 * {@code Sw360ResourceServer.API_WRITE_TOKEN_GENERATOR_ENABLED} static field): the enforcement
 * check must now read {@link SW360ConfigKeys#UI_REST_APITOKEN_GENERATOR_ENABLE} from the
 * UI_CONFIGURATION DB container so that toggling the setting via the Admin UI takes effect
 * immediately.
 */
class Sw360UserServiceTest {

    private final Sw360UserService userService = new Sw360UserService();
    private MockedStatic<ThriftClients> thriftClientsMock;
    private SW360ConfigsService.Iface configsClient;

    @BeforeEach
    void setUp() {
        thriftClientsMock = mockStatic(ThriftClients.class);
        configsClient = mock(SW360ConfigsService.Iface.class);
        thriftClientsMock.when(ThriftClients::makeSW360ConfigsClient).thenReturn(configsClient);
    }

    @AfterEach
    void tearDown() {
        thriftClientsMock.close();
    }

    private Map<String, Object> writeTokenRequestBody() {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", "my-token");
        requestBody.put("expirationDate", LocalDate.now().plusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE));
        requestBody.put("authorities", List.of("READ", "WRITE"));
        return requestBody;
    }

    private User adminUser() {
        return new User().setEmail("admin@sw360.org").setUserGroup(UserGroup.ADMIN);
    }

    @Test
    void shouldRejectWriteTokenWhenDbConfigDisablesGenerator() throws TException {
        Map<String, String> uiConfigs = Map.of(SW360ConfigKeys.UI_REST_APITOKEN_GENERATOR_ENABLE, "false");
        when(configsClient.getConfigForContainer(eq(ConfigFor.UI_CONFIGURATION))).thenReturn(uiConfigs);

        assertThrows(AccessDeniedException.class,
                () -> userService.convertToRestApiToken(writeTokenRequestBody(), adminUser()));
    }

    @Test
    void shouldAllowWriteTokenWhenDbConfigEnablesGenerator() throws TException {
        Map<String, String> uiConfigs = Map.of(SW360ConfigKeys.UI_REST_APITOKEN_GENERATOR_ENABLE, "true");
        when(configsClient.getConfigForContainer(eq(ConfigFor.UI_CONFIGURATION))).thenReturn(uiConfigs);

        RestApiToken token = userService.convertToRestApiToken(writeTokenRequestBody(), adminUser());

        assertNotNull(token);
        assertEquals("my-token", token.getName());
    }

    @Test
    void shouldDefaultToEnabledWhenDbConfigKeyIsMissing() throws TException {
        when(configsClient.getConfigForContainer(eq(ConfigFor.UI_CONFIGURATION))).thenReturn(new HashMap<>());

        RestApiToken token = userService.convertToRestApiToken(writeTokenRequestBody(), adminUser());

        assertNotNull(token);
    }

    @Test
    void shouldDefaultToEnabledWhenThriftCallFails() throws TException {
        when(configsClient.getConfigForContainer(eq(ConfigFor.UI_CONFIGURATION))).thenThrow(new TException("backend unreachable"));

        RestApiToken token = userService.convertToRestApiToken(writeTokenRequestBody(), adminUser());

        assertNotNull(token);
    }

    @Test
    void shouldUseConfiguredMaxValidityWhenExpirationDateIsMissing() throws TException {
        Map<String, String> uiConfigs = Map.of(SW360ConfigKeys.UI_REST_APITOKEN_GENERATOR_ENABLE, "true");
        when(configsClient.getConfigForContainer(eq(ConfigFor.UI_CONFIGURATION))).thenReturn(uiConfigs);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", "my-token");
        requestBody.put("authorities", List.of("READ"));

        RestApiToken token = userService.convertToRestApiToken(requestBody, adminUser());

        assertNotNull(token);
        assertEquals(Integer.parseInt(API_TOKEN_MAX_VALIDITY_IN_DAYS), token.getNumberOfDaysValid());
    }
}

/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.common;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Regression tests pinning the exact string values of frontend-facing configuration keys.
 * <p>
 * These keys must match exactly what the frontend sends in PATCH requests to
 * /api/configurations(/container/{configFor}); otherwise the backend rejects the value as
 * "Invalid config" (see SW360ConfigsDatabaseHandler#isConfigValid), because it never recognizes
 * the key.
 */
public class SW360ConfigKeysTest {

    @Test
    public void apiTokenGeneratorEnableKeyMatchesFrontendContract() {
        assertEquals("ui.rest.apitoken.generator.enable",
                SW360ConfigKeys.UI_REST_APITOKEN_GENERATOR_ENABLE);
    }

    @Test
    public void writeAccessTokenInPreferencesEnabledKeyMatchesFrontendContract() {
        // Regression test for: BadRequestClientException "Invalid config:
        // [ui.rest.api.write.access.token.in.preferences.enabled : true]".
        // The frontend sends this key WITH a "ui." prefix; the backend constant previously
        // omitted it, causing isConfigValid(...) to never match and reject the update.
        assertEquals("ui.rest.api.write.access.token.in.preferences.enabled",
                SW360ConfigKeys.UI_REST_API_WRITE_ACCESS_TOKEN_IN_PREFERENCES_ENABLED);
    }

    @Test
    public void bothApiTokenUiKeysAreRegisteredAsKnownConfigKeys() {
        assertTrue(SW360ConfigKeys.ALL_KNOWN_CONFIG_KEYS.contains(
                SW360ConfigKeys.UI_REST_APITOKEN_GENERATOR_ENABLE));
        assertTrue(SW360ConfigKeys.ALL_KNOWN_CONFIG_KEYS.contains(
                SW360ConfigKeys.UI_REST_API_WRITE_ACCESS_TOKEN_IN_PREFERENCES_ENABLED));
    }
}

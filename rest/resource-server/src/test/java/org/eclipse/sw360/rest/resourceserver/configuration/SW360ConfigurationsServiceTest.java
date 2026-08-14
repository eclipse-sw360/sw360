/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.configuration;

import org.eclipse.sw360.datahandler.common.SW360ConfigKeys;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SW360ConfigurationsServiceTest {

    /**
     * Regression test for the bug where toggling "Enable API Token Generator" (or the
     * write-access-token-in-preferences flag) in the Admin UI silently failed to persist.
     * <p>
     * Root cause: these two keys are DB-backed, admin-toggleable UI configs (see
     * SW360ConfigsDatabaseHandler), but were ALSO being exposed via
     * getSW360ConfigFromProperties(). Since SW360ConfigurationsController#updateConfigInService
     * strips out any key present in getSW360ConfigFromProperties() before persisting (treating it
     * as read-only), the toggle value from the frontend never reached the database. Additionally,
     * on every GET the properties value would override the persisted DB value.
     * <p>
     * Fix: these two keys must NOT appear in getSW360ConfigFromProperties() so they flow through
     * to the DB-backed update/read path untouched. There is no longer a legacy, properties-based
     * fallback for these keys - they are fully DB-config-driven (see also
     * Sw360UserService#isApiTokenGeneratorEnabled, which enforces this DB value directly instead
     * of a static properties-loaded flag).
     */
    @Test
    void shouldNotExposeDbBackedUiTokenConfigsAsReadonlyProperties() {
        SW360ConfigurationsService service = new SW360ConfigurationsService();

        Map<String, String> configs = service.getSW360ConfigFromProperties();

        assertFalse(configs.containsKey(SW360ConfigKeys.UI_REST_APITOKEN_GENERATOR_ENABLE),
                "UI_REST_APITOKEN_GENERATOR_ENABLE must not be treated as a read-only properties config, "
                        + "otherwise admin UI toggles for it will not persist to the DB.");
        assertFalse(configs.containsKey(SW360ConfigKeys.UI_REST_API_WRITE_ACCESS_TOKEN_IN_PREFERENCES_ENABLED),
                "UI_REST_API_WRITE_ACCESS_TOKEN_IN_PREFERENCES_ENABLED must not be treated as a read-only "
                        + "properties config, otherwise admin UI toggles for it will not persist to the DB.");
        assertFalse(configs.containsKey("ui.rest.apitoken.write.generator.enable"),
                "The legacy properties-based key must no longer be exposed; this feature is fully "
                        + "DB-config-driven now.");
        assertTrue(configs.containsKey("rest.apitoken.max.validity.days"),
                "Token max validity must stay properties-backed for UI date limits.");
    }
}

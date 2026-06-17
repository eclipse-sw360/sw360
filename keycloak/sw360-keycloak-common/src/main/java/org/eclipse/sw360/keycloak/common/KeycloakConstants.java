/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.keycloak.common;

/**
 * Shared constants for Keycloak providers.
 *
 * @author SW360 Team
 */
public final class KeycloakConstants {
    private KeycloakConstants() {}

    // Custom Keycloak User Attributes
    public static final String ATTR_DEPARTMENT = "Department";
    public static final String ATTR_EXTERNAL_ID = "externalId";

    // Default values for missing attributes
    public static final String DEFAULT_FIRST_NAME = "Not Provided";
    public static final String DEFAULT_LAST_NAME = "Not Provided";
    public static final String DEFAULT_DEPARTMENT = "DEPARTMENT";
    public static final String DEFAULT_EXTERNAL_ID = "N/A";

    // Common Configuration
    public static final String REALM_SW360 = "sw360";

    public enum ProviderService {
        LISTENER,
        USER_STORAGE_PROVIDER
    }
}

/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.rest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.common.CommonUtils;

/**
 * Shared secret for SW360 backend WAR HTTP access.
 *
 * <p>When enabled, every backend request must carry {@link #HEADER_NAME} matching the
 * configured token. Resource-server and backend {@code *Clients} attach it via
 * {@link BackendRestClients}. The real token must be set per deployment (properties or
 * env); do not commit production secrets.
 */
public final class BackendInternalAuth {

    private static final Logger log = LogManager.getLogger(BackendInternalAuth.class);

    public static final String HEADER_NAME = "X-SW360-Internal-Token";

    public static final String PROP_ENABLED = "backend.internal.auth.enabled";
    public static final String PROP_TOKEN = "backend.internal.token";

    public static final String ENV_ENABLED = "SW360_BACKEND_INTERNAL_AUTH_ENABLED";
    public static final String ENV_TOKEN = "SW360_BACKEND_INTERNAL_TOKEN";

    private static final String PROPERTIES_FILE_PATH = "/sw360.properties";

    private static final boolean ENABLED;
    private static final String TOKEN;

    static {
        Properties props = CommonUtils.loadProperties(BackendInternalAuth.class, PROPERTIES_FILE_PATH);
        ENABLED = parseEnabled(firstNonBlank(System.getenv(ENV_ENABLED), props.getProperty(PROP_ENABLED, "false")));
        TOKEN = nullToEmpty(firstNonBlank(System.getenv(ENV_TOKEN), props.getProperty(PROP_TOKEN, "")));

        if (ENABLED && TOKEN.isEmpty()) {
            log.error("{} is true but no token is configured ({} or env {}). "
                    + "All backend HTTP requests will be rejected until a token is set.",
                    PROP_ENABLED, PROP_TOKEN, ENV_TOKEN);
        } else if (ENABLED) {
            log.info("Backend internal auth is enabled (header {}).", HEADER_NAME);
        } else {
            log.info("Backend internal auth is disabled ({}=false).", PROP_ENABLED);
        }
    }

    private BackendInternalAuth() {}

    public static boolean isEnabled() {
        return ENABLED;
    }

    /**
     * Configured token for outbound clients. Empty when unset. Never log this value.
     */
    public static String getToken() {
        return TOKEN;
    }

    /**
     * Whether {@link BackendRestClients} should send {@link #HEADER_NAME}.
     */
    public static boolean shouldAttachHeader() {
        return ENABLED && !TOKEN.isEmpty();
    }

    /**
     * Constant-time compare of the request header to the configured token.
     */
    public static boolean matches(String providedHeaderValue) {
        if (!ENABLED) {
            return true;
        }
        if (TOKEN.isEmpty() || providedHeaderValue == null) {
            return false;
        }
        byte[] expected = TOKEN.getBytes(StandardCharsets.UTF_8);
        byte[] actual = providedHeaderValue.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }

    private static boolean parseEnabled(String value) {
        return Boolean.parseBoolean(nullToEmpty(value).trim());
    }

    private static String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary.trim();
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback.trim();
        }
        return "";
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}

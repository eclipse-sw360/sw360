/*
 * Copyright Bosch Software Innovations GmbH, 2016.
 * Copyright Siemens AG, 2016-2019, 2024-2026.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.keycloak.event.listener.service;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.jboss.logging.Logger;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Helper class to map organization from identity provider to sw360 internal org names.
 * Uses intelligent lazy loading with automatic retry if initial load fails.
 */
public class OrganizationMapper {

    private static final Logger log = Logger.getLogger(OrganizationMapper.class);

    private static final String MAPPING_KEYS_PREFIX = "mapping.";
    private static final String MAPPING_VALUES_SUFFIX = ".target";
    private static final String MATCH_PREFIX_KEY = "match.prefix";
    private static final String ENABLE_CUSTOM_MAPPING_KEY = "enable.custom.mapping";
    private static final String PROPERTIES_FILE_PATH = "/orgmapping.properties";

    private static boolean matchPrefix = false;
    private static boolean customMappingEnabled = false;
    private static List<Map.Entry<String, String>> sortedOrganizationMappings = new ArrayList<>();

    // Flags for intelligent loading
    private static volatile boolean initialized = false;
    private static volatile boolean loadAttempted = false;
    private static final Object INIT_LOCK = new Object();

    static {
        tryInitialize();
    }

    /**
     * Attempts to initialize the mapper. If initialization fails, it can be retried later.
     */
    private static void tryInitialize() {
        synchronized (INIT_LOCK) {
            if (!initialized) {
                loadAttempted = true;
                loadOrganizationMapperSettings();
            }
        }
    }

    /**
     * Ensures the mapper is properly initialized before use.
     * If initial load failed and properties are empty, attempts to reload once.
     */
    private static void ensureInitialized() {
        if (initialized) {
            return;
        }

        synchronized (INIT_LOCK) {
            if (!initialized && loadAttempted) {
                // Initial load was attempted but failed, try one more time
                log.info("Re-attempting to load organization mapping properties...");
                loadOrganizationMapperSettings();
            } else if (!loadAttempted) {
                // Static block didn't run yet (unusual), initialize now
                tryInitialize();
            }
        }
    }

    /**
     * Maps organization name from identity provider to sw360 internal org name.
     * If custom mapping is disabled, returns the original name.
     *
     * @param name the organization name from identity provider
     * @return the mapped organization name or original if no mapping found
     */
    public static String mapOrganizationName(String name) {
        // Ensure initialization before accessing configuration
        ensureInitialized();

        if (name == null || name.isEmpty()) {
            return name;
        }

        if (!customMappingEnabled) {
            log.debug("Custom organization mapping is disabled, returning original name: " + name);
            return name;
        }

        final Predicate<Map.Entry<String, String>> matcher;
        if (matchPrefix) {
            matcher = e -> name.startsWith(e.getKey());
        } else {
            // match complete name
            matcher = e -> name.equals(e.getKey());
        }

        String mappedName = sortedOrganizationMappings.stream()
                .filter(matcher)
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(name);

        if (!mappedName.equals(name)) {
            log.info("Mapped organization name from '" + name + "' to '" + mappedName + "'");
        }

        return mappedName;
    }

    /**
     * Check if custom organization mapping is enabled.
     *
     * @return true if custom mapping is enabled
     */
    public static boolean isCustomMappingEnabled() {
        ensureInitialized();
        return customMappingEnabled;
    }

    /**
     * Check if prefix matching is enabled.
     *
     * @return true if prefix matching is enabled
     */
    public static boolean isMatchPrefixEnabled() {
        ensureInitialized();
        return matchPrefix;
    }

    /**
     * Get the number of configured mappings.
     *
     * @return number of mappings
     */
    public static int getMappingCount() {
        ensureInitialized();
        return sortedOrganizationMappings.size();
    }

    private static void loadOrganizationMapperSettings() {
        log.info("Initializing OrganizationMapper...");

        // Load properties with preference to system configuration path (e.g., /etc/sw360/orgmapping.properties)
        // System config properties override bundled resource properties
        Properties orgmappingProperties = CommonUtils.loadProperties(OrganizationMapper.class, PROPERTIES_FILE_PATH, true);

        if (orgmappingProperties.isEmpty()) {
            log.info("No organization mapping properties found at " + PROPERTIES_FILE_PATH
                    + " (checked system config path: " + CommonUtils.SYSTEM_CONFIGURATION_PATH + "), custom mapping disabled");
            // Don't mark as initialized - allow retry on next access
            return;
        }

        // Mark as initialized since we successfully loaded properties
        initialized = true;

        log.info("Organization mapping properties loaded from system config path: "
                + CommonUtils.SYSTEM_CONFIGURATION_PATH + PROPERTIES_FILE_PATH + " (if exists) or bundled resource");

        matchPrefix = Boolean.parseBoolean(orgmappingProperties.getProperty(MATCH_PREFIX_KEY, "false"));
        customMappingEnabled = Boolean.parseBoolean(orgmappingProperties.getProperty(ENABLE_CUSTOM_MAPPING_KEY, "false"));

        if (!customMappingEnabled) {
            log.info("Custom organization mapping is disabled via configuration");
            return;
        }

        List<String> mappingSourceKeys = orgmappingProperties
                .stringPropertyNames()
                .stream()
                .filter(p -> p.startsWith(MAPPING_KEYS_PREFIX) && !p.endsWith(MAPPING_VALUES_SUFFIX))
                .toList();

        Map<String, String> tempOrgMappings = new HashMap<>();
        for (String sourceKey : mappingSourceKeys) {
            String sourceOrg = orgmappingProperties.getProperty(sourceKey);
            String targetOrg = orgmappingProperties.getProperty(sourceKey + MAPPING_VALUES_SUFFIX);
            if (sourceOrg != null && targetOrg != null && !sourceOrg.isEmpty() && !targetOrg.isEmpty()) {
                tempOrgMappings.put(sourceOrg, targetOrg);
                log.debug("Loaded organization mapping: '" + sourceOrg + "' -> '" + targetOrg + "'");
            }
        }

        // Sort by key length in descending order for longest match first
        sortedOrganizationMappings = tempOrgMappings
                .entrySet()
                .stream()
                .sorted(Comparator.comparingInt((Map.Entry<String, String> o) -> o.getKey().length()).reversed())
                .collect(Collectors.toList());

        log.info(String.format("OrganizationMapper initialized with %d mappings, matchPrefix=%s",
                sortedOrganizationMappings.size(), matchPrefix));
    }
}

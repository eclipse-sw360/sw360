/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Adapted from the old OrganizationHelper.java used in SW360 18.0.2-M1
 * Original location: frontend/sw360-portlet/src/main/java/org/eclipse/sw360/portal/users/OrganizationHelper.java
 */
package org.eclipse.sw360.keycloak.event.listener.utils;

import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Utility class to map organization names from identity provider to SW360 internal organization names.
 * <p>
 * This class loads mappings from orgmapping.properties file and provides organization name mapping
 * functionality that was previously available in the old frontend OrganizationHelper.
 * </p>
 * <p>
 * Configuration example in orgmapping.properties:
 * <pre>
 * enable.custom.mapping=true
 * match.prefix=false
 * mapping.1=Department-A
 * mapping.1.target=DEPT_A
 * mapping.2=Company XYZ
 * mapping.2.target=XYZ
 * </pre>
 * </p>
 */
public class OrganizationMapper {
    private static final Logger log = Logger.getLogger(OrganizationMapper.class);

    private static final String MAPPING_KEYS_PREFIX = "mapping.";
    private static final String MAPPING_VALUES_SUFFIX = ".target";
    private static final String MATCH_PREFIX_KEY = "match.prefix";
    private static final String ENABLE_CUSTOM_MAPPING_KEY = "enable.custom.mapping";
    private static final String PROPERTIES_FILE_PATH = "/orgmapping.properties";

    private static boolean matchPrefix = false;
    private static List<Map.Entry<String, String>> sortedOrganizationMappings;
    private static boolean customMappingEnabled = false;

    static {
        loadOrganizationMappings();
    }

    /**
     * Maps an organization name according to the configured mappings.
     * <p>
     * If custom mapping is disabled, returns the original name unchanged.
     * If enabled, searches for a matching mapping and returns the target name.
     * </p>
     *
     * @param name The organization name to map (e.g., from Keycloak)
     * @return The mapped organization name, or the original if no mapping found
     */
    public static String mapOrganizationName(String name) {
        if (!customMappingEnabled || name == null || name.trim().isEmpty()) {
            return name;
        }

        final Predicate<Map.Entry<String, String>> matcher;
        if (matchPrefix) {
            // Match if name starts with the mapping key
            matcher = e -> name.startsWith(e.getKey());
        } else {
            // Match complete name
            matcher = e -> name.equals(e.getKey());
        }

        return sortedOrganizationMappings.stream()
                .filter(matcher)
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(name);
    }

    /**
     * Loads organization mapping configuration from orgmapping.properties file.
     * <p>
     * The file should be located in src/main/resources/orgmapping.properties
     * </p>
     */
    private static void loadOrganizationMappings() {
        log.info("Loading organization mapping configuration");

        Properties properties = new Properties();
        try (InputStream inputStream = OrganizationMapper.class.getResourceAsStream(PROPERTIES_FILE_PATH)) {
            if (inputStream == null) {
                log.warn("Organization mapping properties file not found: " + PROPERTIES_FILE_PATH);
                log.info("Organization mapping will be disabled. Create the file to enable custom mappings.");
                customMappingEnabled = false;
                sortedOrganizationMappings = Collections.emptyList();
                return;
            }

            properties.load(inputStream);
        } catch (IOException e) {
            log.error("Failed to load organization mapping properties", e);
            customMappingEnabled = false;
            sortedOrganizationMappings = Collections.emptyList();
            return;
        }

        // Read configuration
        matchPrefix = Boolean.parseBoolean(properties.getProperty(MATCH_PREFIX_KEY, "false"));
        customMappingEnabled = Boolean.parseBoolean(properties.getProperty(ENABLE_CUSTOM_MAPPING_KEY, "false"));

        if (!customMappingEnabled) {
            log.info("Organization mapping is disabled by configuration");
            sortedOrganizationMappings = Collections.emptyList();
            return;
        }

        // Load mappings
        List<Object> mappingSourceKeys = properties.keySet().stream()
                .filter(p -> ((String) p).startsWith(MAPPING_KEYS_PREFIX)
                        && !((String) p).endsWith(MAPPING_VALUES_SUFFIX))
                .collect(Collectors.toList());

        Map<String, String> tempOrgMappings = new HashMap<>();
        for (Object sourceKey : mappingSourceKeys) {
            String sourceOrg = properties.getProperty((String) sourceKey);
            String targetOrg = properties.getProperty(sourceKey + MAPPING_VALUES_SUFFIX);

            if (sourceOrg != null && targetOrg != null
                    && !sourceOrg.trim().isEmpty() && !targetOrg.trim().isEmpty()) {
                tempOrgMappings.put(sourceOrg, targetOrg);
                log.debug("Loaded mapping: " + sourceOrg + " -> " + targetOrg);
            }
        }

        // Sort by key length (longest first) for prefix matching
        sortedOrganizationMappings = tempOrgMappings.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<String, String> o) -> o.getKey().length())
                        .reversed())
                .collect(Collectors.toList());

        log.info(String.format("Organization mapping initialized with %d mappings (matchPrefix=%b)",
                sortedOrganizationMappings.size(), matchPrefix));
    }

    /**
     * Returns whether custom organization mapping is currently enabled.
     *
     * @return true if custom mapping is enabled, false otherwise
     */
    public static boolean isCustomMappingEnabled() {
        return customMappingEnabled;
    }

    /**
     * Returns the number of configured mappings.
     *
     * @return the count of organization mappings
     */
    public static int getMappingCount() {
        return sortedOrganizationMappings != null ? sortedOrganizationMappings.size() : 0;
    }
}

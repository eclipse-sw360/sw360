/*
 * Copyright Siemens AG, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.datahandler.spring;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;

import java.util.Arrays;
import java.util.Properties;

/**
 * Helper utility to load the couchdb.properties or couchdb-test.properties into the Spring environment based on the
 * active profile.
 */
public final class CouchDbPropertyLoader {
    private static final Logger log = LogManager.getLogger(CouchDbPropertyLoader.class);

    private CouchDbPropertyLoader() {
    }

    /**
     * Configures the property sources for the given application context. Calls the CommonUtils.loadProperties() to read
     * CouchDB.properties.
     *
     * @param environment The servlet context to configure.
     */
    public static void loadCouchDbProperties(@NotNull ConfigurableEnvironment environment) {
        MutablePropertySources propertySources = environment.getPropertySources();

        String[] activeProfiles = environment.getActiveProfiles();
        boolean isTestProfileActive = Arrays.asList(activeProfiles).contains(DatabaseConfig.TEST_PROFILE_NAME);

        String propertiesFileNameToLoad = isTestProfileActive ?
                DatabaseConfig.TEST_PROPERTIES_FILE_PATH :
                DatabaseConfig.PROPERTIES_FILE_PATH;

        log.debug("Active profiles: {}. Loading properties from: {}",
                Arrays.toString(activeProfiles), propertiesFileNameToLoad);

        Properties props = CommonUtils.loadProperties(CouchDbContextInitializer.class, propertiesFileNameToLoad);

        if (!isTestProfileActive) {
            propertySources.addFirst(new PropertiesPropertySource("systemCouchDbProperties", props));
        } else {
            propertySources.addFirst(new PropertiesPropertySource("testCouchDbProperties", props));
        }
    }
}

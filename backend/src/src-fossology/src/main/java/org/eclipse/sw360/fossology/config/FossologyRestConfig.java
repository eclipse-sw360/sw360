/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.fossology.config;

import org.eclipse.sw360.datahandler.db.ConfigContainerRepository;
import org.eclipse.sw360.datahandler.thrift.ConfigContainer;
import org.eclipse.sw360.datahandler.thrift.ConfigFor;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;

import static org.apache.log4j.Logger.getLogger;

/**
 * Settings for the fossology rest connection
 */
@Component
public class FossologyRestConfig {

    private final Logger log = getLogger(this.getClass());

    public static final String CONFIG_KEY_URL = "url";
    public static final String CONFIG_KEY_TOKEN = "token";
    public static final String CONFIG_KEY_FOLDER_ID = "folderId";

    private final ConfigContainerRepository repository;

    private ConfigContainer config;

    private boolean outdated;

    @Autowired
    public FossologyRestConfig(ConfigContainerRepository repository) {
        this.repository = repository;
        // eager loading (or initial insert)
        get();
    }

    public String getBaseUrlWithSlash() {
        String url = getFirstValue(CONFIG_KEY_URL);

        if (url != null && !url.endsWith("/")) {
            url += "/";
        }

        return url;
    }

    public String getAccessToken() {
        return getFirstValue(CONFIG_KEY_TOKEN);
    }

    public String getFolderId() {
        return getFirstValue(CONFIG_KEY_FOLDER_ID);
    }

    private String getFirstValue(String key) {
        return get().getConfigKeyToValues().getOrDefault(key, new HashSet<>()).stream().findFirst().orElse(null);
    }

    public ConfigContainer update(ConfigContainer newConfig) {
        if (!ConfigFor.FOSSOLOGY_REST.equals(newConfig.getConfigFor())) {
            throw new IllegalArgumentException(
                    "A ConfigContainer was given that is not meant to carry FOSSology REST configuration. It is for "
                            + newConfig.getConfigFor());
        }

        String url = newConfig.getConfigKeyToValues().get(CONFIG_KEY_URL).stream().findFirst().orElseThrow(
                () -> new IllegalStateException("The new FOSSology REST configuration does not contain a URL."));
        try {
            new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("The new FOSSology REST configuration does not contain a valid URL.");
        }

        newConfig.getConfigKeyToValues().get(CONFIG_KEY_TOKEN).stream().findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "The new FOSSology REST configuration does not contain an access token."));

        String folderId = newConfig.getConfigKeyToValues().get(CONFIG_KEY_FOLDER_ID).stream().findFirst().orElseThrow(
                () -> new IllegalStateException("The new FOSSology REST configuration does not contain a folder id."));
        try {
            Long.parseLong(folderId);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("The new FOSSology REST configuration does not contain a valid folder id.");
        }

        ConfigContainer current = get();
        current.setConfigKeyToValues(newConfig.getConfigKeyToValues());

        repository.update(current);
        outdated = true;

        log.info("Successfully updated fossology configuration to: " + current);

        return current;
    }

    public ConfigContainer get() {
        if (config == null || outdated) {
            try {
                config = repository.getByConfigFor(ConfigFor.FOSSOLOGY_REST);
                outdated = false;
            } catch (IllegalStateException e) {
                ConfigContainer newConfig = new ConfigContainer(ConfigFor.FOSSOLOGY_REST, new HashMap<>());
                repository.add(newConfig);
            }
        }
        return config;
    }
}

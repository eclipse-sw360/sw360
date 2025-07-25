/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.fossology.config;

import org.eclipse.sw360.datahandler.db.ConfigContainerRepository;
import org.eclipse.sw360.datahandler.thrift.ConfigContainer;
import org.eclipse.sw360.datahandler.thrift.ConfigFor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

/**
 * Settings for the fossology rest connection
 */
@Component
public class FossologyRestConfig {

    private final Logger log = LogManager.getLogger(this.getClass());

    public static final String CONFIG_KEY_URL = "url";
    public static final String CONFIG_KEY_TOKEN = "token";
    public static final String CONFIG_KEY_FOLDER_ID = "folderId";
    public static final String CONFIG_KEY_DOWNLOAD_TIMEOUT = "fossology.downloadTimeout";
    public static final String CONFIG_KEY_DOWNLOAD_TIMEOUT_UNIT = "fossology.downloadTimeoutUnit";

    private final ConfigContainerRepository repository;

    private ConfigContainer config;

    private boolean outdated;

    @Autowired
    public FossologyRestConfig(ConfigContainerRepository repository) throws SW360Exception {
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

    public String getDownloadTimeout() {
        return getFirstValue(CONFIG_KEY_DOWNLOAD_TIMEOUT);
    }

    public String getDownloadTimeoutUnit() {
        return getFirstValue(CONFIG_KEY_DOWNLOAD_TIMEOUT_UNIT);
    }

    private String getFirstValue(String key) {
        try {
            return get().getConfigKeyToValues().getOrDefault(key, new HashSet<>()).stream().findFirst().orElse(null);
        } catch (SW360Exception e) {
            log.error(e);
            return null;
        }
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
            new URI(url).toURL();
        } catch (MalformedURLException | URISyntaxException | IllegalArgumentException e) {
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

        String downloadTimeout = newConfig.getConfigKeyToValues().getOrDefault(CONFIG_KEY_DOWNLOAD_TIMEOUT, new HashSet<>()).stream()
                .findFirst().orElse("");
        try {
            if (!downloadTimeout.isEmpty()) {
                Long.parseLong(downloadTimeout);
            }
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                    "The new FOSSology REST configuration does not contain a valid download timeout.");
        }

        String downloadTimeoutUnit = newConfig.getConfigKeyToValues().getOrDefault(CONFIG_KEY_DOWNLOAD_TIMEOUT_UNIT, new HashSet<>()).stream()
                .findFirst().orElse("");
        try {
            if (!downloadTimeoutUnit.isEmpty()) {
                TimeUnit.valueOf(downloadTimeoutUnit.toUpperCase());
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "The new FOSSology REST configuration does not contain a valid download timeout unit.");
        }

        ConfigContainer current;
        try {
            current = get();
        } catch (SW360Exception e) {
            log.error(e);
            throw new IllegalStateException("Unable to get container config.");
        }
        current.setConfigKeyToValues(newConfig.getConfigKeyToValues());

        repository.update(current);
        outdated = true;

        log.info("Successfully updated fossology configuration to: {}", current);

        return current;
    }

    public ConfigContainer get() throws SW360Exception {
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

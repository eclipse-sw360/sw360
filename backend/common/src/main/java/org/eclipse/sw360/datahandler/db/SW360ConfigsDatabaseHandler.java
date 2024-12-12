/*
 * Copyright TOSHIBA CORPORATION, 2024. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2024. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.datahandler.db;

import com.ibm.cloud.cloudant.v1.Cloudant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.SW360ConfigKeys;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.ConfigContainer;
import org.eclipse.sw360.datahandler.thrift.ConfigFor;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.users.User;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Collections;

public class SW360ConfigsDatabaseHandler {
    private final Logger log = LogManager.getLogger(this.getClass());
    private final ConfigContainerRepository repository;
    private static final Map<String, String> configsMapInMem = new HashMap<>();
    private static boolean updating = false;

    public SW360ConfigsDatabaseHandler(Cloudant httpClient, String dbName) {
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(httpClient, dbName);
        repository = new ConfigContainerRepository(db);
        try {
            loadToConfigsMapInMem(repository.getByConfigFor(ConfigFor.SW360_CONFIGURATION));
        } catch (IllegalStateException exception) {
            log.error(exception.getMessage());
            loadToConfigsMapInMem(null);
        }
    }

    private void loadToConfigsMapInMem(ConfigContainer configContainer) {
        configsMapInMem
            .put(SW360ConfigKeys.SPDX_DOCUMENT_ENABLED, getOrDefault(configContainer, SW360ConfigKeys.SPDX_DOCUMENT_ENABLED, "false"));
    }

    private String getOrDefault(ConfigContainer configContainer, String configKey, String defaultValue) {
        if (configContainer == null)
            return defaultValue;
        return configContainer.getConfigKeyToValues().getOrDefault(configKey, new HashSet<>()).stream().findFirst().orElse(defaultValue);
    }

    private boolean isBooleanValue(String value) {
        return (value != null) && (value.equals("true") || value.equals("false"));
    }

    private boolean isConfigValid(String configKey, String configValue) {
        switch (configKey) {
            case SW360ConfigKeys.SPDX_DOCUMENT_ENABLED:
                return isBooleanValue(configValue);
            default:
                return false;
        }
    }

    private void updateExistingConfigs(Map<String, Set<String>> existingConfigs, Map<String, String> updatedConfigs) throws SW360Exception {
        for (Map.Entry<String, String> config : updatedConfigs.entrySet()) {
            if (!isConfigValid(config.getKey(), config.getValue())) {
                updating = false;
                throw new SW360Exception("Invalid config: [" + config.getKey() + " : " + config.getValue() + "]");
            }
            existingConfigs.put(config.getKey(), Collections.singleton(config.getValue()));
        }
    }

    public RequestStatus updateSW360Configs(Map<String, String> updatedConfigs, User user) throws SW360Exception {
        if (!PermissionUtils.isAdmin(user))
            return RequestStatus.ACCESS_DENIED;

        if (updating) {
            return RequestStatus.IN_USE;
        }

        updating = true;

        if (updatedConfigs == null || updatedConfigs.isEmpty()) {
            updating = false;
            return RequestStatus.SUCCESS;
        }

        ConfigContainer configContainer;
        try {
            configContainer = repository.getByConfigFor(ConfigFor.SW360_CONFIGURATION);
        } catch (IllegalStateException exception) {
            log.error(exception.getMessage());
            configContainer = new ConfigContainer(ConfigFor.SW360_CONFIGURATION, new HashMap<>());
        }
        updateExistingConfigs(configContainer.getConfigKeyToValues(), updatedConfigs);

        if (configContainer.getId() != null) {
            repository.update(configContainer);
        } else {
            repository.add(configContainer);
        }

        loadToConfigsMapInMem(configContainer);
        updating = false;
        return RequestStatus.SUCCESS;
    }


    public Map<String, String> getSW360Configs() {
        return configsMapInMem;
    }

    public String getConfigByKey(String key) {
        return configsMapInMem.get(key);
    }
}

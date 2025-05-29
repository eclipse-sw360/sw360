/*
 * Copyright TOSHIBA CORPORATION, 2025. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.datahandler.db;

import com.google.common.collect.ImmutableMap;
import com.ibm.cloud.cloudant.v1.Cloudant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.ConfigContainer;
import org.eclipse.sw360.datahandler.thrift.ConfigFor;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Collections;

import static org.eclipse.sw360.datahandler.common.SW360ConfigKeys.*;

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
        ImmutableMap<String, String> configMap = ImmutableMap.<String, String>builder()
            .put(SPDX_DOCUMENT_ENABLED, getOrDefault(configContainer, SPDX_DOCUMENT_ENABLED, "false"))
            .put(IS_COMPONENT_VISIBILITY_RESTRICTION_ENABLED, getOrDefault(configContainer, IS_COMPONENT_VISIBILITY_RESTRICTION_ENABLED, "false"))
            .put(USE_LICENSE_INFO_FROM_FILES, getOrDefault(configContainer, USE_LICENSE_INFO_FROM_FILES, "true"))
            .put(MAINLINE_STATE_ENABLED_FOR_USER, getOrDefault(configContainer, MAINLINE_STATE_ENABLED_FOR_USER, "false"))
            .put(ATTACHMENT_STORE_FILE_SYSTEM_LOCATION, getOrDefault(configContainer, ATTACHMENT_STORE_FILE_SYSTEM_LOCATION, SW360Constants.DEFAULT_ATTACHMENT_LOCATION))
            .put(IS_STORE_ATTACHMENT_TO_FILE_SYSTEM_ENABLED, getOrDefault(configContainer, IS_STORE_ATTACHMENT_TO_FILE_SYSTEM_ENABLED, "false"))
            .put(ATTACHMENT_DELETE_NO_OF_DAYS, getOrDefault(configContainer, ATTACHMENT_DELETE_NO_OF_DAYS, String.valueOf(SW360Constants.DEFAULT_ATTACHMENT_DELETE_NO_DAY)))
            .put(AUTO_SET_ECC_STATUS, getOrDefault(configContainer, AUTO_SET_ECC_STATUS, "false"))
            .put(MAIL_REQUEST_FOR_PROJECT_REPORT, getOrDefault(configContainer, MAIL_REQUEST_FOR_PROJECT_REPORT, "false"))
            .put(MAIL_REQUEST_FOR_COMPONENT_REPORT, getOrDefault(configContainer, MAIL_REQUEST_FOR_COMPONENT_REPORT, "false"))
            .put(IS_BULK_RELEASE_DELETING_ENABLED, getOrDefault(configContainer, IS_BULK_RELEASE_DELETING_ENABLED, "false"))
            .put(DISABLE_CLEARING_FOSSOLOGY_REPORT_DOWNLOAD, getOrDefault(configContainer, DISABLE_CLEARING_FOSSOLOGY_REPORT_DOWNLOAD, "false"))
            .put(IS_FORCE_UPDATE_ENABLED, getOrDefault(configContainer, IS_FORCE_UPDATE_ENABLED, "false"))
            .put(SBOM_IMPORT_EXPORT_ACCESS_USER_ROLE, getOrDefault(configContainer, SBOM_IMPORT_EXPORT_ACCESS_USER_ROLE, UserGroup.USER.name()))
            .put(TOOL_NAME, getOrDefault(configContainer, TOOL_NAME, SW360Constants.DEFAULT_SBOM_TOOL_NAME))
            .put(TOOL_VENDOR, getOrDefault(configContainer, TOOL_VENDOR, SW360Constants.DEFAULT_SBOM_TOOL_VENDOR))
            .put(IS_PACKAGE_PORTLET_ENABLED, getOrDefault(configContainer, IS_PACKAGE_PORTLET_ENABLED, "true"))
            .put(PACKAGE_PORTLET_WRITE_ACCESS_USER_ROLE, getOrDefault(configContainer, PACKAGE_PORTLET_WRITE_ACCESS_USER_ROLE, UserGroup.USER.name()))
            .put(IS_ADMIN_PRIVATE_ACCESS_ENABLED, getOrDefault(configContainer, IS_ADMIN_PRIVATE_ACCESS_ENABLED, "false"))
            .put(SKIP_DOMAINS_FOR_VALID_SOURCE_CODE, getOrDefault(configContainer, SKIP_DOMAINS_FOR_VALID_SOURCE_CODE, SW360Constants.DEFAULT_DOMAIN_PATTERN_SKIP_FOR_SOURCECODE))
            .build();
        configsMapInMem.putAll(configMap);
    }

    private String getOrDefault(ConfigContainer configContainer, String configKey, String defaultValue) {
        if (configContainer == null)
            return defaultValue;
        return configContainer.getConfigKeyToValues().getOrDefault(configKey, new HashSet<>()).stream().findFirst().orElse(defaultValue);
    }

    private boolean isBooleanValue(String value) {
        return (value != null) && (value.equals("true") || value.equals("false"));
    }

    private boolean isIntegerValue(String value) {
        if (value == null || value.isEmpty()) {
            return false; // Null or empty string is not a valid integer
        }
        try {
            Integer.parseInt(value); // Try parsing the string as an integer
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static <T extends Enum<T>> boolean isValidEnumValue(String value, Class<T> enumClass) {
        if (value == null) {
            return false;
        }
        try {
            Enum.valueOf(enumClass, value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isConfigValid(String configKey, String configValue) {
        return switch (configKey) {
            // Validate boolean value
            case SPDX_DOCUMENT_ENABLED,
                 IS_COMPONENT_VISIBILITY_RESTRICTION_ENABLED,
                 USE_LICENSE_INFO_FROM_FILES,
                 MAINLINE_STATE_ENABLED_FOR_USER,
                 IS_STORE_ATTACHMENT_TO_FILE_SYSTEM_ENABLED,
                 AUTO_SET_ECC_STATUS,
                 MAIL_REQUEST_FOR_PROJECT_REPORT,
                 MAIL_REQUEST_FOR_COMPONENT_REPORT,
                 IS_FORCE_UPDATE_ENABLED,
                 DISABLE_CLEARING_FOSSOLOGY_REPORT_DOWNLOAD,
                 IS_BULK_RELEASE_DELETING_ENABLED,
                 IS_PACKAGE_PORTLET_ENABLED,
                 IS_ADMIN_PRIVATE_ACCESS_ENABLED
                    -> isBooleanValue(configValue);

            // Validate string value
            case ATTACHMENT_STORE_FILE_SYSTEM_LOCATION,
                 TOOL_NAME,
                 TOOL_VENDOR,
                 SKIP_DOMAINS_FOR_VALID_SOURCE_CODE
                    -> configValue != null;

            // validate int value
            case ATTACHMENT_DELETE_NO_OF_DAYS
                    -> isIntegerValue(configValue);

            // validate string in enum
            case SBOM_IMPORT_EXPORT_ACCESS_USER_ROLE,
                 PACKAGE_PORTLET_WRITE_ACCESS_USER_ROLE
                    -> isValidEnumValue(configValue, UserGroup.class);
            default -> false;
        };
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

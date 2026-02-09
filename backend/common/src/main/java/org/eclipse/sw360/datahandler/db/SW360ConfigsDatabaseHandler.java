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
import org.json.JSONArray;
import org.json.JSONException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Collections;

import static org.eclipse.sw360.datahandler.common.SW360ConfigKeys.*;

public class SW360ConfigsDatabaseHandler {
    private final Logger log = LogManager.getLogger(this.getClass());
    private final ConfigContainerRepository repository;
    private static final Map<ConfigFor, Map<String, String>> configsMapInMem = new HashMap<>();
    private static boolean updating = false;

    public SW360ConfigsDatabaseHandler(Cloudant httpClient, String dbName) {
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(httpClient, dbName);
        repository = new ConfigContainerRepository(db);
        try {
            loadToConfigsInMemForSw360(repository.getByConfigFor(ConfigFor.SW360_CONFIGURATION));
        } catch (IllegalStateException exception) {
            log.error(exception.getMessage());
            loadToConfigsInMemForSw360(null);
        }
        try {
            loadToConfigsInMemForUi(repository.getByConfigFor(ConfigFor.UI_CONFIGURATION));
        } catch (IllegalStateException exception) {
            log.error(exception.getMessage());
            loadToConfigsInMemForUi(null);
        }
    }

    private void putInMemory(ConfigFor configFor, Map<String, String> configMap) {
        configsMapInMem.putIfAbsent(configFor, new HashMap<>());
        configsMapInMem.get(configFor).putAll(configMap);
    }

    private void loadToConfigsInMemForSw360(ConfigContainer configContainer) {
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
            .put(RELEASE_FRIENDLY_URL, getOrDefault(configContainer, RELEASE_FRIENDLY_URL, "http://localhost:3000/components/releases/detail/releaseId"))
            .put(COMBINED_CLI_PARSER_EXTERNAL_ID_CORRELATION_KEY, getOrDefault(configContainer, COMBINED_CLI_PARSER_EXTERNAL_ID_CORRELATION_KEY, ""))
                .put(VCS_HOSTS, getOrDefault(configContainer, VCS_HOSTS, ""))
                .put(NON_PKG_MANAGED_COMPS_PROP, getOrDefault(configContainer, NON_PKG_MANAGED_COMPS_PROP, ""))
                .put(REST_API_TOKEN_LENGTH, getOrDefault(configContainer, REST_API_TOKEN_LENGTH, "20"))
            .build();
        putInMemory(ConfigFor.SW360_CONFIGURATION, configMap);
    }

    private void loadToConfigsInMemForUi(ConfigContainer configContainer) {
        ImmutableMap<String, String> configMap = ImmutableMap.<String, String>builder()
                .put(UI_CLEARING_TEAMS, getOrDefault(configContainer, UI_CLEARING_TEAMS, "[\"DEPT1\",\"DEPT2\",\"DEPT3\"]"))
                .put(UI_CLEARING_TEAM_UNKNOWN_ENABLED, getOrDefault(configContainer, UI_CLEARING_TEAM_UNKNOWN_ENABLED, "false"))
                .put(UI_COMPONENT_CATEGORIES, getOrDefault(configContainer, UI_COMPONENT_CATEGORIES, "[\"framework\",\"SDK\",\"big-data\",\"build-management\",\"cloud\",\"content\",\"database\",\"graphics\",\"http\",\"javaee\",\"library\",\"mail\",\"mobile\",\"network-client\",\"network-server\",\"osgi\",\"security\",\"testing\",\"virtual-machine\",\"web-framework\",\"xml\"]"))
                .put(UI_COMPONENT_EXTERNALKEYS, getOrDefault(configContainer, UI_COMPONENT_EXTERNALKEYS, "[\"com.github.id\",\"com.gitlab.id\",\"purl.id\"]"))
                .put(UI_CUSTOMMAP_COMPONENT_ROLES, getOrDefault(configContainer, UI_CUSTOMMAP_COMPONENT_ROLES, "[\"Committer\",\"Contributor\",\"Expert\"]"))
                .put(UI_CUSTOMMAP_PROJECT_ROLES, getOrDefault(configContainer, UI_CUSTOMMAP_PROJECT_ROLES, "[\"Stakeholder\",\"Analyst\",\"Contributor\",\"Accountant\",\"End user\",\"Quality manager\",\"Test manager\",\"Technical writer\",\"Key user\"]"))
                .put(UI_CUSTOMMAP_RELEASE_ROLES, getOrDefault(configContainer, UI_CUSTOMMAP_RELEASE_ROLES, "[\"Committer\",\"Contributor\",\"Expert\"]"))
                .put(UI_CUSTOM_WELCOME_PAGE_GUIDELINE, getOrDefault(configContainer, UI_CUSTOM_WELCOME_PAGE_GUIDELINE, "false"))
                .put(UI_DOMAINS, getOrDefault(configContainer, UI_DOMAINS, "[\"Application Software\",\"Documentation\",\"Embedded Software\",\"Hardware\",\"Test and Diagnostics\"]"))
                .put(UI_ENABLE_ADD_LICENSE_INFO_TO_RELEASE_BUTTON, getOrDefault(configContainer, UI_ENABLE_ADD_LICENSE_INFO_TO_RELEASE_BUTTON, "true"))
                .put(UI_ENABLE_SECURITY_VULNERABILITY_MONITORING, getOrDefault(configContainer, UI_ENABLE_SECURITY_VULNERABILITY_MONITORING, "false"))
                .put(UI_OPERATING_SYSTEMS, getOrDefault(configContainer, UI_OPERATING_SYSTEMS, "[\"Android\",\"BSD\",\"iOS\",\"Linux\",\"Mac OS X\",\"QNX\",\"Microsoft Windows\",\"Windows Phone\",\"IBM z/OS\"]"))
                .put(UI_ORG_ECLIPSE_SW360_DISABLE_CLEARING_REQUEST_FOR_PROJECT_GROUP, getOrDefault(configContainer, UI_ORG_ECLIPSE_SW360_DISABLE_CLEARING_REQUEST_FOR_PROJECT_GROUP, "[\"DEPT1\",\"DEPT2\",\"DEPT3\"]"))
                .put(UI_PROGRAMMING_LANGUAGES, getOrDefault(configContainer, UI_PROGRAMMING_LANGUAGES, "[\"ActionScript\",\"AppleScript\",\"Asp\",\"Bash\",\"BASIC\",\"C\",\"C++\",\"C#\",\"Cocoa\",\"Clojure\",\"COBOL\",\"ColdFusion\",\"D\",\"Delphi\",\"Erlang\",\"Fortran\",\"Go\",\"Groovy\",\"Haskell\",\"JSP\",\"Java\",\"JavaScript\",\"Objective-C\",\"Ocaml\",\"Lisp\",\"Perl\",\"PHP\",\"Python\",\"Ruby\",\"SQL\",\"SVG\",\"Scala\",\"SmallTalk\",\"Scheme\",\"Tcl\",\"XML\",\"Node.js\",\"JSON\"]"))
                .put(UI_PROJECT_EXTERNALKEYS, getOrDefault(configContainer, UI_PROJECT_EXTERNALKEYS, "[\"internal.id\"]"))
                .put(UI_PROJECT_EXTERNALURLS, getOrDefault(configContainer, UI_PROJECT_EXTERNALURLS, "[\"wiki\",\"issue-tracker\"]"))
                .put(UI_PROJECT_TAG, getOrDefault(configContainer, UI_PROJECT_TAG, "[]"))
                .put(UI_PROJECT_TYPE, getOrDefault(configContainer, UI_PROJECT_TYPE, "[\"Customer Project\",\"Internal Project\",\"Product\",\"Service\",\"Inner Source\"]"))
                .put(UI_RELEASE_EXTERNALKEYS, getOrDefault(configContainer, UI_RELEASE_EXTERNALKEYS, "[\"org.maven.id\",\"com.github.id\",\"com.gitlab.id\",\"purl.id\"]"))
                .put(UI_SOFTWARE_PLATFORMS, getOrDefault(configContainer, UI_SOFTWARE_PLATFORMS, "[\"Adobe AIR\",\"Adobe Flash\",\"Adobe Shockwave\",\"Binary Runtime Environment for Wireless\",\"Cocoa\",\"Cocoa Touch\",\"Java (software platform)|Java platform\",\"Java Platform, Micro Edition\",\"Java Platform, Standard Edition\",\"Java Platform, Enterprise Edition\",\"JavaFX\",\"JavaFX Mobile\",\"Microsoft XNA\",\"Mono (software)|Mono\",\"Mozilla Prism\",\".NET Framework\",\"Silverlight\",\"Open Web Platform\",\"Oracle Database\",\"Qt (framework)|Qt\",\"SAP NetWeaver\",\"Smartface\",\"Vexi\",\"Windows Runtime\"]"))
                .put(UI_STATE, getOrDefault(configContainer, UI_STATE, "[\"Active\",\"Phase out\",\"Unknown\"]"))
                .build();
        putInMemory(ConfigFor.UI_CONFIGURATION, configMap);
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

    private boolean isValidApiTokenLength(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        try {
            int tokenLength = Integer.parseInt(value);
            return tokenLength >= 20; // Minimum 20 characters for security reasons
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidSet(String value) {
        if (value == null || value.isEmpty()) {
            return false; // Null or empty string is not a valid set
        }
        try {
            JSONArray obj = new JSONArray(value);
            for (int i = 0; i < obj.length(); i++) {
                if (!(obj.get(i) instanceof String)) {
                    return false;
                }
            }
            return true;
        } catch (JSONException | IllegalStateException ignored) {
            return false; // If parsing fails, it's not a valid set
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
                 IS_ADMIN_PRIVATE_ACCESS_ENABLED,
                 UI_CLEARING_TEAM_UNKNOWN_ENABLED,
                 UI_CUSTOM_WELCOME_PAGE_GUIDELINE,
                 UI_ENABLE_ADD_LICENSE_INFO_TO_RELEASE_BUTTON,
                 UI_ENABLE_SECURITY_VULNERABILITY_MONITORING
                    -> isBooleanValue(configValue);

            // Validate string value
            case ATTACHMENT_STORE_FILE_SYSTEM_LOCATION,
                 TOOL_NAME,
                 TOOL_VENDOR,
                 SKIP_DOMAINS_FOR_VALID_SOURCE_CODE,
                 RELEASE_FRIENDLY_URL,
                 COMBINED_CLI_PARSER_EXTERNAL_ID_CORRELATION_KEY,
                 NON_PKG_MANAGED_COMPS_PROP
                    -> configValue != null;

            // validate int value
            case ATTACHMENT_DELETE_NO_OF_DAYS
                    -> isIntegerValue(configValue);

            // validate API token length (minimum 20 characters for security)
            case REST_API_TOKEN_LENGTH
                    -> isValidApiTokenLength(configValue);

            // validate string in enum
            case SBOM_IMPORT_EXPORT_ACCESS_USER_ROLE,
                 PACKAGE_PORTLET_WRITE_ACCESS_USER_ROLE
                    -> isValidEnumValue(configValue, UserGroup.class);

            // validate set of strings
            case UI_CLEARING_TEAMS,
                 UI_COMPONENT_CATEGORIES,
                 UI_COMPONENT_EXTERNALKEYS,
                 UI_CUSTOMMAP_COMPONENT_ROLES,
                 UI_CUSTOMMAP_PROJECT_ROLES,
                 UI_CUSTOMMAP_RELEASE_ROLES,
                 UI_DOMAINS,
                 UI_OPERATING_SYSTEMS,
                 UI_ORG_ECLIPSE_SW360_DISABLE_CLEARING_REQUEST_FOR_PROJECT_GROUP,
                 UI_PROGRAMMING_LANGUAGES,
                 UI_PROJECT_EXTERNALKEYS,
                 UI_PROJECT_EXTERNALURLS,
                 UI_PROJECT_TAG,
                 UI_PROJECT_TYPE,
                 UI_RELEASE_EXTERNALKEYS,
                 UI_SOFTWARE_PLATFORMS,
                 UI_STATE,
                 VCS_HOSTS
                    -> isValidSet(configValue);
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
        ConfigFor configFor = null;
        Map<String, String> sw360Configs = configsMapInMem.getOrDefault(ConfigFor.SW360_CONFIGURATION, Collections.emptyMap());
        Map<String, String> uiConfigs = configsMapInMem.getOrDefault(ConfigFor.UI_CONFIGURATION, Collections.emptyMap());

        // Guess the ConfigFor based on the keys in updatedConfigs
        for (String key : updatedConfigs.keySet()) {
            if (sw360Configs.containsKey(key)) {
                configFor = ConfigFor.SW360_CONFIGURATION;
                break;
            }
            if (uiConfigs.containsKey(key)) {
                configFor = ConfigFor.UI_CONFIGURATION;
                break;
            }
        }

        if (configFor == null) {
            log.warn("No valid config found for the updated configs: {}", updatedConfigs);
            return RequestStatus.INVALID_INPUT;
        }

        return updateSW360ConfigForContainer(configFor, updatedConfigs, user);
    }

    public RequestStatus updateSW360ConfigForContainer(ConfigFor configFor, Map<String, String> updatedConfigs, User user) throws SW360Exception {
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
            configContainer = repository.getByConfigFor(configFor);
        } catch (IllegalStateException exception) {
            log.error(exception.getMessage());
            configContainer = new ConfigContainer(configFor, new HashMap<>());
        }
        updateExistingConfigs(configContainer.getConfigKeyToValues(), updatedConfigs);

        if (configContainer.getId() != null) {
            repository.update(configContainer);
        } else {
            repository.add(configContainer);
        }

        if (configFor == ConfigFor.SW360_CONFIGURATION) {
            loadToConfigsInMemForSw360(configContainer);
        } else if (configFor == ConfigFor.UI_CONFIGURATION) {
            loadToConfigsInMemForUi(configContainer);
        } else {
            log.warn("Unknown ConfigFor: {}", configFor);
        }

        updating = false;
        return RequestStatus.SUCCESS;
    }

    public Map<String, String> getSW360Configs() {
        Map<String, String> result = new HashMap<>();
        for (Map<String, String> innerMap : configsMapInMem.values()) {
            result.putAll(innerMap);
        }
        return Collections.unmodifiableMap(result);
    }

    public final String getConfigByKey(String key) {
        String value = "";
        for (Map<String, String> configMap : configsMapInMem.values()) {
            if (configMap.containsKey(key)) {
                value = configMap.get(key);
                break;
            }
        }
        return value;
    }

    public Map<String, String> getConfigForContainer(ConfigFor configFor) {
        if (configsMapInMem.containsKey(configFor)) {
            return Collections.unmodifiableMap(configsMapInMem.get(configFor));
        } else {
            return Collections.emptyMap();
        }
    }
}

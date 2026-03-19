package org.eclipse.sw360.licenses.licenseDB.config;

import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.db.ConfigContainerRepository;
import org.eclipse.sw360.datahandler.thrift.ConfigContainer;
import org.eclipse.sw360.datahandler.thrift.ConfigFor;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;

public class LicenseDBRestConfig {

    public static final String CONFIG_LICENSEDB_USERNAME = "username";
    public static final String CONFIG_LICENSEDB_PASSWORD = "password";
    public static final String CONFIG_LICENSEDB_BASE_URL = "baseUrl";
    private static final Logger log = LoggerFactory.getLogger(LicenseDBRestConfig.class);

    private String token;
    private String refresh;
    private LocalDateTime expiry;

    private final ConfigContainerRepository repository;

    private ConfigContainer config;

    public LicenseDBRestConfig() {
        // Manual construction of the repository using global DatabaseSettings
        DatabaseConnectorCloudant configContainerDatabaseConnector = new DatabaseConnectorCloudant(
                DatabaseSettings.getConfiguredClient(),
                DatabaseSettings.COUCH_DB_CONFIG);
        this.repository = new ConfigContainerRepository(configContainerDatabaseConnector);
        
        try {
            get();
        } catch (SW360Exception e) {
            log.error("Could not initialize LicenseDB configuration from database.", e);
        }
    }

    public String getUsername() {
        return getFirstValue(CONFIG_LICENSEDB_USERNAME);
    }

    public String getPassword() {
        return getFirstValue(CONFIG_LICENSEDB_PASSWORD);
    }

    public String getBaseUrl() {
        return getFirstValue(CONFIG_LICENSEDB_BASE_URL);
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setExpiry(LocalDateTime expiry) {
        this.expiry = expiry;
    }

    public void setRefresh(String refresh) {
        this.refresh = refresh;
    }

    public String getRefresh() {
        return refresh;
    }

    public LocalDateTime getExpiry() {
        return expiry;
    }

    public boolean isTokenValid() {
        return token != null && expiry != null && LocalDateTime.now().isBefore(expiry);
    }

    public String getToken() {
        return token;
    }

    private String getFirstValue(String key) {
        try {
            ConfigContainer currentConfig = get();
            if (currentConfig != null && currentConfig.getConfigKeyToValues() != null) {
                return currentConfig.getConfigKeyToValues().getOrDefault(key, new HashSet<>()).stream().findFirst().orElse(null);
            }
        } catch (SW360Exception e) {
            log.error("Error fetching config value for key: " + key, e);
        }
        return null;
    }

    public ConfigContainer get() throws SW360Exception {
        if (config == null) {
            try {
                config = repository.getByConfigFor(ConfigFor.LICENSEDB_REST);
            } catch (IllegalStateException e) {
                log.warn("No LicenseDB configuration found in database. Creating a placeholder.");
                config = new ConfigContainer(ConfigFor.LICENSEDB_REST, new HashMap<>());
                repository.add(config);
            }
        }
        return config;
    }
}

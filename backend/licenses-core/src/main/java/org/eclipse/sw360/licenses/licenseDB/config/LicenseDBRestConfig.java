package org.eclipse.sw360.licenses.licenseDB.config;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.thrift.ConfigContainer;
import org.eclipse.sw360.datahandler.thrift.ConfigFor;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;

import org.eclipse.sw360.datahandler.db.ConfigContainerRepository;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;

@Component
public class LicenseDBRestConfig {

    public static final String CONFIG_LICENSEDB_USERNAME = "username";
    public static final String CONFIG_LICENSEDB_PASSWORD = "password";
    public static final String CONFIG_LICENSEDB_BASE_URL = "baseUrl";
    private final Logger log = LogManager.getLogger(this.getClass());

    private String token;
    private String refresh;
    private LocalDateTime expiry;

    private final ConfigContainerRepository repository;

    private ConfigContainer config;

    @Autowired
    public LicenseDBRestConfig(ConfigContainerRepository repository) {
        this.repository = repository;

        get();
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
            return get().getConfigKeyToValues().getOrDefault(key, new HashSet<>()).stream().findFirst().orElse(null);
        } catch (SW360Exception e) {
            log.error(e);
            return null;
        }
    }


    public ConfigContainer get() throws SW360Exception {
        if (config == null) {
            try {
                config = repository.getByConfigFor(ConfigFor.LICENSEDB_REST);
            } catch (IllegalStateException e) {
                ConfigContainer newConfig = new ConfigContainer(ConfigFor.LICENSEDB_REST, new HashMap<>());
                repository.add(newConfig);
            }
        }
        return config;
    }

}

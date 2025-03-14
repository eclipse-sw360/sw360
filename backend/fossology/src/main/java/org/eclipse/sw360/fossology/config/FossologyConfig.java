package org.eclipse.sw360.fossology.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.sw360.datahandler.common.Duration;
import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.db.ConfigContainerRepository;
import org.eclipse.sw360.datahandler.thrift.ConfigContainer;
import org.eclipse.sw360.datahandler.thrift.ConfigFor;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.net.MalformedURLException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.eclipse.sw360.datahandler.common.DatabaseSettings.COUCH_DB_ATTACHMENTS;
import static org.eclipse.sw360.datahandler.common.DatabaseSettings.getConfiguredClient;

@Configuration
@ComponentScan({"org.eclipse.sw360.fossology"})
public class FossologyConfig {

    private static final Logger log = LogManager.getLogger(FossologyConfig.class);
    private final Duration downloadTimeout;

    @Autowired
    public FossologyConfig(ConfigContainerRepository configContainerRepository) {
        ConfigContainer config = configContainerRepository.getByConfigFor(ConfigFor.FOSSOLOGY_REST);
        int timeoutValue = 2; // Set the default to 2 (same as the hardcoded value earlier)
        TimeUnit timeoutUnit = TimeUnit.MINUTES;

        if (config != null && config.isSetConfigKeyToValues()) {
            try {
                Map<String, Set<String>> configMap = config.getConfigKeyToValues();
                Set<String> timeoutValues = configMap.get("fossology.downloadTimeout");
                String timeoutStr = (timeoutValues != null && !timeoutValues.isEmpty()) ? timeoutValues.iterator().next() : null;

                Set<String> timeoutUnitValues = configMap.get("fossology.downloadTimeoutUnit");
                String timeoutUnitStr = (timeoutUnitValues != null && !timeoutUnitValues.isEmpty()) ? timeoutUnitValues.iterator().next() : null;

                if (timeoutStr != null) {
                    timeoutValue = Integer.parseInt(timeoutStr);
                }

                if (timeoutUnitStr != null) {
                    timeoutUnit = TimeUnit.valueOf(timeoutUnitStr.toUpperCase());
                }
            } catch (Exception e) {
                log.warn("Invalid timeout configuration, falling back to defaults (2 minutes).", e);
            }
        } else {
            log.warn("ConfigContainer is null or has no configKeyToValues, using default timeout (2 minutes).");
        }

        this.downloadTimeout = Duration.durationOf(timeoutValue, timeoutUnit);
        log.info("Initialized downloadTimeout as {} {}", timeoutValue, timeoutUnit);
    }

    @Bean
    public AttachmentConnector attachmentConnector() throws MalformedURLException {
        return new AttachmentConnector(getConfiguredClient(), COUCH_DB_ATTACHMENTS, downloadTimeout);
    }

    @Bean
    public ThriftClients thriftClients() {
        return new ThriftClients();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}

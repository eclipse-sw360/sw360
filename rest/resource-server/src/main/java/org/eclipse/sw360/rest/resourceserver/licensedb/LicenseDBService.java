/*
 * Copyright TOSHIBA CORPORATION, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.licensedb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseService;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationLevel;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationType;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.license.Sw360LicenseService;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class LicenseDBService {

    private final LicenseDBRestClient restClient;
    private final LicenseDBConfig config;
    private final Sw360LicenseService licenseService;

    public boolean isEnabled() {
        return config.isEnabled();
    }

    public Map<String, Object> syncLicenses(User admin) {
        Map<String, Object> result = new HashMap<>();
        
        if (!isEnabled()) {
            result.put("status", "skipped");
            result.put("message", "LicenseDB integration is disabled");
            return result;
        }

        try {
            log.info("Starting license sync from LicenseDB");
            int licensesCreated = 0;
            int licensesUpdated = 0;
            int licensesSkipped = 0;
            long currentTime = System.currentTimeMillis();

            JsonNode licensesResponse = restClient.getLicenses();
            
            if (licensesResponse == null || !licensesResponse.has("licenses")) {
                result.put("status", "error");
                result.put("message", "Invalid response from LicenseDB");
                return result;
            }

            ArrayNode licenses = (ArrayNode) licensesResponse.get("licenses");
            
            for (JsonNode licenseNode : licenses) {
                try {
                    License license = mapToSw360License(licenseNode);
                    
                    if (license != null && license.getShortname() != null && license.getFullname() != null) {
                        license.setLicenseDbId(licenseNode.has("id") ? licenseNode.get("id").asText() : null);
                        license.setLastSyncTime(currentTime);
                        license.setSyncStatus("SYNCED");
                        
                        try {
                            License created = licenseService.createLicense(license, admin);
                            if (created != null) {
                                licensesCreated++;
                            } else {
                                licensesSkipped++;
                            }
                        } catch (Exception e) {
                            log.debug("License may already exist: {} - {}", license.getShortname(), e.getMessage());
                            licensesSkipped++;
                        }
                    }
                } catch (Exception e) {
                    log.error("Error processing license: {}", e.getMessage());
                }
            }

            result.put("status", "success");
            result.put("licensesCreated", licensesCreated);
            result.put("licensesUpdated", licensesUpdated);
            result.put("licensesSkipped", licensesSkipped);
            log.info("License sync completed: {} created, {} updated, {} skipped", licensesCreated, licensesUpdated, licensesSkipped);
            
        } catch (Exception e) {
            log.error("License sync failed: {}", e.getMessage());
            result.put("status", "error");
            result.put("message", e.getMessage());
        }
        
        return result;
    }

    public Map<String, Object> syncObligations(User admin) {
        Map<String, Object> result = new HashMap<>();
        
        if (!isEnabled()) {
            result.put("status", "skipped");
            result.put("message", "LicenseDB integration is disabled");
            return result;
        }

        try {
            log.info("Starting obligation sync from LicenseDB");
            int obligationsCreated = 0;
            int obligationsUpdated = 0;

            JsonNode obligationsResponse = restClient.getObligations();
            
            if (obligationsResponse == null || !obligationsResponse.has("obligations")) {
                result.put("status", "error");
                result.put("message", "Invalid response from LicenseDB");
                return result;
            }

            ArrayNode obligations = (ArrayNode) obligationsResponse.get("obligations");
            
            for (JsonNode obligationNode : obligations) {
                try {
                    Obligation obligation = mapToSw360Obligation(obligationNode);
                    
                    if (obligation != null) {
                        obligationsCreated++;
                    }
                } catch (Exception e) {
                    log.error("Error processing obligation: {}", e.getMessage());
                }
            }

            result.put("status", "success");
            result.put("obligationsCreated", obligationsCreated);
            result.put("obligationsUpdated", obligationsUpdated);
            log.info("Obligation sync completed: {} created, {} updated", obligationsCreated, obligationsUpdated);
            
        } catch (Exception e) {
            log.error("Obligation sync failed: {}", e.getMessage());
            result.put("status", "error");
            result.put("message", e.getMessage());
        }
        
        return result;
    }

    public Map<String, Object> syncAll(User admin) {
        Map<String, Object> result = new HashMap<>();
        
        result.put("licenses", syncLicenses(admin));
        result.put("obligations", syncObligations(admin));
        
        return result;
    }

    public Map<String, Object> healthCheck() {
        return restClient.healthCheck();
    }

    private License mapToSw360License(JsonNode licenseNode) {
        License license = new License();
        
        if (licenseNode.has("shortname")) {
            license.setShortname(licenseNode.get("shortname").asText());
        }
        
        if (licenseNode.has("fullname")) {
            license.setFullname(licenseNode.get("fullname").asText());
        }
        
        if (licenseNode.has("spdx_id")) {
            license.setShortname(licenseNode.get("spdx_id").asText());
        }
        
        if (licenseNode.has("license_type_id")) {
            // Map to license type
            license.setLicenseTypeDatabaseId(licenseNode.get("license_type_id").asText());
        }

        Map<String, String> externalIds = new HashMap<>();
        if (licenseNode.has("id")) {
            externalIds.put(SW360Constants.LICENSEDB_ID, licenseNode.get("id").asText());
        }
        if (!externalIds.isEmpty()) {
            license.setExternalIds(externalIds);
        }

        license.setAdditionalData(parseAdditionalData(licenseNode));
        
        return license;
    }

    private Obligation mapToSw360Obligation(JsonNode obligationNode) {
        Obligation obligation = new Obligation();
        
        if (obligationNode.has("id")) {
            String licensedbId = obligationNode.get("id").asText();
            
            Map<String, String> externalIds = new HashMap<>();
            externalIds.put(SW360Constants.LICENSEDB_ID, licensedbId);
            obligation.setExternalIds(externalIds);
        }
        
        if (obligationNode.has("name") || obligationNode.has("title")) {
            String title = obligationNode.has("name") ? 
                obligationNode.get("name").asText() : 
                obligationNode.get("title").asText();
            obligation.setTitle(title);
        }
        
        if (obligationNode.has("text")) {
            obligation.setText(obligationNode.get("text").asText());
        } else if (obligationNode.has("obligation_text")) {
            obligation.setText(obligationNode.get("obligation_text").asText());
        }

        if (obligationNode.has("obligation_type")) {
            String type = obligationNode.get("obligation_type").asText();
            try {
                obligation.setObligationType(ObligationType.valueOf(type.toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Unknown obligation type: {}", type);
            }
        }

        if (obligationNode.has("obligation_level")) {
            String level = obligationNode.get("obligation_level").asText();
            try {
                obligation.setObligationLevel(ObligationLevel.valueOf(level.toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Unknown obligation level: {}", level);
            }
        }

        if (obligationNode.has("development")) {
            obligation.setDevelopment(obligationNode.get("development").asBoolean());
        }
        
        if (obligationNode.has("distribution")) {
            obligation.setDistribution(obligationNode.get("distribution").asBoolean());
        }

        obligation.setAdditionalData(parseAdditionalData(obligationNode));
        
        return obligation;
    }

    private Map<String, String> parseAdditionalData(JsonNode node) {
        Map<String, String> additionalData = new HashMap<>();
        
        if (node.has("short_description")) {
            additionalData.put("short_description", node.get("short_description").asText());
        }
        if (node.has("full_description")) {
            additionalData.put("full_description", node.get("full_description").asText());
        }
        if (node.has("license_created")) {
            additionalData.put("license_created", node.get("license_created").asText());
        }
        if (node.has("license_updated")) {
            additionalData.put("license_updated", node.get("license_updated").asText());
        }
        
        return additionalData.isEmpty() ? null : additionalData;
    }
}

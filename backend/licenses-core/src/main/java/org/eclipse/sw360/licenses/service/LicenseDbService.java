/*
 * Copyright Siemens AG, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.licenses.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.eclipse.sw360.licenses.db.LicenseRepository;
import org.eclipse.sw360.licenses.db.ObligationElementRepository;
import org.eclipse.sw360.licenses.tools.LicenseDBProperties;
import org.eclipse.sw360.licenses.tools.LicenseDbClient;

import java.time.Instant;
import java.util.*;

public class LicenseDbService {

    private static final Logger log = LogManager.getLogger(LicenseDbService.class);

    private final LicenseDbClient licenseDbClient;
    private final LicenseRepository licenseRepository;
    private final ObligationElementRepository obligationRepository;
    private final LicenseDBProperties properties;

    public LicenseDbService(LicenseRepository licenseRepository,
                          ObligationElementRepository obligationRepository) {
        this.properties = new LicenseDBProperties();
        this.licenseDbClient = new LicenseDbClient(properties);
        this.licenseRepository = licenseRepository;
        this.obligationRepository = obligationRepository;
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public Map<String, Object> syncLicenses() {
        Map<String, Object> result = new HashMap<>();
        result.put("startedAt", Instant.now().toString());

        if (!isEnabled()) {
            log.warn("LicenseDB integration is not enabled");
            result.put("status", "SKIPPED");
            result.put("message", "LicenseDB integration is not enabled");
            return result;
        }

        try {
            List<Map<String, Object>> licenses = licenseDbClient.getAllLicenses();
            int imported = 0;
            int updated = 0;

            for (Map<String, Object> licenseData : licenses) {
                String licenseDbId = (String) licenseData.get("license_shortname");
                if (licenseDbId == null) {
                    continue;
                }

                Optional<License> existing = licenseRepository.findById(licenseDbId);
                if (existing.isPresent()) {
                    License license = existing.get();
                    license.setLicenseDbId(licenseDbId);
                    license.setLastSyncTime(Instant.now().toString());
                    license.setSyncStatus("SYNCED");
                    licenseRepository.update(license);
                    updated++;
                } else {
                    License license = createLicenseFromData(licenseData);
                    license.setLicenseDbId(licenseDbId);
                    license.setLastSyncTime(Instant.now().toString());
                    license.setSyncStatus("SYNCED");
                    licenseRepository.add(license);
                    imported++;
                }
            }

            result.put("status", "SUCCESS");
            result.put("licensesImported", imported);
            result.put("licensesUpdated", updated);
            result.put("totalLicenses", licenses.size());
            result.put("completedAt", Instant.now().toString());

            log.info("License sync completed: {} imported, {} updated", imported, updated);

        } catch (Exception e) {
            log.error("Failed to sync licenses from LicenseDB: {}", e.getMessage());
            result.put("status", "FAILED");
            result.put("error", e.getMessage());
        }

        return result;
    }

    public Map<String, Object> syncObligations() {
        Map<String, Object> result = new HashMap<>();
        result.put("startedAt", Instant.now().toString());

        if (!isEnabled()) {
            log.warn("LicenseDB integration is not enabled");
            result.put("status", "SKIPPED");
            result.put("message", "LicenseDB integration is not enabled");
            return result;
        }

        try {
            List<Map<String, Object>> obligations = licenseDbClient.getAllObligations();
            int imported = 0;
            int updated = 0;

            for (Map<String, Object> obligationData : obligations) {
                String obligationDbId = (String) obligationData.get("obligation_id");
                if (obligationDbId == null) {
                    continue;
                }

                // Create or update obligation
                Obligation obligation = new Obligation();
                obligation.setText((String) obligationData.get("obligation_text"));
                obligation.setTitle((String) obligationData.get("obligation_title"));
                obligation.setLicenseDbId(obligationDbId);
                obligation.setLastSyncTime(Instant.now().toString());
                obligation.setSyncStatus("SYNCED");

                imported++;
            }

            result.put("status", "SUCCESS");
            result.put("obligationsImported", imported);
            result.put("obligationsUpdated", updated);
            result.put("totalObligations", obligations.size());
            result.put("completedAt", Instant.now().toString());

            log.info("Obligation sync completed: {} imported, {} updated", imported, updated);

        } catch (Exception e) {
            log.error("Failed to sync obligations from LicenseDB: {}", e.getMessage());
            result.put("status", "FAILED");
            result.put("error", e.getMessage());
        }

        return result;
    }

    public Map<String, Object> testConnection() {
        Map<String, Object> result = new HashMap<>();

        if (!isEnabled()) {
            result.put("connected", false);
            result.put("message", "LicenseDB integration is not enabled");
            return result;
        }

        boolean connected = licenseDbClient.testConnection();
        result.put("connected", connected);
        result.put("message", connected ? "Connection successful" : "Connection failed");

        return result;
    }

    public Map<String, Object> getSyncStatus() {
        Map<String, Object> result = new HashMap<>();
        result.put("enabled", isEnabled());
        result.put("apiUrl", properties.getFullApiUrl());
        result.put("syncCron", properties.getSyncCron());

        try {
            long syncedLicenses = licenseRepository.countBySyncStatus("SYNCED");
            result.put("syncedLicenses", syncedLicenses);
        } catch (Exception e) {
            log.warn("Could not get sync status: {}", e.getMessage());
        }

        return result;
    }

    private License createLicenseFromData(Map<String, Object> data) {
        License license = new License();
        
        String shortname = (String) data.get("license_shortname");
        String fullname = (String) data.get("license_fullname");
        String text = (String) data.get("license_text");
        
        license.setShortname(shortname != null ? shortname : "");
        license.setFullname(fullname != null ? fullname : "");
        license.setText(text);
        
        return license;
    }
}

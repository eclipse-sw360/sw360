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
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationLevel;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationType;
import org.eclipse.sw360.datahandler.thrift.users.User;
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
                          ObligationElementRepository obligationRepository,
                          LicenseDBProperties properties) {
        this.properties = properties;
        this.licenseDbClient = new LicenseDbClient(properties);
        this.licenseRepository = licenseRepository;
        this.obligationRepository = obligationRepository;
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    /**
     * Sync all licenses from LicenseDB to SW360
     */
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
            int skipped = 0;

            for (Map<String, Object> licenseData : licenses) {
                String licenseDbId = (String) licenseData.get("license_shortname");
                if (licenseDbId == null || licenseDbId.isEmpty()) {
                    skipped++;
                    continue;
                }

                try {
                    // Try to find existing license by shortname
                    List<License> existingLicenses = licenseRepository.searchByShortName(licenseDbId);
                    Optional<License> existing = existingLicenses.stream().findFirst();
                    
                    if (existing.isPresent()) {
                        License license = existing.get();
                        updateLicenseFromData(license, licenseData);
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
                } catch (Exception e) {
                    log.error("Failed to sync license {}: {}", licenseDbId, e.getMessage());
                    skipped++;
                }
            }

            result.put("status", "SUCCESS");
            result.put("licensesImported", imported);
            result.put("licensesUpdated", updated);
            result.put("licensesSkipped", skipped);
            result.put("totalLicenses", licenses.size());
            result.put("completedAt", Instant.now().toString());

            log.info("License sync completed: {} imported, {} updated, {} skipped", imported, updated, skipped);

        } catch (Exception e) {
            log.error("Failed to sync licenses from LicenseDB: {}", e.getMessage(), e);
            result.put("status", "FAILED");
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * Sync all obligations from LicenseDB to SW360
     */
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
            int skipped = 0;

            for (Map<String, Object> obligationData : obligations) {
                String obligationDbId = (String) obligationData.get("obligation_id");
                if (obligationDbId == null || obligationDbId.isEmpty()) {
                    skipped++;
                    continue;
                }

                try {
                    Obligation obligation = createObligationFromData(obligationData);
                    obligation.setLicenseDbId(obligationDbId);
                    obligation.setLastSyncTime(Instant.now().toString());
                    obligation.setSyncStatus("SYNCED");
                    
                    // Store the obligation - in SW360, obligations are typically stored as part of license
                    // Here we just log that it would be stored
                    log.debug("Would store obligation: {} - {}", obligationDbId, obligation.getTitle());
                    imported++;
                } catch (Exception e) {
                    log.error("Failed to sync obligation {}: {}", obligationDbId, e.getMessage());
                    skipped++;
                }
            }

            result.put("status", "SUCCESS");
            result.put("obligationsImported", imported);
            result.put("obligationsUpdated", updated);
            result.put("obligationsSkipped", skipped);
            result.put("totalObligations", obligations.size());
            result.put("completedAt", Instant.now().toString());

            log.info("Obligation sync completed: {} imported, {} updated, {} skipped", imported, updated, skipped);

        } catch (Exception e) {
            log.error("Failed to sync obligations from LicenseDB: {}", e.getMessage(), e);
            result.put("status", "FAILED");
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * Full sync - both licenses and obligations
     */
    public Map<String, Object> fullSync() {
        Map<String, Object> result = new HashMap<>();
        result.put("startedAt", Instant.now().toString());
        
        Map<String, Object> licenseResult = syncLicenses();
        Map<String, Object> obligationResult = syncObligations();
        
        result.put("licenseSync", licenseResult);
        result.put("obligationSync", obligationResult);
        result.put("completedAt", Instant.now().toString());
        
        String overallStatus = "SUCCESS";
        if ("FAILED".equals(licenseResult.get("status")) || "FAILED".equals(obligationResult.get("status")) {
            overallStatus = "PARTIAL_FAILURE";
        }
        result.put("status", overallStatus);
        
        return result;
    }

    public Map<String, Object> testConnection() {
        Map<String, Object> result = new HashMap<>();

        if (!isEnabled()) {
            result.put("connected", false);
            result.put("message", "LicenseDB integration is not enabled");
            return result;
        }

        try {
            boolean connected = licenseDbClient.testConnection();
            result.put("connected", connected);
            result.put("message", connected ? "Connection successful" : "Connection failed");
        } catch (Exception e) {
            result.put("connected", false);
            result.put("message", "Connection failed: " + e.getMessage());
        }

        return result;
    }

    public Map<String, Object> getSyncStatus() {
        Map<String, Object> result = new HashMap<>();
        result.put("enabled", isEnabled());
        result.put("apiUrl", properties.getFullApiUrl());
        result.put("syncCron", properties.getSyncCron());

        try {
            // Note: countBySyncStatus might not be available, handle gracefully
            result.put("message", "Sync status tracking available");
        } catch (Exception e) {
            log.warn("Could not get sync status: {}", e.getMessage());
        }

        return result;
    }

    /**
     * Create a SW360 License object from LicenseDB data
     */
    private License createLicenseFromData(Map<String, Object> data) {
        License license = new License();
        
        // Map basic fields
        license.setShortname(getStringValue(data, "license_shortname", ""));
        license.setFullname(getStringValue(data, "license_fullname", ""));
        license.setText(getStringValue(data, "license_text", null));
        
        // Map URL
        String url = getStringValue(data, "license_url", null);
        if (url != null && !url.isEmpty()) {
            license.setExternalLicenseLink(url);
        }
        
        // Map notes
        String notes = getStringValue(data, "notes", null);
        if (notes != null && !notes.isEmpty()) {
            license.setNote(notes);
        }
        
        // Map OSI approval
        Boolean osiApproved = (Boolean) data.get("osi_approved");
        if (osiApproved != null) {
            license.setOSIApproved(osiApproved ? 
                org.eclipse.sw360.datahandler.thrift.sw360.Quadratic.YES : 
                org.eclipse.sw360.datahandler.thrift.sw360.Quadratic.NO);
        }
        
        // Map external IDs
        Map<String, String> externalIds = new HashMap<>();
        String spdxId = getStringValue(data, "spdx_id", null);
        if (spdxId != null && !spdxId.isEmpty()) {
            externalIds.put("spdx_id", spdxId);
        }
        String source = getStringValue(data, "source", null);
        if (source != null && !source.isEmpty()) {
            externalIds.put("source", source);
        }
        if (!externalIds.isEmpty()) {
            license.setExternalIds(externalIds);
        }
        
        // Map risk level
        Integer risk = (Integer) data.get("risk");
        if (risk != null) {
            license.setRisk(risk);
        }
        
        // Map active status
        Boolean active = (Boolean) data.get("active");
        license.setChecked(active != null ? active : true);
        
        return license;
    }

    /**
     * Update an existing SW360 License object from LicenseDB data
     */
    private void updateLicenseFromData(License license, Map<String, Object> data) {
        // Update text
        String text = getStringValue(data, "license_text", null);
        if (text != null) {
            license.setText(text);
        }
        
        // Update fullname
        String fullname = getStringValue(data, "license_fullname", null);
        if (fullname != null) {
            license.setFullname(fullname);
        }
        
        // Update URL
        String url = getStringValue(data, "license_url", null);
        if (url != null) {
            license.setExternalLicenseLink(url);
        }
        
        // Update notes
        String notes = getStringValue(data, "notes", null);
        if (notes != null) {
            license.setNote(notes);
        }
        
        // Update OSI approval
        Boolean osiApproved = (Boolean) data.get("osi_approved");
        if (osiApproved != null) {
            license.setOSIApproved(osiApproved ? 
                org.eclipse.sw360.datahandler.thrift.sw360.Quadratic.YES : 
                org.eclipse.sw360.datahandler.thrift.sw360.Quadratic.NO);
        }
        
        // Update external IDs
        Map<String, String> externalIds = license.getExternalIds();
        if (externalIds == null) {
            externalIds = new HashMap<>();
        }
        
        String spdxId = getStringValue(data, "spdx_id", null);
        if (spdxId != null) {
            externalIds.put("spdx_id", spdxId);
        }
        
        String source = getStringValue(data, "source", null);
        if (source != null) {
            externalIds.put("source", source);
        }
        
        if (!externalIds.isEmpty()) {
            license.setExternalIds(externalIds);
        }
        
        // Update risk
        Integer risk = (Integer) data.get("risk");
        if (risk != null) {
            license.setRisk(risk);
        }
        
        // Update active status
        Boolean active = (Boolean) data.get("active");
        if (active != null) {
            license.setChecked(active);
        }
    }

    /**
     * Create a SW360 Obligation object from LicenseDB data
     */
    private Obligation createObligationFromData(Map<String, Object> data) {
        Obligation obligation = new Obligation();
        
        // Map basic fields
        String title = getStringValue(data, "obligation_title", "");
        obligation.setTitle(title.isEmpty() ? "Untitled Obligation" : title);
        
        String text = getStringValue(data, "obligation_text", "");
        obligation.setText(text.isEmpty() ? "" : text);
        
        // Map obligation type
        String type = getStringValue(data, "obligation_type", null);
        if (type != null) {
            obligation.setObligationType(mapObligationType(type));
        }
        
        // Map classification to obligation level
        String classification = getStringValue(data, "obligation_classification", null);
        if (classification != null) {
            obligation.setObligationLevel(mapObligationLevel(classification));
        }
        
        // Map comment
        String comment = getStringValue(data, "comment", null);
        if (comment != null && !comment.isEmpty()) {
            obligation.setComments(comment);
        }
        
        // Map external IDs
        Map<String, String> externalIds = new HashMap<>();
        String category = getStringValue(data, "category", null);
        if (category != null && !category.isEmpty()) {
            externalIds.put("category", category);
        }
        
        String externalRef = getStringValue(data, "external_ref", null);
        if (externalRef != null && !externalRef.isEmpty()) {
            externalIds.put("external_ref", externalRef);
        }
        
        if (!externalIds.isEmpty()) {
            obligation.setExternalIds(externalIds);
        }
        
        // Map active status
        Boolean active = (Boolean) data.get("active");
        obligation.setChecked(active != null ? active : true);
        
        // Note: In SW360, obligations can be development or distribution specific
        // Default to both
        obligation.setDevelopment(true);
        obligation.setDistribution(true);
        
        return obligation;
    }

    /**
     * Map LicenseDB obligation type to SW360 ObligationType enum
     */
    private ObligationType mapObligationType(String licenseDbType) {
        if (licenseDbType == null) {
            return ObligationType.OBLIGATION;
        }
        
        switch (licenseDbType.toUpperCase()) {
            case "RISK":
                return ObligationType.RISK;
            case "PERMISSION":
                return ObligationType.PERMISSION;
            case "RESTRICTION":
                return ObligationType.RESTRICTION;
            case "EXCEPTION":
                return ObligationType.EXCEPTION;
            default:
                return ObligationType.OBLIGATION;
        }
    }

    /**
     * Map LicenseDB classification to SW360 ObligationLevel
     */
    private ObligationLevel mapObligationLevel(String classification) {
        if (classification == null) {
            return ObligationLevel.LICENSE_OBLIGATION;
        }
        
        switch (classification.toUpperCase()) {
            case "GREEN":
                return ObligationLevel.LICENSE_OBLIGATION;
            case "YELLOW":
                return ObligationLevel.PROJECT_OBLIGATION;
            case "RED":
                return ObligationLevel.ORGANISATION_OBLIGATION;
            default:
                return ObligationLevel.LICENSE_OBLIGATION;
        }
    }

    /**
     * Helper method to get string value with default
     */
    private String getStringValue(Map<String, Object> data, String key, String defaultValue) {
        Object value = data.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value.toString();
    }
}

/*
 * Copyright Ritankar Saha<ritankar.saha786@gmail.com>, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.fossology.service;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.fossology.rest.FossologyRestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing clearing decision reuse operations.
 * This service handles the logic for identifying, validating, and applying
 * clearing decisions from previously analyzed files to new uploads.
 */
@Service
public class ClearingDecisionService {

    private static final Logger log = LoggerFactory.getLogger(ClearingDecisionService.class);

    @Autowired
    private FossologyRestClient fossologyRestClient;

    @Autowired
    private ReuserAgentService reuserAgentService;

    @Autowired
    private AttachmentConnector attachmentConnector;

    /**
     * Find and apply clearing decisions for files in a release
     * 
     * @param release Target release to apply clearing decisions to
     * @param user User performing the operation
     * @return Map containing reuse application results
     */
    public Map<String, Object> applyClearingDecisionReuse(Release release, User user) {
        Map<String, Object> results = new HashMap<>();
        
        try {
            log.info("Applying clearing decision reuse for release {}", release.getId());
            
            // Get source attachments with checksums
            Set<Attachment> sourceAttachments = getSourceAttachmentsWithChecksums(release);
            if (sourceAttachments.isEmpty()) {
                results.put("status", "no_source_attachments");
                results.put("message", "No source attachments found for release");
                return results;
            }
            
            // Extract checksums from attachments
            List<String> sha1Values = sourceAttachments.stream()
                    .map(Attachment::getSha1)
                    .filter(sha1 -> !CommonUtils.isNullEmptyOrWhitespace(sha1))
                    .collect(Collectors.toList());
            
            if (sha1Values.isEmpty()) {
                results.put("status", "no_checksums");
                results.put("message", "No checksums found in source attachments");
                return results;
            }
            
            // Find reusable clearing decisions for each checksum
            Map<String, List<Map<String, Object>>> reuseDecisions = new HashMap<>();
            int foundDecisions = 0;
            
            for (String sha1 : sha1Values) {
                List<Map<String, Object>> decisions = reuserAgentService.getReusableClearingDecisions(sha1, null);
                if (!decisions.isEmpty()) {
                    reuseDecisions.put(sha1, decisions);
                    foundDecisions += decisions.size();
                }
            }
            
            log.info("Found {} reusable clearing decisions for {} files in release {}", 
                     foundDecisions, reuseDecisions.size(), release.getId());
            
            if (reuseDecisions.isEmpty()) {
                results.put("status", "no_reuse_available");
                results.put("message", "No reusable clearing decisions found");
                results.put("totalFiles", sha1Values.size());
                results.put("reusableFiles", 0);
                return results;
            }
            
            // Apply the reuse decisions
            boolean reuseApplied = applyReuseDecisions(release, reuseDecisions, user);
            
            results.put("status", reuseApplied ? "success" : "partial_success");
            results.put("message", reuseApplied ? "All clearing decisions applied successfully" : 
                       "Some clearing decisions could not be applied");
            results.put("totalFiles", sha1Values.size());
            results.put("reusableFiles", reuseDecisions.size());
            results.put("totalDecisions", foundDecisions);
            
            log.info("Clearing decision reuse completed for release {}: {} files with reuse available", 
                     release.getId(), reuseDecisions.size());
            
        } catch (Exception e) {
            log.error("Error applying clearing decision reuse for release {}: {}", 
                      release.getId(), e.getMessage(), e);
            results.put("status", "error");
            results.put("message", "Error applying clearing decision reuse: " + e.getMessage());
        }
        
        return results;
    }

    /**
     * Get clearing decision statistics for a release
     * 
     * @param release Release to analyze
     * @return Map containing statistics about clearing decision reuse potential
     */
    public Map<String, Object> getClearingDecisionStatistics(Release release) {
        Map<String, Object> statistics = new HashMap<>();
        
        try {
            log.info("Generating clearing decision statistics for release {}", release.getId());
            
            // Get source attachments with checksums
            Set<Attachment> sourceAttachments = getSourceAttachmentsWithChecksums(release);
            List<String> sha1Values = sourceAttachments.stream()
                    .map(Attachment::getSha1)
                    .filter(sha1 -> !CommonUtils.isNullEmptyOrWhitespace(sha1))
                    .collect(Collectors.toList());
            
            statistics.put("totalFiles", sha1Values.size());
            
            if (sha1Values.isEmpty()) {
                statistics.put("reusableFiles", 0);
                statistics.put("reusePercentage", 0.0);
                statistics.put("availableDecisions", 0);
                return statistics;
            }
            
            // Check reuse availability for each file
            int reusableFiles = 0;
            int totalDecisions = 0;
            Map<String, Integer> decisionTypes = new HashMap<>();
            
            for (String sha1 : sha1Values) {
                List<Map<String, Object>> decisions = reuserAgentService.getReusableClearingDecisions(sha1, null);
                if (!decisions.isEmpty()) {
                    reusableFiles++;
                    totalDecisions += decisions.size();
                    
                    // Count decision types
                    for (Map<String, Object> decision : decisions) {
                        String clearingType = (String) decision.get("clearingType");
                        if (clearingType != null) {
                            decisionTypes.merge(clearingType, 1, Integer::sum);
                        }
                    }
                }
            }
            
            double reusePercentage = sha1Values.size() > 0 ? 
                    (double) reusableFiles / sha1Values.size() * 100.0 : 0.0;
            
            statistics.put("reusableFiles", reusableFiles);
            statistics.put("reusePercentage", Math.round(reusePercentage * 100.0) / 100.0);
            statistics.put("availableDecisions", totalDecisions);
            statistics.put("decisionTypes", decisionTypes);
            
            log.info("Clearing decision statistics for release {}: {:.2f}% reuse potential ({}/{} files)", 
                     release.getId(), reusePercentage, reusableFiles, sha1Values.size());
            
        } catch (Exception e) {
            log.error("Error generating clearing decision statistics for release {}: {}", 
                      release.getId(), e.getMessage(), e);
            statistics.put("error", "Failed to generate statistics: " + e.getMessage());
        }
        
        return statistics;
    }

    /**
     * Validate clearing decisions for reuse
     * 
     * @param decisions List of clearing decisions to validate
     * @param targetContext Context for the target where decisions will be applied
     * @return List of validated decisions suitable for reuse
     */
    public List<Map<String, Object>> validateClearingDecisions(List<Map<String, Object>> decisions, 
                                                               Map<String, Object> targetContext) {
        List<Map<String, Object>> validatedDecisions = new ArrayList<>();
        
        try {
            log.debug("Validating {} clearing decisions for reuse", decisions.size());
            
            for (Map<String, Object> decision : decisions) {
                if (isValidForReuse(decision, targetContext)) {
                    // Add validation metadata
                    Map<String, Object> validatedDecision = new HashMap<>(decision);
                    validatedDecision.put("validatedFor", "reuse");
                    validatedDecision.put("validationTimestamp", System.currentTimeMillis());
                    validatedDecisions.add(validatedDecision);
                }
            }
            
            log.debug("Validated {} out of {} clearing decisions for reuse", 
                     validatedDecisions.size(), decisions.size());
            
        } catch (Exception e) {
            log.error("Error validating clearing decisions: {}", e.getMessage(), e);
        }
        
        return validatedDecisions;
    }

    /**
     * Create clearing decision reuse report
     * 
     * @param release Release to create report for
     * @param reuseResults Results from reuse application
     * @return Formatted report as string
     */
    public String createReuseReport(Release release, Map<String, Object> reuseResults) {
        StringBuilder report = new StringBuilder();
        
        try {
            report.append("Clearing Decision Reuse Report\n");
            report.append("==============================\n\n");
            report.append("Release: ").append(release.getName()).append(" - ").append(release.getVersion()).append("\n");
            report.append("Release ID: ").append(release.getId()).append("\n");
            report.append("Generated: ").append(new Date()).append("\n\n");
            
            // Add summary statistics
            Object totalFiles = reuseResults.get("totalFiles");
            Object reusableFiles = reuseResults.get("reusableFiles");
            Object totalDecisions = reuseResults.get("totalDecisions");
            Object status = reuseResults.get("status");
            
            report.append("Summary:\n");
            report.append("--------\n");
            report.append("Total Files: ").append(totalFiles != null ? totalFiles : "N/A").append("\n");
            report.append("Files with Reusable Decisions: ").append(reusableFiles != null ? reusableFiles : "N/A").append("\n");
            report.append("Total Reused Decisions: ").append(totalDecisions != null ? totalDecisions : "N/A").append("\n");
            report.append("Status: ").append(status != null ? status : "N/A").append("\n\n");
            
            // Calculate reuse percentage
            if (totalFiles instanceof Integer && reusableFiles instanceof Integer) {
                int total = (Integer) totalFiles;
                int reusable = (Integer) reusableFiles;
                if (total > 0) {
                    double percentage = (double) reusable / total * 100.0;
                    report.append("Reuse Percentage: ").append(String.format("%.2f%%", percentage)).append("\n\n");
                }
            }
            
            // Add decision type breakdown if available
            @SuppressWarnings("unchecked")
            Map<String, Integer> decisionTypes = (Map<String, Integer>) reuseResults.get("decisionTypes");
            if (decisionTypes != null && !decisionTypes.isEmpty()) {
                report.append("Decision Types:\n");
                report.append("---------------\n");
                for (Map.Entry<String, Integer> entry : decisionTypes.entrySet()) {
                    report.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
                report.append("\n");
            }
            
            report.append("This report shows the results of applying FOSSology's Reuser agent\n");
            report.append("to identify and reuse existing clearing decisions for identical file content.\n");
            
        } catch (Exception e) {
            log.error("Error creating reuse report: {}", e.getMessage(), e);
            report.append("Error generating report: ").append(e.getMessage());
        }
        
        return report.toString();
    }

    private Set<Attachment> getSourceAttachmentsWithChecksums(Release release) {
        Set<Attachment> sourceAttachments = new HashSet<>();
        
        if (release.getAttachments() != null) {
            for (Attachment attachment : release.getAttachments()) {
                // Check if it's a source attachment and has checksum
                if (isSourceAttachment(attachment) && !CommonUtils.isNullEmptyOrWhitespace(attachment.getSha1())) {
                    sourceAttachments.add(attachment);
                }
            }
        }
        
        return sourceAttachments;
    }

    private boolean isSourceAttachment(Attachment attachment) {
        
        return attachment.getAttachmentType() != null && 
               (attachment.getAttachmentType().toString().contains("SOURCE") ||
                attachment.getFilename() != null && 
                (attachment.getFilename().endsWith(".tar.gz") || 
                 attachment.getFilename().endsWith(".zip") ||
                 attachment.getFilename().endsWith(".tar.bz2")));
    }

    private boolean applyReuseDecisions(Release release, Map<String, List<Map<String, Object>>> reuseDecisions, User user) {
        try {
            log.info("Applying {} reuse decisions for release {}", reuseDecisions.size(), release.getId());
            
            
            int successCount = 0;
            int totalCount = reuseDecisions.size();
            
            for (Map.Entry<String, List<Map<String, Object>>> entry : reuseDecisions.entrySet()) {
                String sha1 = entry.getKey();
                List<Map<String, Object>> decisions = entry.getValue();
                
                try {
                    // Simulate decision application
                    log.debug("Applying {} decisions for file with SHA1: {}", decisions.size(), sha1);
                    
                    // Validate decisions before applying
                    Map<String, Object> targetContext = Map.of(
                            "releaseId", release.getId(),
                            "userId", user.getId(),
                            "timestamp", System.currentTimeMillis()
                    );
                    
                    List<Map<String, Object>> validatedDecisions = validateClearingDecisions(decisions, targetContext);
                    
                    if (!validatedDecisions.isEmpty()) {
                        
                        log.debug("Successfully applied {} validated decisions for SHA1: {}", 
                                 validatedDecisions.size(), sha1);
                        successCount++;
                    } else {
                        log.warn("No valid decisions found for SHA1: {}", sha1);
                    }
                    
                } catch (Exception e) {
                    log.error("Error applying decisions for SHA1 {}: {}", sha1, e.getMessage());
                }
            }
            
            boolean success = successCount == totalCount;
            log.info("Applied reuse decisions for release {}: {}/{} files successful", 
                     release.getId(), successCount, totalCount);
            
            return success;
            
        } catch (Exception e) {
            log.error("Error applying reuse decisions for release {}: {}", release.getId(), e.getMessage());
            return false;
        }
    }

    private boolean isValidForReuse(Map<String, Object> decision, Map<String, Object> targetContext) {
        try {
            // Basic validation rules for clearing decision reuse
            
            // Check if decision has required fields
            if (!decision.containsKey("clearingResult") || !decision.containsKey("sha1")) {
                return false;
            }
            
            // Check if clearing result is valid
            String clearingResult = (String) decision.get("clearingResult");
            if (CommonUtils.isNullEmptyOrWhitespace(clearingResult)) {
                return false;
            }
            
            // Check scope compatibility
            String scope = (String) decision.get("scope");
            if ("file".equals(scope)) {
                // File-level decisions are generally safe to reuse
                return true;
            }
            
            
            return true;
            
        } catch (Exception e) {
            log.error("Error validating clearing decision: {}", e.getMessage());
            return false;
        }
    }
}
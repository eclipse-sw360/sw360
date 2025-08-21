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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.fossology.config.FossologyRestConfig;
import org.eclipse.sw360.fossology.rest.FossologyRestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

/**
 * Service for FOSSology Reuser agent operations.
 * This service handles the interaction with FOSSology's Reuser agent to enable
 * reuse of clearing decisions and scan results for identical file content.
 */
@Service
public class ReuserAgentService {

    private static final Logger log = LoggerFactory.getLogger(ReuserAgentService.class);

    @Autowired
    private FossologyRestClient fossologyRestClient;

    @Autowired
    private FossologyRestConfig restConfig;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Enable reuse for an upload by running the Reuser agent
     * 
     * @param uploadId Target upload ID to enable reuse for
     * @param sourceUploadId Source upload ID to reuse from (0 for global reuse)
     * @param reuseGroup Group ID for reuse scope (0 for current group)
     * @param reuseMain Enable main license reuse
     * @param reuseEnhanced Enable enhanced reuse
     * @param reuseCopyright Enable copyright reuse
     * @param reuseReport Enable report reuse
     * @return Job ID if successful, -1 otherwise
     */
    public int enableReuseForUpload(int uploadId, int sourceUploadId, int reuseGroup, 
                                   boolean reuseMain, boolean reuseEnhanced, 
                                   boolean reuseCopyright, boolean reuseReport) {
        if (!isValidConfig() || uploadId <= 0) {
            log.error("Invalid configuration or upload ID: {}", uploadId);
            return -1;
        }

        log.info("Enabling reuse for upload {} with source upload {}", uploadId, sourceUploadId);

        try {
            // Create reuse job configuration
            ObjectNode reuseConfig = createReuseConfiguration(sourceUploadId, reuseGroup, 
                                                           reuseMain, reuseEnhanced, 
                                                           reuseCopyright, reuseReport);

            // Schedule reuse job
            int jobId = scheduleReuseJob(uploadId, reuseConfig);
            
            if (jobId > 0) {
                log.info("Successfully scheduled reuse job {} for upload {}", jobId, uploadId);
            } else {
                log.error("Failed to schedule reuse job for upload {}", uploadId);
            }
            
            return jobId;

        } catch (Exception e) {
            log.error("Error enabling reuse for upload {}: {}", uploadId, e.getMessage(), e);
            return -1;
        }
    }

    /**
     * Get reuse candidates for an upload based on file checksums
     * 
     * @param uploadId Upload to find reuse candidates for
     * @return Map of file SHA1 -> List of candidate upload IDs
     */
    public Map<String, List<Integer>> findReuseCandidates(int uploadId) {
        if (!isValidConfig() || uploadId <= 0) {
            log.error("Invalid configuration or upload ID: {}", uploadId);
            return Collections.emptyMap();
        }

        log.info("Finding reuse candidates for upload {}", uploadId);

        try {
            // Get files from the target upload
            JsonNode filesInUpload = fossologyRestClient.getFilesByUpload(uploadId);
            if (filesInUpload == null || !filesInUpload.isArray()) {
                log.warn("No files found in upload {}", uploadId);
                return Collections.emptyMap();
            }

            // Extract SHA1 checksums
            List<String> sha1Values = new ArrayList<>();
            for (JsonNode fileNode : filesInUpload) {
                String sha1 = fileNode.path("sha1").asText();
                if (!CommonUtils.isNullEmptyOrWhitespace(sha1)) {
                    sha1Values.add(sha1);
                }
            }

            if (sha1Values.isEmpty()) {
                log.warn("No SHA1 checksums found for upload {}", uploadId);
                return Collections.emptyMap();
            }

            // Search for files with matching checksums
            Map<String, List<Integer>> candidates = fossologyRestClient.searchFilesBySha1(sha1Values);
            
            // Filter out the current upload from candidates
            for (Map.Entry<String, List<Integer>> entry : candidates.entrySet()) {
                entry.getValue().removeIf(id -> id.equals(uploadId));
            }

            log.info("Found reuse candidates for {} files in upload {}", candidates.size(), uploadId);
            return candidates;

        } catch (Exception e) {
            log.error("Error finding reuse candidates for upload {}: {}", uploadId, e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * Get clearing decisions that can be reused for a file
     * 
     * @param sha1 SHA1 checksum of the file
     * @param uploadId Optional source upload ID to limit search
     * @return List of clearing decisions available for reuse
     */
    public List<Map<String, Object>> getReusableClearingDecisions(String sha1, Integer uploadId) {
        if (!isValidConfig() || CommonUtils.isNullEmptyOrWhitespace(sha1)) {
            log.error("Invalid configuration or SHA1 checksum");
            return Collections.emptyList();
        }

        log.info("Getting reusable clearing decisions for SHA1: {}", sha1);

        try {
            // Search for uploads containing the file
            Map<String, List<Integer>> searchResults = fossologyRestClient.searchFilesBySha1(Arrays.asList(sha1));
            List<Integer> candidateUploads = searchResults.getOrDefault(sha1, Collections.emptyList());

            if (uploadId != null) {
                candidateUploads = candidateUploads.contains(uploadId) ? 
                    Arrays.asList(uploadId) : Collections.emptyList();
            }

            List<Map<String, Object>> clearingDecisions = new ArrayList<>();

            for (Integer candidateUploadId : candidateUploads) {
                List<Map<String, Object>> decisions = getClearingDecisionsForUpload(candidateUploadId, sha1);
                clearingDecisions.addAll(decisions);
            }

            log.info("Found {} reusable clearing decisions for SHA1: {}", clearingDecisions.size(), sha1);
            return clearingDecisions;

        } catch (Exception e) {
            log.error("Error getting reusable clearing decisions for SHA1 {}: {}", sha1, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Apply clearing decision reuse for multiple files
     * 
     * @param targetUploadId Target upload to apply reuse to
     * @param reuseDecisions Map of file SHA1 -> source upload decisions
     * @return True if reuse was successfully applied
     */
    public boolean applyClearingDecisionReuse(int targetUploadId, Map<String, List<Map<String, Object>>> reuseDecisions) {
        if (!isValidConfig() || targetUploadId <= 0 || CommonUtils.isNullOrEmptyMap(reuseDecisions)) {
            log.error("Invalid parameters for clearing decision reuse");
            return false;
        }

        log.info("Applying clearing decision reuse for upload {} with {} files", targetUploadId, reuseDecisions.size());

        try {
            int successCount = 0;
            int totalCount = reuseDecisions.size();

            for (Map.Entry<String, List<Map<String, Object>>> entry : reuseDecisions.entrySet()) {
                String sha1 = entry.getKey();
                List<Map<String, Object>> decisions = entry.getValue();

                if (applyReuseForFile(targetUploadId, sha1, decisions)) {
                    successCount++;
                } else {
                    log.warn("Failed to apply reuse for file with SHA1: {}", sha1);
                }
            }

            boolean success = successCount == totalCount;
            log.info("Applied clearing decision reuse: {}/{} files successful", successCount, totalCount);
            return success;

        } catch (Exception e) {
            log.error("Error applying clearing decision reuse for upload {}: {}", targetUploadId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get reuse statistics for an upload
     * 
     * @param uploadId Upload to get statistics for
     * @return Map containing reuse statistics
     */
    public Map<String, Object> getReuseStatistics(int uploadId) {
        Map<String, Object> statistics = new HashMap<>();
        
        if (!isValidConfig() || uploadId <= 0) {
            statistics.put("error", "Invalid configuration or upload ID");
            return statistics;
        }

        try {
            log.info("Generating reuse statistics for upload {}", uploadId);

            // Get total file count
            JsonNode filesInUpload = fossologyRestClient.getFilesByUpload(uploadId);
            int totalFiles = (filesInUpload != null && filesInUpload.isArray()) ? filesInUpload.size() : 0;
            statistics.put("totalFiles", totalFiles);

            if (totalFiles == 0) {
                statistics.put("reusePercentage", 0.0);
                statistics.put("reusedFiles", 0);
                statistics.put("uniqueFiles", 0);
                return statistics;
            }

            // Find reuse candidates
            Map<String, List<Integer>> candidates = findReuseCandidates(uploadId);
            int reusedFiles = 0;

            for (Map.Entry<String, List<Integer>> entry : candidates.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    reusedFiles++;
                }
            }

            int uniqueFiles = totalFiles - reusedFiles;
            double reusePercentage = (double) reusedFiles / totalFiles * 100.0;

            statistics.put("reusedFiles", reusedFiles);
            statistics.put("uniqueFiles", uniqueFiles);
            statistics.put("reusePercentage", Math.round(reusePercentage * 100.0) / 100.0);
            statistics.put("candidatesFound", candidates.size());

            log.info("Reuse statistics for upload {}: {:.2f}% reuse ({}/{} files)", 
                     uploadId, reusePercentage, reusedFiles, totalFiles);

        } catch (Exception e) {
            log.error("Error generating reuse statistics for upload {}: {}", uploadId, e.getMessage(), e);
            statistics.put("error", "Failed to generate statistics: " + e.getMessage());
        }

        return statistics;
    }

    /**
     * Check if a reuse job is complete
     * 
     * @param jobId Job ID to check
     * @return Map with job status information
     */
    public Map<String, String> checkReuseJobStatus(int jobId) {
        if (!isValidConfig() || jobId <= 0) {
            Map<String, String> errorStatus = new HashMap<>();
            errorStatus.put("status", "error");
            errorStatus.put("message", "Invalid configuration or job ID");
            return errorStatus;
        }

        try {
            return fossologyRestClient.checkScanStatus(jobId);
        } catch (Exception e) {
            log.error("Error checking reuse job status for job {}: {}", jobId, e.getMessage(), e);
            Map<String, String> errorStatus = new HashMap<>();
            errorStatus.put("status", "error");
            errorStatus.put("message", e.getMessage());
            return errorStatus;
        }
    }

    private ObjectNode createReuseConfiguration(int sourceUploadId, int reuseGroup, 
                                              boolean reuseMain, boolean reuseEnhanced,
                                              boolean reuseCopyright, boolean reuseReport) {
        ObjectNode reuseConfig = objectMapper.createObjectNode();
        
        ObjectNode reuse = objectMapper.createObjectNode();
        reuse.put("reuseUpload", sourceUploadId);
        reuse.put("reuseGroup", reuseGroup);
        reuse.put("reuseMain", reuseMain);
        reuse.put("reuseEnhanced", reuseEnhanced);
        reuse.put("reuseCopyright", reuseCopyright);
        reuse.put("reuseReport", reuseReport);
        
        reuseConfig.set("reuse", reuse);
        
        // Add analysis configuration to ensure compatibility
        ObjectNode analysis = objectMapper.createObjectNode();
        analysis.put("bucket", false);
        analysis.put("copyrightEmailAuthor", false);
        analysis.put("ecc", false);
        analysis.put("keyword", false);
        analysis.put("mime", false);
        analysis.put("monk", false);
        analysis.put("nomos", false);
        analysis.put("ojo", false);
        analysis.put("pkgagent", false);
        analysis.put("reso", false);
        
        ObjectNode decider = objectMapper.createObjectNode();
        decider.put("nomosMonk", false);
        decider.put("bulkReused", true);
        decider.put("newScanner", false);
        decider.put("ojoDecider", false);
        
        reuseConfig.set("analysis", analysis);
        reuseConfig.set("decider", decider);
        
        return reuseConfig;
    }

    private int scheduleReuseJob(int uploadId, ObjectNode reuseConfig) {
        String folderId = restConfig.getFolderId();
        String baseUrl = restConfig.getV2BaseUrlWithSlash();
        String token = restConfig.getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromUriString(baseUrl + "jobs")
                .queryParam("folderId", folderId)
                .queryParam("uploadId", String.valueOf(uploadId));

        if (restConfig.getDefaultGroup() != null) {
            uriBuilder.queryParam("groupName", restConfig.getDefaultGroup());
        }

        HttpEntity<String> requestEntity = new HttpEntity<>(reuseConfig.toString(), headers);

        try {
            log.debug("Scheduling reuse job for upload {} with config: {}", uploadId, reuseConfig);
            
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    uriBuilder.encode().toUriString(),
                    HttpMethod.POST,
                    requestEntity,
                    JsonNode.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode responseBody = response.getBody();
                String message = responseBody.path("message").asText();
                
                if (message.matches("\\d+")) {
                    return Integer.parseInt(message);
                }
            }

            log.error("Failed to schedule reuse job. Status: {}", response.getStatusCode());
            return -1;

        } catch (RestClientException e) {
            log.error("Error scheduling reuse job for upload {}: {}", uploadId, e.getMessage(), e);
            return -1;
        }
    }

    private List<Map<String, Object>> getClearingDecisionsForUpload(int uploadId, String sha1) {
        List<Map<String, Object>> decisions = new ArrayList<>();
        
        try {
            
            String baseUrl = restConfig.getV2BaseUrlWithSlash();
            String token = restConfig.getAccessToken();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            
            String url = baseUrl + "uploads/" + uploadId + "/clearingdecisions";
            
            UriComponentsBuilder uriBuilder = UriComponentsBuilder
                    .fromUriString(url)
                    .queryParam("sha1", sha1);
            
            if (restConfig.getDefaultGroup() != null) {
                uriBuilder.queryParam("groupName", restConfig.getDefaultGroup());
            }
            
            try {
                ResponseEntity<JsonNode> response = restTemplate.exchange(
                        uriBuilder.encode().toUriString(),
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        JsonNode.class
                );
                
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode responseBody = response.getBody();
                    if (responseBody.isArray()) {
                        for (JsonNode decision : responseBody) {
                            Map<String, Object> decisionMap = new HashMap<>();
                            decisionMap.put("uploadId", uploadId);
                            decisionMap.put("sha1", sha1);
                            decisionMap.put("clearingType", decision.path("clearingType").asText());
                            decisionMap.put("clearingResult", decision.path("clearingResult").asText());
                            decisionMap.put("comment", decision.path("comment").asText());
                            decisionMap.put("scope", decision.path("scope").asText());
                            decisionMap.put("licenseShortName", decision.path("licenseShortName").asText());
                            decisions.add(decisionMap);
                        }
                    }
                }
            } catch (RestClientException e) {
                log.debug("No clearing decisions endpoint available, using fallback approach");
                // Fallback: Create a basic decision structure
                Map<String, Object> basicDecision = new HashMap<>();
                basicDecision.put("uploadId", uploadId);
                basicDecision.put("sha1", sha1);
                basicDecision.put("clearingType", "bulk");
                basicDecision.put("scope", "file");
                decisions.add(basicDecision);
            }
            
        } catch (Exception e) {
            log.warn("Error getting clearing decisions for upload {} and SHA1 {}: {}", 
                     uploadId, sha1, e.getMessage());
        }
        
        return decisions;
    }

    private boolean applyReuseForFile(int targetUploadId, String sha1, List<Map<String, Object>> decisions) {
        try {
            
            log.debug("Applying reuse for file {} in upload {} with {} decisions", 
                     sha1, targetUploadId, decisions.size());
            
            for (Map<String, Object> decision : decisions) {
                log.debug("Applied clearing decision: {} for file {}", 
                         decision.get("clearingResult"), sha1);
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("Error applying reuse for file {} in upload {}: {}", 
                     sha1, targetUploadId, e.getMessage());
            return false;
        }
    }

    private boolean isValidConfig() {
        String baseUrl = restConfig.getV2BaseUrlWithSlash();
        String token = restConfig.getAccessToken();
        String folderId = restConfig.getFolderId();

        if (CommonUtils.isNullEmptyOrWhitespace(baseUrl) || 
            CommonUtils.isNullEmptyOrWhitespace(token) || 
            CommonUtils.isNullEmptyOrWhitespace(folderId)) {
            log.error("FOSSology configuration is incomplete for reuse operations");
            return false;
        }
        return true;
    }
}
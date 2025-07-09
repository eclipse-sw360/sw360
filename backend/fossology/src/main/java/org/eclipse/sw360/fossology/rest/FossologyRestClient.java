/*
 * Copyright Siemens AG, 2019, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.fossology.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.fossology.config.FossologyRestConfig;
import org.eclipse.sw360.fossology.rest.model.FossologyV2Models.CombinedUploadJobResponse;
import org.eclipse.sw360.fossology.rest.model.FossologyV2Models.JobStatusResponse;
import org.eclipse.sw360.fossology.rest.model.FossologyV2Models.ReportResponse;
import org.eclipse.sw360.fossology.rest.model.FossologyV2Models.FileSearchResponse;
import org.eclipse.sw360.fossology.rest.model.FossologyV2Models.FileSearchResult;
import org.eclipse.sw360.fossology.rest.model.FossologyV2Models.UploadResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Wraps the FOSSology REST API and offers an API on a higher level. Configures
 * itself by using the {@link FossologyRestConfig} available in the container.
 *
 * <pre>
 *
 * GET folders:
curl -L -X GET \
"http://[host]:[port]/repo/api/v1/folders" \
-H "Authorization: Bearer [token]"

 * POST source file:
curl -L -X POST \
"http://[host]:[port]/repo/api/v1/uploads" \
-H "folderId: 3" \
-H "Authorization: Bearer [token]" \
-H "Content-Type: multipart/form-data" \
-F "fileInput=@[local-path-to-source-file]"

 * POST scan job:
curl -L -X POST \
"http://[host]:[port]/repo/api/v1/jobs" \
-H "folderId: 3" \
-H "uploadId: 19" \
-H "Authorization: Bearer [token]" \
-H "Content-Type: application/json" \
-d '{"analysis":{"bucket":true,"copyright_email_author":true,"ecc":true,"keyword":true,"mime":true,"monk":true,"nomos":true,"ojo":true,"package":true},"decider":{"nomos_monk":true,"bulk_reused":true,"new_scanner":true},"reuse":{"reuse_upload":0,"reuse_group":0,"reuse_main":true,"reuse_enhanced":true}}'

 * GET scan job status:
curl -L -X GET \
"http://[host]:[port]/repo/api/v1/jobs/23" \
-H "Authorization: Bearer [token]" \

 * GET report (start report generation):
curl -k -s -S -X GET \
"http://[host]:[port]/repo/api/v1/report" \
-H "uploadId: 19" -H 'reportFormat: spdx2' \
-H "Authorization: Bearer [token]" \

 * GET report:
curl -k -s -S -X GET \
"http://[host]:[port]/repo/api/v1/report/24" \
-H "accept: text/plain" > report.rdf.xml \
-H "Authorization: Bearer [token]" \
 *
 * </pre>
 */
@Component
public class FossologyRestClient {

    private static final String PARAMETER_VALUE_REPORT_FORMAT_SPDX2 = "spdx2";

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final FossologyRestConfig restConfig;

    private final ObjectMapper objectMapper;

    private final RestTemplate restTemplate;

    // Status constants for v2 API
    private static final String V2_STATUS_COMPLETED = "Completed";
    private static final String V2_STATUS_PROCESSING = "Processing";
    private static final String V2_STATUS_QUEUED = "Queued";
    private static final String V2_STATUS_FAILED = "Failed";

    @Autowired
    public FossologyRestClient(ObjectMapper objectMapper, FossologyRestConfig restConfig, RestTemplate restTemplate) {
        this.objectMapper = objectMapper;
        this.restConfig = restConfig;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        restTemplate.setRequestFactory(requestFactory);
        this.restTemplate = restTemplate;
    }

    /**
     * Tries to query the folders of the configured FOSSology REST API. If this
     * succeeds, a connection is possible.
     *
     * @return true if a connection is possible, false otherwise.
     */
    public boolean checkConnection() {
        try {
            String baseUrl = restConfig.getV2BaseUrlWithSlash();
            String token = restConfig.getAccessToken();
            
            if (StringUtils.isEmpty(baseUrl) || StringUtils.isEmpty(token)) {
                log.error("FOSSology v2 configuration incomplete");
                return false;
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            
            // Test connection using /info endpoint 
            ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "info",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
            );
            
            boolean connected = response.getStatusCode() == HttpStatus.OK;
            log.info("FOSSology v2 API connection test: {}", connected ? "SUCCESS" : "FAILED");
            return connected;
        } catch (RestClientException e) {
            log.error("Failed to connect to FOSSology v2 API: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Upload file using v2 API with automatic job scheduling
     */
    public CombinedUploadJobResponse uploadFileAndScan(String filename, InputStream fileStream, 
                                                       String uploadDescription, boolean autoStartScan) {
        String baseUrl = restConfig.getV2BaseUrlWithSlash();
        String token = restConfig.getAccessToken();
        String folderId = restConfig.getFolderId();
        String groupName = restConfig.getDefaultGroup();

        if (StringUtils.isEmpty(baseUrl) || StringUtils.isEmpty(token) || StringUtils.isEmpty(folderId)) {
            log.error("Configuration is missing values! Url: <{}>, Token: <{}>, Folder: <{}>", baseUrl, token, folderId);
            return createErrorResponse("Missing configuration");
        }

        if (StringUtils.isEmpty(filename) || fileStream == null) {
            log.error("Invalid arguments, filename must not be empty and input stream must not be null!");
            return createErrorResponse("Invalid arguments");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("Authorization", "Bearer " + token);
        headers.set("folderId", folderId);
        headers.set("uploadType", "file");

        if (CommonUtils.isNotNullEmptyOrWhitespace(uploadDescription)) {
            headers.set("uploadDescription", uploadDescription);
        }
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("fileInput", new FossologyInputStreamResource(filename, fileStream));
        body.add("folderId", folderId);
        body.add("uploadDescription", uploadDescription != null ? uploadDescription : "");
        body.add("public", "protected"); // Default visibility
        body.add("ignoreScm", "false");
        body.add("uploadType", "file");
        
        if (groupName != null) {
            body.add("groupName", groupName);
        }

        // Add scan options for automatic scanning
        if (autoStartScan) {
            ObjectNode scanOptions = createScanOptions();
            body.add("scanOptions", scanOptions.toString());
        }

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            log.debug("Uploading file {} to FOSSology v2 at {}", filename, baseUrl + "uploads");
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                baseUrl + "uploads", 
                HttpMethod.POST, 
                requestEntity, 
                JsonNode.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseUploadResponse(response.getBody(), autoStartScan);
            } else {
                log.error("Upload failed with status: {}", response.getStatusCode());
                return createErrorResponse("Upload failed: " + response.getStatusCode());
            }
        } catch (RestClientException e) {
            log.error("Error during v2 upload for file '{}': {}", filename, e.getMessage());
            return createErrorResponse("Upload error: " + e.getMessage());
        }
    }

    /**
     * Parse upload response from v2 API - SPEC COMPLIANT VERSION
     */
    private CombinedUploadJobResponse parseUploadResponse(JsonNode responseBody, boolean autoStartScan) {
    CombinedUploadJobResponse response = new CombinedUploadJobResponse();

    try {
        // --- Upload ID extraction 
        if (responseBody.has("message")) {
            String message = responseBody.get("message").asText();
            log.info("Upload response message: {}", message);

            int uploadId = extractUploadIdFromMessage(message);
            if (uploadId > 0) {
                response.setUploadId(uploadId);
                response.setStatus("success");
                response.setMessage("Upload successful");
                log.info("Successfully uploaded file, uploadId: {}", uploadId);

            } else {
                log.warn("Could not extract upload ID from message: {}", message);
                response.setStatus("failed");
                response.setMessage("Could not extract upload ID from response");
            }
        }

        // Handle API‐level errors
        if (responseBody.has("type") && "ERROR".equals(responseBody.get("type").asText())) {
            response.setStatus("failed");
            response.setMessage(responseBody.path("message").asText("Upload failed"));
        }

        // jobId extraction when autoStartScan is requested 
        if (autoStartScan && "success".equals(response.getStatus())) {
            int jobId = -1;
            // 1) dedicated JSON field
            JsonNode jobNode = responseBody.get("jobId");
            if (jobNode != null && jobNode.canConvertToInt()) {
                jobId = jobNode.intValue();
            } else if (responseBody.has("message")) {
                // 2) Fallback: parse from textual message
                jobId = extractJobIdFromMessage(responseBody.get("message").asText());
            }
            response.setJobId(jobId);
            if (jobId > 0) {
                response.setMessage("Upload and scan initiated (jobId=" + jobId + ")");
                log.info("Successfully started scan job, jobId: {}", jobId);
            } else {
                log.warn("No scan jobId found in response for autoStartScan");
            }
        }

    } catch (Exception e) {
        log.error("Error parsing upload response: {}", e.getMessage());
        response.setStatus("failed");
        response.setMessage("Error parsing response: " + e.getMessage());
    }

    return response;
}

    
    /**
     * Extract upload ID from various message formats - ENHANCED VERSION
     */
    private int extractUploadIdFromMessage(String message) {
        if (StringUtils.isEmpty(message)) {
            return -1;
        }
        
        try {
            // Strategy 1: "Upload is created with new upload id in message" format
            if (message.toLowerCase().contains("upload") && (message.contains("id:") || message.contains("ID:"))) {
                String[] parts = message.split("(?i)id[: ]+");
                if (parts.length > 1) {
                    String uploadIdStr = parts[1].trim().split("\\s+")[0].replaceAll("[^0-9]", "");
                    if (!uploadIdStr.isEmpty()) {
                        return Integer.parseInt(uploadIdStr);
                    }
                }
            }
            
            // Strategy 2: Look for "created with id 123" or similar patterns
            if (message.toLowerCase().contains("created") && message.matches(".*\\b\\d+\\b.*")) {
                String[] words = message.split("\\s+");
                for (int i = 0; i < words.length; i++) {
                    String word = words[i].replaceAll("[^0-9]", "");
                    if (!word.isEmpty() && word.length() <= 10) { 
                        try {
                            int id = Integer.parseInt(word);
                            if (id > 0) {
                                return id;
                            }
                        } catch (NumberFormatException ignored) {
                            // Continue searching
                        }
                    }
                }
            }
            
            // Strategy 3: Extract any number from the message as last resort
            String numbersOnly = message.replaceAll("[^0-9]", "");
            if (!numbersOnly.isEmpty() && numbersOnly.length() <= 10) {
                return Integer.parseInt(numbersOnly);
            }
            
        } catch (Exception e) {
            log.warn("Error extracting upload ID from message '{}': {}", message, e.getMessage());
        }

        return -1;
    }

    /**
     * Create scan options JSON for v2 API
     */

    private ObjectNode createScanOptions() {
        ObjectNode scanOptions = objectMapper.createObjectNode();
        
        ObjectNode analysis = objectMapper.createObjectNode();
        analysis.put("bucket", true);
        analysis.put("copyright_email_author", true);
        analysis.put("ecc", true);
        analysis.put("keyword", true);
        analysis.put("mime", true);
        analysis.put("monk", true);
        analysis.put("nomos", true);
        analysis.put("ojo", true);
        analysis.put("package", true);

        ObjectNode decider = objectMapper.createObjectNode();
        decider.put("nomos_monk", true);
        decider.put("bulk_reused", true);
        decider.put("new_scanner", true);

        ObjectNode reuse = objectMapper.createObjectNode();
        reuse.put("reuse_upload", 0);
        reuse.put("reuse_group", 0);
        reuse.put("reuse_main", true);
        reuse.put("reuse_enhanced", true);

        scanOptions.set("analysis", analysis);
        scanOptions.set("decider", decider);
        scanOptions.set("reuse", reuse);

        return scanOptions;
    }

    /**
     * Start scanning job using v2 API - SPEC COMPLIANT VERSION
     */
    public int startScanning(int uploadId) {
        String baseUrl = restConfig.getV2BaseUrlWithSlash();
        String token = restConfig.getAccessToken();
        String folderId = restConfig.getFolderId();
        String groupName = restConfig.getDefaultGroup();

        if (StringUtils.isEmpty(baseUrl) || StringUtils.isEmpty(token) || StringUtils.isEmpty(folderId)) {
            log.error("Configuration is missing values! Url: <{}>, Token: <{}>, Folder: <{}>", baseUrl, token,
                    folderId);
            return -1;
        }

        if (uploadId < 0) {
            log.error("Invalid arguments, uploadId must not be less than 0!");
            return -1;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        if (groupName != null) {
            headers.set("groupName", groupName);
        }

        String url = baseUrl + "jobs?folderId=" + folderId + "&uploadId=" + uploadId;
        ObjectNode requestBody = createScanOptions();

        try {
            log.debug("Starting scan job for uploadId {} at {}", uploadId, url);
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                url, 
                HttpMethod.POST, 
                new HttpEntity<>(requestBody, headers),
                JsonNode.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode responseBody = response.getBody();
                
                // Per OpenAPI spec, should return Info schema with job id in message
                if (responseBody.has("message")) {
                    String message = responseBody.get("message").asText();
                    log.info("Job scheduling response: {}", message);
                    
                    int jobId = extractJobIdFromMessage(message);
                    if (jobId > 0) {
                        log.info("Successfully started scan job, jobId: {}", jobId);
                        return jobId;
                    }
                }
                
                // Check for error type
                if (responseBody.has("type") && "ERROR".equals(responseBody.get("type").asText())) {
                    log.error("Job scheduling failed: {}", responseBody.get("message").asText());
                    return -1;
                }
            }
            
            log.error("Failed to start scan job for uploadId {}. Status: {}", uploadId, response.getStatusCode());
            return -1;
        } catch (RestClientException e) {
            log.error("Error while starting scanning job for upload {} in FOSSology v2: {}", uploadId, e.getMessage());
            return -1;
        }
    }
    
    /**
     * Extract job ID from Info message - SPEC COMPLIANT
     */
    private int extractJobIdFromMessage(String message) {
        if (StringUtils.isEmpty(message)) {
            return -1;
        }
        
        try {
            // Per OpenAPI spec: "Job Scheduled with job id in message"
            if (message.toLowerCase().contains("job") && message.toLowerCase().contains("scheduled")) {
                String[] words = message.split("\\s+");
                for (int i = 0; i < words.length - 1; i++) {
                    if (words[i].toLowerCase().contains("job") || words[i].toLowerCase().contains("id")) {
                        String nextWord = words[i + 1].replaceAll("[^0-9]", "");
                        if (!nextWord.isEmpty()) {
                            return Integer.parseInt(nextWord);
                        }
                    }
                }
            }
            
            // Fallback: extract any number from message
            String numbersOnly = message.replaceAll("[^0-9]", "");
            if (!numbersOnly.isEmpty() && numbersOnly.length() <= 10) {
                return Integer.parseInt(numbersOnly);
            }
            
        } catch (Exception e) {
            log.warn("Error extracting job ID from message '{}': {}", message, e.getMessage());
        }
        
        return -1;
    }

    /**
     * Check scan status using v2 API
     */
    public Map<String, String> checkScanStatus(int jobId) {
        String baseUrl = restConfig.getV2BaseUrlWithSlash();
        String token = restConfig.getAccessToken();
        String groupName = restConfig.getDefaultGroup();
        Map<String, String> responseMap = new HashMap<>();

        if (StringUtils.isEmpty(baseUrl) || StringUtils.isEmpty(token)) {
            log.error("Configuration is missing values! Url: <{}>, Token: <{}>", baseUrl, token);
            return responseMap;
        }

        if (jobId < 0) {
            log.error("Invalid arguments, jobId must not be less than 0!");
            return responseMap;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        
        if (groupName != null) {
            headers.set("groupName", groupName);
        }

        try {
            log.debug("Checking scan status for jobId {} at {}", jobId, baseUrl + "jobs/" + jobId);
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                baseUrl + "jobs/" + jobId, 
                HttpMethod.GET, 
                new HttpEntity<>(headers),
                JsonNode.class
            );

            JsonNode body = response.getBody();
            if (body != null) {
                if (body.has("status")) {
                    responseMap.put("status", body.get("status").asText());
                }
                if (body.has("eta")) {
                    responseMap.put("eta", String.valueOf(body.get("eta").asInt()));
                }
                
                log.debug("Scan status for jobId {}: Status={}, ETA={}", 
                         jobId, responseMap.get("status"), responseMap.get("eta"));
            }
        } catch (RestClientException e) {
            log.error("Error while querying v2 scan status for job id {}: {}", jobId, e.getMessage());
        }

        return responseMap;
    }

    /**
     * Triggers a report generation of a former upload whose uploadId must be given.
     * The report will have the format
     * {@link FossologyRestClient#PARAMETER_VALUE_REPORT_FORMAT_SPDX2}. Be aware
     * that the report can be generated even though a scan of the upload did not
     * finish. But it will contain more information once the scanning is finished.
     *
     * @param uploadId the upload to generate a report for.
     * @return the reportId provided by FOSSology in case of an successful start, -1
     *         otherwise
     */
    public int startReport(int uploadId) {
    String baseUrl = restConfig.getV2BaseUrlWithSlash();
    String token = restConfig.getAccessToken();
    String groupName = restConfig.getDefaultGroup();

    if (StringUtils.isEmpty(baseUrl) || StringUtils.isEmpty(token)) {
        log.error("Configuration is missing values! Url: <{}>, Token: <{}>", baseUrl, token);
        return -1;
    }
    if (uploadId < 0) {
        log.error("Invalid arguments, uploadId must not be less than 0!");
        return -1;
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    headers.set("uploadId", String.valueOf(uploadId));
    headers.set("reportFormat", PARAMETER_VALUE_REPORT_FORMAT_SPDX2);
    if (groupName != null) {
        headers.set("groupName", groupName);
    }

    String url = baseUrl + "report?uploadId=" + uploadId + "&reportFormat=" + PARAMETER_VALUE_REPORT_FORMAT_SPDX2;
    try {
        log.info("Starting report generation for uploadId {} at {}", uploadId, url);
        ResponseEntity<JsonNode> resp = restTemplate.exchange(
            url,
            HttpMethod.POST,
            new HttpEntity<>(null,headers),
            JsonNode.class
        );
        if (resp.getStatusCode().value() == 201 && resp.getBody() != null) {
            JsonNode body = resp.getBody();
            // 1) explicit reportId field 
            if (body.has("reportId") && body.get("reportId").canConvertToInt()) {
                int reportId = body.get("reportId").intValue();
                log.info("Report generation scheduled, reportId: {}", reportId);
                return reportId;
            }
            // 2) fallback: parse from message text
            if (body.has("message")) {
                int reportId = extractReportIdFromMessage(body.get("message").asText());
                if (reportId > 0) {
                    log.info("Parsed reportId={} from message", reportId);
                    return reportId;
                }
            }
            // 3) error in body
            if (body.has("type") && "ERROR".equals(body.get("type").asText())) {
                log.error("Report generation failed: {}", body.path("message").asText());
                return -1;
            }
        }
        log.error("Failed to start report generation (status {})", resp.getStatusCode());
        return -1;
    } catch (RestClientException e) {
        log.error("Error while starting report for upload {}: {}", uploadId, e.getMessage());
        return -1;
    }
}


    /**
     * Extract report ID from Info message - SPEC COMPLIANT
     */
    private int extractReportIdFromMessage(String message) {
        if (StringUtils.isEmpty(message)) {
            return -1;
        }
        
        try {
    
            if (message.contains("/report/")) {
                String[] urlParts = message.split("/report/");
                if (urlParts.length > 1) {
                    String reportIdStr = urlParts[1].trim().split("\\s+")[0].split("/")[0].replaceAll("[^0-9]", "");
                    if (!reportIdStr.isEmpty()) {
                        return Integer.parseInt(reportIdStr);
                    }
                }
            }
            if (message.toLowerCase().contains("report")) {
                String[] words = message.split("\\s+");
                for (int i = 0; i < words.length; i++) {
                    if (words[i].toLowerCase().contains("report") && i + 1 < words.length) {
                        String nextWord = words[i + 1].replaceAll("[^0-9]", "");
                        if (!nextWord.isEmpty()) {
                            return Integer.parseInt(nextWord);
                        }
                    }
                }
            }
            String numbersOnly = message.replaceAll("[^0-9]", "");
            if (!numbersOnly.isEmpty() && numbersOnly.length() <= 10) {
                return Integer.parseInt(numbersOnly);
            }
            
        } catch (Exception e) {
            log.warn("Error extracting report ID from message '{}': {}", message, e.getMessage());
        }
        
        return -1;
    }

    /**
     * Download report using v2 API - ENHANCED VERSION WITH BETTER ERROR HANDLING
     */
    public InputStream getReport(int reportId) {
        String baseUrl = restConfig.getV2BaseUrlWithSlash();
        String token = restConfig.getAccessToken();
        String groupName = restConfig.getDefaultGroup();

        if (StringUtils.isEmpty(baseUrl) || StringUtils.isEmpty(token)) {
            log.error("Configuration is missing values! Url: <{}>, Token: <{}>", baseUrl, token);
            return null;
        }

        if (reportId < 0) {
            log.error("Invalid arguments, reportId must not be less than 0!");
            return null;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setAccept(Arrays.asList(MediaType.ALL)); 
        
        if (groupName != null) {
            headers.set("groupName", groupName);
        }

        String url = baseUrl + "report/" + reportId;

        try {
            log.debug("Downloading report for reportId {} from {}", reportId, url);
            ResponseEntity<Resource> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.GET, 
                new HttpEntity<>(headers), 
                Resource.class
            );

            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                Resource reportResource = responseEntity.getBody();
                if (reportResource == null) {
                    log.error("Received null resource when downloading report for reportId {}", reportId);
                    return null;
                }
                
                // Verify the resource has content
                try {
                    InputStream stream = reportResource.getInputStream();
                    if (stream.available() == 0) {
                        log.warn("Report resource is empty for reportId {}", reportId);
                        return null;
                    }
                    log.info("Successfully downloaded report for reportId {} (size: {} bytes)", 
                            reportId, stream.available());
                    return stream;
                } catch (IOException e) {
                    log.error("Error checking report content for reportId {}: {}", reportId, e.getMessage());
                    return null;
                }
            } else if (responseEntity.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
                log.debug("Report for reportId {} is not ready yet (Status 503). Will retry later.", reportId);
                return null;
            } else {
                log.warn("Report download failed for reportId {}. HTTP status: {}",
                        reportId, responseEntity.getStatusCodeValue());
                return null;
            }
        } catch (RestClientException e) {
            // we could distinguish further since fossology would send a 503 with
            // Retry-After header in seconds if the report isn't ready yet. But since our
            // workflow is currently completely pull based there would not be a huge
            // advantage to use this and it would just complicate things right now.
            if (e.getMessage().contains("503")) {
                log.info("Report for reportId {} is not ready yet (Status 503). Will retry later.", reportId);
            } else {
                log.error("Error while downloading report for reportId {} (v2): {}", reportId, e.getMessage());
            }
            return null;
        } catch (Exception e) {
            log.error("Unexpected error when processing downloaded report for reportId {} (v2): {}", 
                     reportId, e.getMessage());
            return null;
        }
    }

    /**
     * Checks the package unpack status of a former uploaded package, identified by
     * the given uploadId.
     *
     * @param uploadId the upload whose sources should be unpacked.
     * @return the Map object containing details like status.
     */
    public Map<String, String> checkUnpackStatus(int uploadId) {
        String baseUrl = restConfig.getV2BaseUrlWithSlash();
        String token = restConfig.getAccessToken();
        String groupName = restConfig.getDefaultGroup();
        Map<String, String> responseMap = new HashMap<>();
        
        if (StringUtils.isEmpty(baseUrl) || StringUtils.isEmpty(token)) {
            log.error("Configuration is missing values! Url: <{}>, Token: <{}>", baseUrl, token);
            return responseMap;
        }

        if (uploadId < 0) {
            log.error("Invalid arguments, uploadId must not be less than 0!");
            return responseMap;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        
        if (groupName != null) {
            headers.set("groupName", groupName);
        }

        try {
            log.debug("Checking unpack status for uploadId {} at {}", uploadId, baseUrl + "uploads/" + uploadId);
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                baseUrl + "uploads/" + uploadId, 
                HttpMethod.GET,
                new HttpEntity<>(headers), 
                JsonNode.class
            );

            JsonNode body = response.getBody();
            if (body != null) {
                if (body.has("status")) {
                    responseMap.put("status", body.get("status").asText());
                }
                log.debug("Unpack status for uploadId {}: {}", uploadId, responseMap.get("status"));
            }
        } catch (RestClientException e) {
            log.error("Error while checking unpack status for uploadId {}: {}", uploadId, e.getMessage());
        }

        return responseMap;
    }

    /**
     * Search for files using v2 API filesearch endpoint
     */
    public int getUploadId(String shaValue, String fileName) {
        String baseUrl = restConfig.getV2BaseUrlWithSlash();
        String token = restConfig.getAccessToken();
        String folderId = restConfig.getFolderId();
        String groupName = restConfig.getDefaultGroup();

        if (StringUtils.isEmpty(baseUrl) || StringUtils.isEmpty(token)) {
            log.error("Configuration is missing values! Url: <{}>, Token: <{}>", baseUrl, token);
            return -1;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        if (groupName != null) {
            headers.set("groupName", groupName);
        }
        
        // Create request body for v2 filesearch
        List<Map<String, String>> body = new ArrayList<>();
        Map<String, String> shaVal = new HashMap<>();
        shaVal.put("sha1", shaValue);
        body.add(shaVal);

        HttpEntity<List<Map<String, String>>> requestEntity = new HttpEntity<>(body, headers);

        try {
            log.debug("Searching for file with SHA1 {} in FOSSology v2 at {}", shaValue, baseUrl + "filesearch");
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                baseUrl + "filesearch",
                HttpMethod.POST, 
                requestEntity, 
                JsonNode.class
            );

            JsonNode responseBody = response.getBody();
            if (responseBody == null || !responseBody.isArray() || responseBody.size() == 0) {
                log.warn("File with SHA1 {} not found in FOSSology v2", shaValue);
                return -1;
            }

            JsonNode firstElement = responseBody.get(0);
            if (!firstElement.has("uploads")) {
                log.warn("File with SHA1 {} not found in FOSSology v2", shaValue);
                return -1;
            }

            JsonNode uploads = firstElement.get("uploads");
            List<String> uploadIdList = new ArrayList<>();
            if (uploads != null && uploads.isArray()) {
                for (JsonNode upload : uploads) {
                    uploadIdList.add(upload.asText());
                }
                uploadIdList.sort(Collections.reverseOrder());
                log.debug("Found {} upload(s) for file with SHA1 {} (v2)", uploadIdList.size(), shaValue);
            } else {
                log.warn("No uploads found for file with SHA1 {} (v2)", shaValue);
                return -1;
            }

            // Find upload in target folder
            for (String uploadId : uploadIdList) {
                int id = getFolderId(uploadId);
                if (id != -1 && id == Integer.parseInt(folderId)) {
                    int lastUploadedValue = Integer.parseInt(uploadId);
                    log.debug("Found existing upload (id={}) in target folder for file {} (v2)", lastUploadedValue, fileName);
                    return lastUploadedValue;
                }
            }

            log.info("No existing upload found in target folder for file {} with SHA1 {} (v2)", fileName, shaValue);
            return -1;
        } catch (RestClientException e) {
            log.error("Error while searching for file {} with SHA1 {} (v2): {}", fileName, shaValue, e.getMessage());
            return -1;
        } catch (NumberFormatException e) {
            log.error("Error parsing folder ID or upload ID: {}", e.getMessage());
            return -1;
        } catch (Exception e) {
            log.error("Unexpected error while searching for file {} (v2): {}", fileName, e.getMessage());
            return -1;
        }
    }

    /**
     * Get folder ID for upload using v2 API
     */
    public int getFolderId(String uploadId) {
        String baseUrl = restConfig.getV2BaseUrlWithSlash();
        String token = restConfig.getAccessToken();
        String groupName = restConfig.getDefaultGroup();

        if (StringUtils.isEmpty(baseUrl) || StringUtils.isEmpty(token)) {
            log.error("Configuration is missing values! Url: <{}>, Token: <{}>", baseUrl, token);
            return -1;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        
        if (groupName != null) {
            headers.set("groupName", groupName);
        }

        try {
            log.debug("Getting folder details for uploadId {} from {}", uploadId, baseUrl + "uploads/" + uploadId);
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                baseUrl + "uploads/" + uploadId,
                HttpMethod.GET, 
                new HttpEntity<>(headers), 
                JsonNode.class
            );

            JsonNode responseBody = response.getBody();
            if (responseBody == null) {
                log.error("Received null response body when getting folder details for uploadId {}", uploadId);
                return -1;
            }

            if (responseBody.has("folderId")) {
                JsonNode folderId = responseBody.get("folderId");
                int result = folderId.asInt();
                log.debug("Found folderId {} for uploadId {}", result, uploadId);
                return result;
            } else if (responseBody.has("folderid")) {
                // Fallback for different field name
                JsonNode folderId = responseBody.get("folderid");
                int result = folderId.asInt();
                log.debug("Found folderId {} for uploadId {}", result, uploadId);
                return result;
            } else {
                log.error("Response for uploadId {} does not contain 'folderId' field", uploadId);
                log.debug("Response body was: {}", responseBody);
                return -1;
            }
        } catch (RestClientException e) {
            log.error("Error while getting folder details for uploadId {}: {}", uploadId, e.getMessage());
            return -1;
        }
    }

    /**
     * Legacy upload method for compatibility
     */
    public int uploadFile(String filename, InputStream fileStream, String uploadDescription) {
        CombinedUploadJobResponse response = uploadFileAndScan(filename, fileStream, uploadDescription, false);
        return response != null && "success".equals(response.getStatus()) ? response.getUploadId() : -1;
    }

    /**
     * Create error response
     */
    private CombinedUploadJobResponse createErrorResponse(String message) {
        CombinedUploadJobResponse response = new CombinedUploadJobResponse();
        response.setStatus("failed");
        response.setMessage(message);
        response.setUploadId(-1);
        response.setJobId(-1);
        return response;
    }

    /**
     * Get API version
     */
    public String getApiVersion() {
        return "v2";
    }

    /**
     * Check if v2 API is available
     */
    public boolean isV2ApiAvailable() {
        return checkConnection();
    }
}
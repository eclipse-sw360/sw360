/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 * Copyright Ritankar Saha<ritankar.saha786@gmail.com>, 2025. Part of the SW360 Portal Project.
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

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.InvalidUrlException;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Wraps the FOSSology REST API and offers an API on a higher level. Configures
 * itself by using the {@link FossologyRestConfig} available in the container.
 *
 * <pre>
 *
 * GET folders:
curl -L -X GET \
"http://[host]:[port]/repo/api/v2/folders" \
-H "Authorization: Bearer [token]"

 * POST source file with scan:
curl -L -X 'POST' \
'http://[host]:[port]/repo/api/v2/uploads' \
-H "Authorization: Bearer [token]" \
-H 'Content-Type: multipart/form-data' \
-F 'folderId=3' \
-F 'uploadDescription=string' \
-F 'uploadType=file' \
-F 'scanOptions={"analysis":{"bucket":true,"copyright_email_author":true,"ecc":true,"keyword":true,"mime":true,"monk":true,"nomos":true,"ojo":true,"package":true},"decider":{"nomos_monk":true,"bulk_reused":true,"new_scanner":true},"reuse":{"reuse_upload":0,"reuse_group":0,"reuse_main":true,"reuse_enhanced":true}}' \
-F 'fileInput=@[local-path-to-source-file]'

 * GET scan job status:
curl -L -X GET \
"http://[host]:[port]/repo/api/v2/jobs/23" \
-H "Authorization: Bearer [token]" \

 * GET report (start report generation):
curl -k -s -S -X GET \
"http://[host]:[port]/repo/api/v2/report?uploadId=19&reportFormat=spdx2" \
-H "Authorization: Bearer [token]" \

 * GET report:
curl -k -s -S -X GET \
"http://[host]:[port]/repo/api/v2/report/24" \
-H "accept: text/plain" > report.rdf.xml \
-H "Authorization: Bearer [token]"
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

    private final String expectedVersionPrefix = "2.";

    public FossologyRestClient(ObjectMapper objectMapper, FossologyRestConfig restConfig, RestTemplate restTemplate) {
        this.objectMapper = objectMapper;
        this.restConfig = restConfig;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        restTemplate.setRequestFactory(requestFactory);
        this.restTemplate = restTemplate;
    }

    /**
     * Tries to query the info and folder endpoint of the configured FOSSology
     * REST API. If this succeeds, a connection is possible.
     *
     * @return true if a connection is possible, false otherwise.
     */
    public boolean checkConnection() {
        try {
            String baseUrl = restConfig.getV2BaseUrlWithSlash();
            String token = restConfig.getAccessToken();

            if (CommonUtils.isNullEmptyOrWhitespace(baseUrl) || CommonUtils.isNullEmptyOrWhitespace(token)) {
                log.error("FOSSology v2 configuration incomplete");
                return false;
            }

            if (isCorrectInfoVersion()) {
                return canConnectToFolders();
            }
            return false;
        } catch (RestClientException e) {
            log.error("Failed to connect to FOSSology v2 API: {}", e.getMessage());
            return false;
        }
    }

    private boolean isCorrectInfoVersion() {
        ResponseEntity<String> response = sendGetRequest("info", Map.of(), List.of(), String.class);

        boolean connected = response.getStatusCode() == HttpStatus.OK;
        log.info("FOSSology v2 API connection test: {}", connected ? "SUCCESS" : "FAILED");

        if (!connected) {
            return false;
        }
        // Check if the version starts with expectedVersionPrefix
        String responseBody = response.getBody();
        if (responseBody != null) {
            try {
                // Parse JSON response to extract version
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(responseBody);
                String version = jsonNode.get("version").asText();

                // Log the version returned by the server
                log.info("FOSSology API version: {}", version);

                if (!version.startsWith(expectedVersionPrefix)) {
                    log.error("Expected FOSSology v2 API but got version: {}. Please ensure you have configured the correct v2 endpoint.", version);
                    return false;
                } else {
                    log.info("FOSSology v2 API version verification successful");
                    return true;
                }
            } catch (Exception e) {
                log.error("Failed to parse version from /info response: {}", e.getMessage());
                return false;
            }
        }
        return false;
    }

    private boolean canConnectToFolders() {
        String folderId = restConfig.getFolderId();

        ResponseEntity<String> response = sendGetRequest("folders/" + folderId, Map.of(), List.of(), String.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            log.error("Failed to connect to folders endpoint: {}",
                    response.getBody());
            return false;
        }
        log.info("Successfully connected to folders endpoint.");
        return true;
    }

    private @NotNull <T> ResponseEntity<T> sendGetRequest(
            String url, Map<String, String> queryParameters,
            List<MediaType> accepts, Class<T> type
    ) throws InvalidUrlException {
        String token = restConfig.getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        if (!accepts.isEmpty()) {
            headers.setAccept(accepts);
        }

        UriComponentsBuilder uriBuilder = createRequestUrl(url, queryParameters);

        log.debug("Sending GET request to: {}", uriBuilder.build().toUriString());

        return restTemplate.exchange(
                uriBuilder.encode().toUriString(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                type
        );
    }

    private @NotNull <T, F> ResponseEntity<T> sendPostRequest(
            String url, Map<String, String> queryParameters, MediaType mediaType,
            F body, Class<T> type
    ) throws InvalidUrlException {
        String token = restConfig.getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(mediaType);

        HttpEntity<F> requestEntity = new HttpEntity<>(body, headers);

        UriComponentsBuilder uriBuilder = createRequestUrl(url, queryParameters);

        log.debug("Sending POST request to: {} with body: {}", uriBuilder.encode().toUriString(), body);

        return restTemplate.exchange(
                uriBuilder.encode().toUriString(),
                HttpMethod.POST,
                requestEntity,
                type
        );
    }

    private UriComponentsBuilder createRequestUrl(
            String url, Map<String, String> queryParameters
    ) {
        String baseUrl = restConfig.getV2BaseUrlWithSlash();
        String groupName = restConfig.getDefaultGroup();
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(baseUrl + url);
        for (Map.Entry<String, String> entry : queryParameters.entrySet()) {
            uriBuilder.queryParam(entry.getKey(), entry.getValue());
        }
        if (groupName != null) {
            uriBuilder.queryParam("groupName", groupName);
        }
        return uriBuilder;
    }

    /**
     * Upload a file to FOSSology and schedule agents on it. Returns the upload
     * id.
     */
    public CombinedUploadJobResponse uploadFileAndScan(String filename, InputStream fileStream,
                                                       String uploadDescription) {
        String folderId = restConfig.getFolderId();

        if (isNotValidConfig()) {
            return createErrorResponse("Missing configuration values.");
        }

        if (CommonUtils.isNullEmptyOrWhitespace(filename) || fileStream == null) {
            log.error("Invalid arguments, filename must not be empty and input stream must not be null!");
            return createErrorResponse("Invalid arguments");
        }

        String url = "uploads";

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("fileInput", new FossologyInputStreamResource(filename, fileStream));
        body.add("folderId", folderId);
        if (CommonUtils.isNotNullEmptyOrWhitespace(uploadDescription)) {
            body.set("uploadDescription", uploadDescription);
        }
        body.add("ignoreScm", "false");
        body.add("uploadType", "file");

        // Add scan options for automatic scanning
        ObjectNode scanOptions = createScanOptions();
        body.add("scanOptions", scanOptions.toString());

        try {
            log.debug("Uploading file {} to FOSSology at /{}", filename, "uploads");

            ResponseEntity<JsonNode> response = sendPostRequest(url, Map.of(),
                    MediaType.MULTIPART_FORM_DATA, body, JsonNode.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseUploadResponse(response.getBody());
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
     * Parse upload response from /uploads endpoint.
     */
    private CombinedUploadJobResponse parseUploadResponse(JsonNode responseBody) {
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
        } catch (Exception e) {
            log.error("Error parsing upload response: {}", e.getMessage());
            response.setStatus("failed");
            response.setMessage("Error parsing response: " + e.getMessage());
        }

        return response;
    }

    /**
     * Extract the upload id from the /uploads endpoint response message. If the
     * upload was successful, the message should contain only the upload id.
     *
     * @param message the message from the response.
     * @return upload id if successful, -1 otherwise.
     */
    private int extractUploadIdFromMessage(String message) {
        if (CommonUtils.isNullEmptyOrWhitespace(message)) {
            return -1;
        }

        if (message.matches("\\d+")) {
            try {
                return Integer.parseInt(message);
            } catch (NumberFormatException e) {
                log.error("Failed to parse upload ID '{}' as integer: {}", message, e.getMessage());
            }
        }

        return -1;
    }

    /**
     * Generates a scan options JSON object for the /jobs or /uploads endpoint.
     * @return JSON object containing default scan options.
     */
    private ObjectNode createScanOptions() {
        ObjectNode scanOptions = objectMapper.createObjectNode();

        ObjectNode analysis = objectMapper.createObjectNode();
        analysis.put("bucket", true);
        analysis.put("copyrightEmailAuthor", true);
        analysis.put("ecc", true);
        analysis.put("ipra", true);
        analysis.put("keyword", true);
        analysis.put("mime", true);
        analysis.put("monk", true);
        analysis.put("nomos", true);
        analysis.put("ojo", true);
        analysis.put("pkgagent", true);
        analysis.put("reso", true);

        ObjectNode decider = objectMapper.createObjectNode();
        decider.put("nomosMonk", true);
        decider.put("bulkReused", true);
        decider.put("newScanner", true);
        decider.put("ojoDecider", true);

        ObjectNode reuse = objectMapper.createObjectNode();
        reuse.put("reuseUpload", 0);
        reuse.put("reuseGroup", 0);
        reuse.put("reuseMain", true);
        reuse.put("reuseEnhanced", true);
        reuse.put("reuseReport", true);
        reuse.put("reuseCopyright", true);

        scanOptions.set("analysis", analysis);
        scanOptions.set("decider", decider);
        scanOptions.set("reuse", reuse);

        return scanOptions;
    }

    /**
     * Send a post request to /jobs endpoint with `folderId` and `uploadId` as
     * query parameters. The body contains the scan options as JSON. The
     * function returns the jobId if success, -1 otherwise.
     *
     * @param uploadId the upload to start scan for.
     * @return the jobId if success, -1 otherwise.
     */
    public int startScanning(int uploadId) {
        if (isNotValidConfig()) {
            return -1;
        }

        if (uploadId < 0) {
            log.error("Invalid arguments, uploadId must not be less than 0!");
            return -1;
        }

        String folderId = restConfig.getFolderId();
        Map<String, String> params = new HashMap<>();
        params.put("folderId", folderId);
        params.put("uploadId", String.valueOf(uploadId));

        ObjectNode requestBody = createScanOptions();
        String url = "jobs";
        ResponseEntity<JsonNode> response;

        try {
            log.debug("Starting scan job for uploadId {} at /{}", uploadId, url);
            response = sendPostRequest(url, params, MediaType.APPLICATION_JSON,
                    requestBody, JsonNode.class);
        } catch (RestClientException e) {
            log.error("Error while starting scanning job for upload {} in FOSSology: {}", uploadId, e.getMessage());
            return -1;
        }

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode responseBody = response.getBody();

            // Per OpenAPI spec, should return Info schema with job id in message
            String message = responseBody.get("message").asText();
            log.info("Job scheduling response: {}", message);

            int jobId = extractJobIdFromMessage(message);
            if (jobId > 0) {
                log.info("Successfully started scan job, jobId: {}", jobId);
                return jobId;
            }
        }

        log.error("Failed to start scan job for uploadId {}. Status: {}", uploadId, response.getStatusCode());
        return -1;
    }

    /**
     * Extract job ID from /jobs/ endpoint response. The message should contain
     * the job id if everything went well. Else, it would contain an error
     * message. The idea is, if message is only integer, it is the job ID.
     */
    private int extractJobIdFromMessage(String message) {
        if (CommonUtils.isNullEmptyOrWhitespace(message)) {
            return -1;
        }

        // The message is only an integer
        if (message.matches("\\d+")) {
            try {
                return Integer.parseInt(message);
            } catch (NumberFormatException e) {
                log.error("Error extracting job ID from message '{}': {}", message, e.getMessage());
                return -1;
            }
        }

        return -1;
    }

    /**
     * Checks the status of a former started scan job, identified by the given
     * jobId.
     *
     * @param jobId the id of the scan jobId whose status should be queried.
     * @return the Map object containing details like status, eta.
     */
    public Map<String, String> checkScanStatus(int jobId) {
        Map<String, String> responseMap = new HashMap<>();
        if (isNotValidConfig()) {
            return responseMap;
        }

        if (jobId < 0) {
            log.error("Invalid arguments, jobId must not be less than 0!");
            return responseMap;
        }

        String url = "jobs/" + jobId;
        ResponseEntity<JsonNode> response;
        try {
            log.debug("Checking scan status for jobId {} at /{}", jobId, url);
            response = sendGetRequest(url, Map.of(),
                    List.of(MediaType.APPLICATION_JSON), JsonNode.class);
        } catch (RestClientException e) {
            log.error("Error while querying v2 scan status for job id {}: {}", jobId, e.getMessage());
            return responseMap;
        }

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
        if (isNotValidConfig()) {
            return -1;
        }
        if (uploadId < 0) {
            log.error("Invalid arguments, uploadId must not be less than 0!");
            return -1;
        }

        Map<String, String> params = new HashMap<>();
        params.put("uploadId", String.valueOf(uploadId));
        params.put("reportFormat", PARAMETER_VALUE_REPORT_FORMAT_SPDX2);

        String url = "report";
        ResponseEntity<JsonNode> resp;
        try {
            log.info("Starting report generation for uploadId {} at /{}", uploadId, url);
            resp = sendGetRequest(url, params, List.of(), JsonNode.class);
        } catch (RestClientException e) {
            log.error("Error while starting report for upload {}: {}", uploadId, e.getMessage());
            return -1;
        }
        if (resp.getStatusCode().value() == 201 && resp.getBody() != null) {
            JsonNode body = resp.getBody();
            if (body.has("message")) {
                int reportId = extractReportIdFromMessage(body.get("message").asText());
                if (reportId > 0) {
                    log.info("Parsed reportId={} from message", reportId);
                    return reportId;
                }
            }
        }
        log.error("Failed to start report generation (status: {}, message: {})",
                resp.getStatusCode(),
                resp.getBody() != null ? resp.getBody().toString() : "No body");
        return -1;
    }

    /**
     * The response from API contains the URL to download the report in the
     * response which is not very helpful. The URL contains the report ID at the
     * end which we are looking for here. Thus, we extract the report ID from
     * `baseUrl/report/{reportId}` URL in message and return.
     */
    private int extractReportIdFromMessage(String message) {
        if (CommonUtils.isNullEmptyOrWhitespace(message)) {
            return -1;
        }

        String reportId = message.substring(message.lastIndexOf("/") + 1);
        try {
            return Integer.parseInt(reportId);
        } catch (NumberFormatException e) {
            log.error("Failed to parse reportId '{}' as integer: {}", reportId, e.getMessage());
            return -1;
        }
    }

    /**
     * Tries to get the report identified by the given reportId as an
     * {@link InputStream}. This one will not be streamed through as
     * {@link RestTemplate} persists the response first and then offers an
     * {@link InputStream} of the persisted version. This method will also return
     * null while the report is still generated. So you might want to retry after a
     * certain period of time.
     *
     * @param reportId the id of the report to download
     * @return an InputStream containing the report content or null in case of
     *         errors or if the generation just didn't finish yet.
     */
    public InputStream getReport(int reportId) {
        if (isNotValidConfig()) {
            return null;
        }
        if (reportId < 0) {
            log.error("Invalid arguments, reportId must not be less than 0!");
            return null;
        }

        String url = "report/" + reportId;

        try {
            log.debug("Downloading report for reportId {} from {}", reportId, url);
            ResponseEntity<Resource> responseEntity = sendGetRequest(
                    url, Map.of(), List.of(MediaType.ALL), Resource.class);

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
                        reportId, responseEntity.getStatusCode().value());
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
                log.error("Error while downloading report for reportId {}: {}", reportId, e.getMessage(), e);
            }
            return null;
        } catch (Exception e) {
            log.error("Unexpected error when processing downloaded report for reportId {} (v2): {}",
                     reportId, e.getMessage());
            return null;
        }
    }

    /**
     * Checks the package unpack status of a former uploaded package, identified
     * by the given uploadId. If the ununpack finished, the response should be
     * 200 with upload object. Else, a 503 with `Look-at` header pointing to the
     * `/jobs/{jobId}` to check the status.
     *
     * @param uploadId the upload whose sources should be unpacked.
     * @return the Map object containing details like status.
     */
    public Map<String, String> checkUnpackStatus(int uploadId) {
        Map<String, String> responseMap = new HashMap<>();

        if (isNotValidConfig()) {
            return responseMap;
        }
        if (uploadId < 0) {
            log.error("Invalid arguments, uploadId must not be less than 0!");
            return responseMap;
        }

        String url = "uploads/" + uploadId;
        ResponseEntity<JsonNode> response;

        try {
            log.debug("Checking unpack status for uploadId {} at {}", uploadId, url);
            response = sendGetRequest(url, Map.of(), List.of(), JsonNode.class);
        } catch (RestClientException e) {
            log.error("Error while checking unpack status for uploadId {}: {}", uploadId, e.getMessage());
            return responseMap;
        }

        if (response.getStatusCode().is2xxSuccessful()) {
            responseMap.put("status", "Completed");
            return responseMap;
        }

        if (response.getStatusCode() != HttpStatus.SERVICE_UNAVAILABLE) {
            log.error("Failed to check unpack status for uploadId {}: {}", uploadId, response.getStatusCode());
            responseMap.put("status", "Failed");
            return responseMap;
        }

        List<String> lookAt = response.getHeaders().getOrEmpty("Look-at");
        if (lookAt.isEmpty()) {
            responseMap.put("status", "Failed");
            return responseMap;
        }

        // The response is not 200, but 503 with a valid Look-at header meaning
        // the ununpack is still in progress. Can shortcircuit here.
        // Else, get the jobId from the header and get status with
        // checkScanStatus()
        responseMap.put("status", "Processing");
        return responseMap;
    }

    /**
     * Search for files using v2 API filesearch endpoint
     */
    public int getUploadId(String shaValue, String fileName) {
        if (isNotValidConfig()) {
            return -1;
        }
        String folderId = restConfig.getFolderId();

        String url = "filesearch";

        List<Map<String, String>> body = new ArrayList<>();
        Map<String, String> shaVal = Map.of("sha1", shaValue);
        body.add(shaVal);

        log.debug("Searching for file with SHA1 {} in FOSSology at {}", shaValue, url);
        ResponseEntity<JsonNode> response;
        try {
            response = sendPostRequest(
                    url, Map.of(), MediaType.APPLICATION_JSON, body, JsonNode.class
            );
        } catch (RestClientException e) {
            log.error("Error while searching for file {} with SHA1 {}: {}", fileName, shaValue, e.getMessage());
            return -1;
        }

        JsonNode responseBody = response.getBody();
        if (responseBody == null || !responseBody.isArray() || responseBody.isEmpty()
                || !responseBody.get(0).has("uploads")
        ) {
            log.info("File with SHA1 {} not found in FOSSology", shaValue);
            return -1;
        }

        JsonNode uploads = responseBody.get(0).get("uploads");
        if (uploads == null || uploads.isEmpty() || !uploads.isArray()) {
            log.info("No uploads found for file with SHA1 {}", shaValue);
            return -1;
        }
        try {
            List<Integer> uploadIdList = new ArrayList<>();
            for (JsonNode upload : uploads) {
                uploadIdList.add(upload.asInt());
            }
            uploadIdList.sort(Collections.reverseOrder());
            log.debug("Found {} upload(s) for file with SHA1 {}", uploadIdList.size(), shaValue);

            // Find upload in target folder
            for (Integer uploadId : uploadIdList) {
                int uploadFolderId = getFolderId(uploadId);
                if (uploadFolderId != -1 && uploadFolderId == Integer.parseInt(folderId)) {
                    log.debug("Found existing upload (id={}) in target folder for file {}", uploadId, fileName);
                    return uploadId;
                }
            }

            log.info("No existing upload found in target folder for file {} with SHA1 {}", fileName, shaValue);
            return -1;
        } catch (NumberFormatException e) {
            log.error("Error parsing folder ID or upload ID: {}", e.getMessage());
            return -1;
        } catch (Exception e) {
            log.error("Unexpected error while searching for file {}: {}", fileName, e.getMessage());
            return -1;
        }
    }

    /**
     * Get folder ID for upload using v2 API
     */
    public int getFolderId(int uploadId) {
        if (isNotValidConfig()) {
            return -1;
        }
        String url = "uploads/" + uploadId;

        try {
            log.debug("Getting folder details for uploadId {} from {}", uploadId, url);
            ResponseEntity<JsonNode> response = sendGetRequest(url, Map.of(), List.of(), JsonNode.class);

            JsonNode responseBody = response.getBody();
            if (responseBody == null || responseBody.isEmpty()) {
                log.error("Received empty response body when getting folder details for uploadId: {}", uploadId);
                return -1;
            }

            if (responseBody.has("folderId")) {
                JsonNode folderId = responseBody.get("folderId");
                int result = folderId.asInt();
                log.debug("Found folderId {} for uploadId {}", result, uploadId);
                return result;
            }
            log.error("Response for uploadId {} does not contain 'folderId' field", uploadId);
            log.debug("Response body was: {}", responseBody);
            return -1;
        } catch (RestClientException e) {
            log.error("Error while getting folder details for uploadId {}: {}", uploadId, e.getMessage());
            return -1;
        }
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
     * The uploads endpoint only returns upload id, even if agents are
     * scheduled with it. To get the job id, we need to query the /jobs endpoint
     * with the upload filter and get the job id from the response.
     * @param uploadId Upload to get the job for
     */
    public int getJobIdAfterScan(int uploadId) {
        if (isNotValidConfig()) {
            return -1;
        }
        String url = "jobs";
        ResponseEntity<JsonNode> response;

        try {
            log.debug("Getting job id for uploadId {} from {}", uploadId, url);
            response = sendGetRequest(
                    url, Map.of(
                            "upload", String.valueOf(uploadId),
                            "sort", "DESC"
                    ), List.of(), JsonNode.class
            );
        } catch (RestClientException e) {
            log.error("Error while getting job id for uploadId {}: {}", uploadId, e.getMessage());
            return -1;
        }

        JsonNode responseBody = response.getBody();
        if (responseBody == null || responseBody.isEmpty()) {
            log.error("Received empty response body when getting job id for uploadId: {}", uploadId);
            return -1;
        }

        JsonNode latestJob = responseBody.get(0);
        if (latestJob != null && latestJob.has("id")) {
            int jobId = latestJob.get("id").asInt(-1);
            log.info("Found job id {} for uploadId {}", jobId, uploadId);
            return jobId;
        }
        log.error("Unable to find job id for upload: {}", uploadId);
        log.debug("Response body was: {}", responseBody);
        return -1;
    }

    private boolean isNotValidConfig() {
        String baseUrl = restConfig.getV2BaseUrlWithSlash();
        String token = restConfig.getAccessToken();
        String folderId = restConfig.getFolderId();

        if (CommonUtils.isNullEmptyOrWhitespace(baseUrl) || CommonUtils.isNullEmptyOrWhitespace(token) || CommonUtils.isNullEmptyOrWhitespace(folderId)) {
            log.error("Configuration is missing values! Url: <{}>, Token: <{}>, Folder: <{}>", baseUrl, token,
                    folderId);
            return true;
        }
        return false;
    }
}

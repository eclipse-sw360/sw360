/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
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
        String baseUrl = restConfig.getBaseUrlWithSlash();
        String token = restConfig.getAccessToken();

        if (StringUtils.isEmpty(baseUrl) || StringUtils.isEmpty(token)) {
            return false;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);

        ResponseEntity<String> response = restTemplate.exchange(baseUrl + "folders",
                HttpMethod.GET, new HttpEntity<>(headers), String.class);

        return response.getStatusCode() == HttpStatus.OK;
    }

    /**
     * Uploads the file provided in an {@link InputStream} under the given filename
     * to FOSSology.
     *
     * @param filename   the name of the file
     * @param fileStream the content of the file
     * @return the uploadId provided by FOSSology in case of an successful upload,
     *         -1 otherwise
     */
    public int uploadFile(String filename, InputStream fileStream, String uploadDescription) {
        String baseUrl = restConfig.getBaseUrlWithSlash();
        String token = restConfig.getAccessToken();
        String folderId = restConfig.getFolderId();

        if (StringUtils.isEmpty(baseUrl) || StringUtils.isEmpty(token) || StringUtils.isEmpty(folderId)) {
            log.error("Configuration is missing values! Url: <{}>, Token: <{}>, Folder: <{}>", baseUrl, token,
                    folderId);
            return -1;
        }

        if (StringUtils.isEmpty(filename) || fileStream == null) {
            log.error("Invalid arguments, filename must not be empty and input stream must not be null!");
            return -1;
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

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        FossologyResponse response = null;
        try {
            log.debug("Attempting to upload file {} to Fossology at {}", filename, baseUrl + "uploads");
            response = restTemplate.postForObject(baseUrl + "uploads", requestEntity, FossologyResponse.class);
            log.debug(filename + " uploaded: " + response.getCode() + " - " + response.getMessage() + " - " + response.getType());
        } catch (RestClientException e) {
            log.error("Error while uploading file '{}' to Fossology. HTTP request failed: {}", filename, e.getMessage());
            log.debug("Detailed upload error:", e);
            return -1;
        }

        HttpStatus responseCode = HttpStatus.valueOf(response.getCode());
        if (responseCode.is2xxSuccessful()) {
            try {
                return Integer.valueOf(response.getMessage());
            } catch (NumberFormatException e) {
                log.error("Fossology REST returned non-integer uploadId: '{}'. Error: {}", response.getMessage(), e.getMessage());
                log.debug("Full number format exception:", e);
            }
        } else {
            log.error("Fossology upload failed with status code {} and message: {}", response.getCode(), response.getMessage());
        }

        return -1;
    }

    /**
     * Triggers a scan of a former upload whose uploadId must be given. The
     * configuration, which scanners to use, is currently hardcoded and cannot be
     * configured.
     *
     * @param uploadId the upload whose sources should be scanned.
     * @return the jobId provided by FOSSology in case of an successful start, -1
     *         otherwise
     */
    public int startScanning(int uploadId) {
        String baseUrl = restConfig.getBaseUrlWithSlash();
        String token = restConfig.getAccessToken();
        String folderId = restConfig.getFolderId();

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
        headers.set("Authorization", "Bearer " + token);
        headers.set("folderId", folderId);
        headers.set("uploadId", uploadId + "");
        headers.set("Content-Type", "application/json");

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

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.set("analysis", analysis);
        requestBody.set("decider", decider);
        requestBody.set("reuse", reuse);

        FossologyResponse response;
        try {
            log.debug("Starting scan job for uploadId {} at {}", uploadId, baseUrl + "jobs");
            response = restTemplate.postForObject(baseUrl + "jobs", new HttpEntity<>(requestBody, headers),
                    FossologyResponse.class);
        } catch (RestClientException e) {
            log.error("Error while starting scanning job for upload {} in Fossology: {}", uploadId, e.getMessage());
            log.debug("Detailed scanning error:", e);
            return -1;
        }

        if (HttpStatus.valueOf(response.getCode()).is2xxSuccessful()) {
            log.debug("Successfully started scan job for uploadId {}, received jobId {}", uploadId, response.getMessage());
            return Integer.valueOf(response.getMessage());
        } else {
            log.error("Failed to start scan job for uploadId {}. Status: {}, Message: {}", uploadId, response.getCode(), response.getMessage());
            return -1;
        }
    }

    /**
     * Checks the status of a former started scan job, identified by the given
     * jobId.
     *
     * @param jobId the id of the scan jobId whose status should be queried.
     * @return the Map object containing details like status, eta.
     */
    public Map<String ,String> checkScanStatus(int jobId) {
        String baseUrl = restConfig.getBaseUrlWithSlash();
        String token = restConfig.getAccessToken();
        Map<String ,String> responseMap=new HashMap<>();

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

        ResponseEntity<JsonNode> response;
        try {
            log.debug("Checking scan status for jobId {} at {}", jobId, baseUrl + "jobs/" + jobId);
            response = restTemplate.exchange(baseUrl + "jobs/" + jobId, HttpMethod.GET, new HttpEntity<>(headers),
                    JsonNode.class);
        } catch (RestClientException e) {
            log.error("Error while querying scan status for job id {}: {}", jobId, e.getMessage());
            log.debug("Detailed scan status error:", e);
            return responseMap;
        }

        JsonNode body = response.getBody();
        if (body == null) {
            log.error("Received empty response body when checking scan status for job id {}", jobId);
            return responseMap;
        }

        try {
            String status = body.findValuesAsText("status").get(0);
            int eta = body.get("eta").intValue();
            responseMap.put("eta", eta+"");
            responseMap.put("status", status);
            log.debug("Scan status for jobId {}: Status={}, ETA={}", jobId, status, eta);
        } catch (Exception e) {
            log.error("Error parsing scan status response for job id {}: {}", jobId, e.getMessage());
            log.debug("Response body was: {}", body);
            log.debug("Detailed parse error:", e);
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
        String baseUrl = restConfig.getBaseUrlWithSlash();
        String token = restConfig.getAccessToken();

        if (StringUtils.isEmpty(baseUrl) || StringUtils.isEmpty(token)) {
            log.error("Configuration is missing values! Url: <{}>, Token: <{}>", baseUrl, token);
            return -1;
        }

        if (uploadId < 0) {
            log.error("Invalid arguments, uploadId must not be less than 0!");
            return -1;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("uploadId", uploadId + "");
        headers.set("reportFormat", PARAMETER_VALUE_REPORT_FORMAT_SPDX2);

        FossologyResponse response;
        try {
            log.debug("Starting report generation for uploadId {} at {}", uploadId, baseUrl + "report");
            ResponseEntity<FossologyResponse> responseEntity = restTemplate.exchange(baseUrl + "report", HttpMethod.GET,
                    new HttpEntity<>(headers), FossologyResponse.class);
            response = responseEntity.getBody();
            if (response == null) {
                log.error("Received null response body when starting report generation for uploadId {}", uploadId);
                return -1;
            }
        } catch (RestClientException e) {
            log.error("Error while starting report generation for upload {}: {}", uploadId, e.getMessage());
            log.debug("Detailed report generation error:", e);
            return -1;
        }

        if (HttpStatus.valueOf(response.getCode()).is2xxSuccessful()) {
            String reportId = response.getMessage().substring(response.getMessage().lastIndexOf("/") + 1);
            log.debug("Successfully started report generation for uploadId {}, received reportId {}", uploadId, reportId);
            try {
                return Integer.valueOf(reportId);
            } catch (NumberFormatException e) {
                log.error("Failed to parse reportId '{}' as integer: {}", reportId, e.getMessage());
                return -1;
            }
        } else {
            log.error("Failed to start report generation for uploadId {}. Status: {}, Message: {}",
                    uploadId, response.getCode(), response.getMessage());
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
        String baseUrl = restConfig.getBaseUrlWithSlash();
        String token = restConfig.getAccessToken();

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

        try {
            log.debug("Downloading report for reportId {} from {}", reportId, baseUrl + "report/" + reportId);
            ResponseEntity<Resource> responseEntity = restTemplate.exchange(baseUrl + "report/" + reportId,
                    HttpMethod.GET, new HttpEntity<>(headers), Resource.class);

            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                Resource reportResource = responseEntity.getBody();
                if (reportResource == null) {
                    log.error("Received null resource when downloading report for reportId {}", reportId);
                    return null;
                }
                log.debug("Successfully downloaded report for reportId {}", reportId);
                return reportResource.getInputStream();
            } else {
                log.error("Failed to download report for reportId {}. HTTP status: {}",
                        reportId, responseEntity.getStatusCode());
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
                log.error("Error while downloading report for reportId {}: {}", reportId, e.getMessage());
                log.debug("Detailed report download error:", e);
            }
            return null;
        } catch (IOException e) {
            log.error("I/O error when processing downloaded report for reportId {}: {}", reportId, e.getMessage());
            log.debug("Detailed I/O error:", e);
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
        String baseUrl = restConfig.getBaseUrlWithSlash();
        String token = restConfig.getAccessToken();
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
        JsonNode response;
        try {
            log.debug("Checking unpack status for uploadId {} at {}", uploadId, baseUrl + "jobs?upload=" + uploadId);
            JsonNode[] responseArr = restTemplate.exchange(baseUrl + "jobs?upload=" + uploadId, HttpMethod.GET,
                    new HttpEntity<>(headers), JsonNode[].class).getBody();

            if (responseArr == null || responseArr.length == 0) {
                log.error("Received empty response when checking unpack status for uploadId {}", uploadId);
                return responseMap;
            }

            String uploadIdStr = "" + uploadId;
            List<JsonNode> uploadStatus = Arrays.stream(responseArr)
                    .filter(node -> {
                        List<String> uploadIds = node.findValuesAsText("uploadId");
                        return !uploadIds.isEmpty() && uploadIdStr.equals(uploadIds.get(0));
                    })
                    .collect(Collectors.toList());

            if (!uploadStatus.isEmpty()) {
                response = uploadStatus.get(0);

                String status = response.findValuesAsText("status").get(0);
                responseMap.put("status", status);
                log.debug("Unpack status for uploadId {}: {}", uploadId, status);
            } else {
                log.warn("No status information found for uploadId {} in Fossology response", uploadId);
            }

            return responseMap;
        } catch (RestClientException e) {
            log.error("Error while checking unpack status for uploadId {}: {}", uploadId, e.getMessage());
            log.debug("Detailed unpack status error:", e);
            return responseMap;
        } catch (Exception e) {
            log.error("Unexpected error while processing unpack status for uploadId {}: {}", uploadId, e.getMessage());
            log.debug("Detailed error:", e);
            return responseMap;
        }
    }

    public int getFolderId(String uploadId) {
        String baseUrl = restConfig.getBaseUrlWithSlash();
        String token = restConfig.getAccessToken();

        if (StringUtils.isEmpty(baseUrl) || StringUtils.isEmpty(token)) {
            log.error("Configuration is missing values! Url: <{}>, Token: <{}>", baseUrl, token);
            return -1;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);

        try {
            log.debug("Getting folder details for uploadId {} from {}", uploadId, baseUrl + "uploads/" + uploadId);
            ResponseEntity<JsonNode> response = restTemplate.exchange(baseUrl + "uploads/" + uploadId,
                    HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);

            JsonNode responseBody = response.getBody();
            if (responseBody == null) {
                log.error("Received null response body when getting folder details for uploadId {}", uploadId);
                return -1;
            }

            if (!responseBody.has("folderid")) {
                log.error("Response for uploadId {} does not contain 'folderid' field", uploadId);
                log.debug("Response body was: {}", responseBody);
                return -1;
            }

            JsonNode folderId = responseBody.get("folderid");
            int result = folderId.asInt();
            log.debug("Found folderId {} for uploadId {}", result, uploadId);
            return result;
        } catch (RestClientException e) {
            log.error("Error while getting folder details for uploadId {}: {}", uploadId, e.getMessage());
            log.debug("Detailed error:", e);
            return -1;
        }
    }

    public int getUploadId(String shaValue, String fileName) {
        String baseUrl = restConfig.getBaseUrlWithSlash();
        String token = restConfig.getAccessToken();
        String folderId = restConfig.getFolderId();

        if (StringUtils.isEmpty(baseUrl) || StringUtils.isEmpty(token)) {
            log.error("Configuration is missing values! Url: <{}>, Token: <{}>", baseUrl, token);
            return -1;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        List<Map<String, String>> body = new ArrayList<>();
        Map<String, String> shaVal = new HashMap<>();
        shaVal.put("sha1", shaValue);
        body.add(shaVal);

        int lastUploadedValue = -1;

        HttpEntity<List<Map<String, String>>> requestEntity = new HttpEntity<>(body, headers);

        try {
            log.debug("Searching for file with SHA1 {} in Fossology at {}", shaValue, baseUrl + "filesearch");
            ResponseEntity<JsonNode> response = restTemplate.exchange(baseUrl + "filesearch",
                    HttpMethod.POST, requestEntity, JsonNode.class);

            JsonNode responseBody = response.getBody();
            if (responseBody == null || !responseBody.isArray() || responseBody.size() == 0) {
                log.error("Invalid or empty response when searching for file with SHA1 {}", shaValue);
                return -1;
            }

            JsonNode firstElement = responseBody.get(0);
            if (!firstElement.has("uploads")) {
                log.warn("File with SHA1 {} not found in Fossology", shaValue);
                return -1;
            }

            JsonNode uploads = firstElement.get("uploads");
            List<String> uploadIdList = new ArrayList<>();
            if (uploads != null && uploads.isArray()) {
                for (JsonNode upload : uploads) {
                    uploadIdList.add(upload.asText());
                }
                uploadIdList.sort(Collections.reverseOrder());
                log.debug("Found {} upload(s) for file with SHA1 {}", uploadIdList.size(), shaValue);
            } else {
                log.warn("No uploads found for file with SHA1 {}", shaValue);
                return -1;
            }

            for (String uploadId : uploadIdList) {
                int id = getFolderId(uploadId);
                if (id != -1 && id == Integer.parseInt(folderId)) {
                    lastUploadedValue = Integer.parseInt(uploadId);
                    log.debug("Found existing upload (id={}) in target folder for file {}", lastUploadedValue, fileName);
                    break;
                }
            }

            if (lastUploadedValue == -1) {
                log.info("No existing upload found in target folder for file {} with SHA1 {}", fileName, shaValue);
            }

            return lastUploadedValue;
        } catch (RestClientException e) {
            log.error("Error while searching for file {} with SHA1 {}: {}", fileName, shaValue, e.getMessage());
            log.debug("Detailed search error:", e);
            return -1;
        } catch (NumberFormatException e) {
            log.error("Error parsing folder ID or upload ID: {}", e.getMessage());
            return -1;
        } catch (Exception e) {
            log.error("Unexpected error while searching for file {}: {}", fileName, e.getMessage());
            log.debug("Detailed error:", e);
            return -1;
        }
    }
}

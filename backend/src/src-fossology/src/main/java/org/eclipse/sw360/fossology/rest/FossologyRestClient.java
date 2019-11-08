/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.fossology.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.eclipse.sw360.fossology.config.FossologyRestConfig;

import org.apache.commons.lang.StringUtils;
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

    private static final String SCAN_RESPONSE_STATUS_VALUE_QUEUED = "Queued";
    private static final String SCAN_RESPONSE_STATUS_VALUE_PROCESSING = "Processing";
    private static final String SCAN_RESPONSE_STATUS_VALUE_COMPLETED = "Completed";
    private static final String SCAN_RESPONSE_STATUS_VALUE_FAILED = "Failed";

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
        requestFactory.setBufferRequestBody(false);
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
    public int uploadFile(String filename, InputStream fileStream) {
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

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("fileInput", new FossologyInputStreamResource(filename, fileStream));

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        FossologyResponse response = null;
        try {
            response = restTemplate.postForObject(baseUrl + "uploads", requestEntity, FossologyResponse.class);
            log.debug(filename + "uploaded: " + response.getCode() + " - " + response.getMessage() + " - " + response.getType());
        } catch (RestClientException e) {
            log.error("Error while trying to upload file {}.", e, filename);
            return -1;
        }

        HttpStatus responseCode = HttpStatus.valueOf(response.getCode());
        if (responseCode.is2xxSuccessful()) {
            try {
                return Integer.valueOf(response.getMessage());
            } catch (NumberFormatException e) {
                log.error("Fossology REST returned non integer uploadId: {}", e, response.getMessage());
            }
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
            log.error("Invalid arguments, uploadId must not be less thann 0!");
            return -1;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("folderId", folderId);
        headers.set("uploadId", uploadId + "");

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
            response = restTemplate.postForObject(baseUrl + "jobs", new HttpEntity<>(requestBody, headers),
                    FossologyResponse.class);
        } catch (RestClientException e) {
            log.error("Error while trying to start scanning of upload {}.", e, uploadId);
            return -1;
        }

        return HttpStatus.valueOf(response.getCode()).is2xxSuccessful() ? Integer.valueOf(response.getMessage()) : -1;
    }

    /**
     * Checks the status of a former started scan job, identified by the given
     * jobId.
     *
     * @param jobId the id of the scan jobId whose status should be queried.
     * @return an integer denoting the current status of the scan job, 1 for
     *         completed, 0 for running, -1 for failure
     */
    public int checkScanStatus(int jobId) {
        String baseUrl = restConfig.getBaseUrlWithSlash();
        String token = restConfig.getAccessToken();

        if (StringUtils.isEmpty(baseUrl) || StringUtils.isEmpty(token)) {
            log.error("Configuration is missing values! Url: <{}>, Token: <{}>", baseUrl, token);
            return -1;
        }

        if (jobId < 0) {
            log.error("Invalid arguments, jobId must not be less thann 0!");
            return -1;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);

        ResponseEntity<JsonNode> response;
        try {
            response = restTemplate.exchange(baseUrl + "jobs/" + jobId, HttpMethod.GET, new HttpEntity<>(headers),
                    JsonNode.class);
        } catch (RestClientException e) {
            log.error("Error while trying to query status of scanning process with job id {}.", e, jobId);
            return -1;
        }

        String status = response.getBody().findValuesAsText("status").get(0);
        switch (status) {
        case SCAN_RESPONSE_STATUS_VALUE_COMPLETED:
            return 1;
        case SCAN_RESPONSE_STATUS_VALUE_QUEUED:
        case SCAN_RESPONSE_STATUS_VALUE_PROCESSING:
            return 0;
        case SCAN_RESPONSE_STATUS_VALUE_FAILED:
        default:
            return -1;
        }
    }

    /**
     * Triggers a report generatoin of a former upload whose uploadId must be given.
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
            log.error("Invalid arguments, uploadId must not be less thann 0!");
            return -1;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("uploadId", uploadId + "");
        headers.set("reportFormat", PARAMETER_VALUE_REPORT_FORMAT_SPDX2);

        FossologyResponse response;
        try {
            ResponseEntity<FossologyResponse> responseEntity = restTemplate.exchange(baseUrl + "report", HttpMethod.GET,
                    new HttpEntity<>(headers), FossologyResponse.class);
            response = responseEntity.getBody();
        } catch (RestClientException e) {
            log.error("Error while trying to start report generation of upload {}.", e, uploadId);
            return -1;
        }

        return HttpStatus.valueOf(response.getCode()).is2xxSuccessful()
                ? Integer.valueOf(response.getMessage().substring(response.getMessage().lastIndexOf("/") + 1))
                : -1;
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
            log.error("Invalid arguments, reportId must not be less thann 0!");
            return null;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);

        try {
            ResponseEntity<Resource> responseEntity = restTemplate.exchange(baseUrl + "report/" + reportId,
                    HttpMethod.GET, new HttpEntity<>(headers), Resource.class);

            return responseEntity.getBody().getInputStream();
        } catch (RestClientException | IOException e) {
            // we could distinguish further since fossology would send a 503 with
            // Retry-After header in seconds if the report isn't ready yet. But since our
            // workflow is currently completely pull based there would not be a huge
            // advantage to use this and it would just complicate things right now.
            log.error("Error while trying to download report with id {}.", e, reportId);
            return null;
        }
    }
}

/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.licenseinfo;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.eclipse.sw360.datahandler.services.components.Release;
import org.eclipse.sw360.datahandler.services.licenseinfo.CreateLicenseToObligationMappingRequest;
import org.eclipse.sw360.datahandler.services.licenseinfo.EvaluateAttachmentsRequest;
import org.eclipse.sw360.datahandler.services.licenseinfo.GetLicenseInfoFileRequest;
import org.eclipse.sw360.datahandler.services.licenseinfo.GetLicenseInfoForAttachmentRequest;
import org.eclipse.sw360.datahandler.services.licenseinfo.GetObligationsForAttachmentRequest;
import org.eclipse.sw360.datahandler.services.licenseinfo.GetProjectObligationStatusRequest;
import org.eclipse.sw360.datahandler.services.licenseinfo.LicenseInfoFile;
import org.eclipse.sw360.datahandler.services.licenseinfo.LicenseInfoParsingResult;
import org.eclipse.sw360.datahandler.services.licenseinfo.LicenseNameWithText;
import org.eclipse.sw360.datahandler.services.licenseinfo.LicenseObligationsStatusInfo;
import org.eclipse.sw360.datahandler.services.licenseinfo.ObligationParsingResult;
import org.eclipse.sw360.datahandler.services.licenseinfo.OutputFormatInfo;
import org.eclipse.sw360.datahandler.services.licenseinfo.OutputFormatVariant;
import org.eclipse.sw360.datahandler.services.projects.ObligationStatusInfo;
import org.eclipse.sw360.datahandler.services.projects.Project;
import org.eclipse.sw360.datahandler.services.users.User;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * HTTP implementation of {@link LicenseInfoClient}.
 *
 * Maps to {@code LicenseInfoController} under {@code /licenseinfo/api/licenseinfo}.
 * License-info file responses use an octet-stream body plus {@code X-SW360-Output-*} headers.
 */
public class LicenseInfoServiceRestClient implements LicenseInfoClient {

    private static final String BASE = "/licenseinfo/api/licenseinfo";

    static final String HDR_FILE_EXTENSION = "X-SW360-Output-File-Extension";
    static final String HDR_DESCRIPTION = "X-SW360-Output-Description";
    static final String HDR_GENERATOR_CLASS = "X-SW360-Output-Generator-Class-Name";
    static final String HDR_IS_BINARY = "X-SW360-Output-Is-Binary";
    static final String HDR_MIME_TYPE = "X-SW360-Output-Mime-Type";
    static final String HDR_VARIANT = "X-SW360-Output-Variant";

    private static final ParameterizedTypeReference<List<LicenseInfoParsingResult>> LICENSE_INFO_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<ObligationParsingResult>> OBLIGATION_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<OutputFormatInfo>> OUTPUT_FORMAT_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Map<String, Map<String, String>>> STRING_MAP_MAP =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public LicenseInfoServiceRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    private static <T> T call(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (RestClientResponseException e) {
            String body = e.getResponseBodyAsString();
            throw new SW360Exception(body == null || body.isEmpty() ? e.getMessage() : body,
                    e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw new SW360Exception(e.getMessage(), 503, e);
        }
    }

    private static void addUser(HttpHeaders headers, User user) {
        if (user == null) {
            return;
        }
        if (user.getEmail() != null) {
            headers.set("X-User-Email", user.getEmail());
        }
        if (user.getDepartment() != null) {
            headers.set("X-User-Department", user.getDepartment());
        }
        if (user.getUserGroup() != null) {
            headers.set("X-User-Group", user.getUserGroup().name());
        }
    }

    @Override
    public List<LicenseInfoParsingResult> getLicenseInfoForAttachment(Release release, String attachmentContentId,
            boolean includeConcludedLicense, User user) {
        GetLicenseInfoForAttachmentRequest body = new GetLicenseInfoForAttachmentRequest()
                .setRelease(release)
                .setAttachmentContentId(attachmentContentId)
                .setIncludeConcludedLicense(includeConcludedLicense);
        List<LicenseInfoParsingResult> list = call(() -> restClient.post()
                .uri(BASE + "/attachment/license-info")
                .headers(h -> addUser(h, user))
                .body(body)
                .retrieve()
                .body(LICENSE_INFO_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<ObligationParsingResult> getObligationsForAttachment(Release release, String attachmentContentId,
            User user) {
        GetObligationsForAttachmentRequest body = new GetObligationsForAttachmentRequest()
                .setRelease(release)
                .setAttachmentContentId(attachmentContentId);
        List<ObligationParsingResult> list = call(() -> restClient.post()
                .uri(BASE + "/attachment/obligations")
                .headers(h -> addUser(h, user))
                .body(body)
                .retrieve()
                .body(OBLIGATION_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public LicenseObligationsStatusInfo getProjectObligationStatus(
            Map<String, ObligationStatusInfo> obligationStatusMap,
            List<LicenseInfoParsingResult> licenseInfoResults,
            Map<String, String> excludedReleaseIdToAcceptedCLI) {
        GetProjectObligationStatusRequest body = new GetProjectObligationStatusRequest()
                .setObligationStatusMap(obligationStatusMap)
                .setLicenseInfoResults(licenseInfoResults)
                .setExcludedReleaseIdToAcceptedCLI(excludedReleaseIdToAcceptedCLI);
        return call(() -> restClient.post()
                .uri(BASE + "/project-obligation-status")
                .body(body)
                .retrieve()
                .body(LicenseObligationsStatusInfo.class));
    }

    @Override
    public LicenseInfoParsingResult createLicenseToObligationMapping(LicenseInfoParsingResult licenseInfoResult,
            ObligationParsingResult obligationInfoResult) {
        CreateLicenseToObligationMappingRequest body = new CreateLicenseToObligationMappingRequest()
                .setLicenseInfoResult(licenseInfoResult)
                .setObligationInfoResult(obligationInfoResult);
        return call(() -> restClient.post()
                .uri(BASE + "/license-obligation-mapping")
                .body(body)
                .retrieve()
                .body(LicenseInfoParsingResult.class));
    }

    @Override
    public LicenseInfoFile getLicenseInfoFile(Project project, User user, String outputGeneratorClassName,
            Map<String, Map<String, Boolean>> releaseIdsToSelectedAttachmentIds,
            Map<String, Set<LicenseNameWithText>> excludedLicensesPerAttachment, String externalIds, String fileName) {
        return getLicenseInfoFile(project, user, outputGeneratorClassName, releaseIdsToSelectedAttachmentIds,
                excludedLicensesPerAttachment, externalIds, fileName, false);
    }

    @Override
    public LicenseInfoFile getLicenseInfoFile(Project project, User user, String outputGeneratorClassName,
            Map<String, Map<String, Boolean>> releaseIdsToSelectedAttachmentIds,
            Map<String, Set<LicenseNameWithText>> excludedLicensesPerAttachment, String externalIds, String fileName,
            boolean excludeReleaseVersion) {
        GetLicenseInfoFileRequest body = new GetLicenseInfoFileRequest()
                .setProject(project)
                .setOutputGeneratorClassName(outputGeneratorClassName)
                .setReleaseIdsToSelectedAttachmentIds(releaseIdsToSelectedAttachmentIds)
                .setExcludedLicensesPerAttachment(excludedLicensesPerAttachment)
                .setExternalIds(externalIds)
                .setFileName(fileName)
                .setExcludeReleaseVersion(excludeReleaseVersion);

        ResponseEntity<byte[]> response = call(() -> restClient.post()
                .uri(BASE + "/license-info-file")
                .headers(h -> addUser(h, user))
                .body(body)
                .retrieve()
                .toEntity(byte[].class));

        return assembleLicenseInfoFile(response);
    }

    static LicenseInfoFile assembleLicenseInfoFile(ResponseEntity<byte[]> response) {
        LicenseInfoFile file = new LicenseInfoFile();
        file.setGeneratedOutput(response.getBody() == null ? new byte[0] : response.getBody());

        HttpHeaders headers = response.getHeaders();
        OutputFormatInfo format = new OutputFormatInfo();
        String ext = headers.getFirst(HDR_FILE_EXTENSION);
        if (ext != null) {
            format.setFileExtension(ext);
        }
        String desc = headers.getFirst(HDR_DESCRIPTION);
        if (desc != null) {
            format.setDescription(desc);
        }
        String generator = headers.getFirst(HDR_GENERATOR_CLASS);
        if (generator != null) {
            format.setGeneratorClassName(generator);
        }
        String isBinary = headers.getFirst(HDR_IS_BINARY);
        if (isBinary != null) {
            format.setIsOutputBinary(Boolean.parseBoolean(isBinary));
        }
        String mime = headers.getFirst(HDR_MIME_TYPE);
        if (mime != null) {
            format.setMimeType(mime);
        }
        String variant = headers.getFirst(HDR_VARIANT);
        if (variant != null && !variant.isBlank()) {
            try {
                format.setVariant(OutputFormatVariant.valueOf(variant));
            } catch (IllegalArgumentException ignored) {
                // leave unset if unknown
            }
        }
        file.setOutputFormatInfo(format);
        return file;
    }

    @Override
    public List<OutputFormatInfo> getPossibleOutputFormats() {
        List<OutputFormatInfo> list = call(() -> restClient.get()
                .uri(BASE + "/output-formats")
                .retrieve()
                .body(OUTPUT_FORMAT_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public OutputFormatInfo getOutputFormatInfoForGeneratorClass(String generatorClassName) {
        return call(() -> restClient.get()
                .uri(BASE + "/output-formats/{generatorClassName}", generatorClassName)
                .retrieve()
                .body(OutputFormatInfo.class));
    }

    @Override
    public String getDefaultLicenseInfoHeaderText(String fileName) {
        return call(() -> {
            if (fileName == null || fileName.isBlank()) {
                return restClient.get()
                        .uri(BASE + "/defaults/license-info-header")
                        .retrieve()
                        .body(String.class);
            }
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder.path(BASE + "/defaults/license-info-header")
                            .queryParam("fileName", fileName)
                            .build())
                    .retrieve()
                    .body(String.class);
        });
    }

    @Override
    public String getDefaultObligationsText() {
        return call(() -> restClient.get()
                .uri(BASE + "/defaults/obligations-text")
                .retrieve()
                .body(String.class));
    }

    @Override
    public Map<String, Map<String, String>> evaluateAttachments(String releaseId, User user) {
        EvaluateAttachmentsRequest body = new EvaluateAttachmentsRequest().setReleaseId(releaseId);
        Map<String, Map<String, String>> map = call(() -> restClient.post()
                .uri(BASE + "/evaluate-attachments")
                .headers(h -> addUser(h, user))
                .body(body)
                .retrieve()
                .body(STRING_MAP_MAP));
        return map == null ? Map.of() : map;
    }
}

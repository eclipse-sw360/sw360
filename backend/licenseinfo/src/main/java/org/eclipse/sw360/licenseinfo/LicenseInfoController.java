/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.licenseinfo;

import java.util.List;
import java.util.Map;

import org.apache.thrift.TException;
import org.eclipse.sw360.common.utils.UserUtils;
import org.eclipse.sw360.datahandler.services.licenseinfo.CreateLicenseToObligationMappingRequest;
import org.eclipse.sw360.datahandler.services.licenseinfo.EvaluateAttachmentsRequest;
import org.eclipse.sw360.datahandler.services.licenseinfo.GetLicenseInfoFileRequest;
import org.eclipse.sw360.datahandler.services.licenseinfo.GetLicenseInfoForAttachmentRequest;
import org.eclipse.sw360.datahandler.services.licenseinfo.GetObligationsForAttachmentRequest;
import org.eclipse.sw360.datahandler.services.licenseinfo.GetProjectObligationStatusRequest;
import org.eclipse.sw360.datahandler.services.licenseinfo.LicenseInfoParsingResult;
import org.eclipse.sw360.datahandler.services.licenseinfo.LicenseObligationsStatusInfo;
import org.eclipse.sw360.datahandler.services.licenseinfo.ObligationParsingResult;
import org.eclipse.sw360.datahandler.services.licenseinfo.OutputFormatInfo;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoFile;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/licenseinfo")
public class LicenseInfoController {

    /** Header prefix for OutputFormatInfo metadata on binary file responses. */
    public static final String HDR_FILE_EXTENSION = "X-SW360-Output-File-Extension";
    public static final String HDR_DESCRIPTION = "X-SW360-Output-Description";
    public static final String HDR_GENERATOR_CLASS = "X-SW360-Output-Generator-Class-Name";
    public static final String HDR_IS_BINARY = "X-SW360-Output-Is-Binary";
    public static final String HDR_MIME_TYPE = "X-SW360-Output-Mime-Type";
    public static final String HDR_VARIANT = "X-SW360-Output-Variant";

    private final LicenseInfoHandler handler;

    public LicenseInfoController(LicenseInfoHandler handler) {
        this.handler = handler;
    }

    private static User user(String email, String department, String userGroup) {
        return UserUtils.buildUser(email, department, userGroup);
    }

    @PostMapping("/attachment/license-info")
    public List<LicenseInfoParsingResult> getLicenseInfoForAttachment(
            @RequestBody GetLicenseInfoForAttachmentRequest request,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        return LicenseInfoRestMapper.fromThriftLicenseInfoParsingResults(
                handler.getLicenseInfoForAttachment(
                        LicenseInfoRestMapper.toThriftRelease(request.getRelease()),
                        request.getAttachmentContentId(),
                        request.isIncludeConcludedLicense(),
                        user(email, department, userGroup)));
    }

    @PostMapping("/attachment/obligations")
    public List<ObligationParsingResult> getObligationsForAttachment(
            @RequestBody GetObligationsForAttachmentRequest request,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        return LicenseInfoRestMapper.fromThriftObligationParsingResults(
                handler.getObligationsForAttachment(
                        LicenseInfoRestMapper.toThriftRelease(request.getRelease()),
                        request.getAttachmentContentId(),
                        user(email, department, userGroup)));
    }

    @PostMapping("/project-obligation-status")
    public LicenseObligationsStatusInfo getProjectObligationStatus(
            @RequestBody GetProjectObligationStatusRequest request) {
        return LicenseInfoRestMapper.fromThriftLicenseObligationsStatusInfo(
                handler.getProjectObligationStatus(
                        LicenseInfoRestMapper.toThriftObligationStatusMap(request.getObligationStatusMap()),
                        LicenseInfoRestMapper.toThriftLicenseInfoParsingResults(request.getLicenseInfoResults()),
                        request.getExcludedReleaseIdToAcceptedCLI()));
    }

    @PostMapping("/license-obligation-mapping")
    public LicenseInfoParsingResult createLicenseToObligationMapping(
            @RequestBody CreateLicenseToObligationMappingRequest request) throws TException {
        return LicenseInfoRestMapper.fromThriftLicenseInfoParsingResult(
                handler.createLicenseToObligationMapping(
                        LicenseInfoRestMapper.toThriftLicenseInfoParsingResult(request.getLicenseInfoResult()),
                        LicenseInfoRestMapper.toThriftObligationParsingResult(request.getObligationInfoResult())));
    }

    /**
     * Generates a license info file. Response body is raw bytes; OutputFormatInfo is
     * carried in {@code X-SW360-Output-*} headers (avoids Base64 JSON overhead).
     */
    @PostMapping("/license-info-file")
    public ResponseEntity<byte[]> getLicenseInfoFile(
            @RequestBody GetLicenseInfoFileRequest request,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        LicenseInfoFile file = handler.getLicenseInfoFileWithoutReleaseVersion(
                LicenseInfoRestMapper.toThriftProject(request.getProject()),
                user(email, department, userGroup),
                request.getOutputGeneratorClassName(),
                request.getReleaseIdsToSelectedAttachmentIds(),
                LicenseInfoRestMapper.toThriftExcludedLicenses(request.getExcludedLicensesPerAttachment()),
                request.getExternalIds(),
                request.getFileName(),
                request.isExcludeReleaseVersion());

        org.eclipse.sw360.datahandler.thrift.licenseinfo.OutputFormatInfo format = file.getOutputFormatInfo();
        HttpHeaders headers = new HttpHeaders();
        if (format != null) {
            if (format.isSetFileExtension()) {
                headers.set(HDR_FILE_EXTENSION, format.getFileExtension());
            }
            if (format.isSetDescription()) {
                headers.set(HDR_DESCRIPTION, format.getDescription());
            }
            if (format.isSetGeneratorClassName()) {
                headers.set(HDR_GENERATOR_CLASS, format.getGeneratorClassName());
            }
            if (format.isSetIsOutputBinary()) {
                headers.set(HDR_IS_BINARY, Boolean.toString(format.isIsOutputBinary()));
            }
            if (format.isSetMimeType()) {
                headers.set(HDR_MIME_TYPE, format.getMimeType());
            }
            if (format.isSetVariant()) {
                headers.set(HDR_VARIANT, format.getVariant().name());
            }
        }

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (format != null && format.isSetMimeType() && !format.getMimeType().isBlank()) {
            mediaType = MediaType.parseMediaType(format.getMimeType());
        }

        byte[] body = file.isSetGeneratedOutput() ? file.getGeneratedOutput() : new byte[0];
        return ResponseEntity.ok().headers(headers).contentType(mediaType).body(body);
    }

    @GetMapping("/output-formats")
    public List<OutputFormatInfo> getPossibleOutputFormats() {
        return LicenseInfoRestMapper.fromThriftOutputFormatInfos(handler.getPossibleOutputFormats());
    }

    @GetMapping("/output-formats/{generatorClassName}")
    public OutputFormatInfo getOutputFormatInfoForGeneratorClass(@PathVariable String generatorClassName)
            throws TException {
        return LicenseInfoRestMapper.fromThriftOutputFormatInfo(
                handler.getOutputFormatInfoForGeneratorClass(generatorClassName));
    }

    @GetMapping("/defaults/license-info-header")
    public String getDefaultLicenseInfoHeaderText(
            @RequestParam(value = "fileName", required = false) String fileName) {
        return handler.getDefaultLicenseInfoHeaderText(fileName);
    }

    @GetMapping("/defaults/obligations-text")
    public String getDefaultObligationsText() {
        return handler.getDefaultObligationsText();
    }

    @PostMapping("/evaluate-attachments")
    public Map<String, Map<String, String>> evaluateAttachments(
            @RequestBody EvaluateAttachmentsRequest request,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        return handler.evaluateAttachments(request.getReleaseId(), user(email, department, userGroup));
    }
}

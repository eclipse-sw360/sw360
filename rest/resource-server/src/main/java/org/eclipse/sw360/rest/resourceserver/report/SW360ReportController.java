/*
SPDX-FileCopyrightText: © 2023-2024 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/

package org.eclipse.sw360.rest.resourceserver.report;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.eclipse.sw360.datahandler.common.SW360Constants.CONTENT_TYPE_OPENXML_SPREADSHEET;
import static org.eclipse.sw360.datahandler.common.SW360Constants.XML_FILE_EXTENSION;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.OutputFormatVariant;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.gson.JsonObject;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RestController
@SecurityRequirement(name = "tokenAuth")
@SecurityRequirement(name = "basic")
public class SW360ReportController implements RepresentationModelProcessor<RepositoryLinksResource> {
    public static final String REPORTS_URL = "/reports";
    private static final Logger log = LogManager.getLogger(SW360ReportController.class);
    private static final String LICENSE_INFO = "licenseInfo";
    private static final String LICENSES_RESOURCE_BUNDLE = "licenseResourceBundle";
    private static final String ZIP_CONTENT_TYPE = "application/zip";
    private static final String EXPORT_CREATE_PROJ_CLEARING_REPORT = "exportCreateProjectClearingReport";
    private static final List<String> GENERATOR_MODULES = List.of(LICENSE_INFO, EXPORT_CREATE_PROJ_CLEARING_REPORT);
    @NonNull
    private final RestControllerHelper restControllerHelper;
    @NonNull
    private final SW360ReportService sw360ReportService;
    private final ByteBuffer defaultByteBufferVal = ByteBuffer.wrap(new byte[0]);

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(SW360ReportController.class).slash("api/" + REPORTS_URL).withRel("reports"));
        return resource;
    }

    @Operation(
            summary = "Generate the reports.",
            description = "Generate the reports.",
            tags = {"Reports"}
    )
    @GetMapping(value = REPORTS_URL)
    public void getProjectReport(
            @Parameter(description = "Projects with linked releases.")
            @RequestParam(value = "withlinkedreleases", required = false, defaultValue = "false") boolean withLinkedReleases,
            @Parameter(description = "Project id.")
            @RequestParam(value = "projectId", required = false) String projectId,
            @Parameter(description = "Module name.", schema = @Schema(allowableValues = {
                    SW360Constants.PROJECTS, SW360Constants.COMPONENTS, SW360Constants.LICENSES,
                    LICENSE_INFO, LICENSES_RESOURCE_BUNDLE, SW360Constants.PROJECT_RELEASE_SPREADSHEET_WITH_ECCINFO,
                    EXPORT_CREATE_PROJ_CLEARING_REPORT,SW360Constants.SBOM
            }))
            @RequestParam(value = "module", required = true) String module,
            @Parameter(description = "Exclude release version from the license info file")
            @RequestParam(value = "excludeReleaseVersion", required = false, defaultValue = "false") boolean excludeReleaseVersion,
            @Parameter(description = "Output generator class. Required for modules [" + LICENSE_INFO + ", " + EXPORT_CREATE_PROJ_CLEARING_REPORT + "]",
                    schema = @Schema(type = "string", allowableValues = {"DocxGenerator", "XhtmlGenerator", "TextGenerator"}))
            @RequestParam(value = "generatorClassName", required = false) String generatorClassName,
            @Parameter(description = "Variant of the report. Required for modules [" + LICENSE_INFO + ", " + EXPORT_CREATE_PROJ_CLEARING_REPORT + "]",
                    schema = @Schema(implementation = OutputFormatVariant.class))
            @RequestParam(value = "variant", required = false) String variant,
            @Parameter(description = "Template for generating report. Can be supplied with modules [" + LICENSE_INFO + ", " + EXPORT_CREATE_PROJ_CLEARING_REPORT + "]")
            @RequestParam(value = "template", required = false, defaultValue = "") String template,
            @Parameter(description = "The external Ids of the project. Can be supplied with modules [" + LICENSE_INFO + ", " + EXPORT_CREATE_PROJ_CLEARING_REPORT + "]", example = "376577")
            @RequestParam(value = "externalIds", required = false, defaultValue = "") String externalIds,
            @Parameter(description = "Generate report for only current project or with Sub projects. Can be supplied with modules [" + LICENSE_INFO + ", " + EXPORT_CREATE_PROJ_CLEARING_REPORT + "]")
            @RequestParam(value = "withSubProject", required = false, defaultValue = "false") boolean withSubProject,
            @Parameter(description = "Type of SBOM file extention")
            @RequestParam(value = "bomType", required = false) String bomType,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws TException {
        if (GENERATOR_MODULES.contains(module) && (isNullOrEmpty(generatorClassName) || isNullOrEmpty(variant))) {
            throw new HttpMessageNotReadableException("Error : GeneratorClassName and Variant is required for module " + module);
        }
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        String baseUrl = getBaseUrl(request);
        try {
            switch (module) {
                case SW360Constants.PROJECTS:
                    getProjectReports(withLinkedReleases, response, sw360User, module, projectId,
                            excludeReleaseVersion, baseUrl, generatorClassName, variant, template, externalIds);
                    break;
                case SW360Constants.COMPONENTS:
                    getComponentsReports(withLinkedReleases, response, sw360User, module, excludeReleaseVersion,
                            baseUrl, generatorClassName, variant, template, externalIds);
                    break;
                case SW360Constants.LICENSES:
                    getLicensesReports(response, sw360User, module, excludeReleaseVersion, generatorClassName, variant,
                            template, externalIds);
                    break;
                case LICENSE_INFO:
                    getLicensesInfoReports(response, sw360User, module, projectId, excludeReleaseVersion,
                            generatorClassName, variant, template, externalIds, withSubProject);
                    break;
                case LICENSES_RESOURCE_BUNDLE:
                    getLicenseResourceBundleReports(projectId, response, sw360User, module, generatorClassName, variant,
                            template, externalIds, excludeReleaseVersion, withSubProject);
                    break;
                case SW360Constants.PROJECT_RELEASE_SPREADSHEET_WITH_ECCINFO:
                    getProjectReleaseWithEccSpreadSheet(response, sw360User, module, projectId, excludeReleaseVersion,
                            generatorClassName, variant, template, externalIds);
                    break;
                case EXPORT_CREATE_PROJ_CLEARING_REPORT:
                    exportProjectCreateClearingRequest(response, sw360User, module, projectId, excludeReleaseVersion,
                            generatorClassName, variant, template, externalIds);
                    break;
                case SW360Constants.SBOM:
                    exportSBOM(response, sw360User, module, projectId,generatorClassName,
                            bomType,withSubProject);
                    break;
                default:
                    break;
            }
        }
        catch (AccessDeniedException e) {
            throw  e;
        }
        catch (Exception e) {
            throw new TException(e.getMessage());
        }
    }

    private void getProjectReports(
            boolean withLinkedReleases, HttpServletResponse response, User sw360User, String module, String projectId,
            boolean excludeReleaseVersion, String baseUrl, String generatorClassName, String variant, String template,
            String externalIds
    ) throws TException {
        try {
            if (SW360Constants.MAIL_REQUEST_FOR_PROJECT_REPORT) {
                sw360ReportService.getUploadedProjectPath(sw360User, withLinkedReleases, baseUrl, projectId);
                JsonObject responseJson = new JsonObject();
                responseJson.addProperty("response", "The downloaded report link will be send to the end user.");
                response.getWriter().write(responseJson.toString());
            } else {
                downloadExcelReport(withLinkedReleases, response, sw360User, module, projectId, excludeReleaseVersion,
                        defaultByteBufferVal, generatorClassName, variant, template, externalIds);
            }
        } catch (Exception e) {
            log.error(e);
            throw new TException(e.getMessage());
        }
    }

    private void getComponentsReports(
            boolean withLinkedReleases, HttpServletResponse response, User sw360User, String module,
            boolean excludeReleaseVersion, String baseUrl, String generatorClassName, String variant, String template,
            String externalIds
    ) throws TException {
        try {
            if (SW360Constants.MAIL_REQUEST_FOR_COMPONENT_REPORT) {
                sw360ReportService.getUploadedComponentPath(sw360User, withLinkedReleases, baseUrl);
                JsonObject responseJson = new JsonObject();
                responseJson.addProperty("response", "Component report download link will get send to the end user.");
                response.getWriter().write(responseJson.toString());
            } else {
                downloadExcelReport(withLinkedReleases, response, sw360User, module, null, excludeReleaseVersion,
                        defaultByteBufferVal, generatorClassName, variant, template, externalIds);
            }
        } catch (Exception e) {
            log.error(e);
            throw new TException(e.getMessage());
        }
    }

    private void getLicensesReports(
            HttpServletResponse response, User sw360User, String module, boolean excludeReleaseVersion,
            String generatorClassName, String variant, String template, String externalIds
    ) throws TException {
        try {
            downloadExcelReport(false, response, sw360User, module, null, excludeReleaseVersion,
                    defaultByteBufferVal, generatorClassName, variant, template, externalIds);
        } catch (Exception e) {
            log.error(e);
            throw new TException(e.getMessage());
        }
    }

    private void getLicensesInfoReports(
            HttpServletResponse response, User sw360User, String module, String projectId,
            boolean excludeReleaseVersion, String generatorClassName, String variant, String template,
            String externalIds, boolean withSubProject
    ) throws TException {
        // TODO: use `withSubProject` while generating LicenseInfo report.
        try {
            downloadExcelReport(false, response, sw360User, module, projectId, excludeReleaseVersion,
                    defaultByteBufferVal, generatorClassName, variant, template, externalIds);
        } catch (Exception e) {
            log.error(e);
            throw new TException(e.getMessage());
        }
    }

    private void getProjectReleaseWithEccSpreadSheet(
            HttpServletResponse response, User sw360User, String module, String projectId,
            boolean excludeReleaseVersion, String generatorClassName, String variant, String template,
            String externalIds
    ) throws TException {
        try {
            downloadExcelReport(false, response, sw360User, module, projectId, excludeReleaseVersion,
                    defaultByteBufferVal, generatorClassName, variant, template, externalIds);
        } catch (Exception e) {
            throw new TException(e.getMessage());
        }
    }

    private void exportProjectCreateClearingRequest(
            HttpServletResponse response, User sw360User, String module, String projectId,
            boolean excludeReleaseVersion, String generatorClassName, String variant, String template,
            String externalIds
    ) throws TException {
        try {
            downloadExcelReport(false, response, sw360User, module, projectId, excludeReleaseVersion,
                    defaultByteBufferVal, generatorClassName, variant, template, externalIds);
        } catch (Exception e) {
            log.error(e);
            throw new TException(e.getMessage());
        }
    }

    private void downloadExcelReport(
            boolean withLinkedReleases, HttpServletResponse response, User user, String module, String projectId,
            boolean excludeReleaseVersion, ByteBuffer buffer, String generatorClassName, String variant,
            String template, String externalIds
    ) throws TException {
        try {
            ByteBuffer buff = null;
            String fileName = sw360ReportService.getDocumentName(user, null, module);
            response.setContentType(CONTENT_TYPE_OPENXML_SPREADSHEET);

            switch (module) {
                case SW360Constants.PROJECTS:
                    buff = sw360ReportService.getProjectBuffer(user, withLinkedReleases, projectId);
                    fileName = sw360ReportService.getDocumentName(user, projectId, module);
                    break;
                case SW360Constants.COMPONENTS:
                    buff = sw360ReportService.getComponentBuffer(user, withLinkedReleases);
                    break;
                case SW360Constants.LICENSES:
                    buff = sw360ReportService.getLicenseBuffer();
                    fileName = String.format("licenses-%s.xlsx", SW360Utils.getCreatedOn());
                    break;
                case LICENSES_RESOURCE_BUNDLE:
                    buff = buffer;
                    response.setContentType(ZIP_CONTENT_TYPE);
                    fileName = sw360ReportService.getSourceCodeBundleName(projectId, user);
                    break;
                case LICENSE_INFO:
                case EXPORT_CREATE_PROJ_CLEARING_REPORT:
                    buff = sw360ReportService.getLicenseInfoBuffer(user, projectId, generatorClassName, variant,
                            template, externalIds, excludeReleaseVersion);
                    fileName = sw360ReportService.getGenericLicInfoFileName(user, projectId, generatorClassName,
                            variant);
                    break;
                case SW360Constants.PROJECT_RELEASE_SPREADSHEET_WITH_ECCINFO:
                    buff = sw360ReportService.getProjectReleaseSpreadSheetWithEcc(user, projectId);
                    fileName = sw360ReportService.getDocumentName(user, projectId, module);
                    break;
                default:
                    break;
            }
            if (null == buff) {
                throw new TException("No data available for the user " + user.getEmail());
            }
            response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", fileName));
            copyDataStreamToResponse(response, buff);
        } catch (Exception e) {
            log.error(e);
            throw new TException(e.getMessage());
        }
    }

    private void getLicenseResourceBundleReports(
            String projectId, HttpServletResponse response, User sw360User, String module, String generatorClassName,
            String variant, String template, String externalIds, boolean excludeReleaseVersion, boolean withSubProject
    ) throws TException {
        try {
            ByteBuffer buffer = sw360ReportService.downloadSourceCodeBundle(projectId, sw360User, withSubProject);
            downloadExcelReport(false, response, sw360User, module, projectId, excludeReleaseVersion,
                    buffer, generatorClassName, variant, template, externalIds);
        } catch (Exception e) {
            throw new TException(e.getMessage());
        }
    }

    private void copyDataStreamToResponse(HttpServletResponse response, ByteBuffer buffer) throws IOException {
        FileCopyUtils.copy(buffer.array(), response.getOutputStream());
    }

    @Operation(
            summary = "Download reports.",
            description = "Download reports.",
            tags = {"Reports"},
            responses = {@ApiResponse(
                    responseCode = "200",
                    description = "Generated report.",
                    content = @Content(mediaType = CONTENT_TYPE_OPENXML_SPREADSHEET,
                            schema = @Schema(type = "string", format = "binary"))
            )}
    )
    @GetMapping(value = REPORTS_URL + "/download")
    public void downloadExcel(
            HttpServletRequest request,
            HttpServletResponse response,
            @Parameter(description = "Module name.", schema = @Schema(allowableValues = {SW360Constants.PROJECTS, SW360Constants.COMPONENTS, SW360Constants.LICENSES}))
            @RequestParam(value = "module", required = true) String module,
            @Parameter(description = "Token to download report.")
            @RequestParam(value = "token", required = true) String token,
            @Parameter(description = "Extended by releases.")
            @RequestParam(value = "extendedByReleases", required = false, defaultValue = "false") boolean extendedByReleases
    ) throws TException {
        final User user = restControllerHelper.getUserByEmail(request.getParameter("user"));
        try {
            ByteBuffer buffer = null;
            String fileName = sw360ReportService.getDocumentName(user, null, module);
            switch (module) {
                case SW360Constants.PROJECTS:
                    buffer = sw360ReportService.getReportStreamFromURl(user, extendedByReleases, token);
                    fileName = sw360ReportService.getDocumentName(user, request.getParameter("projectId"), module);
                    break;
                case SW360Constants.COMPONENTS:
                    buffer = sw360ReportService.getComponentReportStreamFromURl(user, extendedByReleases, token);
                    break;
                case SW360Constants.LICENSES:
                    buffer = sw360ReportService.getLicenseReportStreamFromURl(token);
                    fileName = String.format("licenses-%s.xlsx", SW360Utils.getCreatedOn());
                    break;
                default:
                    break;
            }
            if (null == buffer) {
                throw new TException("No data available for the user " + user.getEmail());
            }
            response.setContentType(CONTENT_TYPE_OPENXML_SPREADSHEET);
            response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", fileName));
            copyDataStreamToResponse(response, buffer);
        } catch (Exception e) {
            throw new TException(e.getMessage());
        }
    }

    private String getBaseUrl(HttpServletRequest request) {
        StringBuffer url = request.getRequestURL();
        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        return url.substring(0, url.length() - uri.length() + ctx.length()) + "/";
    }
    

    private void exportSBOM(
            HttpServletResponse response, User sw360User, String module, String projectId,
            String generatorClassName, String bomType, boolean withSubProject
    ) throws TException {
        try {
            String buff = sw360ReportService.getProjectSBOMBuffer(sw360User, projectId, bomType, withSubProject);
            response.setContentType(SW360Constants.CONTENT_TYPE_JSON);
            String fileName = sw360ReportService.getSBOMFileName(sw360User, projectId, module, bomType);
            if (null == buff) {
                throw new TException("No data available for the user " + sw360User.getEmail());
            }
            if (SW360Constants.XML_FILE_EXTENSION.equalsIgnoreCase(bomType)) {
                response.setContentType(SW360Constants.CONTENT_TYPE_XML);
            }
            response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", fileName));
            copyDataStreamToResponse(response, ByteBuffer.wrap(buff.getBytes()));
        }
        catch (AccessDeniedException e) {
            log.error(e);
            throw e;
        }
        catch (Exception e) {
            log.error(e);
            throw new TException(e.getMessage());
        }
    }
}

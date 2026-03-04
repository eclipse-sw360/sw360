/*
SPDX-FileCopyrightText: © 2023-2024 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/

package org.eclipse.sw360.rest.resourceserver.report;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.eclipse.sw360.datahandler.common.SW360ConfigKeys.MAIL_REQUEST_FOR_COMPONENT_REPORT;
import static org.eclipse.sw360.datahandler.common.SW360ConfigKeys.MAIL_REQUEST_FOR_PROJECT_REPORT;
import static org.eclipse.sw360.datahandler.common.SW360Constants.CONTENT_TYPE_OPENXML_SPREADSHEET;
import static org.eclipse.sw360.datahandler.common.SW360Constants.JSON_FILE_EXTENSION;
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
import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.OutputFormatVariant;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.BadRequestClientException;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.gson.JsonObject;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@BasePathAwareController
@RequiredArgsConstructor
@RestController
@SecurityRequirement(name = "tokenAuth")
@SecurityRequirement(name = "basic")
public class SW360ReportController implements RepresentationModelProcessor<RepositoryLinksResource> {
    public static final String REPORTS_URL = "/reports";
    public static final String CONTENT_DISPOSITION = "Content-Disposition";
    private static final Logger log = LogManager.getLogger(SW360ReportController.class);
    private static final String LICENSE_INFO = "licenseInfo";
    private static final String LICENSES_RESOURCE_BUNDLE = "licenseResourceBundle";
    private static final String ZIP_CONTENT_TYPE = "application/zip";
    private static final String EXPORT_CREATE_PROJ_CLEARING_REPORT = "exportCreateProjectClearingReport";
    private static final List<String> GENERATOR_MODULES = List.of(LICENSE_INFO, EXPORT_CREATE_PROJ_CLEARING_REPORT);
    public static final String ATTACHMENT_FILENAME_S = "attachment; filename=\"%s\"";
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
            description = """
                    Generate the reports.

                    Combination of `generatorClassName` and `variant` possible are:

                    When `variant` is `DISCLOSURE`, `generatorClassName` can be one of: \
                    `TextGenerator`, `XhtmlGenerator` or `DISCLOSURE`.
                    When `variant` is `REPORT`, `generatorClassName` can be one of: \
                    `DocxGenerator`.""",
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
            @Parameter(description = "The external Ids of the project. External Ids can be supplied multiple with comma separated. " +
                    "Can be supplied with modules [" + LICENSE_INFO + ", " + EXPORT_CREATE_PROJ_CLEARING_REPORT + "]", example = "376577,12345")
            @RequestParam(value = "externalIds", required = false, defaultValue = "") String externalIds,
            @Parameter(description = "Generate report for only current project or with Sub projects. Can be supplied with modules [" + LICENSE_INFO + ", " + EXPORT_CREATE_PROJ_CLEARING_REPORT + "]")
            @RequestParam(value = "withSubProject", required = false, defaultValue = "false") boolean withSubProject,
            @Parameter(description = "Type of SBOM file", schema = @Schema(allowableValues = {XML_FILE_EXTENSION, JSON_FILE_EXTENSION}))
            @RequestParam(value = "bomType", required = false) String bomType,
            @Parameter(description = "Selected release relationships. Can be supplied with modules [" + LICENSE_INFO + "]", example = "CONTAINED,UNKNOWN")
            @RequestParam(value = "selectedRelRelationship", required = false) List<ReleaseRelationship> selectedRelRelationship,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);
        if (GENERATOR_MODULES.contains(module) && (isNullOrEmpty(generatorClassName) || isNullOrEmpty(variant))) {
            throw new BadRequestClientException("Error : GeneratorClassName and Variant is required for module " + module);
        }
        SW360ReportBean reportBean = createReportBeanObject(withLinkedReleases, excludeReleaseVersion, generatorClassName, variant,
                template, externalIds, withSubProject, bomType, selectedRelRelationship);
        String baseUrl = getBaseUrl(request);
        switch (module) {
            case SW360Constants.PROJECTS:
                getProjectReports(response, sw360User, module, projectId, baseUrl, reportBean);
                break;
            case SW360Constants.COMPONENTS:
                getComponentsReports(response, sw360User, module, baseUrl, reportBean);
                break;
            case SW360Constants.LICENSES:
                getLicensesReports(response, sw360User, module, reportBean);
                break;
            case LICENSE_INFO:
                getLicensesInfoReports(response, sw360User, module, projectId, reportBean);
                break;
            case LICENSES_RESOURCE_BUNDLE:
                getLicenseResourceBundleReports(projectId, response, sw360User, module, reportBean);
                break;
            case SW360Constants.PROJECT_RELEASE_SPREADSHEET_WITH_ECCINFO:
                getProjectReleaseWithEccSpreadSheet(response, sw360User, module, projectId, reportBean);
                break;
            case EXPORT_CREATE_PROJ_CLEARING_REPORT:
                exportProjectCreateClearingRequest(response, sw360User, module, projectId, reportBean);
                break;
            case SW360Constants.SBOM:
                exportSBOM(response, sw360User, module, projectId, reportBean);
                break;
            default:
                break;
        }
    }

    /**
     * Creates a SW360ReportBean object with the given parameters.
     *
     * @param withLinkedReleases      whether to include linked releases
     * @param excludeReleaseVersion   whether to exclude release version
     * @param generatorClassName      the generator class name
     * @param variant                 the variant of the report
     * @param template                the template for generating the report
     * @param externalIds             the external IDs of the project
     * @param withSubProject          whether to include sub-projects
     * @param bomType                 the type of SBOM file
     * @param selectedRelRelationship selected release relationships
     * @return a SW360ReportBean object with the specified parameters
     */
    private SW360ReportBean createReportBeanObject(boolean withLinkedReleases, boolean excludeReleaseVersion, String generatorClassName,
                                                   String variant, String template, String externalIds, boolean withSubProject, String bomType,
                                                   List<ReleaseRelationship> selectedRelRelationship) {
        SW360ReportBean reportBean = new SW360ReportBean();
        reportBean.setWithLinkedReleases(withLinkedReleases);
        reportBean.setExcludeReleaseVersion(excludeReleaseVersion);
        reportBean.setGeneratorClassName(generatorClassName);
        reportBean.setVariant(variant);
        reportBean.setTemplate(template);
        reportBean.setExternalIds(externalIds);
        reportBean.setWithSubProject(withSubProject);
        reportBean.setBomType(bomType);
        reportBean.setSelectedRelRelationship(selectedRelRelationship);
        return reportBean;
    }

    private void getProjectReports(
            HttpServletResponse response, User sw360User, String module, String projectId,
            String baseUrl, SW360ReportBean reportBean
    ) throws SW360Exception {
        try {
            if (SW360Utils.readConfig(MAIL_REQUEST_FOR_PROJECT_REPORT, false)) {
                sw360ReportService.getUploadedProjectPath(sw360User, reportBean.isWithLinkedReleases(), baseUrl, projectId);
                JsonObject responseJson = new JsonObject();
                responseJson.addProperty("response", "The downloaded report link will be send to the end user.");
                response.getWriter().write(responseJson.toString());
            } else {
                downloadExcelReport(response, sw360User, module, projectId, defaultByteBufferVal, reportBean);
            }
        } catch (Exception e) {
            log.error(e);
            throw new SW360Exception(e.getMessage());
        }
    }

    private void getComponentsReports(
            HttpServletResponse response, User sw360User, String module, String baseUrl, SW360ReportBean reportBean
    ) throws SW360Exception {
        try {
            if (SW360Utils.readConfig(MAIL_REQUEST_FOR_COMPONENT_REPORT, false)) {
                sw360ReportService.getUploadedComponentPath(sw360User, reportBean.isWithLinkedReleases(), baseUrl);
                JsonObject responseJson = new JsonObject();
                responseJson.addProperty("response", "Component report download link will get send to the end user.");
                response.getWriter().write(responseJson.toString());
            } else {
                downloadExcelReport(response, sw360User, module, null, defaultByteBufferVal, reportBean);
            }
        } catch (Exception e) {
            log.error(e);
            throw new SW360Exception(e.getMessage());
        }
    }

    private void getLicensesReports(
            HttpServletResponse response, User sw360User, String module, SW360ReportBean reportBean
    ) throws SW360Exception {
        try {
            downloadExcelReport(response, sw360User, module, null, defaultByteBufferVal, reportBean);
        } catch (Exception e) {
            log.error(e);
            throw new SW360Exception(e.getMessage());
        }
    }

    private void getLicensesInfoReports(
            HttpServletResponse response, User sw360User, String module, String projectId, SW360ReportBean reportBean
    ) throws SW360Exception {
        // TODO: use `withSubProject` while generating LicenseInfo report.
        try {
            downloadExcelReport(response, sw360User, module, projectId, defaultByteBufferVal, reportBean);
        } catch (Exception e) {
            log.error(e);
            throw new SW360Exception(e.getMessage());
        }
    }

    private void getProjectReleaseWithEccSpreadSheet(
            HttpServletResponse response, User sw360User, String module, String projectId, SW360ReportBean reportBean
    ) throws SW360Exception {
        try {
            downloadExcelReport(response, sw360User, module, projectId, defaultByteBufferVal, reportBean);
        } catch (Exception e) {
            throw new SW360Exception(e.getMessage());
        }
    }

    private void exportProjectCreateClearingRequest(
            HttpServletResponse response, User sw360User, String module, String projectId, SW360ReportBean reportBean
    ) throws SW360Exception {
        try {
            downloadExcelReport(response, sw360User, module, projectId, defaultByteBufferVal, reportBean);
        } catch (Exception e) {
            log.error(e);
            throw new SW360Exception(e.getMessage());
        }
    }

    private void downloadExcelReport(
            HttpServletResponse response, User user, String module, String projectId,
            ByteBuffer buffer, SW360ReportBean reportBean
    ) throws SW360Exception {
        try {
            ByteBuffer buff = null;
            String fileName = sw360ReportService.getDocumentName(user, null, module);
            response.setContentType(CONTENT_TYPE_OPENXML_SPREADSHEET);

            switch (module) {
                case SW360Constants.PROJECTS:
                    buff = sw360ReportService.getProjectBuffer(user, reportBean.isWithLinkedReleases(), projectId);
                    fileName = sw360ReportService.getDocumentName(user, projectId, module);
                    break;
                case SW360Constants.COMPONENTS:
                    buff = sw360ReportService.getComponentBuffer(user, reportBean.isWithLinkedReleases());
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
                    buff = sw360ReportService.getLicenseInfoBuffer(user, projectId, reportBean);
                    fileName = sw360ReportService.getGenericLicInfoFileName(user, projectId, reportBean.getGeneratorClassName(),
                            reportBean.getVariant());
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
            response.setHeader(CONTENT_DISPOSITION, String.format(ATTACHMENT_FILENAME_S, fileName));
            copyDataStreamToResponse(response, buff);
        } catch (Exception e) {
            log.error(e);
            throw new SW360Exception(e.getMessage());
        }
    }

    private void getLicenseResourceBundleReports(
            String projectId, HttpServletResponse response, User sw360User, String module, SW360ReportBean reportBean
    ) throws SW360Exception {
        try {
            ByteBuffer buffer = sw360ReportService.downloadSourceCodeBundle(projectId, sw360User, reportBean.isWithSubProject());
            downloadExcelReport(response, sw360User, module, projectId,
                    buffer, reportBean);
        } catch (Exception e) {
            throw new SW360Exception(e.getMessage());
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
    ) throws SW360Exception {
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
            response.setHeader(CONTENT_DISPOSITION, String.format(ATTACHMENT_FILENAME_S, fileName));
            copyDataStreamToResponse(response, buffer);
        } catch (Exception e) {
            throw new SW360Exception(e.getMessage());
        }
    }

    private String getBaseUrl(HttpServletRequest request) {
        return restControllerHelper.getBaseUrl(request) + "/";
    }

    private void exportSBOM(
            HttpServletResponse response, User sw360User, String module, String projectId, SW360ReportBean reportBean
    ) throws TException {
        try {
            String buff = sw360ReportService.getProjectSBOMBuffer(sw360User, projectId, reportBean.getBomType(),
                    reportBean.isWithSubProject());
            response.setContentType(SW360Constants.CONTENT_TYPE_JSON);
            String fileName = sw360ReportService.getSBOMFileName(sw360User, projectId, module, reportBean.getBomType());
            if (null == buff) {
                throw new SW360Exception("No data available for the user " + sw360User.getEmail());
            }
            if (SW360Constants.XML_FILE_EXTENSION.equalsIgnoreCase(reportBean.getBomType())) {
                response.setContentType(SW360Constants.CONTENT_TYPE_XML);
            }
            response.setHeader(CONTENT_DISPOSITION, String.format(ATTACHMENT_FILENAME_S, fileName));
            copyDataStreamToResponse(response, ByteBuffer.wrap(buff.getBytes()));
        }
        catch (AccessDeniedException e) {
            log.error(e);
            throw e;
        } catch (IOException e) {
            log.error(e);
            throw new SW360Exception("Unable to generate SBOM report.");
        }
    }
}

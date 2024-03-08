/*
SPDX-FileCopyrightText: © 2023 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/

package org.eclipse.sw360.rest.resourceserver.report;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.server.RepresentationModelProcessor;
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
public class SW360ReportController implements RepresentationModelProcessor<RepositoryLinksResource> {
    private static final String COMPONENTS = "components";

    private static final String PROJECTS = "projects";

    private static final String LICENSES = "licenses";

    public static final String REPORTS_URL = "/reports";

    private static final String CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @NonNull
    private final RestControllerHelper restControllerHelper;

    @NonNull
    private final SW360ReportService sw360ReportService;

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(SW360ReportController.class).slash("api/" + REPORTS_URL).withRel("reports"));
        return resource;
    }

    private final List<String> mimeTypeList = Arrays.asList("xls", "xlsx");

    @Operation(
            summary = "Generate the reports.",
            description = "Generate the reports.",
            tags = {"Reports"}
    )
    @GetMapping(value = REPORTS_URL)
    public void getProjectReport(
            @Parameter(description = "Projects with linked releases.")
            @RequestParam(value = "withlinkedreleases", required = false, defaultValue = "false") boolean withLinkedReleases,
            @Parameter(description = "Report download format.", schema = @Schema(allowableValues = {"xls", "xlsx"}))
            @RequestParam(value = "mimetype", required = false, defaultValue = "xlsx") String mimeType,
            @Parameter(description = "Downloading project report required mail link.")
            @RequestParam(value = "mailrequest", required = false, defaultValue = "false") boolean mailRequest,
            @Parameter(description = "Project id.")
            @RequestParam(value = "projectId", required = false) String projectId,
            @Parameter(description = "Module name.", schema = @Schema(allowableValues = {PROJECTS, COMPONENTS}))
            @RequestParam(value = "module", required = true) String module,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws TException {

        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        try {
            if (validateMimeType(mimeType)) {
                switch (module) {
                    case PROJECTS:
                        getProjectReports(withLinkedReleases, mailRequest, response, request, sw360User, module, projectId);
                        break;
                    case COMPONENTS:
                        getComponentsReports(withLinkedReleases, mailRequest, response, request, sw360User, module);
                        break;
                    case LICENSES:
                        getLicensesReports(response, sw360User, module);
                        break;
                    default:
                        break;
                }
            } else {
                throw new TException("Error : Mimetype either should be : xls/xlsx");
            }
        } catch (Exception e) {
            throw new TException(e.getMessage());
        }
    }

    private void getProjectReports(boolean withLinkedReleases, boolean mailRequest, HttpServletResponse response,
            HttpServletRequest request, User sw360User, String module, String projectId) throws TException {
        try {
            if (mailRequest) {
                sw360ReportService.getUploadedProjectPath(sw360User, withLinkedReleases,getBaseUrl(request), projectId);
                JsonObject responseJson = new JsonObject();
                responseJson.addProperty("response", "Project report download link will get send to the end user.");
                response.getWriter().write(responseJson.toString());
            } else {
                downloadExcelReport(withLinkedReleases, response, sw360User, module, projectId);
            }
        } catch (Exception e) {
            throw new TException(e.getMessage());
        }
    }

    private void getComponentsReports(boolean withLinkedReleases, boolean mailRequest, HttpServletResponse response,
            HttpServletRequest request, User sw360User, String module) throws TException {
        try {
            if (mailRequest) {
                sw360ReportService.getUploadedComponentPath(sw360User, withLinkedReleases, getBaseUrl(request));
                JsonObject responseJson = new JsonObject();
                responseJson.addProperty("response", "Component report download link will get send to the end user.");
                response.getWriter().write(responseJson.toString());
            } else {
                downloadExcelReport(withLinkedReleases, response, sw360User, module, null);
            }
        } catch (Exception e) {
            throw new TException(e.getMessage());
        }
    }

    private void getLicensesReports(HttpServletResponse response, User sw360User, String module) throws TException {
        try {
            downloadExcelReport(false, response, sw360User, module, null);
        } catch (Exception e) {
            throw new TException(e.getMessage());
        }
    }

    private void downloadExcelReport(boolean withLinkedReleases, HttpServletResponse response, User user, String module, String projectId)
            throws TException {
        try {
            ByteBuffer buffer = null;
            switch (module) {
                case PROJECTS:
                    buffer = sw360ReportService.getProjectBuffer(user, withLinkedReleases, projectId);
                    break;
                case COMPONENTS:
                    buffer = sw360ReportService.getComponentBuffer(user, withLinkedReleases);
                    break;
                case LICENSES:
                    buffer = sw360ReportService.getLicenseBuffer();
                    break;
                default:
                    break;
            }
            if (null == buffer) {
                throw new TException("No data available for the user " + user.getEmail());
            }
            response.setContentType(CONTENT_TYPE);
            String fileName;
            if(module.equals(LICENSES)) {
                fileName = String.format("licenses-%s.xlsx", SW360Utils.getCreatedOn());
            } else if(module.equals(PROJECTS)) {
                fileName = sw360ReportService.getDocumentName(user, projectId);
            }else {
                fileName = sw360ReportService.getDocumentName(user, null);
            }
            response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", fileName));
            copyDataStreamToResponse(response, buffer);
        } catch (Exception e) {
            throw new TException(e.getMessage());
        }
    }

    private void copyDataStreamToResponse(HttpServletResponse response, ByteBuffer buffer) throws IOException {
        FileCopyUtils.copy(buffer.array(), response.getOutputStream());
    }

    private boolean validateMimeType(String mimeType) {
        return mimeTypeList.contains(mimeType);
    }

    @Operation(
            summary = "Download reports.",
            description = "Download reports.",
            tags = {"Reports"},
            responses = {@ApiResponse(
                    responseCode = "200",
                    description = "Generated report.",
                    content = @Content(mediaType = CONTENT_TYPE,
                            schema = @Schema(type = "string", format = "binary"))
            )}
    )
    @GetMapping(value = REPORTS_URL + "/download")
    public void downloadExcel(
            HttpServletResponse response,
            @Parameter(description = "Module name.", schema = @Schema(allowableValues = {PROJECTS, COMPONENTS}))
            @RequestParam(value = "module", required = true) String module,
            @Parameter(description = "Token to download report.")
            @RequestParam(value = "token", required = true) String token,
            @Parameter(description = "Extended by releases.")
            @RequestParam(value = "extendedByReleases", required = false, defaultValue = "false") boolean extendedByReleases
    ) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        User user = restControllerHelper.getUserByEmail(sw360User.getEmail());
        String fileConstant = module+"-%s.xlsx";
        try {
            ByteBuffer buffer = null;
            switch (module) {
                case PROJECTS:
                    buffer = sw360ReportService.getReportStreamFromURl(user, extendedByReleases, token);
                    break;
                case COMPONENTS:
                    buffer = sw360ReportService.getComponentReportStreamFromURl(user, extendedByReleases, token);
                    break;
                case LICENSES:
                    buffer = sw360ReportService.getLicenseReportStreamFromURl(token);
                    break;
                default:
                    break;
            }
            if (null == buffer) {
                throw new TException("No data available for the user " + user.getEmail());
            }
            String filename = String.format(fileConstant, SW360Utils.getCreatedOn());
            response.setContentType(CONTENT_TYPE);
            response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", filename));
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
}

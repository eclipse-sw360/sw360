/*
SPDX-FileCopyrightText: © 2023 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/

package org.eclipse.sw360.rest.resourceserver.report;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
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

@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SW360ReportController implements RepresentationModelProcessor<RepositoryLinksResource> {
    private static final String COMPONENTS = "components";

    private static final String PROJECTS = "projects";

    public static final String REPORTS_URL = "/reports";

    private static String CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @NonNull
    private final RestControllerHelper restControllerHelper;

    @NonNull
    private final SW360ReportService sw360ReportService;

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(SW360ReportController.class).slash("api/" + REPORTS_URL).withRel("reports"));
        return resource;
    }

    private List<String> mimeTypeList = Arrays.asList("xls", "xlsx");

    @GetMapping(value = REPORTS_URL)
    public void getProjectReport(
            @RequestParam(value = "withlinkedreleases", required = false, defaultValue = "false") boolean withLinkedReleases,
            @RequestParam(value = "mimetype", required = false, defaultValue = "xlsx") String mimeType,
            @RequestParam(value = "mailrequest", required = false, defaultValue = "false") boolean mailRequest,
            @RequestParam(value = "module", required = true) String module, HttpServletRequest request,
            HttpServletResponse response) throws TException {

        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        try {
            if (validateMimeType(mimeType)) {
                switch (module) {
                case PROJECTS:
                    getProjectReports(withLinkedReleases, mailRequest, response, request, sw360User, module);
                    break;

                case COMPONENTS:
                    getComponentsReports(withLinkedReleases, mailRequest, response, request, sw360User, module);
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
            HttpServletRequest request, User sw360User, String module) throws TException {
        try {
            if (mailRequest) {
                sw360ReportService.getUploadedProjectPath(sw360User, withLinkedReleases,getBaseUrl(request));
                JsonObject responseJson = new JsonObject();
                responseJson.addProperty("response", "Project report download link will get send to the end user.");
                response.getWriter().write(responseJson.toString());
            } else {
                downloadExcelReport(withLinkedReleases, response, sw360User, module);
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
                downloadExcelReport(withLinkedReleases, response, sw360User, module);
            }
        } catch (Exception e) {
            throw new TException(e.getMessage());
        }
    }

    private void downloadExcelReport(boolean withLinkedReleases, HttpServletResponse response, User user, String module)
            throws TException, IOException {
        try {
            ByteBuffer buffer = null;
            switch (module) {
            case PROJECTS:
                buffer = sw360ReportService.getProjectBuffer(user, withLinkedReleases);
                break;
            case COMPONENTS:
                buffer = sw360ReportService.getComponentBuffer(user, withLinkedReleases);
                break;
            default:
                break;
            }
            if (null == buffer) {
                throw new TException("No data available for the user " + user.getEmail());
            }
            response.setContentType(CONTENT_TYPE);
            String filename = String.format("projects-%s.xlsx", SW360Utils.getCreatedOn());
            response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", filename));
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

    @GetMapping(value = REPORTS_URL + "/download")
    public void downloadExcel(HttpServletRequest request, HttpServletResponse response) throws TException {
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        String module = request.getParameter("module");
        String token = request.getParameter("token");
        String extendedByReleases = request.getParameter("extendedByReleases");
        User user = restControllerHelper.getUserByEmail(sw360User.getEmail());
        String fileConstant = module+"-%s.xlsx";
        try {
            ByteBuffer buffer = null;
            switch (module) {
            case PROJECTS:
                buffer = sw360ReportService.getReportStreamFromURl(user, Boolean.valueOf(extendedByReleases), token);
                break;
            case COMPONENTS:
                buffer = sw360ReportService.getComponentReportStreamFromURl(user, Boolean.valueOf(extendedByReleases),
                        token);
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

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.gson.JsonObject;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SW360ReportController implements RepresentationModelProcessor<RepositoryLinksResource>{

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
    
    private List<String> mimeTypeList = Arrays.asList("xls","xlsx");
    
    @RequestMapping(value = REPORTS_URL + "/myprojectreports", method = RequestMethod.GET)
    public void getProjectReport(@RequestParam(value = "withlinkedreleases", required = false, defaultValue = "false") boolean withLinkedReleases,
            @RequestParam(value = "mimetype", required = false, defaultValue = "xlsx") String mimeType,
            @RequestParam(value = "mailrequest", required = false, defaultValue="false") boolean mailRequest, HttpServletRequest request,
            HttpServletResponse response) throws TException{

        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        try {
            if(validateMimeType(mimeType)) {
                if(mailRequest) {
                    StringBuffer url = request.getRequestURL();
                    String uri = request.getRequestURI();
                    String ctx = request.getContextPath();
                    String base = url.substring(0, url.length() - uri.length() + ctx.length()) + "/";

                    String projectPath = sw360ReportService.getUploadedProjectPath(sw360User, withLinkedReleases);

                    String backendURL = base + "api/reports/download?user="+sw360User.getEmail()+"&extendedByReleases="+withLinkedReleases+"&token=";
                    URL emailURL = new URL(backendURL+projectPath);
                    
                    if(!CommonUtils.isNullEmptyOrWhitespace(projectPath)) {
                    	sw360ReportService.sendExportSpreadsheetSuccessMail(emailURL.toString(), sw360User.getEmail());
                    }
                    JsonObject responseJson = new JsonObject();
                    responseJson.addProperty("response", "E-mail sent succesfully to the end user.");
                    responseJson.addProperty("url", emailURL.toString());
                    responseJson.toString();
                    response.getWriter().write(responseJson.toString());
                }else {
                	downloadExcelReport(withLinkedReleases, response, sw360User);
                }
            }else {
                throw new TException("Error : Mimetype either should be : xls/xlsx");
            }
        }catch (Exception e) {
            throw new TException(e.getMessage());
        }
    }

    private void downloadExcelReport(boolean withLinkedReleases, HttpServletResponse response,
            User user) throws TException, IOException {
    	try {
    		ByteBuffer buffer = sw360ReportService.getProjectBuffer(user,withLinkedReleases);
    		if(null==buffer) {
    			throw new TException("No data available for the user "+ user.getEmail());
    		}
            response.setContentType(CONTENT_TYPE);
            String filename = String.format("projects-%s.xlsx", SW360Utils.getCreatedOn());
            response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", filename));
            copyDataStreamToResponse(response, buffer);
    	}catch(Exception e) {
    		throw new TException(e.getMessage());
    	}
    }

    private void copyDataStreamToResponse(HttpServletResponse response, ByteBuffer buffer) throws IOException {
        FileCopyUtils.copy(buffer.array(), response.getOutputStream());
    }

    private boolean validateMimeType(String mimeType) {
        return mimeTypeList.contains(mimeType);
    }
    
    @RequestMapping(value = REPORTS_URL + "/download", method = RequestMethod.GET)
    public void downloadExcel(HttpServletRequest request,HttpServletResponse response) throws TException{
    	final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        String token = request.getParameter("token");
        String extendedByReleases = request.getParameter("extendedByReleases");
        User user=restControllerHelper.getUserByEmail(sw360User.getEmail());
        try {
            ByteBuffer buffer =  sw360ReportService.getReportStreamFromURl(user,Boolean.valueOf(extendedByReleases), token);
            String filename = String.format("projects-%s.xlsx", SW360Utils.getCreatedOn());
            response.setContentType(CONTENT_TYPE);
            response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", filename));
            copyDataStreamToResponse(response, buffer);
        } catch (Exception e) {
            throw new TException(e.getMessage());
        }
    }
    
}

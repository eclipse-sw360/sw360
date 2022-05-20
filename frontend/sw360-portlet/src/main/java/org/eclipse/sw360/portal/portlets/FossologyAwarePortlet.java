/*
 * Copyright Siemens AG, 2013-2017, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.portlets;

import com.google.common.base.Predicate;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.ResourceBundleUtil;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.FossologyUtils;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.attachments.CheckStatus;
import org.eclipse.sw360.datahandler.thrift.components.*;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService.Iface;
import org.eclipse.sw360.datahandler.thrift.fossology.FossologyService;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoParsingResult;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoRequestStatus;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoService;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseNameWithText;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.users.UserCacheHolder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;

import javax.portlet.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.liferay.portal.kernel.json.JSONFactoryUtil.createJSONArray;
import static com.liferay.portal.kernel.json.JSONFactoryUtil.createJSONObject;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyString;
import static org.eclipse.sw360.datahandler.common.SW360Utils.printName;
import static org.eclipse.sw360.portal.common.PortalConstants.PAGENAME;
import static org.eclipse.sw360.portal.common.PortalConstants.RELEASE_ID;

/**
 * Fossology aware portlet implementation
 *
 * @author Daniele.Fognini@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 * @author alex.borodin@evosoft.com
 */
public abstract class FossologyAwarePortlet extends LinkedReleasesAndProjectsAwarePortlet {

    private static final Logger log = LogManager.getLogger(FossologyAwarePortlet.class);

    public FossologyAwarePortlet() {

    }

    public FossologyAwarePortlet(ThriftClients clients) {
        super(clients);
    }

    @Override
    protected void dealWithGenericAction(ResourceRequest request, ResourceResponse response, String action)
            throws IOException, PortletException {

        if (super.isGenericAction(action)) {
            super.dealWithGenericAction(request, response, action);
        } else if (isFossologyAwareAction(action)) {
            dealWithFossologyAction(request, response, action);
        }
    }

    @Override
    protected boolean isGenericAction(String action) {
        return super.isGenericAction(action) || isFossologyAwareAction(action);
    }

    protected boolean isFossologyAwareAction(String action) {
        return action.startsWith(PortalConstants.FOSSOLOGY_PREFIX);
    }

    protected abstract void dealWithFossologyAction(ResourceRequest request, ResourceResponse response, String action) throws IOException, PortletException;


    protected void serveLicenseToSourceFileMapping(ResourceRequest request, ResourceResponse response) throws IOException, PortletException {
        final User user = UserCacheHolder.getUserFromRequest(request);
        final String releaseId = request.getParameter(PortalConstants.RELEASE_ID);
        final ComponentService.Iface componentClient = thriftClients.makeComponentClient();
        final LicenseInfoService.Iface licenseClient = thriftClients.makeLicenseInfoClient();
        final JSONObject jsonResult = createJSONObject();
        final ResourceBundle resourceBundle = ResourceBundleUtil.getBundle("content.Language", request.getLocale(), getClass());
        final Predicate<Attachment> isCLI = attachment -> AttachmentType.COMPONENT_LICENSE_INFO_XML.equals(attachment.getAttachmentType())
                || AttachmentType.COMPONENT_LICENSE_INFO_COMBINED.equals(attachment.getAttachmentType());

        Set<LicenseNameWithText> licenseNameWithTexts = new HashSet<LicenseNameWithText>();
        Release release = null;
        try {
            release = componentClient.getReleaseById(releaseId, user);
            List<Attachment> filteredAttachments = CommonUtils.nullToEmptySet(release.getAttachments()).stream().filter(isCLI).collect(Collectors.toList());
            if (filteredAttachments.size() > 1) {
                Predicate<Attachment> isApprovedCLI = attachment -> CheckStatus.ACCEPTED.equals(attachment.getCheckStatus());
                filteredAttachments = filteredAttachments.stream().filter(isApprovedCLI).collect(Collectors.toList());
            }
            if (filteredAttachments.size() == 1 && filteredAttachments.get(0).getFilename().endsWith(PortalConstants.XML_FILE_EXTENSION)) {
                final Attachment filteredAttachment = filteredAttachments.get(0);
                final String attachmentContentId = filteredAttachment.getAttachmentContentId();

                try {
                    List<LicenseInfoParsingResult> licenseResults = licenseClient.getLicenseInfoForAttachment(release, attachmentContentId, false, user);
                    if (CommonUtils.isNotEmpty(licenseResults) && LicenseInfoRequestStatus.SUCCESS.equals(licenseResults.get(0).getStatus())) {
                        licenseNameWithTexts = licenseResults.get(0).getLicenseInfo().getLicenseNamesWithTexts();
                        if (CommonUtils.isNotEmpty(licenseNameWithTexts)) {
                            JSONArray licenseToSourceData = createJSONArray();
                            for (LicenseNameWithText license : licenseNameWithTexts) {
                                JSONObject jsonObject = JSONFactoryUtil.createJSONObject();
                                jsonObject.put("licName", nullToEmptyString(license.getLicenseName()));
                                jsonObject.put("licSpdxId", nullToEmptyString(license.getLicenseSpdxId()));
                                jsonObject.put("srcFiles",  nullToEmptyString(String.join(",", license.getSourceFiles())));
                                jsonObject.put("licType", SW360Constants.LICENSE_TYPE_GLOBAL.equalsIgnoreCase(license.getType()) ? SW360Constants.LICENSE_TYPE_GLOBAL : SW360Constants.LICENSE_TYPE_OTHERS);
                                licenseToSourceData.put(jsonObject);
                            }
                            jsonResult.put(SW360Constants.STATUS, SW360Constants.SUCCESS);
                            jsonResult.put("data", licenseToSourceData);
                            jsonResult.put("relId", releaseId);
                            jsonResult.put("relName", nullToEmptyString(printName(release)));
                            jsonResult.put("attName", nullToEmptyString(filteredAttachment.getFilename()));
                        } else {
                            jsonResult.put(SW360Constants.STATUS, SW360Constants.FAILURE);
                            jsonResult.put(SW360Constants.MESSAGE, LanguageUtil.get(resourceBundle, "source.file.information.not.found.in.cli"));
                        }
                    } else {
                        jsonResult.put(SW360Constants.STATUS, SW360Constants.FAILURE);
                        jsonResult.put(SW360Constants.MESSAGE, licenseResults.get(0).getMessage());
                    }
                } catch (TException exception) {
                    log.error(String.format("Error fetchinig license Information for attachment: %s in release: %s",
                            filteredAttachment.getFilename(), releaseId), exception);
                }
            } else {
                jsonResult.put(SW360Constants.STATUS, SW360Constants.FAILURE);
                if (filteredAttachments.size() > 1) {
                    jsonResult.put(SW360Constants.MESSAGE, LanguageUtil.get(resourceBundle, "multiple.approved.cli.are.found.in.the.release"));
                } else if (filteredAttachments.isEmpty()) {
                    jsonResult.put(SW360Constants.MESSAGE, LanguageUtil.get(resourceBundle, "cli.attachment.not.found.in.the.release"));
                } else {
                    jsonResult.put(SW360Constants.MESSAGE, LanguageUtil.get(resourceBundle, "source.file.information.not.found.in.cli"));
                }
            }
        } catch (TException e) {
            log.error(String.format("error fetching release from db: %s ", releaseId), e);
        }
        jsonResult.put("releaseId", releaseId);
        jsonResult.put("releaseName", nullToEmptyString(printName(release)));
        try {
            writeJSON(request, response, jsonResult);
        } catch (IOException e) {
            log.error("Error rendering license to source file mapping", e);
        }
    }

    protected void serveFossologyOutdated(ResourceRequest request, ResourceResponse response) {
        FossologyService.Iface fossologyClient = thriftClients.makeFossologyClient();

        String releaseId = request.getParameter(RELEASE_ID);
        RequestStatus result;
        String errorMessage;
        try {
            result = fossologyClient.markFossologyProcessOutdated(releaseId,
                    UserCacheHolder.getUserFromRequest(request));
            errorMessage = "No exception thrown from backend, but setting the FOSSology process of release with id "
                    + releaseId + " to outdated did result in state FAILURE.";
        } catch (TException e) {
            result = RequestStatus.FAILURE;
            errorMessage = "Could not mark FOSSology process of release with id " + releaseId
                    + " as outdated because of: " + e;
        }

        serveRequestStatus(request, response, result, errorMessage, log);
    }

    protected void serveFossologyStatus(ResourceRequest request, ResourceResponse response) {
        Iface componentClient = thriftClients.makeComponentClient();

        String releaseId = request.getParameter(RELEASE_ID);
        JSONObject jsonObject = JSONFactoryUtil.createJSONObject();
        try {
            Release release = componentClient.getReleaseById(releaseId, UserCacheHolder.getUserFromRequest(request));
            Set<ExternalToolProcess> fossologyProcesses = SW360Utils.getNotOutdatedExternalToolProcessesForTool(release,
                    ExternalTool.FOSSOLOGY);
            fillJsonObjectFromFossologyProcess(jsonObject, fossologyProcesses);

            Set<Attachment> sourceAttachments = componentClient.getSourceAttachments(releaseId);
            jsonObject.put("sourceAttachments", sourceAttachments.size());
            if (sourceAttachments.size() == 1) {
                jsonObject.put("sourceAttachmentName", sourceAttachments.iterator().next().getFilename());
            }
        } catch (TException e) {
            jsonObject.put("error", "Could not determine FOSSology state for this release!");
        }

        try {
            writeJSON(request, response, jsonObject);
        } catch (IOException e) {
            log.error("Problem rendering RequestStatus", e);
        }
    }

    protected void serveFossologyProcess(ResourceRequest request, ResourceResponse response) {
        FossologyService.Iface fossologyClient = thriftClients.makeFossologyClient();

        String releaseId = request.getParameter(RELEASE_ID);
        JSONObject jsonObject = JSONFactoryUtil.createJSONObject();
        try {
            ExternalToolProcess fossologyProcess = fossologyClient.process(releaseId,
                    UserCacheHolder.getUserFromRequest(request), "");
            fillJsonObjectFromFossologyProcess(jsonObject, Stream.of(fossologyProcess).collect(Collectors.toSet()));
        } catch (TException e) {
            jsonObject.put("error", "Could not determine FOSSology state for this release!");
        }

        try {
            writeJSON(request, response, jsonObject);
        } catch (IOException e) {
            log.error("Problem rendering RequestStatus", e);
        }
    }

    private void fillJsonObjectFromFossologyProcess(JSONObject jsonObject,
            Set<ExternalToolProcess> fossologyProcesses) {
        if (fossologyProcesses.size() == 0) {
            jsonObject.put("stepName", FossologyUtils.FOSSOLOGY_STEP_NAME_UPLOAD);
            jsonObject.put("stepStatus", ExternalToolProcessStatus.NEW.toString());
        } else if (fossologyProcesses.size() == 1) {
            ExternalToolProcess fossologyProcess = fossologyProcesses.iterator().next();
            FossologyUtils.ensureOrderOfProcessSteps(fossologyProcess);
            ExternalToolProcessStep furthestStep = fossologyProcess.getProcessSteps()
                    .get(fossologyProcess.getProcessStepsSize() - 1);
            jsonObject.put("stepName", furthestStep.getStepName());
            jsonObject.put("stepStatus", furthestStep.getStepStatus().toString());
        } else {
            jsonObject.put("error", "More than one FOSSology process found for this release!");
        }
    }

    private void fillJsonObjectFromFossologyProcessReloadReport(JSONObject jsonObject,
            Set<ExternalToolProcess> fossologyProcesses) {
        if (fossologyProcesses.size() == 1) {
            jsonObject.put("stepName", FossologyUtils.FOSSOLOGY_STEP_NAME_REPORT);
            jsonObject.put("stepStatus", ExternalToolProcessStatus.NEW.toString());
        } else {
            jsonObject.put("error", "The source file is either not yet uploaded or scanning is not done.");
        }
    }

    // USED but not fossology related

    protected void putReleasesAndProjectIntoRequest(PortletRequest request, String projectId, User user)
            throws TException {
        ProjectService.Iface client = thriftClients.makeProjectClient();
        List<ReleaseClearingStatusData> releaseClearingStatuses = client.getReleaseClearingStatusesWithAccessibility(projectId, user);
        request.setAttribute(PortalConstants.RELEASES_AND_PROJECTS, releaseClearingStatuses);
    }

    protected void serveFossologyReloadReport(ResourceRequest request, ResourceResponse response) {
        Iface componentClient = thriftClients.makeComponentClient();
        FossologyService.Iface fossologyClient = thriftClients.makeFossologyClient();
        String releaseId = request.getParameter(RELEASE_ID);
        JSONObject jsonObject = JSONFactoryUtil.createJSONObject();
        String errorMsg = "Could not determine FOSSology state for this release!";
        try {
            Release release = componentClient.getReleaseById(releaseId, UserCacheHolder.getUserFromRequest(request));
            RequestStatus result = fossologyClient.triggerReportGenerationFossology(releaseId,
                    UserCacheHolder.getUserFromRequest(request));
            if (result == RequestStatus.FAILURE) {
                jsonObject.put("error", errorMsg);
            } else {
                Set<ExternalToolProcess> fossologyProcesses = SW360Utils
                        .getNotOutdatedExternalToolProcessesForTool(release, ExternalTool.FOSSOLOGY);
                fillJsonObjectFromFossologyProcessReloadReport(jsonObject, fossologyProcesses);
            }
        } catch (TException e) {
            jsonObject.put("error", errorMsg);
            log.error("Error pulling report from fossology", e);
        }

        try {
            writeJSON(request, response, jsonObject);
        } catch (IOException e) {
            log.error("Problem rendering RequestStatus", e);
        }
    }

    public static void addCustomErrorMessage(String errorMessage, String pageName, ActionRequest request,
            ActionResponse response) {
        SessionErrors.add(request, "custom_error");
        request.setAttribute("cyclicError", errorMessage);
        SessionMessages.add(request,
                PortalUtil.getPortletId(request) + SessionMessages.KEY_SUFFIX_HIDE_DEFAULT_ERROR_MESSAGE);
        SessionMessages.add(request,
                PortalUtil.getPortletId(request) + SessionMessages.KEY_SUFFIX_HIDE_DEFAULT_SUCCESS_MESSAGE);
        response.setRenderParameter(PAGENAME, pageName);
    }
}

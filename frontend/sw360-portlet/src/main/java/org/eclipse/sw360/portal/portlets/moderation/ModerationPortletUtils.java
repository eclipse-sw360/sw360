/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.portlets.moderation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.ClearingRequestState;
import org.eclipse.sw360.datahandler.thrift.ClearingRequestType;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestStatus;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.thrift.ClearingRequestPriority;
import org.eclipse.sw360.datahandler.thrift.Comment;
import org.eclipse.sw360.datahandler.thrift.ModerationState;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationService;
import org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.portal.common.CustomFieldHelper;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.users.UserCacheHolder;

import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.portlet.LiferayPortletURL;
import com.liferay.portal.kernel.portlet.PortletURLFactoryUtil;
import com.liferay.portal.kernel.service.LayoutLocalServiceUtil;

import static java.lang.Integer.parseInt;
import static org.eclipse.sw360.portal.common.PortalConstants.CUSTOM_FIELD_PREFERRED_CLEARING_DATE_LIMIT;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

import javax.portlet.PortletRequest;

/**
 * @author: alex.borodin@evosoft.com
 */
public class ModerationPortletUtils {
    private static final Logger log = LogManager.getLogger(ModerationPortletUtils.class);

    private ModerationPortletUtils() {
        // Utility class with only static functions
    }

    public static Integer loadPreferredClearingDateLimit(PortletRequest request, User user) {
        Integer limit = CustomFieldHelper.loadField(Integer.class, request, user, CUSTOM_FIELD_PREFERRED_CLEARING_DATE_LIMIT).orElse(0);
        // returning default value 7 (days) if variable is not set
        return limit < 1 ? 7 : limit;
    }

    public static RequestStatus deleteModerationRequest(PortletRequest request, Logger log) {
        String id = request.getParameter(PortalConstants.MODERATION_ID);
        if (id != null) {
            try {
                ModerationService.Iface client = new ThriftClients().makeModerationClient();
                return client.deleteModerationRequest(id, UserCacheHolder.getUserFromRequest(request));
            } catch (TException e) {
                log.error("Could not delete moderation request from DB", e);
            }
        }
        return RequestStatus.FAILURE;
    }

    public static RequestStatus addCommentToClearingRequest(PortletRequest request, Logger log) {
        String id = request.getParameter(PortalConstants.CLEARING_REQUEST_ID);
        String commentText = request.getParameter(PortalConstants.CLEARING_REQUEST_COMMENT);
        if (CommonUtils.isNullEmptyOrWhitespace(commentText)) {
            log.warn("Invalid comment, (empty or whitespace)");
            return RequestStatus.FAILURE;
        }
        User user = UserCacheHolder.getUserFromRequest(request);
        Comment comment = new Comment(commentText, user.getEmail());
        try {
            ModerationService.Iface client = new ThriftClients().makeModerationClient();
            return client.addCommentToClearingRequest(id, comment, user);
        } catch (TException e) {
            log.error("failed to add comment in clearing reuest: " + id, e);
        }
        return RequestStatus.FAILURE;
    }

    public static AddDocumentRequestSummary updateClearingRequest(PortletRequest request, Logger log) {
        String id = request.getParameter(PortalConstants.CLEARING_REQUEST_ID);
        User user = UserCacheHolder.getUserFromRequest(request);
        String isReOpen = request.getParameter(PortalConstants.RE_OPEN_REQUEST);
        AddDocumentRequestSummary requestSummary = new AddDocumentRequestSummary().setRequestStatus(AddDocumentRequestStatus.FAILURE);
        if (null != id) {
            if (CommonUtils.isNotNullEmptyOrWhitespace(isReOpen) && Boolean.parseBoolean(isReOpen)) {
                return reOpenClearingRequest(id, request, user);
            }
            try {
                String isClearingExpertEdit = request.getParameter(PortalConstants.IS_CLEARING_EXPERT);
                ModerationService.Iface client = new ThriftClients().makeModerationClient();
                ClearingRequest clearingRequest = client.getClearingRequestByIdForEdit(id, user);

                if (PermissionUtils.isUserAtLeast(UserGroup.SW360_ADMIN, user)) {
                    String requestingUser = request.getParameter(ClearingRequest._Fields.REQUESTING_USER.toString());
                    if (CommonUtils.isNullEmptyOrWhitespace(requestingUser)) {
                        log.warn("Invalid requesting user email: " + requestingUser + " is entered, by user: "+ user.getEmail());
                        return requestSummary.setMessage("Invalid requesting user email");
                    }
                    clearingRequest.setRequestingUser(requestingUser);
                }

                String clearingTeam = request.getParameter(ClearingRequest._Fields.CLEARING_TEAM.toString());
                if (CommonUtils.isNullEmptyOrWhitespace(clearingTeam)) {
                    log.warn("Invalid clearingTeam email: " + clearingTeam + " is entered, by user: "+ user.getEmail());
                    return requestSummary.setMessage("Invalid clearingTeam email");
                }
                clearingRequest.setClearingTeam(clearingTeam);
                String preferredClearingDate = request.getParameter(ClearingRequest._Fields.REQUESTED_CLEARING_DATE.toString());
                if (CommonUtils.isNotNullEmptyOrWhitespace(preferredClearingDate) && !preferredClearingDate.equals(clearingRequest.getRequestedClearingDate())
                        && SW360Utils.isValidDate(preferredClearingDate, DateTimeFormatter.ISO_LOCAL_DATE, null)) {
                    clearingRequest.setRequestedClearingDate(preferredClearingDate);
                }
                if (CommonUtils.isNotNullEmptyOrWhitespace(isClearingExpertEdit) && Boolean.parseBoolean(isClearingExpertEdit)) {
                    String agreedDate = request.getParameter(ClearingRequest._Fields.AGREED_CLEARING_DATE.toString());
                    String status = request.getParameter(ClearingRequest._Fields.CLEARING_STATE.toString());
                    String priority = request.getParameter(ClearingRequest._Fields.PRIORITY.toString());
                    String clearingType = request.getParameter(ClearingRequest._Fields.CLEARING_TYPE.toString());
                    if (CommonUtils.isNotNullEmptyOrWhitespace(agreedDate) && !agreedDate.equals(clearingRequest.getAgreedClearingDate())
                            && !SW360Utils.isValidDate(agreedDate, DateTimeFormatter.ISO_LOCAL_DATE, null)) {
                        log.warn("Invalid agreed clearing date: " + agreedDate + " is entered, by user: "+ user.getEmail());
                        return requestSummary.setMessage("Invalid agreed clearing date");
                    }
                    clearingRequest.setAgreedClearingDate(CommonUtils.nullToEmptyString(agreedDate));
                    clearingRequest.setClearingState(ClearingRequestState.findByValue(parseInt(status)));
                    clearingRequest.setPriority(ClearingRequestPriority.findByValue(parseInt(priority)));
                    clearingRequest.setClearingType(ClearingRequestType.findByValue(parseInt(clearingType)));
                }
                LiferayPortletURL projectUrl = getProjectPortletUrl(request, clearingRequest.getProjectId());
                RequestStatus status = client.updateClearingRequest(clearingRequest, user, CommonUtils.nullToEmptyString(projectUrl));
                if (RequestStatus.SUCCESS.equals(status)) {
                    return new AddDocumentRequestSummary().setRequestStatus(AddDocumentRequestStatus.SUCCESS).setId(id);
                } else {
                    return requestSummary.setMessage("Failed to update clearing request");
                }
            } catch (TException e) {
                log.error("Failed to update clearing request", e);
            }
        }
        log.error("Invalid clearing request Id.");
        return requestSummary.setMessage("Invalid clearing request id");
    }

    private static AddDocumentRequestSummary reOpenClearingRequest(String id, PortletRequest request, User user) {
        AddDocumentRequestSummary requestSummary = new AddDocumentRequestSummary().setRequestStatus(AddDocumentRequestStatus.FAILURE);
        try {
            ModerationService.Iface client = new ThriftClients().makeModerationClient();
            Integer criticalCount = client.getOpenCriticalCrCountByGroup(user.getDepartment());
            String preferredDate = request.getParameter(ClearingRequest._Fields.REQUESTED_CLEARING_DATE.toString());
            String commentText = request.getParameter(ClearingRequest._Fields.REQUESTING_USER_COMMENT.toString());
            String priority = (criticalCount > 1) ? "" : request.getParameter(ClearingRequest._Fields.PRIORITY.toString());
            Integer dateLimit = ModerationPortletUtils.loadPreferredClearingDateLimit(request, user);
            dateLimit = (CommonUtils.isNotNullEmptyOrWhitespace(priority) && criticalCount < 2) ? 0 : (dateLimit < 1) ? 7 : dateLimit;
            if (!SW360Utils.isValidDate(preferredDate, DateTimeFormatter.ISO_LOCAL_DATE, Long.valueOf(dateLimit))) {
                log.warn("Invalid requested clearing date: " + preferredDate + " is entered, by user: "+ user.getEmail());
                return requestSummary.setMessage("Invalid requested clearing date");
            }
            ClearingRequest clearingRequest = client.getClearingRequestByIdForEdit(id, user);
            if (CommonUtils.isNotNullEmptyOrWhitespace(commentText)) {
                commentText = "Reopen comment:\n" + commentText;
                Comment comment = new Comment(commentText, user.getEmail());
                comment.setAutoGenerated(true);
                comment.setCommentedOn(System.currentTimeMillis());
                clearingRequest.addToComments(comment);
            }
            clearingRequest.setRequestedClearingDate(preferredDate);
            clearingRequest.unsetAgreedClearingDate();
            clearingRequest.setClearingState(ClearingRequestState.NEW);
            clearingRequest.unsetTimestampOfDecision();
            if (CommonUtils.isNotNullEmptyOrWhitespace(priority)) {
                clearingRequest.setPriority(ClearingRequestPriority.CRITICAL);
            } else {
                clearingRequest.setPriority(ClearingRequestPriority.LOW);
            }
            clearingRequest.addToReOpenOn(System.currentTimeMillis());
            LiferayPortletURL projectUrl = getProjectPortletUrl(request, clearingRequest.getProjectId());
            RequestStatus status = client.updateClearingRequest(clearingRequest, user, CommonUtils.nullToEmptyString(projectUrl));
            if (RequestStatus.SUCCESS.equals(status)) {
                return new AddDocumentRequestSummary().setRequestStatus(AddDocumentRequestStatus.SUCCESS).setId(id);
            } else {
                return requestSummary.setMessage("Failed to reopen clearing request");
            }
        } catch (TException e) {
            log.error("Failed to re-open clearing request", e);
        }
        return requestSummary.setMessage("Failed to reopen clearing request");
    }

    private static LiferayPortletURL getProjectPortletUrl(PortletRequest request, String projectId) {
        Optional<Layout> layout = LayoutLocalServiceUtil.getLayouts(QueryUtil.ALL_POS, QueryUtil.ALL_POS).stream()
            .filter(l -> ("/projects").equals(l.getFriendlyURL())).findFirst();
        if (layout.isPresent()) {
            long plId = layout.get().getPlid();
            LiferayPortletURL projectUrl = PortletURLFactoryUtil.create(request, PortalConstants.PROJECT_PORTLET_NAME, plId, PortletRequest.RENDER_PHASE);
            projectUrl.setParameter(PortalConstants.PROJECT_ID, projectId);
            projectUrl.setParameter(PortalConstants.PAGENAME, PortalConstants.PAGENAME_DETAIL);
            return projectUrl;
        }
        return null;
    }

    public static boolean isOpenModerationRequest(ModerationRequest mr) {
        return mr.getModerationState() == ModerationState.PENDING || mr.getModerationState() == ModerationState.INPROGRESS;
    }

    public static boolean isClosedClearingRequest(ClearingRequest cr) {
        return cr.getClearingState() == ClearingRequestState.CLOSED || cr.getClearingState() == ClearingRequestState.REJECTED;
    }
}

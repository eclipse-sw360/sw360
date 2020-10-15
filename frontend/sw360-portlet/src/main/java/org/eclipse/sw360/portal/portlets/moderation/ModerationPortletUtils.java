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
import org.eclipse.sw360.datahandler.thrift.ClearingRequestState;
import org.eclipse.sw360.datahandler.thrift.Comment;
import org.eclipse.sw360.datahandler.thrift.ModerationState;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationService;
import org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.users.UserCacheHolder;

import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.model.Portlet;
import com.liferay.portal.kernel.portlet.LiferayPortletURL;
import com.liferay.portal.kernel.portlet.PortletURLFactoryUtil;
import com.liferay.portal.kernel.service.LayoutLocalServiceUtil;
import com.liferay.portal.kernel.service.PortletLocalServiceUtil;

import static java.lang.Integer.parseInt;

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

    public static RequestStatus updateClearingRequest(PortletRequest request, Logger log) {
        String id = request.getParameter(PortalConstants.CLEARING_REQUEST_ID);
        User user = UserCacheHolder.getUserFromRequest(request);
        String isReOpen = request.getParameter(PortalConstants.RE_OPEN_REQUEST);
        if (CommonUtils.isNotNullEmptyOrWhitespace(isReOpen) && Boolean.parseBoolean(isReOpen)) {
            return reOpenClearingRequest(id, request, user);
        }
        String agreedDate = request.getParameter(ClearingRequest._Fields.AGREED_CLEARING_DATE.toString());
        String status = request.getParameter(ClearingRequest._Fields.CLEARING_STATE.toString());
        if (null != id) {
            try {
                ModerationService.Iface client = new ThriftClients().makeModerationClient();
                ClearingRequest clearingRequest = client.getClearingRequestByIdForEdit(id, user);
                if (CommonUtils.isNotNullEmptyOrWhitespace(agreedDate) && !SW360Utils.isValidDate(agreedDate, DateTimeFormatter.ISO_LOCAL_DATE, 0)) {
                    log.warn("Invalid agreed clearing date: " + agreedDate + " is entered, by user: "+ user.getEmail());
                    return RequestStatus.FAILURE;
                }
                clearingRequest.setAgreedClearingDate(CommonUtils.nullToEmptyString(agreedDate));
                clearingRequest.setClearingState(ClearingRequestState.findByValue(parseInt(status)));
                LiferayPortletURL projectUrl = getProjectPortletUrl(request, clearingRequest.getProjectId());
                return client.updateClearingRequest(clearingRequest, user, CommonUtils.nullToEmptyString(projectUrl));
            } catch (TException e) {
                log.error("Failed to update clearing request", e);
            }
        }
        log.error("Invalid clearing request Id.");
        return RequestStatus.FAILURE;
    }

    private static RequestStatus reOpenClearingRequest(String id, PortletRequest request, User user) {
        try {
            String preferredDate = request.getParameter(ClearingRequest._Fields.REQUESTED_CLEARING_DATE.toString());
            if (!SW360Utils.isValidDate(preferredDate, DateTimeFormatter.ISO_LOCAL_DATE, 7)) {
                log.warn("Invalid requested clearing date: " + preferredDate + " is entered, by user: "+ user.getEmail());
                return RequestStatus.FAILURE;
            }
            ModerationService.Iface client = new ThriftClients().makeModerationClient();
            ClearingRequest clearingRequest = client.getClearingRequestByIdForEdit(id, user);
            clearingRequest.setRequestedClearingDate(preferredDate);
            clearingRequest.unsetAgreedClearingDate();
            clearingRequest.setClearingState(ClearingRequestState.NEW);
            clearingRequest.unsetTimestampOfDecision();
            clearingRequest.addToReOpenOn(System.currentTimeMillis());
            LiferayPortletURL projectUrl = getProjectPortletUrl(request, clearingRequest.getProjectId());
            return client.updateClearingRequest(clearingRequest, user, CommonUtils.nullToEmptyString(projectUrl));
        } catch (TException e) {
            log.error("Failed to re-open clearing request", e);
        }
        return RequestStatus.FAILURE;
    }

    private static LiferayPortletURL getProjectPortletUrl(PortletRequest request, String projectId) {
        Portlet portlet = PortletLocalServiceUtil.getPortletById(PortalConstants.PROJECT_PORTLET_NAME);
        Optional<Layout> layout = LayoutLocalServiceUtil.getLayouts(portlet.getCompanyId()).stream()
                .filter(l -> ("/" + PortalConstants.PROJECTS.toLowerCase()).equals(l.getFriendlyURL())).findFirst();
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

/*
 * Copyright Siemens AG, 2023-2024.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.schedule;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class Sw360ScheduleService {
    private static final Logger log = LogManager.getLogger(Sw360ScheduleService.class);

    private RequestSummary scheduleService(User sw360User, String serviceName) throws TException {
        throwIfNotAdmin(sw360User);
        ThriftClients thriftClients = new ThriftClients();
        return thriftClients.makeScheduleClient().scheduleService(serviceName);
    }

    private static void throwIfNotAdmin(User sw360User) throws AccessDeniedException {
        if (!PermissionUtils.isAdmin(sw360User)) {
            throw new AccessDeniedException("User does not have admin access");
        }
    }

    private RequestStatus unscheduleService(User sw360User, String serviceName) throws TException {
        throwIfNotAdmin(sw360User);
        ThriftClients thriftClients = new ThriftClients();
        return thriftClients.makeScheduleClient().unscheduleService(serviceName, sw360User);
    }

    public RequestStatus cancelAllServices(User sw360User) throws TException {
        throwIfNotAdmin(sw360User);
        ThriftClients thriftClients = new ThriftClients();
        return thriftClients.makeScheduleClient().unscheduleAllServices(sw360User);
    }

    public RequestSummary scheduleCveSearch(User sw360User) throws TException {
        return scheduleService(sw360User, ThriftClients.CVESEARCH_SERVICE);
    }

    public RequestStatus cancelCveSearch(User sw360User) throws TException {
        return unscheduleService(sw360User, ThriftClients.CVESEARCH_SERVICE);
    }

    public RequestSummary deleteAttachmentService(User sw360User) throws TException {
        return scheduleService(sw360User, ThriftClients.DELETE_ATTACHMENT_SERVICE);
    }

    public RequestStatus cancelDeleteAttachment(User sw360User) throws TException {
        return unscheduleService(sw360User, ThriftClients.DELETE_ATTACHMENT_SERVICE);
    }

    public RequestStatus cancelAttachmentDeletionLocalFS(User sw360User) throws TException {
        return new ThriftClients().makeAttachmentClient().deleteOldAttachmentFromFileSystem();
    }

    public RequestStatus triggerCveSearch(User sw360User) throws TException {
        throwIfNotAdmin(sw360User);
        try {
            ThriftClients thriftClients = new ThriftClients();
            return thriftClients.makeCvesearchClient().update();
        } catch (TException e) {
            log.error("Error occurred while triggering CVE search: " + e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error occurred while triggering CVE search: " + e.getMessage(), e);
            throw new TException("Unexpected error", e);
        }
    }

    public RequestSummary svmSync(User sw360User) throws TException {
        throwIfNotAdmin(sw360User);
        String serviceName = ThriftClients.SVMSYNC_SERVICE;
        return new ThriftClients().makeScheduleClient().scheduleService(serviceName);
    }

    public RequestStatus cancelSvmSync(User sw360User) throws TException {
        throwIfNotAdmin(sw360User);
        String serviceName = ThriftClients.SVMSYNC_SERVICE;
        return new ThriftClients().makeScheduleClient().unscheduleService(serviceName, sw360User);
    }

    public RequestSummary scheduleSvmReverseMatch(User sw360User) throws TException {
        throwIfNotAdmin(sw360User);
        String serviceName = ThriftClients.SVMMATCH_SERVICE;
        return new ThriftClients().makeScheduleClient().scheduleService(serviceName);
    }

    public RequestStatus cancelSvmReverseMatch(User sw360User) throws TException {
        throwIfNotAdmin(sw360User);
        String serviceName = ThriftClients.SVMMATCH_SERVICE;
        return new ThriftClients().makeScheduleClient().unscheduleService(serviceName, sw360User);
    }

    public RequestSummary svmReleaseTrackingFeedback(User sw360User) throws TException {
        throwIfNotAdmin(sw360User);
        String serviceName = ThriftClients.SVM_TRACKING_FEEDBACK_SERVICE;
        return new ThriftClients().makeScheduleClient().scheduleService(serviceName);
    }

    public RequestSummary svmMonitoringListUpdate(User sw360User) throws TException {
        throwIfNotAdmin(sw360User);
        String serviceName = ThriftClients.SVM_LIST_UPDATE_SERVICE;
        return new ThriftClients().makeScheduleClient().scheduleService(serviceName);
    }

    public RequestStatus cancelSvmMonitoringListUpdate(User sw360User) throws TException {
        throwIfNotAdmin(sw360User);
        String serviceName = ThriftClients.SVM_LIST_UPDATE_SERVICE;
        return new ThriftClients().makeScheduleClient().unscheduleService(serviceName, sw360User);
    }

    public RequestSummary triggerSrcUpload(User sw360User) throws TException {
        throwIfNotAdmin(sw360User);
        String serviceName = ThriftClients.SRC_UPLOAD_SERVICE;
        return new ThriftClients().makeScheduleClient().scheduleService(serviceName);
    }

    public RequestStatus unscheduleSrcUpload(User sw360User) throws TException {
        throwIfNotAdmin(sw360User);
        String serviceName = ThriftClients.SRC_UPLOAD_SERVICE;
        return new ThriftClients().makeScheduleClient().unscheduleService(serviceName, sw360User);
    }

    public RequestStatus triggerSourceUploadForReleaseComponents(User sw360User) throws TException {
        throwIfNotAdmin(sw360User);
        return new ThriftClients().makeComponentClient()
                .uploadSourceCodeAttachmentToReleases();
    }

    public RequestStatus isServiceScheduled(String serviceName, User sw360User) throws TException {
        throwIfNotAdmin(sw360User);
        try {
            boolean requestStatusWithBoolean = new ThriftClients()
                    .makeScheduleClient()
                    .isServiceScheduled(serviceName, sw360User)
                    .isAnswerPositive();

            return requestStatusWithBoolean ? RequestStatus.SUCCESS : RequestStatus.FAILURE;
        } catch (TException e) {
            log.error("Error occurred while fetching the status of service '{}':", serviceName, e);
            throw e;
        }
    }

    public RequestStatus isAnyServiceScheduled(User sw360User) throws TException {
        throwIfNotAdmin(sw360User);
        try {
            boolean requestStatusWithBoolean = new ThriftClients()
                    .makeScheduleClient()
                    .isAnyServiceScheduled(sw360User)
                    .isAnswerPositive();

            return requestStatusWithBoolean ? RequestStatus.SUCCESS : RequestStatus.FAILURE;
        } catch (TException e) {
            log.error("Error occurred while fetching the status of services", e);
            throw e;
        }
    }
}

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
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class Sw360ScheduleService {
    private static final Logger log = LogManager.getLogger(Sw360ScheduleService.class);

    public RequestStatus cancelAllServices(User sw360User) throws TException {
        try {
            if (PermissionUtils.isUserAtLeast(UserGroup.ADMIN, sw360User)) {
                RequestStatus requestStatus = new ThriftClients().makeScheduleClient().unscheduleAllServices(sw360User);
                return requestStatus;
            } else {
                throw new AccessDeniedException("User is not admin");
            }
        } catch (TException e) {
            log.error("Error occurred while scheduling service: " + e);
            throw e;
        }
    }

    public RequestSummary svmSync(User sw360User) throws TException {
        String serviceName = ThriftClients.SVMSYNC_SERVICE;
        try {
            if (PermissionUtils.isUserAtLeast(UserGroup.ADMIN, sw360User)) {
                RequestSummary requestSummary = new ThriftClients().makeScheduleClient().scheduleService(serviceName);
                return requestSummary;
            } else {
                throw new AccessDeniedException("User is not admin");
            }
        } catch (TException e) {
            log.error("Error occurred while scheduling service: " + serviceName, e);
            throw e;
        }
    }

    public RequestStatus cancelSvmSync(User sw360User) throws TException {
        String serviceName = ThriftClients.SVMSYNC_SERVICE;
        try {
            if (PermissionUtils.isUserAtLeast(UserGroup.ADMIN, sw360User)) {
                RequestStatus requestStatus = new ThriftClients().makeScheduleClient().unscheduleService(serviceName, sw360User);
                return requestStatus;
            } else {
                throw new AccessDeniedException("User is not admin");
            }
        } catch (TException e) {
            log.error("Error occurred while scheduling service: " + serviceName, e);
            throw e;
        }
    }

    public RequestSummary scheduleSvmReverseMatch(User sw360User) throws TException {
        String serviceName = ThriftClients.SVMMATCH_SERVICE;
        try {
            if (PermissionUtils.isUserAtLeast(UserGroup.ADMIN, sw360User)) {
                RequestSummary requestSummary = new ThriftClients().makeScheduleClient().scheduleService(serviceName);
                return requestSummary;
            } else {
                throw new AccessDeniedException("User is not admin");
            }
        } catch (TException e) {
            log.error("Error occurred while scheduling service: " + serviceName, e);
            throw e;
        }
    }

    public RequestStatus cancelSvmReverseMatch(User sw360User) throws TException {
        String serviceName = ThriftClients.SVMMATCH_SERVICE;
        try {
            if (PermissionUtils.isUserAtLeast(UserGroup.ADMIN, sw360User)) {
                RequestStatus requestStatus = new ThriftClients().makeScheduleClient().unscheduleService(serviceName, sw360User);
                return requestStatus;
            } else {
                throw new AccessDeniedException("User is not admin");
            }
        } catch (TException e) {
            log.error("Error occurred while scheduling service: " + serviceName, e);
            throw e;
        }
    }

    public RequestSummary svmReleaseTrackingFeedback(User sw360User) throws TException {
        String serviceName = ThriftClients.SVM_TRACKING_FEEDBACK_SERVICE;
        try {
            if (PermissionUtils.isUserAtLeast(UserGroup.ADMIN, sw360User)) {
                RequestSummary requestSummary = new ThriftClients().makeScheduleClient().scheduleService(serviceName);;
                return requestSummary;
            } else {
                throw new AccessDeniedException("User is not admin");
            }
        } catch (TException e) {
            log.error("Error occurred while scheduling service: " + serviceName, e);
            throw e;
        }
    }

    public RequestSummary svmMonitoringListUpdate(User sw360User) throws TException {
        String serviceName = ThriftClients.SVM_LIST_UPDATE_SERVICE;
        try {
            if (PermissionUtils.isUserAtLeast(UserGroup.ADMIN, sw360User)) {
                RequestSummary requestSummary = new ThriftClients().makeScheduleClient().scheduleService(serviceName);
                return requestSummary;
            } else {
                throw new AccessDeniedException("User is not admin");
            }
        } catch (TException e) {
            log.error("Error occurred while scheduling service: " + serviceName, e);
            throw e;
        }
    }

    public RequestStatus cancelSvmMonitoringListUpdate(User sw360User) throws TException {
        String serviceName = ThriftClients.SVM_LIST_UPDATE_SERVICE;
        try {
            if (PermissionUtils.isUserAtLeast(UserGroup.ADMIN, sw360User)) {
                RequestStatus requestStatus = new ThriftClients().makeScheduleClient().unscheduleService(serviceName, sw360User);
                return requestStatus;
            } else {
                throw new AccessDeniedException("User is not admin");
            }
        } catch (TException e) {
            log.error("Error occurred while scheduling service: " + serviceName, e);
            throw e;
        }
    }

    public RequestSummary triggeSrcUpload(User sw360User) throws TException {
        String serviceName = ThriftClients.SRC_UPLOAD_SERVICE;
        try {
            if (PermissionUtils.isUserAtLeast(UserGroup.ADMIN, sw360User)) {
                RequestSummary requestSummary = new ThriftClients().makeScheduleClient().scheduleService(serviceName);
                return requestSummary;
            } else {
                throw new AccessDeniedException("User is not admin");
            }
        } catch (TException e) {
            log.error("Error occurred while scheduling service: " + serviceName, e);
            throw e;
        }
    }

    public RequestStatus unscheduleSrcUpload(User sw360User) throws TException {
        String serviceName = ThriftClients.SRC_UPLOAD_SERVICE;
        try {
            if (PermissionUtils.isUserAtLeast(UserGroup.ADMIN, sw360User)) {
                RequestStatus requestStatus = new ThriftClients().makeScheduleClient().unscheduleService(serviceName, sw360User);
                return requestStatus;
            } else {
                throw new AccessDeniedException("User is not admin");
            }
        } catch (TException e) {
            log.error("Error occurred while scheduling service: " + serviceName, e);
            throw e;
        }
    }


}

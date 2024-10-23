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
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class Sw360ScheduleService {
    private static final Logger log = LogManager.getLogger(Sw360ScheduleService.class);

    private RequestSummary scheduleService(User sw360User, String serviceName) throws TException {
        try {
            if (PermissionUtils.isUserAtLeast(UserGroup.ADMIN, sw360User)) {
                ThriftClients thriftClients = new ThriftClients();
                return thriftClients.makeScheduleClient().scheduleService(serviceName);
            } else {
                throw new AccessDeniedException("User does not have admin access");
            }
        } catch (SW360Exception sw360Exp) {
            if (sw360Exp.getErrorCode() == 403) {
                throw new AccessDeniedException("User does not have admin access", sw360Exp);
            } else {
                throw sw360Exp;
            }
        }
    }

    private RequestStatus unscheduleService(User sw360User, String serviceName) throws TException {
        try {
            if (PermissionUtils.isUserAtLeast(UserGroup.ADMIN, sw360User)) {
                ThriftClients thriftClients = new ThriftClients();
                return thriftClients.makeScheduleClient().unscheduleService(serviceName, sw360User);
            } else {
                throw new AccessDeniedException("User does not have admin access");
            }
        } catch (SW360Exception sw360Exp) {
            if (sw360Exp.getErrorCode() == 403) {
                throw new AccessDeniedException("User does not have admin access", sw360Exp);
            } else {
                throw sw360Exp;
            }
        }
    }

    public RequestStatus cancelAllServices(User sw360User) throws TException {
        try {
            if (PermissionUtils.isUserAtLeast(UserGroup.ADMIN, sw360User)) {
                ThriftClients thriftClients = new ThriftClients();
                return thriftClients.makeScheduleClient().unscheduleAllServices(sw360User);
            } else {
                throw new AccessDeniedException("User does not have admin access");
            }
        } catch (SW360Exception sw360Exp) {
            if (sw360Exp.getErrorCode() == 403) {
                throw new AccessDeniedException("User does not have admin access", sw360Exp);
            } else {
                throw sw360Exp;
            }
        }
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
        return new ThriftClients().makeCvesearchClient().update();
    }
}

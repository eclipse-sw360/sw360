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

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class Sw360ScheduleService {

    public RequestStatus cancelAllServices(User sw360User) throws TException {
        try {
            if (PermissionUtils.isUserAtLeast(UserGroup.ADMIN, sw360User)) {
                RequestStatus requestStatus = new ThriftClients().makeScheduleClient().unscheduleAllServices(sw360User);
                return requestStatus;
            } else {
                throw new HttpMessageNotReadableException("User is not admin");
            }
        } catch (TException e) {
            throw new TException(e.getMessage());
        }
    }

}

/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.schedule;

import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.RequestStatusWithBoolean;
import org.eclipse.sw360.datahandler.services.common.RequestSummary;
import org.eclipse.sw360.datahandler.services.users.User;

/**
 * Client API for the schedule backend service.
 */
public interface ScheduleClient {

    RequestSummary scheduleService(String serviceName);

    RequestStatus unscheduleService(String serviceName, User user);

    RequestStatus triggerManualService(String serviceName, User user);

    RequestStatus cancelAllServices(User user);

    RequestStatusWithBoolean isServiceScheduled(String serviceName, User user);

    RequestStatusWithBoolean isAnyServiceScheduled(User user);

    Integer getFirstRunOffset(String serviceName);

    Integer getInterval(String serviceName);

    String getNextSync(String serviceName);
}

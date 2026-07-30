/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.department;

import org.eclipse.sw360.common.utils.converter.common.RequestStatusConverter;
import org.eclipse.sw360.common.utils.converter.common.RequestStatusWithBooleanConverter;
import org.eclipse.sw360.common.utils.converter.common.RequestSummaryConverter;
import org.eclipse.sw360.common.utils.converter.users.UserConverter;
import org.eclipse.sw360.datahandler.schedule.ScheduleClient;
import org.eclipse.sw360.datahandler.schedule.ScheduleClients;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.springframework.stereotype.Component;

@Component
public class ScheduleRestClient {

    private ScheduleClient client() {
        return ScheduleClients.get();
    }

    public org.eclipse.sw360.datahandler.thrift.RequestStatusWithBoolean isServiceScheduled(
            String serviceName, User user) {
        return RequestStatusWithBooleanConverter.toThrift(
                client().isServiceScheduled(serviceName, UserConverter.fromThrift(user)));
    }

    public org.eclipse.sw360.datahandler.thrift.RequestSummary scheduleService(String serviceName) {
        return RequestSummaryConverter.toThrift(client().scheduleService(serviceName));
    }

    public org.eclipse.sw360.datahandler.thrift.RequestStatus unscheduleService(String serviceName, User user) {
        return RequestStatusConverter.toThrift(
                client().unscheduleService(serviceName, UserConverter.fromThrift(user)));
    }

    public int getInterval(String serviceName) {
        Integer interval = client().getInterval(serviceName);
        return interval != null ? interval : 0;
    }

    public String getNextSync(String serviceName) {
        return client().getNextSync(serviceName);
    }
}

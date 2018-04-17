/*
 * Copyright (c) Bosch Software Innovations GmbH 2016.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */


include "sw360.thrift"
include "users.thrift"

namespace java org.eclipse.sw360.datahandler.thrift.schedule
namespace php sw360.thrift.schedule

typedef sw360.RequestStatus RequestStatus
typedef sw360.RequestStatusWithBoolean RequestStatusWithBoolean
typedef sw360.RequestSummary RequestSummary
typedef users.User User

service ScheduleService {
    /*
     * a service with service name is scheduled
     * serviceName has to be registered in ThriftClients
     * service must provide an "update" method
     *
     * user has to be admin, otherwise FAILURE is returned
     */
    RequestSummary scheduleService(1: string serviceName);

    /*
     * all tasks with  name serviceName are cancelled
     * user has to be admin, otherwise FAILURE is returned
     */
    RequestStatus unscheduleService(1: string serviceName, 2: User user);

    /*
     * all scheduled tasks are cancelled
     * user has to be admin, otherwise FAILURE is returned
     */
    RequestStatus unscheduleAllServices(1: User user);


    RequestStatusWithBoolean isServiceScheduled(1: string serviceName, 2: User user);

    RequestStatusWithBoolean isAnyServiceScheduled(1: User user);

    i32 getFirstRunOffset(1: string serviceName);

    string getNextSync(1: string serviceName);

    i32 getInterval(1: string serviceName);
}

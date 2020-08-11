/*
 * Copyright Siemens AG, 2016. Part of the SW360 Portal Project.
 * With modifications from Bosch Software Innovations GmbH, 2016.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.schedule.timer;

import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

/**
 * creates new {@link java.util.TimerTask} which will be executed on the next scheduled time
 *
 * @author stefan.jaeger@evosoft.com
 * @author birgit.heydenreich@tngtech.com
 */
public class ScheduleSyncTask extends SW360Task {
    private static final Logger log = LogManager.getLogger(ScheduleSyncTask.class);
    private final Supplier<RequestStatus> body;

    public ScheduleSyncTask(Supplier<RequestStatus> body, String name) {
        super(name);
        this.body = body;
    }

    @Override
    public void run() {
        RequestStatus requestStatus = body.get();
        if (RequestStatus.SUCCESS.equals(requestStatus)) {
            log.info("Successfully finished ScheduleSyncTask name=" + getName() + " id=" + getId() + ".");
        } else {
            log.error("ScheduleSyncTask " + getId() + " failed.");
        }
    }
}

/*
 * Copyright Siemens AG, 2016. Part of the SW360 Portal Project.
 * With modifications from Bosch Software Innovations GmbH, 2016.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.schedule.timer;

import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.apache.log4j.Logger;

import java.util.function.Supplier;

import org.apache.log4j.Logger;

/**
 * creates new {@link java.util.TimerTask} which will be executed on the next scheduled time
 *
 * @author stefan.jaeger@evosoft.com
 * @author birgit.heydenreich@tngtech.com
 */
public class ScheduleSyncTask extends SW360Task {
    private static final Logger log = Logger.getLogger(ScheduleSyncTask.class);
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

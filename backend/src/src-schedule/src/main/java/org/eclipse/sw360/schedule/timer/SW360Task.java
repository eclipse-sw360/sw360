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

import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;

import java.util.Date;
import java.util.TimerTask;
import java.util.UUID;

/**
 * creates new {@link TimerTask} which will be executed on the scheduled execution time
 *
 * @author stefan.jaeger@evosoft.com
 * @author birgit.heydenreich@tngtech.com
 */
public abstract class SW360Task extends TimerTask {
    private static final Logger log = LogManager.getLogger(SW360Task.class);

    private String id = UUID.randomUUID().toString();
    private String name;

    public SW360Task (String name){
        this.name = name;
    }

    public String getId() {
        return this.id;
    }
    public String getName() {
        return this.name;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("SW360Task{");
        sb.append("name='").append(name).append('\'');
        sb.append("id='").append(id).append('\'');
        sb.append("scheduledExecutionTime='").append(SW360Utils.getDateTimeString(new Date(this.scheduledExecutionTime()))).append('\'');
        sb.append('}');
        return sb.toString();
    }
}

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

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicBoolean;
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

    /**
     * Fires the task body in a dedicated daemon thread so the single-threaded
     * {@link java.util.Timer} is never blocked by long-running Thrift calls.
     * A per-service {@link AtomicBoolean} flag (managed in {@link Scheduler})
     * prevents concurrent executions of the same service.
     */
    @Override
    public void run() {
        AtomicBoolean running = Scheduler.getOrCreateRunningFlag(getName());
        if (!running.compareAndSet(false, true)) {
            log.info("Schedule: Service '{}' (task: {}) is already running; skipping this triggered execution.",
                    getName(), getId());
            return;
        }

        final String taskName = getName();
        Thread thread = getThread(taskName, running);
        thread.setDaemon(true);
        thread.start();
    }

    private @Nonnull Thread getThread(String taskName, AtomicBoolean running) {
        final String taskId = getId();
        return new Thread(() -> {
            try {
                RequestStatus requestStatus = body.get();
                if (RequestStatus.SUCCESS.equals(requestStatus)) {
                    log.info("Successfully finished ScheduleSyncTask name={} id={}.", taskName, taskId);
                } else {
                    log.error("ScheduleSyncTask {} ({}) failed.", taskName, taskId);
                }
            } finally {
                running.set(false);
            }
        }, "sw360-schedule-" + getName());
    }
}

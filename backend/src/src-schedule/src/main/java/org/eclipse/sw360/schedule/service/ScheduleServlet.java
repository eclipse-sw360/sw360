/*
 * Copyright (c) Bosch Software Innovations GmbH 2016.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.schedule.service;

import org.eclipse.sw360.schedule.timer.ScheduleConstants;
import org.eclipse.sw360.datahandler.thrift.schedule.ScheduleService;
import org.eclipse.sw360.projects.Sw360ThriftServlet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;

import javax.servlet.ServletException;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;

public class ScheduleServlet extends Sw360ThriftServlet {

    Logger log = LogManager.getLogger(ScheduleServlet.class);
    static ScheduleHandler handler = new ScheduleHandler();

    public ScheduleServlet() throws MalformedURLException, FileNotFoundException, TException {
        // Create a service processor using the provided handler
        super(new ScheduleService.Processor<>(handler), new TCompactProtocol.Factory());
    }

    public void init() throws ServletException {
        super.init();
        try {
            autoStart();
        } catch (TException te) {
            throw new ServletException(te.getMessage());
        }
    }

    private void autoStart() throws TException {
        log.info("Auto-starting scheduling tasks in schedule service...");
        String[] servicesToSchedule = ScheduleConstants.autostartServices;
        for (String serviceName : servicesToSchedule) {
            String cleanServiceName = serviceName.trim();
            if(! "".equals(cleanServiceName)){
                handler.scheduleService(cleanServiceName);
            }
        }
        log.info("Auto-start completed.");
    }
}

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
package org.eclipse.sw360.schedule.service;

import org.eclipse.sw360.schedule.timer.ScheduleConstants;
import org.eclipse.sw360.schedule.timer.Scheduler;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestStatusWithBoolean;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.schedule.ScheduleService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;

import java.util.Date;
import java.util.Optional;
import java.util.function.Supplier;

public class ScheduleHandler implements ScheduleService.Iface {

    ThriftClients thriftClients;
    Logger log;

    public ScheduleHandler() {
        thriftClients = new ThriftClients();
        log = Logger.getLogger(ScheduleHandler.class);
    }

    @FunctionalInterface
    public interface SupplierThrowingTException {
        RequestStatus get() throws TException;
    }

    private boolean wrapSupplierException(SupplierThrowingTException body, String serviceName){
        Supplier<RequestStatus> wrappedBody = () -> {
            try {
                return body.get();
            } catch (TException e) {
                log.error("Was not able to schedule sync for client with name:" + serviceName + " message:" + e.getMessage(), e);
                return RequestStatus.FAILURE;
            }
        };
        return Scheduler.scheduleNextSync(wrappedBody, serviceName);
    }

    @Override
    public RequestSummary scheduleService(String serviceName) throws TException {
        if(ScheduleConstants.invalidConfiguredServices.contains(serviceName)){
            log.info("Could not schedule " + serviceName + " because of invalid configuration.");
            return new RequestSummary(RequestStatus.FAILURE);
        }

        Scheduler.cancelSyncJobOfService(serviceName);

        boolean successSync = false;
        switch (serviceName) {
            case ThriftClients.CVESEARCH_SERVICE:
                successSync = wrapSupplierException(() -> thriftClients.makeCvesearchClient().update(), serviceName);
                break;
            default:
                log.error("Could not schedule service: " + serviceName + ". Reason: service is not registered in ThriftClients.");
        }

        if (successSync){
            RequestSummary summary = new RequestSummary(RequestStatus.SUCCESS);
            summary.setMessage(SW360Utils.getDateTimeString(Scheduler.getNextSync()));
            return summary;
        } else {
            return new RequestSummary(RequestStatus.FAILURE);
        }
    }

    @Override
    public RequestStatus unscheduleService(String serviceName, User user) throws TException {
        if (!PermissionUtils.isAdmin(user)) {
            return RequestStatus.FAILURE;
        }
        return Scheduler.cancelSyncJobOfService(serviceName);
    }

    @Override
    public RequestStatus unscheduleAllServices(User user) throws TException {
        if (!PermissionUtils.isAdmin(user)) {
            return RequestStatus.FAILURE;
        }
        return Scheduler.cancelAllSyncJobs();
    }

    @Override
    public RequestStatusWithBoolean isServiceScheduled(String serviceName, User user) {
        if (!PermissionUtils.isAdmin(user)) {
            return failedRequestStatusWithBoolean();
        }
        boolean answer = Scheduler.isServiceScheduled(serviceName);
        return new RequestStatusWithBoolean()
                .setRequestStatus(RequestStatus.SUCCESS)
                .setAnswerPositive(answer);
    }

    @Override
    public RequestStatusWithBoolean isAnyServiceScheduled(User user) {
        if (!PermissionUtils.isAdmin(user)) {
            return failedRequestStatusWithBoolean();
        }
        boolean answer = Scheduler.isAnyServiceScheduled();
        return new RequestStatusWithBoolean()
                .setRequestStatus(RequestStatus.SUCCESS)
                .setAnswerPositive(answer);
    }

    private RequestStatusWithBoolean failedRequestStatusWithBoolean(){
        return new RequestStatusWithBoolean().setRequestStatus(RequestStatus.FAILURE);
    }

    @Override
    public int getFirstRunOffset(String serviceName){
        return ScheduleConstants.SYNC_FIRST_RUN_OFFSET_SEC.get(serviceName) != null ? ScheduleConstants.SYNC_FIRST_RUN_OFFSET_SEC.get(serviceName) : -1;
    }

    @Override
    public String getNextSync(String serviceName){
        Optional<Date> syncDate = Scheduler.getNextSync(serviceName);
        return syncDate.isPresent() ? syncDate.get().toString() : "";
    }

    @Override
    public int getInterval(String serviceName){
        return ScheduleConstants.SYNC_INTERVAL_SEC.get(serviceName) != null ? ScheduleConstants.SYNC_INTERVAL_SEC.get(serviceName) : -1 ;
    }
}

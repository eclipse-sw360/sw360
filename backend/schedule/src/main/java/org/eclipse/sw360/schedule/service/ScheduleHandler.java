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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestStatusWithBoolean;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.schedule.ScheduleService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.schedule.timer.ScheduleConstants;
import org.eclipse.sw360.schedule.timer.Scheduler;

import java.util.Date;
import java.util.Optional;
import java.util.function.Supplier;

public class ScheduleHandler implements ScheduleService.Iface {

    Logger log;

    public ScheduleHandler() {
        log = LogManager.getLogger(ScheduleHandler.class);
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
                successSync = wrapSupplierException(() -> ThriftClients.makeCvesearchClient().update(), serviceName);
                break;
            case ThriftClients.SVMSYNC_SERVICE:
                successSync = wrapSupplierException(() -> ThriftClients.makeVMClient().synchronizeComponents().getRequestStatus(), serviceName);
                break;
            case ThriftClients.SVMMATCH_SERVICE:
                successSync = wrapSupplierException(() -> ThriftClients.makeVMClient().triggerReverseMatch().getRequestStatus(), serviceName);
                break;
            case ThriftClients.SVM_LIST_UPDATE_SERVICE:
                successSync = wrapSupplierException(() -> ThriftClients.makeProjectClient().exportForMonitoringList(), serviceName);
                break;
            case ThriftClients.SVM_TRACKING_FEEDBACK_SERVICE:
                successSync = wrapSupplierException(() -> ThriftClients.makeComponentClient().updateReleasesWithSvmTrackingFeedback(), serviceName);
                break;
            case ThriftClients.DELETE_ATTACHMENT_SERVICE:
                successSync = wrapSupplierException(() -> ThriftClients.makeAttachmentClient().deleteOldAttachmentFromFileSystem(), serviceName);
                break;
            case ThriftClients.IMPORT_DEPARTMENT_SERVICE:
                successSync = wrapSupplierException(() -> ThriftClients.makeUserClient().importDepartmentSchedule(), serviceName);
                break;
            case ThriftClients.SRC_UPLOAD_SERVICE:
                successSync = wrapSupplierException(() -> ThriftClients.makeComponentClient().uploadSourceCodeAttachmentToReleases(), serviceName);
                break;
            case ThriftClients.LICENSEDB_SYNC_SERVICE:
                successSync = wrapSupplierException(() -> ThriftClients.makeLicenseClient().importAllLicenseDBLicenses(null).getRequestStatus(), serviceName);
                break;
            default:
                log.error("Could not schedule service: " + serviceName + ". Reason: service is not registered in ThriftClients.");
        }

        if (successSync){
            RequestSummary summary = new RequestSummary(RequestStatus.SUCCESS);
            summary.setMessage(getNextSync(serviceName));
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
    public RequestStatus triggerManualService(String serviceName, User user) throws TException {
        if (!PermissionUtils.isAdmin(user)) {
            return RequestStatus.ACCESS_DENIED;
        }
        try {
            return switch (serviceName) {
                case ThriftClients.CVESEARCH_SERVICE ->
                        ThriftClients.makeCvesearchClient().update();
                case ThriftClients.SVMSYNC_SERVICE ->
                        ThriftClients.makeVMClient().synchronizeComponents().getRequestStatus();
                case ThriftClients.SVMMATCH_SERVICE ->
                        ThriftClients.makeVMClient().triggerReverseMatch().getRequestStatus();
                case ThriftClients.DELETE_ATTACHMENT_SERVICE ->
                        ThriftClients.makeAttachmentClient().deleteOldAttachmentFromFileSystem();
                case ThriftClients.SVM_LIST_UPDATE_SERVICE ->
                        ThriftClients.makeProjectClient().exportForMonitoringList();
                case ThriftClients.SVM_TRACKING_FEEDBACK_SERVICE ->
                        ThriftClients.makeComponentClient().updateReleasesWithSvmTrackingFeedback();
                case ThriftClients.IMPORT_DEPARTMENT_SERVICE ->
                        ThriftClients.makeUserClient().importDepartmentSchedule();
                case ThriftClients.SRC_UPLOAD_SERVICE ->
                        ThriftClients.makeComponentClient().uploadSourceCodeAttachmentToReleases();
                case ThriftClients.LICENSEDB_SYNC_SERVICE ->
                        ThriftClients.makeLicenseClient().importAllLicenseDBLicenses(user).getRequestStatus();
                default -> {
                    log.error("Could not trigger service: {}. Reason: service is not registered.", serviceName);
                    yield RequestStatus.FAILURE;
                }
            };
        } catch (TException e) {
            log.error("Error while manually triggering service '{}': {}", serviceName, e.getMessage(), e);
            throw e;
        }
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

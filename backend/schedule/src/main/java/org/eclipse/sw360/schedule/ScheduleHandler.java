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
package org.eclipse.sw360.schedule;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.common.utils.ThriftConverter;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.RequestStatusWithBoolean;
import org.eclipse.sw360.datahandler.services.common.RequestSummary;
import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.eclipse.sw360.datahandler.services.common.ServiceNames;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.schedule.client.CveSearchRestClient;
import org.eclipse.sw360.schedule.client.VMComponentsRestClient;
import org.eclipse.sw360.schedule.timer.ScheduleConstants;
import org.eclipse.sw360.schedule.timer.Scheduler;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Date;
import java.util.Optional;
import java.util.function.Supplier;

@Service
public class ScheduleHandler {
    private static final Logger log = LogManager.getLogger(ScheduleHandler.class);

    private final CveSearchRestClient cveSearchRestClient;
    private final VMComponentsRestClient vmComponentsRestClient;

    public ScheduleHandler(CveSearchRestClient cveSearchRestClient, VMComponentsRestClient vmComponentsRestClient) {
        this.cveSearchRestClient = cveSearchRestClient;
        this.vmComponentsRestClient = vmComponentsRestClient;
    }

    @PostConstruct
    public void autoStart() {
        log.info("Auto-starting scheduling tasks in schedule service...");
        for (String serviceName : ScheduleConstants.autostartServices) {
            String cleanServiceName = serviceName.trim();
            if (!cleanServiceName.isEmpty()) {
                scheduleService(cleanServiceName);
            }
        }
        log.info("Auto-start completed.");
    }

    @FunctionalInterface
    private interface DownstreamServiceCall {
        org.eclipse.sw360.datahandler.thrift.RequestStatus execute() throws Exception;
    }

    private RequestStatus callDownstreamService(DownstreamServiceCall call) {
        try {
            return ThriftConverter.fromThriftRequestStatus(call.execute());
        } catch (Exception e) {
            throw new SW360Exception("Downstream service call failed", e);
        }
    }

    private boolean wrapForScheduler(DownstreamServiceCall call, String serviceName) {
        Supplier<RequestStatus> wrappedBody = () -> {
            try {
                return ThriftConverter.fromThriftRequestStatus(call.execute());
            } catch (Exception e) {
                log.error("Was not able to schedule sync for client with name:{} message:{}", serviceName, e.getMessage(), e);
                return RequestStatus.FAILURE;
            }
        };
        return Scheduler.scheduleNextSync(wrappedBody, serviceName);
    }

    private boolean wrapForScheduler(Supplier<RequestStatus> call, String serviceName) {
        Supplier<RequestStatus> wrappedBody = () -> {
            try {
                return call.get();
            } catch (Exception e) {
                log.error("Was not able to schedule sync for client with name:{} message:{}", serviceName, e.getMessage(), e);
                return RequestStatus.FAILURE;
            }
        };
        return Scheduler.scheduleNextSync(wrappedBody, serviceName);
    }

    public RequestSummary scheduleService(String serviceName) {
        if (ScheduleConstants.invalidConfiguredServices.contains(serviceName)) {
            log.info("Could not schedule {} because of invalid configuration.", serviceName);
            return new RequestSummary().setRequestStatus(RequestStatus.FAILURE);
        }

        Scheduler.cancelSyncJobOfService(serviceName);

        boolean successSync = switch (serviceName) {
            case ServiceNames.CVESEARCH_SERVICE ->
                    wrapForScheduler(cveSearchRestClient::update, serviceName);
            case ServiceNames.SVMSYNC_SERVICE ->
                    wrapForScheduler(vmComponentsRestClient::synchronizeComponents, serviceName);
            case ServiceNames.SVMMATCH_SERVICE ->
                    wrapForScheduler(vmComponentsRestClient::triggerReverseMatch, serviceName);
            case ServiceNames.SVM_LIST_UPDATE_SERVICE ->
                    wrapForScheduler(() -> ThriftClients.makeProjectClient().exportForMonitoringList(), serviceName);
            case ServiceNames.SVM_TRACKING_FEEDBACK_SERVICE ->
                    wrapForScheduler(() -> ThriftClients.makeComponentClient().updateReleasesWithSvmTrackingFeedback(), serviceName);
            case ServiceNames.DELETE_ATTACHMENT_SERVICE ->
                    wrapForScheduler(() -> ThriftClients.makeAttachmentClient().deleteOldAttachmentFromFileSystem(), serviceName);
            case ServiceNames.IMPORT_DEPARTMENT_SERVICE ->
                    wrapForScheduler(() -> ThriftClients.makeUserClient().importDepartmentSchedule(), serviceName);
            case ServiceNames.SRC_UPLOAD_SERVICE ->
                    wrapForScheduler(() -> ThriftClients.makeComponentClient().uploadSourceCodeAttachmentToReleases(), serviceName);
            default -> {
                log.error("Could not schedule service: {}. Reason: service is not registered.", serviceName);
                yield false;
            }
        };

        if (successSync) {
            return new RequestSummary()
                    .setRequestStatus(RequestStatus.SUCCESS)
                    .setMessage(getNextSync(serviceName));
        } else {
            return new RequestSummary().setRequestStatus(RequestStatus.FAILURE);
        }
    }

    public RequestStatus unscheduleService(String serviceName, User user) {
        if (!PermissionUtils.isAdmin(user)) {
            return RequestStatus.FAILURE;
        }
        return Scheduler.cancelSyncJobOfService(serviceName);
    }

    public RequestStatus triggerManualService(String serviceName, User user) {
        if (!PermissionUtils.isAdmin(user)) {
            return RequestStatus.ACCESS_DENIED;
        }
        return switch (serviceName) {
            case ServiceNames.CVESEARCH_SERVICE ->
                    cveSearchRestClient.update();
            case ServiceNames.SVMSYNC_SERVICE ->
                    vmComponentsRestClient.synchronizeComponents();
            case ServiceNames.SVMMATCH_SERVICE ->
                    vmComponentsRestClient.triggerReverseMatch();
            case ServiceNames.DELETE_ATTACHMENT_SERVICE ->
                    callDownstreamService(() -> ThriftClients.makeAttachmentClient().deleteOldAttachmentFromFileSystem());
            case ServiceNames.SVM_LIST_UPDATE_SERVICE ->
                    callDownstreamService(() -> ThriftClients.makeProjectClient().exportForMonitoringList());
            case ServiceNames.SVM_TRACKING_FEEDBACK_SERVICE ->
                    callDownstreamService(() -> ThriftClients.makeComponentClient().updateReleasesWithSvmTrackingFeedback());
            case ServiceNames.IMPORT_DEPARTMENT_SERVICE ->
                    callDownstreamService(() -> ThriftClients.makeUserClient().importDepartmentSchedule());
            case ServiceNames.SRC_UPLOAD_SERVICE ->
                    callDownstreamService(() -> ThriftClients.makeComponentClient().uploadSourceCodeAttachmentToReleases());
            default -> {
                log.error("Could not trigger service: {}. Reason: service is not registered.", serviceName);
                yield RequestStatus.FAILURE;
            }
        };
    }

    public RequestStatus unscheduleAllServices(User user) {
        if (!PermissionUtils.isAdmin(user)) {
            return RequestStatus.FAILURE;
        }
        return Scheduler.cancelAllSyncJobs();
    }

    public RequestStatusWithBoolean isServiceScheduled(String serviceName, User user) {
        if (!PermissionUtils.isAdmin(user)) {
            return new RequestStatusWithBoolean().setRequestStatus(RequestStatus.FAILURE);
        }
        boolean answer = Scheduler.isServiceScheduled(serviceName);
        return new RequestStatusWithBoolean()
                .setRequestStatus(RequestStatus.SUCCESS)
                .setAnswerPositive(answer);
    }

    public RequestStatusWithBoolean isAnyServiceScheduled(User user) {
        if (!PermissionUtils.isAdmin(user)) {
            return new RequestStatusWithBoolean().setRequestStatus(RequestStatus.FAILURE);
        }
        boolean answer = Scheduler.isAnyServiceScheduled();
        return new RequestStatusWithBoolean()
                .setRequestStatus(RequestStatus.SUCCESS)
                .setAnswerPositive(answer);
    }

    public int getFirstRunOffset(String serviceName) {
        Integer offset = ScheduleConstants.SYNC_FIRST_RUN_OFFSET_SEC.get(serviceName);
        return offset != null ? offset : -1;
    }

    public String getNextSync(String serviceName) {
        Optional<Date> syncDate = Scheduler.getNextSync(serviceName);
        return syncDate.isPresent() ? syncDate.get().toString() : "";
    }

    public int getInterval(String serviceName) {
        Integer interval = ScheduleConstants.SYNC_INTERVAL_SEC.get(serviceName);
        return interval != null ? interval : -1;
    }
}

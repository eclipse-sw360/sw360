/*
 * Copyright Siemens AG, 2023-2024.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.schedule;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.RequestStatusWithBoolean;
import org.eclipse.sw360.datahandler.services.common.RequestSummary;
import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.eclipse.sw360.datahandler.services.common.ServiceNames;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper.throwIfNotAdmin;

@Service
@RequiredArgsConstructor
public class Sw360ScheduleService {
    private static final Logger log = LogManager.getLogger(Sw360ScheduleService.class);

    private static final String SCHEDULE_URI = "/schedule/api/schedule";

    private final RestClient restClient;

    private void addUserHeaders(RestClient.RequestHeadersSpec<?> spec, User user) {
        spec.header("X-User-Email", user.getEmail())
            .header("X-User-Department", user.getDepartment())
            .header("X-User-Group", user.getUserGroup() != null ? user.getUserGroup().name() : "");
    }

    public RequestSummary scheduleService(User sw360User, String serviceName) {
        throwIfNotAdmin(sw360User);
        var req = restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(SCHEDULE_URI + "/scheduleService")
                        .queryParam("serviceName", serviceName)
                        .build());
        return req.retrieve().body(RequestSummary.class);
    }

    public RequestStatus unscheduleService(User sw360User, String serviceName) {
        throwIfNotAdmin(sw360User);
        var req = restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(SCHEDULE_URI + "/unscheduleService")
                        .queryParam("serviceName", serviceName)
                        .build());
        addUserHeaders(req, sw360User);
        return req.retrieve().body(RequestStatus.class);
    }

    public RequestStatus triggerManualService(User sw360User, String serviceName) {
        throwIfNotAdmin(sw360User);
        var req = restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(SCHEDULE_URI + "/triggerManualService")
                        .queryParam("serviceName", serviceName)
                        .build());
        addUserHeaders(req, sw360User);
        return req.retrieve().body(RequestStatus.class);
    }

    public RequestStatus cancelAllServices(User sw360User) {
        throwIfNotAdmin(sw360User);
        var req = restClient.post()
                .uri(SCHEDULE_URI + "/unscheduleAllServices");
        addUserHeaders(req, sw360User);
        return req.retrieve().body(RequestStatus.class);
    }

    public RequestStatus isServiceScheduled(String serviceName, User sw360User) {
        throwIfNotAdmin(sw360User);
        try {
            var req = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(SCHEDULE_URI + "/isServiceScheduled")
                            .queryParam("serviceName", serviceName)
                            .build());
            addUserHeaders(req, sw360User);
            RequestStatusWithBoolean result = req.retrieve().body(RequestStatusWithBoolean.class);
            boolean isScheduled = result != null && Boolean.TRUE.equals(result.getAnswerPositive());
            return isScheduled ? RequestStatus.SUCCESS : RequestStatus.FAILURE;
        } catch (Exception e) {
            log.error("Error occurred while fetching the status of service '{}':", serviceName, e);
            throw new SW360Exception("Failed to check schedule status for service: " + serviceName, e);
        }
    }

    public RequestStatus isAnyServiceScheduled(User sw360User) {
        throwIfNotAdmin(sw360User);
        try {
            var req = restClient.get()
                    .uri(SCHEDULE_URI + "/isAnyServiceScheduled");
            addUserHeaders(req, sw360User);
            RequestStatusWithBoolean result = req.retrieve().body(RequestStatusWithBoolean.class);
            boolean isAny = result != null && Boolean.TRUE.equals(result.getAnswerPositive());
            return isAny ? RequestStatus.SUCCESS : RequestStatus.FAILURE;
        } catch (Exception e) {
            log.error("Error occurred while fetching the status of services", e);
            throw new SW360Exception("Failed to check if any service is scheduled", e);
        }
    }

    public Map<String, Object> getServiceDetails(String serviceName, User sw360User) {
        throwIfNotAdmin(sw360User);
        try {
            var isScheduledReq = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(SCHEDULE_URI + "/isServiceScheduled")
                            .queryParam("serviceName", serviceName)
                            .build());
            addUserHeaders(isScheduledReq, sw360User);
            RequestStatusWithBoolean statusResult = isScheduledReq.retrieve().body(RequestStatusWithBoolean.class);
            boolean isScheduled = statusResult != null && Boolean.TRUE.equals(statusResult.getAnswerPositive());

            Integer offsetSeconds = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(SCHEDULE_URI + "/getFirstRunOffset")
                            .queryParam("serviceName", serviceName)
                            .build())
                    .retrieve().body(Integer.class);

            Integer intervalSeconds = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(SCHEDULE_URI + "/getInterval")
                            .queryParam("serviceName", serviceName)
                            .build())
                    .retrieve().body(Integer.class);

            String nextSync = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(SCHEDULE_URI + "/getNextSync")
                            .queryParam("serviceName", serviceName)
                            .build())
                    .retrieve().body(String.class);

            return Map.of(
                    "isScheduled", isScheduled,
                    "firstOffsetSeconds", offsetSeconds != null ? offsetSeconds : 0,
                    "intervalSeconds", intervalSeconds != null ? intervalSeconds : 0,
                    "nextSynchronization", nextSync != null ? nextSync : "N/A"
            );
        } catch (Exception e) {
            log.error("Error occurred while fetching details for service '{}':", serviceName, e);
            throw new SW360Exception("Failed to fetch details for service: " + serviceName, e);
        }
    }

    public Map<String, Map<String, Object>> getAllServicesDetails(User sw360User) {
        throwIfNotAdmin(sw360User);
        List<String> services = List.of(
                ServiceNames.CVESEARCH_SERVICE,
                ServiceNames.SVMSYNC_SERVICE,
                ServiceNames.SVMMATCH_SERVICE,
                ServiceNames.DELETE_ATTACHMENT_SERVICE,
                ServiceNames.SVM_TRACKING_FEEDBACK_SERVICE,
                ServiceNames.SVM_LIST_UPDATE_SERVICE,
                ServiceNames.SRC_UPLOAD_SERVICE,
                ServiceNames.IMPORT_DEPARTMENT_SERVICE
        );

        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (String svc : services) {
            try {
                result.put(svc, getServiceDetails(svc, sw360User));
            } catch (Exception e) {
                log.warn("Could not fetch details for service '{}': {}", svc, e.getMessage());
                result.put(svc, Map.of("error", "Failed to retrieve details"));
            }
        }
        return result;
    }
}

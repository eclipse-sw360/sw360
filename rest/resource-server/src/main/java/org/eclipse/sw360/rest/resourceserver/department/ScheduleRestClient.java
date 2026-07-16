/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.department;

import lombok.RequiredArgsConstructor;
import org.eclipse.sw360.common.utils.converter.common.RequestStatusConverter;
import org.eclipse.sw360.common.utils.converter.common.RequestStatusWithBooleanConverter;
import org.eclipse.sw360.common.utils.converter.common.RequestSummaryConverter;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.RequestStatusWithBoolean;
import org.eclipse.sw360.datahandler.services.common.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class ScheduleRestClient {

    private static final String SCHEDULE_URI = "/schedule/api/schedule";

    private final RestClient restClient;

    public org.eclipse.sw360.datahandler.thrift.RequestStatusWithBoolean isServiceScheduled(
            String serviceName, User user) {
        RequestStatusWithBoolean response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(SCHEDULE_URI + "/isServiceScheduled")
                        .queryParam("serviceName", serviceName)
                        .build())
                .headers(headers -> addUserHeaders(headers, user))
                .retrieve()
                .body(RequestStatusWithBoolean.class);
        return RequestStatusWithBooleanConverter.toThrift(response);
    }

    public org.eclipse.sw360.datahandler.thrift.RequestSummary scheduleService(String serviceName) {
        RequestSummary response = restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(SCHEDULE_URI + "/scheduleService")
                        .queryParam("serviceName", serviceName)
                        .build())
                .retrieve()
                .body(RequestSummary.class);
        return RequestSummaryConverter.toThrift(response);
    }

    public org.eclipse.sw360.datahandler.thrift.RequestStatus unscheduleService(String serviceName, User user) {
        RequestStatus response = restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(SCHEDULE_URI + "/unscheduleService")
                        .queryParam("serviceName", serviceName)
                        .build())
                .headers(headers -> addUserHeaders(headers, user))
                .retrieve()
                .body(RequestStatus.class);
        return RequestStatusConverter.toThrift(response);
    }

    public int getInterval(String serviceName) {
        Integer interval = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(SCHEDULE_URI + "/getInterval")
                        .queryParam("serviceName", serviceName)
                        .build())
                .retrieve()
                .body(Integer.class);
        return interval != null ? interval : 0;
    }

    public String getNextSync(String serviceName) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(SCHEDULE_URI + "/getNextSync")
                        .queryParam("serviceName", serviceName)
                        .build())
                .retrieve()
                .body(String.class);
    }

    private static void addUserHeaders(org.springframework.http.HttpHeaders headers, User user) {
        headers.set("X-User-Email", user.getEmail());
        headers.set("X-User-Department", user.getDepartment());
        headers.set("X-User-Group", user.getUserGroup() != null ? user.getUserGroup().name() : "");
    }
}

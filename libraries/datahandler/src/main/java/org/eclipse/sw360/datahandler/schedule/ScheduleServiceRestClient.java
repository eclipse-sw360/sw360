/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.schedule;

import java.util.function.Supplier;

import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.RequestStatusWithBoolean;
import org.eclipse.sw360.datahandler.services.common.RequestSummary;
import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.eclipse.sw360.datahandler.services.users.User;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

public class ScheduleServiceRestClient implements ScheduleClient {

    private static final String BASE = "/schedule/api/schedule";

    private final RestClient restClient;

    public ScheduleServiceRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    private static <T> T call(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (RestClientResponseException e) {
            String body = e.getResponseBodyAsString();
            throw new SW360Exception(body == null || body.isEmpty() ? e.getMessage() : body,
                    e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw new SW360Exception(e.getMessage(), 503, e);
        }
    }

    private static void addUser(HttpHeaders headers, User user) {
        if (user == null) {
            return;
        }
        if (user.getEmail() != null) {
            headers.set("X-User-Email", user.getEmail());
        }
        if (user.getDepartment() != null) {
            headers.set("X-User-Department", user.getDepartment());
        }
        if (user.getUserGroup() != null) {
            headers.set("X-User-Group", user.getUserGroup().name());
        }
    }

    @Override
    public RequestSummary scheduleService(String serviceName) {
        return call(() -> restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE + "/scheduleService")
                        .queryParam("serviceName", serviceName)
                        .build())
                .retrieve()
                .body(RequestSummary.class));
    }

    @Override
    public RequestStatus unscheduleService(String serviceName, User user) {
        return call(() -> restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE + "/unscheduleService")
                        .queryParam("serviceName", serviceName)
                        .build())
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(RequestStatus.class));
    }

    @Override
    public RequestStatus triggerManualService(String serviceName, User user) {
        return call(() -> restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE + "/triggerManualService")
                        .queryParam("serviceName", serviceName)
                        .build())
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(RequestStatus.class));
    }

    @Override
    public RequestStatus cancelAllServices(User user) {
        return call(() -> restClient.post()
                .uri(BASE + "/unscheduleAllServices")
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(RequestStatus.class));
    }

    @Override
    public RequestStatusWithBoolean isServiceScheduled(String serviceName, User user) {
        return call(() -> restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE + "/isServiceScheduled")
                        .queryParam("serviceName", serviceName)
                        .build())
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(RequestStatusWithBoolean.class));
    }

    @Override
    public RequestStatusWithBoolean isAnyServiceScheduled(User user) {
        return call(() -> restClient.get()
                .uri(BASE + "/isAnyServiceScheduled")
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(RequestStatusWithBoolean.class));
    }

    @Override
    public Integer getFirstRunOffset(String serviceName) {
        return call(() -> restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE + "/getFirstRunOffset")
                        .queryParam("serviceName", serviceName)
                        .build())
                .retrieve()
                .body(Integer.class));
    }

    @Override
    public Integer getInterval(String serviceName) {
        return call(() -> restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE + "/getInterval")
                        .queryParam("serviceName", serviceName)
                        .build())
                .retrieve()
                .body(Integer.class));
    }

    @Override
    public String getNextSync(String serviceName) {
        return call(() -> restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE + "/getNextSync")
                        .queryParam("serviceName", serviceName)
                        .build())
                .retrieve()
                .body(String.class));
    }
}

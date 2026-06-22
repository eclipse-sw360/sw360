/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.fossology;

import java.util.Map;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.services.fossology.FossologyProcessRequest;
import org.eclipse.sw360.datahandler.services.fossology.FossologyReleaseRequest;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.components.ExternalToolProcess;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SW360FossologyService {

    private static final String FOSSOLOGY_URI = "/fossology/api/fossology";

    @NonNull
    private final RestClient restClient;

    @NonNull
    private final FossologyTypeBridge fossologyTypeBridge;

    private void addUserHeaders(RestClient.RequestHeadersSpec<?> spec, User user) {
        spec.header("X-User-Email", user.getEmail())
            .header("X-User-Department", user.getDepartment())
            .header("X-User-Group", user.getUserGroup() != null ? user.getUserGroup().name() : "");
    }

    public org.eclipse.sw360.datahandler.thrift.ConfigContainer getFossologyConfig() throws TException {
        var config = restClient.get()
                .uri(FOSSOLOGY_URI + "/config")
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.ConfigContainer.class);
        return fossologyTypeBridge.toThrift(config);
    }

    public RequestStatus setFossologyConfig(org.eclipse.sw360.datahandler.thrift.ConfigContainer config)
            throws TException {
        var status = restClient.put()
                .uri(FOSSOLOGY_URI + "/config")
                .body(fossologyTypeBridge.toPojo(config))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class);
        return fossologyTypeBridge.toThriftRequestStatus(status);
    }

    public RequestStatus checkConnection() throws TException {
        var status = restClient.get()
                .uri(FOSSOLOGY_URI + "/connection")
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class);
        return fossologyTypeBridge.toThriftRequestStatus(status);
    }

    public ExternalToolProcess process(String releaseId, User user, String uploadDescription) throws TException {
        var request = new FossologyProcessRequest()
                .setReleaseId(releaseId)
                .setUploadDescription(uploadDescription);
        var spec = restClient.post()
                .uri(FOSSOLOGY_URI + "/process")
                .body(request);
        addUserHeaders(spec, user);
        var process = spec.retrieve()
                .body(org.eclipse.sw360.datahandler.services.components.ExternalToolProcess.class);
        return fossologyTypeBridge.toThrift(process);
    }

    public RequestStatus markFossologyProcessOutdated(String releaseId, User user) throws TException {
        var request = new FossologyReleaseRequest().setReleaseId(releaseId);
        var spec = restClient.post()
                .uri(FOSSOLOGY_URI + "/process/outdated")
                .body(request);
        addUserHeaders(spec, user);
        var status = spec.retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class);
        return fossologyTypeBridge.toThriftRequestStatus(status);
    }

    public RequestStatus triggerReportGenerationFossology(String releaseId, User user) throws TException {
        var request = new FossologyReleaseRequest().setReleaseId(releaseId);
        var spec = restClient.post()
                .uri(FOSSOLOGY_URI + "/process/report")
                .body(request);
        addUserHeaders(spec, user);
        var status = spec.retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class);
        return fossologyTypeBridge.toThriftRequestStatus(status);
    }

    public Map<String, String> checkUnpackStatus(int uploadId) throws TException {
        return restClient.get()
                .uri(FOSSOLOGY_URI + "/unpack-status/{uploadId}", uploadId)
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<Map<String, String>>() {});
    }

    public Map<String, String> checkScanStatus(int scanJobId) throws TException {
        return restClient.get()
                .uri(FOSSOLOGY_URI + "/scan-status/{scanJobId}", scanJobId)
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<Map<String, String>>() {});
    }
}

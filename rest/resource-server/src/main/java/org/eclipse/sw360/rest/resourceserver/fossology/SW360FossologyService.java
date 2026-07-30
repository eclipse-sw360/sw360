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
import org.eclipse.sw360.common.utils.converter.users.UserConverter;
import org.eclipse.sw360.datahandler.fossology.FossologyClient;
import org.eclipse.sw360.datahandler.fossology.FossologyClients;
import org.eclipse.sw360.datahandler.services.fossology.FossologyProcessRequest;
import org.eclipse.sw360.datahandler.services.fossology.FossologyReleaseRequest;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.components.ExternalToolProcess;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.springframework.stereotype.Service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SW360FossologyService {

    @NonNull
    private final FossologyTypeBridge fossologyTypeBridge;

    private FossologyClient client() {
        return FossologyClients.get();
    }

    public org.eclipse.sw360.datahandler.thrift.ConfigContainer getFossologyConfig() throws TException {
        return fossologyTypeBridge.toThrift(client().getFossologyConfig());
    }

    public RequestStatus setFossologyConfig(org.eclipse.sw360.datahandler.thrift.ConfigContainer config)
            throws TException {
        var status = client().setFossologyConfig(fossologyTypeBridge.toPojo(config));
        return fossologyTypeBridge.toThriftRequestStatus(status);
    }

    public RequestStatus checkConnection() throws TException {
        return fossologyTypeBridge.toThriftRequestStatus(client().checkConnection());
    }

    public ExternalToolProcess process(String releaseId, User user, String uploadDescription) throws TException {
        var request = new FossologyProcessRequest()
                .setReleaseId(releaseId)
                .setUploadDescription(uploadDescription);
        return fossologyTypeBridge.toThrift(
                client().process(request, UserConverter.fromThrift(user)));
    }

    public RequestStatus markFossologyProcessOutdated(String releaseId, User user) throws TException {
        var request = new FossologyReleaseRequest().setReleaseId(releaseId);
        return fossologyTypeBridge.toThriftRequestStatus(
                client().markFossologyProcessOutdated(request, UserConverter.fromThrift(user)));
    }

    public RequestStatus triggerReportGenerationFossology(String releaseId, User user) throws TException {
        var request = new FossologyReleaseRequest().setReleaseId(releaseId);
        return fossologyTypeBridge.toThriftRequestStatus(
                client().triggerReportGenerationFossology(request, UserConverter.fromThrift(user)));
    }

    public Map<String, String> checkUnpackStatus(int uploadId) throws TException {
        return client().checkUnpackStatus(uploadId);
    }

    public Map<String, String> checkScanStatus(int scanJobId) throws TException {
        return client().checkScanStatus(scanJobId);
    }
}

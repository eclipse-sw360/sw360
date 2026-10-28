/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.fossology;

import java.util.Map;

import org.apache.thrift.TException;
import org.eclipse.sw360.common.utils.UserUtils;
import org.eclipse.sw360.datahandler.services.common.ConfigContainer;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.components.ExternalToolProcess;
import org.eclipse.sw360.datahandler.services.fossology.FossologyProcessRequest;
import org.eclipse.sw360.datahandler.services.fossology.FossologyReleaseRequest;
import org.eclipse.sw360.datahandler.services.users.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fossology")
public class FossologyController {

    private final FossologyHandler fossologyHandler;

    public FossologyController(FossologyHandler fossologyHandler) {
        this.fossologyHandler = fossologyHandler;
    }

    @GetMapping("/config")
    public ConfigContainer getFossologyConfig() throws TException {
        return fossologyHandler.getFossologyConfig();
    }

    @PutMapping("/config")
    public RequestStatus setFossologyConfig(@RequestBody ConfigContainer newConfig) throws TException {
        return fossologyHandler.setFossologyConfig(newConfig);
    }

    @GetMapping("/connection")
    public RequestStatus checkConnection() throws TException {
        return fossologyHandler.checkConnection();
    }

    @PostMapping("/process")
    public ExternalToolProcess process(
            @RequestBody FossologyProcessRequest request,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return fossologyHandler.process(request.getReleaseId(), user, request.getUploadDescription());
    }

    @PostMapping("/process/outdated")
    public RequestStatus markFossologyProcessOutdated(
            @RequestBody FossologyReleaseRequest request,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return fossologyHandler.markFossologyProcessOutdated(request.getReleaseId(), user);
    }

    @PostMapping("/process/report")
    public RequestStatus triggerReportGenerationFossology(
            @RequestBody FossologyReleaseRequest request,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return fossologyHandler.triggerReportGenerationFossology(request.getReleaseId(), user);
    }

    @GetMapping("/unpack-status/{uploadId}")
    public Map<String, String> checkUnpackStatus(@PathVariable int uploadId) throws TException {
        return fossologyHandler.checkUnpackStatus(uploadId);
    }

    @GetMapping("/scan-status/{scanJobId}")
    public Map<String, String> checkScanStatus(@PathVariable int scanJobId) throws TException {
        return fossologyHandler.checkScanStatus(scanJobId);
    }
}

/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.vmcomponents;

import java.util.List;

import org.eclipse.sw360.common.utils.UserUtils;
import org.eclipse.sw360.datahandler.services.common.RequestSummary;
import org.eclipse.sw360.datahandler.services.vmcomponents.VMMatch;
import org.eclipse.sw360.datahandler.services.vmcomponents.VMProcessReporting;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vmcomponents")
public class VMComponentController {

    private final VMComponentHandler vmComponentHandler;

    public VMComponentController(VMComponentHandler vmComponentHandler) {
        this.vmComponentHandler = vmComponentHandler;
    }

    @GetMapping("/processes")
    public List<VMProcessReporting> getAllProcesses(
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        return vmComponentHandler.getAllProcesses(UserUtils.buildUser(email, department, userGroup));
    }

    @GetMapping("/matches")
    public List<VMMatch> getAllMatches(
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        return vmComponentHandler.getAllMatches(UserUtils.buildUser(email, department, userGroup));
    }

    /**
     * Scheduled SVM component sync entry point (formerly Thrift {@code synchronizeComponents}).
     */
    @PostMapping("/synchronize")
    public RequestSummary synchronizeComponents() {
        return vmComponentHandler.synchronizeComponents();
    }

    /**
     * Scheduled SVM reverse match entry point (formerly Thrift {@code triggerReverseMatch}).
     */
    @PostMapping("/reverse-match")
    public RequestSummary triggerReverseMatch() {
        return vmComponentHandler.triggerReverseMatch();
    }

    @PostMapping("/matches/{matchId}/accept")
    public RequestSummary acceptMatch(
            @PathVariable String matchId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        return vmComponentHandler.acceptMatch(UserUtils.buildUser(email, department, userGroup), matchId);
    }

    @PostMapping("/matches/{matchId}/decline")
    public RequestSummary declineMatch(
            @PathVariable String matchId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        return vmComponentHandler.declineMatch(UserUtils.buildUser(email, department, userGroup), matchId);
    }
}

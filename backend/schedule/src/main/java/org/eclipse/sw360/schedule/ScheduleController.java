/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.schedule;

import org.eclipse.sw360.common.utils.UserUtils;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.RequestStatusWithBoolean;
import org.eclipse.sw360.datahandler.services.common.RequestSummary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {

    private final ScheduleHandler scheduleHandler;

    public ScheduleController(ScheduleHandler scheduleHandler) {
        this.scheduleHandler = scheduleHandler;
    }

    @PostMapping("/scheduleService")
    public RequestSummary scheduleService(@RequestParam String serviceName) {
        return scheduleHandler.scheduleService(serviceName);
    }

    @PostMapping("/unscheduleService")
    public RequestStatus unscheduleService(
            @RequestParam String serviceName,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Department") String department,
            @RequestHeader("X-User-Group") String userGroup) {
        return scheduleHandler.unscheduleService(serviceName, UserUtils.buildUser(email, department, userGroup));
    }

    @PostMapping("/triggerManualService")
    public RequestStatus triggerManualService(
            @RequestParam String serviceName,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Department") String department,
            @RequestHeader("X-User-Group") String userGroup) {
        return scheduleHandler.triggerManualService(serviceName, UserUtils.buildUser(email, department, userGroup));
    }

    @PostMapping("/unscheduleAllServices")
    public RequestStatus unscheduleAllServices(
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Department") String department,
            @RequestHeader("X-User-Group") String userGroup) {
        return scheduleHandler.unscheduleAllServices(UserUtils.buildUser(email, department, userGroup));
    }

    @GetMapping("/isServiceScheduled")
    public RequestStatusWithBoolean isServiceScheduled(
            @RequestParam String serviceName,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Department") String department,
            @RequestHeader("X-User-Group") String userGroup) {
        return scheduleHandler.isServiceScheduled(serviceName, UserUtils.buildUser(email, department, userGroup));
    }

    @GetMapping("/isAnyServiceScheduled")
    public RequestStatusWithBoolean isAnyServiceScheduled(
            @RequestHeader("X-User-Email") String email,
            @RequestHeader("X-User-Department") String department,
            @RequestHeader("X-User-Group") String userGroup) {
        return scheduleHandler.isAnyServiceScheduled(UserUtils.buildUser(email, department, userGroup));
    }

    @GetMapping("/getFirstRunOffset")
    public int getFirstRunOffset(@RequestParam String serviceName) {
        return scheduleHandler.getFirstRunOffset(serviceName);
    }

    @GetMapping("/getNextSync")
    public String getNextSync(@RequestParam String serviceName) {
        return scheduleHandler.getNextSync(serviceName);
    }

    @GetMapping("/getInterval")
    public int getInterval(@RequestParam String serviceName) {
        return scheduleHandler.getInterval(serviceName);
    }
}

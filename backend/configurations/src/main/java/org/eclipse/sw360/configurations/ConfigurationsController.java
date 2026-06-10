/*
 *  Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 * 
 *  This program and the accompanying materials are made
 *  available under the terms of the Eclipse Public License 2.0
 *  which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 *  SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.configurations;

import java.util.Map;

import org.eclipse.sw360.common.utils.UserUtils;
import org.eclipse.sw360.datahandler.services.common.ConfigContainer;
import org.eclipse.sw360.datahandler.services.common.ConfigFor;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/configurations")
public class ConfigurationsController {
    
    private final SW360ConfigsHandler handler;

    public ConfigurationsController(SW360ConfigsHandler handler){
        this.handler = handler;
    }

    @PostMapping
    public RequestStatus createSW360Configs(@RequestBody ConfigContainer newConfig){
        return handler.createSW360Configs(newConfig);
    }

    @GetMapping
    public Map<String,String> getSW360Configs(){
        return handler.getSW360Configs();
    }

    @GetMapping("/{key}")
    public String getConfigByKey(@PathVariable String key){
        return handler.getConfigByKey(key);
    }

    @GetMapping("/group/{configFor}")
    public Map<String,String> getConfigForContainer(@PathVariable ConfigFor configFor){
        return handler.getConfigForContainer(configFor);
    }

    @PutMapping
    public RequestStatus updateSW360Configs(
            @RequestBody Map<String,String> updatedConfigs,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-User-Department") String department,
            @RequestHeader("X-User-Group") String userGroup) {
        return handler.updateSW360Configs(updatedConfigs, UserUtils.buildUser(userEmail, department, userGroup));
    }

    @PutMapping("/group/{configFor}")
    public RequestStatus updateSW360ConfigForContainer(
            @PathVariable ConfigFor configFor,
            @RequestBody Map<String,String> updatedConfigs,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-User-Department") String department,
            @RequestHeader("X-User-Group") String userGroup) {
        return handler.updateSW360ConfigForContainer(configFor, updatedConfigs, UserUtils.buildUser(userEmail, department, userGroup));
    }
}

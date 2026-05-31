/*
 *  Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 * 
 *  This program and the accompanying materials are made
 *  available under the terms of the Eclipse Public License 2.0
 *  which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 *  SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.eclipse.sw360.datahandler.services.health.HealthResponse;
import java.util.Set;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final HealthHandler healthHandler;

    public HealthController(HealthHandler healthHandler) {
        this.healthHandler = healthHandler;
    }

    @GetMapping
    public HealthResponse getHealth() {
        return healthHandler.getHealth();
    }

    @GetMapping("/db")
    public HealthResponse getHealthOfSpecificDbs(@RequestParam Set<String> dbsToCheck) {
        return healthHandler.getHealthOfSpecificDbs(dbsToCheck);
    }

}
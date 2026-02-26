/*
 * Copyright Siemens AG, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.licenses.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class LicenseDbSyncScheduler {

    private static final Logger log = LogManager.getLogger(LicenseDbSyncScheduler.class);

    private final LicenseDbService licenseDbService;

    public LicenseDbSyncScheduler(LicenseDbService licenseDbService) {
        this.licenseDbService = licenseDbService;
    }

    @Scheduled(cron = "${licensedb.sync.cron:0 0 2 * * ?}")
    public void scheduledSync() {
        if (!licenseDbService.isEnabled()) {
            log.debug("LicenseDB sync skipped - integration not enabled");
            return;
        }

        log.info("Starting scheduled LicenseDB sync...");
        try {
            Map<String, Object> result = licenseDbService.fullSync();
            log.info("Scheduled LicenseDB sync completed: {}", result.get("status"));
        } catch (Exception e) {
            log.error("Scheduled LicenseDB sync failed: {}", e.getMessage(), e);
        }
    }

    @Scheduled(fixedDelayString = "${licensedb.sync.fixedDelay:3600000}")
    public void scheduledHealthCheck() {
        if (!licenseDbService.isEnabled()) {
            return;
        }

        try {
            Map<String, Object> connectionStatus = licenseDbService.testConnection();
            if (Boolean.TRUE.equals(connectionStatus.get("connected"))) {
                log.debug("LicenseDB connection health check: OK");
            } else {
                log.warn("LicenseDB connection health check failed: {}", connectionStatus.get("message"));
            }
        } catch (Exception e) {
            log.warn("LicenseDB health check error: {}", e.getMessage());
        }
    }
}

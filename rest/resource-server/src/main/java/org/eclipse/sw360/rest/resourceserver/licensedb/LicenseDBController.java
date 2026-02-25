/*
 * Copyright TOSHIBA CORPORATION, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.licensedb;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/license-db")
@ConditionalOnProperty(name = "licensedb.enabled", havingValue = "true", matchIfMissing = false)
@Slf4j
@RequiredArgsConstructor
public class LicenseDBController {

    private final LicenseDBService licenseDBService;
    private final LicenseDBConfig licenseDBConfig;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = licenseDBService.healthCheck();
        return ResponseEntity.ok(health);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = Map.of(
                "enabled", licenseDBConfig.isEnabled(),
                "apiUrl", licenseDBConfig.getApiUrl() != null ? licenseDBConfig.getApiUrl() : "not configured",
                "apiVersion", licenseDBConfig.getApiVersion()
        );
        return ResponseEntity.ok(status);
    }

    @PostMapping("/sync")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, Object>> syncAll() {
        log.info("Manual license database sync triggered");
        Map<String, Object> result = licenseDBService.syncAll(null);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/sync/licenses")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, Object>> syncLicenses() {
        log.info("Manual license sync triggered");
        Map<String, Object> result = licenseDBService.syncLicenses(null);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/sync/obligations")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Map<String, Object>> syncObligations() {
        log.info("Manual obligation sync triggered");
        Map<String, Object> result = licenseDBService.syncObligations(null);
        return ResponseEntity.ok(result);
    }
}

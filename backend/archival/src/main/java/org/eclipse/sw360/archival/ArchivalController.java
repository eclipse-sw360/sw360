/*
 * Copyright Taanvi Khevaria, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.archival;

import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.services.archival.ArchivalRecord;
import org.eclipse.sw360.services.archival.ArchiveRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/archival")
@PreAuthorize("hasAuthority('ADMIN')")
public class ArchivalController {

    private final ArchivalHandler handler;

    public ArchivalController(ArchivalHandler handler) {
        this.handler = handler;
    }

    @PostMapping("/archive")
    public ResponseEntity<StreamingResponseBody> archive(@RequestBody ArchiveRequest req,
                                                         Principal principal) {
        String userEmail = principal != null ? principal.getName() : "unknown";
        String filename = "sw360_archive_" + Instant.now().toEpochMilli() + ".tar.gz";

        StreamingResponseBody body = sink -> {
            try {
                handler.archive(req, userEmail, sink);
            } catch (SW360Exception e) {
                throw new IOException("archive failed: " + e.getMessage(), e);
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/gzip"))
                .body(body);
    }

    @GetMapping("/records")
    public List<ArchivalRecord> listRecords() {
        return handler.getAll();
    }

    @GetMapping("/records/{id}")
    public ResponseEntity<ArchivalRecord> getRecord(@PathVariable String id) {
        ArchivalRecord r = handler.get(id);
        return r == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(r);
    }

    @DeleteMapping("/records/{id}")
    public ResponseEntity<Void> deleteRecord(@PathVariable String id) {
        return handler.delete(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/health")
    @PreAuthorize("permitAll()")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "archival",
                "timestamp", Instant.now().toString()
        );
    }
}

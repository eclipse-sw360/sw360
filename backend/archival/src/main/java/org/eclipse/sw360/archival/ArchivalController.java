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

import org.eclipse.sw360.common.utils.UserUtils;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.services.archival.ArchivalRecord;
import org.eclipse.sw360.datahandler.services.archival.ArchivePreview;
import org.eclipse.sw360.datahandler.services.archival.ArchiveRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/archival")
public class ArchivalController {

    private final ArchivalHandler handler;

    public ArchivalController(ArchivalHandler handler) {
        this.handler = handler;
    }

    @PostMapping("/archive")
    public ResponseEntity<StreamingResponseBody> archive(
            @RequestBody ArchiveRequest req,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        User user = requireAdmin(email, department, userGroup);
        String filename = "sw360_archive_" + Instant.now().toEpochMilli() + ".tar.gz";

        StreamingResponseBody body = sink -> {
            try {
                handler.archive(req, user, sink);
            } catch (SW360Exception e) {
                throw new IOException("archive failed: " + e.getMessage(), e);
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/gzip"))
                .body(body);
    }

    @PostMapping("/preview")
    public ResponseEntity<ArchivePreview> preview(
            @RequestBody ArchiveRequest req,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws SW360Exception {
        User user = requireAdmin(email, department, userGroup);
        return ResponseEntity.ok(handler.preview(req, user));
    }

    /**
     * Builds the acting user from the request's X-User-* headers and enforces that
     * archival is admin-only, returning 403 rather than surfacing the deeper check
     * as a 500. The provider re-checks as defence in depth.
     */
    private static User requireAdmin(String email, String department, String userGroup) {
        User user = UserUtils.buildUser(email, department, userGroup);
        if (!PermissionUtils.isAdmin(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "archival is restricted to SW360 administrators");
        }
        return user;
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
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "archival",
                "timestamp", Instant.now().toString()
        );
    }
}

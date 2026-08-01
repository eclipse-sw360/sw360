/*
 * Copyright Taanvi Khevaria, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.archival;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.eclipse.sw360.datahandler.services.archival.ArchivalRecord;
import org.eclipse.sw360.datahandler.services.archival.ArchivePreview;
import org.eclipse.sw360.datahandler.services.archival.ArchiveRequest;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@BasePathAwareController
@RequiredArgsConstructor
@RestController
@SecurityRequirement(name = "tokenAuth")
@SecurityRequirement(name = "basic")
public class ArchivalController {

    public static final String ARCHIVAL_URL = "/archival";

    @Autowired
    private Sw360ArchivalService archivalService;

    @NonNull
    private final RestControllerHelper restControllerHelper;

    @PostMapping(ARCHIVAL_URL + "/preview")
    public ResponseEntity<ArchivePreview> preview(@RequestBody ArchiveRequest request) {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        return ResponseEntity.ok(archivalService.preview(request, user));
    }

    @PostMapping(ARCHIVAL_URL + "/archive")
    public ResponseEntity<StreamingResponseBody> archive(@RequestBody ArchiveRequest request) {
        User user = restControllerHelper.getSw360UserFromAuthentication();
        InputStream bundle = archivalService.archive(request, user);
        String filename = "sw360_archive_" + Instant.now().toEpochMilli() + ".tar.gz";
        StreamingResponseBody body = out -> {
            try (InputStream in = bundle) {
                in.transferTo(out);
            }
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/gzip"))
                .body(body);
    }

    @GetMapping(ARCHIVAL_URL + "/records")
    public ResponseEntity<List<ArchivalRecord>> listRecords() {
        return ResponseEntity.ok(archivalService.listRecords());
    }

    @GetMapping(ARCHIVAL_URL + "/records/{id}")
    public ResponseEntity<ArchivalRecord> getRecord(@PathVariable String id) {
        ArchivalRecord record = archivalService.getRecord(id);
        return record == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(record);
    }

    @DeleteMapping(ARCHIVAL_URL + "/records/{id}")
    public ResponseEntity<Void> deleteRecord(@PathVariable String id) {
        archivalService.deleteRecord(id);
        return ResponseEntity.noContent().build();
    }
}

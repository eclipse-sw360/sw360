/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.spdxdocument;

import java.util.List;

import org.eclipse.sw360.common.utils.UserUtils;
import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.services.common.ModerationUpdate;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.spdx.SPDXDocument;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/spdx-documents")
public class SPDXDocumentController {

    private final SPDXDocumentHandler handler;

    public SPDXDocumentController(SPDXDocumentHandler handler) {
        this.handler = handler;
    }

    @GetMapping("/summary")
    public List<SPDXDocument> getSummary(
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        return handler.getSPDXDocumentSummary(UserUtils.buildUser(email, department, userGroup));
    }

    @GetMapping("/{id}")
    public SPDXDocument getById(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        return handler.getSPDXDocumentById(id, UserUtils.buildUser(email, department, userGroup));
    }

    @GetMapping("/{id}/edit")
    public SPDXDocument getForEdit(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        return handler.getSPDXDocumentForEdit(id, UserUtils.buildUser(email, department, userGroup));
    }

    @PostMapping
    public AddDocumentRequestSummary add(
            @RequestBody SPDXDocument spdxDocument,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        return handler.addSPDXDocument(spdxDocument, UserUtils.buildUser(email, department, userGroup));
    }

    @PutMapping
    public RequestStatus update(
            @RequestBody SPDXDocument spdxDocument,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        return handler.updateSPDXDocument(spdxDocument, UserUtils.buildUser(email, department, userGroup));
    }

    @PutMapping("/moderation")
    public RequestStatus updateFromModeration(
            @RequestBody ModerationUpdate<SPDXDocument> update,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        return handler.updateSPDXDocumentFromModerationRequest(
                update, UserUtils.buildUser(email, department, userGroup));
    }

    @DeleteMapping("/{id}")
    public RequestStatus delete(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        return handler.deleteSPDXDocument(id, UserUtils.buildUser(email, department, userGroup));
    }

    /**
     * SBOM file validation (formerly Thrift {@code isValidSbomFile}).
     */
    @PostMapping("/validate-sbom")
    public boolean validateSbom(
            @RequestBody byte[] file,
            @RequestParam String type,
            @RequestParam String extension) {
        return handler.isValidSbomFile(file, type, extension);
    }
}

/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.spdxdocumentcreationinfo;

import java.util.List;

import org.eclipse.sw360.common.utils.UserUtils;
import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.services.common.ModerationUpdate;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.spdx.DocumentCreationInformation;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/document-creation-information")
public class DocumentCreationInformationController {

    private final DocumentCreationInformationHandler handler;

    public DocumentCreationInformationController(DocumentCreationInformationHandler handler) {
        this.handler = handler;
    }

    @GetMapping("/summary")
    public List<DocumentCreationInformation> getSummary(
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        return handler.getDocumentCreationInformationSummary(
                UserUtils.buildUser(email, department, userGroup));
    }

    @GetMapping("/{id}")
    public DocumentCreationInformation getById(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        return handler.getDocumentCreationInformationById(id, UserUtils.buildUser(email, department, userGroup));
    }

    @GetMapping("/{id}/edit")
    public DocumentCreationInformation getForEdit(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        return handler.getDocumentCreationInfoForEdit(id, UserUtils.buildUser(email, department, userGroup));
    }

    @PostMapping
    public AddDocumentRequestSummary add(
            @RequestBody DocumentCreationInformation documentCreationInformation,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        return handler.addDocumentCreationInformation(
                documentCreationInformation, UserUtils.buildUser(email, department, userGroup));
    }

    @PutMapping
    public RequestStatus update(
            @RequestBody DocumentCreationInformation documentCreationInformation,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        return handler.updateDocumentCreationInformation(
                documentCreationInformation, UserUtils.buildUser(email, department, userGroup));
    }

    @PutMapping("/moderation")
    public RequestStatus updateFromModeration(
            @RequestBody ModerationUpdate<DocumentCreationInformation> update,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        return handler.updateDocumentCreationInfomationFromModerationRequest(
                update, UserUtils.buildUser(email, department, userGroup));
    }

    @DeleteMapping("/{id}")
    public RequestStatus delete(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        return handler.deleteDocumentCreationInformation(id, UserUtils.buildUser(email, department, userGroup));
    }
}

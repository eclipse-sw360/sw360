/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.wsimport;

import org.eclipse.sw360.common.utils.UserUtils;
import org.eclipse.sw360.common.utils.converter.users.UserConverter;
import org.eclipse.sw360.datahandler.services.importstatus.ImportStatus;
import org.eclipse.sw360.datahandler.services.projectimport.ImportDataRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wsimport")
public class WsImportController {

    private final WsImportHandler wsImportHandler;

    public WsImportController(WsImportHandler wsImportHandler) {
        this.wsImportHandler = wsImportHandler;
    }

    /**
     * Import WhiteSource/Mend projects by project token (formerly Thrift {@code importData}).
     */
    @PostMapping("/import")
    public ImportStatus importData(
            @RequestBody ImportDataRequest request,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        return wsImportHandler.importData(
                request.getProjectTokens(),
                UserConverter.fromThrift(UserUtils.buildUser(email, department, userGroup)),
                request.getTokenCredentials());
    }

    /**
     * External-id key used to link imported entities (formerly Thrift {@code getIdName}).
     */
    @GetMapping("/id-name")
    public String getIdName() {
        return wsImportHandler.getIdName();
    }
}

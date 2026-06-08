/*
 *  Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 * 
 *  This program and the accompanying materials are made
 *  available under the terms of the Eclipse Public License 2.0
 *  which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 *  SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.changelogs;

import java.util.List;
import java.util.Map;

import org.eclipse.sw360.datahandler.services.changelogs.ChangeLogs;
import org.eclipse.sw360.datahandler.services.common.PaginatedResult;
import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/changelogs")
public class ChangeLogsController {
    
    private final ChangeLogsHandler handler;

    public ChangeLogsController(ChangeLogsHandler handler){
        this.handler = handler;
    }

    @GetMapping("/{changeLogId}")
    public ChangeLogs getChangeLogsById(@PathVariable String changeLogId) {
        return handler.getChangeLogsById(changeLogId);
    }

    @DeleteMapping("/{docId}")
    public RequestStatus deleteChangeLogsByDocumentId(
        @PathVariable String docId,
        @RequestHeader("X-User-Email") String userEmail
    ) {
        return handler.deleteChangeLogsByDocumentId(docId, makeUser(userEmail));
    }

    @GetMapping("/doc/{docId}")
    public List<ChangeLogs> getChangeLogsByDocumentId(
    @RequestHeader("X-User-Email") String userEmail,
    @PathVariable String docId) {
        return handler.getChangeLogsByDocumentId(makeUser(userEmail), docId);
    }

    @GetMapping("/doc/{docId}/page")
    public PaginatedResult<ChangeLogs> getChangeLogsByDocumentIdPaginated(
    @RequestHeader("X-User-Email") String userEmail,
    @PathVariable String docId,
    @ModelAttribute PaginationData pageData) {
        Map<PaginationData, List<ChangeLogs>> result =
                handler.getChangeLogsByDocumentIdPaginated(makeUser(userEmail), docId, pageData);
        Map.Entry<PaginationData, List<ChangeLogs>> entry = result.entrySet().iterator().next();
        return new PaginatedResult<>(entry.getKey(), entry.getValue());
    }

    private static User makeUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setDepartment("REST");
        return user;
    }
}

/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.changelogs;

import java.util.List;

import org.eclipse.sw360.datahandler.services.changelogs.ChangeLogs;
import org.eclipse.sw360.datahandler.services.common.PaginatedResult;
import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.services.users.User;

/**
 * Client API for the changelogs backend service.
 */
public interface ChangeLogsClient {

    List<ChangeLogs> getChangeLogsByDocumentId(String docId, User user);

    PaginatedResult<ChangeLogs> getChangeLogsByDocumentIdPaginated(String docId, User user, PaginationData pageData);
}

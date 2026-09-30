/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver;

import java.util.List;
import java.util.Map;

import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.services.users.User;

/**
 * Test helpers for paginated user service responses.
 */
public final class TestUserConverters {

    private TestUserConverters() {}

    public static Map<PaginationData, List<User>> paginatedUsers(
            PaginationData paginationData, List<User> users) {
        return Map.of(paginationData, users);
    }
}

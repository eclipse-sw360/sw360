/*
 *  Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 *  This program and the accompanying materials are made
 *  available under the terms of the Eclipse Public License 2.0
 *  which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.common.utils;

import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;

/**
 * Utility for constructing Thrift {@link User} stubs from REST request headers.
 * <p>
 * Three tiers based on what the downstream handler actually needs:
 * <ul>
 *   <li><b>Tier 1 — audit only:</b> email (+ placeholder department for {@code assertUser})</li>
 *   <li><b>Tier 2 — role gate:</b> email + department + userGroup (for {@code PermissionUtils} checks)</li>
 *   <li><b>Tier 3 — full user:</b> requires a DB lookup by email — not handled here</li>
 * </ul>
 */
public final class UserUtils {

    private UserUtils() {}

    /**
     * Tier 1: Lightweight user stub for audit/logging operations.
     * Sets a placeholder department to satisfy {@code SW360Assert.assertUser}.
     */
    public static User buildUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setDepartment("REST");
        return user;
    }

    /**
     * Tier 2: User stub with role information for permission-gated operations.
     * Use when the handler calls {@code PermissionUtils.isAdmin(user)} or similar.
     */
    public static User buildUser(String email, String department, String userGroup) {
        User user = new User();
        user.setEmail(email);
        user.setDepartment(department != null && !department.isBlank() ? department : "REST");
        if (userGroup != null && !userGroup.isBlank()) {
            user.setUserGroup(UserGroup.valueOf(userGroup));
        }
        return user;
    }
}

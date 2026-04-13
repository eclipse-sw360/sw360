/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.keycloak.common;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

import static org.eclipse.sw360.keycloak.common.KeycloakConstants.ATTR_DEPARTMENT;
import static org.eclipse.sw360.keycloak.common.KeycloakConstants.ATTR_EXTERNAL_ID;
import static org.eclipse.sw360.keycloak.common.KeycloakConstants.DEFAULT_DEPARTMENT;
import static org.eclipse.sw360.keycloak.common.KeycloakConstants.DEFAULT_EXTERNAL_ID;
import static org.eclipse.sw360.keycloak.common.KeycloakConstants.DEFAULT_FIRST_NAME;
import static org.eclipse.sw360.keycloak.common.KeycloakConstants.DEFAULT_LAST_NAME;

/**
 * Utility class for mapping between Keycloak UserModels and SW360 User objects.
 *
 * @author SW360 Team
 */
public final class UserMapper {
    private static final Logger logger = LoggerFactory.getLogger(UserMapper.class);

    private UserMapper() {}

    /**
     * Populates Keycloak user attributes from an SW360 User object.
     */
    public static void mapSw360ToKeycloak(
            @Nonnull UserModel keycloakUser, @Nonnull RealmModel realm,
            @Nonnull User sw360User
    ) {
        keycloakUser.setFirstName(
                Optional.ofNullable(sw360User.getGivenname())
                        .filter(StringUtils::isNotBlank)
                        .orElseGet(() -> {
                            logger.warn("Given name is null or empty for user: {}", sw360User.getEmail());
                            return DEFAULT_FIRST_NAME;
                        })
        );

        keycloakUser.setLastName(
                Optional.ofNullable(sw360User.getLastname())
                        .filter(StringUtils::isNotBlank)
                        .orElseGet(() -> {
                            logger.warn("Last name is null or empty for user: {}", sw360User.getEmail());
                            return DEFAULT_LAST_NAME;
                        })
        );

        keycloakUser.setEmail(sw360User.getEmail());
        keycloakUser.setEmailVerified(true);
        keycloakUser.setUsername(sw360User.getEmail());

        keycloakUser.setSingleAttribute(ATTR_DEPARTMENT,
                Optional.ofNullable(sw360User.getDepartment())
                        .filter(StringUtils::isNotBlank)
                        .orElseGet(() -> {
                            logger.warn("Department is null or empty for user: {}", sw360User.getEmail());
                            return DEFAULT_DEPARTMENT;
                        })
        );

        keycloakUser.setSingleAttribute(ATTR_EXTERNAL_ID,
                Optional.ofNullable(sw360User.getExternalid())
                        .filter(StringUtils::isNotBlank)
                        .orElseGet(() -> {
                            logger.warn("External ID is null or empty for user: {}", sw360User.getEmail());
                            return DEFAULT_EXTERNAL_ID;
                        })
        );

        assignGroupToUser(keycloakUser, realm, sw360User.getUserGroup());
    }

    /**
     * Assigns the user to a group based on the external user's group.
     * <p>
     * This method checks if the external user group is valid and not empty. If it is valid, it assigns the user
     * to the corresponding group in Keycloak. If the user is already in the target group, it does nothing.
     *
     * @param user      the Keycloak user to be assigned to a group.
     * @param realm     the Keycloak realm.
     * @param userGroup the SW360 user group.
     */
    public static void assignGroupToUser(UserModel user, RealmModel realm, UserGroup userGroup) {
        if (userGroup == null || StringUtils.isBlank(userGroup.name())) {
            logger.warn("Invalid or empty group provided for user: {}", user.getEmail());
            return;
        }

        String groupName = userGroup.name();
        GroupModel targetGroup = realm.getGroupsStream()
                .filter(g -> g.getName().equals(groupName))
                .findFirst()
                .orElse(null);

        if (targetGroup == null) {
            logger.warn("Group '{}' not found in Keycloak for user: {}", groupName, user.getEmail());
            return;
        }

        // Collect current groups to avoid stream reuse
        List<GroupModel> currentGroups = user.getGroupsStream().toList();

        // Check if the user is already in the target group
        if (currentGroups.stream().anyMatch(g -> g.equals(targetGroup))) {
            logger.debug("User {} is already in group {}", user.getEmail(), groupName);
            return;
        }

        // Remove user from all other groups
        currentGroups.forEach(user::leaveGroup);

        // Add user to the target group
        user.joinGroup(targetGroup);

        logger.debug("Assigned user {} to group {}", user.getEmail(), groupName);
    }
}

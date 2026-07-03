/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.authserver.client.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.annotations.VisibleForTesting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.users.ClientMetadata;
import org.eclipse.sw360.datahandler.services.users.User;
import org.eclipse.sw360.datahandler.services.users.UserAccess;
import org.eclipse.sw360.clients.users.UsersClient;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Maintains the {@code User.oidcClientInfos} map on a SW360 user document so
 * that {@code client_credentials} tokens minted for an OAuth client can be
 * resolved back to a SW360 user by the resource server's
 * {@code Sw360JWTAccessTokenConverter} via the {@code client_id} JWT claim.
 *
 * <p>This service is only used by the {@code /client-management} controller.
 * Clients created out-of-band (e.g. the bootstrap {@code trusted-sw360-client}
 * seeded by {@code scripts/addUnsafeDefaultClient.sh}) are intentionally not
 * mirrored - those clients service interactive {@code authorization_code} /
 * {@code password} flows where the JWT carries the real human's email, so
 * the {@code oidcClientInfos} lookup is bypassed in favour of the email-claim
 * path.</p>
 */
@Service
@RequiredArgsConstructor
public class Sw360UserMirrorService {

    private static final Logger log = LogManager.getLogger(Sw360UserMirrorService.class);

    private final UsersClient usersClient;

    /**
     * Look up the SW360 user record by email, returning {@code null} when no
     * user with that primary email exists.
     */
    public User getByEmail(String email) {
        if (CommonUtils.isNullEmptyOrWhitespace(email)) {
            return null;
        }
        try {
            User user = usersClient.getByEmail(email);
            // Backend may return an empty User stub instead of null on miss;
            // treat missing email as "not found".
            if (user == null || CommonUtils.isNullEmptyOrWhitespace(user.getEmail())) {
                return null;
            }
            return user;
        } catch (Exception e) {
            log.warn("Failed to look up user by email <{}>", email, e);
            return null;
        }
    }

    @VisibleForTesting
    public @NonNull UsersClient getUsersClient() {
        return usersClient;
    }

    /**
     * Add {@code clientId} to {@code user.oidcClientInfos} with the supplied
     * {@code name} (typically the OAuth client's description) and an access
     * level derived from the client's scope set.
     *
     * <p>Idempotent: re-mirroring an existing entry overwrites the previous
     * {@link ClientMetadata}, which keeps {@code name}/{@code access} in sync
     * with the OAuth client doc on update.</p>
     */
    public boolean mirrorClient(User user, String clientId, String name, UserAccess access) {
        if (user == null || CommonUtils.isNullEmptyOrWhitespace(clientId)) {
            return false;
        }
        Map<String, ClientMetadata> infos = user.getOidcClientInfos();
        if (infos == null) {
            infos = new HashMap<>();
        }
        ClientMetadata metadata = new ClientMetadata();
        metadata.setName(CommonUtils.nullToEmptyString(name));
        metadata.setAccess(access != null ? access : UserAccess.READ);
        infos.put(clientId, metadata);
        user.setOidcClientInfos(infos);
        return updateUser(user);
    }

    /**
     * Remove {@code clientId} from {@code user.oidcClientInfos}, persisting
     * the change. Returns {@code true} when the user document was updated.
     * Returns {@code false} (without persisting) when {@code user} is
     * {@code null} or the map already has no such entry.
     */
    public boolean unmirrorClient(User user, String clientId) {
        if (user == null || CommonUtils.isNullEmptyOrWhitespace(clientId)) {
            return false;
        }
        Map<String, ClientMetadata> infos = user.getOidcClientInfos();
        if (infos == null || !infos.containsKey(clientId)) {
            return false;
        }
        infos.remove(clientId);
        user.setOidcClientInfos(infos);
        return updateUser(user);
    }

    /**
     * Derive the {@link UserAccess} for {@code oidcClientInfos} from an OAuth
     * client's scope set. {@code WRITE} (case-insensitive) anywhere in the
     * scope set yields {@link UserAccess#READ_WRITE}; otherwise
     * {@link UserAccess#READ}.
     */
    public static UserAccess accessFromScope(Set<String> scope) {
        if (scope == null) {
            return UserAccess.READ;
        }
        for (String s : scope) {
            if (s != null && "WRITE".equalsIgnoreCase(s.trim())) {
                return UserAccess.READ_WRITE;
            }
        }
        return UserAccess.READ;
    }

    private boolean updateUser(User user) {
        try {
            RequestStatus status = usersClient.updateUser(user);
            if (status != RequestStatus.SUCCESS) {
                log.warn("updateUser returned non-SUCCESS status {} for user <{}>", status, user.getEmail());
                return false;
            }
            return true;
        } catch (Exception e) {
            log.warn("Failed to update user <{}> for OAuth client mirror", user.getEmail(), e);
            return false;
        }
    }
}

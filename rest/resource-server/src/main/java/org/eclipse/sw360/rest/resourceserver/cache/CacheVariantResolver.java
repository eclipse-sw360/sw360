/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.cache;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resolves cache variant key from current user's UserGroup.
 * Extracts email from authentication and looks up UserGroup via UserService.
 */
@Component
public class CacheVariantResolver {

    private static final Logger log = LogManager.getLogger(CacheVariantResolver.class);

    private final Sw360UserService userService;

    public CacheVariantResolver(Sw360UserService userService) {
        this.userService = userService;
    }

    /**
     * Resolve variant from SecurityContext by extracting email and looking up User in UserService.
     * This ensures consistency with how UserGroup is determined throughout SW360.
     *
     * @return UserGroup name or DEFAULT_VARIANT if not found
     */
    public String resolve() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return ApiResponseCacheManager.DEFAULT_VARIANT;
        }

        String email = auth.getName();
        try {
            User user = userService.getUserByEmail(email);
            if (user != null && user.isSetUserGroup()) {
                log.debug("Resolved UserGroup '{}' for email '{}'", user.getUserGroup(), email);
                return user.getUserGroup().name();
            }
        } catch (Exception e) {
            log.warn("Failed to resolve UserGroup for {}: {}", email, e.getMessage());
        }

        return ApiResponseCacheManager.DEFAULT_VARIANT;
    }
}

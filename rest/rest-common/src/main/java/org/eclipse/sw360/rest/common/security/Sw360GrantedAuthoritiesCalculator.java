/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.common.security;

import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.services.users.User;
import org.eclipse.sw360.datahandler.services.users.UserGroup;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * Helper that derives the {@link GrantedAuthority granted authorities} of a SW360 user.
 *
 * <p>The actual mapping of SW360 user groups onto REST authorities
 * ({@code READ}/{@code WRITE}/{@code ADMIN}) is not implemented here; it is delegated to the
 * shared {@link Sw360GrantedAuthoritiesUtils} so the authorization server and resource server
 * stay in sync. This class only adapts that shared mapping to the configured write/admin
 * user groups from {@code sw360.properties}.</p>
 */
@Component
public class Sw360GrantedAuthoritiesCalculator {

    private static final Logger log = LogManager.getLogger(Sw360GrantedAuthoritiesCalculator.class);

    private static final String SW360_PROPERTIES_FILE_PATH = "/sw360.properties";
    private static final String DEFAULT_WRITE_ACCESS_USERGROUP = UserGroup.SW360_ADMIN.name();
    private static final String DEFAULT_ADMIN_ACCESS_USERGROUP = UserGroup.SW360_ADMIN.name();
    public static final UserGroup CONFIG_WRITE_ACCESS_USERGROUP;
    public static final UserGroup CONFIG_ADMIN_ACCESS_USERGROUP;

    static {
        Properties props = CommonUtils.loadProperties(Sw360GrantedAuthoritiesCalculator.class, SW360_PROPERTIES_FILE_PATH);
        CONFIG_WRITE_ACCESS_USERGROUP = UserGroup.valueOf(props.getProperty("rest.write.access.usergroup", DEFAULT_WRITE_ACCESS_USERGROUP));
        CONFIG_ADMIN_ACCESS_USERGROUP = UserGroup.valueOf(props.getProperty("rest.admin.access.usergroup", DEFAULT_ADMIN_ACCESS_USERGROUP));
    }

    /**
     * Maps the given user to REST authorities using the authorization server's configured write
     * and admin user groups.
     *
     * @param user the SW360 user to map (may be {@code null}, in which case only {@code READ} is
     *             granted)
     * @return the granted authorities derived from the user's group membership
     */
    public static List<GrantedAuthority> generateFromUser(User user) {
        String email = user != null ? user.getEmail() : "<unknown>";
        log.debug("Generating authorities for user {}", email);
        List<GrantedAuthority> grantedAuthorities = Sw360GrantedAuthoritiesUtils.generateFromUser(
                user,
                CONFIG_WRITE_ACCESS_USERGROUP,
                CONFIG_ADMIN_ACCESS_USERGROUP
        );
        log.info("Granted authorities for user {} are {}", email, grantedAuthorities);
        return grantedAuthorities;
    }
}

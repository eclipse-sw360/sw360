/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.authserver;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;

import java.util.Properties;

@SpringBootApplication
public class Sw360AuthorizationServer extends SpringBootServletInitializer {

    private static final String PROPERTIES_FILE_PATH = "/sw360.properties";
    private static final String DEFAULT_ACCESS_TIME_IN_SECONDS = "3600";
    private static final String DEFAULT_WRITE_ACCESS_USERGROUP = UserGroup.SW360_ADMIN.name();

    public static final int TOKEN_ACCESS_VALIDITY;
    public static final UserGroup WRITE_ACCESS_USERGROUP;

    static {
        Properties props = CommonUtils.loadProperties(Sw360AuthorizationServer.class, PROPERTIES_FILE_PATH);

        TOKEN_ACCESS_VALIDITY = Integer.parseInt(props.getProperty(
                "rest.token.access.validity", DEFAULT_ACCESS_TIME_IN_SECONDS));
        WRITE_ACCESS_USERGROUP = UserGroup.valueOf(props.getProperty(
                "rest.write.access.usergroup", DEFAULT_WRITE_ACCESS_USERGROUP));
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(Sw360AuthorizationServer.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(Sw360AuthorizationServer.class, args);
    }
}

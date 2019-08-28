/*
 * Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.authserver;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.rest.common.PropertyUtils;
import org.eclipse.sw360.rest.common.Sw360CORSFilter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(Sw360CORSFilter.class)
public class Sw360AuthorizationServer extends SpringBootServletInitializer {

    private static final Logger log = Logger.getLogger(Sw360AuthorizationServer.class);

    private static final String SW360_PROPERTIES_FILE_PATH = "/sw360.properties";
    private static final String DEFAULT_WRITE_ACCESS_USERGROUP = UserGroup.SW360_ADMIN.name();
    private static final String DEFAULT_ADMIN_ACCESS_USERGROUP = UserGroup.SW360_ADMIN.name();
    private static final String APPLICATION_ID = "authorization";

    public static final String CONFIG_ACCESS_TOKEN_VALIDITY_SECONDS;
    public static final UserGroup CONFIG_WRITE_ACCESS_USERGROUP;
    public static final UserGroup CONFIG_ADMIN_ACCESS_USERGROUP;

    static {
        Properties props = CommonUtils.loadProperties(Sw360AuthorizationServer.class, SW360_PROPERTIES_FILE_PATH);
        CONFIG_WRITE_ACCESS_USERGROUP = UserGroup.valueOf(props.getProperty("rest.write.access.usergroup", DEFAULT_WRITE_ACCESS_USERGROUP));
        CONFIG_ADMIN_ACCESS_USERGROUP = UserGroup.valueOf(props.getProperty("rest.admin.access.usergroup", DEFAULT_ADMIN_ACCESS_USERGROUP));
        CONFIG_ACCESS_TOKEN_VALIDITY_SECONDS = props.getProperty("rest.access.token.validity.seconds", null);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder
            .sources(Sw360AuthorizationServer.class)
            .properties(PropertyUtils.createDefaultProperties(APPLICATION_ID));
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(Sw360AuthorizationServer.class)
            .properties(PropertyUtils.createDefaultProperties(APPLICATION_ID))
            .build()
            .run(args);
    }
}

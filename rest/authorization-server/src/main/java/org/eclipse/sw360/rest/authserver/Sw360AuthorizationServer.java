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

import org.apache.log4j.Logger;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;

import javax.crypto.SealedObject;
import java.io.IOException;
import java.util.Properties;

import static org.eclipse.sw360.rest.authserver.security.Sw360SecurityEncryptor.encrypt;

@SpringBootApplication
public class Sw360AuthorizationServer extends SpringBootServletInitializer {

    private static final Logger log = Logger.getLogger(Sw360AuthorizationServer.class);

    private static final String PROPERTIES_FILE_PATH = "/sw360.properties";
    private static final String DEFAULT_WRITE_ACCESS_USERGROUP = UserGroup.SW360_ADMIN.name();

    public static final String CONFIG_ACCESS_TOKEN_VALIDITY_SECONDS;
    public static final UserGroup CONFIG_WRITE_ACCESS_USERGROUP;
    public static final String CONFIG_CLIENT_ID;
    public static final SealedObject CONFIG_CLIENT_SECRET;

    static {
        Properties props = CommonUtils.loadProperties(Sw360AuthorizationServer.class, PROPERTIES_FILE_PATH);
        CONFIG_WRITE_ACCESS_USERGROUP = UserGroup.valueOf(props.getProperty("rest.write.access.usergroup", DEFAULT_WRITE_ACCESS_USERGROUP));
        CONFIG_ACCESS_TOKEN_VALIDITY_SECONDS = props.getProperty("rest.access.token.validity.seconds", null);
        CONFIG_CLIENT_ID = props.getProperty("rest.security.client.id", null);
        CONFIG_CLIENT_SECRET = getConfigClientSecret(props);
    }

    private static SealedObject getConfigClientSecret(Properties props) {
        try {
            return encrypt(props.getProperty("rest.security.client.secret", null));
        } catch (IOException e) {
            log.error("Error occured while encrypting client password", e);
            return null;
        }
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(Sw360AuthorizationServer.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(Sw360AuthorizationServer.class, args);
    }
}

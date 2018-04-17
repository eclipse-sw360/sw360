/*
 * Copyright Siemens AG, 2013-2015.
 * With modifications by Bosch Software Innovations GmbH, 2016
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.fossology.config;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.apache.log4j.Logger.getLogger;

/**
 * Constants for the fossology host connection
 *
 * @author daniele.fognini@tngtech.com
 */
@Component
public class FossologySettings {

    private static final String PROPERTIES_FILE_PATH = "/fossology.properties";

    private static final int FOSSOLOGY_CONNECTION_TIMEOUT = 30000;
    private static final long FOSSOLOGY_EXECUTION_TIMEOUT = 100000;
    private static final String FOSSOLOGY_HOST;
    private static final int FOSSOLOGY_PORT;
    private static final String FOSSOLOGY_SSH_USERNAME;
    private static final byte[] FOSSOLOGY_PRIVATE_KEY;
    private static final byte[] FOSSOLOGY_PUBLIC_KEY;

    private static final Logger log = getLogger(FossologySettings.class);

    static {

        Properties props = CommonUtils.loadProperties(FossologySettings.class, PROPERTIES_FILE_PATH);

        FOSSOLOGY_HOST = props.getProperty("fossology.host", "localhost");
        FOSSOLOGY_PORT = Integer.parseInt(props.getProperty("fossology.port", "22"));
        FOSSOLOGY_SSH_USERNAME = props.getProperty("fossology.user", "sw360");

        final String keyFilePath = props.getProperty("fossology.key.file", "/fossology.id_rsa");
        final String pubKeyFilePath = props.getProperty("fossology.key.pub.file", keyFilePath + ".pub");

        FOSSOLOGY_PRIVATE_KEY = loadKeyFile(keyFilePath);
        FOSSOLOGY_PUBLIC_KEY = loadKeyFile(pubKeyFilePath);
    }

    private static byte[] loadKeyFile(String keyFilePath) {
        return CommonUtils.loadResource(FossologySettings.class, keyFilePath)
                .orElse(null);
    }

    public int getFossologyConnectionTimeout() {
        return FOSSOLOGY_CONNECTION_TIMEOUT;
    }

    public long getFossologyExecutionTimeout() {
        return FOSSOLOGY_EXECUTION_TIMEOUT;
    }

    public String getFossologyHost() {
        return FOSSOLOGY_HOST;
    }

    public int getFossologyPort() {
        return FOSSOLOGY_PORT;
    }

    public String getFossologySshUsername() {
        return FOSSOLOGY_SSH_USERNAME;
    }

    public byte[] getFossologyPrivateKey() {
        return FOSSOLOGY_PRIVATE_KEY;
    }

    public byte[] getFossologyPublicKey() {
        return FOSSOLOGY_PUBLIC_KEY;
    }
}

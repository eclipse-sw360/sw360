/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.common;

import java.util.Properties;

import org.eclipse.sw360.datahandler.common.CommonUtils;

public class PropertyUtils {
    private static final String SPRING_CONFIG_LOCATIION_KEY = "spring.config.location";
    
    public static Properties createDefaultProperties(String applicationName) {
        Properties properties = new Properties();

        if(System.getProperty(SPRING_CONFIG_LOCATIION_KEY) == null) {
            properties.setProperty(SPRING_CONFIG_LOCATIION_KEY,
                "file:" + CommonUtils.SYSTEM_CONFIGURATION_PATH + "/" + applicationName + "/");
        }

        return properties;
    }
}

/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

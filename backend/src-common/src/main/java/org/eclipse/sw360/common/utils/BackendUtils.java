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
package org.eclipse.sw360.common.utils;

import java.util.Properties;

import org.eclipse.sw360.datahandler.common.CommonUtils;

public class BackendUtils {

    private static final String PROPERTIES_FILE_PATH = "/sw360.properties";
    protected static final Properties loadedProperties;
    public static final Boolean MAINLINE_STATE_ENABLED_FOR_USER;

    static {
        loadedProperties = CommonUtils.loadProperties(BackendUtils.class, PROPERTIES_FILE_PATH);
        MAINLINE_STATE_ENABLED_FOR_USER = Boolean.parseBoolean(loadedProperties.getProperty("mainline.state.enabled.for.user", "true"));
    }

    protected BackendUtils() {
        // Utility class with only static functions
    }
}

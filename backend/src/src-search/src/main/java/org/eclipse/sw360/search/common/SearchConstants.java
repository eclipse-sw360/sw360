/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.search.common;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Properties;

/**
 * Properties class for the user service.
 *
 * @author cedric.bodet@tngtech.com
 */
public class SearchConstants {

    public static final String PROPERTIES_FILE_PATH = "/search.properties";
    public static final int NAME_MAX_LENGTH;

    static {
        Properties props = CommonUtils.loadProperties(SearchConstants.class, PROPERTIES_FILE_PATH);

        NAME_MAX_LENGTH = Integer.parseInt(props.getProperty("search.name.max.length", "64"));
    }

    private SearchConstants() {
        // Utility class with only static functions
    }

}

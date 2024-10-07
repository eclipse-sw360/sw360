/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.search.common;

import org.eclipse.sw360.datahandler.common.CommonUtils;

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

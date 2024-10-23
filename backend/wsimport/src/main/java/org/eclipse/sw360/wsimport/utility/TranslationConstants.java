/*
 * Copyright (c) Bosch Software Innovations GmbH 2017.
 * With modifications by Verifa Oy, 2018-2019.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.wsimport.utility;

/**
 * @author: birgit.heydenreich@tngtech.com
 * @author: ksoranko@verifa.io
 */
public class TranslationConstants {
    public static final String WS_ID = "wsId";
    public static final String UNKNOWN = "UNKNOWN";
    public static final String FILENAME = "Filename";
    public static final String POM_FILE_URL = "POM File URL";
    public static final String SCM_URL = "SCM URL";
    public static final String SUSPECTED = "Suspected";
    public static final String IMPORTED_FROM_WHITESOURCE = "Imported from Whitesource";
    public static final String APPLICATION_JSON = "application/json";
    public static final String VERSION_SUFFIX_REGEX = "$*?-SNAPSHOT|$*?.RELEASE|$*?-RELEASE|$*?RELEASE|$*?.Final";

    public static final String GET_PROJECT_VITALS = "getProjectVitals";
    public static final String GET_PROJECT_LICENSES = "getProjectLicenses";
    public static final String GET_ORGANIZATION_PROJECT_VITALS = "getOrganizationProjectVitals";

    private TranslationConstants(){
        //Utility class with only static members
    }
}

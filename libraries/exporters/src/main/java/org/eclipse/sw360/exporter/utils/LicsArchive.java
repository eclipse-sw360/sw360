/*
 * Copyright Siemens AG, 2016.
 * Copyright Bosch Software Innovations GmbH, 2018.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.exporter.utils;

import com.google.common.collect.ImmutableSet;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;

public class LicsArchive {
    public static final String TODO_FILE = "dbo.Obligations.csv";
    public static final String LICENSETYPE_FILE = "dbo.Licensetype.csv";
    public static final String LICENSE_TODO_FILE = "dbo.licenseoblig.csv";
    public static final String LICENSE_FILE = "dbo.License.csv";
    public static final String CUSTOM_PROPERTIES_FILE = "dbo.customProperties.csv";
    public static final String TODO_CUSTOM_PROPERTIES_FILE = "dbo.obligCustomProperties.csv";
    public static final Set<String> requiredLicenseFileNames = ImmutableSet.<String>builder()
            .add(TODO_FILE).add(LICENSETYPE_FILE)
            .add(LICENSE_TODO_FILE)
            .add(LICENSE_FILE).build();
    public static final Set<String> optionalLicenseFileNames = ImmutableSet.<String>builder()
            .addAll(requiredLicenseFileNames)
            .add(CUSTOM_PROPERTIES_FILE)
            .add(TODO_CUSTOM_PROPERTIES_FILE).build();

    public static boolean isValidLicenseArchive(Map<String, InputStream> inputMap) {
        if(inputMap==null) {
            return false;
        }
        boolean isValidLicenseArchive = inputMap.size() >= requiredLicenseFileNames.size();

        for (String licenseFileName : requiredLicenseFileNames) {
            isValidLicenseArchive &= inputMap.containsKey(licenseFileName);
        }

        for (String licenseFileName : inputMap.keySet()){
            isValidLicenseArchive &= optionalLicenseFileNames.contains(licenseFileName);
        }

        isValidLicenseArchive &=  ! inputMap.keySet().contains(TODO_CUSTOM_PROPERTIES_FILE)
                || inputMap.keySet().contains(CUSTOM_PROPERTIES_FILE);

        return isValidLicenseArchive;
    }
}

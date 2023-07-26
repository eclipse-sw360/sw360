/*
 * Copyright Siemens Healthineers GmBH, 2023. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.portlets.packages;

import javax.portlet.PortletRequest;

import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.portal.common.PortletUtils;

public class PackagePortletUtils {

    private PackagePortletUtils() {
        // Utility class with only static functions
    }

    public static void updatePackageFromRequest(PortletRequest request, Package pkg) {
        for (Package._Fields field : Package._Fields.values()) {
                    setFieldValue(request, pkg, field);
        }
    }

    private static void setFieldValue(PortletRequest request, Package pkg, Package._Fields field) {
        PortletUtils.setFieldValue(request, pkg, field, Package.metaDataMap.get(field), "");
    }
}

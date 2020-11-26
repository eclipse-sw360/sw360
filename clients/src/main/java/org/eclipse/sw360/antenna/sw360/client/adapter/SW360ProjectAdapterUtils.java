/*
 * Copyright (c) Bosch Software Innovations GmbH 2018.
 * Copyright (c) Bosch.IO GmbH 2020.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.antenna.sw360.client.adapter;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.projects.SW360Project;

class SW360ProjectAdapterUtils {

    private SW360ProjectAdapterUtils() {}

    public static boolean isValidProject(SW360Project project) {
        return StringUtils.isNotEmpty(project.getName()) && StringUtils.isNotEmpty(project.getVersion());
    }

    public static boolean hasEqualCoordinates(SW360Project sw360Project, String projectName, String projectVersion) {
        boolean isAppIdEqual = sw360Project.getName().equalsIgnoreCase(projectName);
        boolean isProjectVersionEqual = sw360Project.getVersion().equalsIgnoreCase(projectVersion);
        return isAppIdEqual && isProjectVersionEqual;
    }
}

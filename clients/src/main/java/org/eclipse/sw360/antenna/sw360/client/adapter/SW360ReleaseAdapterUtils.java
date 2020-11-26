/*
 * Copyright (c) Bosch Software Innovations GmbH 2019.
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
import org.eclipse.sw360.antenna.sw360.client.rest.resource.releases.SW360Release;

class SW360ReleaseAdapterUtils {

    private SW360ReleaseAdapterUtils() {
    }

    /**
     * Validates the given release before it is sent to the server. Checks
     * whether all mandatory properties are set. If validation failures are
     * detected, exceptions with corresponding messages are thrown.
     *
     * @param release the release to be validated
     * @return the validated release
     * @throws IllegalArgumentException if the release is not valid
     */
    public static SW360Release validateRelease(SW360Release release) {
        if (StringUtils.isEmpty(release.getName())) {
            throw new IllegalArgumentException("Invalid release: missing property 'name'.");
        }
        if (StringUtils.isEmpty(release.getVersion())) {
            throw new IllegalArgumentException("Invalid release: missing property 'version'.");
        }
        return release;
    }
}

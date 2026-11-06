/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.clients.adapter;

import org.eclipse.sw360.clients.rest.SW360VersionClient;
import org.eclipse.sw360.clients.rest.resource.version.VersionData;
import org.eclipse.sw360.clients.utils.SW360ClientException;

/**
 * <p>
 * Service interface for an adapter supporting access to the SW360 version
 * endpoint.
 * </p>
 */
public interface SW360VersionClientAdapter {
    SW360VersionClient getVersionClient();

    /**
     * Returns the version information reported by the SW360 server.
     *
     * @return the version information
     * @throws SW360ClientException if an error occurs
     */
    VersionData getVersion();
}

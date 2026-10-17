/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.clients.rest;

import org.eclipse.sw360.http.utils.HttpUtils;
import org.eclipse.sw360.clients.auth.AccessTokenProvider;
import org.eclipse.sw360.clients.config.SW360ClientConfig;
import org.eclipse.sw360.clients.rest.resource.version.VersionData;

import java.util.concurrent.CompletableFuture;

/**
 * <p>
 * An SW360 REST client implementation providing access to the {@code /version}
 * endpoint. It can be used as a lightweight connectivity check.
 * </p>
 */
public class SW360VersionClient extends SW360Client {
    static final String TAG_GET_VERSION = "get_version";

    private static final String VERSION_ENDPOINT = "version";

    public SW360VersionClient(SW360ClientConfig config, AccessTokenProvider provider) {
        super(config, provider);
    }

    public CompletableFuture<VersionData> getVersion() {
        return executeRequestWithoutToken(HttpUtils.get(resourceUrl(VERSION_ENDPOINT)),
                HttpUtils.jsonResult(getClientConfig().getObjectMapper(), VersionData.class), TAG_GET_VERSION);
    }
}

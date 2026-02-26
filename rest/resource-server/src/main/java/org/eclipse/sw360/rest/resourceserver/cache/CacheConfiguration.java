/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.cache;

import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;

/**
 * Configuration for a cached endpoint. Loaded from sw360.properties.
 */
@Data
@Builder
public class CacheConfiguration {
    private CachedEndpoint endpoint;
    private boolean enabled;
    private boolean perRoleCaching;
    private long ttlSeconds;
    private long maxStaleSeconds;
    private Path cacheDirectory;
}

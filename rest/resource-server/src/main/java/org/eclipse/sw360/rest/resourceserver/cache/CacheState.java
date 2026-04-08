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

/**
 * Represents the current state of a cached response file.
 */
public enum CacheState {
    /** No cache file exists. */
    EMPTY,
    /** Cache exists and is within TTL. */
    FRESH,
    /** Cache exists but past TTL, within max stale time. */
    STALE,
    /** Cache is past both TTL and max stale time. */
    EXPIRED,
    /** Cache metadata is corrupted or unreadable. */
    ERROR
}

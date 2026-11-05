/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.component.cache;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Contract for component endpoint cache-eligibility checks.
 */
public interface ComponentCachingCriteria {

    String name();

    boolean shouldCache(HttpServletRequest request);
}

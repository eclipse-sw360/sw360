/*
 * Copyright TOSHIBA CORPORATION, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.licensedb.resolution;

public interface ConflictResolver<T> {
    ConflictResult<T> resolve(T incomingData, T existingData, ResolutionStrategy strategy);
    
    boolean hasConflict(T incomingData, T existingData);
    
    T applyStrategy(T incomingData, T existingData, ResolutionStrategy strategy);
}

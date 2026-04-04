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

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ConflictResult<T> {
    private T resolvedData;
    private ResolutionStrategy strategy;
    private boolean requiresManualReview;
    private String reason;
    private List<String> conflicts;
    private boolean hasConflict;

    public ConflictResult() {
        this.conflicts = new ArrayList<>();
        this.hasConflict = false;
        this.requiresManualReview = false;
    }

    public ConflictResult(T data, ResolutionStrategy strategy) {
        this();
        this.resolvedData = data;
        this.strategy = strategy;
    }

    public void addConflict(String field) {
        this.conflicts.add(field);
        this.hasConflict = true;
    }

    public void markForManualReview(String reason) {
        this.requiresManualReview = true;
        this.reason = reason;
    }
}

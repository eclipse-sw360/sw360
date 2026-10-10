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

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ComponentCacheDecisionContext {

    private final String requestUriWithQuery;
    private boolean eligible;
    private final List<String> rejectedBy;
    @Setter
    private boolean permissionSensitive;

    public ComponentCacheDecisionContext(String requestUriWithQuery) {
        this.requestUriWithQuery = requestUriWithQuery;
        this.rejectedBy = new ArrayList<>();
    }

    public void markRejected(String criterionName) {
        this.eligible = false;
        this.rejectedBy.add(criterionName);
    }

    public void markEligible() {
        this.eligible = true;
    }

}

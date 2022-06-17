/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.common.customfields;

public enum CustomFieldPropertyKey {
    DISPLAY_TYPE("display-type"),
    HIDDEN("hidden");

    private String propertyKey;
    CustomFieldPropertyKey(String propertyKey) {
        this.propertyKey = propertyKey;
    }

    public String getKey() {
        return this.propertyKey;
    }
}

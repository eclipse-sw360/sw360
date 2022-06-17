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

public enum CustomFieldPageIdentifier {
    PROJECT("project-"),
    COMPONENT("component-"),
    RELEASE("release-");
    private String value;

    CustomFieldPageIdentifier(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public static boolean is(String key, CustomFieldPageIdentifier identifier) {
        return (key != null && key.startsWith(identifier.getValue()));
    }
}

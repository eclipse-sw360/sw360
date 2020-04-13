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

import java.util.ArrayList;

public enum CustomFieldType {
    DROPDOWN("selection-list"),
    CHECKBOX("checkbox"),
    TEXTAREA("text-box"),
    TEXTFIELD("input-field"),
    DATE("date");

    public static ArrayList<CustomFieldType> optionRequiredTypes = new ArrayList<>();
    static {
        optionRequiredTypes.add(DROPDOWN);
        optionRequiredTypes.add(CHECKBOX);
    }

    private String liferayType;
    CustomFieldType(String liferayType) {
        this.liferayType = liferayType;
    }

    public String getLiferayType() {
        return this.liferayType;
    }

    public static CustomFieldType getType(String liferayType) {
        for (CustomFieldType customFieldType : CustomFieldType.values()) {
            if (customFieldType.getLiferayType().equals(liferayType)) {
                return customFieldType;
            }
        }
        return null;
    }

    public static boolean isOptionRequiredType(CustomFieldType type) {
        return optionRequiredTypes.contains(type);
    }
}

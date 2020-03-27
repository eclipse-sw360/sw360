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

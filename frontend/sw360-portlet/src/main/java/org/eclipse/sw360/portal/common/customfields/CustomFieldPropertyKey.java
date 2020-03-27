package org.eclipse.sw360.portal.common.customfields;

public enum CustomFieldPropertyKey {
    DISPLAY_TYPE("display-type");

    private String propertyKey;
    CustomFieldPropertyKey(String propertyKey) {
        this.propertyKey = propertyKey;
    }

    public String getKey() {
        return this.propertyKey;
    }
}

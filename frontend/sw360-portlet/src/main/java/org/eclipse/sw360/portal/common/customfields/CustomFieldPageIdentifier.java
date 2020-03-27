package org.eclipse.sw360.portal.common.customfields;

public enum CustomFieldPageIdentifier {
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

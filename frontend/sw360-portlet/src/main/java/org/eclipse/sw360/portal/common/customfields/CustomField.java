package org.eclipse.sw360.portal.common.customfields;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CustomField{
    private String fieldKey;
    private String fieldLabel;
    private int fieldId;
    private CustomFieldType fieldType;
    private String value;
    private List<String> options;

    public String getFieldKey() {
        return fieldKey;
    }

    public void setFieldKey(String fieldKey) {
        this.fieldKey = fieldKey;
    }

    public String getFieldLabel() {
        return fieldLabel;
    }

    public void setFieldLabel(String fieldLabel) {
        this.fieldLabel = fieldLabel;
    }

    public int getFieldId() {
        return fieldId;
    }

    public void setFieldId(int fieldId) {
        this.fieldId = fieldId;
    }

    public CustomFieldType getFieldType() {
        return fieldType;
    }

    public void setFieldType(CustomFieldType fieldType) {
        this.fieldType = fieldType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public void addOption(String option) {
        if (this.options == null) {
            this.options = new ArrayList<>();
        }
        this.options.add(option);
    }

    public static class Comparators {
        public static Comparator<CustomField> FIELDID = Comparator.comparingInt(o -> o.fieldId);
    }
}

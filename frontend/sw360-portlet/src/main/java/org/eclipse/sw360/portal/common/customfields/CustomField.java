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
import java.util.Comparator;
import java.util.List;

public class CustomField{
    private String fieldKey;
    private String fieldLabel;
    private int fieldId;
    private CustomFieldType fieldType;
    private String fieldPattern;
    private String value;
    private List<String> options;
    private boolean isHidden;

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

    public String getFieldPattern() {
        return fieldPattern;
    }

    public void setFieldPattern(String fieldPattern) {
        this.fieldPattern = fieldPattern;
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

    public boolean isHidden() {
        return isHidden;
    }

    public void setHidden(boolean hidden) {
        isHidden = hidden;
    }

    public static class Comparators {
        public static Comparator<CustomField> FIELDID = Comparator.comparingInt(o -> o.fieldId);
    }
}

/*
 * Copyright Siemens AG, 2024. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.nouveau.designdocument;

import com.google.gson.annotations.SerializedName;

import java.util.Map;
import java.util.Objects;

/**
 * Represents the index function for a nouveau index. Provide `default_analyzer`
 * and `field_analyzers` to specify the analyzers for the fields.
 */
public class NouveauIndexFunction {
    @SerializedName("default_analyzer")
    private String defaultAnalyzer;
    @SerializedName("field_analyzers")
    private Map<String, String> fieldAnalyzer;
    @SerializedName("index")
    private final String index;

    public NouveauIndexFunction(String index) {
        if (index == null || index.isEmpty()) {
            throw new IllegalArgumentException("Index cannot cannot be empty!");
        }
        this.index = index;
    }

    public String getDefaultAnalyzer() {
        return defaultAnalyzer;
    }

    public NouveauIndexFunction setDefaultAnalyzer(String defaultAnalyzer) {
        this.defaultAnalyzer = defaultAnalyzer;
        return this;
    }

    public Map<String, String> getFieldAnalyzer() {
        return fieldAnalyzer;
    }

    public NouveauIndexFunction setFieldAnalyzer(Map<String, String> fieldAnalyzer) {
        this.fieldAnalyzer = fieldAnalyzer;
        return this;
    }

    public String getIndex() {
        return index;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NouveauIndexFunction that = (NouveauIndexFunction) o;
        return Objects.equals(defaultAnalyzer, that.defaultAnalyzer) && Objects.equals(fieldAnalyzer, that.fieldAnalyzer) && Objects.equals(index, that.index);
    }

    @Override
    public int hashCode() {
        return Objects.hash(defaultAnalyzer, fieldAnalyzer, index);
    }
}

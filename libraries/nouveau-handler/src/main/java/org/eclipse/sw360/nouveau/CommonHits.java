/*
 * Copyright Siemens AG, 2024. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.nouveau;

import com.google.gson.annotations.SerializedName;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Represents the Hits of a Lucene query.
 */
class CommonHits {
    @SerializedName("order")
    protected List<LinkedHashMap<String, Object>> order;

    @SerializedName("id")
    protected String id;

    @SerializedName("fields")
    protected LinkedHashMap<String, Object> fields;

    public List<LinkedHashMap<String, Object>> getOrder() {
        return order;
    }

    public String getId() {
        return id;
    }

    public LinkedHashMap<String, Object> getFields() {
        return fields;
    }

    /**
     * Returns the score of the hit which is typically the only float value in
     * the order list.
     * @return Score of the hit (higher is better)
     */
    public double getScore() {
        double score = 0.0;
        for (LinkedHashMap<String, Object> order : this.getOrder()) {
            if (order.get("@type").equals("float")) {
                score = Double.parseDouble(String.valueOf(order.get("value")));
                break;
            }
        }
        return score;
    }
}

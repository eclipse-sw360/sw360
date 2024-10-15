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

import junit.framework.TestCase;

import java.util.LinkedHashMap;
import java.util.List;

public class CommonHitsTest extends TestCase {

    public void testGetScoreSet() {
        CommonHits commonHits = new CommonHits();
        LinkedHashMap<String, Object> order = new LinkedHashMap<>();
        order.put("@type", "float");
        order.put("value", 1);
        LinkedHashMap<String, Object> id = new LinkedHashMap<>();
        id.put("@type", "string");
        id.put("value", "deadbeef");
        commonHits.order = List.of(id, order);
        assert commonHits.getScore() == 1.0;
    }

    public void testGetScoreUnset() {
        CommonHits commonHits = new CommonHits();
        LinkedHashMap<String, Object> id = new LinkedHashMap<>();
        id.put("@type", "string");
        id.put("value", "deadbeef");
        commonHits.order = List.of(id);
        assert commonHits.getScore() == 0.0;
    }
}

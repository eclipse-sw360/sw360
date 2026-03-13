/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ReleaseSearchHandlerTest {

    @Test
    public void should_sort_versions_naturally_for_numeric_segments() {
        List<String> versions = new ArrayList<>(Arrays.asList("1.10", "1.2", "1.0", "2.0", "1.9"));

        versions.sort(Comparator.comparing(ReleaseSearchHandler::normalizeVersionForSort));

        assertEquals(Arrays.asList("1.0", "1.2", "1.9", "1.10", "2.0"), versions);
    }

    @Test
    public void should_treat_leading_zero_numeric_segments_as_equal_value() {
        String v1 = ReleaseSearchHandler.normalizeVersionForSort("1.02");
        String v2 = ReleaseSearchHandler.normalizeVersionForSort("1.2");

        assertEquals(v1, v2);
    }

    @Test
    public void should_sort_numeric_suffixes_naturally() {
        String alpha2 = ReleaseSearchHandler.normalizeVersionForSort("1.0.0-alpha2");
        String alpha10 = ReleaseSearchHandler.normalizeVersionForSort("1.0.0-alpha10");

        assertTrue(alpha2.compareTo(alpha10) < 0);
    }

    @Test
    public void should_pad_numeric_length_prefix_to_six_digits() {
        String normalized = ReleaseSearchHandler.normalizeVersionForSort("v123");

        assertEquals("v{000003123}", normalized);
    }
}
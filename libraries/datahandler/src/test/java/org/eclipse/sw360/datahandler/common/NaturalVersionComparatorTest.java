/*
 * Copyright Himanshu Gupta, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.datahandler.common;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NaturalVersionComparatorTest {

    private final NaturalVersionComparator comparator = NaturalVersionComparator.INSTANCE;

    @Test
    public void testNumericSegmentsComparedAsNumbers() {
        assertTrue("15.0 should be > 2.0", comparator.compare("15.0", "2.0") > 0);
        assertTrue("2.0 should be < 15.0", comparator.compare("2.0", "15.0") < 0);
    }

    @Test
    public void testSimpleVersionOrdering() {
        assertTrue(comparator.compare("1.0", "1.1") < 0);
        assertTrue(comparator.compare("1.1", "2.0") < 0);
        assertTrue(comparator.compare("1.9", "1.10") < 0);
    }

    @Test
    public void testEqualVersions() {
        assertEquals(0, comparator.compare("1.0", "1.0"));
        assertEquals(0, comparator.compare("2.1.3", "2.1.3"));
    }

    @Test
    public void testVersionWithSuffixComesAfterBase() {
        assertTrue("1.1 should be < 1.1 SR1", comparator.compare("1.1", "1.1 SR1") < 0);
        assertTrue("1.1 SR1 should be < 2.0", comparator.compare("1.1 SR1", "2.0") < 0);
    }

    @Test
    public void testSortingFullList() {
        List<String> versions = Arrays.asList("2.0", "1.1 SR1", "1.0", "1.1");
        versions.sort(comparator);
        assertEquals(Arrays.asList("1.0", "1.1", "1.1 SR1", "2.0"), versions);
    }

    @Test
    public void testThreePartVersions() {
        List<String> versions = Arrays.asList("2.1.0", "10.0.0", "2.0.1", "2.0.0");
        versions.sort(comparator);
        assertEquals(Arrays.asList("2.0.0", "2.0.1", "2.1.0", "10.0.0"), versions);
    }

    @Test
    public void testVersionsWithLeadingZeros() {
        assertEquals(0, comparator.compare("01.02", "1.2"));
        assertTrue(comparator.compare("01.02", "1.10") < 0);
    }

    @Test
    public void testNullSafeComparator() {
        assertTrue(NaturalVersionComparator.NULLS_FIRST_INSTANCE.compare(null, "1.0") < 0);
        assertTrue(NaturalVersionComparator.NULLS_FIRST_INSTANCE.compare("1.0", null) > 0);
        assertEquals(0, NaturalVersionComparator.NULLS_FIRST_INSTANCE.compare(null, null));
    }

    @Test
    public void testIssue206LargeVersionNumbers() {
        List<String> versions = Arrays.asList("15.0", "2.0", "1.0", "10.0", "3.0");
        versions.sort(comparator);
        assertEquals(Arrays.asList("1.0", "2.0", "3.0", "10.0", "15.0"), versions);
    }
}

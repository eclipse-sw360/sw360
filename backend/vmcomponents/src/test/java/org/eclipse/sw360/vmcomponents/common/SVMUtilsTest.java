/*
 * Copyright Siemens AG, 2026.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.vmcomponents.common;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for SVMUtils, particularly the delta sync date calculation.
 *
 * <p>Note: the production code performs date arithmetic in the JVM's local timezone and emits the
 * final timestamp in UTC. The tests therefore compute their expected values using the same logic
 * so they are stable across CI machines regardless of system zone.
 */
public class SVMUtilsTest {

    private static final DateTimeFormatter SVM_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /** Computes the same UTC output the production code would produce, for a given local input. */
    private static String expectedUtc(String localInput, int offsetDays) throws Exception {
        SimpleDateFormat sw360Format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date d = sw360Format.parse(localInput);
        ZoneId localZone = ZoneId.systemDefault();
        LocalDateTime local = LocalDateTime.ofInstant(d.toInstant(), localZone);
        LocalDateTime shifted = local.minusDays(offsetDays);
        ZonedDateTime utc = shifted.atZone(localZone).withZoneSameInstant(ZoneOffset.UTC);
        return utc.format(SVM_FORMAT);
    }

    /**
     * Result must be formatted as SVM-compatible naive UTC timestamp {@code YYYY-MM-DDTHH:MM:SS}.
     */
    @Test
    public void calculateModifiedAfter_withValidDate_returnsCorrectSVMFormat() {
        String result = SVMUtils.calculateModifiedAfter("2022-05-01 04:05:00", 1);
        assertNotNull(result);
        assertTrue("Result should match SVM date format: " + result,
                result.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}"));
    }

    /**
     * Subtracting N days from a local timestamp and converting to UTC must match the same
     * arithmetic done with java.time APIs (zone-independent equivalence).
     */
    @Test
    public void calculateModifiedAfter_withOneDay_matchesUtcEquivalent() throws Exception {
        String localInput = "2022-05-05 12:00:00";
        String result = SVMUtils.calculateModifiedAfter(localInput, 1);
        assertEquals(expectedUtc(localInput, 1), result);
    }

    /**
     * Negative offset must add days (defensive verification of arithmetic direction).
     */
    @Test
    public void calculateModifiedAfter_withNegativeOffset_addsDays() throws Exception {
        String localInput = "2022-05-01 12:00:00";
        String result = SVMUtils.calculateModifiedAfter(localInput, -1);
        assertNotNull(result);
        assertEquals(expectedUtc(localInput, -1), result);
    }

    @Test
    public void calculateModifiedAfter_withNullInput_returnsNull() {
        assertNull(SVMUtils.calculateModifiedAfter(null, 1));
    }

    @Test
    public void calculateModifiedAfter_withEmptyInput_returnsNull() {
        assertNull(SVMUtils.calculateModifiedAfter("", 1));
    }

    @Test
    public void calculateModifiedAfter_withInvalidDateFormat_returnsNull() {
        // wrong separators
        assertNull(SVMUtils.calculateModifiedAfter("2022/05/01 04:05:00", 1));
    }

    /** SVM delta endpoint expects UTC; verify the output equals the same instant in UTC. */
    @Test
    public void calculateModifiedAfter_outputIsUtc() throws Exception {
        String localInput = "2022-05-05 12:00:00";
        String result = SVMUtils.calculateModifiedAfter(localInput, 0);
        assertNotNull(result);

        // Re-parse result as naive UTC and compare instant with local-zoned input.
        ZonedDateTime resultAsUtc = LocalDateTime.parse(result, SVM_FORMAT).atZone(ZoneOffset.UTC);
        SimpleDateFormat sw360Format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        ZonedDateTime inputAsLocal = LocalDateTime.ofInstant(
                sw360Format.parse(localInput).toInstant(), ZoneId.systemDefault())
                .atZone(ZoneId.systemDefault());
        assertEquals("Result must represent the same instant as input (offset 0)",
                inputAsLocal.toInstant(), resultAsUtc.toInstant());
    }
}

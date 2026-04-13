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

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NaturalVersionComparator implements Comparator<String> {

    public static final NaturalVersionComparator INSTANCE = new NaturalVersionComparator();

    public static final Comparator<String> NULLS_FIRST_INSTANCE =
            Comparator.nullsFirst(INSTANCE);

    private static final Pattern SEGMENT_PATTERN = Pattern.compile("(\\d+)|(\\D+)");

    private NaturalVersionComparator() {
    }

    @Override
    public int compare(String v1, String v2) {
        Matcher m1 = SEGMENT_PATTERN.matcher(v1);
        Matcher m2 = SEGMENT_PATTERN.matcher(v2);

        while (m1.find() && m2.find()) {
            String seg1 = m1.group();
            String seg2 = m2.group();

            int cmp;
            if (m1.group(1) != null && m2.group(1) != null) {
                cmp = compareLong(seg1, seg2);
            } else {
                cmp = seg1.compareToIgnoreCase(seg2);
            }

            if (cmp != 0) {
                return cmp;
            }
        }

        if (m1.find()) {
            return 1;
        }
        if (m2.find()) {
            return -1;
        }
        return 0;
    }

    private static int compareLong(String n1, String n2) {
        String stripped1 = n1.replaceFirst("^0+", "");
        String stripped2 = n2.replaceFirst("^0+", "");

        if (stripped1.length() != stripped2.length()) {
            return Integer.compare(stripped1.length(), stripped2.length());
        }
        return stripped1.compareTo(stripped2);
    }
}

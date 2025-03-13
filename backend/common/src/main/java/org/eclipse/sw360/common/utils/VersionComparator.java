/*
 * Copyright Ankush1oo8, 2025.
 * Copyright NEXT Ankush1oo8, 2025.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 */
package org.eclipse.sw360.common.utils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionComparator implements Comparator<String> {
    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+|[a-zA-Z]+)");
    private static final Pattern EPOCH_PATTERN = Pattern.compile("^(\\d+):(.+)$");
    private static final Pattern BUILD_METADATA_PATTERN = Pattern.compile("^(.+?)\\+.*$");

    private static final List<String> DEFAULT_PRE_RELEASE_ORDER = Arrays.asList(
        "alpha", "beta", "rc", "snapshot", "pre", "milestone", "preview", "dev"
    );

    private static final Map<String, List<String>> VERSION_CACHE = new ConcurrentHashMap<>();
    private final List<String> preReleaseOrder;

    public VersionComparator() {
        this.preReleaseOrder = DEFAULT_PRE_RELEASE_ORDER;
    }

    public VersionComparator(List<String> customPreReleaseOrder) {
        this.preReleaseOrder = customPreReleaseOrder;
    }

    @Override
    public int compare(String v1, String v2) {
        if (v1 == null || v2 == null) return (v1 == null) ? ((v2 == null) ? 0 : -1) : 1;

        int epoch1 = extractEpoch(v1);
        int epoch2 = extractEpoch(v2);

        if (epoch1 != epoch2) return Integer.compare(epoch1, epoch2);

        v1 = stripEpochAndBuildMetadata(v1);
        v2 = stripEpochAndBuildMetadata(v2);

        List<String> parts1 = getCachedVersionParts(v1);
        List<String> parts2 = getCachedVersionParts(v2);

        int length = Math.max(parts1.size(), parts2.size());
        for (int i = 0; i < length; i++) {
            String part1 = (i < parts1.size()) ? parts1.get(i) : "";
            String part2 = (i < parts2.size()) ? parts2.get(i) : "";

            int result = compareParts(part1, part2);
            if (result != 0) return result;
        }
        return 0;
    }

    private int extractEpoch(String version) {
        Matcher matcher = EPOCH_PATTERN.matcher(version);
        return matcher.matches() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    private String stripEpochAndBuildMetadata(String version) {
        version = version.replaceFirst(EPOCH_PATTERN.pattern(), "$2");
        Matcher matcher = BUILD_METADATA_PATTERN.matcher(version);
        return matcher.matches() ? matcher.group(1) : version;
    }

    private List<String> getCachedVersionParts(String version) {
        return VERSION_CACHE.computeIfAbsent(version, this::splitVersion);
    }

    private List<String> splitVersion(String version) {
        Matcher matcher = VERSION_PATTERN.matcher(version);
        List<String> parts = new ArrayList<>();
        while (matcher.find()) {
            parts.add(matcher.group());
        }
        return parts;
    }

    private int compareParts(String part1, String part2) {
        boolean isNumeric1 = part1.chars().allMatch(Character::isDigit);
        boolean isNumeric2 = part2.chars().allMatch(Character::isDigit);

        if (isNumeric1 && isNumeric2) {
            return Long.compare(Long.parseLong(part1), Long.parseLong(part2));
        }

        if (!isNumeric1 && !isNumeric2) {
            return comparePreRelease(part1, part2);
        }

        return isNumeric1 ? -1 : 1;
    }

    private int comparePreRelease(String part1, String part2) {
        if (Objects.equals(part1, part2)) return 0;
        if (part1.isEmpty()) return 1;
        if (part2.isEmpty()) return -1;

        int index1 = preReleaseOrder.indexOf(part1.toLowerCase());
        int index2 = preReleaseOrder.indexOf(part2.toLowerCase());

        if (index1 == -1 && index2 == -1) return part1.compareTo(part2);
        if (index1 == -1) return 1;
        if (index2 == -1) return -1;

        return Integer.compare(index1, index2);
    }
}

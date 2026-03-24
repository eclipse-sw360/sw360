/*
 * SPDX-FileCopyrightText: 2026 Eclipse SW360 Contributors
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.migration.poc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Migrated enum — plain Java enum replacing the Thrift-generated UserGroup TEnum.
 *
 * <p>Before: {@code public enum UserGroup implements TEnum { USER(0), ADMIN(1), ... }}
 * <p>After: standard Java enum with Jackson annotations for JSON round-trip.
 */
public enum UserGroup {
    USER(0),
    ADMIN(1),
    CLEARING_ADMIN(2),
    ECC_ADMIN(3),
    SECURITY_ADMIN(4),
    SW360_ADMIN(5);

    private final int value;

    UserGroup(int value) {
        this.value = value;
    }

    @JsonValue
    public int getValue() {
        return value;
    }

    @JsonCreator
    public static UserGroup findByValue(int value) {
        for (UserGroup g : values()) {
            if (g.value == value) return g;
        }
        throw new IllegalArgumentException("Unknown UserGroup value: " + value);
    }
}

/*
 * SPDX-FileCopyrightText: 2026 Eclipse SW360 Contributors
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.migration.poc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Migrated enum — plain Java enum replacing the Thrift-generated RequestStatus TEnum.
 *
 * <p>Before: {@code public enum RequestStatus implements TEnum { SUCCESS(0), ... }}
 * <p>After: plain Java enum. Values match the Thrift ordinals exactly so existing
 * JSON serialized data (stored in CouchDB) deserializes without any migration.
 */
public enum RequestStatus {
    SUCCESS(0),
    SENT_TO_MODERATOR(1),
    FAILURE(2),
    ACCESS_DENIED(3),
    IN_USE(4),
    CLOSED_UPDATE_NOT_SUCCESSFUL(5);

    private final int value;

    RequestStatus(int value) {
        this.value = value;
    }

    @JsonValue
    public int getValue() {
        return value;
    }

    @JsonCreator
    public static RequestStatus findByValue(int value) {
        for (RequestStatus s : values()) {
            if (s.value == value) return s;
        }
        throw new IllegalArgumentException("Unknown RequestStatus value: " + value);
    }
}

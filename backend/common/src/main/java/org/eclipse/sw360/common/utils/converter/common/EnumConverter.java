/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.common.utils.converter.common;

/**
 * Shared enum conversion for Thrift and service-api enums with matching constant names.
 */
public final class EnumConverter {

    private EnumConverter() {}

    public static <P extends Enum<P>> P fromThrift(Enum<?> thrift, Class<P> pojoEnumClass) {
        if (thrift == null) {
            return null;
        }
        return Enum.valueOf(pojoEnumClass, thrift.name());
    }

    public static <T extends Enum<T>> T toThrift(Enum<?> pojo, Class<T> thriftEnumClass) {
        if (pojo == null) {
            return null;
        }
        return Enum.valueOf(thriftEnumClass, pojo.name());
    }
}

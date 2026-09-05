/*
 * Copyright Siemens AG, 2014-2017, 2019. Part of the SW360 Portal Project.
 * With modifications by Bosch Software Innovations GmbH, 2016.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.common;

import org.apache.thrift.TEnum;

/**
 * Thrift-facing shim over {@link EnumDisplayNames}.
 *
 * <p>The label tables moved to {@link EnumDisplayNames}, keyed by the service-api enums. Thrift and
 * service-api enums share their constant names — that invariant is what {@code EnumConverter} relies
 * on — so a thrift value is resolved to its service-api twin by name and the lookup is delegated.
 * Nothing is duplicated, so the two cannot drift.
 *
 * <p>This class exists only for callers that still hold thrift enums and is deleted with thrift.
 * New code should call {@link EnumDisplayNames} directly.
 */
public class ThriftEnumUtils {

    private ThriftEnumUtils() {
    }

    public static String enumToString(TEnum value) {
        return EnumDisplayNames.toDisplayString(toServiceApi(value));
    }

    public static String enumToShortString(TEnum value) {
        return EnumDisplayNames.toShortString(toServiceApi(value));
    }

    public static <T extends Enum<T>> T stringToEnum(String in, Class<T> clazz) {
        return EnumDisplayNames.byName(in, clazz);
    }

    public static <T extends Enum<T>> T enumByString(String in, Class<T> clazz) {
        return byLabel(in, clazz, false);
    }

    public static <T extends Enum<T>> T enumByShortString(String in, Class<T> clazz) {
        return byLabel(in, clazz, true);
    }

    /**
     * Maps a thrift enum constant onto the service-api constant of the same name, so the labels in
     * {@link EnumDisplayNames} can be reused without a second copy.
     *
     * @return null when there is no service-api twin, which makes the caller fall back to the
     *         constant name rather than fail.
     */
    private static Enum<?> toServiceApi(TEnum value) {
        if (!(value instanceof Enum<?> thriftConstant)) {
            return null;
        }
        Class<?> twin = serviceApiTwin(value.getClass());
        if (twin == null) {
            return thriftConstant;
        }
        for (Object candidate : twin.getEnumConstants()) {
            if (((Enum<?>) candidate).name().equals(thriftConstant.name())) {
                return (Enum<?>) candidate;
            }
        }
        return thriftConstant;
    }

    /**
     * Resolves a thrift enum class to its service-api counterpart by simple name. The service-api
     * packages are searched because thrift keeps its enums in a parallel package tree.
     */
    private static Class<?> serviceApiTwin(Class<?> thriftType) {
        for (String pkg : SERVICE_API_PACKAGES) {
            try {
                Class<?> candidate = Class.forName(pkg + "." + thriftType.getSimpleName());
                if (candidate.isEnum()) {
                    return candidate;
                }
            } catch (ClassNotFoundException expected) {
                // try the next package
            }
        }
        return null;
    }

    private static final String[] SERVICE_API_PACKAGES = {
            "org.eclipse.sw360.datahandler.services.common",
            "org.eclipse.sw360.datahandler.services.components",
            "org.eclipse.sw360.datahandler.services.attachments",
            "org.eclipse.sw360.datahandler.services.projects",
            "org.eclipse.sw360.datahandler.services.licenses",
            "org.eclipse.sw360.datahandler.services.users",
            "org.eclipse.sw360.datahandler.services.moderation",
            "org.eclipse.sw360.datahandler.services.packages",
            "org.eclipse.sw360.datahandler.services.vulnerabilities",
    };

    @SuppressWarnings("unchecked")
    private static <T extends Enum<T>> T byLabel(String label, Class<T> thriftType, boolean shortLabel) {
        Class<?> twin = serviceApiTwin(thriftType);
        if (twin == null) {
            return null;
        }
        Enum<?> match = shortLabel
                ? EnumDisplayNames.byShortString(label, (Class) twin)
                : EnumDisplayNames.byDisplayString(label, (Class) twin);
        return match == null ? null : EnumDisplayNames.byName(match.name(), thriftType);
    }
}

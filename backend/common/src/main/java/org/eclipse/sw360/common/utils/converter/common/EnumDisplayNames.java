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

import java.util.Map;

import org.apache.thrift.TEnum;
import org.eclipse.sw360.datahandler.common.ThriftEnumUtils;

/**
 * Human readable labels for service-api enums, e.g. {@code UNDER_CLEARING} → "Under clearing".
 *
 * <p>The labels themselves stay in {@link ThriftEnumUtils} so there is a single source of truth
 * shared with the still-thrift call sites. Service-api enums mirror the thrift constant names
 * (that invariant is what {@code EnumConverter} relies on), so the label can be resolved by name
 * without converting the surrounding document.
 *
 * <p>Do not replace a call to this class with {@code value.name()}: the raw constant name leaks
 * into user facing output such as the project clearing status report and the Excel/CSV exports.
 */
public final class EnumDisplayNames {

    private static final Map<Class<? extends Enum<?>>, Class<? extends TEnum>> POJO_TO_THRIFT = Map.of(
            org.eclipse.sw360.datahandler.services.common.MainlineState.class,
            org.eclipse.sw360.datahandler.thrift.MainlineState.class,
            org.eclipse.sw360.datahandler.services.common.ReleaseRelationship.class,
            org.eclipse.sw360.datahandler.thrift.ReleaseRelationship.class,
            org.eclipse.sw360.datahandler.services.components.ClearingState.class,
            org.eclipse.sw360.datahandler.thrift.components.ClearingState.class,
            org.eclipse.sw360.datahandler.services.components.ComponentType.class,
            org.eclipse.sw360.datahandler.thrift.components.ComponentType.class,
            org.eclipse.sw360.datahandler.services.projects.ProjectClearingState.class,
            org.eclipse.sw360.datahandler.thrift.projects.ProjectClearingState.class,
            org.eclipse.sw360.datahandler.services.projects.ProjectRelationship.class,
            org.eclipse.sw360.datahandler.thrift.projects.ProjectRelationship.class,
            org.eclipse.sw360.datahandler.services.projects.ProjectState.class,
            org.eclipse.sw360.datahandler.thrift.projects.ProjectState.class,
            org.eclipse.sw360.datahandler.services.projects.ProjectType.class,
            org.eclipse.sw360.datahandler.thrift.projects.ProjectType.class);

    private EnumDisplayNames() {}

    /**
     * @return the display label for {@code value}, an empty string when {@code value} is null, or
     *         the raw constant name when the enum type has no registered label map.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static String toDisplayString(Enum<?> value) {
        if (value == null) {
            return "";
        }
        Class<? extends TEnum> thriftType = POJO_TO_THRIFT.get(value.getClass());
        if (thriftType == null) {
            return value.name();
        }
        return ThriftEnumUtils.enumToString((TEnum) Enum.valueOf((Class) thriftType, value.name()));
    }
}

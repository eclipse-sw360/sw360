/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.exporter;

import org.eclipse.sw360.datahandler.services.common.FieldAccessor;
import org.eclipse.sw360.datahandler.services.common.PojoFields;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Spreadsheet columns follow POJO declaration order, the same order Thrift
 * {@code metaDataMap} used to provide.
 */
final class ExporterFields {

    private ExporterFields() {
    }

    static <T> List<FieldAccessor<T>> except(Class<T> type, String... ignored) {
        Set<String> skip = Set.of(ignored);
        return PojoFields.of(type).stream()
                .filter(field -> !skip.contains(field.name()))
                .toList();
    }

    static <T> List<FieldAccessor<T>> only(Class<T> type, String... names) {
        Set<String> keep = Set.of(names);
        return PojoFields.of(type).stream()
                .filter(field -> keep.contains(field.name()))
                .toList();
    }

    /**
     * Fields in the given name order. Vendor export lists fullname before shortname, which is
     * the opposite of declaration order, so filtering {@link PojoFields#of} would swap columns.
     */
    static <T> List<FieldAccessor<T>> inOrder(Class<T> type, String... names) {
        List<FieldAccessor<T>> ordered = new ArrayList<>(names.length);
        for (String name : names) {
            FieldAccessor<T> field = PojoFields.byName(type, name);
            if (field == null) {
                throw new IllegalArgumentException(type.getSimpleName() + " has no field " + name);
            }
            ordered.add(field);
        }
        return List.copyOf(ordered);
    }
}

/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.services.common;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Field accessors for service-api POJOs, replacing the generated Thrift {@code <Struct>._Fields}
 * enum and {@code metaDataMap}.
 *
 * <p>Thrift's equivalents map across as:
 * <table>
 *   <tr><td>{@code Release._Fields.values()}</td><td>{@code PojoFields.of(Release.class)}</td></tr>
 *   <tr><td>{@code Release.metaDataMap.keySet()}</td><td>{@code PojoFields.of(Release.class)}</td></tr>
 *   <tr><td>{@code Release._Fields.findByName(n)}</td><td>{@code PojoFields.byName(Release.class, n)}</td></tr>
 * </table>
 *
 * <p><b>Ordering.</b> {@link #of} returns fields in the POJO's declaration order, which mirrors the
 * Thrift field order the POJOs were written from. Exporters derive spreadsheet column order from
 * this, so reordering a POJO's fields reorders exported columns —
 * {@code PojoFieldsOrderParityTest} pins the order against the Thrift {@code metaDataMap} to catch
 * that.
 *
 * <p>Static and synthetic fields are excluded, so instrumentation artefacts such as JaCoCo's
 * {@code $jacocoData} never surface as document fields.
 */
public final class PojoFields {

    private static final Map<Class<?>, List<FieldAccessor<?>>> ACCESSORS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Map<String, FieldAccessor<?>>> BY_NAME =
            new ConcurrentHashMap<>();

    private PojoFields() {
    }

    /**
     * Every readable/writable field of {@code type}, in declaration order.
     *
     * <p>The returned list is immutable and cached; callers may iterate it repeatedly without cost.
     */
    @SuppressWarnings("unchecked")
    public static <T> List<FieldAccessor<T>> of(Class<T> type) {
        List<FieldAccessor<?>> cached = ACCESSORS.computeIfAbsent(type, PojoFields::introspect);
        return (List<FieldAccessor<T>>) (List<?>) cached;
    }

    /**
     * The accessor for one field, or {@code null} when {@code type} has no such field — matching
     * {@code _Fields.findByName}, which also returned null rather than throwing.
     */
    @SuppressWarnings("unchecked")
    public static <T> FieldAccessor<T> byName(Class<T> type, String name) {
        Map<String, FieldAccessor<?>> index = BY_NAME.computeIfAbsent(type, key -> {
            Map<String, FieldAccessor<?>> map = new LinkedHashMap<>();
            for (FieldAccessor<?> accessor : of(key)) {
                map.put(accessor.name(), accessor);
            }
            return Collections.unmodifiableMap(map);
        });
        return (FieldAccessor<T>) index.get(name);
    }

    /** The field names of {@code type}, in declaration order. */
    public static List<String> names(Class<?> type) {
        List<String> names = new ArrayList<>();
        for (FieldAccessor<?> accessor : of(type)) {
            names.add(accessor.name());
        }
        return Collections.unmodifiableList(names);
    }

    private static List<FieldAccessor<?>> introspect(Class<?> type) {
        List<FieldAccessor<?>> accessors = new ArrayList<>();
        for (Field field : type.getDeclaredFields()) {
            if (field.isSynthetic() || Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            field.setAccessible(true);
            accessors.add(new ReflectiveAccessor<>(field));
        }
        return Collections.unmodifiableList(accessors);
    }

    private static final class ReflectiveAccessor<T> implements FieldAccessor<T> {

        private final Field field;

        private ReflectiveAccessor(Field field) {
            this.field = field;
        }

        @Override
        public String name() {
            return field.getName();
        }

        @Override
        public Class<?> type() {
            return field.getType();
        }

        @Override
        public Object get(T target) {
            try {
                return field.get(target);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Cannot read " + describe(), e);
            }
        }

        @Override
        public void set(T target, Object value) {
            try {
                field.set(target, value);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Cannot write " + describe(), e);
            }
        }

        private String describe() {
            return field.getDeclaringClass().getSimpleName() + "." + field.getName();
        }

        @Override
        public String toString() {
            return describe();
        }
    }
}

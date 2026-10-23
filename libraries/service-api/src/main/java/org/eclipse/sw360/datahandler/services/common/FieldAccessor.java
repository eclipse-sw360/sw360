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

/**
 * Generic access to one field of a service-api POJO, replacing the generated Thrift
 * {@code <Struct>._Fields} enum.
 *
 * <p>Thrift structs carry a {@code _Fields} enum plus {@code getFieldValue} /
 * {@code setFieldValue} / {@code isSet}, which callers use to walk a document's fields without
 * naming them — the REST PATCH merge and the Excel/CSV exporters both rely on it. Lombok POJOs have
 * no such enum, so this interface supplies the same four operations. Obtain instances from
 * {@link PojoFields}.
 *
 * <p>The mapping from Thrift to here:
 * <table>
 *   <tr><td>{@code field.getFieldName()}</td><td>{@link #name()}</td></tr>
 *   <tr><td>{@code struct.getFieldValue(field)}</td><td>{@link #get(Object)}</td></tr>
 *   <tr><td>{@code struct.setFieldValue(field, v)}</td><td>{@link #set(Object, Object)}</td></tr>
 *   <tr><td>{@code struct.isSet(field)}</td><td>{@link #isSet(Object)}</td></tr>
 * </table>
 *
 * @param <T> the POJO type this accessor reads and writes
 */
public interface FieldAccessor<T> {

    /**
     * The field's name, identical to the Thrift IDL name the generated
     * {@code _Fields.getFieldName()} returned. REST payload keys and exporter column lookups are
     * keyed on this, so it must not diverge.
     */
    String name();

    /** The declared type of the field. */
    Class<?> type();

    /** Reads the field, as {@code getFieldValue} did. */
    Object get(T target);

    /**
     * Writes the field, as {@code setFieldValue} did. Passing {@code null} clears it, matching
     * Thrift's unset behaviour for non-primitive fields.
     *
     * @throws IllegalArgumentException if {@code value} is not assignable to the field
     */
    void set(T target, Object value);

    /**
     * Whether the field holds a value, as {@code isSet} did.
     *
     * <p>Thrift tracked primitives in a bitfield, so it could tell {@code false} from unset. Every
     * POJO field reachable through {@link PojoFields} is a reference type, so a null check carries
     * the same meaning.
     */
    default boolean isSet(T target) {
        return get(target) != null;
    }
}

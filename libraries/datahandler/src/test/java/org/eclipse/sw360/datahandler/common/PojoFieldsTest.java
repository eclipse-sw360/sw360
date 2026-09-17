/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.thrift.TFieldIdEnum;
import org.eclipse.sw360.datahandler.services.common.FieldAccessor;
import org.eclipse.sw360.datahandler.services.common.PojoFields;
import org.eclipse.sw360.datahandler.services.components.Release;
import org.junit.Test;

/**
 * {@link PojoFields} replaces the generated Thrift {@code _Fields} enum. This test lives in
 * datahandler rather than service-api because it needs both the POJOs and the Thrift classes on the
 * classpath to compare them.
 */
public class PojoFieldsTest {

    @Test
    public void readsAndWritesByName() {
        Release release = new Release();
        FieldAccessor<Release> name = PojoFields.byName(Release.class, "name");

        assertNotNull("Release should expose a 'name' field", name);
        assertFalse("a fresh POJO field is unset", name.isSet(release));
        assertNull(name.get(release));

        name.set(release, "commons-lang");
        assertEquals("commons-lang", name.get(release));
        assertEquals("commons-lang", release.getName());
        assertTrue(name.isSet(release));

        // null clears it, as Thrift's setFieldValue(field, null) did
        name.set(release, null);
        assertFalse(name.isSet(release));
    }

    @Test
    public void unknownFieldYieldsNullLikeFindByName() {
        assertNull(PojoFields.byName(Release.class, "noSuchField"));
    }

    @Test
    public void excludesStaticAndSyntheticFields() {
        // JaCoCo adds a static synthetic $jacocoData field during instrumented CI runs; if it were
        // treated as a document field it would leak into REST payloads and exported spreadsheets.
        for (String fieldName : PojoFields.names(Release.class)) {
            assertFalse("synthetic field leaked: " + fieldName, fieldName.startsWith("$"));
            assertFalse("synthetic field leaked: " + fieldName, fieldName.contains("jacoco"));
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void accessorListIsImmutable() {
        PojoFields.of(Release.class).clear();
    }

    @Test
    public void repeatedCallsReturnTheSameCachedAccessors() {
        assertEquals(PojoFields.of(Release.class), PojoFields.of(Release.class));
    }

    /**
     * Every Thrift field must exist on the POJO under the same name. Field access is by name, so a
     * Thrift field with no POJO counterpart would turn a working read into a silent null.
     */
    @Test
    public void everyThriftFieldExistsOnThePojo() {
        List<String> missing = new ArrayList<>();
        for (Case testCase : CASES) {
            for (String thriftField : thriftFieldNames(testCase.thriftFields)) {
                if (PojoFields.byName(testCase.pojoType, thriftField) == null) {
                    missing.add(testCase.pojoType.getSimpleName() + "." + thriftField);
                }
            }
        }
        assertTrue("Thrift fields with no POJO counterpart: " + missing, missing.isEmpty());
    }

    /**
     * The exporters take spreadsheet column order from field order — it used to come from Thrift's
     * {@code metaDataMap}, which iterates in field-id order, and now comes from POJO declaration
     * order. Reordering a POJO's fields would silently reorder columns in every Excel and CSV
     * export, so pin the two together.
     */
    @Test
    public void pojoDeclarationOrderMatchesThriftFieldOrder() {
        for (Case testCase : CASES) {
            List<String> thriftOrder = thriftFieldNames(testCase.thriftFields);
            List<String> pojoOrder = new ArrayList<>(PojoFields.names(testCase.pojoType));
            // the POJO may carry fields Thrift never had; compare only the shared ones
            pojoOrder.retainAll(thriftOrder);
            thriftOrder.retainAll(PojoFields.names(testCase.pojoType));

            assertEquals(testCase.pojoType.getSimpleName()
                    + ": POJO field order diverged from Thrift field order, which reorders exported"
                    + " spreadsheet columns", thriftOrder, pojoOrder);
        }
    }

    private static List<String> thriftFieldNames(TFieldIdEnum[] fields) {
        List<String> names = new ArrayList<>();
        for (TFieldIdEnum field : fields) {
            names.add(field.getFieldName());
        }
        return names;
    }

    private static final class Case {
        private final Class<?> pojoType;
        private final TFieldIdEnum[] thriftFields;

        private Case(Class<?> pojoType, TFieldIdEnum[] thriftFields) {
            this.pojoType = pojoType;
            this.thriftFields = thriftFields;
        }
    }

    private static final List<Case> CASES = Arrays.asList(
            new Case(Release.class,
                    org.eclipse.sw360.datahandler.thrift.components.Release._Fields.values()),
            new Case(org.eclipse.sw360.datahandler.services.components.Component.class,
                    org.eclipse.sw360.datahandler.thrift.components.Component._Fields.values()),
            new Case(org.eclipse.sw360.datahandler.services.projects.Project.class,
                    org.eclipse.sw360.datahandler.thrift.projects.Project._Fields.values()));
}

/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.components.summary;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.sw360.datahandler.services.components.Component;
import org.eclipse.sw360.datahandler.services.components.ComponentFields;
import org.eclipse.sw360.datahandler.services.components.Release;
import org.eclipse.sw360.datahandler.services.components.ReleaseFields;
import org.eclipse.sw360.datahandler.services.vendors.Vendor;
import org.junit.Test;

/**
 * The summary classes copy fields with explicit setters, and the CouchDB field-name constants are
 * hand-maintained string literals. Both mirror the POJOs without any compile-time link, so this
 * test is what keeps them from drifting: add a field to {@link Component} or {@link Release} and
 * forget the corresponding entry, and this fails instead of a column silently going blank or a
 * Mango query silently matching nothing.
 */
public class SummaryFieldParityTest {

    /**
     * Not copied by {@code ComponentSummary.setSummaryFields}, by design.
     * Empty: {@code SummaryType.SUMMARY} is a full shallow copy of the document.
     */
    private static final Set<String> COMPONENT_SUMMARY_IGNORED = Collections.emptySet();

    /**
     * Not copied by {@code ReleaseSummary.setDetailedExportSummaryFields}, by design. Mirrors
     * {@code ReleaseExporter.RELEASE_IGNORED_FIELDS}.
     */
    private static final Set<String> RELEASE_DETAILED_EXPORT_IGNORED =
            new HashSet<>(Arrays.asList("revision", "documentState", "permissions", "vendorId"));

    @Test
    public void componentSummaryCopiesEveryField() throws Exception {
        Component document = populate(new Component());
        Component copy = new ComponentSummary().makeSummary(SummaryType.SUMMARY, document);

        assertAllFieldsCopied(Component.class, document, copy, COMPONENT_SUMMARY_IGNORED,
                "ComponentSummary.setSummaryFields");
    }

    @Test
    public void releaseDetailedExportSummaryCopiesEveryRenderedField() throws Exception {
        Release document = populate(new Release());
        // summary() re-resolves the vendor from vendorId, so the provider must return one
        Release copy = new ReleaseSummary()
                .summary(SummaryType.DETAILED_EXPORT_SUMMARY, document, vendorId -> new Vendor());

        assertAllFieldsCopied(Release.class, document, copy, RELEASE_DETAILED_EXPORT_IGNORED,
                "ReleaseSummary.setDetailedExportSummaryFields");
    }

    @Test
    public void componentFieldsConstantsNameRealComponentFields() throws Exception {
        assertConstantsAreDeclaredFields(ComponentFields.class, Component.class);
    }

    @Test
    public void releaseFieldsConstantsNameRealReleaseFields() throws Exception {
        // the dotted Mango path is a composite of two constants that are checked individually
        assertConstantsAreDeclaredFields(ReleaseFields.class, Release.class,
                "ATTACHMENT_TYPE", "ATTACHMENTS_ATTACHMENT_TYPE");
    }

    // ---- helpers ----

    private static void assertAllFieldsCopied(Class<?> type, Object document, Object copy,
            Set<String> ignored, String copierName) throws Exception {
        List<String> missing = new ArrayList<>();
        for (Field field : type.getDeclaredFields()) {
            if (field.isSynthetic() || Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (ignored.contains(field.getName())) {
                continue;
            }
            field.setAccessible(true);
            if (field.get(copy) == null) {
                missing.add(field.getName());
            }
        }
        assertTrue(copierName + " does not copy " + missing + " — add them there, or to the"
                + " documented ignore list in this test if they are deliberately dropped",
                missing.isEmpty());
    }

    private static void assertConstantsAreDeclaredFields(Class<?> constantsType, Class<?> pojoType,
            String... skippedConstants) throws Exception {
        Set<String> pojoFields = new HashSet<>();
        for (Field field : pojoType.getDeclaredFields()) {
            if (!field.isSynthetic() && !Modifier.isStatic(field.getModifiers())) {
                pojoFields.add(field.getName());
            }
        }
        Set<String> skipped = new HashSet<>(Arrays.asList(skippedConstants));
        Set<String> unknown = new TreeSet<>();
        for (Field constant : constantsType.getDeclaredFields()) {
            if (!Modifier.isStatic(constant.getModifiers()) || constant.getType() != String.class) {
                continue;
            }
            if (skipped.contains(constant.getName())) {
                continue;
            }
            constant.setAccessible(true);
            String value = (String) constant.get(null);
            if (!pojoFields.contains(value)) {
                unknown.add(constant.getName() + "=\"" + value + "\"");
            }
        }
        assertEquals(constantsType.getSimpleName() + " names CouchDB keys that are not fields of "
                + pojoType.getSimpleName() + " — a rename must be applied to both",
                Collections.emptySet(), unknown);
    }

    /** Gives every field a distinct non-null value so an uncopied field shows up as null. */
    private static <T> T populate(T pojo) throws Exception {
        for (Field field : pojo.getClass().getDeclaredFields()) {
            if (field.isSynthetic() || Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            field.setAccessible(true);
            field.set(pojo, sampleValue(field.getType(), field.getName()));
        }
        return pojo;
    }

    private static Object sampleValue(Class<?> type, String name) throws Exception {
        if (type == String.class) {
            return "value-" + name;
        }
        if (type == Boolean.class || type == boolean.class) {
            return Boolean.TRUE;
        }
        if (type == Integer.class || type == int.class) {
            return 1;
        }
        if (type == Long.class || type == long.class) {
            return 1L;
        }
        if (type.isEnum()) {
            return type.getEnumConstants()[0];
        }
        if (Set.class.isAssignableFrom(type)) {
            return new HashSet<>(Collections.singleton("value-" + name));
        }
        if (List.class.isAssignableFrom(type)) {
            return new ArrayList<>(Collections.singletonList("value-" + name));
        }
        if (java.util.Map.class.isAssignableFrom(type)) {
            return new HashMap<>(Collections.singletonMap("key", "value-" + name));
        }
        // nested service-api POJO: an empty instance is enough, we only assert non-null
        return type.getDeclaredConstructor().newInstance();
    }
}

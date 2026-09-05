/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.datahandler.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.thrift.TEnum;
import org.junit.Test;

/**
 * {@link ThriftEnumUtils} is now a shim that resolves a thrift enum to its service-api twin and
 * delegates to {@link EnumDisplayNames}. These tests cover the shim; the label tables themselves
 * are covered by {@code EnumDisplayNamesTest}.
 */
public class ThriftEnumUtilsTest {

    @Test
    public void testToString() {
        assertEquals("Design document", ThriftEnumUtils.enumToString(
                org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType.DESIGN));
        assertEquals("Git", ThriftEnumUtils.enumToString(
                org.eclipse.sw360.datahandler.thrift.components.RepositoryType.GIT));
    }

    @Test
    public void testToShortString() {
        assertEquals("CRT", ThriftEnumUtils.enumToShortString(
                org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType.CLEARING_REPORT));
    }

    @Test
    public void testRoundTripThroughLabel() {
        org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType type =
                org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType.DESIGN;
        String label = ThriftEnumUtils.enumToString(type);
        assertEquals(type, ThriftEnumUtils.enumByString(label,
                org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType.class));
        assertEquals(type, ThriftEnumUtils.stringToEnum(type.name(),
                org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType.class));
    }

    /**
     * The shim matches a thrift constant to its service-api twin by name. If any thrift enum gains a
     * constant its twin does not have, the label silently degrades to the raw constant name and
     * shows up in the clearing status report and the Excel/CSV exports — so assert every constant of
     * every registered type still resolves to a real label.
     */
    @Test
    public void everyThriftConstantOfEveryRegisteredTypeHasALabel() {
        List<String> unlabelled = new ArrayList<>();
        int checked = 0;

        for (Map.Entry<Class<?>, Map<?, String>> entry : EnumDisplayNames.MAP_ENUMTYPE_MAP.entrySet()) {
            Class<?> thriftType = thriftTwinOf(entry.getKey());
            if (thriftType == null) {
                continue;
            }
            for (Object constant : thriftType.getEnumConstants()) {
                checked++;
                String label = ThriftEnumUtils.enumToString((TEnum) constant);
                if (label == null || label.isEmpty() || label.equals(((Enum<?>) constant).name())) {
                    unlabelled.add(thriftType.getSimpleName() + "." + ((Enum<?>) constant).name());
                }
            }
        }

        assertTrue("no thrift enum constants were checked — the twin lookup is broken", checked > 0);
        assertTrue("thrift constants with no service-api label: " + unlabelled, unlabelled.isEmpty());
    }

    /** Mirror of the shim's own lookup, so the test fails rather than silently skipping. */
    private static Class<?> thriftTwinOf(Class<?> serviceApiType) {
        for (String pkg : new String[] {
                "org.eclipse.sw360.datahandler.thrift",
                "org.eclipse.sw360.datahandler.thrift.components",
                "org.eclipse.sw360.datahandler.thrift.attachments",
                "org.eclipse.sw360.datahandler.thrift.projects",
                "org.eclipse.sw360.datahandler.thrift.licenses",
                "org.eclipse.sw360.datahandler.thrift.users",
                "org.eclipse.sw360.datahandler.thrift.moderation",
                "org.eclipse.sw360.datahandler.thrift.packages",
                "org.eclipse.sw360.datahandler.thrift.vulnerabilities",
        }) {
            try {
                Class<?> candidate = Class.forName(pkg + "." + serviceApiType.getSimpleName());
                if (candidate.isEnum()) {
                    assertNotNull(candidate);
                    return candidate;
                }
            } catch (ClassNotFoundException expected) {
                // try the next package
            }
        }
        return null;
    }
}

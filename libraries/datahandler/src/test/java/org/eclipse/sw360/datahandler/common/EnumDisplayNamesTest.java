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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.sw360.datahandler.services.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.services.components.ClearingState;
import org.junit.Test;

/**
 * These labels reach user facing output — the project clearing status report and the Excel/CSV
 * exports — so a missing entry is a visible defect, not a cosmetic one.
 */
public class EnumDisplayNamesTest {

    @Test
    public void everyConstantOfEveryRegisteredTypeHasALabel() {
        List<String> unlabelled = new ArrayList<>();

        for (Map.Entry<Class<?>, Map<?, String>> entry : EnumDisplayNames.MAP_ENUMTYPE_MAP.entrySet()) {
            Class<?> type = entry.getKey();
            Map<?, String> labels = entry.getValue();
            for (Object constant : type.getEnumConstants()) {
                String label = labels.get(constant);
                if (label == null || label.isEmpty()) {
                    unlabelled.add(type.getSimpleName() + "." + ((Enum<?>) constant).name());
                }
            }
        }

        assertTrue("service-api enum constants with no display label: " + unlabelled,
                unlabelled.isEmpty());
    }

    @Test
    public void labelsAreHumanReadableNotConstantNames() {
        assertEquals("Under clearing", EnumDisplayNames.toDisplayString(ClearingState.UNDER_CLEARING));
        assertEquals("Report available", EnumDisplayNames.toDisplayString(ClearingState.REPORT_AVAILABLE));
        assertEquals("Design document", EnumDisplayNames.toDisplayString(AttachmentType.DESIGN));
    }

    @Test
    public void shortLabelsResolve() {
        assertEquals("CRT", EnumDisplayNames.toShortString(AttachmentType.CLEARING_REPORT));
    }

    @Test
    public void labelsRoundTrip() {
        for (ClearingState state : ClearingState.values()) {
            assertEquals(state,
                    EnumDisplayNames.byDisplayString(EnumDisplayNames.toDisplayString(state),
                            ClearingState.class));
        }
    }

    @Test
    public void nullAndUnknownAreHandledWithoutThrowing() {
        assertEquals("", EnumDisplayNames.toDisplayString(null));
        assertEquals("", EnumDisplayNames.toShortString(null));
        assertNull(EnumDisplayNames.byDisplayString("no such label", ClearingState.class));
        assertNull(EnumDisplayNames.byName("NO_SUCH_CONSTANT", ClearingState.class));
        // an enum with no registered labels falls back to its constant name rather than failing
        assertEquals(Unregistered.SOMETHING.name(),
                EnumDisplayNames.toDisplayString(Unregistered.SOMETHING));
    }

    private enum Unregistered {
        SOMETHING
    }
}

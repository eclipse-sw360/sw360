/*
 * Copyright (c) 2026 Dhawaj and others. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.moderation.db;

import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ComponentModerationRequestGeneratorTest {

    private ComponentModerationRequestGenerator generator;
    private Component actualComponent;

    @BeforeEach
    void setUp() {
        generator = new ComponentModerationRequestGenerator();

        // Baseline: Existing component in db where homepage is originally null
        actualComponent = new Component();
        actualComponent.setId("comp-001");
        actualComponent.setName("Original Component");
        actualComponent.setHomepage(null);
    }

    @Test
    @DisplayName("Second edit must not wipe out prior pending additions when field was originally null in DB")
    void testAccumulateAdditions_WhenActualFieldOriginallyNull() {
        ModerationRequest request = new ModerationRequest();

        // EDIT 1: User adds homepage for the first time
        Component edit1 = new Component(actualComponent);
        edit1.setHomepage("https://eclipse.org");
        generator.setAdditionsAndDeletions(request, edit1, actualComponent);

        // Verify Edit 1 recorded properly
        assertNotNull(request.getComponentAdditions());
        assertTrue(request.getComponentAdditions().isSetHomepage(), "Edit 1 should have homepage set");
        assertEquals("https://eclipse.org", request.getComponentAdditions().getHomepage());

        // EDIT 2: User comes back and only changes component name
        // Homepage is untouched (remains null in the update payload)
        Component edit2 = new Component(actualComponent);
        edit2.setName("Updated Component Name");
        edit2.setHomepage(null);

        // Act: Run second edit on the same ModerationRequest
        generator.setAdditionsAndDeletions(request, edit2, actualComponent);

        // Assert Accumulation
        Component additions = request.getComponentAdditions();
        assertNotNull(additions, "componentAdditions must not be null");

        // Edit 2 change present
        assertEquals("Updated Component Name", additions.getName());

        // Check: Did Edit 2 wipe out Edit 1's homepage addition?
        assertTrue(additions.isSetHomepage(),
                "Homepage addition from Edit 1 must NOT be wiped out by subsequent edit!");
        assertEquals("https://eclipse.org", additions.getHomepage());
    }
}

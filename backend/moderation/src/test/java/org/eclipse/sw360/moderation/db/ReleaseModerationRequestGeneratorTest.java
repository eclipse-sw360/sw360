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

import org.eclipse.sw360.datahandler.thrift.components.ClearingInformation;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ReleaseModerationRequestGeneratorTest {
    private ReleaseModerationRequestGenerator generator;
    private Release actualRelease;
    @BeforeEach
    void setUp() {
        generator = new ReleaseModerationRequestGenerator();
        // Existing release in database
        actualRelease = new Release();
        actualRelease.setId("rel-001");
        actualRelease.setName("Apache Thrift");
        actualRelease.setVersion("1.0.0");
        actualRelease.setComponentId("comp-001");
        // Existing clearing information
        ClearingInformation initialCI = new ClearingInformation();
        initialCI.setAdditionalRequestInfo("Initial notes");
        actualRelease.setClearingInformation(initialCI);
    }

    @Test
    void testInitialEditGeneratesCorrectDiff() {
        // Arrange
        ModerationRequest request = new ModerationRequest();
        Release updatedRelease = new Release(actualRelease);
        ClearingInformation updatedCI = new ClearingInformation(actualRelease.getClearingInformation());
        updatedCI.setAdditionalRequestInfo("Need clarification on licenses");
        updatedRelease.setClearingInformation(updatedCI);

        // Act
        generator.setAdditionsAndDeletions(request, updatedRelease, actualRelease);

        // Assert
        assertNotNull(request.getReleaseAdditions(), "releaseAdditions should not be null");
        assertNotNull(request.getReleaseDeletions(), "releaseDeletions should not be null");

        // Verify additions (new value)
        assertTrue(request.getReleaseAdditions().isSetClearingInformation());
        assertEquals("Need clarification on licenses",
                request.getReleaseAdditions().getClearingInformation().getAdditionalRequestInfo());

        // Verify deletions (old baseline value)
        assertTrue(request.getReleaseDeletions().isSetClearingInformation());
        assertEquals("Initial notes",
                request.getReleaseDeletions().getClearingInformation().getAdditionalRequestInfo());
    }

    @Test
    void testAccumulateDiffsAcrossMultipleEdits() {
        ModerationRequest request = new ModerationRequest();

        // STEP 1: First Edit (Clearing Information update)
        Release firstEdit = new Release(actualRelease);
        ClearingInformation ciEdit = new ClearingInformation(actualRelease.getClearingInformation());
        ciEdit.setAdditionalRequestInfo("Pending clarification");
        firstEdit.setClearingInformation(ciEdit);
        generator.setAdditionsAndDeletions(request, firstEdit, actualRelease);

        // Verify that Edit 1 is stored correctly
        assertNotNull(request.getReleaseAdditions());
        assertEquals("Pending clarification",
                request.getReleaseAdditions().getClearingInformation().getAdditionalRequestInfo());

        // STEP 2: Second Edit (User only changes Release Name )
        Release secondEdit = new Release(actualRelease);
        secondEdit.setName("Apache Thrift Modernized");

        // Pass same request
        generator.setAdditionsAndDeletions(request, secondEdit, actualRelease);

        // STEP 3: Check if both changes are merged
        Release additions = request.getReleaseAdditions();
        Release deletions = request.getReleaseDeletions();
        assertNotNull(additions);
        assertNotNull(deletions);

        // Additions should have edit 2 changes
        assertEquals("Apache Thrift Modernized", additions.getName());
        assertEquals("Apache Thrift", deletions.getName());

        // Changes of edit 1 must not be wiped off
        assertTrue(additions.isSetClearingInformation(),
                "ClearingInformation additions should still be present from Edit 1!");
        assertEquals("Pending clarification",
                additions.getClearingInformation().getAdditionalRequestInfo(),
                "Edit 1 additionalRequestInfo must be preserved after Edit 2!");
    }

    @Test
    void testAccumulateDiffs_WhenActualClearingInformationOriginallyNull() {
        // Arrange: one release where ClearingInformation is null
        Release bareRelease = new Release();
        bareRelease.setId("rel-bare-001");
        bareRelease.setName("Apache Thrift");
        bareRelease.setVersion("1.0.0");
        bareRelease.setComponentId("comp-001");
        bareRelease.setClearingInformation(null); // Explicitly null in database

        ModerationRequest request = new ModerationRequest();

        // EDIT 1: User adds Clearing Information for the first time
        Release firstEdit = new Release(bareRelease);
        ClearingInformation newCI = new ClearingInformation();
        newCI.setAdditionalRequestInfo("First time clarification");
        firstEdit.setClearingInformation(newCI);
        generator.setAdditionsAndDeletions(request, firstEdit, bareRelease);

        // Verify Edit 1
        assertNotNull(request.getReleaseAdditions());
        assertTrue(request.getReleaseAdditions().isSetClearingInformation());
        assertEquals("First time clarification",
                request.getReleaseAdditions().getClearingInformation().getAdditionalRequestInfo());

        // EDIT 2: User only changes release name; Clearing Information is untouched (null)
        Release secondEdit = new Release(bareRelease);
        secondEdit.setName("Apache Thrift v2");
        secondEdit.setClearingInformation(null); // untouched / absent

        // Act: Run second edit on the same request
        generator.setAdditionsAndDeletions(request, secondEdit, bareRelease);

        // Assert
        Release additions = request.getReleaseAdditions();
        assertNotNull(additions);

        // Second edit change present
        assertEquals("Apache Thrift v2", additions.getName());

        // Edit 1 addition must NOT be wiped to null
        assertTrue(additions.isSetClearingInformation(),
                "ClearingInformation additions must be preserved even when actual release originally had null CI");
        assertEquals("First time clarification",
                additions.getClearingInformation().getAdditionalRequestInfo());
    }
}

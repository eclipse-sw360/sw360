/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.components.summary;

import org.eclipse.sw360.datahandler.services.projects.Project;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ProjectSummaryTest {

    private Project fullyPopulatedProject() {
        return new Project()
                .setId("id-1")
                .setName("Project Name")
                .setDescription("Project Description")
                .setVersion("1.0")
                .setClearingTeam("clearing-team")
                .setBusinessUnit("BU")
                .setProjectResponsible("responsible@sw360.org")
                .setCreatedBy("creator@sw360.org")
                .setTag("tag");
    }

    @Test
    public void testSummaryCopiesExpectedFields() {
        Project document = fullyPopulatedProject();
        Project copy = new ProjectSummary().makeSummary(SummaryType.SUMMARY, document);

        assertEquals(document.getId(), copy.getId());
        assertEquals(document.getName(), copy.getName());
        assertEquals(document.getDescription(), copy.getDescription());
        assertEquals(document.getVersion(), copy.getVersion());
        assertEquals(document.getClearingTeam(), copy.getClearingTeam());
        assertEquals(document.getBusinessUnit(), copy.getBusinessUnit());
        assertEquals(document.getProjectResponsible(), copy.getProjectResponsible());
        assertEquals(document.getCreatedBy(), copy.getCreatedBy());
        assertEquals(document.getTag(), copy.getTag());
    }

    @Test
    public void testDefaultCopiesExpectedFields() {
        Project document = fullyPopulatedProject();
        Project copy = new ProjectSummary().makeSummary(SummaryType.SHORT, document);

        assertEquals(document.getId(), copy.getId());
        assertEquals(document.getName(), copy.getName());
        assertEquals(document.getDescription(), copy.getDescription());
        assertEquals(document.getVersion(), copy.getVersion());
        assertEquals(document.getClearingTeam(), copy.getClearingTeam());
        assertNull(copy.getBusinessUnit());
        assertNull(copy.getProjectResponsible());
        assertNull(copy.getCreatedBy());
    }

    @Test
    public void testLinkedProjectAccessibleCopiesExpectedFields() {
        Project document = fullyPopulatedProject();
        Project copy = new ProjectSummary().makeSummary(SummaryType.LINKED_PROJECT_ACCESSIBLE, document);

        assertEquals(document.getId(), copy.getId());
        assertEquals(document.getName(), copy.getName());
        assertEquals(document.getDescription(), copy.getDescription());
        assertEquals(document.getVersion(), copy.getVersion());
        assertEquals(document.getClearingTeam(), copy.getClearingTeam());
        assertEquals(document.getBusinessUnit(), copy.getBusinessUnit());
        assertEquals(document.getProjectResponsible(), copy.getProjectResponsible());
        assertNull(copy.getCreatedBy());
        assertNull(copy.getTag());
    }
}

/*
 * Copyright Bosch Software Innovations GmbH, 2018.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.datahandler.resourcelists;

import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.thrift.components.ClearingState;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.ComponentType;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectClearingState;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectType;
import org.eclipse.sw360.datahandler.thrift.search.SearchResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Comparator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class ResourceComparatorGeneratorTest {

    private ResourceComparatorGenerator resourceComparatorGenerator;

    @Before
    public void setUp() throws Exception {
        resourceComparatorGenerator = new ResourceComparatorGenerator();
    }

    @Test
    public void checkComponentComparatorName() throws ResourceClassNotFoundException {
        Comparator<Component> nameComparator = resourceComparatorGenerator.generateComparator(SW360Constants.TYPE_COMPONENT, "name");
        assertNotNull(nameComparator);
        assertTrue(nameComparator.compare(new Component().setName("alpha"), new Component().setName("beta")) < 0);
        assertTrue(nameComparator.compare(new Component().setName("beta"), new Component().setName("alpha")) > 0);
        assertEquals(nameComparator.compare(new Component().setName("alpha"), new Component().setName("alpha")), 0);
    }

    @Test
    public void checkComponentComparatorCreatedOn() throws ResourceClassNotFoundException {
        Comparator<Component> createdOnComparator = resourceComparatorGenerator.generateComparator(SW360Constants.TYPE_COMPONENT, "createdOn");
        assertNotNull(createdOnComparator);
        assertTrue(createdOnComparator.compare(new Component().setCreatedOn("06-08-2018"), new Component().setCreatedOn("07-08-2018")) < 0);
        assertTrue(createdOnComparator.compare(new Component().setCreatedOn("07-08-2018"), new Component().setCreatedOn("06-08-2018")) > 0);
        assertEquals(createdOnComparator.compare(new Component().setCreatedOn("07-08-2018"), new Component().setCreatedOn("07-08-2018")), 0);
    }

    @Test
    public void checkComponentComparatorCreatedBy() throws ResourceClassNotFoundException {
        Comparator<Component> createdByComparator = resourceComparatorGenerator.generateComparator(SW360Constants.TYPE_COMPONENT, "createdBy");
        assertNotNull(createdByComparator);
        assertTrue(createdByComparator.compare(new Component().setCreatedBy("John Doe"), new Component().setCreatedBy("Max Mustermann")) < -0);
        assertTrue(createdByComparator.compare(new Component().setCreatedBy("Max Mustermann"), new Component().setCreatedBy("John Doe")) > 0);
        assertEquals(createdByComparator.compare(new Component().setCreatedBy("John Doe"), new Component().setCreatedBy("John Doe")),0);
    }

    @Test
    public void checkComponentTypeComparator() throws ResourceClassNotFoundException {
        Comparator<Component> componentTypeComparator = resourceComparatorGenerator.generateComparator(SW360Constants.TYPE_COMPONENT, "componentType");
        assertNotNull(componentTypeComparator);
        assertTrue(componentTypeComparator.compare(new Component().setComponentType(ComponentType.COTS), new Component().setComponentType(ComponentType.FREESOFTWARE)) < -0);
        assertTrue(componentTypeComparator.compare(new Component().setComponentType(ComponentType.FREESOFTWARE), new Component().setComponentType(ComponentType.COTS)) > 0);
        assertEquals(componentTypeComparator.compare(new Component().setComponentType(ComponentType.COTS), new Component().setComponentType(ComponentType.COTS)),0);
    }

    @Test
    public void checkProjectComparatorName() throws ResourceClassNotFoundException {
        Comparator<Project> nameComparator = resourceComparatorGenerator.generateComparator(SW360Constants.TYPE_PROJECT, "name");
        assertNotNull(nameComparator);
        assertTrue(nameComparator.compare(new Project().setName("John Doe"), new Project().setName("Max Mustermann")) < -0);
        assertTrue(nameComparator.compare(new Project().setName("Max Mustermann"), new Project().setName("John Doe")) > 0);
        assertEquals(nameComparator.compare(new Project().setName("John Doe"), new Project().setName("John Doe")),0);
    }

    @Test
    public void checkProjectComparatorCreatedOn() throws ResourceClassNotFoundException {
        Comparator<Project> createdOnComparator = resourceComparatorGenerator.generateComparator(SW360Constants.TYPE_PROJECT, "createdOn");
        assertNotNull(createdOnComparator);
        assertTrue(createdOnComparator.compare(new Project().setCreatedOn("06-08-2018"), new Project().setCreatedOn("07-08-2018")) < 0);
        assertTrue(createdOnComparator.compare(new Project().setCreatedOn("07-08-2018"), new Project().setCreatedOn("06-08-2018")) > 0);
        assertEquals(createdOnComparator.compare(new Project().setCreatedOn("07-08-2018"), new Project().setCreatedOn("07-08-2018")), 0);
    }

    @Test
    public void checkProjectComparatorCreatedBy() throws ResourceClassNotFoundException {
        Comparator<Project> createdByComparator = resourceComparatorGenerator.generateComparator(SW360Constants.TYPE_PROJECT, "createdBy");
        assertNotNull(createdByComparator);
        assertTrue(createdByComparator.compare(new Project().setCreatedBy("John Doe"), new Project().setCreatedBy("Max Mustermann")) < -0);
        assertTrue(createdByComparator.compare(new Project().setCreatedBy("Max Mustermann"), new Project().setCreatedBy("John Doe")) > 0);
        assertEquals(createdByComparator.compare(new Project().setCreatedBy("John Doe"), new Project().setCreatedBy("John Doe")),0);
    }

    @Test
    public void checkProjectComparatorCleringState() throws ResourceClassNotFoundException {
        Comparator<Project> clearingSateComparator = resourceComparatorGenerator.generateComparator(SW360Constants.TYPE_PROJECT, "clearingState");
        assertNotNull(clearingSateComparator);
        assertTrue(clearingSateComparator.compare(new Project().setClearingState(ProjectClearingState.CLOSED), new Project().setClearingState(ProjectClearingState.OPEN)) < -0);
        assertTrue(clearingSateComparator.compare(new Project().setClearingState(ProjectClearingState.OPEN), new Project().setClearingState(ProjectClearingState.CLOSED)) > 0);
        assertEquals(clearingSateComparator.compare(new Project().setClearingState(ProjectClearingState.OPEN), new Project().setClearingState(ProjectClearingState.OPEN)), 0);
    }

    @Test
    public void checkProjectTypeComparator() throws ResourceClassNotFoundException {
        Comparator<Project> clearingSateComparator = resourceComparatorGenerator.generateComparator(SW360Constants.TYPE_PROJECT, "projectType");
        assertNotNull(clearingSateComparator);
        assertTrue(clearingSateComparator.compare(new Project().setProjectType(ProjectType.CUSTOMER), new Project().setProjectType(ProjectType.INTERNAL)) < -0);
        assertTrue(clearingSateComparator.compare(new Project().setProjectType(ProjectType.INTERNAL), new Project().setProjectType(ProjectType.CUSTOMER)) > 0);
        assertEquals(clearingSateComparator.compare(new Project().setProjectType(ProjectType.CUSTOMER), new Project().setProjectType(ProjectType.CUSTOMER)), 0);
    }

    @Test
    public void checkReleaseComparatorCleringState() throws ResourceClassNotFoundException {
        Comparator<Release> clearingSateComparator = resourceComparatorGenerator.generateComparator(SW360Constants.TYPE_RELEASE, "clearingState");
        assertNotNull(clearingSateComparator);
        assertTrue(clearingSateComparator.compare(new Release().setClearingState(ClearingState.APPROVED), new Release().setClearingState(ClearingState.NEW_CLEARING)) < -0);
        assertTrue(clearingSateComparator.compare(new Release().setClearingState(ClearingState.NEW_CLEARING), new Release().setClearingState(ClearingState.APPROVED)) > 0);
        assertEquals(clearingSateComparator.compare(new Release().setClearingState(ClearingState.APPROVED), new Release().setClearingState(ClearingState.APPROVED)), 0);
    }

    @Test
    public void checkReleaseComparatorName() throws ResourceClassNotFoundException {
        Comparator<Release> nameComparator = resourceComparatorGenerator.generateComparator(SW360Constants.TYPE_RELEASE, "name");
        assertNotNull(nameComparator);
        assertTrue(nameComparator.compare(new Release().setName("alpha"), new Release().setName("beta")) < 0);
        assertTrue(nameComparator.compare(new Release().setName("beta"), new Release().setName("alpha")) > 0);
        assertEquals(nameComparator.compare(new Release().setName("alpha"), new Release().setName("alpha")), 0);
    }

    @Test
    public void checkSearchResultComparatorName() throws ResourceClassNotFoundException {
        Comparator<SearchResult> nameComparator = resourceComparatorGenerator.generateComparator(SW360Constants.TYPE_SEARCHRESULT, "name");
        assertNotNull(nameComparator);
        assertTrue(nameComparator.compare(new SearchResult().setName("alpha"), new SearchResult().setName("beta")) < 0);
        assertTrue(nameComparator.compare(new SearchResult().setName("beta"), new SearchResult().setName("alpha")) > 0);
        assertEquals(nameComparator.compare(new SearchResult().setName("alpha"), new SearchResult().setName("alpha")), 0);
    }

    @Test
    public void checkSearchResultComparatorType() throws ResourceClassNotFoundException {
        Comparator<SearchResult> nameComparator = resourceComparatorGenerator.generateComparator(SW360Constants.TYPE_SEARCHRESULT, "type");
        assertNotNull(nameComparator);
        assertTrue(nameComparator.compare(new SearchResult().setType("project"), new SearchResult().setType("release")) < 0);
        assertTrue(nameComparator.compare(new SearchResult().setType("release"), new SearchResult().setType("project")) > 0);
        assertEquals(nameComparator.compare(new SearchResult().setType("project"), new SearchResult().setType("project")), 0);
    }
}

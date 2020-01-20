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
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class ResourceListControllerTest {

    private ResourceListController resourceListController;
    private ResourceComparatorGenerator resourceComparatorGenerator;
    private List<Component> unsortedComponents;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        resourceListController = new ResourceListController();
        resourceComparatorGenerator = new ResourceComparatorGenerator();
        unsortedComponents = new ArrayList<>();

        Component componentC = new Component();
        componentC.setName("ccc");
        componentC.setCreatedOn("2014-01-05");
        componentC.setCreatedBy("D");
        unsortedComponents.add(componentC);

        Component componentB = new Component();
        componentB.setName("bbb");
        componentB.setCreatedOn("2018-04-06");
        componentB.setCreatedBy("A");
        unsortedComponents.add(componentB);

        Component componentA = new Component();
        componentA.setName("aaa");
        componentA.setCreatedOn("2018-06-13");
        componentA.setCreatedBy("P");
        unsortedComponents.add(componentA);

        Component componentE = new Component();
        componentE.setName("eee");
        componentE.setCreatedOn("2017-09-29");
        componentE.setCreatedBy("Z");
        unsortedComponents.add(componentE);

        Component componentD = new Component();
        componentD.setName("ddd");
        componentD.setCreatedOn("2018-07-29");
        componentD.setCreatedBy("M");
        unsortedComponents.add(componentD);
    }

    @Test
    public void testPagingAndSortingByName() throws PaginationParameterException, ResourceClassNotFoundException {
        Comparator comparator = resourceComparatorGenerator.generateComparator(SW360Constants.TYPE_COMPONENT, "name");
        PaginationOptions paginationOptions = new PaginationOptions(0, 2, comparator);
        List<Component> tmpList = new ArrayList<>(unsortedComponents);
        PaginationResult paginationResult = resourceListController.applyPagingToList(tmpList, paginationOptions);
        assertEquals(paginationResult.getResources().size(), 2);
        assertEquals(unsortedComponents.get(2), paginationResult.getResources().get(0));
        assertEquals(unsortedComponents.get(1), paginationResult.getResources().get(1));
    }

    @Test
    public void testPagingAndSortingByCreatedOn() throws PaginationParameterException, ResourceClassNotFoundException {
        Comparator comparator = resourceComparatorGenerator.generateComparator(SW360Constants.TYPE_COMPONENT, "createdOn").reversed();
        PaginationOptions paginationOptions = new PaginationOptions(1, 3, comparator);
        List<Component> tmpList = new ArrayList<>(unsortedComponents);
        PaginationResult paginationResult = resourceListController.applyPagingToList(tmpList, paginationOptions);
        assertTrue(paginationResult.getResources().size() == 2);
        assertEquals(unsortedComponents.get(3), paginationResult.getResources().get(0));
        assertEquals(unsortedComponents.get(0), paginationResult.getResources().get(1));
    }

    @Test
    public void testPagingAndSortingByCreatedBy() throws PaginationParameterException, ResourceClassNotFoundException {
        Comparator comparator = resourceComparatorGenerator.generateComparator(SW360Constants.TYPE_COMPONENT, "createdBy");
        PaginationOptions paginationOptions = new PaginationOptions(0, 4, comparator);
        List<Component> tmpList = new ArrayList<>(unsortedComponents);
        PaginationResult paginationResult = resourceListController.applyPagingToList(tmpList, paginationOptions);
        assertTrue(paginationResult.getResources().size() == 4);
        assertEquals(unsortedComponents.get(1), paginationResult.getResources().get(0));
        assertEquals(unsortedComponents.get(0), paginationResult.getResources().get(1));
        assertEquals(unsortedComponents.get(4), paginationResult.getResources().get(2));
        assertEquals(unsortedComponents.get(2), paginationResult.getResources().get(3));
    }

    @Test
    public void testPagingAndSortingInvalidPage() throws PaginationParameterException, ResourceClassNotFoundException {
        Comparator comparator = resourceComparatorGenerator.generateComparator(SW360Constants.TYPE_COMPONENT, "name");
        PaginationOptions paginationOptions = new PaginationOptions(3, 4, comparator);
        List<Component> tmpList = new ArrayList<>(unsortedComponents);
        thrown.expect(PaginationParameterException.class);
        resourceListController.applyPagingToList(tmpList, paginationOptions);
    }

}

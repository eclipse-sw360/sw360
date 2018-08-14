/*
 * Copyright Bosch Software Innovations GmbH, 2018.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.datahandler.resourcelists;

import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.thrift.components.Component;
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

}

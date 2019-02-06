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

import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(MockitoJUnitRunner.class)
public class ResourcePredicateGeneratorTest {

    private ResourcePredicateGenerator resourcePredicateGenerator = new ResourcePredicateGenerator();
    private List<Component> componentList;

    @Before
    public void setUp() throws Exception {
        componentList = new ArrayList<>();
        componentList.add(new Component().setName("AAA"));
        componentList.add(new Component().setName("BBB"));
        componentList.add(new Component().setName("CCC"));
    }

    @Test
    public void checkComponentPredicateValid() {
        String filterString = "AAA";

        Map<String, String> filterParams = new HashMap<>();
        filterParams.put("name",filterString);
        Predicate<Component> namePredicate = resourcePredicateGenerator.predicateFromFilterMap(Component.class, filterParams);
        assertNotNull(namePredicate);

        List<Component> result = componentList.stream().filter(namePredicate).collect(Collectors.toList());
        assertEquals(result.size(), 1);
        Component filteredComponent = result.get(0);
        assertEquals(filteredComponent.getName(), filterString);
    }

    @Test
    public void checkComponentPredicateNoResult() {
        String filterString = "DDD";

        Map<String, String> filterParams = new HashMap<>();
        filterParams.put("name",filterString);
        Predicate<Component> namePredicate = resourcePredicateGenerator.predicateFromFilterMap(Component.class, filterParams);
        assertNotNull(namePredicate);

        List<Component> result = componentList.stream().filter(namePredicate).collect(Collectors.toList());
        assertEquals(result.size(), 0);
    }

}

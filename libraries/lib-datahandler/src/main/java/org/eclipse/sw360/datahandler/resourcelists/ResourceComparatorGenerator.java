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

import org.apache.thrift.TBase;
import org.apache.thrift.TFieldIdEnum;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.thrift.components.Component;

import java.util.*;

public class ResourceComparatorGenerator {

    private static final Map<Component._Fields, Comparator<Component>> componentMap = generateComponentMap();

    private static Map<Component._Fields, Comparator<Component>> generateComponentMap() {
        Map<Component._Fields, Comparator<Component>> componentMap = new HashMap<>();
        componentMap.put(Component._Fields.NAME, Comparator.comparing(Component::getName, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        componentMap.put(Component._Fields.CREATED_ON, Comparator.comparing(Component::getCreatedOn, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        componentMap.put(Component._Fields.CREATED_BY, Comparator.comparing(Component::getCreatedBy, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        componentMap.put(Component._Fields.COMPONENT_TYPE, Comparator.comparing(c -> Optional.ofNullable(c.getComponentType()).map(Object::toString).orElse(null), Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        return Collections.unmodifiableMap(componentMap);
    }

    public <T extends TBase<?, ? extends TFieldIdEnum>> Comparator<T> generateComparator(String type) throws ResourceClassNotFoundException {
        switch (type) {
            case SW360Constants.TYPE_COMPONENT:
                return (Comparator<T>)defaultComponentComparator();
            default:
                throw new ResourceClassNotFoundException("No default comparator for resource class with name " + type);
        }
    }

    public <T extends TBase<?, ? extends TFieldIdEnum>> Comparator<T> generateComparator(String type, String property) throws ResourceClassNotFoundException {
        return generateComparator(type, Collections.singletonList(property));
    }

    public <T extends TBase<?, ? extends TFieldIdEnum>> Comparator<T> generateComparator(String type, List<String> properties) throws ResourceClassNotFoundException {
        switch (type) {
            case SW360Constants.TYPE_COMPONENT:
                List<Component._Fields> fields = new ArrayList<>();
                for(String property:properties) {
                    Component._Fields field = Component._Fields.findByName(property);
                    if (field != null) {
                        fields.add(field);
                    }
                }
                return generateComparatorWithFields(type, fields);
            default:
                throw new ResourceClassNotFoundException("No comparator for resource class with name " + type);
        }
    }

    public <T extends TBase<?, ? extends TFieldIdEnum>> Comparator<T> generateComparatorWithFields(String type, List<Component._Fields> fields) throws ResourceClassNotFoundException {
        switch (type) {
            case SW360Constants.TYPE_COMPONENT:
                return (Comparator<T>)componentComparator(fields);
            default:
                throw new ResourceClassNotFoundException("No comparator for resource class with name " + type);
        }
    }

    private Comparator<Component> componentComparator(List<Component._Fields> fields) {
        Comparator<Component> comparator = Comparator.comparing(x -> true);
        for (Component._Fields field:fields) {
            comparator = comparator.thenComparing(componentMap.get(field));
        }
        comparator = comparator.thenComparing(defaultComponentComparator());
        return comparator;
    }

    private Comparator<Component> defaultComponentComparator() {
        return componentMap.get(Component._Fields.NAME);
    }

}

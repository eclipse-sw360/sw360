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
import org.eclipse.sw360.datahandler.thrift.components.Release;

import java.util.*;

public class ResourceComparatorGenerator<T> {

    private static final Map<Component._Fields, Comparator<Component>> componentMap = generateComponentMap();
    private static final Map<Release._Fields, Comparator<Release>> releaseMap = generateReleaseMap();

    private static Map<Component._Fields, Comparator<Component>> generateComponentMap() {
        Map<Component._Fields, Comparator<Component>> componentMap = new HashMap<>();
        componentMap.put(Component._Fields.NAME, Comparator.comparing(Component::getName, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        componentMap.put(Component._Fields.CREATED_ON, Comparator.comparing(Component::getCreatedOn, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        componentMap.put(Component._Fields.CREATED_BY, Comparator.comparing(Component::getCreatedBy, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        componentMap.put(Component._Fields.COMPONENT_TYPE, Comparator.comparing(c -> Optional.ofNullable(c.getComponentType()).map(Object::toString).orElse(null), Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        return Collections.unmodifiableMap(componentMap);
    }

    private static Map<Release._Fields, Comparator<Release>> generateReleaseMap() {
        Map<Release._Fields, Comparator<Release>> releaseMap = new HashMap<>();
        releaseMap.put(Release._Fields.NAME, Comparator.comparing(Release::getName, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        releaseMap.put(Release._Fields.VERSION, Comparator.comparing(Release::getVersion, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        releaseMap.put(Release._Fields.COMPONENT_ID, Comparator.comparing(Release::getComponentId, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        releaseMap.put(Release._Fields.RELEASE_DATE, Comparator.comparing(Release::getReleaseDate, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        releaseMap.put(Release._Fields.CREATED_ON, Comparator.comparing(Release::getCreatedOn, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        releaseMap.put(Release._Fields.CREATED_BY, Comparator.comparing(Release::getCreatedBy, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        return Collections.unmodifiableMap(releaseMap);
    }

    public Comparator<T> generateComparator(String type) throws ResourceClassNotFoundException {
        switch (type) {
            case SW360Constants.TYPE_COMPONENT:
                return (Comparator<T>)defaultComponentComparator();
            case SW360Constants.TYPE_RELEASE:
                return (Comparator<T>)defaultReleaseComparator();
            default:
                throw new ResourceClassNotFoundException("No default comparator for resource class with name " + type);
        }
    }

    public Comparator<T> generateComparator(String type, String property) throws ResourceClassNotFoundException {
        return generateComparator(type, Collections.singletonList(property));
    }

    public Comparator<T> generateComparator(String type, List<String> properties) throws ResourceClassNotFoundException {
        switch (type) {
            case SW360Constants.TYPE_COMPONENT:
                List<Component._Fields> componentFields = new ArrayList<>();
                for(String property:properties) {
                    Component._Fields field = Component._Fields.findByName(property);
                    if (field != null) {
                        componentFields.add(field);
                    }
                }
                return generateComparatorWithFields(type, componentFields);
            case SW360Constants.TYPE_RELEASE:
                List<Release._Fields> releaseFields = new ArrayList<>();
                for(String property:properties) {
                    Release._Fields field = Release._Fields.findByName(property);
                    if (field != null) {
                        releaseFields.add(field);
                    }
                }
                return generateComparatorWithFields(type, releaseFields);
            default:
                throw new ResourceClassNotFoundException("No comparator for resource class with name " + type);
        }
    }

    public Comparator<T> generateComparatorWithFields(String type, List fields) throws ResourceClassNotFoundException {
        switch (type) {
            case SW360Constants.TYPE_COMPONENT:
                return (Comparator<T>)componentComparator(fields);
            case SW360Constants.TYPE_RELEASE:
                return (Comparator<T>)releaseComparator(fields);
            default:
                throw new ResourceClassNotFoundException("No comparator for resource class with name " + type);
        }
    }

    private Comparator<Component> componentComparator(List<Component._Fields> fields) {
        Comparator<Component> comparator = Comparator.comparing(x -> true);
        for (Component._Fields field:fields) {
            Comparator<Component> fieldComparator = componentMap.get(field);
            if(fieldComparator != null) {
                comparator = comparator.thenComparing(fieldComparator);
            }
        }
        comparator = comparator.thenComparing(defaultComponentComparator());
        return comparator;
    }

    private Comparator<Release> releaseComparator(List<Release._Fields> fields) {
        Comparator<Release> comparator = Comparator.comparing(x -> true);
        for (Release._Fields field:fields) {
            Comparator<Release> fieldComparator = releaseMap.get(field);
            if(fieldComparator != null) {
                comparator = comparator.thenComparing(fieldComparator);
            }
        }
        comparator = comparator.thenComparing(defaultReleaseComparator());
        return comparator;
    }

    private Comparator<Component> defaultComponentComparator() {
        return componentMap.get(Component._Fields.NAME);
    }

    private Comparator<Release> defaultReleaseComparator() { return releaseMap.get(Release._Fields.NAME); }

}

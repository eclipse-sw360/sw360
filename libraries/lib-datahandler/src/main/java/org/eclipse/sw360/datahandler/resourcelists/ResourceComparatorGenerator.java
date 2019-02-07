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
import java.util.stream.Collectors;

public class ResourceComparatorGenerator {

    private static final Map<Component._Fields, Comparator<Component>> componentMap;
    private static final Map<Release._Fields, Comparator<Release>> releaseMap;
    private static final Comparator<Component> defaultComponentComparator;
    private static final Comparator<Release> defaultReleaseComparator;


    static {
        Map<Component._Fields, Comparator<Component>> mutableComponentMap = new HashMap<>();
        mutableComponentMap.put(Component._Fields.NAME, Comparator.comparing(Component::getName, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        mutableComponentMap.put(Component._Fields.CREATED_ON, Comparator.comparing(Component::getCreatedOn, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        mutableComponentMap.put(Component._Fields.CREATED_BY, Comparator.comparing(Component::getCreatedBy, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        mutableComponentMap.put(Component._Fields.COMPONENT_TYPE, Comparator.comparing(c -> Optional.ofNullable(c.getComponentType()).map(Object::toString).orElse(null), Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        componentMap = Collections.unmodifiableMap(mutableComponentMap);
        defaultComponentComparator = componentMap.get(Component._Fields.NAME);

        Map<Release._Fields, Comparator<Release>> mutableReleaseMap = new HashMap<>();
        mutableReleaseMap.put(Release._Fields.NAME, Comparator.comparing(Release::getName, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        mutableReleaseMap.put(Release._Fields.VERSION, Comparator.comparing(Release::getVersion, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        mutableReleaseMap.put(Release._Fields.COMPONENT_ID, Comparator.comparing(Release::getComponentId, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        mutableReleaseMap.put(Release._Fields.RELEASE_DATE, Comparator.comparing(Release::getReleaseDate, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        mutableReleaseMap.put(Release._Fields.CREATED_ON, Comparator.comparing(Release::getCreatedOn, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        mutableReleaseMap.put(Release._Fields.CREATED_BY, Comparator.comparing(Release::getCreatedBy, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        releaseMap = Collections.unmodifiableMap(mutableReleaseMap);
        defaultReleaseComparator = releaseMap.get(Release._Fields.NAME);
    }

    public static <T> Comparator<T> generateComparator(Class<T> type) throws ResourceClassNotFoundException {
        return generateComparator(type, Collections.emptyList());
    }

    public static <T> Comparator<T> generateComparator(Class<T> type, String property) throws ResourceClassNotFoundException {
        return generateComparator(type, Collections.singletonList(property));
    }

    private static <T> Comparator<T> generateComparator(Class<T> type,  List<String> properties) throws ResourceClassNotFoundException {
        if (type.equals(Component.class)){
            List<Component._Fields> componentFields = properties.stream()
                    .map(Component._Fields::findByName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            return generateComparatorWithFields(type, componentFields);
        }
        if (type.equals(Release.class)) {
            List<Release._Fields> releaseFields = properties.stream()
                    .map(Release._Fields::findByName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            return generateComparatorWithFields(type, releaseFields);
        }
        throw new ResourceClassNotFoundException("No comparator for resource class with name " + type);
    }

    @SuppressWarnings("unchecked")
    private static <T> Comparator<T> generateComparatorWithFields(Class<T> type, List fields) throws ResourceClassNotFoundException {
        if (type.equals(Component.class)) {
            return componentComparator(fields);
        }
        if (type.equals(Release.class)) {
            return releaseComparator(fields);
        }
        throw new ResourceClassNotFoundException("No comparator for resource class with name " + type);
    }

    private static Comparator<Component> componentComparator(List<Component._Fields> fields) {
        return comparator(fields, componentMap, defaultComponentComparator);
    }

    private static Comparator<Release> releaseComparator(List<Release._Fields> fields) {
        return comparator(fields, releaseMap, defaultReleaseComparator);
    }

    private static <T extends TBase<T,F>, F extends TFieldIdEnum> Comparator<T> comparator(List<F> fields,
                                                                                    Map<F, Comparator<T>> map,
                                                                                    Comparator<T> defaultComparator) {
        Comparator<T> comparator = Comparator.comparing(x -> true);
        for (F field:fields) {
            Comparator<T> fieldComparator = map.get(field);
            if(fieldComparator != null) {
                comparator = comparator.thenComparing(fieldComparator);
            }
        }
        return comparator.thenComparing(defaultComparator);
    }
}

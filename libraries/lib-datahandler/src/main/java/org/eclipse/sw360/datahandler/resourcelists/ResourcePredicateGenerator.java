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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

class ResourcePredicateGenerator {

    private static final Logger log = LoggerFactory.getLogger(ResourcePredicateGenerator.class.getName());

    private static final Map<Component._Fields, Function<Component, String>> componentMap;
    static {
        componentMap = new HashMap<>();
        componentMap.put(Component._Fields.NAME, Component::getName);
        componentMap.put(Component._Fields.CREATED_ON, Component::getCreatedOn);
        componentMap.put(Component._Fields.CREATED_BY, Component::getCreatedBy);
        componentMap.put(Component._Fields.COMPONENT_TYPE, component -> Optional.ofNullable(component.getComponentType()).map(Object::toString).orElse(""));
        componentMap.put(Component._Fields.COMPONENT_OWNER, Component::getComponentOwner);
        componentMap.put(Component._Fields.OWNER_ACCOUNTING_UNIT, Component::getOwnerAccountingUnit);
        componentMap.put(Component._Fields.OWNER_GROUP, Component::getOwnerGroup);
        componentMap.put(Component._Fields.OWNER_COUNTRY, Component::getOwnerCountry);
    }

    <T extends TBase<?, ? extends TFieldIdEnum>> Predicate<T> predicateFromFilterMap(String resourceClass, Map<String, String> filter) {
        Predicate<T> finalPredicate = alwaysTruePredicate();
        for (Map.Entry<String, String> entry : filter.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            Predicate<T> keyValuePredicate = predicateForKey(resourceClass, key, value);
            finalPredicate = finalPredicate.and(keyValuePredicate);
        }
        return finalPredicate;
    }

    private <T extends TBase<?, ? extends TFieldIdEnum>> Predicate<T> predicateForKey(String resourceClass, String key, String value) {
        switch (resourceClass) {
            case SW360Constants.TYPE_COMPONENT:
                return (Predicate<T>) componentPredicateForKey(key, value);
            default:
                log.debug("Filters for " + resourceClass + " are not yet supported");
                return alwaysTruePredicate();
        }
    }

    private <Resource, R> Predicate<Resource> predicateFunctionForValue(Function<? super Resource, String> f, String value) {
        if (value == null) {
            return resource -> f.apply(resource) == null;
        }
        return resource -> (resource != null && f.apply(resource).toLowerCase().contains(value.toLowerCase()));
    }

    private <T extends TBase<?, ? extends TFieldIdEnum>> Predicate<T> alwaysTruePredicate() {
        return x -> true;
    }

    private Predicate<Component> componentPredicateForKey(String key, String value) {
        Component._Fields field = Component._Fields.findByName(key);
        if(field == null) {
            return alwaysTruePredicate();
        }
        Function<Component, String> f = componentMap.get(field);
        if(f != null) {
            return predicateFunctionForValue(f, value);
        }
        log.debug("Filter for component property '" + key + "' is not yet supported");
        return alwaysTruePredicate();
    }

}

/*
 * Copyright (c) Bosch Software Innovations GmbH 2018-2019.
 * Copyright (c) Bosch.IO GmbH 2020.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.antenna.sw360.client.adapter;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.components.SW360Component;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.components.SW360ComponentType;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.releases.SW360Release;

import java.util.Collections;

public class SW360ComponentAdapterUtils {

    private SW360ComponentAdapterUtils() {
    }

    public static void setComponentType(SW360Component component, boolean isProprietary) {
        if (isProprietary) {
            component.setComponentType(SW360ComponentType.INTERNAL);
        } else {
            component.setComponentType(SW360ComponentType.OSS);
        }
    }

    static SW360Component createFromRelease(SW360Release release) {
        SW360Component sw360Component = new SW360Component();
        sw360Component.setName(release.getName());
        setComponentType(sw360Component, release.isProprietary());
        sw360Component.setCategories(Collections.singleton("Antenna"));
        sw360Component.setHomepage(release.getHomepageUrl());
        return sw360Component;
    }

    /**
     * Validates the passed in component. Checks whether all mandatory fields
     * are set. If the component is valid, it is returned without changes.
     * Otherwise, an exception is thrown reporting the concrete validation
     * failure.
     *
     * @param component the component to validate
     * @return the validated component
     */
    static SW360Component validateComponent(SW360Component component) {
        if (StringUtils.isEmpty(component.getName())) {
            throw new IllegalArgumentException("Invalid component: missing property 'name'.");
        }
        if (component.getCategories() == null || component.getCategories().isEmpty()) {
            throw new IllegalArgumentException("Invalid component: missing property 'categories'.");
        }
        return component;
    }
}

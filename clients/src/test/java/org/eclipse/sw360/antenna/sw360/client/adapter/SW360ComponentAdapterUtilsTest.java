/*
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


import org.eclipse.sw360.antenna.sw360.client.rest.resource.components.SW360Component;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.components.SW360ComponentType;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.releases.SW360Release;
import org.junit.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class SW360ComponentAdapterUtilsTest {
    @Test
    public void testSetComponentTypeProprietary() {
        SW360Component component = new SW360Component();
        component.setName("test");

        SW360ComponentAdapterUtils.setComponentType(component, true);

        assertThat(component.getComponentType()).isEqualTo(SW360ComponentType.INTERNAL);
    }

    @Test
    public void testSetComponentTypeNonProprietary() {
        SW360Component component = new SW360Component();
        component.setName("test");

        SW360ComponentAdapterUtils.setComponentType(component, false);

        assertThat(component.getComponentType()).isEqualTo(SW360ComponentType.OSS);

    }

    @Test
    public void testCreateFromRelease() {
        final String homepageUrl = "dummy.page.url";
        SW360Release release = new SW360Release()
                .setName("test")
                .setProprietary(true)
                .setHomepageUrl(homepageUrl);

        SW360Component component = SW360ComponentAdapterUtils.createFromRelease(release);

        assertThat(component.getName()).isEqualTo(release.getName());
        assertThat(component.getComponentType()).isEqualTo(SW360ComponentType.INTERNAL);
        assertThat(component.getHomepage()).isEqualTo(homepageUrl);
    }

    @Test
    public void testValidateComponentValid() {
        SW360Component component = new SW360Component();
        component.setName("test");
        component.setCategories(Collections.singleton("Antenna"));

        assertThat(SW360ComponentAdapterUtils.validateComponent(component)).isSameAs(component);
    }

    @Test
    public void testValidateComponentNullName() {
        SW360Component component = new SW360Component();
        component.setCategories(Collections.singleton("Antenna"));

        try {
            SW360ComponentAdapterUtils.validateComponent(component);
            fail("Invalid component not detected");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("missing property 'name'");
        }
    }

    @Test
    public void testValidateComponentEmptyName() {
        SW360Component component = new SW360Component();
        component.setName("");
        component.setCategories(Collections.singleton("Antenna"));

        try {
            SW360ComponentAdapterUtils.validateComponent(component);
            fail("Invalid component not detected");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("missing property 'name'");
        }
    }

    @Test
    public void testValidateComponentNullCategories() {
        SW360Component component = new SW360Component();
        component.setName("component");

        try {
            SW360ComponentAdapterUtils.validateComponent(component);
            fail("Invalid component not detected");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("missing property 'categories'");
        }
    }

    @Test
    public void testValidateComponentEmptyCategories() {
        SW360Component component = new SW360Component();
        component.setCategories(Collections.emptySet());
        component.setName("component");

        try {
            SW360ComponentAdapterUtils.validateComponent(component);
            fail("Invalid component not detected");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("missing property 'categories'");
        }
    }
}
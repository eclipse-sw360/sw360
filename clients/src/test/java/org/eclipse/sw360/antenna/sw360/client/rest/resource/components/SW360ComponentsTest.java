/*
 * Copyright (c) Bosch Software Innovations GmbH 2019.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.antenna.sw360.client.rest.resource.components;

import org.eclipse.sw360.antenna.sw360.client.rest.resource.LinkObjects;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.SW360ResourcesTestUtils;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.Self;
import org.junit.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class SW360ComponentsTest extends SW360ResourcesTestUtils<SW360Component> {
    @Override
    public SW360Component prepareItem() {
        SW360Component sw360Component = new SW360Component();
        sw360Component.setName("Component Name");
        sw360Component.setComponentType(SW360ComponentType.COTS);
        sw360Component.setHomepage("componentName.org");
        sw360Component.setCreatedOn("2019-12-09");
        return sw360Component;
    }

    @Override
    public SW360Component prepareItemWithoutOptionalInput() {
        SW360Component sw360Component = new SW360Component();
        sw360Component.setName("Component Name");
        return sw360Component;
    }

    @Override
    public Class<SW360Component> getHandledClassType() {
        return SW360Component.class;
    }

    @Test
    public void testGetId() {
        final String id = "0123456789abcdef";
        final String selfLink = "https://www.sw360.test.org/item/" + id;
        SW360Component item = prepareItem();
        LinkObjects links = new LinkObjects();
        Self self = new Self(selfLink);
        links.setSelf(self);
        item.setLinks(links);

        assertThat(item.getId()).isEqualTo(id);
    }
}

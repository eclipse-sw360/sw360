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
package org.eclipse.sw360.antenna.sw360.client.rest.resource.components;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SW360ComponentTypeTest {
    private void checkFindByValue(SW360ComponentType componentType, int value) {
        SW360ComponentType result = SW360ComponentType.findByValue(value);

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(componentType);
        assertThat(result.getValue()).isEqualTo(value);
    }

    @Test
    public void testFindByValueInternal() {
        checkFindByValue(SW360ComponentType.INTERNAL, 0);
    }

    @Test
    public void testFindByValueOSS() {
        checkFindByValue(SW360ComponentType.OSS, 1);
    }

    @Test
    public void testFindByValueCOTS() {
        checkFindByValue(SW360ComponentType.COTS, 2);
    }

    @Test
    public void testFindByValueFreeSoftware() {
        checkFindByValue(SW360ComponentType.FREESOFTWARE, 3);
    }

    @Test
    public void testFindByValueInnerSource() {
        checkFindByValue(SW360ComponentType.INNER_SOURCE, 4);
    }

    @Test
    public void testFindByValueService() {
        checkFindByValue(SW360ComponentType.SERVICE, 5);
    }

    @Test
    public void testFindByValueUnknown() {
        SW360ComponentType result = SW360ComponentType.findByValue(815);

        assertThat(result).isNull();
    }
}
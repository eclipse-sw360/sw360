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
package org.eclipse.sw360.antenna.sw360.client.rest.resource.projects;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SW360ProjectTypeTest {
    private void checkFindByValue(SW360ProjectType projectType, int value) {
        SW360ProjectType result = SW360ProjectType.findByValue(value);

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(projectType);
        assertThat(result.getValue()).isEqualTo(value);
    }

    @Test
    public void testFindByValueCustomer() {
        checkFindByValue(SW360ProjectType.CUSTOMER, 0);
    }

    @Test
    public void testFindByValueInternal() {
        checkFindByValue(SW360ProjectType.INTERNAL, 1);
    }

    @Test
    public void testFindByValueProduct() {
        checkFindByValue(SW360ProjectType.PRODUCT, 2);
    }

    @Test
    public void testFindByValueService() {
        checkFindByValue(SW360ProjectType.SERVICE, 3);
    }

    @Test
    public void testFindByValueInnerSource() {
        checkFindByValue(SW360ProjectType.INNER_SOURCE, 4);
    }

    @Test
    public void testFindByValueUnknown() {
        SW360ProjectType result = SW360ProjectType.findByValue(111);

        assertThat(result).isNull();
    }
}
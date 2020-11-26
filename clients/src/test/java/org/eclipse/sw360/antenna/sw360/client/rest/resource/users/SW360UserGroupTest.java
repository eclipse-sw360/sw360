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
package org.eclipse.sw360.antenna.sw360.client.rest.resource.users;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SW360UserGroupTest {
    private void checkFindByValue(SW360UserGroup group, int value) {
        SW360UserGroup result = SW360UserGroup.findByValue(value);

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(group);
        assertThat(result.getValue()).isEqualTo(value);
    }

    @Test
    public void testFindByValueUser() {
        checkFindByValue(SW360UserGroup.USER, 0);
    }

    @Test
    public void testFindByValueAdmin() {
        checkFindByValue(SW360UserGroup.ADMIN, 1);
    }

    @Test
    public void testFindByValueClearingAdmin() {
        checkFindByValue(SW360UserGroup.CLEARING_ADMIN, 2);
    }

    @Test
    public void testFindByValueECCAdmin() {
        checkFindByValue(SW360UserGroup.ECC_ADMIN, 3);
    }

    @Test
    public void testFindByValueSecurityAdmin() {
        checkFindByValue(SW360UserGroup.SECURITY_ADMIN, 4);
    }

    @Test
    public void testFindByValueSW360Admin() {
        checkFindByValue(SW360UserGroup.SW360_ADMIN, 5);
    }

    @Test
    public void testFindByValueUnknown() {
        SW360UserGroup result = SW360UserGroup.findByValue(1000);

        assertThat(result).isNull();
    }
}
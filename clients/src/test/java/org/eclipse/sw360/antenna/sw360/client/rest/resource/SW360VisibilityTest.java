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
package org.eclipse.sw360.antenna.sw360.client.rest.resource;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SW360VisibilityTest {
    private static void checkFindByValue(SW360Visibility visibility, int value) {
        SW360Visibility result = SW360Visibility.findByValue(value);

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(visibility);
        assertThat(result.getValue()).isEqualTo(value);
    }

    @Test
    public void testFindByValuePrivate() {
        checkFindByValue(SW360Visibility.PRIVATE, 0);
    }

    @Test
    public void testFindByValueMeAndModerators() {
        checkFindByValue(SW360Visibility.ME_AND_MODERATORS, 1);
    }

    @Test
    public void testFindByValueBusinessUnitAndModerators() {
        checkFindByValue(SW360Visibility.BUISNESSUNIT_AND_MODERATORS, 2);
    }

    @Test
    public void testFindByValueEveryone() {
        checkFindByValue(SW360Visibility.EVERYONE, 3);
    }

    @Test
    public void testFindByValueUnknown() {
        SW360Visibility result = SW360Visibility.findByValue(42);

        assertThat(result).isNull();
    }
}
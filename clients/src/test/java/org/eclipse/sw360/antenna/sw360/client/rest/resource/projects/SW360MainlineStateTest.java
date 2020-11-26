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

public class SW360MainlineStateTest {
    private static void checkFindByValue(SW360MainlineState state, int value) {
        SW360MainlineState result = SW360MainlineState.findByValue(value);

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(state);
        assertThat(result.getValue()).isEqualTo(value);
    }

    @Test
    public void testFindByValueOpen() {
        checkFindByValue(SW360MainlineState.OPEN, 0);
    }

    @Test
    public void testFindByValueMainline() {
        checkFindByValue(SW360MainlineState.MAINLINE, 1);
    }

    @Test
    public void testFindByValueSpecific() {
        checkFindByValue(SW360MainlineState.SPECIFIC, 2);
    }

    @Test
    public void testFindByValuePhaseOut() {
        checkFindByValue(SW360MainlineState.PHASEOUT, 3);
    }

    @Test
    public void testFindByValueDenied() {
        checkFindByValue(SW360MainlineState.DENIED, 4);
    }

    @Test
    public void testFindByValueUnknown() {
        SW360MainlineState result = SW360MainlineState.findByValue(77);

        assertThat(result).isNull();
    }
}
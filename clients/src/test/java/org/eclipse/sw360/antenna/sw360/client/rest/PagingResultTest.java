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
package org.eclipse.sw360.antenna.sw360.client.rest;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class PagingResultTest {
    @Test
    public void testEquals() {
        EqualsVerifier.forClass(PagingResult.class)
                .withNonnullFields("result")
                .verify();
    }

    @Test
    public void testDefensiveCopyOfResultList() {
        List<Object> data = new ArrayList<>();
        data.add("entry1");

        PagingResult<Object> result = new PagingResult<>(data, null, null);
        data.add("entry2");
        assertThat(result.getResult()).containsOnly("entry1");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetResultUnmodifiable() {
        PagingResult<Object> result = new PagingResult<>(Arrays.asList("a", "b", "c"), null, null);

        result.getResult().add("d");
    }

    @Test(expected = NullPointerException.class)
    public void testCreationFailsForNullResultList() {
        new PagingResult<Object>(null, null, null);
    }
}
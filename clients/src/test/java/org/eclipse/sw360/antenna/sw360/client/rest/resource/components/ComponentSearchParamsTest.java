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

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentSearchParamsTest {
    @Test
    public void testEquals() {
        EqualsVerifier.forClass(ComponentSearchParams.class)
                .withNonnullFields("orderClauses", "fields")
                .verify();
    }

    @Test
    public void testDefensiveCopyOfOrderClauses() {
        ComponentSearchParams.Builder builder = ComponentSearchParams.builder();
        ComponentSearchParams params = builder
                .orderAscending("foo")
                .build();

        builder.orderDescending("bar");
        assertThat(params.getOrderClauses()).containsOnly("foo,ASC");
    }

    @Test
    public void testDefensiveCopyOfFields() {
        ComponentSearchParams.Builder builder = ComponentSearchParams.builder();
        ComponentSearchParams params = builder.retrieveFields("foo")
                .build();

        builder.retrieveFields("bar", "baz");
        assertThat(params.getFields()).containsOnly("foo");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testOrderClausesUnmodifiable() {
        ComponentSearchParams params = ComponentSearchParams.builder()
                .orderDescending("foo")
                .orderAscending("bar")
                .build();

        params.getOrderClauses().add("another order");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testFieldsUnmodifiable() {
        ComponentSearchParams params = ComponentSearchParams.builder()
                .retrieveFields("a", "b", "c")
                .build();

        params.getFields().add("z");
    }
}
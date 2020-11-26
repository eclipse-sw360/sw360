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
package org.eclipse.sw360.antenna.sw360.client.rest.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class SW360ResourcesTestUtils<T extends SW360HalResource<?,?>> {

    public abstract T prepareItem();
    public abstract T prepareItemWithoutOptionalInput();

    public abstract Class<T> getHandledClassType();

    @Test
    public void serializationTest() throws Exception {
        final T item = prepareItem();
        serialize(item);
    }

    @Test
    public void serializationTestWithoutOptionalInput() throws JsonProcessingException {
        final T item = prepareItemWithoutOptionalInput();
        serialize(item);
    }

    private void serialize(T item) throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        final String jsonBody = objectMapper.writeValueAsString(item);
        final T deserialized = objectMapper.readValue(jsonBody, getHandledClassType());

        assertThat(deserialized.getEmbedded())
                .isEqualTo(item.getEmbedded());
        assertThat(deserialized)
                .isEqualTo(item);
    }

    @Test
    public void equalsTest() {
        EqualsVerifier.forClass(getHandledClassType())
                .withRedefinedSuperclass()
                .suppress(Warning.NONFINAL_FIELDS)
                .verify();
    }

    @Test
    public void equalsTestWithoutOptionalInput() {
        assertThat(prepareItemWithoutOptionalInput())
                .isEqualTo(prepareItemWithoutOptionalInput());
    }

    @Test
    public void testSelfLinkUninitialized() {
        assertThat(prepareItem().getSelfLink()).isNull();
    }

    @Test
    public void testIdUninitialized() {
        assertThat(prepareItem().getId()).isNull();
    }
}

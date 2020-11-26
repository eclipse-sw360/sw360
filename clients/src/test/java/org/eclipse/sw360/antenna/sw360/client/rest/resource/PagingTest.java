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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.components.SW360ComponentList;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

public class PagingTest {
    @Test
    public void testEquals() {
        EqualsVerifier.forClass(Paging.class)
                .verify();
    }

    @Test
    public void testToString() {
        Paging paging = new Paging(42, 5, 1024, 25);

        String s = paging.toString();
        assertThat(s).contains("size=" + paging.getSize(), "number=" + paging.getNumber(),
                "totalElements=" + paging.getTotalElements(), "totalPages=" + paging.getTotalPages());
    }

    @Test
    public void testDeserializeFromJson() throws IOException {
        URL testFile = getClass().getResource("/__files/all_components_paging.json");
        Paging expPaging = new Paging(5, 1, 12, 3);
        ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        SW360ComponentList componentList = mapper.readValue(testFile, SW360ComponentList.class);
        assertThat(componentList.getPage()).isEqualTo(expPaging);
    }
}

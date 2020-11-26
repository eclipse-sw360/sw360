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
import nl.jqno.equalsverifier.Warning;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.components.SW360ComponentList;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

public class PagingLinkObjectsTest {
    /**
     * Template for the base URL used to request a specific page.
     */
    private static final String PAGING_URL = "https://sw360.test.com//resource/api/components?page=%d&page_entries=5";

    /**
     * Checks whether a paging link contains the expected URI.
     *
     * @param index the index of the expected target page
     * @param link  the link to be checked
     */
    private static void assertPage(int index, Self link) {
        String expUri = String.format(PAGING_URL, index);
        assertThat(link.getHref()).isEqualTo(expUri);
    }

    @Test
    public void testDeserializeFromJson() throws IOException {
        URL testFile = getClass().getResource("/__files/all_components_paging.json");
        ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        SW360ComponentList componentList = mapper.readValue(testFile, SW360ComponentList.class);
        PagingLinkObjects links = componentList.getLinks();
        assertPage(0, links.getFirst());
        assertPage(1, links.getPrevious());
        assertPage(2, links.getNext());
        assertPage(3, links.getLast());
    }

    @Test
    public void testEquals() {
        EqualsVerifier.forClass(PagingLinkObjects.class)
                .withRedefinedSuperclass()
                .suppress(Warning.NONFINAL_FIELDS)
                .verify();
    }
}

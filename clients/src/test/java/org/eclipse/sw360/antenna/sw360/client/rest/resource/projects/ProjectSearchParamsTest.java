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

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ProjectSearchParamsTest {
    @Test
    public void testEquals() {
        EqualsVerifier.forClass(ProjectSearchParams.class)
                .verify();
    }

    @Test
    public void testToString() {
        ProjectSearchParams params = ProjectSearchParams.builder()
                .withName("projectName")
                .withType(SW360ProjectType.INNER_SOURCE)
                .withTag("projectTag")
                .withBusinessUnit("projectUnit")
                .build();
        String s = params.toString();

        assertThat(s).contains(params.getName(), params.getBusinessUnit(), params.getType().toString(),
                params.getTag());
    }
}
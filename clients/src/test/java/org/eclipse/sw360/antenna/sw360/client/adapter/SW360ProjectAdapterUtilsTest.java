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
package org.eclipse.sw360.antenna.sw360.client.adapter;

import org.eclipse.sw360.antenna.sw360.client.rest.resource.projects.SW360Project;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SW360ProjectAdapterUtilsTest {

    private static final String PROJECT_VERSION = "1.0-projectVersion";
    private static final String PROJECT_NAME = "projectName";

    @Test
    public void testIsValidProjectWithValidProject() {
        SW360Project project = new SW360Project()
                .setName(PROJECT_NAME)
                .setVersion(PROJECT_VERSION);

        boolean validComponent = SW360ProjectAdapterUtils.isValidProject(project);

        assertThat(validComponent).isTrue();
    }

    @Test
    public void testIsValidProjectWithNoVersion() {
        SW360Project project = new SW360Project();
        project.setName(PROJECT_NAME);

        boolean validComponent = SW360ProjectAdapterUtils.isValidProject(project);

        assertThat(validComponent).isFalse();
    }

    @Test
    public void testIsValidProjectWithNoName() {
        SW360Project project = new SW360Project();
        project.setVersion(PROJECT_VERSION);

        boolean validComponent = SW360ProjectAdapterUtils.isValidProject(project);

        assertThat(validComponent).isFalse();
    }

    @Test
    public void testHasEqualCoordinatesTrue() {
        SW360Project project = new SW360Project()
                .setName(PROJECT_NAME)
                .setVersion(PROJECT_VERSION);

        boolean hasEqualCoordinates = SW360ProjectAdapterUtils.hasEqualCoordinates(project, PROJECT_NAME, PROJECT_VERSION);

        assertThat(hasEqualCoordinates).isTrue();
    }

    @Test
    public void testHasEqualCoordinatesFalseByVersion() {
        SW360Project project = new SW360Project()
                .setName(PROJECT_NAME)
                .setVersion(PROJECT_VERSION);

        boolean hasEqualCoordinates = SW360ProjectAdapterUtils.hasEqualCoordinates(project, PROJECT_NAME, PROJECT_VERSION + "-no");

        assertThat(hasEqualCoordinates).isFalse();
    }

    @Test
    public void testHasEqualCoordinatesFalseByName() {
        SW360Project project = new SW360Project()
                .setName(PROJECT_NAME)
                .setVersion(PROJECT_VERSION);

        boolean hasEqualCoordinates = SW360ProjectAdapterUtils.hasEqualCoordinates(project, PROJECT_NAME + "-no", PROJECT_VERSION);

        assertThat(hasEqualCoordinates).isFalse();
    }
}

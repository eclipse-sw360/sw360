/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.exporter;

import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Created by heydenrb on 06.11.15.
 */
@RunWith(MockitoJUnitRunner.class)
public class ProjectExporterTest {
    @Mock
    ComponentService.Iface componentClient;

    @Mock
    ProjectService.Iface projectClient;

    @Mock
    User user;

    @Test
    public void testEveryRenderedProjectFieldHasAHeader() throws Exception {
        ProjectExporter exporter = new ProjectExporter(componentClient,
                projectClient, user, Collections.emptyList(), false);
        assertThat(ProjectExporter.PROJECT_RENDERED_FIELDS.size(), is(ProjectExporter.HEADERS.size()));
    }
}

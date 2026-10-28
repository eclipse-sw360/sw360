/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.exporter;

import org.eclipse.sw360.datahandler.services.users.User;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

/**
 * Created by heydenrb on 06.11.15.
 */
@RunWith(MockitoJUnitRunner.class)
public class ProjectExporterTest {
    @Mock
    ComponentExportClient componentClient;

    @Mock
    User user;

    @Test
    public void testEveryRenderedProjectFieldHasAHeader() throws Exception {
        ProjectExporter exporter = new ProjectExporter(componentClient,
                (ids, actor) -> Collections.emptyList(),
                user, Collections.emptyList(), false);
        Assert.assertEquals(ProjectExporter.HEADERS.size(), ProjectExporter.PROJECT_RENDERED_FIELDS.size());
    }
}

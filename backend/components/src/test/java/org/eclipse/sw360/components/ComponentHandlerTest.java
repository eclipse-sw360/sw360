/*
 * Copyright Siemens AG, 2013-2015, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.components;

import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.common.DatabaseSettingsTest;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.components.Component;
import org.eclipse.sw360.datahandler.services.components.ComponentType;
import org.eclipse.sw360.datahandler.services.components.ExternalTool;
import org.eclipse.sw360.datahandler.services.components.ExternalToolProcess;
import org.eclipse.sw360.datahandler.services.components.ExternalToolProcessStep;
import org.eclipse.sw360.datahandler.services.components.Release;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.users.User;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;

import static org.eclipse.sw360.datahandler.TestUtils.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.fail;

/**
 * @author daniele.fognini@tngtech.com
 */
public class ComponentHandlerTest {

    private ComponentHandler componentHandler;
    private User adminUser = TestUtils.getAdminUser(getClass());

    @Before
    public void setUp() throws Exception {
        assertTestDbNames();
        deleteAllDatabases();
        componentHandler = new ComponentHandler(DatabaseSettingsTest.getConfiguredClient(),
                DatabaseSettingsTest.COUCH_DB_DATABASE, DatabaseSettingsTest.COUCH_DB_CHANGELOGS,
                DatabaseSettingsTest.COUCH_DB_ATTACHMENTS);
    }

    @After
    public void tearDown() throws Exception {
        deleteAllDatabases();
    }

    @Test
    public void testGetByUploadId() throws Exception {

        Component originalComponent = new Component().setName("name").setDescription("a desc")
                .setComponentType(ComponentType.OSS);
        originalComponent.setCategories(new HashSet<>(Collections.singleton("Library")));
        String componentId = componentHandler.addComponent(originalComponent, adminUser).getId();

        Release release = new Release().setName("name").setVersion("version").setComponentId(componentId);
        ExternalToolProcess etp = new ExternalToolProcess();
        etp.setExternalTool(ExternalTool.FOSSOLOGY);
        release.setExternalToolProcesses(new HashSet<>(Collections.singletonList(etp)));
        ExternalToolProcessStep etps = new ExternalToolProcessStep();
        // do not use FossologyUtils.FOSSOLOGY_STEP_NAME_UPLOAD so that test fails when
        // it gets refactored and no one thinks of adjusting the view definition in
        // ComponentRepository
        etps.setStepName("01_upload");
        etps.setProcessStepIdInTool("12345");
        etp.setProcessSteps(Collections.singletonList(etps));
        String releaseId = componentHandler.addRelease(release, adminUser).getId();

        Component component = componentHandler.getComponentForReportFromFossologyUploadId("12345");

        assertThat(component, is(not(nullValue())));
        assertThat(component.getId(), is(originalComponent.getId()));
        assertThat(component.getName(), is(originalComponent.getName()));
        assertThat(component.getDescription(), is(originalComponent.getDescription()));

        assertThat(componentHandler.getReleaseById(releaseId, adminUser), is(not(nullValue())));
        assertThat(componentHandler.getComponentById(componentId, adminUser), is(not(nullValue())));

        assertThat(componentHandler.deleteRelease(releaseId, adminUser), is(RequestStatus.SUCCESS));
        assertThat(componentHandler.deleteComponent(componentId, adminUser), is(RequestStatus.SUCCESS));

        try {
            componentHandler.getReleaseById(releaseId, adminUser);
            fail("expected exception not thrown");
        } catch (SW360Exception e) {
            assertThat(e.getWhy(), containsString("Could not fetch"));
        }
        try {
            componentHandler.getComponentById(componentId, adminUser);
            fail("expected exception not thrown");
        } catch (SW360Exception e) {
            assertThat(e.getWhy(), containsString("Could not fetch"));
        }
    }
}

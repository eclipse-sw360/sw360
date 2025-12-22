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

import com.ibm.cloud.cloudant.v1.Cloudant;
import org.eclipse.sw360.datahandler.spring.CouchDbContextInitializer;
import org.eclipse.sw360.datahandler.spring.DatabaseConfig;
import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.components.*;
import org.eclipse.sw360.datahandler.thrift.users.User;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Set;

import static org.eclipse.sw360.datahandler.TestUtils.*;
import static org.eclipse.sw360.datahandler.thrift.components.Component._Fields.DESCRIPTION;
import static org.eclipse.sw360.datahandler.thrift.components.Component._Fields.ID;
import static org.eclipse.sw360.datahandler.thrift.components.Component._Fields.NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.fail;

/**
 * @author daniele.fognini@tngtech.com
 */
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(
        classes = {DatabaseConfig.class},
        initializers = {CouchDbContextInitializer.class}
)
@ActiveProfiles("test")
public class ComponentHandlerTest {

    @Autowired
    Cloudant client;

    @Autowired
    private ComponentHandler componentHandler;
    private User adminUser = TestUtils.getAdminUser(getClass());

    @Autowired
    @Qualifier("COUCH_DB_ALL_NAMES")
    private Set<String> allDatabaseNames;

    @Before
    public void setUp() throws Exception {
        assertTestDbNames(allDatabaseNames);
    }

    @After
    public void tearDown() throws Exception {
        TestUtils.deleteAllDatabases(client, allDatabaseNames);
    }

    @Test
    public void testGetByUploadId() throws Exception {

        Component originalComponent = new Component("name").setDescription("a desc").setComponentType(ComponentType.OSS);
        originalComponent.addToCategories("Library");
        String componentId = componentHandler.addComponent(originalComponent, adminUser).getId();

        Release release = new Release("name", "version", componentId);
        ExternalToolProcess etp = new ExternalToolProcess();
        etp.setExternalTool(ExternalTool.FOSSOLOGY);
        release.addToExternalToolProcesses(etp);
        ExternalToolProcessStep etps = new ExternalToolProcessStep();
        // do not use FossologyUtils.FOSSOLOGY_STEP_NAME_UPLOAD so that test fails when
        // it gets refactored and no one thinks of adjusting the view definition in
        // ComponentRepository
        etps.setStepName("01_upload");
        etps.setProcessStepIdInTool("12345");
        etp.addToProcessSteps(etps);
        String releaseId = componentHandler.addRelease(release, adminUser).getId();

        Component component = componentHandler.getComponentForReportFromFossologyUploadId("12345");

        assertThat(component, is(not(nullValue())));
        assertThat(component, is(equalTo(originalComponent, restrictedToFields(ID, NAME, DESCRIPTION))));

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

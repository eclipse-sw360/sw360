/*
 * Copyright Siemens Healthineers GmBH, 2023. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.cyclonedx;

import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.db.ComponentDatabaseHandler;
import org.eclipse.sw360.datahandler.db.PackageDatabaseHandler;
import org.eclipse.sw360.datahandler.db.ProjectDatabaseHandler;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class CycloneDxBOMImporterTest {

    @Mock
    private ProjectDatabaseHandler projectDatabaseHandler;
    @Mock
    private ComponentDatabaseHandler componentDatabaseHandler;
    @Mock
    private PackageDatabaseHandler packageDatabaseHandler;
    @Mock
    private AttachmentConnector attachmentConnector;
    @Mock
    private User user;

    private CycloneDxBOMImporter importer;

    @Before
    public void setUp() {
        // Instantiate the class with mocked dependencies
        importer = new CycloneDxBOMImporter(projectDatabaseHandler, componentDatabaseHandler,
                packageDatabaseHandler, attachmentConnector, user);
    }

    /**
     * Helper method to call the private createComponent method using Reflection
     */
    private Component callCreateComponent(org.cyclonedx.model.Component cdxComponent) throws Exception {
        Method method = CycloneDxBOMImporter.class.getDeclaredMethod("createComponent", org.cyclonedx.model.Component.class);
        method.setAccessible(true);
        return (Component) method.invoke(importer, cdxComponent);
    }

    @Test
    public void testCreateComponent_ConcatenatesGroupAndName() throws Exception {
        // Setup: A component with distinct group and name (e.g. NPM scoped package)
        org.cyclonedx.model.Component cdxComponent = new org.cyclonedx.model.Component();
        cdxComponent.setGroup("@sentry");
        cdxComponent.setName("browser");
        cdxComponent.setVersion("1.0.0");

        // Execute
        Component result = callCreateComponent(cdxComponent);

        // Verify: The SW360 component name should be "Group/Name"
        assertEquals("@sentry/browser", result.getName());
    }

    @Test
    public void testCreateComponent_GuardsAgainstDoublePrefix() throws Exception {
        // Setup: A component where the generator already included the group in the name
        org.cyclonedx.model.Component cdxComponent = new org.cyclonedx.model.Component();
        cdxComponent.setGroup("@sentry");
        cdxComponent.setName("@sentry/browser"); // Already prefixed!
        cdxComponent.setVersion("1.0.0");

        // Execute
        Component result = callCreateComponent(cdxComponent);

        // Verify: The logic should DETECT the prefix and NOT add it again
        assertEquals("@sentry/browser", result.getName());
    }

    @Test
    public void testCreateComponent_HandlesMissingGroup() throws Exception {
        // Setup: A standard component with no group
        org.cyclonedx.model.Component cdxComponent = new org.cyclonedx.model.Component();
        cdxComponent.setGroup(null);
        cdxComponent.setName("lodash");
        cdxComponent.setVersion("4.17.21");

        // Execute
        Component result = callCreateComponent(cdxComponent);

        // Verify: Name remains unchanged
        assertEquals("lodash", result.getName());
    }
}

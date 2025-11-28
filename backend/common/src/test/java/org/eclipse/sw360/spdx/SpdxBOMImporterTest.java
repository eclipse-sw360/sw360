/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.spdx;

import com.ibm.cloud.cloudant.v1.Cloudant;
import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.spring.CouchDbContextInitializer;
import org.eclipse.sw360.datahandler.spring.DatabaseConfig;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.InputStream;
import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(
        classes = {DatabaseConfig.class},
        initializers = {CouchDbContextInitializer.class}
)
@ActiveProfiles("test")
public class SpdxBOMImporterTest {

    private InputStream inputStream;
    private AttachmentContent attachmentContent;

    @MockitoBean
    private SpdxBOMImporterSink spdxBOMImporterSink;

    @Autowired
    private SpdxBOMImporter spdxBOMImporter;

    @Mock
    private User user;

    @Autowired
    private Cloudant client;

    @Autowired
    @Qualifier("COUCH_DB_ALL_NAMES")
    private Set<String> allDatabaseNames;

    @Before
    public void before() throws Exception {
        inputStream = getClass()
             .getClassLoader().getResourceAsStream("bom.spdx.rdf");

        when(spdxBOMImporterSink.addProject(any(Project.class))).then(i -> {
            Project project = i.getArgument(0);
            return new SpdxBOMImporterSink.Response(project.getName() + "-" + project.getVersion());
        });
        when(spdxBOMImporterSink.addRelease(any(Release.class))).then(i -> {
            Release release = i.getArgument(0);
            return new SpdxBOMImporterSink.Response(release.getName() + "-" + release.getVersion());
        });
        when(spdxBOMImporterSink.addComponent(any(Component.class))).then(i -> {
            Component component = i.getArgument(0);
            return new SpdxBOMImporterSink.Response(component.getName());
        });
        when(spdxBOMImporterSink.searchComponent(anyString())).thenReturn(new Component());
        when(spdxBOMImporterSink.searchRelease(anyString())).thenReturn(new Release());

        attachmentContent = new AttachmentContent();
        attachmentContent.setFilename("attchmentContentFilename.rdf");
        attachmentContent.setContentType("contentType");
        attachmentContent.setId("attachmentContentId");
    }

    @After
    public void after() throws Exception {
        if(inputStream != null) {
            inputStream.close();
        }
        TestUtils.deleteAllDatabases(client, allDatabaseNames);
    }

    @Test
    public void testProject() throws  Exception {
        final RequestSummary requestSummary = spdxBOMImporter.importSpdxBOMAsProject(inputStream, attachmentContent, user);
        assertNotNull(requestSummary);

        verify(spdxBOMImporterSink, times(1)).addProject(ArgumentMatchers.any());
        verify(spdxBOMImporterSink, times(3)).addComponent(ArgumentMatchers.any());
        verify(spdxBOMImporterSink, times(3)).addRelease(ArgumentMatchers.any());
    }

    @Test
    public void testRelease() throws  Exception {
        final RequestSummary requestSummary = spdxBOMImporter.importSpdxBOMAsRelease(inputStream, attachmentContent, user);
        assertNotNull(requestSummary);

        verify(spdxBOMImporterSink, times(4)).addComponent(ArgumentMatchers.any());
        verify(spdxBOMImporterSink, times(4)).addRelease(ArgumentMatchers.any());
    }
}

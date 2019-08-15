/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.fossology;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.common.FossologyUtils;
import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.db.ConfigContainerRepository;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.attachments.*;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService.Iface;
import org.eclipse.sw360.datahandler.thrift.components.ExternalToolProcess;
import org.eclipse.sw360.datahandler.thrift.components.ExternalToolProcessStatus;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.fossology.config.FossologyRestConfig;
import org.eclipse.sw360.fossology.rest.FossologyRestClient;

import org.apache.thrift.TException;
import org.hamcrest.Matchers;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.springframework.web.client.RestTemplate;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.eclipse.sw360.datahandler.common.DatabaseSettings.COUCH_DB_CONFIG;
import static org.eclipse.sw360.datahandler.common.DatabaseSettings.getConfiguredHttpClient;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * Can be executed manually by removing the @Ignore statement at class level.
 * Change the first few static config fields according to your running fossology
 * instance.
 */
@Ignore
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FossologyHandlerLocalhostIntegrationTest {

    private static final String CONFIG_URL_VALUE = "http://localhost:8081/repo/api/v1/";
    private static final String CONFIG_TOKEN_VALUE = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJsb2NhbGhvc3QiLCJhdWQiOiJsb2NhbGhvc3QiLCJleHAiOjE1NzA3NTE5OTksIm5iZiI6MTU2ODA3MzYwMCwianRpIjoiTkM0eiIsInNjb3BlIjoid3JpdGUifQ.WU63wqKXqf0byMBh9Bw0mU0Obt-1srw5VwJq2R9UXNg";
    private static final String CONFIG_FOLDER_ID_VALUE = "3";

    private static Release sharedRelease;

    private FossologyHandler uut;

    private ThriftClients thriftClients;

    private AttachmentConnector attachmentConnector;

    @BeforeClass
    public static void setupClass() {
        // we need to keep the release consistent to be able to move forward with the
        // same ExternalToolProcess over all process steps
        sharedRelease = new Release();
        sharedRelease.setId("42");
    }

    @Before
    public void setup() throws MalformedURLException, TException {
        ObjectMapper objectMapper = new ObjectMapper();

        FossologyRestConfig restConfig = new FossologyRestConfig(
                new ConfigContainerRepository(new DatabaseConnector(getConfiguredHttpClient(), COUCH_DB_CONFIG)));
        Map<String, Set<String>> configKeyToValues = new HashMap<>();
        configKeyToValues.put(FossologyRestConfig.CONFIG_KEY_URL,
                Stream.of(CONFIG_URL_VALUE).collect(Collectors.toSet()));
        configKeyToValues.put(FossologyRestConfig.CONFIG_KEY_TOKEN,
                Stream.of(CONFIG_TOKEN_VALUE).collect(Collectors.toSet()));
        configKeyToValues.put(FossologyRestConfig.CONFIG_KEY_FOLDER_ID,
                Stream.of(CONFIG_FOLDER_ID_VALUE).collect(Collectors.toSet()));
        restConfig.update(new ConfigContainer(ConfigFor.FOSSOLOGY_REST, configKeyToValues));

        RestTemplate restTemplate = new RestTemplate();
        FossologyRestClient fossologyRestClient = new FossologyRestClient(objectMapper, restConfig, restTemplate);

        thriftClients = mock(ThriftClients.class);

        attachmentConnector = mock(AttachmentConnector.class);

        uut = new FossologyHandler(thriftClients, restConfig, fossologyRestClient, attachmentConnector);
    }

    @Test
    public void testSetFossologyConfig() throws TException {
        // given:
        Map<String, Set<String>> configKeyToValues = new HashMap<>();
        configKeyToValues.put("url", Stream.of("http://newUrl.org").collect(Collectors.toSet()));
        configKeyToValues.put("token", Stream.of("newToken").collect(Collectors.toSet()));
        configKeyToValues.put("folderId", Stream.of("21").collect(Collectors.toSet()));
        ConfigContainer newConfig = new ConfigContainer(ConfigFor.FOSSOLOGY_REST, configKeyToValues);

        // when:
        RequestStatus actual = uut.setFossologyConfig(newConfig);

        // then:
        assertThat(actual, is(RequestStatus.SUCCESS));
    }

    @Test
    public void testGetFossologyConfig() throws TException {
        // given:

        // when:
        ConfigContainer actual = uut.getFossologyConfig();

        // then:
        assertThat(actual, notNullValue(ConfigContainer.class));
        assertThat(actual.getConfigKeyToValues().keySet(), Matchers.containsInAnyOrder("url", "token", "folderId"));
        assertThat(actual.getConfigKeyToValues().get("url").size(), is(1));
        assertThat(actual.getConfigKeyToValues().get("token").size(), is(1));
        assertThat(actual.getConfigKeyToValues().get("folderId").size(), is(1));
        assertThat(actual.getConfigKeyToValues().get("url").iterator().next(), is(CONFIG_URL_VALUE));
        assertThat(actual.getConfigKeyToValues().get("token").iterator().next(), is(CONFIG_TOKEN_VALUE));
        assertThat(actual.getConfigKeyToValues().get("folderId").iterator().next(), is(CONFIG_FOLDER_ID_VALUE));
    }

    @Test
    public void testCheckConnection() throws TException {
        // given:

        // when:
        RequestStatus actual = uut.checkConnection();

        // then:
        assertThat(actual, is(RequestStatus.SUCCESS));
    }

    // FIXME:
    // test preconditions in normal test (only mocks needed, no integration):
    // - more than one fossology process
    // - more than one source attachment
    // - new source attachment, different from existing fossology process

    private void prepareValidPreconditions(User user) throws FileNotFoundException, TException {
        Attachment sourceAttachment = new Attachment("84", "commons-lang3-3.5-sources.jar");
        sourceAttachment.setAttachmentContentId("84c");

        AttachmentContent sourceAttachmentContent = new AttachmentContent("commons-lang3-3.5-sources.jar");

        InputStream attachmentInputStream = new FileInputStream("src/test/resources/commons-lang3-3.5-sources.jar");
        when(attachmentConnector.getAttachmentContent("84c")).thenReturn(sourceAttachmentContent);
        when(attachmentConnector.getAttachmentStream(Mockito.eq(sourceAttachmentContent), any(), any()))
                .thenReturn(attachmentInputStream);

        Iface componentClient = mock(Iface.class);
        when(componentClient.getReleaseById(sharedRelease.getId(), user)).thenReturn(sharedRelease);
        when(componentClient.getSourceAttachments(sharedRelease.getId()))
                .thenReturn(Stream.of(sourceAttachment).collect(Collectors.toSet()));

        when(thriftClients.makeComponentClient()).thenReturn(componentClient);
    }

    @Test
    public void test_01_noFossologyProcessYet_soUploadSource() throws TException, FileNotFoundException {
        // given:
        User user = TestUtils.getAdminUser(getClass());
        prepareValidPreconditions(user);

        // when:
        ExternalToolProcess actual = uut.process(sharedRelease.getId(), user);

        // then:
        assertNotNull(actual);
        assertThat(actual.getProcessSteps(), hasSize(1));
        assertThat(actual.getProcessSteps().get(0).getStepName(), is(FossologyUtils.FOSSOLOGY_STEP_NAME_UPLOAD));
        assertThat(actual.getProcessSteps().get(0).getStepStatus(), is(ExternalToolProcessStatus.DONE));
        assertThat(actual.getProcessSteps().get(0).getResult(), not(isEmptyString()));

        // prepare next test:
        sharedRelease.setExternalToolProcesses(Stream.of(actual).collect(Collectors.toSet()));
    }

    @Test
    public void test_02_uploadFinishedSynchronously_soStartScan()
            throws TException, FileNotFoundException, InterruptedException {
        // given:
        User user = TestUtils.getAdminUser(getClass());
        prepareValidPreconditions(user);

        // when:
        ExternalToolProcess actual = sharedRelease.getExternalToolProcesses().iterator().next();
        while (actual.getProcessSteps().size() < 2
                || actual.getProcessSteps().get(1).getStepStatus().equals(ExternalToolProcessStatus.NEW)) {
            // give fossology some time to process the upload
            Thread.sleep(1_000l);
            actual = uut.process(sharedRelease.getId(), user);
        }

        // then:
        assertNotNull(actual);
        assertThat(actual.getProcessSteps(), hasSize(2));
        assertThat(actual.getProcessSteps().get(1).getStepName(), is(FossologyUtils.FOSSOLOGY_STEP_NAME_SCAN));
        assertThat(actual.getProcessSteps().get(1).getStepStatus(), is(ExternalToolProcessStatus.IN_WORK));
        assertNull(actual.getProcessSteps().get(1).getResult());

        // prepare next test:
        sharedRelease.setExternalToolProcesses(Stream.of(actual).collect(Collectors.toSet()));
    }

    @Test
    public void test_03_scanRunning() throws TException, FileNotFoundException, InterruptedException {
        // given:
        User user = TestUtils.getAdminUser(getClass());
        prepareValidPreconditions(user);

        // when:
        ExternalToolProcess actual = uut.process(sharedRelease.getId(), user);

        // then:
        assertNotNull(actual);
        assertThat(actual.getProcessSteps(), hasSize(2));
        assertThat(actual.getProcessSteps().get(1).getStepName(), is(FossologyUtils.FOSSOLOGY_STEP_NAME_SCAN));
        assertThat(actual.getProcessSteps().get(1).getStepStatus(), is(ExternalToolProcessStatus.IN_WORK));
        assertNull(actual.getProcessSteps().get(1).getResult());

        // prepare next test:
        sharedRelease.setExternalToolProcesses(Stream.of(actual).collect(Collectors.toSet()));
    }

    @Test
    public void test_04_scanRunning_soWaitOnResult() throws TException, FileNotFoundException, InterruptedException {
        // given:
        User user = TestUtils.getAdminUser(getClass());
        prepareValidPreconditions(user);

        // when:
        ExternalToolProcess actual = sharedRelease.getExternalToolProcesses().iterator().next();
        while (actual.getProcessSteps().get(1).getStepStatus().equals(ExternalToolProcessStatus.IN_WORK)) {
            // give fossology some time to process the scan
            Thread.sleep(5_000l);
            actual = uut.process(sharedRelease.getId(), user);
        }

        // then:
        assertNotNull(actual);
        assertThat(actual.getProcessSteps(), hasSize(2));
        assertThat(actual.getProcessSteps().get(1).getStepName(), is(FossologyUtils.FOSSOLOGY_STEP_NAME_SCAN));
        assertThat(actual.getProcessSteps().get(1).getStepStatus(), is(ExternalToolProcessStatus.DONE));
        assertThat(actual.getProcessSteps().get(1).getResult(), not(isEmptyString()));

        // prepare next test:
        sharedRelease.setExternalToolProcesses(Stream.of(actual).collect(Collectors.toSet()));
    }

    @Test
    public void test_05_scanFinished_soStartReport() throws TException, FileNotFoundException, InterruptedException {
        // given:
        User user = TestUtils.getAdminUser(getClass());
        prepareValidPreconditions(user);

        // when:
        ExternalToolProcess actual = uut.process(sharedRelease.getId(), user);

        // then:
        assertNotNull(actual);
        assertThat(actual.getProcessSteps(), hasSize(3));
        assertThat(actual.getProcessSteps().get(2).getStepName(), is(FossologyUtils.FOSSOLOGY_STEP_NAME_REPORT));
        assertThat(actual.getProcessSteps().get(2).getStepStatus(), is(ExternalToolProcessStatus.IN_WORK));
        assertNull(actual.getProcessSteps().get(2).getResult());

        // prepare next test:
        sharedRelease.setExternalToolProcesses(Stream.of(actual).collect(Collectors.toSet()));
    }

    @Test
    public void test_06_reportRunning() throws TException, FileNotFoundException, InterruptedException {
        // given:
        User user = TestUtils.getAdminUser(getClass());
        prepareValidPreconditions(user);

        // when:
        ExternalToolProcess actual = uut.process(sharedRelease.getId(), user);

        // then:
        assertNotNull(actual);
        assertThat(actual.getProcessSteps(), hasSize(3));
        assertThat(actual.getProcessSteps().get(2).getStepName(), is(FossologyUtils.FOSSOLOGY_STEP_NAME_REPORT));
        assertThat(actual.getProcessSteps().get(2).getStepStatus(), is(ExternalToolProcessStatus.IN_WORK));
        assertNull(actual.getProcessSteps().get(2).getResult());

        // prepare next test:
        sharedRelease.setExternalToolProcesses(Stream.of(actual).collect(Collectors.toSet()));
    }

    @Test
    public void test_07_reportRunning_soWaitOnResult() throws TException, FileNotFoundException, InterruptedException {
        // given:
        final String attachmentContentId = "126";
        User user = TestUtils.getAdminUser(getClass());
        prepareValidPreconditions(user);

        AttachmentService.Iface attachmentClient = mock(AttachmentService.Iface.class);
        when(attachmentClient.makeAttachmentContent(any(AttachmentContent.class))).thenAnswer(invocation -> {
            AttachmentContent attachmentContent = (AttachmentContent) invocation.getArguments()[0];
            attachmentContent.setId(attachmentContentId);
            return attachmentContent;
        });
        when(thriftClients.makeAttachmentClient()).thenReturn(attachmentClient);

        // when:
        ExternalToolProcess actual = sharedRelease.getExternalToolProcesses().iterator().next();
        while (actual.getProcessSteps().get(2).getStepStatus().equals(ExternalToolProcessStatus.IN_WORK)) {
            // give fossology some time to process the report generation
            Thread.sleep(1_000l);
            actual = uut.process(sharedRelease.getId(), user);
        }

        // then:
        assertNotNull(actual);
        assertThat(actual.getProcessSteps(), hasSize(3));
        assertThat(actual.getProcessSteps().get(2).getStepName(), is(FossologyUtils.FOSSOLOGY_STEP_NAME_REPORT));
        assertThat(actual.getProcessSteps().get(2).getStepStatus(), is(ExternalToolProcessStatus.DONE));
        assertThat(actual.getProcessSteps().get(2).getResult(), is(attachmentContentId));

        verify(attachmentConnector, times(1)).uploadAttachment(any(), any());

        List<Attachment> actualAttachments = sharedRelease.getAttachments().stream()
                .filter(a -> attachmentContentId.equals(a.getAttachmentContentId())).collect(Collectors.toList());
        // source attachments has only been mocked in service calls, so only resulting
        // attachment should be here
        assertThat(actualAttachments, hasSize(1));
        assertThat(actualAttachments.get(0).getAttachmentType(), is(AttachmentType.COMPONENT_LICENSE_INFO_XML));
    }
}

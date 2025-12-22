/*
 * Copyright Siemens AG, 2017.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.licenseinfo.parsers;

import com.google.common.collect.ImmutableMap;

import com.ibm.cloud.cloudant.v1.Cloudant;
import org.eclipse.sw360.datahandler.spring.CouchDbContextInitializer;
import org.eclipse.sw360.datahandler.spring.DatabaseConfig;
import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.db.AttachmentDatabaseHandler;
import org.eclipse.sw360.datahandler.db.ComponentDatabaseHandler;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoParsingResult;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseNameWithText;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ReaderInputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.StringReader;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.eclipse.sw360.licenseinfo.TestHelper.assertLicenseInfoParsingResult;
import static org.eclipse.sw360.licenseinfo.TestHelper.makeAttachmentContentStream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * @author: alex.borodin@evosoft.com
 */
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(
        classes = {DatabaseConfig.class},
        initializers = {CouchDbContextInitializer.class}
)
@ActiveProfiles("test")
public class CombinedCLIParserTest {
    private static final String TEST_XML_FILENAME = "CombinedCLITest.xml";

    @Spy
    @Autowired
    private CombinedCLIParser parser;

    @Autowired
    private Cloudant client;

    @Autowired
    @Qualifier("COUCH_DB_ALL_NAMES")
    private Set<String> allDatabaseNames;

    @MockitoBean
    private AttachmentDatabaseHandler attachmentDatabaseHandler;

    @MockitoBean
    private AttachmentConnector connector;
    private AttachmentContent content;
    private Attachment attachment;
    @MockitoBean
    private ComponentDatabaseHandler componentDatabaseHandler;
    private String cliTestfile;

    // Initialize the mocked objects
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Before
    public void setUp() throws Exception {
        cliTestfile = IOUtils.toString(makeAttachmentContentStream(TEST_XML_FILENAME), StandardCharsets.UTF_8);
        attachment = new Attachment("A1", "a.xml").setAttachmentType(AttachmentType.COMPONENT_LICENSE_INFO_COMBINED);
        content = new AttachmentContent().setId("A1").setFilename("a.xml").setContentType("application/xml");
        doReturn("external-correlation-id").when(parser).getCorrelationKey();
        Release r1 = new Release().setId("id1")
                .setName("r1")
                .setVersion("1.0")
                .setVendor(new Vendor().setFullname("VendorA Fullname").setShortname("VendorA"))
                .setExternalIds(ImmutableMap.of(parser.getCorrelationKey(), "1234"));
        Release r2 = new Release().setId("id2")
                .setName("r2")
                .setVersion("2.0")
                .setVendor(new Vendor().setFullname("VendorB Fullname").setShortname("VendorB"))
                .setExternalIds(ImmutableMap.of(parser.getCorrelationKey(), "4321"));
        Release r3 = new Release().setId("id3")
                .setName("r3")
                .setVersion("3.0")
                .setVendor(new Vendor().setFullname("VendorC Fullname").setShortname("VendorC"));
        Release r4 = new Release().setId("id4")
                .setName("r4")
                .setVersion("4.0")
                .setVendor(new Vendor().setFullname("VendorD Fullname").setShortname("VendorD"))
                .setExternalIds(ImmutableMap.of("some_external_id", "1234"));

        when(componentDatabaseHandler.getAllReleasesIdMap()).thenReturn(ImmutableMap.of(r1.getId(), r1, r2.getId(), r2, r3.getId(), r3, r4.getId(), r4));
        when(attachmentDatabaseHandler.getAttachmentContent("A1")).thenReturn(content);
    }

    @After
    public void tearDown() throws MalformedURLException {
        TestUtils.deleteAllDatabases(client, allDatabaseNames);
    }

    @Test
    public void testIsApplicableTo() throws Exception {
        when(connector.getAttachmentStream(content, new User(), new Project())).thenReturn(makeAttachmentContentStream(TEST_XML_FILENAME));
        assertTrue(parser.isApplicableTo(attachment, new User(), new Project()));
    }

    @Test
    public void testIsApplicableToFailsOnIncorrectRootElement() throws Exception {
        AttachmentContent content = new AttachmentContent().setId("A1").setFilename("a.xml").setContentType("application/xml");
        when(connector.getAttachmentStream(content, new User(), new Project())).thenReturn(
                ReaderInputStream.builder()
                        .setCharset(StandardCharsets.UTF_8)
                        .setReader(new StringReader("<wrong-root/>")).get()
        );
        assertFalse(parser.isApplicableTo(attachment, new User(), new Project()));
    }

    @Test
    public void testIsApplicableToFailsOnMalformedXML() throws Exception {
        AttachmentContent content = new AttachmentContent().setId("A1").setFilename("a.xml").setContentType("application/xml");
        when(connector.getAttachmentStream(content, new User(), new Project())).thenReturn(
                ReaderInputStream.builder()
                        .setCharset(StandardCharsets.UTF_8)
                        .setReader(new StringReader("this is not an xml file")).get()
        );
        assertFalse(parser.isApplicableTo(attachment, new User(), new Project()));
    }

    @Test
    public void testGetCLI() throws Exception {
        Attachment cliAttachment = new Attachment("A1", "a.xml");
        when(connector.getAttachmentStream(any(), any(), any())).thenReturn(
                ReaderInputStream.builder()
                        .setCharset(StandardCharsets.UTF_8)
                        .setReader(new StringReader(cliTestfile)).get()
        );
        List<LicenseInfoParsingResult> results = parser.getLicenseInfos(cliAttachment, new User(), new Project());
        assertThat(results.size(), is(1));
        LicenseInfoParsingResult res = results.get(0);
        assertLicenseInfoParsingResult(res);
        assertThat(res.getLicenseInfo().getFilenames(), containsInAnyOrder("a.xml"));
        assertThat(res.getLicenseInfo().getLicenseNamesWithTexts().size(), is(3));
        assertThat(res.getLicenseInfo().getLicenseNamesWithTexts().stream().map(LicenseNameWithText::getLicenseText).collect(Collectors.toSet()),
                containsInAnyOrder("License1Text", "License2Text", "License3&'Text"));
        LicenseNameWithText l2 = res.getLicenseInfo().getLicenseNamesWithTexts().stream().filter(l -> l.getLicenseName().equals("License2")).findFirst().orElseThrow(AssertionError::new);
        assertThat(l2.getAcknowledgements(), is("License2Acknowledgements"));
        assertThat(res.getLicenseInfo().getCopyrights().size(), is(5));
        assertThat(res.getLicenseInfo().getCopyrights(), containsInAnyOrder("Copyright1", "Copyright2", "Copyright3", "Copyright4", "Copyright5"));
        assertThat(res.getVendor(), is("VendorA"));
        assertThat(res.getName(), is("r1"));
        assertThat(res.getVersion(), is("1.0"));
    }
}

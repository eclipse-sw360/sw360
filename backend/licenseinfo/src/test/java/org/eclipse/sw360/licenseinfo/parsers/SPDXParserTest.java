/*
 * Copyright Bosch Software Innovations GmbH, 2016-2017.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.licenseinfo.parsers;

import com.google.common.collect.Sets;
import com.ibm.cloud.cloudant.v1.Cloudant;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

import org.eclipse.sw360.datahandler.spring.CouchDbContextInitializer;
import org.eclipse.sw360.datahandler.spring.DatabaseConfig;
import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.common.SW360ConfigKeys;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.test.SpringDataProviderRunner;
import org.eclipse.sw360.datahandler.thrift.Visibility;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfo;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoParsingResult;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseNameWithText;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.licenseinfo.TestHelper.AttachmentContentStore;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.*;

import static org.eclipse.sw360.licenseinfo.TestHelper.*;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.withSettings;

/**
 * @author: maximilian.huber@tngtech.com
 */
@RunWith(SpringDataProviderRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(
        classes = {DatabaseConfig.class},
        initializers = {CouchDbContextInitializer.class}
)
@ActiveProfiles("test")
public class SPDXParserTest {

    private User dummyUser = new User().setEmail("dummy@some.domain");

    @MockitoBean
    private SPDXParser parser;

    @MockitoBean
    private AttachmentContentStore attachmentContentStore;

    @Autowired
    private Cloudant client;

    @Autowired
    @Qualifier("COUCH_DB_ALL_NAMES")
    private Set<String> allDatabaseNames;

    @MockitoBean
    private AttachmentConnector connector;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    public static final String spdxExampleFile = "SPDXRdfExample-v2.0.rdf";
    public static final String spdx11ExampleFile = "SPDXRdfExample-v1.1.rdf";
    public static final String spdx12ExampleFile = "SPDXRdfExample-v1.2.rdf";

    @DataProvider
    public static Object[][] dataProviderAdd() {
        // @formatter:off
        return new Object[][] {
                { spdxExampleFile,
                        Arrays.asList("Apache-2.0", "LGPL-2.0", "1", "GPL-2.0", "3", "2"),
                        4,
                        "Copyright 2008-2010 John Smith",
                        Sets.newHashSet("3", "LGPL-2.0") },
                { spdx11ExampleFile,
                        Arrays.asList("4", "1", "Apache-2.0", "2", "Apache-1.0", "MPL-1.1", "3"),
                        2,
                        "Hewlett-Packard Development Company, LP",
                        Sets.newHashSet("1", "2", "3", "4", "Apache-1.0", "Apache-2.0", "MPL-1.1") },
                { spdx12ExampleFile,
                        Arrays.asList("4", "1", "Apache-2.0", "2", "Apache-1.0", "MPL-1.1", "3"),
                        3,
                        "Hewlett-Packard Development Company, LP",
                        Sets.newHashSet("1", "2", "3", "4", "Apache-1.0", "Apache-2.0", "MPL-1.1") },
        };
        // @formatter:on
    }

    @Before
    public void setUp() throws Exception {
        attachmentContentStore.put(spdxExampleFile);
        attachmentContentStore.put(spdx11ExampleFile);
        attachmentContentStore.put(spdx12ExampleFile);
    }

    @After
    public void tearDown() throws MalformedURLException {
        TestUtils.deleteAllDatabases(client, allDatabaseNames);
    }

    private void assertIsResultOfExample(LicenseInfo result, String exampleFile, List<String> expectedLicenses,
            int numberOfCoyprights, String exampleCopyright, Set<String> exampleConcludedLicenseIds) {
        assertLicenseInfo(result);

        assertEquals(1, result.getFilenames().size());
        assertEquals(exampleFile, result.getFilenames().get(0));

        assertEquals(expectedLicenses.size(), result.getLicenseNamesWithTextsSize());
        expectedLicenses.stream()
                .forEach(licenseId -> assertTrue(result.getLicenseNamesWithTexts().stream()
                        .map(LicenseNameWithText::getLicenseName)
                        .anyMatch(licenseId::equals)));
        assertTrue(result.getLicenseNamesWithTexts().stream()
                        .map(lt -> lt.getLicenseText())
                        .anyMatch(t -> t.contains("The CyberNeko Software License, Version 1.0")));

        assertEquals(numberOfCoyprights, result.getCopyrightsSize());
        assertTrue(result.getCopyrights().stream()
                        .anyMatch(c -> c.contains(exampleCopyright)));

        assertTrue(containsInAnyOrder(exampleConcludedLicenseIds.toArray()).matches(result.getConcludedLicenseIds()));
    }

    @Test
    @UseDataProvider("dataProviderAdd")
    public void testAddSPDXContentToCLI(String exampleFile, List<String> expectedLicenses, int numberOfCoyprights,
            String exampleCopyright, Set<String> exampleConcludedLicenseIds) throws Exception {
        AttachmentContent attachmentContent = new AttachmentContent()
                .setFilename(exampleFile);

        InputStream input = makeAttachmentContentStream(exampleFile);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(true);
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document spdxDocument = dBuilder.parse(input);
        spdxDocument.getDocumentElement().normalize();
        try (MockedStatic<SW360Utils> mockedStatic = mockStatic(SW360Utils.class, withSettings().defaultAnswer(Answers.CALLS_REAL_METHODS))) {
            mockedStatic.when(() -> SW360Utils.readConfig(SW360ConfigKeys.USE_LICENSE_INFO_FROM_FILES, true)).thenReturn(true);
            LicenseInfoParsingResult result = SPDXParserTools.getLicenseInfoFromSpdx(attachmentContent, true, false, spdxDocument);
            assertIsResultOfExample(result.getLicenseInfo(), exampleFile, expectedLicenses, numberOfCoyprights,
                    exampleCopyright, exampleConcludedLicenseIds);
        }
    }

    @Test
    @UseDataProvider("dataProviderAdd")
    public void testGetLicenseInfo(String exampleFile, List<String> expectedLicenses, int numberOfCoyprights,
            String exampleCopyright, Set<String> exampleConcludedLicenseIds) throws Exception {

        Attachment attachment = makeAttachment(exampleFile,
                Arrays.stream(AttachmentType.values())
                        .filter(SW360Constants.LICENSE_INFO_ATTACHMENT_TYPES::contains)
                        .findAny()
                        .get());
        try (MockedStatic<SW360Utils> mockedStatic = mockStatic(SW360Utils.class, withSettings().defaultAnswer(Answers.CALLS_REAL_METHODS))) {
            mockedStatic.when(() -> SW360Utils.readConfig(SW360ConfigKeys.USE_LICENSE_INFO_FROM_FILES, true)).thenReturn(true);
            LicenseInfoParsingResult result = parser.getLicenseInfos(attachment, dummyUser,
                            new Project()
                                    .setVisbility(Visibility.ME_AND_MODERATORS)
                                    .setCreatedBy(dummyUser.getEmail())
                                    .setAttachments(Collections.singleton(new Attachment().setAttachmentContentId(attachment.getAttachmentContentId()))))
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Parser returned empty LisenceInfoParsingResult list"));

            assertLicenseInfoParsingResult(result);
            assertIsResultOfExample(result.getLicenseInfo(), exampleFile, expectedLicenses, numberOfCoyprights,
                    exampleCopyright, exampleConcludedLicenseIds);
        }
    }
}

/*
 * Copyright Bosch Software Innovations GmbH, 2016-2017.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.licenseinfo.parsers;

import com.google.common.collect.Sets;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.spdx.rdfparser.SPDXDocumentFactory;
import org.spdx.rdfparser.model.SpdxDocument;

import java.io.InputStream;
import java.util.*;

import static org.eclipse.sw360.licenseinfo.TestHelper.*;
import static org.eclipse.sw360.licenseinfo.parsers.SPDXParser.FILETYPE_SPDX_INTERNAL;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author: maximilian.huber@tngtech.com
 */
@RunWith(DataProviderRunner.class)
public class SPDXParserTest {

    private User dummyUser = new User().setEmail("dummy@some.domain");

    private SPDXParser parser;

    private AttachmentContentStore attachmentContentStore;

    @Mock
    private AttachmentConnector connector;

    public static final String spdxExampleFile = "SPDXRdfExample-v2.0.rdf";
    public static final String spdx11ExampleFile = "SPDXRdfExample-v1.1.rdf";
    public static final String spdx12ExampleFile = "SPDXRdfExample-v1.2.rdf";

    @DataProvider
    public static Object[][] dataProviderAdd() {
        // @formatter:off
        return new Object[][] {
                { spdxExampleFile,
                        Arrays.asList("Apache-2.0", "LGPL-2.0", "1", "GPL-2.0", "CyberNeko License", "2"),
                        4,
                        "Copyright 2008-2010 John Smith",
                        Sets.newHashSet("3", "LGPL-2.0") },
                { spdx11ExampleFile,
                        Arrays.asList("4", "1", "Apache-2.0", "2", "Apache-1.0", "MPL-1.1", "CyberNeko License"),
                        2,
                        "Hewlett-Packard Development Company, LP",
                        Sets.newHashSet("1", "2", "3", "4", "Apache-1.0", "Apache-2.0", "MPL-1.1") },
                { spdx12ExampleFile,
                        Arrays.asList("4", "1", "Apache-2.0", "2", "Apache-1.0", "MPL-1.1", "CyberNeko License"),
                        3,
                        "Hewlett-Packard Development Company, LP",
                        Sets.newHashSet("1", "2", "3", "4", "Apache-1.0", "Apache-2.0", "MPL-1.1") },
        };
        // @formatter:on
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        attachmentContentStore = new AttachmentContentStore(connector);

        parser = new SPDXParser(connector, attachmentContentStore.getAttachmentContentProvider());

        attachmentContentStore.put(spdxExampleFile);
        attachmentContentStore.put(spdx11ExampleFile);
        attachmentContentStore.put(spdx12ExampleFile);
    }

    private void assertIsResultOfExample(LicenseInfo result, String exampleFile, List<String> expectedLicenses,
            int numberOfCoyprights, String exampleCopyright, Set<String> exampleConcludedLicenseIds) {
        assertLicenseInfo(result);

        assertThat(result.getFilenames().size(), is(1));
        assertThat(result.getFilenames().get(0), is(exampleFile));

        assertThat(result.getLicenseNamesWithTextsSize(), is(expectedLicenses.size()));
        expectedLicenses.stream()
                .forEach(licenseId -> assertThat(result.getLicenseNamesWithTexts().stream()
                        .map(LicenseNameWithText::getLicenseName)
                        .anyMatch(licenseId::equals), is(true)));
        assertThat(result.getLicenseNamesWithTexts().stream()
                        .map(lt -> lt.getLicenseText())
                        .anyMatch(t -> t.contains("The CyberNeko Software License, Version 1.0")),
                is(true));

        assertThat(result.getCopyrightsSize(), is(numberOfCoyprights));
        assertThat(result.getCopyrights().stream()
                        .anyMatch(c -> c.contains(exampleCopyright)),
                is(true));

        assertThat(result.getConcludedLicenseIds(), containsInAnyOrder(exampleConcludedLicenseIds.toArray()));
    }

    @Test
    @UseDataProvider("dataProviderAdd")
    public void testAddSPDXContentToCLI(String exampleFile, List<String> expectedLicenses, int numberOfCoyprights,
            String exampleCopyright, Set<String> exampleConcludedLicenseIds) throws Exception {
        AttachmentContent attachmentContent = new AttachmentContent()
                .setFilename(exampleFile);

        InputStream input = makeAttachmentContentStream(exampleFile);
        SpdxDocument spdxDocument = SPDXDocumentFactory.createSpdxDocument(input,
                parser.getUriOfAttachment(attachmentContentStore.get(exampleFile)),
                FILETYPE_SPDX_INTERNAL);

        LicenseInfoParsingResult result = SPDXParserTools.getLicenseInfoFromSpdx(attachmentContent, spdxDocument);
        assertIsResultOfExample(result.getLicenseInfo(), exampleFile, expectedLicenses, numberOfCoyprights,
                exampleCopyright, exampleConcludedLicenseIds);
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

        LicenseInfoParsingResult result = parser.getLicenseInfos(attachment, dummyUser,
                                            new Project()
                                                    .setVisbility(Visibility.ME_AND_MODERATORS)
                                                    .setCreatedBy(dummyUser.getEmail())
                                                    .setAttachments(Collections.singleton(new Attachment().setAttachmentContentId(attachment.getAttachmentContentId()))))
                .stream()
                .findFirst()
                .orElseThrow(()->new RuntimeException("Parser returned empty LisenceInfoParsingResult list"));

        assertLicenseInfoParsingResult(result);
        assertIsResultOfExample(result.getLicenseInfo(), exampleFile, expectedLicenses, numberOfCoyprights,
                exampleCopyright, exampleConcludedLicenseIds);
    }
}

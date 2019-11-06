/*
 * Copyright Siemens AG, 2016-2017.
 * With modifications by Bosch Software Innovations GmbH, 2016.
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

import org.apache.commons.io.input.ReaderInputStream;
import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoParsingResult;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoRequestStatus;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseNameWithText;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.ObligationInfoRequestStatus;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.ObligationParsingResult;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.StringReader;
import java.util.stream.Collectors;

import static org.eclipse.sw360.licenseinfo.TestHelper.assertLicenseInfoParsingResult;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author: alex.borodin@evosoft.com
 */
@RunWith(MockitoJUnitRunner.class)
public class CLIParserTest {
    private static final String CLI_TESTFILE = "<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>\n" +
            "<ComponentLicenseInformation component=\"Clearing_Report_jquery-1_12_1\" creator=\"ite40294\" date=\"30/06/2016\"  baseDoc=\"Clearing_Report_jquery-1_12_1.doc\" toolUsed=\"ReadMe Generator V0.86\" componentID=\"-1\" >\n" +
            "<License type=\"global\" name=\"MIT License\" spdxidentifier=\"n/a\" > \n" +
            "<Content><![CDATA[jQuery projects are released under the terms of the MIT license.\n" +
            "]]></Content>\n" +
            "<Files><![CDATA[Found in:\n" +
            "\n" +
            "https://jquery.org/license/ \n" +
            "]]></Files>\n" +
            "</License>\n" +
            "<Obligation>\n" +
            "<Topic><![CDATA[do not change the nature of the package\n" +
            "]]></Topic>\n" +
            "<Text><![CDATA[LGPL code must only be changed if the result is still a software library.\n" +
            "]]></Text>\n" +
            "<Licenses>\n" +
            "<License><![CDATA[LGPL-2.1+]]></License>\n" +
            "</Licenses>\n" +
            "</Obligation>\n" +
            "<Obligation>\n" +
            "<Topic><![CDATA[(Copyleft Effect) license derived works under the same license\n" +
            "]]></Topic>\n" +
            "<Text><![CDATA[In any case contact your 3rd Party Software Manager to check the copyleft effect\n" +
            "]]></Text>\n" +
            "<Licenses>\n" +
            "<License><![CDATA[GPL-1.0+]]></License>\n" +
            "<License><![CDATA[GPL-2.0]]></License>\n" +
            "<License><![CDATA[GPL-2.0+]]></License>\n" +
            "<License><![CDATA[LGPL-2.1+]]></License>\n" +
            "</Licenses>\n" +
            "</Obligation>\n" +
            "<Copyright>\n" +
            "<Content><![CDATA[Copyrights\n" +
            "]]></Content>\n" +
            "<Files><![CDATA[Found in:\n" +
            "]]></Files>\n" +
            "</Copyright>\n" +
            "<Copyright>\n" +
            "<Content><![CDATA[(c) jQuery Foundation, Inc. | jquery.org\n" +
            "]]></Content>\n" +
            "<Files><![CDATA[\\jquery-1.12.1.min.js\n" +
            "]]></Files>\n" +
            "</Copyright>\n" +
            "</ComponentLicenseInformation>";

    @Mock
    private AttachmentConnector connector;
    private CLIParser parser;
    private AttachmentContent content;
    private Attachment attachment;

    @Before
    public void setUp() throws Exception {
        attachment = new Attachment("A1", "a.xml").setAttachmentType(AttachmentType.COMPONENT_LICENSE_INFO_XML);
        content = new AttachmentContent().setId("A1").setFilename("a.xml").setContentType("application/xml");
        parser = new CLIParser(connector, attachment -> content);
    }

    @Test
    public void testIsApplicableTo() throws Exception {
        when(connector.getAttachmentStream(eq(content), anyObject(), anyObject())).thenReturn(new ReaderInputStream(new StringReader(CLI_TESTFILE)));
        assertTrue(parser.isApplicableTo(attachment, new User(), new Project()));
    }

    @Test
    public void testIsApplicableToFailsOnIncorrectRootElement() throws Exception {
        AttachmentContent content = new AttachmentContent().setId("A1").setFilename("a.xml").setContentType("application/xml");
        when(connector.getAttachmentStream(eq(content), anyObject(), anyObject())).thenReturn(new ReaderInputStream(new StringReader("<wrong-root/>")));
        assertFalse(parser.isApplicableTo(attachment, new User(), new Project()));
    }

    @Test
    public void testIsApplicableToFailsOnMalformedXML() throws Exception {
        AttachmentContent content = new AttachmentContent().setId("A1").setFilename("a.xml").setContentType("application/xml");
        when(connector.getAttachmentStream(eq(content), anyObject(), anyObject())).thenReturn(new ReaderInputStream(new StringReader("this is not an xml file")));
        assertFalse(parser.isApplicableTo(attachment, new User(), new Project()));
    }

    @Test
    public void testGetCLI() throws Exception {
        Attachment cliAttachment = new Attachment("A1", "a.xml");
        when(connector.getAttachmentStream(anyObject(), anyObject(), anyObject())).thenReturn(new ReaderInputStream(new StringReader(CLI_TESTFILE)));
        LicenseInfoParsingResult res = parser.getLicenseInfos(cliAttachment, new User(), new Project()).stream().findFirst().orElseThrow(()->new RuntimeException("Parser returned empty LisenceInfoParsingResult list"));
        assertLicenseInfoParsingResult(res);
        assertThat(res.getStatus(), is(LicenseInfoRequestStatus.SUCCESS));
        assertThat(res.getLicenseInfo(), notNullValue());
        assertThat(res.getLicenseInfo().getFilenames(), contains("a.xml"));
        assertThat(res.getLicenseInfo().getLicenseNamesWithTexts().size(), is(1));
        assertThat(res.getLicenseInfo().getLicenseNamesWithTexts().stream().map(LicenseNameWithText::getLicenseText).collect(Collectors.toSet()),
                containsInAnyOrder("jQuery projects are released under the terms of the MIT license."));
        assertThat(res.getLicenseInfo().getCopyrights().size(), is(2));
        assertThat(res.getLicenseInfo().getCopyrights(), containsInAnyOrder("Copyrights", "(c) jQuery Foundation, Inc. | jquery.org"));

    }

    @Test
    public void testGetCLIObligations() throws Exception {
        Attachment cliAttachment = new Attachment("A1", "a.xml");
        when(connector.getAttachmentStream(anyObject(), anyObject(), anyObject())).thenReturn(new ReaderInputStream(new StringReader(CLI_TESTFILE)));
        ObligationParsingResult oblRes = parser.getObligations(cliAttachment, new User(), new Project());
        assertThat(oblRes.getStatus(), is(ObligationInfoRequestStatus.SUCCESS));
        assertThat(oblRes.getObligationsSize(), is(2));
        assertThat(oblRes.getObligations().get(0).getTopic(), equalTo("do not change the nature of the package"));
        assertThat(oblRes.getObligations().get(1).getText(), equalTo("In any case contact your 3rd Party Software Manager to check the copyleft effect\n"));
        assertThat(oblRes.getObligations().get(1).getLicenseIDsSize(), is(4));
        assertThat(oblRes.getObligations().get(1).getLicenseIDs(), containsInAnyOrder("GPL-1.0+", "GPL-2.0", "GPL-2.0+", "LGPL-2.1+"));
    }

    @Test
    public void testGetCLIFailsOnMalformedXML() throws Exception {
        Attachment cliAttachment = new Attachment("A1", "a.xml");
        when(connector.getAttachmentStream(anyObject(), anyObject(), anyObject())).thenReturn(new ReaderInputStream(new StringReader(CLI_TESTFILE.replaceAll("</Content>", "</Broken>"))));
        LicenseInfoParsingResult res = parser.getLicenseInfos(cliAttachment, new User(), new Project()).stream().findFirst().orElseThrow(()->new RuntimeException("Parser returned empty LisenceInfoParsingResult list"));
        assertLicenseInfoParsingResult(res, LicenseInfoRequestStatus.FAILURE);
        assertThat(res.getStatus(), is(LicenseInfoRequestStatus.FAILURE));
        assertThat(res.getLicenseInfo(), notNullValue());
        assertThat(res.getLicenseInfo().getFilenames(), contains("a.xml"));

    }

}

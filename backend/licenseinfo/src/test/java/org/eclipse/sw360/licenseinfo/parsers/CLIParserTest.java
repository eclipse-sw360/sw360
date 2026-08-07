/*
 * Copyright Siemens AG, 2016-2017.
 * With modifications by Bosch Software Innovations GmbH, 2016.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.licenseinfo.parsers;

import org.apache.commons.io.input.ReaderInputStream;
import org.eclipse.sw360.datahandler.common.SW360ConfigKeys;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoParsingResult;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoRequestStatus;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseNameWithText;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.ObligationInfoRequestStatus;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.ObligationAtProject;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.ObligationParsingResult;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseService;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationType;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.StringReader;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.eclipse.sw360.licenseinfo.TestHelper.assertLicenseInfoParsingResult;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
        when(connector.getAttachmentStream(eq(content), any(), any())).thenReturn(
                ReaderInputStream.builder().setReader(new StringReader(CLI_TESTFILE)).get()
        );
        assertTrue(parser.isApplicableTo(attachment, new User(), new Project()));
    }

    @Test
    public void testIsApplicableToFailsOnIncorrectRootElement() throws Exception {
        AttachmentContent content = new AttachmentContent().setId("A1").setFilename("a.xml").setContentType("application/xml");
        when(connector.getAttachmentStream(eq(content), any(), any())).thenReturn(
                ReaderInputStream.builder().setReader(new StringReader("<wrong-root/>")).get()
        );
        assertFalse(parser.isApplicableTo(attachment, new User(), new Project()));
    }

    @Test
    public void testIsApplicableToFailsOnMalformedXML() throws Exception {
        AttachmentContent content = new AttachmentContent().setId("A1").setFilename("a.xml").setContentType("application/xml");
        when(connector.getAttachmentStream(eq(content), any(), any())).thenReturn(
                ReaderInputStream.builder().setReader(new StringReader("this is not an xml file")).get()
        );
        assertFalse(parser.isApplicableTo(attachment, new User(), new Project()));
    }

    @Test
    public void testGetCLI() throws Exception {
        Attachment cliAttachment = new Attachment("A1", "a.xml");
        when(connector.getAttachmentStream(any(), any(), any())).thenReturn(
                ReaderInputStream.builder().setReader(new StringReader(CLI_TESTFILE)).get()
        );
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
        when(connector.getAttachmentStream(any(), any(), any())).thenReturn(
                ReaderInputStream.builder().setReader(new StringReader(CLI_TESTFILE)).get()
        );
        ObligationParsingResult oblRes = parser.getObligations(cliAttachment, new User(), new Project());
        assertThat(oblRes.getStatus(), is(ObligationInfoRequestStatus.SUCCESS));
        assertThat(oblRes.getObligationsAtProjectSize(), is(2));
        assertThat(oblRes.getObligationsAtProject().get(0).getTopic(), equalTo("do not change the nature of the package"));
        assertThat(oblRes.getObligationsAtProject().get(1).getText(), equalTo("In any case contact your 3rd Party Software Manager to check the copyleft effect\n"));
        assertThat(oblRes.getObligationsAtProject().get(1).getLicenseIDsSize(), is(4));
        assertThat(oblRes.getObligationsAtProject().get(1).getLicenseIDs(), containsInAnyOrder("GPL-1.0+", "GPL-2.0", "GPL-2.0+", "LGPL-2.1+"));
    }

    @Test
    public void testGetCLIFailsOnMalformedXML() throws Exception {
        Attachment cliAttachment = new Attachment("A1", "a.xml");
        when(connector.getAttachmentStream(any(), any(), any())).thenReturn(
                ReaderInputStream.builder().setReader(new StringReader(CLI_TESTFILE.replaceAll("</Content>", "</Broken>"))).get()
        );
        LicenseInfoParsingResult res = parser.getLicenseInfos(cliAttachment, new User(), new Project()).stream().findFirst().orElseThrow(()->new RuntimeException("Parser returned empty LisenceInfoParsingResult list"));
        assertLicenseInfoParsingResult(res, LicenseInfoRequestStatus.FAILURE);
        assertThat(res.getStatus(), is(LicenseInfoRequestStatus.FAILURE));
        assertThat(res.getLicenseInfo(), notNullValue());
        assertThat(res.getLicenseInfo().getFilenames(), contains("a.xml"));
    }

    @Test
    public void testIsApplicableToFailsOnNonXmlExtension() throws Exception {
        // A file whose name does not end in .xml must be rejected without reading content.
        AttachmentContent txtContent = new AttachmentContent().setId("B1").setFilename("report.txt").setContentType("text/plain");
        Attachment txtAttachment = new Attachment("B1", "report.txt").setAttachmentType(AttachmentType.COMPONENT_LICENSE_INFO_XML);
        CLIParser txtParser = new CLIParser(connector, a -> txtContent);
        assertFalse(txtParser.isApplicableTo(txtAttachment, new User(), new Project()));
    }

    @Test
    public void testGetCLIWithNoLicenseElements_returnsSuccessWithEmptyLicenseSet() throws Exception {
        // A valid CLI XML that contains no <License> elements must parse successfully
        // and return an empty licence set rather than throwing.
        String noLicensesXml =
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<ComponentLicenseInformation component=\"empty-comp\" creator=\"test\" date=\"01/01/2024\">\n" +
                "<Copyright>\n" +
                "<Content><![CDATA[Copyright 2024 Example Corp.]]></Content>\n" +
                "<Files><![CDATA[src/main.c]]></Files>\n" +
                "</Copyright>\n" +
                "</ComponentLicenseInformation>";
        Attachment cliAttachment = new Attachment("A1", "a.xml");
        when(connector.getAttachmentStream(any(), any(), any())).thenReturn(
                ReaderInputStream.builder().setReader(new StringReader(noLicensesXml)).get()
        );
        LicenseInfoParsingResult res = parser.getLicenseInfos(cliAttachment, new User(), new Project())
                .stream().findFirst().orElseThrow(() -> new RuntimeException("empty result list"));
        assertThat(res.getStatus(), is(LicenseInfoRequestStatus.SUCCESS));
        assertThat(res.getLicenseInfo().getLicenseNamesWithTexts(), is(empty()));
        assertThat(res.getLicenseInfo().getCopyrights(), containsInAnyOrder("Copyright 2024 Example Corp."));
    }

    @Test
    public void testGetCLIWithSpecialCharactersInCDATA_preservesTextVerbatim() throws Exception {
        // CDATA sections must pass through <, >, and & literally without XML-escaping them.
        String specialCharsXml =
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<ComponentLicenseInformation component=\"special\" creator=\"test\" date=\"01/01/2024\">\n" +
                "<License type=\"global\" name=\"Custom\" spdxidentifier=\"n/a\">\n" +
                "<Content><![CDATA[Use <this> library & tools freely.]]></Content>\n" +
                "<Files><![CDATA[lib/]]></Files>\n" +
                "</License>\n" +
                "</ComponentLicenseInformation>";
        Attachment cliAttachment = new Attachment("A1", "a.xml");
        when(connector.getAttachmentStream(any(), any(), any())).thenReturn(
                ReaderInputStream.builder().setReader(new StringReader(specialCharsXml)).get()
        );
        LicenseInfoParsingResult res = parser.getLicenseInfos(cliAttachment, new User(), new Project())
                .stream().findFirst().orElseThrow(() -> new RuntimeException("empty result list"));
        assertThat(res.getStatus(), is(LicenseInfoRequestStatus.SUCCESS));
        assertThat(res.getLicenseInfo().getLicenseNamesWithTexts().size(), is(1));
        String licenseText = res.getLicenseInfo().getLicenseNamesWithTexts().iterator().next().getLicenseText();
        assertThat(licenseText, containsString("<this>"));
        assertThat(licenseText, containsString("&"));
    }

    @Test
    public void testGetObligationsWithNoObligationElements_returnsSuccessWithEmptyList() throws Exception {
        // A valid CLI XML with no <Obligation> elements must return SUCCESS and an empty list.
        String noObligationsXml =
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<ComponentLicenseInformation component=\"no-oblig\" creator=\"test\" date=\"01/01/2024\">\n" +
                "<License type=\"global\" name=\"MIT\" spdxidentifier=\"MIT\">\n" +
                "<Content><![CDATA[MIT License text.]]></Content>\n" +
                "<Files><![CDATA[src/]]></Files>\n" +
                "</License>\n" +
                "</ComponentLicenseInformation>";
        Attachment cliAttachment = new Attachment("A1", "a.xml");
        when(connector.getAttachmentStream(any(), any(), any())).thenReturn(
                ReaderInputStream.builder().setReader(new StringReader(noObligationsXml)).get()
        );
        ObligationParsingResult oblRes = parser.getObligations(cliAttachment, new User(), new Project());
        assertThat(oblRes.getStatus(), is(ObligationInfoRequestStatus.SUCCESS));
        assertThat(oblRes.getObligationsAtProjectSize(), is(0));
    }

    @Test
    public void testEnrichFromCouchDBReplacesTextWhenSpdxIdMatches() throws Exception {
        String cliXml =
                "<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>\n" +
                "<ComponentLicenseInformation component=\"enrichment_test\" creator=\"test\" date=\"01/01/2024\" componentID=\"-1\" >\n" +
                "<License type=\"global\" name=\"Apache-2.0\" spdxidentifier=\"Apache-2.0\" > \n" +
                "<Content><![CDATA[CLIXML license text that should be replaced.\n" +
                "]]></Content>\n" +
                "<Files><![CDATA[src/\n" +
                "]]></Files>\n" +
                "</License>\n" +
                "</ComponentLicenseInformation>";

        License couchDbLicense = new License()
                .setId("Apache-2.0")
                .setShortname("Apache-2.0")
                .setText("Canonical Apache 2.0 text from LicenseDB");

        LicenseService.Iface mockLicenseClient = Mockito.mock(LicenseService.Iface.class);
        when(mockLicenseClient.getByID(eq("Apache-2.0"), any())).thenReturn(couchDbLicense);

        CLIParser enrichedParser = new CLIParser(connector, a -> content, mockLicenseClient);
        Attachment cliAttachment = new Attachment("A1", "a.xml");
        when(connector.getAttachmentStream(any(), any(), any())).thenReturn(
                ReaderInputStream.builder().setReader(new StringReader(cliXml)).get()
        );

        try (MockedStatic<SW360Utils> mockedUtils = Mockito.mockStatic(SW360Utils.class)) {
            mockedUtils.when(() -> SW360Utils.readConfig(SW360ConfigKeys.LICENSEDB_ENABLED, "false"))
                    .thenReturn("true");

            LicenseInfoParsingResult res = enrichedParser.getLicenseInfos(cliAttachment, new User(), new Project())
                    .stream().findFirst().orElseThrow(() -> new RuntimeException("empty result"));
            assertThat(res.getStatus(), is(LicenseInfoRequestStatus.SUCCESS));
            String licenseText = res.getLicenseInfo().getLicenseNamesWithTexts().iterator().next().getLicenseText();
            assertThat(licenseText, is("Canonical Apache 2.0 text from LicenseDB"));
        }
    }

    @Test
    public void testEnrichFromCouchDBFallsBackToShortnameMatch() throws Exception {
        String cliXml =
                "<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>\n" +
                "<ComponentLicenseInformation component=\"enrichment_test\" creator=\"test\" date=\"01/01/2024\" componentID=\"-1\" >\n" +
                "<License type=\"global\" name=\"MIT\" spdxidentifier=\"n/a\" > \n" +
                "<Content><![CDATA[Old MIT text from CLIXML.\n" +
                "]]></Content>\n" +
                "<Files><![CDATA[src/\n" +
                "]]></Files>\n" +
                "</License>\n" +
                "</ComponentLicenseInformation>";

        License couchDbLicense = new License()
                .setId("MIT")
                .setShortname("MIT")
                .setText("Canonical MIT text from LicenseDB");

        LicenseService.Iface mockLicenseClient = Mockito.mock(LicenseService.Iface.class);
        when(mockLicenseClient.getByID(eq("MIT"), any())).thenReturn(couchDbLicense);

        CLIParser enrichedParser = new CLIParser(connector, a -> content, mockLicenseClient);
        Attachment cliAttachment = new Attachment("A1", "a.xml");
        when(connector.getAttachmentStream(any(), any(), any())).thenReturn(
                ReaderInputStream.builder().setReader(new StringReader(cliXml)).get()
        );

        try (MockedStatic<SW360Utils> mockedUtils = Mockito.mockStatic(SW360Utils.class)) {
            mockedUtils.when(() -> SW360Utils.readConfig(SW360ConfigKeys.LICENSEDB_ENABLED, "false"))
                    .thenReturn("true");

            LicenseInfoParsingResult res = enrichedParser.getLicenseInfos(cliAttachment, new User(), new Project())
                    .stream().findFirst().orElseThrow(() -> new RuntimeException("empty result"));
            assertThat(res.getStatus(), is(LicenseInfoRequestStatus.SUCCESS));
            String licenseText = res.getLicenseInfo().getLicenseNamesWithTexts().iterator().next().getLicenseText();
            assertThat(licenseText, is("Canonical MIT text from LicenseDB"));
        }
    }

    @Test
    public void testEnrichFromCouchDBPreservesCliXmlWhenNoMatch() throws Exception {
        String cliXml =
                "<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>\n" +
                "<ComponentLicenseInformation component=\"enrichment_test\" creator=\"test\" date=\"01/01/2024\" componentID=\"-1\" >\n" +
                "<License type=\"global\" name=\"Custom-Internal\" spdxidentifier=\"n/a\" > \n" +
                "<Content><![CDATA[Custom internal license text.\n" +
                "]]></Content>\n" +
                "<Files><![CDATA[src/\n" +
                "]]></Files>\n" +
                "</License>\n" +
                "</ComponentLicenseInformation>";

        LicenseService.Iface mockLicenseClient = Mockito.mock(LicenseService.Iface.class);

        CLIParser enrichedParser = new CLIParser(connector, a -> content, mockLicenseClient);
        Attachment cliAttachment = new Attachment("A1", "a.xml");
        when(connector.getAttachmentStream(any(), any(), any())).thenReturn(
                ReaderInputStream.builder().setReader(new StringReader(cliXml)).get()
        );

        try (MockedStatic<SW360Utils> mockedUtils = Mockito.mockStatic(SW360Utils.class)) {
            mockedUtils.when(() -> SW360Utils.readConfig(SW360ConfigKeys.LICENSEDB_ENABLED, "false"))
                    .thenReturn("true");

            LicenseInfoParsingResult res = enrichedParser.getLicenseInfos(cliAttachment, new User(), new Project())
                    .stream().findFirst().orElseThrow(() -> new RuntimeException("empty result"));
            assertThat(res.getStatus(), is(LicenseInfoRequestStatus.SUCCESS));
            String licenseText = res.getLicenseInfo().getLicenseNamesWithTexts().iterator().next().getLicenseText();
            assertThat(licenseText, is("Custom internal license text."));
        }
    }

    @Test
    public void testEnrichFromCouchDBNoEnrichmentWhenDisabled() throws Exception {
        String cliXml =
                "<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>\n" +
                "<ComponentLicenseInformation component=\"enrichment_test\" creator=\"test\" date=\"01/01/2024\" componentID=\"-1\" >\n" +
                "<License type=\"global\" name=\"MIT\" spdxidentifier=\"MIT\" > \n" +
                "<Content><![CDATA[Original CLIXML text.\n" +
                "]]></Content>\n" +
                "<Files><![CDATA[src/\n" +
                "]]></Files>\n" +
                "</License>\n" +
                "</ComponentLicenseInformation>";

        LicenseService.Iface mockLicenseClient = Mockito.mock(LicenseService.Iface.class);

        CLIParser enrichedParser = new CLIParser(connector, a -> content, mockLicenseClient);
        Attachment cliAttachment = new Attachment("A1", "a.xml");
        when(connector.getAttachmentStream(any(), any(), any())).thenReturn(
                ReaderInputStream.builder().setReader(new StringReader(cliXml)).get()
        );

        LicenseInfoParsingResult res = enrichedParser.getLicenseInfos(cliAttachment, new User(), new Project())
                .stream().findFirst().orElseThrow(() -> new RuntimeException("empty result"));
        assertThat(res.getStatus(), is(LicenseInfoRequestStatus.SUCCESS));
        String licenseText = res.getLicenseInfo().getLicenseNamesWithTexts().iterator().next().getLicenseText();
        assertThat(licenseText, is("Original CLIXML text."));
    }

    @Test
    public void testEnrichFromCouchDBSetsObligationsFromLicenseDB() throws Exception {
        String cliXml =
                "<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>\n" +
                "<ComponentLicenseInformation component=\"enrichment_test\" creator=\"test\" date=\"01/01/2024\" componentID=\"-1\" >\n" +
                "<License type=\"global\" name=\"Apache-2.0\" spdxidentifier=\"Apache-2.0\" > \n" +
                "<Content><![CDATA[Apache license text.\n" +
                "]]></Content>\n" +
                "<Files><![CDATA[src/\n" +
                "]]></Files>\n" +
                "</License>\n" +
                "</ComponentLicenseInformation>";

        License couchDbLicense = new License()
                .setId("Apache-2.0")
                .setShortname("Apache-2.0")
                .setText("Apache text from LicenseDB");

        Obligation licenseDbObligation = new Obligation()
                .setId("ob-001")
                .setTitle("Provide notice")
                .setText("You must include the license notice.")
                .setObligationType(ObligationType.OBLIGATION)
                .setExternalIds(Collections.singletonMap("licensedb-ob-id", "uuid-123"));

        LicenseService.Iface mockLicenseClient = Mockito.mock(LicenseService.Iface.class);
        when(mockLicenseClient.getByID(eq("Apache-2.0"), any())).thenReturn(couchDbLicense);
        when(mockLicenseClient.getObligationsByLicenseId(eq("Apache-2.0")))
                .thenReturn(List.of(licenseDbObligation));

        CLIParser enrichedParser = new CLIParser(connector, a -> content, mockLicenseClient);
        Attachment cliAttachment = new Attachment("A1", "a.xml");
        when(connector.getAttachmentStream(any(), any(), any())).thenReturn(
                ReaderInputStream.builder().setReader(new StringReader(cliXml)).get()
        );

        try (MockedStatic<SW360Utils> mockedUtils = Mockito.mockStatic(SW360Utils.class)) {
            mockedUtils.when(() -> SW360Utils.readConfig(SW360ConfigKeys.LICENSEDB_ENABLED, "false"))
                    .thenReturn("true");

            LicenseInfoParsingResult res = enrichedParser.getLicenseInfos(cliAttachment, new User(), new Project())
                    .stream().findFirst().orElseThrow(() -> new RuntimeException("empty result"));
            assertThat(res.getStatus(), is(LicenseInfoRequestStatus.SUCCESS));
            LicenseNameWithText lwt = res.getLicenseInfo().getLicenseNamesWithTexts().iterator().next();
            assertThat(lwt.getObligationsAtProjectSize(), is(1));
            ObligationAtProject oap = lwt.getObligationsAtProject().iterator().next();
            assertThat(oap.getTopic(), is("Provide notice"));
            assertThat(oap.getText(), is("You must include the license notice."));
            assertThat(oap.getType(), is("OBLIGATION"));
        }
    }

    @Test
    public void testEnrichFromCouchDBFiltersNonLicenseDBObligations() throws Exception {
        String cliXml =
                "<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>\n" +
                "<ComponentLicenseInformation component=\"enrichment_test\" creator=\"test\" date=\"01/01/2024\" componentID=\"-1\" >\n" +
                "<License type=\"global\" name=\"MIT\" spdxidentifier=\"MIT\" > \n" +
                "<Content><![CDATA[MIT license text.\n" +
                "]]></Content>\n" +
                "<Files><![CDATA[src/\n" +
                "]]></Files>\n" +
                "</License>\n" +
                "</ComponentLicenseInformation>";

        License couchDbLicense = new License()
                .setId("MIT")
                .setShortname("MIT")
                .setText("MIT text from LicenseDB");

        Obligation licenseDbObligation = new Obligation()
                .setId("ob-001")
                .setTitle("LicenseDB obligation")
                .setText("From LicenseDB")
                .setObligationType(ObligationType.OBLIGATION)
                .setExternalIds(Collections.singletonMap("licensedb-ob-id", "uuid-456"));

        Obligation sw360NativeObligation = new Obligation()
                .setId("ob-002")
                .setTitle("SW360 native obligation")
                .setText("Added manually in SW360")
                .setObligationType(ObligationType.RISK);

        LicenseService.Iface mockLicenseClient = Mockito.mock(LicenseService.Iface.class);
        when(mockLicenseClient.getByID(eq("MIT"), any())).thenReturn(couchDbLicense);
        when(mockLicenseClient.getObligationsByLicenseId(eq("MIT")))
                .thenReturn(List.of(licenseDbObligation, sw360NativeObligation));

        CLIParser enrichedParser = new CLIParser(connector, a -> content, mockLicenseClient);
        Attachment cliAttachment = new Attachment("A1", "a.xml");
        when(connector.getAttachmentStream(any(), any(), any())).thenReturn(
                ReaderInputStream.builder().setReader(new StringReader(cliXml)).get()
        );

        try (MockedStatic<SW360Utils> mockedUtils = Mockito.mockStatic(SW360Utils.class)) {
            mockedUtils.when(() -> SW360Utils.readConfig(SW360ConfigKeys.LICENSEDB_ENABLED, "false"))
                    .thenReturn("true");

            LicenseInfoParsingResult res = enrichedParser.getLicenseInfos(cliAttachment, new User(), new Project())
                    .stream().findFirst().orElseThrow(() -> new RuntimeException("empty result"));
            assertThat(res.getStatus(), is(LicenseInfoRequestStatus.SUCCESS));
            LicenseNameWithText lwt = res.getLicenseInfo().getLicenseNamesWithTexts().iterator().next();
            assertThat(lwt.getObligationsAtProjectSize(), is(1));
            ObligationAtProject oap = lwt.getObligationsAtProject().iterator().next();
            assertThat(oap.getTopic(), is("LicenseDB obligation"));
        }
    }
}

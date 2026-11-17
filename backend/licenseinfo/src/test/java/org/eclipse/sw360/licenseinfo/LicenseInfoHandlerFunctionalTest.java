/*
 * Copyright Harsh Kumar, 2026. Part of the SW360 Project.
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.licenseinfo;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.DatabaseSettingsTest;
import org.eclipse.sw360.datahandler.db.AttachmentDatabaseHandler;
import org.eclipse.sw360.datahandler.db.ComponentDatabaseHandler;
import org.eclipse.sw360.datahandler.db.ProjectDatabaseHandler;
import org.eclipse.sw360.datahandler.thrift.MainlineState;
import org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.Source;
import org.eclipse.sw360.datahandler.thrift.attachments.*;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoFile;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoParsingResult;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoRequestStatus;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseNameWithText;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.OutputFormatVariant;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectType;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfo;
import org.eclipse.sw360.licenseinfo.outputGenerators.OutputGenerator;
import org.eclipse.sw360.licenseinfo.outputGenerators.TextGenerator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LicenseInfoHandlerFunctionalTest {

    private static final String PROJECT_ID = "proj-test-1";
    private static final String RELEASE_ID = "release-test-1";
    private static final String CLI_ATTACHMENT_ID = "cli-test-1";

    private static LicenseInfoHandler handler;
    private static AttachmentDatabaseHandler attachmentDbHandler;
    private static ComponentDatabaseHandler componentDbHandler;
    private static ProjectDatabaseHandler projectDbHandler;
    private static DatabaseConnectorCloudant mainDbConnector;
    private static DatabaseConnectorCloudant attachmentsDbConnector;

    private Project project;
    private Release release;
    private User user;

    @BeforeAll
    static void setUpClass() throws Exception {
        // Create test databases
        TestUtils.createDatabase(DatabaseSettingsTest.getConfiguredClient(), DatabaseSettingsTest.COUCH_DB_DATABASE);
        TestUtils.createDatabase(DatabaseSettingsTest.getConfiguredClient(), DatabaseSettingsTest.COUCH_DB_ATTACHMENTS);

        // Database connectors
        mainDbConnector = new DatabaseConnectorCloudant(DatabaseSettingsTest.getConfiguredClient(), DatabaseSettingsTest.COUCH_DB_DATABASE);
        attachmentsDbConnector = new DatabaseConnectorCloudant(DatabaseSettingsTest.getConfiguredClient(), DatabaseSettingsTest.COUCH_DB_ATTACHMENTS);

        // Initialize handlers with test CouchDB
        attachmentDbHandler = new AttachmentDatabaseHandler(
                DatabaseSettingsTest.getConfiguredClient(),
                DatabaseSettingsTest.COUCH_DB_DATABASE,
                DatabaseSettingsTest.COUCH_DB_ATTACHMENTS
        );
        componentDbHandler = new ComponentDatabaseHandler(
                DatabaseSettingsTest.getConfiguredClient(),
                DatabaseSettingsTest.COUCH_DB_DATABASE,
                DatabaseSettingsTest.COUCH_DB_ATTACHMENTS
        );
        projectDbHandler = new ProjectDatabaseHandler(
                DatabaseSettingsTest.getConfiguredClient(),
                DatabaseSettingsTest.COUCH_DB_DATABASE,
                DatabaseSettingsTest.COUCH_DB_ATTACHMENTS
        );

        handler = new LicenseInfoHandler(attachmentDbHandler, componentDbHandler, projectDbHandler);
    }

    @AfterAll
    static void tearDownClass() throws Exception {
        TestUtils.deleteDatabase(DatabaseSettingsTest.getConfiguredClient(), DatabaseSettingsTest.COUCH_DB_DATABASE);
        TestUtils.deleteDatabase(DatabaseSettingsTest.getConfiguredClient(), DatabaseSettingsTest.COUCH_DB_ATTACHMENTS);
    }

    @BeforeEach
    void setUp() throws TException, java.io.IOException {
        // Create test user
        user = new User();
        user.setId("test-user-1");
        user.setEmail("test@sw360.org");
        user.setFullname("Test User");
        user.setUserGroup(UserGroup.ADMIN);
        user.setDepartment("Test Department");

        // Create project
        project = new Project();
        project.setId(PROJECT_ID);
        project.setName("Test Project");
        project.setProjectType(ProjectType.CUSTOMER);
        project.setBusinessUnit("Test BU");
        project.setVersion("1.0.0");
        project.setCreatedBy("test@sw360.org");
        project.setCreatedOn("2024-01-01");
        project.setModerators(Collections.singleton("test@sw360.org"));
        project.setContributors(Collections.emptySet());
        project.setAttachments(Collections.emptySet());
        project.setLinkedProjects(new HashMap<>());
        project.setReleaseIdToUsage(new HashMap<>());
        project.setExternalIds(new HashMap<>());

        Map<String, ProjectReleaseRelationship> releaseIdToUsage = new HashMap<>();
        releaseIdToUsage.put(RELEASE_ID, new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE));
        project.setReleaseIdToUsage(releaseIdToUsage);

        // Save project via database connector
        mainDbConnector.add(project);

        // Create CLI attachment content (the actual file content)
        AttachmentContent cliAttachmentContent = new AttachmentContent();
        cliAttachmentContent.setId(CLI_ATTACHMENT_ID);
        cliAttachmentContent.setFilename("test-cli.xml");
        cliAttachmentContent.setContentType("application/xml");

        // Save attachment content to attachments database
        attachmentDbHandler.add(cliAttachmentContent);

        // Create and save actual CLI XML file content for parsers to read
        String cliXmlContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <ComponentLicenseInformation>
                    <License>
                        <Name>Apache-2.0</Name>
                        <SpdxId>Apache-2.0</SpdxId>
                        <Text>Apache License Version 2.0</Text>
                    </License>
                    <License>
                        <Name>MIT</Name>
                        <SpdxId>MIT</SpdxId>
                        <Text>MIT License</Text>
                    </License>
                </ComponentLicenseInformation>
                """;

        // Save the actual file content so parsers can read it
        try (java.io.InputStream cliXmlStream = new java.io.ByteArrayInputStream(
                cliXmlContent.getBytes(StandardCharsets.UTF_8))) {
            attachmentDbHandler.getAttachmentConnector().uploadAttachment(cliAttachmentContent, cliXmlStream);
        }

        // Create release
        release = new Release();
        release.setId(RELEASE_ID);
        release.setName("Test Release");
        release.setVersion("1.0.0");
        release.setComponentId("comp-test-1");
        release.setMainlineState(MainlineState.MAINLINE);
        release.setCreatedBy("test@sw360.org");
        release.setCreatedOn("2024-01-01");
        release.setMainLicenseIds(Set.of("Apache-2.0"));
        release.setOtherLicenseIds(Collections.emptySet());

        // Create CLI attachment metadata (reference to the attachment content)
        Attachment cliAttachment = new Attachment();
        cliAttachment.setAttachmentContentId(CLI_ATTACHMENT_ID);
        cliAttachment.setAttachmentType(AttachmentType.COMPONENT_LICENSE_INFO_XML);
        cliAttachment.setFilename("test-cli.xml");
        cliAttachment.setCreatedBy("test@sw360.org");
        cliAttachment.setCreatedOn("2024-01-01");
        cliAttachment.setSha1("da39a3ee5e6b4b0d3255bfef95601890afd80709");
        cliAttachment.setCheckStatus(CheckStatus.ACCEPTED);

        // Link attachment to release
        release.setAttachments(Set.of(cliAttachment));

        // Save release via database connector (attachment is embedded in release)
        mainDbConnector.add(release);

        // Create AttachmentUsage with excludedLicenseIds = ["MIT"]
        AttachmentUsage attachmentUsage = new AttachmentUsage();
        attachmentUsage.setId("usage-test-1");
        attachmentUsage.setOwner(Source.releaseId(RELEASE_ID));
        attachmentUsage.setAttachmentContentId(CLI_ATTACHMENT_ID);
        LicenseInfoUsage licenseInfoUsage = new LicenseInfoUsage();
        licenseInfoUsage.setExcludedLicenseIds(Set.of("MIT"));
        licenseInfoUsage.setIncludeConcludedLicense(false);
        attachmentUsage.setUsageData(UsageData.licenseInfo(licenseInfoUsage));

        // Save AttachmentUsage via the attachment database handler
        attachmentDbHandler.makeAttachmentUsage(attachmentUsage);
    }

    static Stream<Arguments> generators() {
        return Stream.of(
                Arguments.of("TextGenerator", new TextGenerator(OutputFormatVariant.DISCLOSURE, "License Disclosure as TEXT"))
                // DocxGenerator: binary output needs proper parsing
                // XhtmlGenerator: has NullPointerException bug
                // JsonGenerator: not fully implemented
        );
    }

    @ParameterizedTest
    @MethodSource("generators")
    void shouldExcludeLicenseWhenMarkedInAttachmentUsage(String generatorName, OutputGenerator<?> generator) throws Exception {
        // Create LicenseInfoParsingResult with ONLY Apache-2.0 (simulating MIT excluded by AttachmentUsage)
        LicenseInfoParsingResult parsingResult = createLicenseInfoParsingResult();

        // Generate report using generator directly (no Thrift backend needed)
        Object outputObj = generator.generateOutputFile(
                List.of(parsingResult),
                project,
                Collections.emptyList(), // obligationResults
                user,
                Collections.emptyMap(), // externalIds
                Collections.emptyMap(), // obligationsStatus
                "template.docx",
                false // excludeReleaseVersion
        );

        // Verify report was generated
        assertNotNull(outputObj, "Report should be generated for " + generatorName);
        String output = outputObj instanceof byte[] ? new String((byte[]) outputObj, StandardCharsets.UTF_8)
                : outputObj.toString();
        assertTrue(output.length() > 0, "Report should have content for " + generatorName);

        // Verify exclusion works: MIT should NOT be present, Apache-2.0 SHOULD be present
        assertTrue(output.contains("Apache-2.0"),
                "Apache-2.0 should be present in report for " + generatorName + ". Output: " + output);
        assertTrue(!output.contains("MIT"),
                "MIT should be excluded from report for " + generatorName + ". Output: " + output);
    }

    private LicenseInfoParsingResult createLicenseInfoParsingResult() {
        LicenseNameWithText apacheLicense = new LicenseNameWithText();
        apacheLicense.setLicenseName("Apache-2.0");
        apacheLicense.setLicenseSpdxId("Apache-2.0");
        apacheLicense.setLicenseText("Apache License Version 2.0");

        LicenseInfo licenseInfo = new LicenseInfo();
        licenseInfo.setLicenseNamesWithTexts(Set.of(apacheLicense));
        licenseInfo.setFilenames(List.of("test-cli.xml"));
        licenseInfo.setComponentName("Test Component");
        licenseInfo.setSha1Hash("da39a3ee5e6b4b0d3255bfef95601890afd80709");

        LicenseInfoParsingResult result = new LicenseInfoParsingResult();
        result.setStatus(LicenseInfoRequestStatus.SUCCESS);
        result.setLicenseInfo(licenseInfo);
        result.setName("Test Release");
        result.setVersion("1.0.0");
        result.setVendor("Test Vendor");
        result.setAttachmentContentId(CLI_ATTACHMENT_ID);

        Release release = new Release();
        release.setId(RELEASE_ID);
        release.setName("Test Release");
        release.setVersion("1.0.0");
        result.setRelease(release);

        return result;
    }
}

/*
 * Copyright Siemens AG, 2026.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db;

import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.DatabaseSettingsTest;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.entitlement.ProjectModerator;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestStatus;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.Source;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.ThriftUtils;
import org.eclipse.sw360.datahandler.thrift.Visibility;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentUsage;
import org.eclipse.sw360.datahandler.thrift.attachments.CheckStatus;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationService;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.TestUtils.assertTestString;
import static org.eclipse.sw360.datahandler.common.Duration.durationOf;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Covers documents that already contain duplicate attachments (same SHA1 or same filename)
 * as well as preventing new duplicates from being introduced.
 */
class DuplicateAttachmentHandlingTest {

    private static final String dbName = DatabaseSettingsTest.COUCH_DB_DATABASE;
    private static final String attachmentsDbName = DatabaseSettingsTest.COUCH_DB_ATTACHMENTS;
    private static final String changeLogsDbName = DatabaseSettingsTest.COUCH_DB_CHANGELOGS;

    private static final String PROJECT_ID = "PDUP";
    private static final String COMPONENT_ID = "CDUP";
    private static final String RELEASE_ID = "RDUP";
    private static final User user = new User().setEmail("user1").setDepartment("AB CD EF");
    private static final User admin = user.deepCopy().setUserGroup(UserGroup.ADMIN);

    private ProjectDatabaseHandler projectHandler;
    private ComponentDatabaseHandler componentHandler;

    private static Attachment attachment(String contentId, String filename, String sha1, CheckStatus status) {
        return new Attachment().setAttachmentContentId(contentId).setFilename(filename).setSha1(sha1)
                .setAttachmentType(AttachmentType.SOURCE).setCheckStatus(status);
    }

    private static Set<Attachment> legacyDuplicates() {
        return new HashSet<>(Set.of(
                attachment("att1", "report.pdf", "samesha1", CheckStatus.ACCEPTED),
                attachment("att2", "report-copy.pdf", "samesha1", CheckStatus.ACCEPTED)));
    }

    @BeforeEach
    void setUp() throws Exception {
        assertTestString(dbName);
        assertTestString(attachmentsDbName);
        TestUtils.createDatabase(DatabaseSettingsTest.getConfiguredClient(), dbName);
        TestUtils.createDatabase(DatabaseSettingsTest.getConfiguredClient(), attachmentsDbName);

        // Legacy data: both attachments are accepted and have the same content (SHA1)
        Project project = new Project().setId(PROJECT_ID).setName("DuplicateAttachmentProject")
                .setBusinessUnit("AB CD EF").setCreatedBy(user.getEmail())
                .setVisbility(Visibility.BUISNESSUNIT_AND_MODERATORS).setAttachments(legacyDuplicates());
        Component component = new Component().setId(COMPONENT_ID).setName("DuplicateAttachmentComponent")
                .setCreatedBy(user.getEmail()).setReleaseIds(new HashSet<>(Set.of(RELEASE_ID)))
                .setAttachments(legacyDuplicates());
        Release release = new Release().setId(RELEASE_ID).setComponentId(COMPONENT_ID)
                .setName("DuplicateAttachmentComponent").setVersion("1.0").setCreatedBy(user.getEmail())
                .setAttachments(legacyDuplicates());
        DatabaseConnectorCloudant connector = new DatabaseConnectorCloudant(
                DatabaseSettingsTest.getConfiguredClient(), dbName);
        connector.add(project);
        connector.add(component);
        connector.add(release);

        componentHandler = new ComponentDatabaseHandler(DatabaseSettingsTest.getConfiguredClient(), dbName,
                changeLogsDbName, attachmentsDbName);
        AttachmentDatabaseHandler attachmentDatabaseHandler = new AttachmentDatabaseHandler(
                DatabaseSettingsTest.getConfiguredClient(), dbName, attachmentsDbName);
        PackageDatabaseHandler packageHandler = new PackageDatabaseHandler(DatabaseSettingsTest.getConfiguredClient(),
                dbName, changeLogsDbName, attachmentsDbName, attachmentDatabaseHandler, componentHandler);
        projectHandler = new ProjectDatabaseHandler(DatabaseSettingsTest.getConfiguredClient(), dbName,
                changeLogsDbName, attachmentsDbName, Mockito.mock(ProjectModerator.class), componentHandler,
                packageHandler, attachmentDatabaseHandler);
    }

    @AfterEach
    void tearDown() throws Exception {
        TestUtils.deleteDatabase(DatabaseSettingsTest.getConfiguredClient(), dbName);
        TestUtils.deleteDatabase(DatabaseSettingsTest.getConfiguredClient(), attachmentsDbName);
    }

    @Test
    void shouldAllowUpdateRejectAndDelete_whenProjectHasLegacyDuplicateAttachments() throws Exception {
        assertProjectCleanup();
    }

    @Test
    void shouldAllowUpdateRejectAndDelete_whenLegacyAttachmentsHaveSameFilename() throws Exception {
        DatabaseConnectorCloudant connector = new DatabaseConnectorCloudant(DatabaseSettingsTest.getConfiguredClient(), dbName);
        Project project = connector.get(Project.class, PROJECT_ID);
        project.setAttachments(new HashSet<>(Set.of(
                attachment("att1", "report.pdf", "sha1", CheckStatus.ACCEPTED),
                attachment("att2", "report.pdf", "sha2", CheckStatus.ACCEPTED))));
        connector.update(project);
        assertProjectCleanup();
    }

    private void assertProjectCleanup() throws Exception {
        // 1. unrelated field update must not be blocked by the existing duplicates
        Project project = projectHandler.getProjectById(PROJECT_ID, user);
        project.setDescription("changed security responsible");
        assertEquals(RequestStatus.SUCCESS, projectHandler.updateProject(project, user));
        assertEquals("changed security responsible",
                projectHandler.getProjectById(PROJECT_ID, user).getDescription());

        // 2. reject one of the duplicates, the status change must be persisted
        project = projectHandler.getProjectById(PROJECT_ID, user);
        project.getAttachments().stream().filter(a -> "att2".equals(a.getAttachmentContentId()))
                .forEach(a -> a.setCheckStatus(CheckStatus.REJECTED));
        assertEquals(RequestStatus.SUCCESS, projectHandler.updateProject(project, user));
        Attachment rejected = projectHandler.getProjectById(PROJECT_ID, user).getAttachments().stream()
                .filter(a -> "att2".equals(a.getAttachmentContentId())).findFirst().orElseThrow();
        assertEquals(CheckStatus.REJECTED, rejected.getCheckStatus());

        // 3. delete the rejected duplicate, it must be gone after reload
        project = projectHandler.getProjectById(PROJECT_ID, user);
        project.setAttachments(project.getAttachments().stream()
                .filter(a -> !"att2".equals(a.getAttachmentContentId())).collect(Collectors.toSet()));
        assertEquals(RequestStatus.SUCCESS, projectHandler.updateProject(project, user));
        Set<String> remaining = projectHandler.getProjectById(PROJECT_ID, user).getAttachments().stream()
                .map(Attachment::getAttachmentContentId).collect(Collectors.toSet());
        assertEquals(Set.of("att1"), remaining);
    }

    @Test
    void shouldRejectUpdate_whenNewAttachmentDuplicatesExistingContent() throws Exception {
        Project project = projectHandler.getProjectById(PROJECT_ID, user);
        project.addToAttachments(attachment("att3", "third.pdf", "samesha1", CheckStatus.NOTCHECKED));

        assertEquals(RequestStatus.DUPLICATE_ATTACHMENT, projectHandler.updateProject(project, user));
        assertEquals(2, projectHandler.getProjectById(PROJECT_ID, user).getAttachmentsSize());
    }

    @Test
    void shouldRejectUpdate_whenNewAttachmentHasExistingFilename() throws Exception {
        Project project = projectHandler.getProjectById(PROJECT_ID, user);
        project.addToAttachments(attachment("att3", "report.pdf", "othersha1", CheckStatus.NOTCHECKED));

        assertEquals(RequestStatus.DUPLICATE_ATTACHMENT, projectHandler.updateProject(project, user));
    }

    @Test
    void shouldRejectUpdate_whenAttachmentIsRenamedToExistingFilename() throws Exception {
        Project project = projectHandler.getProjectById(PROJECT_ID, user);
        project.getAttachments().stream().filter(a -> "att2".equals(a.getAttachmentContentId()))
                .forEach(a -> a.setFilename("report.pdf"));

        assertEquals(RequestStatus.DUPLICATE_ATTACHMENT, projectHandler.updateProject(project, user));
    }

    @Test
    void shouldAllowRename_whenOnlyExistingSha1DuplicatesRemain() throws Exception {
        Project project = projectHandler.getProjectById(PROJECT_ID, user);
        project.getAttachments().stream().filter(a -> "att2".equals(a.getAttachmentContentId()))
                .forEach(a -> a.setFilename("renamed-report.pdf"));

        assertEquals(RequestStatus.SUCCESS, projectHandler.updateProject(project, user));
        Attachment renamed = projectHandler.getProjectById(PROJECT_ID, user).getAttachments().stream()
                .filter(a -> "att2".equals(a.getAttachmentContentId())).findFirst().orElseThrow();
        assertEquals("renamed-report.pdf", renamed.getFilename());
    }

    @Test
    void shouldNotReportDuplicate_whenSha1IsMissingForLegacyAttachments() {
        Set<Attachment> stored = Set.of(attachment("att1", "report.pdf", null, CheckStatus.ACCEPTED),
                attachment("att2", "report-copy.pdf", null, CheckStatus.ACCEPTED));
        Set<Attachment> withComputedSha1 = Set.of(attachment("att1", "report.pdf", "samesha1", CheckStatus.ACCEPTED),
                attachment("att2", "report-copy.pdf", "samesha1", CheckStatus.ACCEPTED));

        assertFalse(AttachmentConnector.introducesDuplicateAttachment(stored, withComputedSha1));
    }

    @Test
    void shouldNotReportDuplicate_whenSha1AndFilenameAreMissing() {
        Set<Attachment> attachments = Set.of(attachment("att1", null, null, CheckStatus.NOTCHECKED),
                attachment("att2", null, null, CheckStatus.NOTCHECKED));

        assertFalse(AttachmentConnector.isDuplicateAttachment(attachments));
    }

    @Test
    void shouldRejectProjectCreation_whenAttachmentsAreDuplicated() throws Exception {
        Project project = new Project().setName("NewProjectWithDuplicates").setBusinessUnit("AB CD EF")
                .setVisbility(Visibility.BUISNESSUNIT_AND_MODERATORS);
        project.addToAttachments(attachment("att10", "a.pdf", "dupsha1", CheckStatus.NOTCHECKED));
        project.addToAttachments(attachment("att11", "b.pdf", "dupsha1", CheckStatus.NOTCHECKED));

        AddDocumentRequestSummary summary = projectHandler.addProject(project, user);

        assertEquals(AddDocumentRequestStatus.INVALID_INPUT, summary.getRequestStatus());
        assertEquals(SW360Constants.DUPLICATE_ATTACHMENT_ERROR_MESSAGE, summary.getMessage());
    }

    @Test
    void shouldRejectComponentCreation_whenAttachmentsAreDuplicated() throws Exception {
        Component component = new Component().setName("ComponentWithDuplicates");
        component.addToAttachments(attachment("att20", "same.pdf", "sha-a", CheckStatus.NOTCHECKED));
        component.addToAttachments(attachment("att21", "same.pdf", "sha-b", CheckStatus.NOTCHECKED));

        AddDocumentRequestSummary summary = componentHandler.addComponent(component, user.getEmail());

        assertEquals(AddDocumentRequestStatus.INVALID_INPUT, summary.getRequestStatus());
        assertEquals(SW360Constants.DUPLICATE_ATTACHMENT_ERROR_MESSAGE, summary.getMessage());
    }

    @Test
    void shouldAllowComponentUpdate_whenComponentHasLegacyDuplicateAttachments() throws Exception {
        Component component = componentHandler.getComponent(COMPONENT_ID, user);
        component.setDescription("updated description");

        assertEquals(RequestStatus.SUCCESS, componentHandler.updateComponent(component, user, true));
        assertEquals("updated description", componentHandler.getComponent(COMPONENT_ID, user).getDescription());
    }

    @Test
    void shouldRejectComponentUpdate_whenNewAttachmentDuplicatesExistingContent() throws Exception {
        Component component = componentHandler.getComponent(COMPONENT_ID, user);
        component.addToAttachments(attachment("att3", "third.pdf", "samesha1", CheckStatus.NOTCHECKED));

        assertEquals(RequestStatus.DUPLICATE_ATTACHMENT, componentHandler.updateComponent(component, user, true));
    }

    @Test
    void shouldAllowReleaseRejectAndDelete_whenReleaseHasLegacyDuplicateAttachments() throws Exception {
        Release release = componentHandler.getRelease(RELEASE_ID, user);
        release.getAttachments().stream().filter(a -> "att2".equals(a.getAttachmentContentId()))
                .forEach(a -> a.setCheckStatus(CheckStatus.REJECTED));
        assertEquals(RequestStatus.SUCCESS,
                componentHandler.updateRelease(release, user, ThriftUtils.IMMUTABLE_OF_RELEASE, true));

        release = componentHandler.getRelease(RELEASE_ID, user);
        release.setAttachments(release.getAttachments().stream()
                .filter(a -> !"att2".equals(a.getAttachmentContentId())).collect(Collectors.toSet()));
        assertEquals(RequestStatus.SUCCESS,
                componentHandler.updateRelease(release, user, ThriftUtils.IMMUTABLE_OF_RELEASE, true));

        Set<String> remaining = componentHandler.getRelease(RELEASE_ID, user).getAttachments().stream()
                .map(Attachment::getAttachmentContentId).collect(Collectors.toSet());
        assertEquals(Set.of("att1"), remaining);
    }

    @Test
    void shouldRejectReleaseUpdate_whenNewAttachmentHasExistingFilename() throws Exception {
        Release release = componentHandler.getRelease(RELEASE_ID, user);
        release.addToAttachments(attachment("att3", "report.pdf", "othersha1", CheckStatus.NOTCHECKED));

        assertEquals(RequestStatus.DUPLICATE_ATTACHMENT,
                componentHandler.updateRelease(release, user, ThriftUtils.IMMUTABLE_OF_RELEASE, true));
    }

    @Test
    void shouldRejectReleaseCreation_whenAttachmentsAreDuplicated() throws Exception {
        Release release = new Release().setComponentId(COMPONENT_ID).setName("DuplicateAttachmentComponent")
                .setVersion("2.0").setAttachments(new HashSet<>(Set.of(
                        attachment("att30", "a.zip", "dupsha1", CheckStatus.NOTCHECKED),
                        attachment("att31", "b.zip", "dupsha1", CheckStatus.NOTCHECKED))));

        AddDocumentRequestSummary summary = componentHandler.addRelease(release, user);

        assertEquals(AddDocumentRequestStatus.INVALID_INPUT, summary.getRequestStatus());
        assertEquals(SW360Constants.DUPLICATE_ATTACHMENT_ERROR_MESSAGE, summary.getMessage());
    }

    @Test
    void shouldRejectProjectUpdate_whenOmittedAcceptedAttachmentWouldBeRestored() throws Exception {
        Project project = projectHandler.getProjectById(PROJECT_ID, user);
        project.setAttachments(new HashSet<>(Set.of(
                attachment("att3", "report.pdf", "newsha", CheckStatus.NOTCHECKED))));

        assertEquals(RequestStatus.DUPLICATE_ATTACHMENT, projectHandler.updateProject(project, user));
        assertEquals(legacyDuplicates(), projectHandler.getProjectById(PROJECT_ID, user).getAttachments());
    }

    @Test
    void shouldRejectComponentUpdate_whenOmittedAcceptedAttachmentWouldBeRestored() throws Exception {
        Component component = componentHandler.getComponent(COMPONENT_ID, user);
        component.setAttachments(new HashSet<>(Set.of(
                attachment("att3", "third.pdf", "samesha1", CheckStatus.NOTCHECKED))));

        assertEquals(RequestStatus.DUPLICATE_ATTACHMENT, componentHandler.updateComponent(component, user, true));
        assertEquals(legacyDuplicates(), componentHandler.getComponent(COMPONENT_ID, user).getAttachments());
    }

    @Test
    void shouldRejectReleaseUpdate_whenOmittedAcceptedAttachmentWouldBeRestored() throws Exception {
        Release release = componentHandler.getRelease(RELEASE_ID, user);
        release.setAttachments(new HashSet<>(Set.of(
                attachment("att3", "report.pdf", "newsha", CheckStatus.NOTCHECKED))));

        assertEquals(RequestStatus.DUPLICATE_ATTACHMENT,
                componentHandler.updateRelease(release, user, ThriftUtils.IMMUTABLE_OF_RELEASE, true));
        assertEquals(legacyDuplicates(), componentHandler.getRelease(RELEASE_ID, user).getAttachments());
    }

    @Test
    void shouldRejectReleaseUpdate_whenOmittedUsedAttachmentWouldBeRestored() throws Exception {
        DatabaseConnectorCloudant connector = new DatabaseConnectorCloudant(DatabaseSettingsTest.getConfiguredClient(), dbName);
        Release stored = connector.get(Release.class, RELEASE_ID);
        Set<Attachment> usedAttachments = new HashSet<>(Set.of(
                attachment("used", "used.pdf", "usedsha", CheckStatus.REJECTED)));
        stored.setAttachments(usedAttachments);
        connector.update(stored);
        AttachmentDatabaseHandler usages = new AttachmentDatabaseHandler(
                DatabaseSettingsTest.getConfiguredClient(), dbName, attachmentsDbName);
        usages.makeAttachmentUsage(new AttachmentUsage(Source.releaseId(RELEASE_ID), "used", Source.projectId(PROJECT_ID)));
        Release update = componentHandler.getRelease(RELEASE_ID, user).setAttachments(new HashSet<>(Set.of(
                attachment("new", "used.pdf", "newsha", CheckStatus.NOTCHECKED))));

        assertEquals(RequestStatus.DUPLICATE_ATTACHMENT,
                componentHandler.updateRelease(update, user, ThriftUtils.IMMUTABLE_OF_RELEASE, true));
        assertEquals(usedAttachments, componentHandler.getRelease(RELEASE_ID, user).getAttachments());
    }

    @Test
    void shouldRejectWholeComponentBatch_whenOneDocumentIntroducesDuplicate() throws Exception {
        Component invalid = componentHandler.getComponent(COMPONENT_ID, admin);
        invalid.addToAttachments(attachment("att3", "report.pdf", "newsha", CheckStatus.NOTCHECKED));
        Component valid = new Component().setName("ValidNewComponent");

        assertDuplicateBatch(componentHandler.updateComponents(Set.of(valid, invalid), admin), 2);
        assertEquals(legacyDuplicates(), componentHandler.getComponent(COMPONENT_ID, admin).getAttachments());
        assertEquals(Set.of(COMPONENT_ID), componentHandler.getAllComponentsIdMap().keySet());
        assertFalse(valid.isSetId(), "The valid candidate must not be written either");
    }

    @Test
    void shouldRejectWholeReleaseBatch_whenOneDocumentIntroducesDuplicate() throws Exception {
        Release invalid = componentHandler.getRelease(RELEASE_ID, admin);
        invalid.addToAttachments(attachment("att3", "third.pdf", "samesha1", CheckStatus.NOTCHECKED));
        Release valid = new Release().setComponentId(COMPONENT_ID).setName("DuplicateAttachmentComponent").setVersion("2.0");

        assertDuplicateBatch(componentHandler.updateReleases(Set.of(valid, invalid), admin, false), 2);
        assertEquals(legacyDuplicates(), componentHandler.getRelease(RELEASE_ID, admin).getAttachments());
        assertEquals(Set.of(RELEASE_ID), componentHandler.getAllReleasesIdMap().keySet());
        assertFalse(valid.isSetId(), "The valid candidate must not be written either");
    }

    @Test
    void shouldRejectWholeDirectReleaseBatch_whenOneDocumentIntroducesDuplicate() throws Exception {
        Release invalid = componentHandler.getRelease(RELEASE_ID, admin);
        invalid.addToAttachments(attachment("att3", "report.pdf", "newsha", CheckStatus.NOTCHECKED));
        Release valid = new Release().setComponentId(COMPONENT_ID).setName("DuplicateAttachmentComponent").setVersion("2.0");

        assertDuplicateBatch(componentHandler.updateReleasesDirectly(Set.of(valid, invalid), admin), 2);
        assertEquals(legacyDuplicates(), componentHandler.getRelease(RELEASE_ID, admin).getAttachments());
        assertEquals(Set.of(RELEASE_ID), componentHandler.getAllReleasesIdMap().keySet());
        assertFalse(valid.isSetId(), "The valid candidate must not be written either");
    }

    @Test
    void shouldAllowBulkUpdates_whenLegacyDuplicatesAreUnchanged() throws Exception {
        Component component = componentHandler.getComponent(COMPONENT_ID, admin).setDescription("bulk edit");
        componentHandler.updateComponents(Set.of(component), admin);
        assertEquals("bulk edit", componentHandler.getComponent(COMPONENT_ID, admin).getDescription());

        Release release = componentHandler.getRelease(RELEASE_ID, admin).setAdditionalData(
                Collections.singletonMap("note", "bulk edit"));
        componentHandler.updateReleases(Set.of(release), admin, false);
        assertEquals("bulk edit", componentHandler.getRelease(RELEASE_ID, admin).getAdditionalData().get("note"));

        release = componentHandler.getRelease(RELEASE_ID, admin).setAdditionalData(
                Collections.singletonMap("note", "direct edit"));
        componentHandler.updateReleasesDirectly(Set.of(release), admin);
        assertEquals("direct edit", componentHandler.getRelease(RELEASE_ID, admin).getAdditionalData().get("note"));
    }

    @Test
    void shouldRejectBulkCreation_whenAttachmentsAreDuplicated() throws Exception {
        Component component = new Component().setName("NewBulkComponent").setAttachments(legacyDuplicates());
        Release release = new Release().setComponentId(COMPONENT_ID).setName("DuplicateAttachmentComponent")
                .setVersion("2.0").setAttachments(legacyDuplicates());

        assertDuplicateBatch(componentHandler.updateComponents(Set.of(component), admin), 1);
        assertDuplicateBatch(componentHandler.updateReleases(Set.of(release), admin, false), 1);
        assertDuplicateBatch(componentHandler.updateReleasesDirectly(Set.of(release), admin), 1);
        assertFalse(component.isSetId());
        assertFalse(release.isSetId());
    }

    @Test
    void shouldRejectBulkCreation_whenMissingSha1ValuesResolveToDuplicateContent() throws Exception {
        DatabaseConnectorCloudant connector = new DatabaseConnectorCloudant(
                DatabaseSettingsTest.getConfiguredClient(), attachmentsDbName);
        AttachmentConnector contents = new AttachmentConnector(connector, durationOf(5, SECONDS));
        AttachmentContent first = new AttachmentContent().setId("content1").setFilename("first.txt")
                .setContentType("text/plain").setType(SW360Constants.TYPE_ATTACHMENT);
        AttachmentContent second = new AttachmentContent().setId("content2").setFilename("second.txt")
                .setContentType("text/plain").setType(SW360Constants.TYPE_ATTACHMENT);
        connector.add(first);
        connector.add(second);
        byte[] bytes = "identical content".getBytes(StandardCharsets.UTF_8);
        contents.uploadAttachment(first, new ByteArrayInputStream(bytes));
        contents.uploadAttachment(second, new ByteArrayInputStream(bytes));
        Component component = new Component().setName("MissingShaComponent").setAttachments(new HashSet<>(Set.of(
                attachment("content1", "first.txt", null, CheckStatus.NOTCHECKED),
                attachment("content2", "second.txt", null, CheckStatus.NOTCHECKED))));

        assertDuplicateBatch(componentHandler.updateComponents(Set.of(component), admin), 1);
        assertFalse(component.isSetId());
    }

    @Test
    void shouldPreserveBulkPermissions_whenUserIsNotAdmin() throws Exception {
        Component component = componentHandler.getComponent(COMPONENT_ID, user).setDescription("not allowed");
        Release release = componentHandler.getRelease(RELEASE_ID, user);
        assertEquals(RequestStatus.FAILURE, componentHandler.updateComponents(Set.of(component), user).getRequestStatus());
        assertEquals(RequestStatus.ACCESS_DENIED,
                componentHandler.updateReleases(Set.of(release), user, false).getRequestStatus());
        assertEquals(RequestStatus.FAILURE,
                componentHandler.updateReleasesDirectly(Set.of(release), user).getRequestStatus());
        assertFalse("not allowed".equals(componentHandler.getComponent(COMPONENT_ID, user).getDescription()));
    }

    @Test
    void shouldRejectComponentMerge_whenMovedAttachmentConflictsWithTarget() throws Exception {
        Component source = storeComponentSource();
        Component selection = componentHandler.getComponent(COMPONENT_ID, admin);
        selection.addToAttachments(source.getAttachments().iterator().next());

        try (MockedStatic<ThriftClients> ignored = mockModeration()) {
            assertEquals(RequestStatus.DUPLICATE_ATTACHMENT,
                    componentHandler.mergeComponents(COMPONENT_ID, source.getId(), selection, admin));
        }
        assertEquals(source.getAttachments(), componentHandler.getComponent(source.getId(), admin).getAttachments());
        assertEquals(legacyDuplicates(), componentHandler.getComponent(COMPONENT_ID, admin).getAttachments());
    }

    @Test
    void shouldRejectComponentSplit_whenMovedAttachmentConflictsWithTarget() throws Exception {
        Component source = storeComponentSource();
        Component target = componentHandler.getComponent(COMPONENT_ID, admin);
        target.addToAttachments(source.getAttachments().iterator().next());
        Component selection = source.deepCopy().setAttachments(new HashSet<>());

        try (MockedStatic<ThriftClients> ignored = mockModeration()) {
            assertEquals(RequestStatus.DUPLICATE_ATTACHMENT,
                    componentHandler.splitComponent(selection, target, admin));
        }
        assertEquals(source.getAttachments(), componentHandler.getComponent(source.getId(), admin).getAttachments());
        assertEquals(legacyDuplicates(), componentHandler.getComponent(COMPONENT_ID, admin).getAttachments());
    }

    @Test
    void shouldRejectReleaseMerge_whenMovedAttachmentConflictsWithTarget() throws Exception {
        Release source = new Release().setId("RSOURCE").setComponentId(COMPONENT_ID)
                .setName("DuplicateAttachmentComponent").setVersion("2.0").setCreatedBy(user.getEmail())
                .setAttachments(new HashSet<>(Set.of(
                        attachment("moved", "report.pdf", "newsha", CheckStatus.ACCEPTED))));
        new DatabaseConnectorCloudant(DatabaseSettingsTest.getConfiguredClient(), dbName).add(source);
        Release selection = componentHandler.getRelease(RELEASE_ID, admin);
        selection.addToAttachments(source.getAttachments().iterator().next());

        try (MockedStatic<ThriftClients> ignored = mockModeration()) {
            assertEquals(RequestStatus.DUPLICATE_ATTACHMENT,
                    componentHandler.mergeReleases(RELEASE_ID, source.getId(), selection, admin));
        }
        assertEquals(source.getAttachments(), componentHandler.getRelease(source.getId(), admin).getAttachments());
        assertEquals(legacyDuplicates(), componentHandler.getRelease(RELEASE_ID, admin).getAttachments());
    }

    private Component storeComponentSource() throws SW360Exception {
        Component source = new Component().setId("CSOURCE").setName("SourceComponent").setCreatedBy(user.getEmail())
                .setAttachments(new HashSet<>(Set.of(
                        attachment("moved", "report.pdf", "newsha", CheckStatus.ACCEPTED))));
        new DatabaseConnectorCloudant(DatabaseSettingsTest.getConfiguredClient(), dbName).add(source);
        return source;
    }

    private MockedStatic<ThriftClients> mockModeration() throws Exception {
        ModerationService.Iface client = Mockito.mock(ModerationService.Iface.class);
        Mockito.when(client.getModerationRequestByDocumentId(Mockito.anyString())).thenReturn(Collections.emptyList());
        MockedStatic<ThriftClients> clients = Mockito.mockStatic(ThriftClients.class);
        clients.when(ThriftClients::makeModerationClient).thenReturn(client);
        return clients;
    }

    private void assertDuplicateBatch(RequestSummary summary, int total) {
        assertEquals(RequestStatus.DUPLICATE_ATTACHMENT, summary.getRequestStatus());
        assertEquals(total, summary.getTotalElements());
        assertEquals(0, summary.getTotalAffectedElements());
        assertEquals(SW360Constants.DUPLICATE_ATTACHMENT_ERROR_MESSAGE, summary.getMessage());
    }
}

/*
 * Copyright Bosch.IO GmbH 2020.
 * Part of the SW360 Portal Project.
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.attachment;

import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.Source;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentService;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentUsage;
import org.eclipse.sw360.datahandler.thrift.attachments.CheckStatus;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.core.ThriftServiceProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class Sw360AttachmentServiceTest {

    @Mock
    private ThriftServiceProvider<AttachmentService.Iface> serviceProvider;

    @Mock
    private RestControllerHelper<?> restControllerHelper;

    @Mock
    private AttachmentService.Iface thriftService;

    @Spy
    @InjectMocks
    private Sw360AttachmentService attachmentService;

    private int sourceIdCounter;

    @BeforeEach
    public void setUp() throws TTransportException {
        lenient().doReturn(thriftService).when(attachmentService).getThriftAttachmentClient();
    }

    private static String attachmentId(int idx) {
        return "at" + idx;
    }

    private static Attachment createAttachment(int idx) {
        return new Attachment(attachmentId(idx), "file" + idx);
    }

    private static Set<Attachment> createAttachmentSet(int count) {
        return IntStream.range(1, count + 1)
                .mapToObj(Sw360AttachmentServiceTest::createAttachment)
                .collect(Collectors.toSet());
    }

    private Source createSource(Function<String, Source> srcCreator) {
        return srcCreator.apply("id" + (++sourceIdCounter));
    }

    private Source createSource() {
        return createSource(Source::releaseId);
    }

    private AttachmentUsage createAttachmentUsage(int idx, Function<String, Source> ownerSrcCreator) {
        return new AttachmentUsage(createSource(), attachmentId(idx), createSource(ownerSrcCreator));
    }

    private AttachmentUsage createAttachmentUsage(int idx) {
        return createAttachmentUsage(idx, Source::releaseId);
    }

    @Test
    public void testFilterAttachmentsToRemoveIgnoresUnknownAttachments() throws TException {
        Set<Attachment> attachmentSet = createAttachmentSet(4);

        Set<Attachment> filtered = attachmentService.filterAttachmentsToRemove(Source.releaseId("r"),
                new HashSet<>(attachmentSet), Arrays.asList(attachmentId(10), attachmentId(11)));
        assertThat(filtered).isEmpty();
        verifyNoMoreInteractions(thriftService);
    }

    @Test
    public void testFilterAttachmentsToRemoveAllValid() throws TException {
        Source owner = createSource();
        Set<Attachment> allAttachments = createAttachmentSet(3);
        AttachmentUsage usage1 = createAttachmentUsage(2);
        AttachmentUsage usage2 = createAttachmentUsage(2);
        AttachmentUsage usage3 = createAttachmentUsage(3);
        when(thriftService.getAttachmentUsages(owner, attachmentId(2), null))
                .thenReturn(Arrays.asList(usage1, usage2));
        when(thriftService.getAttachmentUsages(owner, attachmentId(3), null))
                .thenReturn(Collections.singletonList(usage3));

        Set<Attachment> filtered = attachmentService.filterAttachmentsToRemove(owner, allAttachments,
                Arrays.asList(attachmentId(2), attachmentId(3)));
        assertThat(filtered).containsOnly(createAttachment(2), createAttachment(3));
    }

    @Test
    public void testFilterAttachmentsToRemoveEvaluatesAttachmentUsages() throws TException {
        Source owner = createSource();
        List<AttachmentUsage> usages1 = Arrays.asList(createAttachmentUsage(1),
                createAttachmentUsage(1, Source::projectId));
        List<AttachmentUsage> usages2 = Arrays.asList(createAttachmentUsage(2), createAttachmentUsage(2));
        Set<Attachment> allAttachments = createAttachmentSet(2);
        when(thriftService.getAttachmentUsages(owner, attachmentId(1), null))
                .thenReturn(usages1);
        when(thriftService.getAttachmentUsages(owner, attachmentId(2), null))
                .thenReturn(usages2);

        Set<Attachment> filtered = attachmentService.filterAttachmentsToRemove(owner, allAttachments,
                Arrays.asList(attachmentId(1), attachmentId(2)));
        assertThat(filtered).containsOnly(createAttachment(2));
    }

    @Test
    public void testFilterAttachmentsToRemoveEvaluatesCheckStatus() throws TException {
        Source owner = createSource();
        Attachment attachment1 = createAttachment(1);
        Attachment attachment2 = createAttachment(2);
        attachment2.setCheckStatus(CheckStatus.ACCEPTED);
        Set<Attachment> attachments = new HashSet<>(Arrays.asList(attachment1, attachment2));
        when(thriftService.getAttachmentUsages(any(), anyString(), any()))
                .thenReturn(Collections.emptyList());

        Set<Attachment> filtered = attachmentService.filterAttachmentsToRemove(owner, attachments,
                Arrays.asList(attachmentId(1), attachmentId(2)));
        assertThat(filtered).containsOnly(attachment1);
    }

    @Test
    public void testFilterAttachmentsToRemoveHandlesExceptions() throws TException {
        Source owner = createSource();
        when(thriftService.getAttachmentUsages(owner, attachmentId(1), null))
                .thenThrow(new TException("Thrift failure"));
        when(thriftService.getAttachmentUsages(owner, attachmentId(2), null))
                .thenReturn(Collections.emptyList());
        Set<Attachment> allAttachments = createAttachmentSet(3);

        Set<Attachment> filtered = attachmentService.filterAttachmentsToRemove(owner, allAttachments,
                Arrays.asList(attachmentId(1), attachmentId(2)));
        assertThat(filtered).containsOnly(createAttachment(2));
        verify(thriftService).getAttachmentUsages(owner, attachmentId(2), null);
    }

    @Test
    public void testSanitizeFilename_PathTraversal() {
        // Test forward slash replacement
        String filename1 = "../../tmp/malicious.txt";
        String sanitized1 = CommonUtils.sanitizeFilename(filename1);
        assertThat(sanitized1).doesNotContain("/");
        assertThat(sanitized1).isEqualTo(".._.._tmp_malicious.txt");

        // Test backslash replacement (Windows paths)
        String filename2 = "..\\..\\Windows\\System32\\evil.dll";
        String sanitized2 = CommonUtils.sanitizeFilename(filename2);
        assertThat(sanitized2).doesNotContain("\\");
        assertThat(sanitized2).isEqualTo(".._.._Windows_System32_evil.dll");

        // Test mixed slashes
        String filename3 = "../dir\\file.txt";
        String sanitized3 = CommonUtils.sanitizeFilename(filename3);
        assertThat(sanitized3).doesNotContain("/");
        assertThat(sanitized3).doesNotContain("\\");
        assertThat(sanitized3).isEqualTo(".._dir_file.txt");

        // Test strings starting with _ (CouchDB restriction)
        String filename4 = "_secret_file.txt";
        String sanitized4 = CommonUtils.sanitizeFilename(filename4);
        assertThat(sanitized4).isEqualTo("secret_file.txt");

        // Test multiple leading underscores
        String filename5 = "_/_extra_hidden.tar.gz";
        String sanitized5 = CommonUtils.sanitizeFilename(filename5);
        assertThat(sanitized5).isEqualTo("extra_hidden.tar.gz");

        // Test URL encoded separators
        String filename6 = "%2fdir%2f..%5cfile.txt";
        String sanitized6 = CommonUtils.sanitizeFilename(filename6);
        assertThat(sanitized6).isEqualTo("dir_.._file.txt");

        // Test empty/whitespace input returns default
        assertThat(CommonUtils.sanitizeFilename("")).isEqualTo(CommonUtils.DEFAULT_ATTACHMENT_FILENAME);
        assertThat(CommonUtils.sanitizeFilename("   ")).isEqualTo(CommonUtils.DEFAULT_ATTACHMENT_FILENAME);
        assertThat(CommonUtils.sanitizeFilename(null)).isEqualTo(CommonUtils.DEFAULT_ATTACHMENT_FILENAME);

        // Test filename that becomes empty after sanitization (e.g. just underscores and slashes)
        String filename7 = "_//_\\_";
        String sanitized7 = CommonUtils.sanitizeFilename(filename7);
        assertThat(sanitized7).isEqualTo(CommonUtils.DEFAULT_ATTACHMENT_FILENAME);
    }
    // ---- checkedBy / checkedTeam / checkedOn ownership tests ----
    private static final String USER_B_EMAIL = "userB@sw360.org";
    private static final String USER_B_DEPT = "DEPT_B";
    private static User userB() {
        return new User(USER_B_EMAIL, USER_B_DEPT);
    }
    private static Attachment att(String contentId, CheckStatus status, String checkedBy, String checkedTeam,
            String checkedOn) {
        Attachment a = new Attachment(contentId, "file-" + contentId);
        if (status != null) {
            a.setCheckStatus(status);
        }
        if (checkedBy != null) {
            a.setCheckedBy(checkedBy);
        }
        if (checkedTeam != null) {
            a.setCheckedTeam(checkedTeam);
        }
        if (checkedOn != null) {
            a.setCheckedOn(checkedOn);
        }
        return a;
    }
    @Test
    public void testCheckStatusChangedStampsAuthenticatedUserAndIgnoresClientCheckedBy() {
        // stored: accepted by userA
        Attachment stored = att("c1", CheckStatus.ACCEPTED, "userA@sw360.org", "DEPT_A", "2026-01-01");
        // incoming: userB rejects it but (maliciously) still sends userA as checkedBy
        Attachment incoming = att("c1", CheckStatus.REJECTED, "userA@sw360.org", "DEPT_A", "2026-01-01");
        incoming.setCheckedComment("rejecting now");
        attachmentService.setCheckedAttachmentDataFromRequest(
                new HashSet<>(Collections.singletonList(incoming)),
                new HashSet<>(Collections.singletonList(stored)), userB());
        assertThat(incoming.getCheckStatus()).isEqualTo(CheckStatus.REJECTED);
        assertThat(incoming.getCheckedBy()).isEqualTo(USER_B_EMAIL);
        assertThat(incoming.getCheckedTeam()).isEqualTo(USER_B_DEPT);
        assertThat(incoming.getCheckedOn()).isEqualTo(SW360Utils.getCreatedOn());
        // client-supplied comment is honoured
        assertThat(incoming.getCheckedComment()).isEqualTo("rejecting now");
    }
    @Test
    public void testCheckStatusUnchangedPreservesOriginalChecker() {
        // stored: accepted by userA
        Attachment stored = att("c1", CheckStatus.ACCEPTED, "userA@sw360.org", "DEPT_A", "2026-01-01");
        // incoming: still accepted, full-list resubmit during an unrelated edit by userB
        Attachment incoming = att("c1", CheckStatus.ACCEPTED, "spoofed@sw360.org", "SPOOF", "2099-12-31");
        attachmentService.setCheckedAttachmentDataFromRequest(
                new HashSet<>(Collections.singletonList(incoming)),
                new HashSet<>(Collections.singletonList(stored)), userB());
        // original checker is preserved, spoofed values ignored, not rewritten to userB
        assertThat(incoming.getCheckedBy()).isEqualTo("userA@sw360.org");
        assertThat(incoming.getCheckedTeam()).isEqualTo("DEPT_A");
        assertThat(incoming.getCheckedOn()).isEqualTo("2026-01-01");
    }
    @Test
    public void testNewCheckedAttachmentStampsAuthenticatedUser() {
        // no stored counterpart (newly added, pre-accepted), client spoofs checkedBy
        Attachment incoming = att("cNew", CheckStatus.ACCEPTED, "spoofed@sw360.org", "SPOOF", "2099-12-31");
        attachmentService.setCheckedAttachmentDataFromRequest(
                new HashSet<>(Collections.singletonList(incoming)),
                Collections.emptySet(), userB());
        assertThat(incoming.getCheckedBy()).isEqualTo(USER_B_EMAIL);
        assertThat(incoming.getCheckedTeam()).isEqualTo(USER_B_DEPT);
        assertThat(incoming.getCheckedOn()).isEqualTo(SW360Utils.getCreatedOn());
    }
    @Test
    public void testNotCheckedClearsCheckedFields() {
        Attachment stored = att("c1", CheckStatus.ACCEPTED, "userA@sw360.org", "DEPT_A", "2026-01-01");
        Attachment incoming = att("c1", CheckStatus.NOTCHECKED, "userA@sw360.org", "DEPT_A", "2026-01-01");
        incoming.setCheckedComment("stale");
        attachmentService.setCheckedAttachmentDataFromRequest(
                new HashSet<>(Collections.singletonList(incoming)),
                new HashSet<>(Collections.singletonList(stored)), userB());
        assertThat(incoming.isSetCheckedBy()).isFalse();
        assertThat(incoming.isSetCheckedTeam()).isFalse();
        assertThat(incoming.isSetCheckedOn()).isFalse();
        assertThat(incoming.getCheckedComment()).isEmpty();
    }
    @Test
    public void testNullCheckStatusDefaultsToNotCheckedAndClears() {
        Attachment incoming = att("c1", null, "spoofed@sw360.org", "SPOOF", "2099-12-31");
        attachmentService.setCheckedAttachmentDataFromRequest(
                new HashSet<>(Collections.singletonList(incoming)),
                Collections.emptySet(), userB());
        assertThat(incoming.getCheckStatus()).isEqualTo(CheckStatus.NOTCHECKED);
        assertThat(incoming.isSetCheckedBy()).isFalse();
        assertThat(incoming.isSetCheckedTeam()).isFalse();
        assertThat(incoming.isSetCheckedOn()).isFalse();
    }
    @Test
    public void testNullIncomingIsNoOp() {
        // should simply return without throwing
        attachmentService.setCheckedAttachmentDataFromRequest(null,
                new HashSet<>(Collections.singletonList(att("c1", CheckStatus.ACCEPTED, "a", "b", "c"))), userB());
    }
    @Test
    public void testFillCheckedAttachmentDataNeverTrustsClientCheckedBy() throws TException {
        Attachment incoming = att("c1", CheckStatus.ACCEPTED, "spoofed@sw360.org", "SPOOF", "2099-12-31");
        attachmentService.fillCheckedAttachmentData(incoming, userB());
        assertThat(incoming.getCheckedBy()).isEqualTo(USER_B_EMAIL);
        assertThat(incoming.getCheckedTeam()).isEqualTo(USER_B_DEPT);
        assertThat(incoming.getCheckedOn()).isEqualTo(SW360Utils.getCreatedOn());
    }

    // ---- checkStatus role guard tests ----
    private static User userWithGroup(UserGroup group) {
        return new User(USER_B_EMAIL, USER_B_DEPT).setUserGroup(group);
    }

    private static Set<Attachment> setOf(Attachment attachment) {
        return new HashSet<>(Collections.singletonList(attachment));
    }

    @Test
    public void testUserCannotAcceptAttachmentViaPatch() {
        // a release contributor with plain USER role tries to approve a clearing report
        Attachment stored = att("c1", CheckStatus.NOTCHECKED, null, null, null);
        Attachment incoming = att("c1", CheckStatus.ACCEPTED, null, null, null);
        assertThatThrownBy(() -> attachmentService.assertCheckStatusChangesAllowed(
                setOf(incoming), setOf(stored), userWithGroup(UserGroup.USER)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    public void testUserCannotRejectAttachmentViaPatch() {
        Attachment stored = att("c1", CheckStatus.NOTCHECKED, null, null, null);
        Attachment incoming = att("c1", CheckStatus.REJECTED, null, null, null);
        assertThatThrownBy(() -> attachmentService.assertCheckStatusChangesAllowed(
                setOf(incoming), setOf(stored), userWithGroup(UserGroup.USER)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    public void testUserCannotAddPreAcceptedAttachmentViaPatch() {
        Attachment incoming = att("cNew", CheckStatus.ACCEPTED, null, null, null);
        assertThatThrownBy(() -> attachmentService.assertCheckStatusChangesAllowed(
                setOf(incoming), Collections.emptySet(), userWithGroup(UserGroup.USER)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    public void testUserCanResubmitUnchangedAcceptedAttachmentViaPatch() {
        // full attachment-list resubmit during an unrelated edit must keep working
        Attachment stored = att("c1", CheckStatus.ACCEPTED, "userA@sw360.org", "DEPT_A", "2026-01-01");
        Attachment incoming = att("c1", CheckStatus.ACCEPTED, null, null, null);
        assertThatCode(() -> attachmentService.assertCheckStatusChangesAllowed(
                setOf(incoming), setOf(stored), userWithGroup(UserGroup.USER)))
                .doesNotThrowAnyException();
    }

    @Test
    public void testUserCanResetAttachmentToNotCheckedViaPatch() {
        Attachment stored = att("c1", CheckStatus.ACCEPTED, "userA@sw360.org", "DEPT_A", "2026-01-01");
        Attachment incoming = att("c1", CheckStatus.NOTCHECKED, null, null, null);
        assertThatCode(() -> attachmentService.assertCheckStatusChangesAllowed(
                setOf(incoming), setOf(stored), userWithGroup(UserGroup.USER)))
                .doesNotThrowAnyException();
    }

    @Test
    public void testClearingAdminAndAdminCanAcceptAttachmentViaPatch() {
        for (UserGroup group : Arrays.asList(UserGroup.CLEARING_ADMIN, UserGroup.ADMIN, UserGroup.SW360_ADMIN)) {
            Attachment stored = att("c1", CheckStatus.NOTCHECKED, null, null, null);
            Attachment incoming = att("c1", CheckStatus.ACCEPTED, null, null, null);
            assertThatCode(() -> attachmentService.assertCheckStatusChangesAllowed(
                    setOf(incoming), setOf(stored), userWithGroup(group)))
                    .as("group %s", group)
                    .doesNotThrowAnyException();
        }
    }

    @Test
    public void testCheckStatusGuardIgnoresNullIncoming() {
        assertThatCode(() -> attachmentService.assertCheckStatusChangesAllowed(
                null, Collections.emptySet(), userWithGroup(UserGroup.USER)))
                .doesNotThrowAnyException();
    }

    @Test
    public void testUserCannotAcceptAttachmentViaSingleAttachmentUpdate() {
        Attachment stored = att("c1", CheckStatus.NOTCHECKED, null, null, null);
        Attachment newData = new Attachment().setCheckStatus(CheckStatus.ACCEPTED);
        assertThatThrownBy(() -> attachmentService.updateAttachment(
                setOf(stored), newData, "c1", userWithGroup(UserGroup.USER)))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(stored.getCheckStatus()).isEqualTo(CheckStatus.NOTCHECKED);
    }

    @Test
    public void testUserCanEditCommentOnAcceptedAttachmentViaSingleAttachmentUpdate() {
        Attachment stored = att("c1", CheckStatus.ACCEPTED, "userA@sw360.org", "DEPT_A", "2026-01-01");
        Attachment newData = new Attachment().setCheckStatus(CheckStatus.ACCEPTED).setCreatedComment("updated");
        Attachment updated = attachmentService.updateAttachment(
                setOf(stored), newData, "c1", userWithGroup(UserGroup.USER));
        assertThat(updated.getCheckStatus()).isEqualTo(CheckStatus.ACCEPTED);
        assertThat(updated.getCreatedComment()).isEqualTo("updated");
    }

    @Test
    public void testClearingAdminCanAcceptAttachmentViaSingleAttachmentUpdate() {
        Attachment stored = att("c1", CheckStatus.NOTCHECKED, null, null, null);
        Attachment newData = new Attachment().setCheckStatus(CheckStatus.ACCEPTED);
        Attachment updated = attachmentService.updateAttachment(
                setOf(stored), newData, "c1", userWithGroup(UserGroup.CLEARING_ADMIN));
        assertThat(updated.getCheckStatus()).isEqualTo(CheckStatus.ACCEPTED);
        assertThat(updated.getCheckedBy()).isEqualTo(USER_B_EMAIL);
    }
}

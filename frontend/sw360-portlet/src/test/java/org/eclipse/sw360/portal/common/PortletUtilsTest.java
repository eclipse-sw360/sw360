/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.common;


import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.attachments.CheckStatus;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.portal.TestUserCacheHolder;
import org.eclipse.sw360.portal.users.UserCacheHolder;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.Mockito;

import javax.portlet.PortletRequest;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.common.CommonUtils.splitToSet;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class PortletUtilsTest {

    @Test
    public void testSplitToSet() throws Exception {

        Set<String> setOne = splitToSet("a, , b ,  ");
        assertThat(setOne, Matchers.containsInAnyOrder("a", "b"));

        assertThat(splitToSet("a, b,"), containsInAnyOrder("a", "b"));
        assertThat(splitToSet("a, b,"), containsInAnyOrder("a", "b"));
        assertThat(splitToSet("b, a, b , a b"), containsInAnyOrder("a", "a b", "b"));
    }

    @Test
    public void testUpdateAttachmentsFromRequestIdsAreNull() {
        PortletRequest request = Mockito.mock(PortletRequest.class);
        new TestUserCacheHolder().enable();

        Set<Attachment> documentAttachments = ImmutableSet.of(); // use immutable set to ensure that set is not changed.
        assertThat(PortletUtils.updateAttachmentsFromRequest(request, documentAttachments), is(empty()));
    }

    @Test
    public void testUpdateAttachmentsFromRequestIdsAreEmpty() {
        PortletRequest request = Mockito.mock(PortletRequest.class);
        new TestUserCacheHolder().enable();

        // existing data
        Set<Attachment> documentAttachments = ImmutableSet.of(); // use immutable set to ensure that set is not changed.

        // fill request
        setAttachmentIds(request);

        // run test
        assertThat(PortletUtils.updateAttachmentsFromRequest(request, documentAttachments), is(empty()));
    }

    @Test
    public void testUpdateAttachmentsFromRequestOneArrayIsNull() {
        PortletRequest request = Mockito.mock(PortletRequest.class);
        new TestUserCacheHolder().enable();

        // fill existing data
        Set<Attachment> documentAttachments = ImmutableSet.of(); // use immutable set to ensure that set is not changed.

        // fill request
        setAttachmentIds(request, "1", "2", "3");

        // run test
        Set<Attachment> updatedAttachments = PortletUtils.updateAttachmentsFromRequest(request, documentAttachments);
        assertTrue(updatedAttachments.isEmpty());
        assertThat(updatedAttachments, is(sameInstance(documentAttachments)));
    }

    @Test
    public void testUpdateAttachmentsFromRequestOneArrayHasDifferentLength() {
        PortletRequest request = Mockito.mock(PortletRequest.class);
        new TestUserCacheHolder().enable();

        // fill existing data
        Set<Attachment> documentAttachments = ImmutableSet.of(); // use immutable set to ensure that set is not changed.

        // fill request data
        setAttachmentIds(request, "1", "2", "3");
        setAttachmentFilenames(request, "A", "B", "C");
        setAttachmentTypes(request, AttachmentType.DOCUMENT, AttachmentType.BINARY, AttachmentType.DECISION_REPORT);
        setAttachmenCreatedComments(request, "CC1", "CC2", "CC3");
        setAttachmentCheckStatus(request, CheckStatus.NOTCHECKED, CheckStatus.ACCEPTED, CheckStatus.REJECTED);
        setAttachmentCheckComment(request, "CCK1", "CCK2");

        // run test
        Set<Attachment> updatedAttachments = PortletUtils.updateAttachmentsFromRequest(request, documentAttachments);
        assertTrue(updatedAttachments.isEmpty());
        assertThat(updatedAttachments, is(sameInstance(documentAttachments)));
    }

    @Test
    public void testUpdateAttachmentsFromRequestOnlyAddToEmpty() {
        PortletRequest request = Mockito.mock(PortletRequest.class);
        new TestUserCacheHolder().enable();

        // fill existing data
        Set<Attachment> documentAttachments = ImmutableSet.of(); // use immutable set to ensure that set is not changed.

        // fill request
        setAttachmentIds(request, "1", "2", "3");
        setAttachmentFilenames(request, "A", "B", "C");
        setAttachmentTypes(request, AttachmentType.DOCUMENT, AttachmentType.BINARY, AttachmentType.DECISION_REPORT);
        setAttachmenCreatedComments(request, "CC1", "CC2", "CC3");
        setAttachmentCheckStatus(request, CheckStatus.NOTCHECKED, CheckStatus.ACCEPTED, CheckStatus.REJECTED);
        setAttachmentCheckComment(request, "CCK1", "CCK2", "CCK3");

        // run tests
        Set<Attachment> updatedAttachments = PortletUtils.updateAttachmentsFromRequest(request, documentAttachments);
        assertThat(updatedAttachments.size(), is(3));
        assertThat(updatedAttachments.stream().map(Attachment::getAttachmentContentId).collect(Collectors.toList()),
                containsInAnyOrder("1", "2", "3"));
    }

    @Test
    public void testUpdateAttachmentsFromRequestAddAndUpdate() {
        PortletRequest request = Mockito.mock(PortletRequest.class);
        new TestUserCacheHolder().enable();

        // fill existing data
        Set<Attachment> documentAttachments = createAttachments("9", "8");

        // fill request
        setAttachmentIds(request, "1", "2", "8", "9");
        setAttachmentFilenames(request, "A", "B", "C", "D");
        setAttachmentTypes(request, AttachmentType.DOCUMENT, AttachmentType.BINARY, AttachmentType.DECISION_REPORT, AttachmentType.OTHER);
        setAttachmenCreatedComments(request, "CC1", "CC2", "CC3", "CC4");
        setAttachmentCheckStatus(request, CheckStatus.NOTCHECKED, CheckStatus.ACCEPTED, CheckStatus.REJECTED, CheckStatus.NOTCHECKED);
        setAttachmentCheckComment(request, "CCK1", "CCK2", "CCK3", "CCK4;");

        // run test
        Set<Attachment> updatedAttachments = PortletUtils.updateAttachmentsFromRequest(request, documentAttachments);
        assertThat(updatedAttachments.size(), is(4));
        assertThat(updatedAttachments.stream().map(Attachment::getAttachmentContentId).collect(Collectors.toList()),
                containsInAnyOrder("1", "2", "8", "9"));
        assertThat(updatedAttachments.stream().map(Attachment::getFilename).collect(Collectors.toList()),
                containsInAnyOrder("A", "B", "file8", "file9"));
    }

    @Test
    public void testUpdateAttachmentsFromRequestUpdateOnly() {
        PortletRequest request = Mockito.mock(PortletRequest.class);

        // fill existing data
        User user = createUser("tid", "test@example.org", "tpd");
        new TestUserCacheHolder().enable(user);
        Set<Attachment> documentAttachments = createAttachments(user, "1", "2", "3");

        // fill request
        new TestUserCacheHolder().enable(createUser("cid", "check@example.org", "cpd"));
        setAttachmentIds(request, "1", "2", "3");
        setAttachmentFilenames(request, "A", "B", "C");
        setAttachmentTypes(request, AttachmentType.DOCUMENT, AttachmentType.SOURCE, AttachmentType.DESIGN);
        setAttachmenCreatedComments(request, "CC1", "CC2", "CC3");
        setAttachmentCheckStatus(request, CheckStatus.NOTCHECKED, CheckStatus.ACCEPTED, CheckStatus.ACCEPTED);
        setAttachmentCheckComment(request, "", "CCK2", "CCK3");

        // run tests
        Set<Attachment> updatedAttachments = PortletUtils.updateAttachmentsFromRequest(request, documentAttachments);
        assertThat(updatedAttachments.size(), is(3));
        assertThat(updatedAttachments.stream().map(Attachment::getAttachmentContentId).collect(Collectors.toList()),
                containsInAnyOrder("1", "2", "3"));
        assertThat(updatedAttachments.stream().map(Attachment::getFilename).collect(Collectors.toList()),
                containsInAnyOrder("file1", "file2", "file3"));
        assertThat(updatedAttachments.stream().map(Attachment::getAttachmentType).collect(Collectors.toList()),
                containsInAnyOrder(AttachmentType.DOCUMENT, AttachmentType.SOURCE, AttachmentType.DESIGN));
        assertThat(updatedAttachments.stream().map(Attachment::getCreatedBy).collect(Collectors.toList()),
                containsInAnyOrder("test@example.org", "test@example.org", "test@example.org"));
        assertThat(updatedAttachments.stream().map(Attachment::getCreatedTeam).collect(Collectors.toList()),
                containsInAnyOrder("tpd", "tpd", "tpd"));
        assertThat(updatedAttachments.stream().map(Attachment::getCreatedComment).collect(Collectors.toList()),
                containsInAnyOrder("CC1", "CC2", "CC3"));
        assertThat(updatedAttachments.stream().map(Attachment::getCheckStatus).collect(Collectors.toList()),
                containsInAnyOrder(CheckStatus.NOTCHECKED, CheckStatus.ACCEPTED, CheckStatus.ACCEPTED));
        assertThat(updatedAttachments.stream().map(Attachment::getCheckedBy).collect(Collectors.toList()),
                containsInAnyOrder(null, "check@example.org", "check@example.org"));
        assertThat(updatedAttachments.stream().map(Attachment::getCheckedTeam).collect(Collectors.toList()),
                containsInAnyOrder(null, "cpd", "cpd"));
        assertThat(updatedAttachments.stream().map(Attachment::getCheckedComment).collect(Collectors.toList()),
                containsInAnyOrder("", "CCK2", "CCK3"));
    }

    @Test
    public void testUpdateAttachmentsUncheckRemovesCheckedFields() {
        PortletRequest request = Mockito.mock(PortletRequest.class);

        // fill existing data
        User user = createUser("tid", "test@example.org", "tpd");
        Attachment attachment = createAttachments(user, "1").iterator().next();
        attachment.setCheckStatus(CheckStatus.ACCEPTED);
        attachment.setCheckedBy("check@example.org");
        attachment.setCheckedComment("check-comment");
        attachment.setCheckedOn("2017-01-31");
        attachment.setCheckedTeam("check-team");

        // fill request
        setAttachmentIds(request, "1");
        setAttachmentFilenames(request, "A");
        setAttachmentTypes(request, AttachmentType.SOURCE);
        setAttachmenCreatedComments(request, "CC1");
        setAttachmentCheckStatus(request, CheckStatus.NOTCHECKED);
        setAttachmentCheckComment(request, "");

        // run test
        Set<Attachment> updatedAttachments = PortletUtils.updateAttachmentsFromRequest(request, Sets.newHashSet(attachment));
        assertThat(updatedAttachments.size(), is(1));

        Attachment updatedAttachment = updatedAttachments.iterator().next();
        assertThat(updatedAttachment.getAttachmentContentId(), is("1"));
        assertThat(updatedAttachment.getCheckStatus(), is(CheckStatus.NOTCHECKED));
        assertNull(updatedAttachment.getCheckedBy());
        assertNull(updatedAttachment.getCheckedTeam());
        assertThat(updatedAttachment.getCheckedComment(), is(""));
    }

    @Test
    public void testUpdateAttachmentsRemoveAndUpdate() {
        PortletRequest request = Mockito.mock(PortletRequest.class);
        new TestUserCacheHolder().enable();

        // fill existing data
        Set<Attachment> documentAttachments = createAttachments("1", "2", "3");

        // fill request
        new TestUserCacheHolder().enable(createUser("tid", "test@example.org", "tpd"));
        setAttachmentIds(request, "2");
        setAttachmentFilenames(request, "B");
        setAttachmentTypes(request, AttachmentType.SCREENSHOT);
        setAttachmenCreatedComments(request, "CC2");
        setAttachmentCheckStatus(request, CheckStatus.ACCEPTED);
        setAttachmentCheckComment(request, "CCK2");

        // run tests
        Set<Attachment> updatedAttachments = PortletUtils.updateAttachmentsFromRequest(request, documentAttachments);
        // assertThat(documentAttachments.size(), is(3));
        assertThat(updatedAttachments.size(), is(1));

        Attachment updatedAttachment = updatedAttachments.iterator().next();
        assertThat(updatedAttachment.getAttachmentContentId(), is("2"));
        assertThat(updatedAttachment.getCheckStatus(), is(CheckStatus.ACCEPTED));
        assertThat(updatedAttachment.getCheckedBy(), is("test@example.org"));
        assertThat(updatedAttachment.getCheckedTeam(), is("tpd"));
        assertThat(updatedAttachment.getCheckedComment(), is("CCK2"));
    }

    @Test
    public void testUpdateAttachmentsRemoveAll() {
        PortletRequest request = Mockito.mock(PortletRequest.class);
        new TestUserCacheHolder().enable();

        // fill existing data
        Set<Attachment> documentAttachments = createAttachments("1", "2", "3");

        // fill request
        setAttachmentIds(request);
        setAttachmentFilenames(request);
        setAttachmentTypes(request);
        setAttachmenCreatedComments(request);
        setAttachmentCheckStatus(request);
        setAttachmentCheckComment(request);

        // run tests
        Set<Attachment> updatedAttachments = PortletUtils.updateAttachmentsFromRequest(request, documentAttachments);
        assertThat(documentAttachments.size(), is(3));
        assertTrue(updatedAttachments.isEmpty());
    }

    protected static Set<Attachment> createAttachments(String... ids) {
        return createAttachments(UserCacheHolder.EMPTY_USER, ids);
    }

    protected static Set<Attachment> createAttachments(User user, String... ids) {
        Set<Attachment> attachments = Sets.newHashSet();

        for (String id : ids) {
            attachments.add(CommonUtils.getNewAttachment(user, id, "file" + id));
        }

        return ImmutableSet.copyOf(attachments); // use immutable set to ensure that set is not changed.attachments;
    }

    protected static void setAttachmentIds(PortletRequest request, String... ids) {
        Mockito.when(
                request.getParameterValues(Release._Fields.ATTACHMENTS.toString() + Attachment._Fields.ATTACHMENT_CONTENT_ID.toString()))
                .thenReturn(ids);
    }

    protected static void setAttachmentFilenames(PortletRequest request, String... fileNames) {
        Mockito.when(request.getParameterValues(Release._Fields.ATTACHMENTS.toString() + Attachment._Fields.FILENAME.toString()))
                .thenReturn(fileNames);
    }

    protected static void setAttachmentTypes(PortletRequest request, AttachmentType... types) {
        List<String> stringTypes = Lists.newArrayList();

        for (AttachmentType type : types) {
            stringTypes.add("" + type.getValue());
        }

        Mockito.when(request.getParameterValues(Release._Fields.ATTACHMENTS.toString() + Attachment._Fields.ATTACHMENT_TYPE.toString()))
                .thenReturn(stringTypes.toArray(new String[] {}));
    }

    protected static void setAttachmenCreatedComments(PortletRequest request, String... comments) {
        Mockito.when(request.getParameterValues(Release._Fields.ATTACHMENTS.toString() + Attachment._Fields.CREATED_COMMENT.toString()))
                .thenReturn(comments);
    }

    protected static void setAttachmentCheckStatus(PortletRequest request, CheckStatus... statuses) {
        List<String> stringTypes = Lists.newArrayList();

        for (CheckStatus status : statuses) {
            stringTypes.add("" + status.getValue());
        }

        Mockito.when(request.getParameterValues(Release._Fields.ATTACHMENTS.toString() + Attachment._Fields.CHECK_STATUS.toString()))
                .thenReturn(stringTypes.toArray(new String[] {}));
    }

    protected static void setAttachmentCheckComment(PortletRequest request, String... comments) {
        Mockito.when(request.getParameterValues(Release._Fields.ATTACHMENTS.toString() + Attachment._Fields.CHECKED_COMMENT.toString()))
                .thenReturn(comments);
    }

    protected static User createUser(String id, String email, String department) {
        return new User().setId(id).setEmail(email).setDepartment(department).setExternalid("").setLastname("").setGivenname("");
    }
}


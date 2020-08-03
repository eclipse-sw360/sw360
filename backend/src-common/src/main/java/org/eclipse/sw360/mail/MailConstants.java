/*
 * Copyright Siemens AG, 2016-2017. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.mail;

/**
 * Constants for the MailUtil class.
 *
 * @author birgit.heydenreich@tngtech.com
 */
public class MailConstants {

    public static final String MAIL_PROPERTIES_FILE_PATH = "/sw360.properties";

    public static final String DEFAULT_BEGIN = "defaultBegin";
    public static final String DEFAULT_END = "defaultEnd";
    public static final String UNSUBSCRIBE_NOTICE_BEFORE = "unsubscribeNoticeBefore";
    public static final String UNSUBSCRIBE_NOTICE_AFTER = "unsubscribeNoticeAfter";
    public static final String DASH = " - ";

    public static final String SUBJECT_FOR_NEW_MODERATION_REQUEST = "subjectForNewModerationRequest";
    public static final String SUBJECT_FOR_UPDATE_MODERATION_REQUEST = "subjectForUpdateModerationRequest";
    public static final String SUBJECT_FOR_ACCEPTED_MODERATION_REQUEST = "subjectForAcceptedModerationRequest";
    public static final String SUBJECT_FOR_DECLINED_MODERATION_REQUEST = "subjectForDeclinedModerationRequest";
    public static final String SUBJECT_FOR_DECLINED_USER_MODERATION_REQUEST = "subjectForDeclinedUserModerationRequest";
    public static final String SUBJECT_FOR_NEW_COMPONENT = "subjectForNewComponent";
    public static final String SUBJECT_FOR_UPDATE_COMPONENT = "subjectForUpdateComponent";
    public static final String SUBJECT_FOR_NEW_RELEASE = "subjectForNewRelease";
    public static final String SUBJECT_FOR_UPDATE_RELEASE = "subjectForUpdateRelease";
    public static final String SUBJECT_FOR_NEW_PROJECT = "subjectForNewProject";
    public static final String SUBJECT_FOR_UPDATE_PROJECT = "subjectForUpdateProject";
    public static final String SUBJECT_FOR_NEW_CLEARING_REQUEST = "subjectForNewClearingRequest";
    public static final String SUBJECT_FOR_CLEARING_REQUEST_COMMENT = "subjectForClearingRequestComment";
    public static final String SUBJECT_FOR_UPDATED_CLEARING_REQUEST = "subjectForUpdatedClearingRequest";
    public static final String SUBJECT_FOR_CLOSED_CLEARING_REQUEST = "subjectForClosedClearingRequest";
    public static final String SUBJECT_FOR_REJECTED_CLEARING_REQUEST = "subjectForRejectedClearingRequest";
    public static final String SUBJECT_FOR_UPDATED_PROJECT_WITH_CLEARING_REQUEST = "subjectForUpdatedProjectWithClearingRequest";

    public static final String TEXT_FOR_NEW_MODERATION_REQUEST = "textForNewModerationRequest";
    public static final String TEXT_FOR_UPDATE_MODERATION_REQUEST = "textForUpdateModerationRequest";
    public static final String TEXT_FOR_ACCEPTED_MODERATION_REQUEST = "textForAcceptedModerationRequest";
    public static final String TEXT_FOR_DECLINED_MODERATION_REQUEST = "textForDeclinedModerationRequest";
    public static final String TEXT_FOR_DECLINED_USER_MODERATION_REQUEST = "textForDeclinedUserModerationRequest";
    public static final String TEXT_FOR_NEW_COMPONENT = "textForNewComponent";
    public static final String TEXT_FOR_UPDATE_COMPONENT = "textForUpdateComponent";
    public static final String TEXT_FOR_NEW_RELEASE = "textForNewRelease";
    public static final String TEXT_FOR_UPDATE_RELEASE = "textForUpdateRelease";
    public static final String TEXT_FOR_NEW_PROJECT = "textForNewProject";
    public static final String TEXT_FOR_UPDATE_PROJECT = "textForUpdateProject";
    public static final String TEXT_FOR_CLOSED_CLEARING_REQUEST = "textForClosedClearingRequest";
    public static final String TEXT_FOR_REJECTED_CLEARING_REQUEST = "textForRejectedClearingRequest";

    private MailConstants() {
        // Utility class with only static functions
    }

}

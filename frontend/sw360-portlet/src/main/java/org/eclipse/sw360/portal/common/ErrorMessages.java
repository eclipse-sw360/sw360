/*
 * Copyright Siemens AG, 2013-2016. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.common;

import com.google.common.collect.ImmutableList;

/**
 * Defines possible error messages that can be displayed in liferay
 */
public class ErrorMessages {

    public static final String PROJECT_NOT_ADDED = "Project could not be added.";
    public static final String PROJECT_DUPLICATE = "A project with the same name and version already exists.";
    public static final String CLOSED_UPDATE_NOT_ALLOWED = "User cannot edit a closed project";
    public static final String COMPONENT_NOT_ADDED = "Component could not be added.";
    public static final String COMPONENT_DUPLICATE = "A component with the same name already exists.";
    public static final String COMPONENT_NAMING_ERROR = "Name of component cannot contain only space characters.";
    public static final String RELEASE_NOT_ADDED = "Release could not be added.";
    public static final String RELEASE_DUPLICATE = "A release with the same name and version already exists.";
    public static final String DUPLICATE_ATTACHMENT = "Multiple attachments with same name or content cannot be present in attachment list.";
    public static final String ERROR_GETTING_PROJECT = "Error fetching project from backend.";
    public static final String ERROR_GETTING_COMPONENT = "Error fetching component from backend.";
    public static final String ERROR_GETTING_LICENSE = "No license details found in the database for given license id.";
    public static final String ERROR_GETTING_RELEASE = "Error fetching release from backend.";
    public static final String LICENSE_USED_BY_RELEASE =  "Request could not be processed, as license is used by at least one release!";
    public static final String DOCUMENT_USED_BY_PROJECT_OR_RELEASE = "Document could not be processed, as it is used by other Projects or Releases!";
    public static final String DOCUMENT_NOT_PROCESSED_SUCCESSFULLY = "Document could not be processed.";
    public static final String FIRST_NAME_CANNOT_BE_EMPTY= "First name cannot be empty.";
    public static final String LAST_NAME_CANNOT_BE_EMPTY = "Last name cannot be empty.";
    public static final String EMAIL_NOT_VALID = "Email is not valid.";
    public static final String DEPARTMENT_CANNOT_BE_EMPTY = "Department cannot be empty.";
    public static final String EXTERNAL_ID_CANNOT_BE_EMPTY = "External ID cannot be empty.";
    public static final String PASSWORD_CANNOT_BE_EMPTY = "Password cannot be empty.";
    public static final String PASSWORDS_DONT_MATCH = "Password do not match.";
    public static final String COULD_NOT_CREATE_USER_MODERATION_REQUEST = "Could not create user moderation request.";
    public static final String EMAIL_ALREADY_EXISTS = "Email already exists.";
    public static final String EXTERNAL_ID_ALREADY_EXISTS = "External id already exists.";
    public static final String DEFAULT_ERROR_MESSAGE = "Request could not be processed.";
    public static final String DOCUMENT_NOT_AVAILABLE = "The requested document is not available.";
    public static final String LICENSE_SHORTNAME_TAKEN = "License shortname is already taken.";
    public static final String UPDATE_FAILED_SANITY_CHECK = "Document update has been rejected; cannot delete all linked releases or projects at once.";
    public static final String REST_API_TOKEN_ERROR = "Token could not generated/deleted. Please verify if the service is available.";
    public static final String REST_API_TOKEN_NAME_DUPLICATE = "Token name already exists.";
    public static final String REST_API_EXPIRE_DATE_NOT_VALID = "Your selected token expiration date is not valid.";

    //this map is used in errorKeyToMessage.jspf to generate key-value pairs for the liferay-ui error tag
    public static final ImmutableList<String> allErrorMessages = ImmutableList.<String>builder()
            .add(PROJECT_NOT_ADDED)
            .add(PROJECT_DUPLICATE)
            .add(CLOSED_UPDATE_NOT_ALLOWED)
            .add(COMPONENT_NOT_ADDED)
            .add(COMPONENT_DUPLICATE)
            .add(COMPONENT_NAMING_ERROR)
            .add(RELEASE_NOT_ADDED)
            .add(RELEASE_DUPLICATE)
            .add(DUPLICATE_ATTACHMENT)
            .add(LICENSE_USED_BY_RELEASE)
            .add(DOCUMENT_USED_BY_PROJECT_OR_RELEASE)
            .add(DOCUMENT_NOT_PROCESSED_SUCCESSFULLY)
            .add(DEFAULT_ERROR_MESSAGE)
            .add(FIRST_NAME_CANNOT_BE_EMPTY)
            .add(LAST_NAME_CANNOT_BE_EMPTY)
            .add(EMAIL_NOT_VALID)
            .add(DEPARTMENT_CANNOT_BE_EMPTY)
            .add(EXTERNAL_ID_CANNOT_BE_EMPTY)
            .add(PASSWORD_CANNOT_BE_EMPTY)
            .add(PASSWORDS_DONT_MATCH)
            .add(COULD_NOT_CREATE_USER_MODERATION_REQUEST)
            .add(EMAIL_ALREADY_EXISTS)
            .add(EXTERNAL_ID_ALREADY_EXISTS)
            .add(DOCUMENT_NOT_AVAILABLE)
            .add(ERROR_GETTING_PROJECT)
            .add(ERROR_GETTING_COMPONENT)
            .add(ERROR_GETTING_RELEASE)
            .add(ERROR_GETTING_LICENSE)
            .add(LICENSE_SHORTNAME_TAKEN)
            .add(UPDATE_FAILED_SANITY_CHECK)
            .add(REST_API_TOKEN_ERROR)
            .add(REST_API_TOKEN_NAME_DUPLICATE)
            .add(REST_API_EXPIRE_DATE_NOT_VALID)
            .build();

    private ErrorMessages() {
        // Utility class with only static functions
    }
}

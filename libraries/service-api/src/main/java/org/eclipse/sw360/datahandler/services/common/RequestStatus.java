/*
 *  Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 * 
 *  This program and the accompanying materials are made
 *  available under the terms of the Eclipse Public License 2.0
 *  which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 *  SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.services.common;

public enum RequestStatus {
    SUCCESS,
    SENT_TO_MODERATOR,
    FAILURE,
    IN_USE,
    FAILED_SANITY_CHECK,
    DUPLICATE,
    DUPLICATE_ATTACHMENT,
    ACCESS_DENIED,
    CLOSED_UPDATE_NOT_ALLOWED,
    INVALID_INPUT,
    PROCESSING,
    NAMINGERROR,
    INVALID_SOURCE_CODE_URL,
}
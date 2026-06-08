/*
 *  Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 * 
 *  This program and the accompanying materials are made
 *  available under the terms of the Eclipse Public License 2.0
 *  which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 *  SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.datahandler.services.changelogs;

public enum Operation {
    CREATE,
    UPDATE,
    DELETE,
    MODERATION_ACCEPT,
    PROJECT_UPDATE,
    COMPONENT_UPDATE,
    RELEASE_CREATE,
    RELEASE_UPDATE,
    RELEASE_DELETE,
    MERGE_COMPONENT,
    MERGE_RELEASE,
    OBLIGATION_UPDATE,
    OBLIGATION_ADD,
    SPLIT_COMPONENT,
    SPDX_DOCUMENT_CREATE,
    SPDX_DOCUMENT_DELETE,
    SPDX_DOCUMENT_CREATION_INFO_CREATE,
    SPDX_DOCUMENT_CREATION_INFO_DELETE,
    SPDX_PACKAGE_INFO_CREATE,
    SPDX_PACKAGE_INFO_DELETE,
    LICENSE_CREATE,
    LICENSE_UPDATE,
    LICENSE_DELETE
}

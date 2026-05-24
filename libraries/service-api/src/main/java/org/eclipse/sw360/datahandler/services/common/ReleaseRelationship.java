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

public enum ReleaseRelationship {
    CONTAINED,
    REFERRED,
    UNKNOWN,
    DYNAMICALLY_LINKED,
    STATICALLY_LINKED,
    SIDE_BY_SIDE,
    STANDALONE,
    INTERNAL_USE,
    OPTIONAL,
    TO_BE_REPLACED,
    CODE_SNIPPET
}

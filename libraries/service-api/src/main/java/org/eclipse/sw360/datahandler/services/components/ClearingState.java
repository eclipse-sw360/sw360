/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.services.components;

public enum ClearingState {
    NEW_CLEARING,
    SENT_TO_CLEARING_TOOL,
    UNDER_CLEARING,
    REPORT_AVAILABLE,
    APPROVED,
    SCAN_AVAILABLE,
    INTERNAL_USE_SCAN_AVAILABLE
}

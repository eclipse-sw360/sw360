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

public enum ClearingRequestState {
    NEW,
    ACCEPTED,
    REJECTED,
    IN_QUEUE,
    IN_PROGRESS,
    CLOSED,
    AWAITING_RESPONSE,
    ON_HOLD,
    SANITY_CHECK,
    PENDING_INPUT
}

/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.vmcomponents;

import org.eclipse.sw360.datahandler.services.common.RequestStatus;

/**
 * Client API for the vmcomponents backend (scheduled SVM sync / reverse-match).
 */
public interface VMComponentsClient {

    RequestStatus synchronizeComponents();

    RequestStatus triggerReverseMatch();
}

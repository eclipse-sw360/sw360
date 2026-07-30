/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.cvesearch;

import java.util.Set;

import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.cvesearch.VulnerabilityUpdateStatus;

/**
 * Client API for the cvesearch backend service.
 */
public interface CveSearchClient {

    VulnerabilityUpdateStatus updateForRelease(String releaseId);

    VulnerabilityUpdateStatus updateForComponent(String componentId);

    VulnerabilityUpdateStatus updateForProject(String projectId);

    VulnerabilityUpdateStatus fullUpdate();

    RequestStatus update();

    Set<String> findCpes(String vendor, String product, String version);
}

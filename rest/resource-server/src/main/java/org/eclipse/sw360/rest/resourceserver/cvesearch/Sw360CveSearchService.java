/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.cvesearch;

import java.util.Set;

import org.eclipse.sw360.datahandler.cvesearch.CveSearchClient;
import org.eclipse.sw360.datahandler.cvesearch.CveSearchClients;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.cvesearch.VulnerabilityUpdateStatus;
import org.springframework.stereotype.Service;

@Service
public class Sw360CveSearchService {

    private CveSearchClient client() {
        return CveSearchClients.get();
    }

    public VulnerabilityUpdateStatus updateForRelease(String releaseId) {
        return client().updateForRelease(releaseId);
    }

    public VulnerabilityUpdateStatus updateForComponent(String componentId) {
        return client().updateForComponent(componentId);
    }

    public VulnerabilityUpdateStatus updateForProject(String projectId) {
        return client().updateForProject(projectId);
    }

    public VulnerabilityUpdateStatus fullUpdate() {
        return client().fullUpdate();
    }

    public RequestStatus update() {
        return client().update();
    }

    public Set<String> findCpes(String vendor, String product, String version) {
        return client().findCpes(vendor, product, version);
    }
}

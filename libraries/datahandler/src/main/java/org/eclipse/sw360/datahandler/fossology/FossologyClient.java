/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.fossology;

import java.util.Map;

import org.eclipse.sw360.datahandler.services.common.ConfigContainer;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.components.ExternalToolProcess;
import org.eclipse.sw360.datahandler.services.fossology.FossologyProcessRequest;
import org.eclipse.sw360.datahandler.services.fossology.FossologyReleaseRequest;
import org.eclipse.sw360.datahandler.services.users.User;

/**
 * Client API for the fossology backend service.
 */
public interface FossologyClient {

    ConfigContainer getFossologyConfig();

    RequestStatus setFossologyConfig(ConfigContainer config);

    RequestStatus checkConnection();

    ExternalToolProcess process(FossologyProcessRequest request, User user);

    RequestStatus markFossologyProcessOutdated(FossologyReleaseRequest request, User user);

    RequestStatus triggerReportGenerationFossology(FossologyReleaseRequest request, User user);

    Map<String, String> checkUnpackStatus(int uploadId);

    Map<String, String> checkScanStatus(int scanJobId);
}

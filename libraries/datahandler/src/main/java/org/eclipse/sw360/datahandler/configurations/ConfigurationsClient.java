/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.configurations;

import java.util.Map;

import org.eclipse.sw360.datahandler.services.common.ConfigContainer;
import org.eclipse.sw360.datahandler.services.common.ConfigFor;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.users.User;

/**
 * Client API for the configurations backend service.
 */
public interface ConfigurationsClient {

    RequestStatus createSW360Configs(ConfigContainer newConfig);

    Map<String, String> getSW360Configs();

    String getConfigByKey(String key);

    Map<String, String> getConfigForContainer(ConfigFor configFor);

    RequestStatus updateSW360Configs(Map<String, String> updatedConfigs, User user);

    RequestStatus updateSW360ConfigForContainer(ConfigFor configFor, Map<String, String> updatedConfigs, User user);

    /** Drop any locally cached {@link #getConfigByKey} values. */
    void invalidateCache();
}

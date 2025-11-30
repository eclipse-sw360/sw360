/*
 * Copyright TOSHIBA CORPORATION, 2025. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.configurations;

import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.db.SW360ConfigsDatabaseHandler;
import org.eclipse.sw360.datahandler.thrift.ConfigContainer;
import org.eclipse.sw360.datahandler.thrift.ConfigFor;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.configurations.SW360ConfigsService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import java.util.Map;

public class SW360ConfigsHandler implements SW360ConfigsService.Iface {
    private final SW360ConfigsDatabaseHandler sw360ConfigsDatabaseHandler;

    public SW360ConfigsHandler() {
        sw360ConfigsDatabaseHandler = new SW360ConfigsDatabaseHandler(DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_CONFIG);
    }

    @Override
    public RequestStatus createSW360Configs(ConfigContainer newConfig) {
        return RequestStatus.SUCCESS;
    }

    @Override
    public RequestStatus updateSW360Configs(Map<String, String> updatedConfigs, User user) throws SW360Exception {
        return sw360ConfigsDatabaseHandler.updateSW360Configs(updatedConfigs, user);
    }

    @Override
    public Map<String, String> getSW360Configs() {
        return sw360ConfigsDatabaseHandler.getSW360Configs();
    }

    @Override
    public String getConfigByKey(String key) {
        return sw360ConfigsDatabaseHandler.getConfigByKey(key);
    }

    @Override
    public Map<String, String> getConfigForContainer(ConfigFor configFor) {
        return sw360ConfigsDatabaseHandler.getConfigForContainer(configFor);
    }

    @Override
    public RequestStatus updateSW360ConfigForContainer(
            ConfigFor configFor, Map<String, String> updatedConfigs, User user
    ) throws SW360Exception {
        return sw360ConfigsDatabaseHandler.updateSW360ConfigForContainer(configFor, updatedConfigs, user);
    }
}

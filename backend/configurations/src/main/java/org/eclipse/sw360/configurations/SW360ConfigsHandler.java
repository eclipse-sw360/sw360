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
import org.eclipse.sw360.datahandler.services.common.ConfigContainer;
import org.eclipse.sw360.datahandler.services.common.ConfigFor;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.common.utils.ThriftConverter;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SW360ConfigsHandler {

    private final SW360ConfigsDatabaseHandler sw360ConfigsDatabaseHandler;

    public SW360ConfigsHandler() {
        sw360ConfigsDatabaseHandler = new SW360ConfigsDatabaseHandler(DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_CONFIG);
    }

    public RequestStatus createSW360Configs(ConfigContainer newConfig) {
        return RequestStatus.SUCCESS;
    }

    public RequestStatus updateSW360Configs(Map<String, String> updatedConfigs, User user) {
        try {
            return ThriftConverter.fromThriftRequestStatus(
                    sw360ConfigsDatabaseHandler.updateSW360Configs(updatedConfigs, user));
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }

    public Map<String, String> getSW360Configs() {
        return sw360ConfigsDatabaseHandler.getSW360Configs();
    }

    public String getConfigByKey(String key) {
        return sw360ConfigsDatabaseHandler.getConfigByKey(key);
    }

    public Map<String, String> getConfigForContainer(ConfigFor configFor) {
        return sw360ConfigsDatabaseHandler.getConfigForContainer(ThriftConverter.toThriftConfigFor(configFor));
    }

    public RequestStatus updateSW360ConfigForContainer(
            ConfigFor configFor, Map<String, String> updatedConfigs, User user) {
        try {
            return ThriftConverter.fromThriftRequestStatus(
                    sw360ConfigsDatabaseHandler.updateSW360ConfigForContainer(
                            ThriftConverter.toThriftConfigFor(configFor), updatedConfigs, user));
        } catch (org.eclipse.sw360.datahandler.thrift.SW360Exception e) {
            throw ThriftConverter.fromThriftException(e);
        }
    }
}

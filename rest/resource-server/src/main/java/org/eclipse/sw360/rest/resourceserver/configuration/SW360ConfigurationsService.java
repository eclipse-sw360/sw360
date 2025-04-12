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

package org.eclipse.sw360.rest.resourceserver.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.SW360ConfigKeys;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.configurations.SW360ConfigsService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SW360ConfigurationsService {
    private SW360ConfigsService.Iface getThriftConfigsClient() {
        return new ThriftClients().makeSW360ConfigsClient();
    }

    public Map<String, String> getSW360Configs() throws TException {
        Map<String, String> combinedConfig = getSW360ConfigFromDb();
        combinedConfig.putAll(getSW360ConfigFromProperties());
        return combinedConfig;
    }

    public Map<String, String> getSW360ConfigFromDb() throws TException {
        SW360ConfigsService.Iface configService = getThriftConfigsClient();
        return configService.getSW360Configs();
    }

    public Map<String, String> getSW360ConfigFromProperties() {
        Map<String, String> configFromProperties = new HashMap<>();
        configFromProperties.put("enable.flexible.project.release.relationship", String.valueOf(SW360Constants.ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP));
        return configFromProperties;
    }

    public RequestStatus updateSW360Configs(Map<String, String> updatedConfig, User user) throws TException, InvalidPropertiesFormatException {
        try {
            SW360ConfigsService.Iface configsService = getThriftConfigsClient();
            return configsService.updateSW360Configs(updatedConfig, user);
        } catch (SW360Exception sw360Exception) {
            throw new InvalidPropertiesFormatException(sw360Exception.getWhy());
        }
    }
}

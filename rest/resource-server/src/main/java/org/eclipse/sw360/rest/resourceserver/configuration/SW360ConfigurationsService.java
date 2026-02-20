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

import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.thrift.ConfigFor;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.configurations.SW360ConfigsService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.Sw360ResourceServer;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.Map;

@Service
@Slf4j
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
        configFromProperties.put("rest.apitoken.read.validity.days", String.valueOf(Sw360ResourceServer.API_TOKEN_MAX_VALIDITY_READ_IN_DAYS));
        configFromProperties.put("rest.apitoken.write.validity.days", String.valueOf(Sw360ResourceServer.API_TOKEN_MAX_VALIDITY_WRITE_IN_DAYS));
        configFromProperties.put("ui.rest.apitoken.write.generator.enable", String.valueOf(Sw360ResourceServer.API_WRITE_TOKEN_GENERATOR_ENABLED));
        configFromProperties.put("svm.notification.url", String.valueOf(Sw360ResourceServer.SVM_NOTIFICATION_URL));
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

    public Map<String, String> getConfigForContainer(ConfigFor configFor) throws TException {
        Map<String, String> combinedConfig = getSW360ConfigFromDb(configFor);
        combinedConfig.putAll(getSW360ConfigFromProperties());
        return combinedConfig;
    }

    public Map<String, String> getSW360ConfigFromDb(ConfigFor configFor) throws TException {
        SW360ConfigsService.Iface configService = getThriftConfigsClient();
        return configService.getConfigForContainer(configFor);
    }

    public RequestStatus updateSW360ConfigForContainer(ConfigFor configFor, Map<String, String> updatedConfig, User user) throws TException, InvalidPropertiesFormatException {
        try {
            SW360ConfigsService.Iface configsService = getThriftConfigsClient();
            return configsService.updateSW360ConfigForContainer(configFor, updatedConfig, user);
        } catch (SW360Exception sw360Exception) {
            throw new InvalidPropertiesFormatException(sw360Exception.getWhy());
        }
    }
}

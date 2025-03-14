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
include "sw360.thrift"
include "users.thrift"

namespace java org.eclipse.sw360.datahandler.thrift.configurations
namespace php sw360.thrift.configurations

typedef sw360.SW360Exception SW360Exception
typedef sw360.RequestStatus RequestStatus
typedef sw360.ConfigContainer ConfigContainer
typedef users.User User

service SW360ConfigsService {

    /**
     * Create ConfigContainer with configFor SW360_CONFIGURATION.
     * Only users have role ADMIN are allowed
     **/
    RequestStatus createSW360Configs(1: ConfigContainer newConfig);

    /**
     * Update config in database with configFor SW360_CONFIGURATION.
     * Only users have role ADMIN are allowed
     **/
    RequestStatus updateSW360Configs(1: map<string, string> updatedConfigs, 2: User user) throws (1: SW360Exception exp);

    /**
     * gets the current sw360 config map for configFor SW360_CONFIGURATION.
     **/
    map<string, string> getSW360Configs();

    /**
     * get config value by config key name.
     **/
    string getConfigByKey(string key);

}

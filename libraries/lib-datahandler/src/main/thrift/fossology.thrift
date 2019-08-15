/*
 * Copyright Siemens AG, 2014-2015, 2019. Part of the SW360 Portal Project.
 * With contributions by Bosch Software Innovations GmbH, 2016.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
include "sw360.thrift"
include "users.thrift"
include "components.thrift"

namespace java org.eclipse.sw360.datahandler.thrift.fossology
namespace php sw360.thrift.fossology

typedef sw360.RequestStatus RequestStatus
typedef sw360.ConfigContainer ConfigContainer
typedef users.User User
typedef components.ExternalToolProcess ExternalToolProcess


service FossologyService {

    /**
     * Saves a ConfigContainer with configFor FOSSOLOGY_REST.
     **/
    RequestStatus setFossologyConfig(1: ConfigContainer newConfig);

    /**
     * Gets the current ConfigContainer for configFor FOSSOLOGY_REST.
     **/
    ConfigContainer getFossologyConfig();

    /**
     * Check connection with fossology, if connection works, SUCCESS is returned
     **/
    RequestStatus checkConnection();

    /**
     * Invokes the next step of the one Fossology workflow for the given release.
     * Not only saves the reached state in the release, but also returns the 
     * ExternalToolProcess.
     **/
    ExternalToolProcess process(1: string releaseId, 2: User user);

    /**
     * Since there should only be one actice Fossology process at most for a release
     * no extra identification needed. The active one, if there is one, will be marked
     * outdated, so that a new one can be invoked via process().
     * If setting the state was successful, SUCCESS is returned.
     **/
    RequestStatus markFossologyProcessOutdated(1: string releaseId, 2: User user);

}

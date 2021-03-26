/*
 * Copyright Bosch.IO GmbH 2020
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
namespace java org.eclipse.sw360.datahandler.thrift.health
namespace php sw360.thrift.health

/**
* List of all accepted health statuses
**/
enum Status {
    UP = 0,
    UNKNOWN = 1,
    DOWN = 2,
    ERROR = 3
}

struct Health {
    1: required Status status = Status.UNKNOWN,
    2: required map<string, string> details,
}

service HealthService {
    /**
    * Returns a single health of the thrift service and all running thrift services
    **/
    Health getHealth();

    Health getHealthOfSpecificDbs(1: set<string> dbsToCheck);
}
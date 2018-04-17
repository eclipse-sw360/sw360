/*
 * Copyright (c) Bosch Software Innovations GmbH 2016.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */


include "sw360.thrift"

namespace java org.eclipse.sw360.datahandler.thrift.cvesearch
namespace php sw360.thrift.cvesearch

typedef sw360.RequestStatus RequestStatus

enum UpdateType {
    NEW = 0,
    UPDATED = 1,
    OLD = 2,
    FAILED = 3,
}

struct VulnerabilityUpdateStatus {
    1: map<UpdateType, list<string>> statusToVulnerabilityIds;
    2: RequestStatus requestStatus;
}

service CveSearchService {
    /**
    * applies cve search for given release, writes vulnerabilities to database and creates for each
    * vulnerability a ReleaseVulnerabilityRelation in the database
    * returns VulnerabilityUpdateStatus
    **/
    VulnerabilityUpdateStatus updateForRelease(1: string ReleaseId);

    /**
     * calls updateForRelease for every release of given component and aggregates results
     **/
    VulnerabilityUpdateStatus updateForComponent(1: string ComponentId);

    /**
      * calls updateForRelease for every release directly linked to given project and aggregates results
      **/
    VulnerabilityUpdateStatus updateForProject(1: string ProjectId);

    /**
      * calls updateForRelease for every release in the database and aggregates results
      **/
    VulnerabilityUpdateStatus fullUpdate();

    /**
      * method called by ScheduleService, calls fullUpdate
      * returns the RequestStatus from the return value of fullUpdate, logs the other returned information of fullUpdate
      **/
   RequestStatus update();

    set<string> findCpes(1: string vendor, 2: string product, 3:string version);
}

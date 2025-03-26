/*
 * Copyright Siemens AG, 2014-2015, 2019. Part of the SW360 Portal Project.
 * With contributions by Bosch Software Innovations GmbH, 2016.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
    ExternalToolProcess process(1: string releaseId, 2: User user,3: string uploadDescription);

    /**
     * Since there should only be one actice Fossology process at most for a release
     * no extra identification needed. The active one, if there is one, will be marked
     * outdated, so that a new one can be invoked via process().
     * If setting the state was successful, SUCCESS is returned.
     **/
    RequestStatus markFossologyProcessOutdated(1: string releaseId, 2: User user);

    /**
     * only trigger the report generation for already scanned documents.
     **/
    RequestStatus triggerReportGenerationFossology(1: string releaseId, 2: User user);

    /**
     * Check unpack status for an upload Id.
     **/
    map<string, string> checkUnpackStatus(i32 uploadId);

    /**
     * Check scan status for a scan job Id.
     **/
    map<string, string> checkScanStatus(i32 scanJobId);

    /**
     * Check report generation status for a report Id.
     **/
    map<string, string> checkReportGenerationStatus(i32 reportId);
}

/*
 * Copyright (c) Bosch Software Innovations GmbH 2015.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

/*
     @author Andreas.Reichel@tngtech.com
*/

include "sw360.thrift"

namespace java org.eclipse.sw360.datahandler.thrift.importstatus
namespace php sw360.datahandler.thrift.importstatus

typedef sw360.RequestStatus RequestStatus


struct ImportStatus {
    // List of all Ids that where sucessfully imported.
    1: list<string> successfulIds;
    // Map of failed Ids (key) with a message why it failed (value)
    2: map<string, string> failedIds;
    3: RequestStatus requestStatus;
}

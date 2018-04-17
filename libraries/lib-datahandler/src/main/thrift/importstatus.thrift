/*
 * Copyright (c) Bosch Software Innovations GmbH 2015.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

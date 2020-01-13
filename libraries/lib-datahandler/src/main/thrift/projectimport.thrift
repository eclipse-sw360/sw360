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
     @author Maximilian.Huber@tngtech.com
     @author Andreas.Reichel@tngtech.com
     @author ksoranko@verifa.io
*/

include "projects.thrift"
include "sw360.thrift"
include "users.thrift"
include "importstatus.thrift"

namespace java org.eclipse.sw360.datahandler.thrift.projectimport
namespace php sw360.thrift.projectimport

typedef projects.Project Project
typedef users.User User
typedef importstatus.ImportStatus ImportStatus

struct RemoteCredentials {
1:  string username,
2:  string password,
3:  string serverUrl,
}

struct TokenCredentials {
1:  string token,
2:  string serverUrl,
3:  string userKey
}

service ProjectImportService {
   /**
    * check credentials with API
    **/
   bool validateCredentials(1: RemoteCredentials credentials)

   /**
    * returns a list of projects that can be imported with `reCred` credentials
    **/
   list<Project> loadImportables(1: RemoteCredentials reCred)

   /**
    * returns a list of projects that can be imported with `reCred` credentials,
    * where any word in the project name starts with the given string
    **/
   list<Project> suggestImportables(1: RemoteCredentials reCred, 2: string projectName)

   /**
    *  imports projects from external source specified by `projectIds` with credentials `reCred` and set user as creating
    *  user in SW360
    **/
   ImportStatus importDatasources(1: list<string> projectIds, 2: User user, 3: RemoteCredentials reCred);

   string getIdName();

   /**
   *  imports projects from external source specified by `projectIds` with credentials `tokenCred` and set user as creating
   *  user in SW360
   **/
   ImportStatus importData(1: list<string> projectIds, 2: User user, 3: TokenCredentials tokenCred);
}


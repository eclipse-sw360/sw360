/*
 * Copyright Siemens AG, 2014-2019. Part of the SW360 Portal Project.
 * With contributions by Bosch Software Innovations GmbH, 2016.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
include "users.thrift"
include "sw360.thrift"

namespace java org.eclipse.sw360.datahandler.thrift.changelogs
namespace php sw360.thrift.changelogs

typedef users.User User
typedef sw360.SW360Exception SW360Exception
typedef sw360.RequestStatus RequestStatus
enum Operation {
    CREATE = 0,
    UPDATE = 1,
    DELETE = 2,
    MODERATION_ACCEPT = 3,
    PROJECT_UPDATE = 4,
    COMPONENT_UPDATE = 5,
    RELEASE_CREATE=6,
    RELEASE_UPDATE=7,
    RELEASE_DELETE=8,
    MERGE_COMPONENT = 9,
    MERGE_RELEASE = 10,
    OBLIGATION_UPDATE = 11,
    OBLIGATION_ADD = 12,
    SPLIT_COMPONENT = 13,
    SPDXDOCUMENT_CREATE = 14,
    SPDXDOCUMENT_DELETE = 15,
    SPDX_DOCUMENT_CREATION_INFO_CREATE = 16,
    SPDX_DOCUMENT_CREATION_INFO_DELETE = 17,
    SPDX_PACKAGE_INFO_CREATE = 18,
    SPDX_PACKAGE_INFO_DELETE = 19
}

struct ChangeLogs {
    1: optional string id,
    2: optional string revision,
    3: optional string type = "changeLogs",
    4: optional string parentDocId,
    5: required string documentId,
    6: required string documentType,
    7: required string dbName,
    8: optional set<ChangedFields> changes,
    9: required Operation operation,
    10: required string userEdited,
    11: required string changeTimestamp,
    12: optional set<ReferenceDocData> referenceDoc,
    13: optional map<string,string> info,
}

struct ReferenceDocData
{
    1: required string refDocId,
    2: required string dbName,
    3: required string refDocType,
    4: required Operation refDocOperation,
}

struct ChangedFields
{
    1: required string fieldName,
    2: optional string fieldValueOld,
    3: optional string fieldValueNew,
}

service ChangeLogsService {
     /**
     * get Change Log by Id
     */
    ChangeLogs getChangeLogsById(1: string changeLogId) throws (1: SW360Exception exp);
    
     /**
     * get all Change Logs associated with Document Id
     */
    list<ChangeLogs> getChangeLogsByDocumentId(1: User user,2: string docId) throws (1: SW360Exception exp);

    /**
     * delete all Change Logs associated with Document,
    **/
    RequestStatus deleteChangeLogsByDocumentId(1: string documentId, 2: User user);
}

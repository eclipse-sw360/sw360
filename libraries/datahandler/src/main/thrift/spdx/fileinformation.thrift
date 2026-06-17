/*
 * Copyright TOSHIBA CORPORATION, 2021. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2021. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
include "sw360.thrift"
include "users.thrift"
include "documentcreationinformation.thrift"
include "snippetinformation.thrift"
include "annotations.thrift"
include "otherlicensinginformationdetected.thrift"


namespace java org.eclipse.sw360.datahandler.thrift.spdx.fileinformation
namespace php sw360.thrift.spdx.fileinformation

typedef sw360.RequestStatus RequestStatus
typedef sw360.RequestSummary RequestSummary
typedef sw360.AddDocumentRequestSummary AddDocumentRequestSummary
typedef sw360.DocumentState DocumentState
typedef sw360.SW360Exception SW360Exception
typedef sw360.PaginationData PaginationData
typedef users.User User
typedef users.RequestedAction RequestedAction
typedef documentcreationinformation.CheckSum CheckSum
typedef snippetinformation.SnippetInformation SnippetInformation
typedef annotations.Annotations Annotation
typedef otherlicensinginformationdetected.OtherLicensingInformationDetected OtherLicensingInformationDetected

struct FileInformation {
    1: optional string id,
    2: optional string revision,
    3: optional string type = "spdxFileInformation",
    4: optional string fileName,
    5: optional string SPDXID,
    6: optional set<string> fileTypes,
    7: optional set<CheckSum> checksums,
    8: optional string licenseConcluded,
    9: optional set<string> licenseInfoInFiles,
    10: optional string licenseComments,
    11: optional string copyrightText,
    12: optional string fileComment,
    13: optional string noticeText,
    14: optional set<string> fileContributors,
    15: optional set<string> fileAttributionText,
    16: optional set<SnippetInformation> snippetInformation,
    17: optional set<OtherLicensingInformationDetected> hasExtractedLicensingInfos,
    18: optional set<Annotation> annotations,
}

service FileInformationService {
    list<FileInformation> getFileInformationsShort(1: set<string> ids, 2: User user);
    list<FileInformation> getFileInformationSummary(1: User user);
    FileInformation getFileInformationById(1: string id, 2: User user);
    RequestStatus addFileInformation(1: FileInformation fileInformation, 2: User user);
    AddDocumentRequestSummary addFileInformations(1: set<FileInformation> fileInformations, 2: User user);
    RequestStatus updateFileInformation(1: FileInformation fileInformation, 2: User user);
    RequestSummary updateFileInformations(1: set<FileInformation> fileInformations, 2: User user);
    RequestStatus deleteFileInformation(1: string id, 2: User user);
}

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
namespace java org.eclipse.sw360.datahandler.thrift.spdx.snippetinformation
namespace php sw360.thrift.spdx.snippetinformation

struct SnippetInformation {
    1: optional string SPDXID,  // 9.1
    2: optional string snippetFromFile ,    // 9.2
    3: optional set<SnippetRange> snippetRanges,    // 9.3, 9.4
    4: optional string licenseConcluded,    // 9.5
    5: optional set<string> licenseInfoInSnippets,  // 9.6
    6: optional string licenseComments,     // 9.7
    7: optional string copyrightText,       // 9.8
    8: optional string comment,             // 9.9
    9: optional string name,                // 9.10
    10: optional string snippetAttributionText, // 9.11
    11: optional i32 index,
}

struct SnippetRange {
    1: optional string rangeType,
    2: optional string startPointer,
    3: optional string endPointer,
    4: optional string reference,
    5: optional i32 index,
}

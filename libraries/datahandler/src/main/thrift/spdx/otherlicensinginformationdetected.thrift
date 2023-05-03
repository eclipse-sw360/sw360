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
namespace java org.eclipse.sw360.datahandler.thrift.spdx.otherlicensinginformationdetected
namespace php sw360.thrift.spdx.otherlicensinginformationdetected

struct OtherLicensingInformationDetected {
    1: optional string licenseId,               // 10.1
    2: optional string extractedText,           // 10.2
    3: optional string licenseName,             // 10.3
    4: optional set<string> licenseCrossRefs,   // 10.4
    5: optional string licenseComment,          // 10.5
    6: optional i32 index,
}

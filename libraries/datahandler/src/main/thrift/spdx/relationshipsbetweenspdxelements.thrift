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
namespace java org.eclipse.sw360.datahandler.thrift.spdx.relationshipsbetweenspdxelements
namespace php sw360.thrift.spdx.relationshipsbetweenspdxelements

struct RelationshipsBetweenSPDXElements {
    1: optional string spdxElementId,       // 11.1
    2: optional string relationshipType,    // 11.1
    3: optional string relatedSpdxElement,  // 11.1
    4: optional string relationshipComment, // 11.2
    5: optional i32 index,
}

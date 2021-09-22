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
namespace java org.eclipse.sw360.datahandler.thrift.spdx.annotations
namespace php sw360.thrift.spdx.annotations

struct Annotations {
    1: optional string annotator,           // 8.1
    2: optional string annotationDate,      // 8.2
    3: optional string annotationType,      // 8.3
    4: optional string spdxIdRef,           // 8.4
    5: optional string annotationComment,   // 8.5
    6: optional string index,
}
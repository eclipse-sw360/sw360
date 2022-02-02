/**
 * @license
 * Copyright (c) Bosch Software Innovations GmbH 2017-2018.
 * Copyright (c) Bosch.IO GmbH 2022.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
export enum ComponentType {
    INTERNAL     = 'INTERNAL',
    OSS          = 'OSS',
    COTS         = 'COTS',
    FREESOFTWARE = 'FREESOFTWARE',
    INNER_SOURCE = 'INNER_SOURCE',
    SERVICE      = 'SERVICE',
}

interface Attachment { x: string; }
interface Release { x: string; }
export interface Component {
    id: string;
    revision: string;
    type: 'component';
    name: string;
    description: string;
    attachments: Set<Attachment>;
    createdOn: string;
    componentType: ComponentType;
    createdBy: string;
    subscribers: Set<string>;
    moderators: Set<string>;
    componentOwner: string;
    ownerAccountingUnit: string;
    ownerGroup: string;
    roles: Map<string, Set<string>>;
    releases: Release[];
    releaseIds: Set<string>;
    mainLicenseIds: Set<string>;
    categories: Set<string>;
    languages: Set<string>;
    softwarePlatforms: Set<string>;
    operatingSystems: Set<string>;
    vendorNames: Set<string>;
    homepage: string;
    mailinglist: string;
    wiki: string;
    blog: string;
    wikipedia: string;
    openHub: string;
    // DocumentState documentState;
    // Map<RequestedAction, Boolean> permissions;
    // private static final Component._Fields[] optionals;
    // public static final Map<Component._Fields, FieldMetaData> metaDataMap;
}

// export interface Component {
//     name: string;
//     description: string;
//     createdOn: string;
//     type: 'component';
//     componentType: string;
//     _links: Links;
//     _embedded: Embedded;
// }

// interface Self {
//     href: string;
// }

// interface Links {
//     self: Self;
// }

// interface Embedded {
//     createdBy: string;
//     releases: any[];
//     moderators: string[];
//     vendors: string[];
// }

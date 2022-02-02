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
import { Injectable } from "@angular/core";

@Injectable()
export class ProjectTableService {
    getProjects(amount: number) {
        return mockProjects(amount);
    }
}

export const mockId = (): string => {
    const r = Math.random().toString(36).substr(2, 12);
    return r + r;
}

export const mockValue = (s: string[] | string): string => {
    return (typeof s === 'string')
        ? s
        : s[Math.floor(Math.random() * s.length)];
}

// export interface ExternalIds {
//     [key: string]: string;
// }

export interface Release {

}

export interface Project {
    
    // Details
    id: string;
    revision: string;
    type: 'project';
    name: string;
    description: string;
    version: string;
    // externalIds: ExternalIds;
    // public Set < Attachment > attachments;
    createdOn: string;
    businessUnit: string;
    // public ProjectType projectType;
    tag: string;
    createdBy: string;
    
    // Users
    projectResponsible: string;
    leadArchitect: string;
    // public Set < String > moderators;
    // public Set < String > contributors;
    // public Visibility visbility;
    // public Map < String, Set < String >> roles;
    // public Set < String > securityResponsibles;
    projectOwner: string;
    ownerAccountingUnit: string;
    ownerGroup: string;
    // public Map < String, ProjectRelationship > linkedProjects;
    // public Map < String, ProjectReleaseRelationship > releaseIdToUsage;
    clearingTeam: string;
    
    // State
    state: string; // public ProjectState state;
    clearingState: string; // public ProjectClearingState clearingState;
    preevaluationDeadline: string;
    systemTestStart: string;
    systemTestEnd: string;
    deliveryStart: string;
    phaseOutSince: string;
    // public DocumentState documentState;
    // public ReleaseClearingStateSummary releaseClearingStateSummary;

    // ???
    enableSvm: boolean;
    licenseInfoHeaderText: string;
    homepage: string;
    wiki: string;
    // public Map < RequestedAction, Boolean > permissions;




    // Test embedded realeases
    releases: Release[];
}

// region mock
const DATES = ['2016-12-11', '2018-01-23', '2017-03-18', '2015-01-04', '2018-03-14', '2017-12-01'];
const REVISIONS = ['1.1.3', '1.2.3', '3.1.3', '4.1.3', '1.5.2', '8.1.3', '2.1.3', '1.0.3', '1.0.4', '3.1.3', '4.1.3', '1.5.2', '8.1.3', '2.1.3', 'xyz-core-1.0.3', 'abc-1.1'];
const TYPE = 'project';
const NAMES = ['Evadne Symaithis', 'Ornia Crotheise', 'Caprea Bungeana', 'Albizia Anchusa', 'Nepta Anisum', 'Begonia Bumelia', 'Gilliphae Lavendera', 'Hollis Buxise', 'Clovea Hempae', 'Daisis Fernosia'];
const DESCRIPTIONS = [
    'A short and pregnant description for a project or other resource.',
    // 'Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren',
    // 'Sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam',
    // 'Labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam',
    // 'At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam',
    // 'Consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam',
    // 'Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam',
];
const VERSIONS = REVISIONS;
const CREATED_ONS = DATES;
const BUSINESS_UNITS = NAMES;
const TAGS = ['tagged', 'needs discussion', 'production ready', 'unclear', 'random'];
const CREATED_BYS = NAMES;
const PROJECT_RESPONSIBLES = NAMES;
const LEAD_ARCHITECTS = NAMES;
const PROJECT_OWNERS = NAMES;
const OWNER_ACCOUNTING_UNITS = NAMES;
const OWNER_GROUPS = NAMES;
const CLEARING_TEAMS = NAMES;
const PREEVALUATION_DEADLINES = DATES;
const SYSTEM_TEST_STARTS = DATES;
const SYSTEM_TEST_ENDS = DATES;
const DELIVERY_STARTS = DATES;
const PHASE_OUT_SINCES = DATES;
// const ENABLE_SVMS = [true, false];
const LICENSE_INFO_HEADER_TEXTS = [
    'Use of this source code is governed by a BSD-style license that can be found in the LICENSE file or at https://developers.google.com/open-source/licenses/bsd',
    'Use of this source code is governed by an MIT-style license that can be found in the LICENSE file or at https://opensource.org/licenses/MIT.',
    'This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License version 2 as published by the Free Software Foundation.'
];
const HOMEPAGES = [
    'http://symaithis.com',
    'http://bungeana.com',
    'http://nepta.com',
    'http://consetetur-sadipscing.com',
    'http://diam-nonumy.com',
    'http://vero-eos-et.com',
    'http://dolores-et-ea.com',
    'http://consetetur-sadipscing.com',
    'http://consetetur-sadipscing-nonumy.com',
    'http://sed-diam-nonumy.com',
];
const WIKIS = HOMEPAGES;
const STATES = ['ACTIVE', 'PHASE_OUT', 'UNKNOWN'];
const CLEARING_STATES = ['0', '1', '2', '3'];

export const mockProject = (): Project => {
    return {
        id: mockId(),
        revision: mockValue(REVISIONS),
        type: TYPE,
        name: mockValue(NAMES),
        description: mockValue(DESCRIPTIONS),
        version: mockValue(VERSIONS),
        createdOn: mockValue(CREATED_ONS),
        businessUnit: mockValue(BUSINESS_UNITS),
        tag: mockValue(TAGS),
        createdBy: mockValue(CREATED_BYS),
        projectResponsible: mockValue(PROJECT_RESPONSIBLES),
        leadArchitect: mockValue(LEAD_ARCHITECTS),
        projectOwner: mockValue(PROJECT_OWNERS),
        ownerAccountingUnit: mockValue(OWNER_ACCOUNTING_UNITS),
        ownerGroup: mockValue(OWNER_GROUPS),
        clearingTeam: mockValue(CLEARING_TEAMS),
        preevaluationDeadline: mockValue(PREEVALUATION_DEADLINES),
        systemTestStart: mockValue(SYSTEM_TEST_STARTS),
        systemTestEnd: mockValue(SYSTEM_TEST_ENDS),
        deliveryStart: mockValue(DELIVERY_STARTS),
        phaseOutSince: mockValue(PHASE_OUT_SINCES),
        enableSvm: Math.random() > 0.5 ? true : false,
        licenseInfoHeaderText: mockValue(LICENSE_INFO_HEADER_TEXTS),
        homepage: mockValue(HOMEPAGES),
        wiki: mockValue(WIKIS),
        state: mockValue(STATES),
        clearingState: mockValue(CLEARING_STATES),

        // Test embedded releases
        releases: []
    };
};

export const mockProjects = (n: number): Project[] => {
    const projects: Project[] = [];
    for (let i = 0; i < n; i++)
        projects.push(mockProject());
    return projects;
};
// endregion

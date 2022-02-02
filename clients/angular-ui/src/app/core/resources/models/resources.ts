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
import { Type } from '@angular/core';
import { RequestSortDirection } from '../components/resource-table/resource-table.component';
import { JSONSchema7 } from "json-schema";

/**
 * API resource types.
 */
export enum ResourceType {
    attachment = 'attachment',
    component = 'component',
    license = 'license',
    project = 'project',
    release = 'release',
    user = 'user',
    vendor = 'vendor'
}

export interface ResourceTypeUriMap {
    [type: string]: string;
}

/**
 * API resource uri segments.
*/
export const resourceTypeUriMap: ResourceTypeUriMap = {
    [ResourceType.attachment]: 'attachments',
    [ResourceType.component]: 'components',
    [ResourceType.license]: 'licenses',
    [ResourceType.project]: 'projects',
    [ResourceType.release]: 'releases',
    [ResourceType.user]: 'users',
    [ResourceType.vendor]: 'vendors'
};

export interface ResourceTypeIndexMap {
    [type: string]: string;
}

/**
 * API _embedded resource access indices.
 */
export const resourceTypeIndexMap: ResourceTypeIndexMap = {
    [ResourceType.attachment]: 'sw360:attachments',
    [ResourceType.component]: 'sw360:components',
    [ResourceType.license]: 'sw360:licenses',
    [ResourceType.project]: 'sw360:projects',
    [ResourceType.release]: 'sw360:releases',
    [ResourceType.user]: 'sw360:users',
    [ResourceType.vendor]: 'sw360:vendors'
};

/**
 * Resource action interface.
 */
export interface ResourceAction {
    title: string;
    action: Function;
    oneSelected?: boolean;
    manySelected?: boolean;
    noneSelected?: boolean;
    matIconString?: string;
}

export interface EnumMap {
    [key: string]: string;
}

export interface JSONSchema extends JSONSchema7 {
    client: ApiClientSchema;
}

export enum FormType {
    // A string input
    input = "input",
    // A string textarea
    textarea = "textarea",
    // A datepicker
    date = "date",
    // Both selectOne and selectMany either contain values through oneOf or anyOf
    // or there is referenced data (stated by having a resourceType)
    // which means an user can select from an embedded resource table.
    selectOne = "selectOne",   // if resourceType else self contained -> enum in oneOf, anyOf
    selectMany = "selectMany", // if resourceType else self contained -> enum in oneOf, anyOf
    // A string array
    array = "array",
    // A string: string map
    map = "map",
    // If the formType of a client object is embedded those values can be created and edited via the
    // embedded resource tables inside a resource detail
    embedded = "embedded"
}

// TODO: Introduce an order property
// -> forms order, table property select order
export interface ApiClientSchema {
    enumMap: EnumMap;
    title: string;
    description: string;
    visible: boolean;
    tooltip: string;
    group: string;
    editable: boolean;
    formType: FormType;
    formError: string;
    filterable: boolean;
    sortable: boolean;
    pattern: string;
    resourceType: ResourceType
}

export interface ResourceProperty extends ApiClientSchema {
    property: string; // key
    required: boolean; // derived from JSONSchema required array
    type: string; // JSONSchema property type
    
    // table specific (persisted by store during session)
    checked: boolean;
    sorted: boolean;
    sortDirection: RequestSortDirection;
}

// TODO: is this thing used?
export class ResourceComponentModel {
    constructor(public component: Type<any>, public data: any) { }
}

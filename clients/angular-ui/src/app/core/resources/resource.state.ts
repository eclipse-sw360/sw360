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
import { Action, createSelector } from '@ngrx/store';
import { AppState } from '../../app.state';
import { ResourceType, ResourceProperty, JSONSchema, FormType } from './models/resources';

interface ResourcePropertiesMap {
    [resourceType: string]: ResourceProperty[];
}

interface SchemaMap {
    [resourceType: string]: JSONSchema;
}

export interface ResourceState {
    // Dispatching true will trigger the resource table to update.
    // The resource table dispatches false after it updates.
    resourceTableChanged: boolean;

    resourceProperties: ResourcePropertiesMap;
    schemes: SchemaMap;
}

const initialResourceState: ResourceState = {
    resourceTableChanged: false,
    resourceProperties: {},
    schemes: {}
};

export const SET_RESOURCE_TABLE_CHANGED = '[Resource] Set resource table changed';
export const SET_RESOURCE_PROPERTIES = '[Resource] Set resource properties';
export const SET_SCHEMA = '[Resource] Set schema';

export class SetResourceTableChanged implements Action {
    readonly type = SET_RESOURCE_TABLE_CHANGED;
    constructor(public resourceTableChanged: boolean) { }
}

export class SetResourceProperties implements Action {
    readonly type = SET_RESOURCE_PROPERTIES;
    constructor(public resourceType: ResourceType | string, public resourceProperties: ResourceProperty[]) { }
}

export class SetSchema implements Action {
    readonly type = SET_SCHEMA;
    constructor(public resourceType: ResourceType | string, public schema: JSONSchema) { }
}

export type ResourceActions =
    SetResourceTableChanged |
    SetResourceProperties |
    SetSchema
    ;

export function resourceReducer(state: ResourceState = initialResourceState, action: ResourceActions) {
    switch (action.type) {
        case SET_RESOURCE_TABLE_CHANGED:
            return { ...state, resourceTableChanged: action.resourceTableChanged }
        case SET_RESOURCE_PROPERTIES: {
            const resourceProperties = { ...state.resourceProperties };
            resourceProperties[action.resourceType] = action.resourceProperties;
            return { ...state, resourceProperties: resourceProperties };
        }
        case SET_SCHEMA: {
            const schemes = { ...state.schemes };
            schemes[action.resourceType] = action.schema;
            return { ...state, schemes: schemes };
        }
        default:
            return state;
    }
}

// ResourceTableChanged
///////////////////////
export const selectResourceTableChanged = (state: AppState) => state.resource.resourceTableChanged;

// ResourceProperties
/////////////////////
export const selectAllResourceProperties = (state: AppState) => state.resource.resourceProperties;
export const selectResourceProperties = (resourceType: ResourceType) => createSelector(
    selectAllResourceProperties,
    resourceProperties => resourceProperties[resourceType]
);
export const selectTableResourceProperties = (resourceType: ResourceType) => createSelector(
    selectAllResourceProperties,
    resourceProperties => resourceProperties[resourceType]
        // .map(x => {
        //     console.warn("TODO: remove: ", x)
        //     return x;
        // })
        .filter(property =>
            property.visible &&
            property.formType !== FormType.embedded)
);
export const selectFormsResourceProperties = (resourceType: ResourceType) => createSelector(
    selectAllResourceProperties,
    resourceProperties => resourceProperties[resourceType]
        .filter(property =>
            property.visible &&
            property.editable &&
            property.formType !== FormType.embedded)
);

// Schemes
//////////
export const selectAllSchemes = (state: AppState) => state.resource.schemes;
export const selectSchema = (resourceType: ResourceType) => createSelector(
    selectAllSchemes,
    schemes => schemes[resourceType]
);

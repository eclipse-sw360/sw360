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
import { ApiClientSchema, JSONSchema } from '../../resources/models/resources';
import { Injectable } from "@angular/core";
import { ResourceType, ResourceProperty } from "../../resources/models/resources";
import { Store } from '@ngrx/store';
import { AppState } from '../../../app.state';
import { HttpService } from '../../http/http.service';
import { SetResourceProperties, SetSchema } from '../../resources/resource.state';

/*
TODO: Json Schema requirements:
- Where to put the pattern? In normal schema or Client?
- Where to define the formType enum? This is actually client stuff, but should be carried by schema..
*/

@Injectable()
export class JsonSchemaService {
    constructor(private store: Store<AppState>, private httpService: HttpService) {
        // Provide schemes for all resource types.
        Object.values(ResourceType)
            .filter(resourceType =>  // TODO: remove
                resourceType === ResourceType.project ||
                resourceType === ResourceType.component ||
                resourceType === ResourceType.release ||
                resourceType === ResourceType.user
            )
            .forEach(resourceType => this.provideSchema(resourceType));
    }

    /**
     * Sets the resource schema and the derived ClientSchemaMap in store.
     * @param resourceType The resource type.
     */
    provideSchema(resourceType: ResourceType | string) {
        this.httpService.getSchema(resourceType).subscribe((schema: JSONSchema) => {
            this.store.dispatch(new SetSchema(resourceType, schema));
            this.store.dispatch(new SetResourceProperties(resourceType, this.getInitialResourceProperties(schema)));
        });
    }

    getInitialResourceProperties(schema: JSONSchema): ResourceProperty[] {
        const resourceProperties: ResourceProperty[] = [];
        Object.entries(schema.properties).forEach(([property, schemaProperty]: [string, any]) => {
            const resourceProperty: ResourceProperty = { ...schemaProperty.client };
            resourceProperty.property = property;
            resourceProperty.type = schemaProperty.type.toString();
            resourceProperty.required = schema.required.includes(property);
            resourceProperty.checked = false;
            resourceProperty.sorted = false;
            resourceProperty.sortDirection = undefined;
            resourceProperties.push(resourceProperty);
        });
        return resourceProperties;
    }
}

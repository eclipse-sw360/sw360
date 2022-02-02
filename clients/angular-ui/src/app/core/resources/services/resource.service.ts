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
import { ResourceType, resourceTypeUriMap, resourceTypeIndexMap, ResourceAction } from '../models/resources';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { take, map, filter } from 'rxjs/operators';
import { UrlSegment } from '@angular/router/src/url_tree';
import { AppState } from '../../../app.state';
import { Store } from '@ngrx/store';
import { HttpService } from '../../http/http.service';
import { HttpParams } from '@angular/common/http';
import { ComponentService } from '../../../features/component/services/component.service';
import { FormGroup } from '@angular/forms';
import { JsonSchemaService } from '../../json-schema/services/json-schema.service';
import { JsonSchemaFormsService } from '../../json-schema/services/json-schema-forms.service';
import { SetResourceTableChanged } from '../resource.state';
import { SelectedResourcesMap } from '../components/resource-table/resource-table.component';
import { Router } from '@angular/router';

export interface ApiPage {
    number: number;
    size: number;
    totalElements: number;
    totalPages: number;
}

interface ApiResourcesResponse {
    _embedded: any;
    _links: any;
    page: ApiPage;
}

export interface ClientResourcesResponse {
    resources: any[];
    page: ApiPage;
}

/**
 * SW360 resources utility service.
 */
@Injectable()
export class ResourceService {
    constructor(
        private httpService: HttpService,
        private store: Store<AppState>,
        private componentService: ComponentService,
        private jsonSchemaService: JsonSchemaService,
        private router: Router
    ) { }

    /**
     * Returns the ResourceType (enum) of a ResourceUriSegment (enum) as string or undefined.
     * Hint: Handling resourceUriSegment and resourceType as string comes from JavaScript limitations.
     * @param resourceUriSegment The ResourceUriSegment (enum) as string.
     */
    getResourceTypeFromResourceUriSegment(resourceUriSegment: string): string | undefined {
        let resourceTypeResult: string = undefined;
        Object.keys(ResourceType).forEach(resourceType => {
            if (resourceUriSegment.includes(resourceType))
                resourceTypeResult = resourceType;
        });
        return resourceTypeResult;
    }

    // TODO:
    getResources(resourceType: ResourceType, params?: HttpParams): Observable<ClientResourcesResponse> {
        return this.httpService.get(resourceTypeUriMap[resourceType], params).pipe(
            map((resourcesResponse: ApiResourcesResponse) => {
                // TODO: handle this when requesting releases (flat vs embedded request) is defined.
                return {
                    resources: resourcesResponse._embedded[resourceTypeIndexMap[resourceType]],
                    page: resourcesResponse.page
                }
            })
        );
    }

    postResource(resourceType: ResourceType, body: any) {
        this.httpService.post(resourceTypeUriMap[resourceType], body).subscribe(resource => this.routeToSelflink(resourceType, resource));
    }

    routeToSelflink(resourceType: ResourceType, resource: any) {
        const selflink = resource._links.self.href;
        if (selflink)
            this.router.navigate([resourceTypeUriMap[resourceType], this.httpService.getIdFromSelflink(selflink)]);
    }

    patchResource(resourceType: ResourceType, selflink: string, body: any) {
        this.httpService.patch(selflink, body).subscribe(resource => this.routeToSelflink(resourceType, resource));
    }

    getResource(resourceType: ResourceType, selflink: string) {
        // TODO: users/byid/id ... (gets changed by API anyway, but for now...)
        if (resourceType === ResourceType.user)
            return this.httpService.getUser(selflink).pipe(map(resource => {
                // TODO: This can be removed when the API always delivers the type inside a resource.
                return { ...resource, type: resourceType };
            }));

        return this.httpService.getResource(selflink).pipe(map(resource => {
            // TODO: This can be removed when the API always delivers the type inside a resource.
            return { ...resource, type: resourceType };
        }));
    }

    getResourceByUrl(resourceType: ResourceType, url: string) {
        return this.getResource(resourceType, this.httpService.getSelflinkFromId(resourceType, this.httpService.getIdFromSelflink(url)));
    }

    getActions(resourceType: ResourceType) {
        // TODO: get crud actions from below and extend by resource specific actions here
        const resourceActions: ResourceAction[] = [
            { title: 'Create', action: this.createResourceAction(), matIconString: 'add', noneSelected: true, oneSelected: true, manySelected: true },
            { title: 'Edit', action: this.editResourceAction(), matIconString: 'edit', oneSelected: true },
            { title: 'Delete', action: this.deleteResourcesAction(), matIconString: 'delete', oneSelected: true, manySelected: true },
        ];
        let resourceSpecificActions: ResourceAction[] = [];
        switch (resourceType) {
            case ResourceType.component: resourceSpecificActions = this.componentService.getActions();
            // TODO: other resource types
            // case ResourceType.project: resourceSpecificActions = this.projectService.getActions();
            default: break;
        }
        resourceSpecificActions.forEach(a => resourceActions.push(a));
        return resourceActions;
    }

    // Create, Edit, Delete Actions (available for all resources)
    /////////////////////////////////////////////////////////////
    createResourceAction(resourceType?: ResourceType, selectedResources?: SelectedResourcesMap) {
        return (resourceType, selectedResources) => this.router.navigate([resourceTypeUriMap[resourceType], 'create']);
    }

    editResourceAction() {
        return (resourceType, selectedResources) => {
            let selflink = Object.keys(selectedResources)[0];
            const id = this.httpService.getIdFromSelflink(selflink);
            this.router.navigate([resourceTypeUriMap[resourceType], id, 'edit'], {
                queryParams: { edit: true, selflink: selflink }
            });
        }
    }

    deleteResourcesAction(resourceType?: ResourceType, selectedResources?: SelectedResourcesMap) {
        return (resourceType, selectedResources) => {
            const ids = Object.keys(selectedResources)
                .map(selflink => this.httpService.getIdFromSelflink(selflink))
                .reduce((p, n) => p + ',' + n);
            let path = `${resourceTypeUriMap[ResourceType.component]}/${ids}`;
            this.httpService.delete(path)
                .subscribe(() => this.store.dispatch(new SetResourceTableChanged(true)));
        }
    }

}

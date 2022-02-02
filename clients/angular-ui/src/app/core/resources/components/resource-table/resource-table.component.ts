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
import { resourceTypeUriMap } from '../../models/resources';
import { SetResourceTableChanged, SetResourceProperties, selectTableResourceProperties } from '../../resource.state';
import { Component, ViewChild, OnInit, OnDestroy, Input, Output, EventEmitter } from "@angular/core";
import { ActivatedRoute } from '@angular/router';
import { take, map, filter } from 'rxjs/operators';
import { ResourceType, ResourceAction, ResourceProperty } from '../../models/resources';
import { ResourceService, ClientResourcesResponse, ApiPage } from '../../services/resource.service';
import { TitleService } from '../../../view/services/title.service';
import { Subscription, Observable, forkJoin } from 'rxjs';
import { SW360RouteData } from '../../../routing/routes';
import { HttpParams } from '@angular/common/http';
import { AppState } from '../../../../app.state';
import { Store } from '@ngrx/store';
import { ComponentService } from '../../../../features/component/services/component.service';
import { selectResourceTableChanged } from '../../resource.state';
import { FormGroup, FormBuilder, FormControl, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpService } from '../../../http/http.service';

export enum RequestSortDirection {
    asc = 'asc',
    desc = 'desc'
}

interface RequestSortParam {
    property: string;
    direction: RequestSortDirection;
}

interface RequestFilter {
    property: string;
    filter: string;
}

interface RequestParams {
    page: number;
    page_entries: number;
    sort: RequestSortParam;
    fields: string[];
    filters: RequestFilter[];
}

// The mat-paginator page($event) data
interface MatPaginatorEvent {
    previousPageIndex: number;
    pageIndex: number;
    pageSize: number;
    length: number;
}

interface Pagination extends MatPaginatorEvent {
    pageSizeOptions: number[];
    showFirstLastButtons: boolean;
}


interface Resource {
    open: boolean;
    selected: boolean;
    data: any;
    _links: any;
}

export interface SelectedResourcesMap {
    [selflink: string]: Resource;
}

interface ResourcesResourcePropertiesFork {
    resources: Resource[];
    page: ApiPage;
    resourceProperties: ResourceProperty[];
}

export enum SelectionType {
    radio = "radio",
    checkbox = "checkbox"
}

// TODO: persist selection until component gets destroyed
// check for each results if the element is selected and set it appropriate!

@Component({
    selector: 'sw-resource-table',
    templateUrl: './resource-table.component.html',
    styleUrls: ['./resource-table.component.scss'],
})
export class ResourceTableComponent implements OnInit, OnDestroy {

    @Input() resourceType: ResourceType; // The current resourceType.
    @Input() selectionType: SelectionType = SelectionType.checkbox;
    @Input() showActions: boolean = true;
    @Output() $selectedResources = new EventEmitter<SelectedResourcesMap>();

    // Data
    resources: Resource[];
    resourceProperties: ResourceProperty[];

    // Actions
    actions: ResourceAction[];

    // Selection
    noneSelected: boolean;
    oneSelected: boolean;
    manySelected: boolean;
    allSelected: boolean;
    selectedResources: SelectedResourcesMap;
    selectedResourcesLength: number;

    // Pagination
    pagination: Pagination;

    // Activated route data
    activatedRouteSub: Subscription;
    // This can be set from anywhere (store) and should update the table.
    resourceTableChangedSub: Subscription;

    // Local request params persistence. This is used to build the request HttpParams.
    requestParams: RequestParams = {
        page: 0,
        page_entries: 10,
        sort: { property: 'name', direction: RequestSortDirection.asc },
        fields: [],
        filters: []
    };

    // Filter
    showFilter = false; // TODO: set false as default (after complete)
    filterFormGroup: FormGroup;
    filterFormKeys: string[] = [];
    filterInputFormAddition = '-input';

    constructor(
        private activatedRoute: ActivatedRoute,
        private resourceService: ResourceService,
        private titleService: TitleService, // TODO: remove
        private store: Store<AppState>,
        private formBuilder: FormBuilder,
        private router: Router,
        private httpServie: HttpService
    ) {
        this.filterFormGroup = this.formBuilder.group({});
        titleService.setSync('0', 'TODO: INTO STATE');
    }

    addFilter() {
        const selectKey = (++Object.keys(this.filterFormGroup.controls).length).toString();
        this.filterFormKeys.push(selectKey);
        const inputKey = selectKey + this.filterInputFormAddition;
        this.filterFormGroup.addControl(selectKey, new FormControl('', Validators.required));
        this.filterFormGroup.addControl(inputKey, new FormControl('', Validators.required));
    }

    removeFilter(key: string) {
        this.filterFormKeys.splice(this.filterFormKeys.indexOf(key), 1);
        this.filterFormGroup.removeControl(key);
        this.filterFormGroup.removeControl(key + this.filterInputFormAddition);
        this.updateRequestFilters();
    }

    removeAllFilters() {
        this.filterFormKeys = [];
        Object.keys(this.filterFormGroup.controls).forEach(key =>
            this.filterFormGroup.removeControl(key));
        this.updateRequestFilters();
    }

    ngOnInit(): void {
        this.update();
        this.resourceTableChangedSub = this.store.select(selectResourceTableChanged).subscribe(changed => {
            if (changed) {
                this.store.dispatch(new SetResourceTableChanged(false));
                this.update();
            }
        });
    }

    goToResource(resource: Resource) {
        this.router.navigate([resourceTypeUriMap[this.resourceType], this.httpServie.getIdFromSelflink(resource._links.self.href)]);
    }

    updateRequestFilters() {
        this.requestParams.filters = [];
        const keys = Object.keys(this.filterFormGroup.value)
            .filter(v => !v.includes(this.filterInputFormAddition));

        keys.forEach(key => this.requestParams.filters.push({
            property: this.filterFormGroup.value[key],
            filter: this.filterFormGroup.value[key + this.filterInputFormAddition]
        }));
        this.store.dispatch(new SetResourceTableChanged(true));
    }

    updateRequestPagination(page: number, page_entries: number) {
        this.requestParams.page = page;
        this.requestParams.page_entries = page_entries;
        this.store.dispatch(new SetResourceTableChanged(true));
    }

    updateRequestSort(sort: RequestSortParam) {
        this.requestParams.sort = sort;
        this.store.dispatch(new SetResourceTableChanged(true));
    }

    updateRequestFields(fields: string[]) {
        this.requestParams.fields = fields;
        this.store.dispatch(new SetResourceTableChanged(true));
    }

    getHttpParams(): HttpParams {
        let params = new HttpParams()
            .set('page', this.requestParams.page.toString())
            .set('page_entries', this.requestParams.page_entries.toString())
            .set('sort', `${this.requestParams.sort.property},${this.requestParams.sort.direction}`)
            .set('fields', this.requestParams.fields.length > 0
                ? this.requestParams.fields.reduce((p, n) => p + ',' + n)
                : ''
            );
        this.requestParams.filters.forEach(filter =>
            params = params.set(filter.property, filter.filter));
        return params;
    }

    update() {
        this.resourceType
            ? this.updateResources(this.resourceType)
            : this.activatedRoute.data.pipe(take(1))
                .subscribe((routeData: SW360RouteData) => this.updateResources(routeData.resourceType));
    }

    updateResources(resourceType: ResourceType) {
        this.resources = [];
        this.resourceProperties = [];

        this.resourceType = resourceType;
        const params = this.getHttpParams();

        const resourcesResponse$: Observable<ClientResourcesResponse> =
            this.resourceService.getResources(this.resourceType, params); // Cold by HttpClient
        const resourceProperties$: Observable<ResourceProperty[]> =
            this.store.select(selectTableResourceProperties(this.resourceType)).pipe(take(1)); // Cold via take(1)

        forkJoin(resourcesResponse$, resourceProperties$).pipe(
            map((fork: any[]): ResourcesResourcePropertiesFork => {
                return {
                    resources: fork[0].resources,
                    page: fork[0].page,
                    resourceProperties: fork[1]
                };
            })
        ).subscribe(data => {
            // Properties contained in the response are checked by default.
            const responseContainedProperties = {};
            data.resources.forEach(resource => Object.keys(resource).forEach(property => responseContainedProperties[property] = true));
            data.resourceProperties.forEach(resourceProperty => {
                Object.keys(responseContainedProperties).forEach(property => {
                    if (resourceProperty.property === property) resourceProperty.checked = true;
                });
                if (resourceProperty.property === this.requestParams.sort.property) {
                    resourceProperty.sorted = true;
                    resourceProperty.sortDirection = this.requestParams.sort.direction;
                }
            });
            this.resourceProperties = data.resourceProperties;
            this.store.dispatch(new SetResourceProperties(this.resourceType, this.resourceProperties));

            // Resources (data)
            ///////////////////
            const checkedProperties = this.resourceProperties
                .filter(resourceProperty => resourceProperty.checked)
                .map(resourceProperty => resourceProperty.property);

            const alignedResponseData = [];
            data.resources.forEach(resource => {
                const alignedResource: Resource = { open: false, selected: false, data: [], _links: resource._links };
                checkedProperties.forEach(checkedProperty =>
                    resource[checkedProperty]
                        ? alignedResource.data.push(resource[checkedProperty])
                        : alignedResource.data.push(''));
                alignedResponseData.push(alignedResource);
            });
            this.resources = alignedResponseData;
            // TODO:
            // The page object should never be undefined, this check is neccessary as long as
            // the API does not support pagination for any requested resources.
            if (data.page)
                this.updatePagination(data.page);
        });
        this.updateSelection();
        this.updateActions(this.resourceType);
    }

    invoceRowAction(action: Function, resource: Resource) {
        action(this.resourceType, { [resource._links.self.href]: true });
    }

    ngOnDestroy(): void {
        if (this.resourceTableChangedSub) this.resourceTableChangedSub.unsubscribe();
        if (this.activatedRouteSub) this.activatedRouteSub.unsubscribe();
    }

    updatePagination(page: ApiPage) {
        this.pagination = {
            previousPageIndex: page.number > 0 ? page.number - 1 : 0,
            pageIndex: page.number,
            pageSize: page.size,
            length: page.totalElements,
            pageSizeOptions: [5, 10, 25, 100],
            showFirstLastButtons: true
        };
    }

    // TODO: Actions???
    updateActions(resourceType: ResourceType) {
        this.actions = this.showActions
            ? this.resourceService.getActions(resourceType)
            : [];
    }

    updateSelection() {
        this.noneSelected = true;
        this.oneSelected = false;
        this.manySelected = false;
        this.allSelected = false;
        this.selectedResources = {};
        this.selectedResourcesLength = 0;
    }

    toggleDetail(resource: Resource) {
        if (!resource.open)
            this.resources.forEach(resource => resource.open = false);
        resource.open = !resource.open;
    }

    selectAll(checkedEvent: any) {
        this.allSelected = checkedEvent.checked;
        if (this.allSelected) {
            this.resources.forEach(resource => {
                resource.selected = true;
                this.selectedResources[resource._links.self.href] = resource;
            });
        } else {
            this.resources.forEach(resource => {
                resource.selected = false;
                delete this.selectedResources[resource._links.self.href];
            });
        }
        this.updateSelectionState();
    }

    toggleRadio(resource: Resource) {
        this.selectedResources = {};
        this.resources.forEach(resource => resource.selected = false);
        resource.selected = true;
        this.selectedResources[resource._links.self.href] = resource.data;
        this.updateSelectionState();
    }

    toggleSelect(checkedEvent: any, resource: Resource) {
        resource.selected = checkedEvent.checked;
        resource.selected
            ? this.selectedResources[resource._links.self.href] = resource.data
            : delete this.selectedResources[resource._links.self.href];
        this.updateSelectionState();
    }

    updateSelectionState() {
        this.selectedResourcesLength = Object.keys(this.selectedResources).length;
        if (this.selectedResourcesLength === 0) { this.manySelected = false; this.oneSelected = false; this.noneSelected = true; }
        if (this.selectedResourcesLength === 1) { this.manySelected = false; this.oneSelected = true; this.noneSelected = false; }
        if (this.selectedResourcesLength > 1) { this.manySelected = true; this.oneSelected = false; this.noneSelected = false; }
        this.allSelected = (this.selectedResourcesLength === this.resources.length) ? true : false;
        // Emit selectedResources
        this.$selectedResources.next(this.selectedResources);
    }

    sort(property: ResourceProperty) {
        this.resourceProperties.forEach(p => {
            if (property.property === p.property) {
                if (!p.sorted) {
                    p.sorted = true;
                    p.sortDirection = RequestSortDirection.asc;
                } else {
                    p.sortDirection = p.sortDirection === RequestSortDirection.asc
                        ? RequestSortDirection.desc
                        : RequestSortDirection.asc;
                }
                this.updateRequestSort({ property: p.property, direction: p.sortDirection });
            } else {
                p.sorted = false;
                p.sortDirection = undefined;
            }
        });
    }

    updateResourceProperties(resourceProperties: ResourceProperty[]) {
        this.store.dispatch(new SetResourceProperties(this.resourceType, resourceProperties));
        const fields = resourceProperties.filter(p => p.checked).map(p => p.property);
        this.updateRequestFields(fields);
    }

    page(event: MatPaginatorEvent) {
        this.updateRequestPagination(event.pageIndex, event.pageSize);
    }
}

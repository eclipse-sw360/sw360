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
import { ResourceComponentModel } from '../../models/resources';
import { Type, OnInit, Component, Directive, ViewContainerRef, ViewChild, ComponentFactoryResolver, Input } from '@angular/core';
import { ComponentComponent } from '../../../../features/component/components/component/component.component';
import { ActivatedRoute, Router } from '@angular/router';
import { ProjectComponent } from '../../../../features/project/components/project/project.component';
import { ResourceDirective } from '../../directives/resource.directive';

interface Action {
    name: string;
    // action: Function; // can be anything
}

interface Resource {
    title: string;
    subtitle: string;

    // The resources actions.
    actions: Action[];

    // An array of detail rows (array).
    detailRows: Detail[][];
}

interface Detail {
    // Content
    title: string;
    subtitle: string;
    content: string;

    // Layout
    flex: boolean;
    width: string;
}

const DETAIL: Detail = {
    // data
    title: 'A card title',
    subtitle: 'Card subtitle',
    content: 'A cards content',

    // layout TODO: what makes sense here?
    flex: false, // fit?
    width: null
}

const LONG_STRING = `
asidaisdaosidb asid iashd iashdi sahdio ahsdio hasiodh aiosdh aioshd ioashdo iahsdio ahsiodh 
asidaisdaosidb asid iashd iashdi sahdio ahsdio hasiodh aiosdh aioshd ioashdo iahsdio ahsiodh 
asidaisdaosidb asid iashd iashdi sahdio ahsdio hasiodh aiosdh aioshd ioashdo iahsdio ahsiodh 
`;

const DETAIL_FLEX: Detail = { ...DETAIL, flex: true };
const DETAIL_WIDTH: Detail = { ...DETAIL, width: '400px', content: LONG_STRING };

const RESOURCE: Resource = {
    title: 'Resource Detail Title',
    subtitle: 'Resource Detail Title',
    actions: [{ name: 'Edit' }],
    detailRows: [
        // [{ ...DETAIL }, { ...DETAIL_WIDTH }],
        // [{ ...DETAIL }, { ...DETAIL_FLEX }, { ...DETAIL_WIDTH }],
        // [{ ...DETAIL_FLEX }, { ...DETAIL }],
        [{ ...DETAIL_WIDTH }, { ...DETAIL }, { ...DETAIL_FLEX }, { ...DETAIL_FLEX }],
        [{ ...DETAIL_FLEX }, { ...DETAIL_FLEX }],
        // [{ ...DETAIL_FLEX }, { ...DETAIL_WIDTH }],
        // [{ ...DETAIL }, { ...DETAIL }],
    ]
}

@Component({
    selector: 'sw-resource',
    templateUrl: './resource.component.html',
    styles: [`
        .host { }
        .title-subtitle { margin-bottom: 1em; }
        .card-group { margin-bottom: 1em; }
    `]
})
export class ResourceComponent implements OnInit {

    // resource: Resource = RESOURCE;
    
    // TODO: get data from rest and cast it into its type after the resource is retrieved
    // resourceData: any = {
    //     type: 'component'   
    // };

    @Input() selflink;

    resourceItem: ResourceComponentModel;
    @ViewChild(ResourceDirective) resourceCardContentDirective: ResourceDirective;

    constructor(
        private componentFactoryResolver: ComponentFactoryResolver,
        private router: Router
    ) { }

    ngOnInit(): void {

        // TODO: use route until rest with typed resource data is available
        const url = this.router.url;
        let type = '';
        if (url.includes('component')) {
            type = 'component';
        } else if (url.includes('project')) {
            type = 'project';
        }

        // this.fixLayout(this.resource);
        const anyData = { bla: "BLABLASSSS", xyz: "XYZ" };

        // TODO: Type from enum
        // TODO: pass component || project || license API data
        // TODO: pass JSON schema for layouting each resource type, but layout in its 
        switch (type) {
            case 'project':
                this.resourceItem = new ResourceComponentModel(ProjectComponent, anyData );
                break;
            case 'component':
                this.resourceItem = new ResourceComponentModel(ComponentComponent, anyData );
                break;
            default:
                break;
        }

        let componentFactory = this.componentFactoryResolver.resolveComponentFactory(this.resourceItem.component);
        this.resourceCardContentDirective.viewContainerRef.clear();
        let componentRef = this.resourceCardContentDirective.viewContainerRef.createComponent(componentFactory);

        componentRef.instance.data = this.resourceItem.data;
    }

    // Fix layout
    // Possible card behaviors: width, auto or flex
    fixLayout(resource: Resource): void {
        resource.detailRows.forEach((cards, i) => {
            // if a row exists of only of 1 card, make it flex
            if (cards.length === 1) {
                cards.forEach(card => card.flex = true);
            } else {
                // if no card has either flex or width, flex all cards
                if (!cards.some(card => card.flex) && cards.some(card => card.width === null)) {
                    cards.forEach(card => card.flex = true);
                }
            }
        });
    }
}

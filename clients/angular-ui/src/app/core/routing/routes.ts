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
import { PageNotFoundComponent } from '../view/page-not-found/page-not-found.component';
import { ResourceForms } from '../resources/components/resource-forms/resource-forms.component';
import { ResourceComponent } from '../resources/components/resource/resource.component';
import { Route } from "@angular/router";
import { ProjectComponent } from "../../features/project/components/project/project.component";
import { ComponentComponent } from '../../features/component/components/component/component.component';
import { ResourceTableComponent } from '../resources/components/resource-table/resource-table.component';
import { ResourceType } from '../resources/models/resources';
import { Type } from '@angular/core';
import { HomeComponent } from '../view/home/home.component';

export interface SW360RouteData {
    title: string;
    resourceType?: ResourceType;
    embeddingResourceType?: ResourceType;
}

export interface SW360Route extends Route {
    data?: SW360RouteData;
}

export const routes: SW360Route[] = [
    // Components
    /////////////
    {
        path: 'components/:id/releases/:id', component: ResourceComponent, data: {
            title: 'Release Detail',
            resourceType: ResourceType.release,
            embeddingResourceType: ResourceType.component,
        }
    },
    {
        path: 'components/:id/releases', component: ResourceTableComponent, data: {
            title: 'Releases',
            resourceType: ResourceType.release,
            embeddingResourceType: ResourceType.component,
        }
    },
    {
        path: 'components/:id/edit', component: ResourceForms, data: {
            title: 'Edit Component',
            resourceType: ResourceType.component,
        }
    },
    {
        path: 'components/create', component: ResourceForms, data: {
            title: 'Create Component',
            resourceType: ResourceType.component,
        }
    },
    {
        path: 'components/:id', component: ResourceComponent, data: {
            title: 'Component Detail',
            resourceType: ResourceType.component,
        }
    },
    {
        path: 'components',
        component: ResourceTableComponent, data: {
            title: 'Components',
            resourceType: ResourceType.component,
        }
    },

    // Home
    ///////
    {
        path: '', component: HomeComponent, data: {
            title: 'Welcome to SW360'
        }
    },

    // Projects
    ///////////
    {
        path: 'projects/:id/releases/:id', component: ResourceComponent, data: {
            title: 'Release Detail',
            resourceType: ResourceType.release,
            embeddingResourceType: ResourceType.component,
        }
    },
    {
        path: 'projects/:id/releases', component: ResourceTableComponent, data: {
            title: 'Releases',
            resourceType: ResourceType.release,
            embeddingResourceType: ResourceType.project,
        }
    },
    {
        path: 'projects/:id/edit', component: ResourceForms, data: {
            title: 'Edit Component',
            resourceType: ResourceType.project,
        }
    },
    {
        path: 'projects/create', component: ResourceForms, data: {
            title: 'Create Component',
            resourceType: ResourceType.project,
        }
    },
    {
        path: 'projects/:id', component: ResourceComponent, data: {
            title: 'Component Detail',
            resourceType: ResourceType.project,
        }
    },
    {
        path: 'projects', component: ResourceTableComponent, data: {
            title: 'Projects',
            resourceType: ResourceType.project,
        }
    },

    // Page not found
    /////////////////
    {
        path: '**', component: PageNotFoundComponent
    },
];

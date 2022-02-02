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
import { Injectable } from '@angular/core';
import { ResourceType } from '../resources/models/resources';
import { Router, ActivationEnd } from '@angular/router';
import { ResourceService } from '../resources/services/resource.service';

/**
 * The RouterService should be treated as a singleton, injected at bootstrap in app.component.
 * The RouterService carries the responsibility for the seamless navigation throughout any
 * client specific routes as well as routes that require resource API server interaction.
 * All type information get derived from an activated URL so that it's possible to bookmark and resolve 
 * any route.
 */
@Injectable()
export class RouterService {
    constructor(
        private router: Router,
        private resourceService: ResourceService
    ) {
        // Singleton subscribe to all router events.
        this.router.events.subscribe(routerEvent => {
            // Only handle the ActivationEnd event which means that
            // the routing processed ended and a navigation took place.
            if (routerEvent instanceof ActivationEnd)
                this.processActivationEnd(routerEvent);
        });
    }

    /**
     * Processes ActivationEnd and handles (global) resource state.
     * @param activationEnd The Angular Router ActivationEnd event.
     */
    processActivationEnd(activationEnd: ActivationEnd) {
        const urlSegments = activationEnd.snapshot.url;
        if(urlSegments.length === 0) return;

        // console.log("BANANENKUCHEN!")

        // Try to determine and handle the ResourceType for different route segment lengths.
        // If there is no resourceType, return.
        let resourceType;

        // TODO:
        const last = urlSegments[urlSegments.length - 1];
        // resourceType = this.resourceService.getResourceTypeFromUrlSegment(last);
        if(urlSegments.length === 1 && resourceType) {
            // console.log("It is a one segment resource list/table view!: " + resourceType);
            return;
        }
        
        // TODO:
        const nextToLast= urlSegments[urlSegments.length - 2];
        // resourceType = this.resourceService.getResourceTypeFromUrlSegment(nextToLast);
        if (urlSegments.length === 2 && resourceType) {
            // console.log("It is a two segment resource route: " + resourceType);
            // console.log("TODO: check if its create edit or detail?");
            urlSegments.forEach(u => console.log(u.path));
            return;
        }

        // console.log("still here")






















        



            
        // } else {
        //     if (urlSegmentsLength > 1) {
        //         let nextToLastIndex = urlSegments.length - 2;
        //         console.warn("Handle 2+ segment urls")
        //     } else {
        //         console.log("It is a non resource route of length 1");
        //         // It is a specific general route, that needs no further processing.
        //         return;
        //     }
        // }

        // // TODO: specific one segment routes do not need further resource API handling, simply return.
        // // TODO: avoid that condition with first handling resource (list/table) routes
        // if (lastIndex === 0) {
        //     // It is a one segment route.
        //     // Thefore it must be a general or self-contained resource (list/table) route.
        //     // TODO: check it here.
        // } else {
        //     // It is a 2+ segment route. There exists a nextToLast (penultimate) index.
        //     nextToLast = urlSegments.length - 2;
        //     // TODO: check the following:
        //     // - 
        // }

        // // Cases:
        // // 

        // // console.log(urlSegments.length);
        // urlSegments.forEach(urlSegment => {
        //     // console.log(urlSegment);
        // });

        // if (urlSegments.length === 0) {
        //     // console.log("root route, navigate to test1/test2/test3");
        //     this.router.navigate(['test1/test2/test3']);
        // }


        // console.warn("data");
        // console.log(activationEnd.snapshot.data);


        // What routes will exist?
        /*
        /components --> List
        /components/create --> Creation
        /components/123 --> Detail
        /components/123/edit --> Edit (Create, but with data, reuse component)
        /components/123/releases/ --> NOT ALONE, its embedded
        /components/123/releases/123 --> EXISTS!
        /components/123/releases/create --> Release creation
        /components/123/releases/123/edit --> Release edit (Create, but with data, reuse component)

        /projects
        /projects/create
        /projects/123
        /projects/123/edit
        /projects/123/releases --> embedded
        /projects/123/projects --> embedded

        // TODO: Questions
        Can licenses be created/edited by a SW360 user?
        Where does licenses come from? Static list in backend?
        /licenses
        /licenses/create --> TODO: ???
        /licenses/123
        /licenses

        */
    }
}


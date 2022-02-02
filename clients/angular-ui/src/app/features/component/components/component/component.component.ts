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
import { ActivatedRoute } from '@angular/router';
import { Component, Input, OnInit } from '@angular/core';
import { RouterService } from '../../../../core/routing/router.service';
import { ResourceService } from '../../../../core/resources/services/resource.service';
import { Subscription } from 'rxjs';
import { Router } from '@angular/router';
import { ResourceType } from '../../../../core/resources/models/resources';
import { HttpService } from '../../../../core/http/http.service';

@Component({
    selector: 'sw-component',
    templateUrl: './component.component.html',
    styles: [`
    
    `]
})
export class ComponentComponent implements OnInit {

    @Input() data: any;
    @Input() selflink: string;

    resourceType = ResourceType.component;

    constructor(private resourceService: ResourceService, private router: Router) { }

    ngOnInit(): void {
        this.selflink
            ? this.resourceService.getResource(this.resourceType, this.selflink).subscribe(data => this.data = data)
            : this.resourceService.getResourceByUrl(this.resourceType, this.router.url).subscribe(data => this.data = data);
    }
}

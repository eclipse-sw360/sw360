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
import { Component, Input } from "@angular/core";

@Component({
    selector: 'sw-project-details',
    template: `
    <mat-card>
        <mat-card-title>Details</mat-card-title>
        <mat-card-subtitle>Description</mat-card-subtitle>
        <mat-card-content>{{ element.description }}</mat-card-content>
        <mat-card-subtitle>Created by</mat-card-subtitle>
        <mat-card-content>{{ element.createdBy }}</mat-card-content>
        <mat-card-subtitle>Owner Group</mat-card-subtitle>
        <mat-card-content>{{ element.ownerGroup }}</mat-card-content>
        <mat-card-subtitle>Created On</mat-card-subtitle>
        <mat-card-content>{{ element.createdOn }}</mat-card-content>
        <mat-card-subtitle>Tag</mat-card-subtitle>
        <mat-card-content>{{ element.tag }}</mat-card-content>
        <mat-card-subtitle>Homepage</mat-card-subtitle>
        <mat-card-content>
            <a href="">{{ element.homepage }}</a>
        </mat-card-content>
    </mat-card>
    `
})
export class ProjectDetailsComponent {
    @Input() element: any;
}

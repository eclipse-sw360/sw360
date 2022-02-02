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
    selector: 'sw-project-state',
    template: `
    <mat-card class="host">
        <mat-card-title>State</mat-card-title>
        <mat-card-subtitle>Revision</mat-card-subtitle>
        <mat-card-content>{{ element.revision }}</mat-card-content>
        <mat-card-subtitle>Preevaluation Deadline</mat-card-subtitle>
        <mat-card-content>{{ element.preevaluationDeadline }}</mat-card-content>
    </mat-card>
    `,
    styles: [`
        .host {
            height: 100%;
        }
    `]
})
export class ProjectStateComponent {
    @Input() element: any;
}

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
import { Component } from '@angular/core';
import { ProjectTableService } from '../../services/project-table.service';
import { TitleService } from '../../../../core/view/services/title.service';
import { timer } from 'rxjs';

@Component({
    selector: 'sw-project',
    templateUrl: './project.component.html',
    styles: [`
        :host { display: block; width: 100%; }
        .t { margin-bottom: 1em; }
        mat-card {
            min-width: 300px;
        }
    `],
    providers: [ProjectTableService]
})
export class ProjectComponent {
    element: any;
    constructor(
        private projectService: ProjectTableService,
        private titleService: TitleService
    ) {
        titleService.registerSetAsync("ProjectComponent", "Loading...");
        timer(3000).subscribe(() => titleService.setAsync("ProjectComponent", "Ornia Crotheise"));
        this.element = this.projectService.getProjects(1)[0];
    }
}

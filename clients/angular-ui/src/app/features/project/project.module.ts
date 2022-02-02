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
import { NgModule } from "@angular/core";
import { ProjectComponent } from "./components/project/project.component";
import { RouterModule } from "@angular/router";
import { ProjectUsersComponent } from "./components/project-users/project-users.component";
import { ProjectDetailsComponent } from "./components/project-details/project-details.component";
import { ProjectStateComponent } from "./components/project-state/project-state.component";
import { FeatureModule } from "../../core/shared/feature.module";

@NgModule({
    declarations: [
        ProjectUsersComponent,
        ProjectDetailsComponent,
        ProjectStateComponent,
        ProjectComponent
    ],
    entryComponents: [
        ProjectComponent
    ],
    imports: [
        FeatureModule,
        RouterModule
    ],
    exports: [
        ProjectComponent
    ]
})
export class ProjectModule { }

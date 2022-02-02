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
import { ComponentComponent } from './components/component/component.component';
import { ComponentService } from './services/component.service';
import { NgModule } from "@angular/core";
import { FeatureModule } from '../../core/shared/feature.module';
import { TitleCasePipe } from '@angular/common';
import { ProjectComponent } from '../project/components/project/project.component';
import { ProjectModule } from '../project/project.module';
import { ResourcesModule } from '../../core/resources/resources.module';

@NgModule({
    imports: [
        FeatureModule,
        // ResourcesModule
    ],
    declarations: [
        ComponentComponent,
    ],
    providers: [
        ComponentService
    ],
    exports: [
        ComponentComponent
    ],
    entryComponents: [
        ComponentComponent
    ]
})
export class ComponentModule { }

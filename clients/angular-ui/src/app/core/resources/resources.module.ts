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
import { TableDialogComponent, TableDialog } from './components/resource-forms/table-dialog/table-dialog.component';
import { PropertyDialogComponent, PropertyDialog } from './components/resource-table/property-dialog/property-dialog.component';
import { ResourceForms } from './components/resource-forms/resource-forms.component';
import { ResourceComponent } from './components/resource/resource.component';
import { ResourceService } from './services/resource.service';
import { ResourceTableComponent } from './components/resource-table/resource-table.component';
import { NgModule } from '@angular/core';
import { FeatureModule } from '../shared/feature.module';
import { ResourceDirective } from './directives/resource.directive';
import { ProjectModule } from '../../features/project/project.module';
import { ProjectTableService } from '../../features/project/services/project-table.service';
import { ComponentModule } from '../../features/component/component.module';

@NgModule({
    imports: [
        FeatureModule,
        ProjectModule,
        ComponentModule
    ],
    declarations: [
        ResourceDirective,
        ResourceTableComponent,
        ResourceComponent,
        ResourceForms,
        PropertyDialogComponent,
        PropertyDialog,
        TableDialogComponent,
        TableDialog
    ],
    entryComponents: [
        PropertyDialog,
        TableDialog
    ],
    exports: [ResourceTableComponent],
    providers: [ResourceService, ProjectTableService]
})
export class ResourcesModule { }

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
import { CommonModule, TitleCasePipe } from "@angular/common";
import { MaterialModule } from "./material.module";
import { ReactiveFormsModule, FormsModule } from "@angular/forms";
import { FlexLayoutModule } from "@angular/flex-layout";
import { PipesModule } from "./pipes/pipes.module";

const SHARED_MODULES = [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    FlexLayoutModule,
    MaterialModule,
    PipesModule
];

@NgModule({
    imports: SHARED_MODULES,
    exports: SHARED_MODULES,
    providers: [TitleCasePipe]
})
export class FeatureModule { }

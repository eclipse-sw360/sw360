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
import { JsonSchemaFormsService } from './services/json-schema-forms.service';
import { JsonSchemaService } from './services/json-schema.service';
import { NgModule } from '@angular/core';
import { EffectsModule } from '../../../../node_modules/@ngrx/effects';

@NgModule({
    providers: [
        JsonSchemaService,
        JsonSchemaFormsService
    ]
})
export class JsonSchemaModule { }

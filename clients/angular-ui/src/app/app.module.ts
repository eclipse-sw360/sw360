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
import { HttpService } from './core/http/http.service';
import { JsonSchemaModule } from './core/json-schema/json-schema.module';
import { RouterService } from './core/routing/router.service';
import { appReducers } from './app.state';
import { AuthService } from './core/auth/auth.service';
import { TokenInterceptorService } from './core/auth/token-interceptor.service';
// ng
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { BrowserModule } from '@angular/platform-browser';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

// app
import { AppComponent } from './app.component';

import { TitleService } from './core/view/services/title.service';
import { routes } from './core/routing/routes';

import { FeatureModule } from './core/shared/feature.module';
import { ViewModule } from './core/view/view.module';
import { ResourcesModule } from './core/resources/resources.module';
import { ComponentModule } from './features/component/component.module';
import { ProjectModule } from './features/project/project.module';
import { StoreModule } from '@ngrx/store';
import { EffectsModule } from '@ngrx/effects';
import { StoreDevtoolsModule } from '@ngrx/store-devtools';

@NgModule({
  declarations: [
    AppComponent,
  ],
  imports: [
    HttpClientModule,
    BrowserModule,
    BrowserAnimationsModule,

    // Init store
    StoreModule.forRoot(appReducers),
    EffectsModule.forRoot([]),
    StoreDevtoolsModule.instrument({
      maxAge: 25, // Retains last 25 states
    }),

    JsonSchemaModule,

    RouterModule.forRoot(routes, { onSameUrlNavigation: 'reload' }),
    ViewModule,
    ResourcesModule,
    ProjectModule,
    ComponentModule,
  ],
  providers: [
    HttpService,
    TitleService, // TODO: put into state
    RouterService,
    AuthService,
    {
      provide: HTTP_INTERCEPTORS,
      useClass: TokenInterceptorService,
      multi: true
    }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }

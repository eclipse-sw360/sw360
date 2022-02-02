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
import { AuthService } from './core/auth/auth.service';
import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RouterService } from './core/routing/router.service';
import { JsonSchemaService } from './core/json-schema/services/json-schema.service';

@Component({
  selector: 'sw-root',
  template: `<sw-layout></sw-layout>`,
  styles: [`:host { display: block; height: 100%; }`]
})
export class AppComponent implements OnInit {
  constructor(
    private authService: AuthService,
    private jsonSchemaService: JsonSchemaService,
    private routerService: RouterService
  ) { }

  /**
   * The app component's ngOnInit is used to bootstrap all services that
   * are provided as singletons throughout the app.
   * TODO:
   * Important data initializations could block the layout until all data is there.
   */
  ngOnInit() {
    // 1. Auth data. If there is no user, let him log in.
    // 2. Only when auth state is available, get data via the API:
    // 3. JSON schemas (the app will know about all possible resources)
    // Hint: It is a little more expensive, but saves a lot of requests later regarding user interactions
  }
}

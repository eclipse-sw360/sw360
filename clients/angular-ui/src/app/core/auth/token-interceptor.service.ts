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
import { Injectable } from '@angular/core';
import { HttpRequest, HttpHandler, HttpEvent, HttpInterceptor } from '@angular/common/http';
import { Observable } from 'rxjs/Observable';
import { Store } from '@ngrx/store';
import { AppState } from '../../app.state';
import { selectToken } from './auth.state';
import { empty } from 'rxjs/observable/empty';
import { interval, timer } from 'rxjs';
import { take, map, flatMap, switchMap } from 'rxjs/operators';
import { debounceTime } from 'rxjs/internal/operators/debounceTime';

@Injectable()
export class TokenInterceptorService implements HttpInterceptor {
    private token: string;
    constructor(private store: Store<AppState>) {
        store.select(selectToken).subscribe(token => this.token = token);
    }

    // TODO: error handling
    // TODO: docs
    intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        if (req.url.indexOf('authorization') === -1)
            req = req.clone({ setHeaders: { Authorization: this.token } });
        return next.handle(req);
    }
}

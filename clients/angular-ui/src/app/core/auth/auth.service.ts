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
import { Observable } from 'rxjs';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { timer } from 'rxjs/observable/timer';
import { AppState } from '../../app.state';
import { Store } from '@ngrx/store';
import { SetAuth } from './auth.state';
import { map } from 'rxjs/operators';

@Injectable()
export class AuthService {

    // SW360 config admin: sw360setup : sw360fossy
    constructor(private http: HttpClient, private store: Store<AppState>) {
        // TODO: auto login
        this.login('admin@sw360.org', '12345');
    }

    login(username: string, password: string) {
        // TODO: BaseUrl, HttpParams
        const url = `http://localhost:8090/authorization/oauth/token?grant_type=password&username=${username}&password=${password}`;
        const credentials = btoa('trusted-sw360-client:sw360-secret');
        const headers = new HttpHeaders().set('Authorization', 'Basic ' + credentials)
        this.http.post(url, {}, { headers, withCredentials: true }).subscribe((res: any) => {
            const token = res.access_token;
            const expiresIn = res.expires_in;
            // TODO: Security?
            this.store.dispatch(new SetAuth(username, `Bearer ${token}`));
            // Refresh the token 2 minutes before it expires
            timer((expiresIn - 120) * 1000).subscribe(() => this.login(username, password));
        },
            err => console.warn(err)
        );
    }

    logout() {
        this.store.dispatch(new SetAuth('',''));
    }
}

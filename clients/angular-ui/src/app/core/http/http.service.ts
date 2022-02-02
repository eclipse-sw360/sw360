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
import { HttpClient, HttpParams, HttpErrorResponse } from '@angular/common/http';
import { Observable, timer, of } from 'rxjs';
import { Injectable } from '@angular/core';
import { resourceTypeUriMap, ResourceType, JSONSchema } from '../resources/models/resources';
import { SetMessage, MessageType } from '../view/message/message.state';
import { AppState } from '../../app.state';
import { Store } from '@ngrx/store';
import { map, catchError, tap } from 'rxjs/operators';

/**
 * The global http service.
 */
@Injectable()
export class HttpService {

    // TODO:
    // patch  components/123 -> ignores unknown, ignores empty body
    // provide this (static) through environment at build time!
    baseUrl = 'http://localhost:8090/resource/api';

    constructor(
        private http: HttpClient,
        private store: Store<AppState>
    ) { }

    getResource(selflink: string, params?: HttpParams): Observable<any> {
        return this.http.get<any>(selflink, { params: params })
            .pipe(catchError((error: Response | HttpErrorResponse | any) => this.handleError(error)));
    }

    // TODO: remove when user request works the same as other resources (API inconsistency)
    getUser(selflink: string, params?: HttpParams): Observable<any> {
        return this.http.get<any>(`${this.baseUrl}/users/byid/${this.getIdFromSelflink(selflink)}`, { params: params })
            .pipe(catchError((error: Response | HttpErrorResponse | any) => this.handleError(error)));
    }

    patch(selflink: string, body: any): Observable<any> {
        return this.http.patch<any>(selflink, body)
            .pipe(catchError((error: Response | HttpErrorResponse | any) => this.handleError(error)));
    }

    get(path: string, params?: HttpParams): Observable<any> {
        return this.http.get<any>(`${this.baseUrl}/${path}`, { params: params })
            .pipe(catchError((error: Response | HttpErrorResponse | any) => this.handleError(error)));
    }

    post(path: string, body: any): Observable<any> {
        return this.http.post<any>(`${this.baseUrl}/${path}`, body)
            .pipe(catchError((error: Response | HttpErrorResponse | any) => this.handleError(error)));
    }

    delete(path: string): Observable<any> {
        return this.http.delete<any>(`${this.baseUrl}/${path}`)
            .pipe(catchError((error: Response | HttpErrorResponse | any) => this.handleError(error)));
    }

    getSchema(resourceType: ResourceType | string): Observable<JSONSchema> {
        // TODO: Get from API as soon as implemented (missing in API)
        return this.http.get<JSONSchema>(`/assets/json-schema/${resourceType}.json`)
            .pipe(catchError((error: Response | HttpErrorResponse | any) => this.handleError(error)));
    }

    handleError(errorResponse: Response | HttpErrorResponse | any): Observable<any> {
        let statusText = '';
        if (errorResponse.statusText) statusText = errorResponse.statusText;
        else if (errorResponse.error && errorResponse.error.error) statusText = errorResponse.error.error;

        let status: number;
        if (errorResponse.satus) status = errorResponse.satus;
        else if (errorResponse.error && errorResponse.error.status) status = errorResponse.error.status;

        let errorMessage = '';
        if (errorResponse.message) errorMessage = errorResponse.message;
        if (errorResponse.error && errorResponse.error.message) errorMessage += `\n${errorResponse.error.message}`;

        let message = 'A http error occurred:\n';
        if (statusText) message += `Status text: ${statusText}\n`
        if (status) message += `Status code: ${status}\n`;
        if (errorMessage) message += `Error message(s):\n${errorMessage}`;

        console.error(message);
        this.store.dispatch(new SetMessage(message, MessageType.error));
        return of();
    }

    getSelflinkFromId(resourceType: ResourceType, id: string): string {
        return `${this.baseUrl}/${resourceTypeUriMap[resourceType]}/${id}`;
    }

    getIdFromSelflink(selflink: string): string {
        const split = selflink.split('/');
        return split[split.length - 1];
    }
}

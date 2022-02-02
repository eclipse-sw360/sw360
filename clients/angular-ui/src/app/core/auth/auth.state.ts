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
import { Action } from '@ngrx/store';
import { AppState } from '../../app.state';

export interface AuthState {
    isAuth: boolean;
    token: string;
    user: string;
}

const initialAuthState: AuthState = {
    isAuth: false,
    token: '',
    user: ''
};

// export const SET_TOKEN = '[Auth] Set token';
// export const SET_USER  = '[Auth] Set user';
export const SET_AUTH  = '[Auth] Set auth';

export class SetAuth implements Action {
    readonly type = SET_AUTH;
    constructor(public user: string, public token: string) { }
}

// export class SetToken implements Action {
//     readonly type = SET_TOKEN;
//     constructor(public token: string) { }
// }

// export class SetUser implements Action {
//     readonly type = SET_USER;
//     constructor(public user: string) { }
// }

export type AuthActions = 
    // SetToken |
    // SetUser |
    SetAuth
    ;

export function authReducer(state = initialAuthState, action: AuthActions) {
    switch (action.type) {
        // case SET_TOKEN: {
        //     return { ...state, token: action.token };
        // }
        // case SET_USER: {
        //     return { ...state, user: action.user };
        // }
        case SET_AUTH: {
            const isAuth = (action.user && action.token) ? true : false;
            return { ...state, user: action.user, token: action.token, isAuth: isAuth }
        }
        default:
            return state;
    }
}

export const selectIsAuth = (state: AppState) => state.auth.isAuth;
export const selectToken = (state: AppState) => state.auth.token;
export const selectUser = (state: AppState) => state.auth.user;

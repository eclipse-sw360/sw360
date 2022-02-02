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
import { ActionReducerMap } from '@ngrx/store';
import { MessageState, messageReducer } from './core/view/message/message.state';
import { AuthState, authReducer } from './core/auth/auth.state';
import { ResourceState, resourceReducer } from './core/resources/resource.state';

export interface AppState {
    auth: AuthState;
    message: MessageState;
    resource: ResourceState;
}

export const appReducers: ActionReducerMap<AppState> = {
    auth: authReducer,
    message: messageReducer,
    resource: resourceReducer
}

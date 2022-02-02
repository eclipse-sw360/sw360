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
import { AppState } from '../../../app.state';

export enum MessageType {
    plain = 'plain',
    success = 'success',
    warning = 'warning',
    error = 'error'
}

export interface MessageState {
    message: string;
    messageType: MessageType;
}

const initialState: MessageState = {
    message: '',
    messageType: MessageType.plain
};

export const SET_MESSAGE = '[Message] Set message';

export class SetMessage implements Action {
    readonly type = SET_MESSAGE;
    constructor(public message: string, public messageType: MessageType) { }
}

export type MessageActions =
    SetMessage
    ;

export function messageReducer(state = initialState, action: MessageActions) {
    switch (action.type) {
        case SET_MESSAGE: {
            return {
                ...state,
                message: action.message,
                messageType: action.messageType
            }
        }
        default:
            return state;
    }
}

export const selectMessage = (state: AppState) => state.message;

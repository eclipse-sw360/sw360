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
import { Component } from "@angular/core";
import { AppState } from "../../../app.state";
import { Store } from "@ngrx/store";
import { selectMessage, MessageState, MessageType } from "./message.state";
import { tap, switchMap } from 'rxjs/operators'
import { timer, interval, Subscription } from 'rxjs';

@Component({
    selector: 'sw-message',
    template: `
    <div class="host" *ngIf="message.message && time > 0">
        ({{ time }}) {{ message.message }}
    </div>
    `,
    styles: [`
    .host {
        border: 2px solid red;
        background: rgba(255,0,0,0.2);
        padding: 1em;
    }
    `]
})
export class MessageComponent {
    message: MessageState = {
        message: '',
        messageType: MessageType.plain
    };
    time: number;
    constructor(private store: Store<AppState>) {
        interval(1000).subscribe(() => {
            if (this.time > 0) this.time--;
        });
        store.select(selectMessage).subscribe(message => {
            this.message = message;
            this.time = 10;
        });
    }
}

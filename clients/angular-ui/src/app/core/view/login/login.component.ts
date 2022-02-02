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
import { AuthService } from '../../auth/auth.service';
import { FormControl, FormGroup, Validators, FormBuilder } from '@angular/forms';
import { Component } from '@angular/core';

@Component({
    selector: 'sw-login',
    template: `
        <div>You are not logged in yet!</div>
        <div class="todo">Please make sure SW360 is running and available through your network.</div>
        <form [formGroup]="group" fxLayout="column">
            <mat-form-field>
                <input matInput formControlName="user" placeholder="User" required>
                <mat-error>abc</mat-error>
            </mat-form-field>
            <mat-form-field>
                <input matInput formControlName="password" placeholder="Password" required type="password">
                <mat-error>abc</mat-error>
            </mat-form-field>
        </form>
        <button mat-raised-button (click)="login()" [disabled]="!group.valid">Login</button>
    `,
    styles: [`
        .todo {
            margin-bottom: 1em;
        }
        mat-form-field {
            max-width: 300px;
        }
    `]
})
export class LoginComponent {
    group: FormGroup;
    
    constructor(private fb: FormBuilder, private authService: AuthService) {
        this.group = fb.group({ user: ['', Validators.required], password: ['', Validators.required] });
    }

    login() {
        this.authService.login(this.group.value.user, this.group.value.password);
    }
}

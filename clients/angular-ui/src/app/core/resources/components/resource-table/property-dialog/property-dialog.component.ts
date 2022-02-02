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
import { Component, Inject, Input, Output, EventEmitter } from '@angular/core';
import { MatDialog, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material';
import { ResourceProperty } from '../../../models/resources';

@Component({
    selector: 'sw-property-dialog',
    template: `
        <button mat-button (click)="openDialog()">
            <mat-icon>visibility</mat-icon>
            <span class="fields-button">Columns</span>
        </button>
    `,
    styles: [`.fields-button { margin-left: 0.7em; }`]
})
export class PropertyDialogComponent {
    @Input() properties: ResourceProperty[] = [];
    @Output() checkedProperties: EventEmitter<ResourceProperty[]> = new EventEmitter();
    constructor(public dialog: MatDialog) { }
    openDialog(): void {
        const dialogRef = this.dialog.open(PropertyDialog, { width: '500px', data: this.properties });
        dialogRef.afterClosed().subscribe(result => {
            if (!result) return;
            this.checkedProperties.next(result)
        });
    }
}

@Component({
    selector: 'property-dialog',
    template: `
    <h1 mat-dialog-title>Table Properties</h1>
    <div mat-dialog-content>
        <p>Check the properties that should be displayed by the table:</p>
        <div fxLayout="row wrap">
            <div class="check-box" *ngFor="let p of properties">
                <mat-checkbox [(ngModel)]="p.checked" [checked]="p.checked" color="primary">{{ p.title }}</mat-checkbox>
            </div>
        </div>
    </div>
    <div mat-dialog-actions>
        <button mat-button (click)="onNoClick()">Cancel</button>
        <button mat-button [mat-dialog-close]="properties" cdkFocusInitial>Apply</button>
    </div>
    `,
    styles: [`.check-box { width: 200px; margin-bottom: 0.1em; }`]
})
export class PropertyDialog {
    properties: ResourceProperty[] = [];
    constructor(
        public dialogRef: MatDialogRef<PropertyDialog>,
        @Inject(MAT_DIALOG_DATA) public data: ResourceProperty[]
    ) {
        this.properties = data.map(p => { return { ...p } });
    }
    onNoClick(): void {
        this.dialogRef.close();
    }
}

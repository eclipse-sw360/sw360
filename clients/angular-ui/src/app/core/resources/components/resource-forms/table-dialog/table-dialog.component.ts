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
import { ResourceProperty, ResourceType } from '../../../models/resources';
import { SelectedResourcesMap, SelectionType } from '../../resource-table/resource-table.component';

interface TableDialogConfiguration {
    title: string;
    description: string;
    resourceType: ResourceType;
    selectionType: SelectionType;
    showActions: boolean;
}

@Component({
    selector: 'sw-table-dialog',
    template: `
        <button mat-button (click)="openDialog()">
            <mat-icon>list</mat-icon>
            <span class="fields-button">{{ title }}</span>
        </button>
    `,
    styles: [`.fields-button { margin-left: 0.7em; }`]
})
export class TableDialogComponent {
    @Input() title: string;
    @Input() description: string;
    @Input() resourceType: ResourceType;
    @Input() selectionType: SelectionType;
    @Input() showActions: boolean = false;
    @Input() width: string = "70%";

    @Output() $selectedResources = new EventEmitter<SelectedResourcesMap>();

    constructor(public dialog: MatDialog) { }
    openDialog(): void {
        const dialogRef = this.dialog.open(TableDialog, {
            width: this.width,
            data: {
                title: this.title,
                description: this.description,
                resourceType: this.resourceType,
                selectionType: this.selectionType,
                showActions: this.showActions
            }
        });
        dialogRef.afterClosed().subscribe(result => this.$selectedResources.next(result));
    }
}

@Component({
    selector: 'table-dialog',
    template: `
        <h1 mat-dialog-title>{{ data.title }}</h1>
        <div mat-dialog-content>
            <p>{{ data.description }}</p>
            <sw-resource-table
                ($selectedResources)="persistSelectedResources($event)"
                [resourceType]="data.resourceType"
                [selectionType]="data.selectionType"
                [showActions]="data.showActions">
            </sw-resource-table>
        </div>
        <div mat-dialog-actions>
            <button mat-button (click)="onNoClick()">Cancel</button>
            <button mat-button [mat-dialog-close]="selectedResources" cdkFocusInitial>Apply</button>
        </div>
    `,
    styles: [`.check-box { width: 200px; margin-bottom: 0.1em; }`]
})
export class TableDialog {
    selectedResources: SelectedResourcesMap;
    constructor(
        public dialogRef: MatDialogRef<TableDialog>,
        @Inject(MAT_DIALOG_DATA) public data: TableDialogConfiguration[]
    ) {
        this.selectedResources = {};
    }
    onNoClick(): void {
        this.dialogRef.close(this.selectedResources);
    }
    persistSelectedResources(selectedResources: SelectedResourcesMap) {
        this.selectedResources = selectedResources;
    }
}

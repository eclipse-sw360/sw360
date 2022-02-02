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
import { HttpService } from '../../../http/http.service';
import { SelectedResourcesMap } from '../resource-table/resource-table.component';
import { JsonSchemaFormsService } from '../../../json-schema/services/json-schema-forms.service';
import { Component, OnInit, AfterViewInit, ChangeDetectorRef, OnDestroy, OnChanges } from "@angular/core";
import { FormBuilder, FormControl, Validators, FormGroup, ValidatorFn, AbstractControl } from "@angular/forms";
import { Subscription } from '../../../../../../node_modules/rxjs';
import { ActivatedRoute } from '../../../../../../node_modules/@angular/router';
import { map, take } from 'rxjs/operators';
import { ResourceService } from '../../services/resource.service';
import { SW360RouteData } from '../../../routing/routes';
import { AppState } from '../../../../app.state';
import { Store } from '@ngrx/store';
import { selectFormsResourceProperties } from '../../resource.state';
import { FormType, ResourceProperty, ResourceType } from '../../models/resources';
import { COMMA, ENTER } from '@angular/cdk/keycodes';
import { MatChipInputEvent } from '@angular/material';
import { Observable } from 'rxjs/Observable';

interface GroupNameResourcePropertiesMap {
    [groupName: string]: ResourceProperty[];
}

interface Group {
    groupName: string;
    resourceProperties: ResourceProperty[];
    formGroup: FormGroup;
}

interface CollectionPropertyValues {
    [property: string]: any[];
}

interface CustomValidation {
    valid: boolean;
    formError: string;
}

@Component({
    selector: 'sw-resource-forms',
    templateUrl: './resource-forms.component.html',
    styles: [`
        .formControls {
            max-width: 80em;
        }
        mat-form-field, .mat-form-field {
            min-width: 30em;
            min-height: 100px;
            margin-bottom: 1.5em;
        }
        .full-width {
            width: 63em;
        }
    `]
})
export class ResourceForms implements OnInit, OnDestroy, AfterViewInit {

    resourceType: ResourceType;
    activatedRouteSub: Subscription;
    groups: Group[];
    collectionPropertyValues: CollectionPropertyValues = {};
    mapProperties = {}; // TODO: types

    readonly separatorKeysCodes: number[] = [ENTER, COMMA];

    mapValueExtension = '-mapValue';
    valid = true;

    edit = false;
    selflink: string;

    constructor(
        private changeDetectorRef: ChangeDetectorRef,
        private activatedRoute: ActivatedRoute,
        private formBuilder: FormBuilder,
        private store: Store<AppState>,
        private resourceService: ResourceService
    ) { }

    ngOnInit(): void {

        this.activatedRouteSub = this.activatedRoute.data.subscribe((data: SW360RouteData) => {

            this.resourceType = data.resourceType;
            let $existingResource: Observable<any>;

            // The route for editing a resource contains the edit (true) query param.
            this.activatedRoute.queryParams.pipe(take(1)).subscribe(queryParams => {
                if (queryParams && queryParams.edit) {
                    this.edit = true;
                    this.selflink = queryParams.selflink;
                    $existingResource = this.resourceService.getResource(data.resourceType, queryParams.selflink);
                }
            });

            // take(1) makes it a cold observable.
            const resourceProperties$ = this.store.select(selectFormsResourceProperties(data.resourceType)).pipe(take(1));
            resourceProperties$.subscribe(resourceProperties => {
                // Initialize groups
                this.groups = [];
                const groups: GroupNameResourcePropertiesMap = {};
                resourceProperties.forEach(resourceProperty => groups[resourceProperty.group] = []);
                resourceProperties.forEach(resourceProperty => groups[resourceProperty.group].push(resourceProperty));
                Object.entries(groups).forEach(([groupName, resourceProperties]: [string, ResourceProperty[]]) =>
                    this.groups.push({ groupName: groupName, resourceProperties: resourceProperties, formGroup: this.formBuilder.group({}) }));
                // Initialize FormControls for each FormGroup
                this.groups.forEach(group => {
                    group.resourceProperties.forEach(resourceProperty => {
                        // If the property is a collection, add a backup collection map for it.
                        // All values that getting added for that property will be collected there, this will also be
                        // used for the forms validation.
                        if (resourceProperty.formType === FormType.array ||
                            resourceProperty.formType === FormType.map ||
                            resourceProperty.formType === FormType.selectMany ||
                            (resourceProperty.formType === FormType.selectOne && !resourceProperty.enumMap)) {
                            this.collectionPropertyValues[resourceProperty.property] = [];
                        }

                        const validators: ValidatorFn[] = [];
                        if (resourceProperty.required) validators.push(Validators.required);
                        if (resourceProperty.pattern) validators.push(Validators.pattern(resourceProperty.pattern));

                        // TODO: Two cases: enumMap:string and string:string maps.
                        let mapValueProperty = undefined;
                        if (resourceProperty.formType === FormType.map) {
                            mapValueProperty = resourceProperty.property + this.mapValueExtension;
                        }

                        group.formGroup.addControl(resourceProperty.property, new FormControl(null, validators));
                    });
                });

                // Observe and validate all form group changes.
                this.groups.forEach(group => group.formGroup.valueChanges.subscribe(() => this.validate()));

                // If the resource gets edited, initialize it with existing data.
                if (this.edit) {
                    $existingResource.subscribe(existingResource => {
                        console.log(existingResource);
                        Object.entries(existingResource).forEach(([k, v]) => {
                            this.groups.forEach(group => {
                                const control = group.formGroup.get(k);
                                // Find the matching resourceProperty
                                const resourceProperty = group.resourceProperties.find(resourceProperty => resourceProperty.property === k);
                                if (resourceProperty) {
                                    // The direct assignment is okay since API results only contain properties that have values.
                                    if (resourceProperty.formType === FormType.array) {
                                        this.collectionPropertyValues[k] = <any[]> v;
                                        this.validateCollection(resourceProperty);
                                    } else {
                                        if (control)
                                            control.setValue(v);
                                    }
                                    // TODO: Initialize other form types (maps, embedded data, ...) from request result (existingResource)
                                }
                            });
                        });
                    });
                    this.validate();
                }
            });
        });
    }

    doSomething(resourceProperty: ResourceProperty, selectedResources: SelectedResourcesMap) {
        Object.keys(selectedResources).forEach(selflink => {
            this.resourceService.getResource(resourceProperty.resourceType, selflink)
                .subscribe(resource => this.collectionPropertyValues[resourceProperty.property].push(resource));
        });
    }

    removeFromMap(resourcePropertyKey, mapKey) {
        // Remove from the map via a new reference (ngFor object reference update behavior)
        const clone = { ...this.mapProperties[resourcePropertyKey] };
        delete clone[mapKey];
        this.mapProperties[resourcePropertyKey] = clone;
    }

    addToMap(resourcePropertyKey, resourcePropertyValue) {
        let controlKey: AbstractControl;
        let controlValue: AbstractControl;
        this.groups.forEach(group => {
            const ck = group.formGroup.get(resourcePropertyKey);
            const cv = group.formGroup.get(resourcePropertyValue);
            if (ck) controlKey = ck;
            if (cv) controlValue = cv;
        });
        if (controlKey.value && controlValue.value) {
            // If it doesn't exist yet
            if (!this.mapProperties[resourcePropertyKey])
                this.mapProperties[resourcePropertyKey] = {};
            // Add to the map via a new reference (ngFor object reference update behavior)
            const clone = { ...this.mapProperties[resourcePropertyKey] };
            clone[controlKey.value] = controlValue.value;
            this.mapProperties[resourcePropertyKey] = clone;
        }
    }

    addToArray(event: MatChipInputEvent, resourceProperty: ResourceProperty): void {
        const input = event.input;
        let value = event.value;
        if (value) value = value.trim();
        if (!this.collectionPropertyValues[resourceProperty.property].includes(value))
            this.collectionPropertyValues[resourceProperty.property].push(value);
        if (input) input.value = '';
        this.validateCollection(resourceProperty);
    }

    validateCollection(resourceProperty: ResourceProperty) {
        let control: AbstractControl;
        this.groups.forEach(group => {
            const c = group.formGroup.get(resourceProperty.property);
            if (c) control = c;
        });
        if (resourceProperty.required && this.collectionPropertyValues[resourceProperty.property]) {
            (this.collectionPropertyValues[resourceProperty.property].length > 0)
                ? control.setErrors(null)
                : control.setErrors({ incorrect: true });
        }
        this.validate();
    }

    remove(value: any, resourceProperty: ResourceProperty): void {
        const index = this.collectionPropertyValues[resourceProperty.property].indexOf(value);
        if (index >= 0)
            this.collectionPropertyValues[resourceProperty.property].splice(index, 1);
        this.validateCollection(resourceProperty);
    }

    ngOnDestroy(): void {
        if (this.activatedRouteSub) this.activatedRouteSub.unsubscribe();
    }

    validate() {
        this.valid = true;
        this.groups.forEach(group => {
            Object.keys(group.formGroup.controls).forEach(field => {
                const control = group.formGroup.get(field);
                control.markAsTouched({ onlySelf: true });
            });
            if (!group.formGroup.valid)
                this.valid = false;
        });
    }

    submit() {
        this.validate();
        if (this.valid) {
            const requestObject = {};
            // Regular form values
            this.groups.forEach(group => Object.entries(group.formGroup.value).forEach(([k, v]) => {
                if (v) requestObject[k] = v;
            }));
            // Collection (array) values
            Object.entries(this.collectionPropertyValues).forEach(([k, v]) => {
                if (v && v.length > 0) requestObject[k] = v;
            });
            // TODO: The enumMap:string and string:string values are still missing in request object.
            // TODO: The embedded resources like users are also missing. The API is not yet clear about how
            // to relate embededd SW360 resources to each other.
            console.warn("TODO INCOMPLETE: ", requestObject)

            this.edit
                ? this.resourceService.patchResource(this.resourceType, this.selflink, requestObject)
                : this.resourceService.postResource(this.resourceType, requestObject);
        }
    }

    // Manual change detection after view init (mat-hint):
    // https://blog.angularindepth.com/everything-you-need-to-know-about-the-expressionchangedafterithasbeencheckederror-error-e3fd9ce7dbb4
    ngAfterViewInit(): void {
        this.changeDetectorRef.detectChanges();
    }

}

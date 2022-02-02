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
import { Injectable } from "@angular/core";
import { FormGroup, FormBuilder, ValidatorFn, Validators, FormControl } from "@angular/forms";
import { JSONSchema7 } from 'json-schema';
import { ResourceType, FormType, JSONSchema } from "../../resources/models/resources";
import { Store } from "@ngrx/store";
import { AppState } from "../../../app.state";
import { selectSchema } from "../../resources/resource.state";
import { take } from "rxjs/operators";

@Injectable()
export class JsonSchemaFormsService {
    constructor(private formBuilder: FormBuilder, private store: Store<AppState>) { }

    // TODO: return some values.
    parseSchema(resourceType: ResourceType) {
        this.store.select(selectSchema(resourceType)).pipe(take(1)).subscribe(schema => 
            Object.entries(schema.properties)
                .forEach(([key, schema]: [string, JSONSchema]) => this.parseProperty(key, schema)));
    };

    parseProperty(key: string, schema: JSONSchema) {
        // Discard client (!visible || !editable || formType.embedded) properties since they are no direct form elements.
        // formType.embedded properties are create and editable due their detail embedded tables after parent resource creation.
        if (!schema.client.visible || !schema.client.editable || schema.client.formType === FormType.embedded) return;
        
        switch (schema.client.formType) {
            case FormType.input: break;
            case FormType.textarea: break;
            case FormType.date: break;
            case FormType.map: break;
            case FormType.array: break;
            case FormType.selectOne: break;
            case FormType.selectMany: break;
            default: break;
        }
    }


    parseFormTypeInputTextarea(key: string, schema: JSONSchema) {

    }

    parseObjectProperty(key: string, schema: JSONSchema) {

    }

    parseArrayProperty(key: string, schema: JSONSchema) {

    }
























































    // appendSchema(o: any[]) {
    //     this.http.get('/assets/schemas/component-schema.json').subscribe((schema: any) => {
    //         const schemaProperties = schema.properties;
    //         // console.log(schemaProperties);
    //         // o.forEach(x => console.log(x))
    //         Object.entries(o).forEach(([k, v]: [string, any]) => {
    //             // console.log(schemaProperties[k]);
    //         });
    //     });
    // }

    // private parseSchema(schema: any) {
    //     // console.log(schema);
    // }

    // // TODO: We need a solution for adding other resources! Embedded resoures like release or attachment
    // getFormGroups(clientPropertySchemaMap: any): FormGroupMap {
    //     const groups: any = {};
    //     Object.values(clientPropertySchemaMap).map((c: any) => c.group).forEach(group => groups[group] = []);
    //     Object.entries(clientPropertySchemaMap).forEach(([p, clientPropertySchema]: [string, any]) =>
    //     groups[clientPropertySchema.group].push(clientPropertySchema));

    //     const formGroupMap: FormGroupMap = {};
    //     Object.entries(groups).forEach(([groupName, clientPropertySchemes]: [string, any[]]) => {
    //         const group = this.formBuilder.group({});
    //         clientPropertySchemes.forEach(s => {
    //             const validators: ValidatorFn[] = [];
    //             if (s.required) validators.push(Validators.required);
    //             if (s.pattern) validators.push(Validators.pattern(s.pattern));
    //             group.addControl(s.property, new FormControl('', validators));
    //         });
    //         formGroupMap[groupName] = group;
    //     });
    //     return formGroupMap;
    // }



























    // getFormFieldsFromSchema(schema: any): FormField[] {
    //     // TODO: Is schema valid?
    //     if (schema.properties)
    //         return this.properties(schema.properties, schema);
    // }

    // // Parse schema properties into an array of FormFields,
    // // which are used by a component to generate UI,
    // // the FormField interface should be as flat as possible
    // private properties(properties: any, schema: any): FormField[] {

    //     const fields: FormField[] = [];

    //     // Build FormFields
    //     Object.entries(properties).map(([key, value]: [string, any]) => {
    //         if (!key || !value) {
    //             console.error(`Error while parsing properties from a schema: ${schema.id},  property: key: ${key}, value: ${value}`);
    //             return;
    //         }
    //         const field = this.property(key, value, schema);
    //         fields.push(field);
    //     });
    //     return fields;
    // }

    // private property(key: string, value: any, schema: any): FormField {
    //     const type = value.type ? value.type : null;
    //     const title = value.title ? value.title : null;
    //     const description = value.description ? value.description : null;
    //     const required = (schema.required && schema.required.includes(key)) ? true : false;
    //     const pattern = value.pattern ? value.pattern : null;
    //     const defaultValue = value.default ? value.default : null;
    //     const oneOf: ValueLabel[] = value.oneOf ? this.getFirstEnumAndDescriptionArray(value.oneOf) : null;
    //     const anyOf: ValueLabel[] = value.anyOf ? this.getFirstEnumAndDescriptionArray(value.anyOf) : null;
    //     const allOf: ValueLabel[] = value.allOf ? this.getFirstEnumAndDescriptionArray(value.allOf) : null;

    //     const field: FormField = {
    //         key: key,
    //         value: defaultValue,
    //         type: type,
    //         title: title,
    //         description: description,
    //         required: required,
    //         pattern: pattern,
    //         formType: this.getFormTypeFromSchemaType(type),
    //         error: null,
    //         tooltip: null,
    //         oneOf: oneOf,
    //         anyOf: anyOf,
    //         allOf: allOf
    //     }

    //     if (value.meta)
    //         this.patchMeta(field, value.meta);

    //     return field;
    // }

    // getFirstEnumAndDescriptionArray(arr: any[]): ValueLabel[] {
    //     return arr.map(x => {
    //         if (x.enum && x.description)
    //             return { value: x.enum[0], label: x.description }
    //     });
    // }

    // // Extract the SW360 meta data fields from a schema property value.
    // private patchMeta(field: FormField, meta: Meta): void {
    //     field.formType = meta.formType ? meta.formType : field.formType;
    //     field.error = meta.error ? meta.error : null;
    //     field.tooltip = meta.tooltip ? meta.tooltip : null;
    // }

    // private getFormTypeFromSchemaType(type: string) {
    //     switch (type) {
    //         case JSONSchemaTypes.string:
    //             return FormType.input;
    //         case JSONSchemaTypes.number:
    //             return FormType.input;
    //         case JSONSchemaTypes.boolean:
    //             return FormType.checkbox;
    //         default:
    //             return null;
    //     }
    // }

}

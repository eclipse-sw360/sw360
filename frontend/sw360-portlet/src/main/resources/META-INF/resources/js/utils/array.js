/*
 * Copyright TOSHIBA CORPORATION, 2021. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2021. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
function dynamicSort(property, type) {
    var sortOrder = 1;

    if(property[0] === "-") {
        sortOrder = -1;

        property = property.substr(1);
    }

    return function (a,b) {
        var result;

        switch (type) {
            case 'int':
                result = (parseInt(a[property]) < parseInt(b[property])) ? -1 : (parseInt(a[property]) > (b[property])) ? 1 : 0;
                break;
            case 'string':
            default:
                result = (a[property] < b[property]) ? -1 : (a[property] > b[property]) ? 1 : 0;
        }

        return  result * sortOrder;
    }
}
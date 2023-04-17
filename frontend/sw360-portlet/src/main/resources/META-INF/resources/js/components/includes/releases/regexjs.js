/*
 * Copyright TOSHIBA CORPORATION, 2022. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2022. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

define('components/includes/releases/regexjs', ['jquery'] , function($) {
    function regex(key,value) {
                let regexEmail = /^\w+([\.-]\w+)*@\w+([\.-]\w+)*(\.\w{2,3})+$/;
                let regexUrl = /^(https?|chrome):\/\/[^\s$.?#].[^\s]*$/g;
                let content = '';
                if (value.match(regexEmail)) {
                    content +=
                        '<li>' +
                        '<span class="mapDisplayChildItemLeft">'+ key +': </span>'+
                        '<span class="mapDisplayChildItemRight"><a href="mailto:'+ value +'"> '+ value +'</a></span>'+
                        '</li>'
                } else if (value.match(regexUrl)) {
                    content +=
                        '<li>' +
                        '<span class="mapDisplayChildItemLeft">'+ key +': </span>'+
                        '<span class="mapDisplayChildItemRight"><a href="'+ value +'"> '+ value +'</a></span>'+
                        '</li>'
                } else {
                    content +=
                        '<li>' +
                        '<span class="mapDisplayChildItemLeft">' + key + ': </span>' +
                        '<span class="mapDisplayChildItemRight"> ' + value + '</span>' +
                        '</li>'
                }
                return content;
    }
    return {
        regex: regex,
    };
});

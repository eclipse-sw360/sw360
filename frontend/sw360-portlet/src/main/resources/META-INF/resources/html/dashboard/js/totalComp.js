/*
 * Copyright Siemens AG, 2024.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
define(['d3'], function(d3) {
var csvFilePathComp = contextPathUrl + '/html/dashboard/data/noOfProjCompRel.csv';

var dataDisplayDivComp = d3.select('#totalCompContent');

d3.csv(csvFilePathComp)
    .then(function(data) {
        const totalComponents = data[0].value;

        dataDisplayDivComp.text(`Total Components: ${totalComponents}`);
    })
    .catch(function(error) {
        console.error('Error loading CSV file:', error);
    });
});

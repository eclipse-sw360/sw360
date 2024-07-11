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
var csvFilePath = contextPathUrl + '/html/dashboard/data/noOfProjCompRel.csv';

var dataDisplayDiv = d3.select('#totalProjContent');

d3.csv(csvFilePath)
    .then(function(data) {
	
	const totalProjects = data[1].value;

	dataDisplayDiv.text(`Total Projects: ${totalProjects}`);
	
    })
    .catch(function(error) {
	console.error('Error loading CSV file:', error);
});
});

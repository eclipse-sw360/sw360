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
	var csvFilePathRel = contextPathUrl + '/html/dashboard/data/SI/noOfProjCompRel.csv';

	var dataDisplayDivRel = d3.select('#totalRelContent');

	d3.csv(csvFilePathRel)
	    .then(function(data) {
		const totalReleases = data[2].value;

		dataDisplayDivRel.text(`Total Releases: ${totalReleases}                                    `);
	    })
	    .catch(function(error) {
		console.error('Error loading CSV file:', error);
	    });
});

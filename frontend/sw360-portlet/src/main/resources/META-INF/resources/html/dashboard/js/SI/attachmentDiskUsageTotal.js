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
	var csvFilePathAttachment = contextPathUrl + '/html/dashboard/data/SI/attachmentUsage.csv';

	var dataDisplayDivAttachment = d3.select('#attachmentDiskUsageTotal');

	// Load the CSV file and handle the data
	d3.csv(csvFilePathAttachment)
	    .then(function(data) {
		const attchmentContentLength = data[0].Value;
		const attchmentContentCount = data[0].Key;

		dataDisplayDivAttachment.text(`CLI Disk Usage: ${(attchmentContentLength/(1024*1024*1024)).toFixed(2)} GB`);
	    })
	    .catch(function(error) {
		console.error('Error loading CSV file:', error);
	    });
});

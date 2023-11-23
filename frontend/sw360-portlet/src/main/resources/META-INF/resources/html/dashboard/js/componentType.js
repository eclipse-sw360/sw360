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
	function drawComponentTypeChart() {
	    // Load CSV file using Fetch API
	    fetch(contextPathUrl + '/html/dashboard/data/componentType.csv')
	      .then(response => response.text())
	      .then(csvData => {
		// Parse CSV data and convert to array
		const rows = csvData.split('\n');
		const data = [['type', 'value']];

		for (let i = 1; i < rows.length; i++) {
		  const columns = rows[i].split(',');
		  data.push([columns[0], parseInt(columns[1])]);
		}

		// Create DataTable using Google Charts
		var dataTable = google.visualization.arrayToDataTable(data);

		var options = {
		 // pieHole: 0.4,
		  is3D: true,
		  slices: {  0: {offset: 0.2},
		          2: {offset: 0.2},
		          4: {offset: 0.2},
		          6: {offset: 0.2},}
		};

		var chart = new google.visualization.PieChart(document.getElementById('componentTypeViz'));
		chart.draw(dataTable, options);
	      })
	      .catch(error => {
		console.error('Error loading CSV:', error);
	      });
	    }
	
	return {
        	drawComponentTypeChart: drawComponentTypeChart
    	};
});

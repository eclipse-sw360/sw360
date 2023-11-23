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
	function drawmostUsedLicense() {
	    fetch(contextPathUrl + '/html/dashboard/data/mostUsedLicenses.csv')
	    .then(response => response.text())
	    .then(csvData => {
	      // Parse CSV data and convert to array
	      const rows = csvData.split('\n');
	      const data = [['type', 'value']];

	      for (let i = 1; i < 6; i++) {
		const columns = rows[i].split(',');
		data.push([columns[0], parseInt(columns[1])]);
	      }

	      // Create DataTable using Google Charts
	      var dataTable = google.visualization.arrayToDataTable(data);

	      var options = {
	       pieHole: 0.4,
	       is3D: true,
	       colors: ['#dc3912',  '#109618', '#3366cc','#ff9900', '#cccccc'],
		slices: {  0: {offset: 0.0},
		        2: {offset: 0.3},
		        4: {offset: 0.3}
		        }
	      };
	    var chart = new google.visualization.PieChart(document.getElementById('mostUsedLicenseViz'));
	    chart.draw(dataTable, options);
	    
	  })
	  .catch(error => {
	      console.error('Error loading CSV:', error);
	    });
	  }
	return {
        	drawmostUsedLicense: drawmostUsedLicense
    	};
});

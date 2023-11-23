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
  function init() {
    // Load CSV data and initialize the chart
    d3.csv(contextPathUrl + '/html/dashboard/data/SI/releaseStatusBasedOnCompType.csv').then(function(data) {
      populateFilterOptions(data);
      drawChart(data);

       document.getElementById('typeFilter').onchange = function() {
      updateChart();
    	};
    });
  }

  function populateFilterOptions(data) {
    // Populate filter dropdown based on unique types in the data
    var types = [...new Set(data.map(row => row.type))];
    var typeFilter = document.getElementById('typeFilter');
    console.log("types ", types);
    console.log("typeFilter ", typeFilter);
    types.forEach(function(type) {
      var option = document.createElement('option');
      option.value = type;
      option.textContent = type;
      typeFilter.appendChild(option);
    });
  }

  function drawChart(data) {
    var selectedType = document.getElementById('typeFilter').value;
    var filteredData = data.filter(row => row.type === selectedType);

    // Create DataTable using Google Charts
    var dataTable = new google.visualization.DataTable();
    dataTable.addColumn('string', 'Status');
    dataTable.addColumn('number', 'Count');
    dataTable.addRows(filteredData.map(row => [row.status, parseInt(row.count)]));

    var options = {
      pieHole: 0.4,
      colors: ['#dc3912',  '#109618', '#3366cc','#ff9900', '#cccccc']
    };

    var chart = new google.visualization.PieChart(document.getElementById('releaseStatusVizSI'));
    chart.draw(dataTable, options);
  }

  // Update the chart when the filter changes
  function updateChart() {
    d3.csv(contextPathUrl + '/html/dashboard/data/SI/releaseStatusBasedOnCompType.csv').then(function(data) {
      drawChart(data);
    });
  }
  return {
        init: init,
        updateChart: updateChart
    };
});


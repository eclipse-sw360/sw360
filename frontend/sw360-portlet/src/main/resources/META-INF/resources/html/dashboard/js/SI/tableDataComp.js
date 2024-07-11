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
// Function to load and display data from CSV
function loadTableData() {
    const csvFilePath = contextPathUrl + '/html/dashboard/data/SI/mostUsedComp.csv';
  
    // Use D3 to load the CSV file
    d3.csv(csvFilePath).then(function(data) {
      const table = d3.select('#data-table');
  
      const header = d3.select('#header2');

        // Extract the second and third column headers from the CSV data
        const columns = data.columns.slice(1, 3);

        // Append the table header row with column names
        table.select('thead')
      .selectAll('th')
      .data(columns)
      .enter()
      .append('th')
      .text(function(d) {
        return d;
      });
      // Append table rows with data
      table.select('tbody')
        .selectAll('tr')
        .data(data)
        .enter()
        .append('tr')
        .selectAll('td')
        .data(function(d) {
          return columns.map(function(column) {
            return { column: column, value: d[column] };
          });
        })
        .enter()
        .append('td')
        .text(function(d) {
          return d.value;
        });
    }).catch(function(error) {
      console.error(error);
    });
  }
  
  // Call the loadTableData function to populate the table
  loadTableData();
  });

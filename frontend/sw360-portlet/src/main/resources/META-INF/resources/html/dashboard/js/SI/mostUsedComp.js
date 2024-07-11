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
	// set the dimensions and margin5s of the graph
	var margin5 = {top: 10, right: 100, bottom: 45, left: 45},
	    width5 = 1200 - margin5.left - margin5.right,
	    height5 = 510 - margin5.top - margin5.bottom;

	// append the svg5 object to the body of the page
	var svg5 = d3.select("#mostUsedCompVizSI")
	  .append("svg")
	    .attr("viewBox", `0 0 ${width5 + margin5.left + margin5.right} ${height5 + margin5.top + margin5.bottom}`)
	    .attr("preserveAspectRatio", "xMidYMid meet")
	    .attr("width", width5 + margin5.left + margin5.right)
	    .attr("height", height5 + margin5.top + margin5.bottom)
	  .append("g")
	    .attr("transform", `translate(${margin5.left},${margin5.top})`);
	    
	function shuffleArray(array) {
		for (let i = array.length - 1; i > 0; i--) {
		  const j = Math.floor(Math.random() * (i + 1));
		  [array[i], array[j]] = [array[j], array[i]];
		}
		return array;
	      }
	//Read the data
	d3.csv(contextPathUrl + '/html/dashboard/data/SI/mostUsedComp.csv').then( function(csvData) {
	  var data = csvData.slice(0, 10);
	  data = shuffleArray(data)

	  // Add X axis
	  const x5 = d3.scaleBand()
	    .domain(data.map(d => d.name))
	    .range([ 0, width5 ])
	    .padding(0.5);
	  svg5.append("g")
	    .attr("transform", `translate(0, ${height5})`)
	    .call(d3.axisBottom(x5))
	    .attr("class", "x-axis")
	  .selectAll("text")
	    .attr("transform", "translate(-10,0)rotate(-45)")
	    .style("text-anchor", "end")
	    .style("font-size", "15px");
	    ;

	  // Add Y axis
	  const y5 = d3.scaleLinear()
	    .domain([0, 200])
	    .range([ height5, 0]);
	  svg5.append("g")
	    .call(d3.axisLeft(y5))
	    .selectAll("text")
		.style("font-size", "15px");
	    ;

	  // Add a scale for bubble size
	  const z5 = d3.scaleLinear()
	    .domain([200000, 1310000000])
	    .range([ 4, 40]);

	  // Add a scale for bubble color
	  const myColor5 = d3.scaleOrdinal()
	    .range(d3.schemeSet2);

	  // -1- Create a tooltip div that is hidden by default:
	  const tooltip5 = d3.select("#mostUsedCompVizSI")
	    .append("div")
	      .style("opacity", 0)
	      .style("position", "absolute")
	      .attr("class", "tooltip5")
	      .style("background-color", "white")
	      .style("border", "solid")
	      .style("border-width", "2px")
	      .style("border-radius", "5px")
	      .style("padding", "5px")
	      .style("top","40px")

	  // -2- Create 3 functions to show / update (when mouse move but stay on same circle) / hide the tooltip
	  const showTooltip5 = function(event, d) {
	    tooltip5
	      .transition()
	      .duration(200)
	    tooltip5
	      .style("opacity", 1)
	      //.html("Component Name: " + d.name + " " + "Reused count: " + d.count)
	      .html(d.name + ": " + d.count)
	      .style("left", `${event.layerX+10}px`)
	      .style("top", `${event.layerY}px`)
	  }
	  const moveTooltip5 = function(event, d) {
	    tooltip5
	      .style("left", `${event.layerX+10}px`)
	      .style("top", `${event.layerY}px`)
	  }
	  const hideTooltip5 = function(event, d) {
	    tooltip5
	      .transition()
	      .duration(200)
	      .style("opacity", 0)
	  }

	  // Add dots
	  svg5.append('g')
	    .selectAll("dot")
	    .data(data)
	    .join("circle")
	      .attr("class", "bubbles")
	      .attr("cx", d => x5(d.name))
	      .attr("cy", d => y5(d.count))
	      .attr("r", d => z5(d.count*6000000))
	      .style("fill", d => myColor5(d.name))
	    // -3- Trigger the functions
	    .on("mouseover", showTooltip5 )
	    .on("mousemove", moveTooltip5 )
	    .on("mouseleave", hideTooltip5 )

	  })
});

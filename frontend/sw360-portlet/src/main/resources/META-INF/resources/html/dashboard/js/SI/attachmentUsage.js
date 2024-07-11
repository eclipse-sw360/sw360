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
	var margin4 = {top: 10, right: 100, bottom: 30, left: 30},
	    width4 = 1000 - margin4.left - margin4.right,
	    height4 = 510 - margin4.top - margin4.bottom;

	// append the svg4 object to the body of the page
	var svg4 = d3.select("#CLISizeVizSI")
	  .append("svg")
	    .attr("viewBox", `0 0 ${width4 + margin4.left + margin4.right} ${height4 + margin4.top + margin4.bottom}`)
	    .attr("preserveAspectRatio", "xMidYMid meet")
	    .attr("width", width4 + margin4.left + margin4.right)
	    .attr("height", height4 + margin4.top + margin4.bottom)
	  .append("g")
	    .attr("transform",`translate(50,10)`)

	// Parse the Data 
	d3.csv(contextPathUrl + '/html/dashboard/data/SI/attachmentUsageDept.csv').then( function(data) {

	// X axis
	const x4 = d3.scaleBand()
	  .range([ 0, width4 ])
	  .domain(data.map(d => d.Key))
	  .padding(0.2);
	svg4.append("g")
	  .attr("transform", `translate(0,${height4})`)
	  .attr("class", "x-axis")
	  .call(d3.axisBottom(x4))
	  .selectAll("text")
	    .attr("transform", "translate(-10,0)rotate(-45)")
	    .style("text-anchor", "end")
            .style("font-size", "15px");

	// Add Y axis
	const y4 = d3.scaleLinear()
	  .domain([0, (4).toFixed(4)])
	  .range([ height4, 0]);
	svg4.append("g")
	.attr("class", "x-axis")
	  .call(d3.axisLeft(y4))
          .selectAll("text")
            .style("font-size", "15px");

	var Tooltip4 = d3.select("#CLISizeVizSI")
	.append("div")
	.style("opacity", 0)
	.style("position", "absolute")
	.attr("class", "tooltip4")
	.style("background-color", "white")
	.style("border", "solid")
	.style("border-width", "2px")
	.style("border-radius", "5px")
	.style("padding", "5px")
	.style("top","40px")
	console.log("Inside Tooltip")
	console.log("Tooltip")
	// Three function that change the tooltip when user hover / move / leave a cell
	var mouseover4 = function(event,d) {
	    Tooltip4
	    .style("opacity", 1)
	}
	var mousemove4 = function(event,d) {
	    Tooltip4
	    .html("Exact value: " + ((d.Value/(1024 ** 3)).toFixed(4)) + " GB")
	    .style("left", `${event.layerX+10}px`)
	    .style("top", `${event.layerY}px`)
	}
	var mouseleave4 = function(event,d) {
	    Tooltip4
	    .style("opacity", 0)
	}

	// Bars
	/*svg4.selectAll("mybar")
	  .data(data)
	  .join("rect")
	    .attr("x", d => x4(d.Key))
	    .attr("width", x4.bandwidth())
	    .attr("fill", "steelblue")
	    .attr("height", d => height4 - y4(0)) 
	    .attr("y", d => y4(0))
	    .on("mouseover", mouseover4)
	    .on("mousemove", mousemove4)
	    .on("mouseleave", mouseleave4)

	// Animation
	svg4.selectAll("rect")
	  .transition()
	  .duration(800)
	  .attr("y", d =>  y4((d.Value/(1024 ** 3)).toFixed(4)))
	  .attr("height", d => height4 - y4((d.Value/(1024 ** 3)).toFixed(4)))
	  .delay((d,i) => {console.log(i); return i*100})

	})*/

	// Bars
	    svg4.selectAll(".mybar")
	      .data(data)
	      .join("rect")
		.attr("x", d => x4(d.Key))
		.attr("y", d => y4((d.Value/(1024 ** 3)).toFixed(4)))
		.attr("width", x4.bandwidth())
		.attr("height", d => height4 - y4((d.Value/(1024 ** 3)).toFixed(4)))
		.attr("fill", "steelblue");

	    // Invisible Bars for Tooltip Trigger
	    svg4.selectAll(".tooltip-trigger")
	      .data(data)
	      .join("rect")
		.attr("x", d => x4(d.Key))
		.attr("y", 0)
		.attr("width", x4.bandwidth())
		.attr("height", height4)
		.attr("fill", "transparent")
		.on("mouseover", mouseover4)
		.on("mousemove", mousemove4)
		.on("mouseleave", mouseleave4);

	});

});

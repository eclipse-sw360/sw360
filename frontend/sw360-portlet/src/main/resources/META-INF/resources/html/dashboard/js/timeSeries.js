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
// setting the dimensions and margins of the graph
var margin3 = {top: 10, right: 100, bottom: 30, left: 30},
    width3 = 1000 - margin3.left - margin3.right,
    height3 = 510 - margin3.top - margin3.bottom;

// append the svg3 object to the body of the page
var svg3 = d3.select("#timeSeriesViz")
  .append("svg")
    .attr("viewBox", `0 0 ${width3 + margin3.left + margin3.right} ${height3 + margin3.top + margin3.bottom}`)
    .attr("preserveAspectRatio", "xMidYMid meet")
    .attr("width", width3 + margin3.left + margin3.right)
    .attr("height", height3 + margin3.top + margin3.bottom)
    .attr("max-width", width3 + margin3.left + margin3.right)
    .attr("max-height", height3 + margin3.top + margin3.bottom)
  .append("g")
    .attr("transform",`translate(60,10)`)
    ;


//Read the data TimeSeries.csv
d3.csv(contextPathUrl + '/html/dashboard/data/TimeSeries.csv').then(function(data) {

    // List all groups [Project, Component, Releases]
    const allGroup = ["Project", "Component", "Release"]

    d3.select("#selectButton")
      .selectAll('myOptions')
     	.data(allGroup)
      .enter()
    	.append('option')
      .text(d => d) // text showed in the menu
      .attr("value", d => d) // corresponding value returned by the button

    let selectedOption = d3.select("#selectButton").property("value")

    // Add X axis --> it is a year format
    var x3 = d3.scaleLinear()
      .domain([2015,2024])
      .range([ 0, width3 ])
      ;

    const xaxis3 = d3.axisBottom(x3)
    .tickFormat(d3.format("d"));
    svg3.append("g")
      .attr("class", "x-axis")
      .attr("transform", `translate(0, ${height3})`)
      .call(xaxis3)
      .selectAll("text")
    	.style("font-size", "15px");

    const dataFilter2 = data.map(function(d){return {time: d.Year, value:d[selectedOption]} })
    const maxValueObj2 = dataFilter2.reduce((max, current) => {
    const currentValue2 = parseInt(current.value);
    if (currentValue2 > max.value) {
        return { time: current.time, value: currentValue2 };
        }
        return max;
    }, { value: -Infinity });
    
    // Add Y axis 
    var y3 = d3.scaleLinear()
      .domain([0,maxValueObj2.value ])     
      .range([ height3, 0 ])
    
    svg3.append("g")
    .attr("class", "y-axis")
      .call(d3.axisLeft(y3))
      .selectAll("text")
    	.style("font-size", "15px");

      

      //Adding tooltip
    var Tooltip = d3.select("#timeSeriesViz")
    .append("div")
    .style("opacity", 0)
    .style("position", "absolute")
    .attr("class", "tooltip")
    .style("background-color", "white")
    .style("border", "solid")
    .style("border-width", "2px")
    .style("border-radius", "5px")
    .style("padding", "5px")
    .style("top","40px")
 
    // Three function that change the tooltip when user hovers / moves / leaves a cell
    var mouseover = function(event,d) {
        Tooltip
          .style("opacity", 1)
      }
    var mousemove = function(event,d) {
        Tooltip
          .html("Exact value: " + d[selectedOption])
          .style("left", `${event.layerX - 10}px`)
          .style("top", `${event.layerY + 10}px`)
      }
    var mouseleave = function(event,d) {
        Tooltip
          .style("opacity", 0)
      }

    // Initialize line with group project
    var line = svg3
      .append('g')
      .append("path")
        .datum(data)
        .attr("d", d3.line()
          .x(d => x3(+d.Year))
          .y(d => y3(+d.Project))
        )
        .attr("stroke", "black")
        .style("stroke-width", 2)
        .style("fill", "none")

    // Initialize dots with group Project
    var dot = svg3
      .selectAll('circle')
      .data(data)
      .join('circle')
      .attr("class", "myCircle")
        .attr("cx", d => x3(+d.Year))
        .attr("cy", d => y3(+d.Project))
        .attr("r", 10)
        .style("fill", "#F7941E")
        .on("mouseover", mouseover)
        .on("mousemove", mousemove)
        .on("mouseleave", mouseleave)

    
    
    // A function that updates the chart
    function update(selectedGroup) {

      // Create new data with the selection
      const dataFilter = data.map(function(d){return {time: d.Year, value:d[selectedGroup]} })
      const maxValueObj = dataFilter.reduce((max, current) => {
        const currentValue = parseInt(current.value);
        if (currentValue > max.value) {
            return { time: current.time, value: currentValue };
          }
          return max;
      }, { value: -Infinity });

      svg3.select(".y-axis")
    .transition()
    .duration(1000)  
    .call(d3.axisLeft(y3.domain([0, maxValueObj.value])));

      
      
      Tooltip.html("");
      
      // Give these new data to update line
     line
          .datum(dataFilter)
          .transition()
          .duration(1000)
          .attr("d", d3.line()
            .x(d => x3(+d.time))
            .y(d => y3(+d.value))
          )
      dot
        .data(dataFilter)
        .transition()
        .duration(1000)
          .attr("cx", d => x3(+d.time))
          .attr("cy", d => y3(+d.value))
      
          dot
          .data(dataFilter)
          .on("mouseover", function(event, d) {
            Tooltip
              .html("Exact value: " + d.value)
              .style("left", `${event.layerX - 10}px`)
              .style("top", `${event.layerY + 10}px`)
              .style("opacity", 1);
          })
          .on("mousemove", function(event) {
            Tooltip
            .style("left", `${event.layerX - 10}px`)
            .style("top", `${event.layerY + 10}px`);
          })
          .on("mouseleave", function() {
            Tooltip.style("opacity", 0);
          });
        
        
    }

    // When the button is changed, run the updateChart function
    d3.select("#selectButton").on("change", function(event, d) {
        // recover the option that has been chosen
        let selectedOption = d3.select(this).property("value")
        // run the updateChart function with this selected option
        update(selectedOption)
    })

})
});

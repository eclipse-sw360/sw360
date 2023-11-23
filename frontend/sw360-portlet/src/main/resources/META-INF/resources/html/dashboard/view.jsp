<%--
  ~ Copyright Siemens AG, 2024. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
--%>

<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@ page import="javax.portlet.PortletRequest" %>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>

<%@ taglib prefix="tags" tagdir="/WEB-INF/tags" %>

<%@ include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@ include file="/html/utils/includes/errorKeyToMessage.jspf"%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<portlet:renderURL var="viewSIDashboardURL">
    <portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.PAGENAME_SI_DASHBOARD%>"/>
</portlet:renderURL>
<!DOCTYPE html>
<head>
  <meta charset="utf-8">
  <title>SW360 Dashboard</title>
  <!--<link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.5.0/css/bootstrap.min.css">-->
  <script type="text/javascript" src="https://www.gstatic.com/charts/loader.js"></script>
  <!-- Load d3.js -->
  <script src="https://code.jquery.com/jquery-3.6.4.min.js"></script>
  <script src="https://code.jquery.com/jquery-3.6.4.min.js"></script>

</head>
<style>

body {
            font-family: "Helvetica Neue", Helvetica, Arial, sans-serif;
            background-color: #e7e7ed;
        }

        .dash-header {
            background-color: #fff;
            border-bottom: 1px solid #e7e7ed;
            height: 100px;
        }

        .dash-footer {
            background-color: #fff;
            border-bottom: 1px solid #e7e7ed;
            height: 100px;
            width: 100%;
        }

        .portlet-toolbar {
            justify-content: flex-end;
        }

        svg {
            width: 100%;
            height: 100%;
        }

        path.slice {
            stroke-width: 2px;
        }

        polyline {
            opacity: .3;
            stroke: black;
            stroke-width: 2px;
            fill: none;
        }

        /*.row {
          border: 1px solid #ccc;
          margin-left: 3px;
          margin-right: 3px;

        }*/

        .card {
            flex: 0 0 25%;
            max-width: 25%;
        }

        .card-body {
            flex: 1 1 auto;
            min-height: 1px;
            color: black;
            height: 100%;
            background-color: #fff !important;
        }

        .card-column {
            border-left: 0.25rem solid #5D8EA9!important;
            width: 80%;
            height: 100%;
            background-color: #fff;
            border: 1px solid #ccc;
        }

        /*#footer {
            display: flex;
            background-color: #fff;
            border: 1px solid #ccc;
            height: 150px;
        }*/

        .content {
            padding-left: 20px;
            padding-top: 20px;
            font-size: 30px;
            font-weight: bold;
            color: black;
            white-space: pre-line;
            white-space: pre-wrap;
            white-space: break-spaces;
        }

        #dash-div-header {
            display: flex;
            color: #fff;
            background-color: #5D8EA9;
            font-weight: bold;
            border: 1px solid #ccc;
            margin-bottom: 20px;
        }

        .dash-div-item {
            width: 51%;
        }

        #horizontal-container {
            display: flex;
            width: 100%;
            height: 600px;
            background-color: #fff;
            margin-bottom: 20px;
            flex-wrap: wrap;
        }

        .chart-container {
            border: 1px solid #ccc;
            padding: 10px;
            width: 50%;
            background-color: #fff;
        }

        #CLISizeViz {
            border: 1px solid #ccc;
            padding: 10px;
            width: 50%; 
            /*margin-right: 20px;  */
        }
        #releaseStatusSelect {
            width:10%;
            border-left: 1px solid #ccc;
        }

        #typeFilter {
            margin-top: 10px;
            margin-left: 10px;
            width: 80%; 
            margin-right: 20px; 
        }

        #releaseStatusViz {
            /*border-right: 1px solid #ccc;*/
            width:35%;
            /*width: calc(45% - 20px);  */
        }

        .dash-select {
  
            background-color: #F7941E;
            border-color: #F7941E;
            color: #272833;
            border-style: solid;
            border-width: 0.0625rem;
            border-radius: 0.25rem;
            box-shadow: none;
            cursor: pointer;
            display: inline-block;
            font-size: 1rem;
            font-weight: 600;
            line-height: 1.5;
            padding-bottom: 0.4375rem;
            padding-left: 0.9375rem;
            padding-right: 0.9375rem;
            padding-top: 0.4375rem;
            text-align: center;
            text-transform: none;
        }

        .dash-link {
	    background-color: #F7941E;
            border-color: #F7941E;
            color: #272833;
            border-style: solid;
            border-width: 0.0625rem;
            border-radius: 0.25rem;
            box-shadow: none;
            cursor: pointer;
            display: inline-block;
            font-size: 1rem;
            font-weight: 600;
            line-height: 1.5;
            padding-bottom: 0.4375rem;
            padding-left: 0.9375rem;
            padding-right: 0.9375rem;
            padding-top: 0.4375rem;
            text-align: center;
            text-transform: none;
	    }

        .col-auto {
            margin-left: 15px;
        }


        #table-container {
            overflow: auto;
            position: relative;
            border: 1px solid #ccc;
            width: 50%; 
            max-height: 600px; 
            background-color: white;
           /* margin-bottom: 20px;*/
        }

        #data-table {
            border-collapse: collapse;
            width: 100%;
            table-layout: fixed;
        }

        #data-table th,
        #data-table td,
        #data-table2 th,
        #data-table2 td{
            padding: 8px;
            border: 1px solid #ccc;
            text-align: left;
            word-wrap: break-word;
        }

        #header2,
        #header3 {
            background-color: #f2f2f2; 
            position: sticky;
            top: 0;
            z-index: 1;
        }
        h3 {
        	font-size: 30px;
			text-align: center;
			color: #5D8EA9;
			font-weight: bold;
			padding-top: 10px;
        }
</style>
  

<body>
<script type="text/javascript">
    require([
    'totalProj',
    'totalComp',
    'totalRel',
    'attachmentDiskUsageTotal',
    'timeSeries',
    'attachmentUsage',
    'mostUsedComp',
    'tableDataComp',
    'tableNotUsedComp',
    'componentType',
    'mostUsedLicense',
    'releaseStatusBasedonType'
], function(
    totalProj,
    totalComp,
    totalRel,
    attachmentDiskUsageTotal,
    timeSeries,
    attachmentUsage,
    mostUsedComp,
    tableDataComp,
    tableNotUsedComp,
    componentType,
    mostUsedLicense,
    releaseStatusBasedonType
) {
    console.log(componentType);
    if (componentType && componentType.drawComponentTypeChart) {
        google.charts.load('current', { packages: ['corechart'] });
        google.charts.setOnLoadCallback(componentType.drawComponentTypeChart);
        google.charts.setOnLoadCallback(mostUsedLicense.drawmostUsedLicense);
        google.charts.setOnLoadCallback(releaseStatusBasedonType.init);
	     
		var typeFilterSelect = document.getElementById('typeFilter');
        if (typeFilterSelect) {
            typeFilterSelect.onchange = releaseStatusBasedonType.updateChart;
        }
    } else {
        console.error('componentTypeSI module not loaded or drawComponentTypeChart not defined');
    }
});
</script>

<script type="text/javascript">
	var contextPathUrl = '<%=request.getContextPath()%>';
</script>
<div class="dash-header">
	<div class="row">
		<div class="col">
            <div class="row portlet-toolbar">
                <div class="col-auto">
                    <div class="btn-toolbar" role="toolbar">
                        <div class="btn-group" role="group">
                            <a class ="dash-link" href="<%=viewSIDashboardURL%>">SI Dashboard</a> <!--<%=request.getContextPath()%>/html/dashboard/viewSI.jsp"-->
                        </div>
                    </div>
                </div>
             </div>
         </div>
     </div>
</div>
    <br>
<div class="container-fluid">
        <div class="row">
            <div class="card">
                <div class="card-body">
                    <div class="card-column">
                        <div class="content" id="totalProjContent">
                        </div>
                    </div>
                </div>
            </div>
            <div class="card">
                <div class="card-body">
                    <div class="card-column">
                        <div class="content" id="totalCompContent">
                        </div>
                    </div>
                </div>
            </div>
            <div class="card">
			    <div class="card-body">
			        <div class="card-column">
			            <div class="content" id="totalRelContent">
			            </div>
			         </div>
				 </div>
			</div>
            <div class="card">
                <div class="card-body">
                    <div class="card-column">
                        <div class="content" id="attachmentDiskUsageTotal">
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <br><br>
        <div id="dash-div-header">
            <div class="dash-div-item"><h1 style="padding-left: 10px;"> Time-Series</h1></div>
            <div class="dash-div-item"><h1 style="padding-left: 150px;">Component based on Type</h1></div>
        </div>
        <div class="row">
            <div class="chart-container" id="timeSeriesViz">
                <select class = "dash-select" id="selectButton"></select>
            </div>
            <div class="chart-container" id="componentTypeViz"></div>
        </div>
        <br><br>
        <div id="dash-div-header">
            <div class="dash-div-item"><h1 style="padding-left: 10px;"> CLI Size Per Group </h1></div>
            <div class="dash-div-item"><h1 style="padding-left: 150px;">Release ECC Status</h1></div>
        </div>
        <div class="row">
          <div id="horizontal-container">
            <div id="CLISizeViz">
            </div>
            <div id="releaseStatusSelect">
               <select class="dash-select" style="margin-top: 10px;" id="typeFilter"></select>
            </div>
            <div id="releaseStatusViz">
            </div>
        </div>
        </div>
        <br><br>
        <div id="dash-div-header">
            <div class="dash-div-item"><h1 style="padding-left: 10px;"> Most Used Components</h1></div>
        </div>
        <div class="row">
            <div class="chart-container" id="mostUsedCompViz">
            </div>
            <div id="table-container">
                <table id="data-table">
                    <thead id="header2">
                        <tr>
                            <!-- Table header columns will be generated dynamically -->
                        </tr>
                    </thead>
                    <tbody>
                        <!-- Table rows will be generated dynamically -->
                    </tbody>
                </table>
            </div>
        </div>
        <br><br>
        <div id="dash-div-header">
            <div class="dash-div-item"><h1 style="padding-left: 10px;"> Most Used Licenses </h1></div>
            <div class="dash-div-item"><h1 style="padding-left: 150px;">Not Used Components</h1></div>
        </div>
        <div class="row">
            <div class="chart-container" id="mostUsedLicenseViz"></div>
            <div id="table-container">
                <table id="data-table2">
                    <thead id="header3">
                        <tr>
                            <!-- Table header columns will be generated dynamically -->
                        </tr>
                    </thead>
                    <tbody>
                        <!-- Table rows will be generated dynamically -->
                    </tbody>
                </table>
            </div>
        </div>
        <br><br>
        

        <script type="text/javascript" src="https://www.gstatic.com/charts/loader.js"></script>
        <script type="text/javascript">            
        </script>
    </body>
    <script></script>
</html>

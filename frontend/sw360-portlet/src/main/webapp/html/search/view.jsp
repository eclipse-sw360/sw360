<%--
  ~ Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
  ~ With contributions by Bosch Software Innovations GmbH, 2016.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
  --%>

<%@include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@include file="/html/utils/includes/errorKeyToMessage.jspf"%>
<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<%@ page import="static org.eclipse.sw360.portal.common.PortalConstants.KEY_SEARCH_TEXT" %>
<%@ page import="org.eclipse.sw360.datahandler.common.SW360Constants" %>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>

<portlet:renderURL var="edit">
</portlet:renderURL>


<%@ taglib prefix="sw360" uri="/WEB-INF/customTags.tld" %>
<jsp:useBean id="searchtext" type="java.lang.String"  scope="request"/>
<jsp:useBean id="documents" type="java.util.List<org.eclipse.sw360.datahandler.thrift.search.SearchResult>"  scope="request"/>
<jsp:useBean id="typeMask" type="java.util.List<java.lang.String>"  scope="request"/>

<div id="header"></div>
<p class="pageHeader"><span class="pageHeaderBigSpan">Search results</span> <span
        class="pageHeaderSmallSpan">(${documents.size()}) </span></p>

<div id="searchInput" class="content1">
    <form action="${edit}" method="post">
        <table>
            <thead>
            <tr>
                <th style="background-color: #b2e0e0; padding: 10px; dir: left; valign: middle; align: left; font-size: 14px; font-family: Arial;">
                    Keyword Search
                </th>
            </tr>
            </thead>
            <tbody style="background-color: #f8f7f7; border: none;">
            <tr>
                <td>
                    <input type="text"  maxlength="100" class="searchbar"
                           id="keywordsearchinput" name="<portlet:namespace/><%=KEY_SEARCH_TEXT%>"
                           value="${searchtext}">
                    <br/>
                    <input style="padding: 5px 20px 5px 20px; border: none; font-weight:bold; align:center"
                           type="submit" name="searchBtn" value="Search">
                </td>
            </tr>
            </tbody>
        </table>
        <br>
        <ul style="list-style-type: none;">
            <li style="height: 35px;margin-left: 3%;">
                <label> <input type="checkbox" class="typeFilter" value="<%=SW360Constants.TYPE_PROJECT%>" name="<portlet:namespace/><%=PortalConstants.TYPE_MASK%>"   <core_rt:if test="<%=typeMask.contains(SW360Constants.TYPE_PROJECT)%>">   checked="" </core_rt:if> > <img src="<%=request.getContextPath()%>/images/legends_P.png" class="typePic" title="Project"/>&ensp;Projects </label>
            </li>
            <li style="height: 35px;margin-left: 3%;">
                <label> <input type="checkbox" class="typeFilter" value="<%=SW360Constants.TYPE_COMPONENT%>" name="<portlet:namespace/><%=PortalConstants.TYPE_MASK%>" <core_rt:if test="<%=typeMask.contains(SW360Constants.TYPE_COMPONENT)%>"> checked="" </core_rt:if> > <img src="<%=request.getContextPath()%>/images/legend_C.png" class="typePic" title="Component"/>&ensp;Components</label>
            </li>
            <li style="height: 35px;margin-left: 3%;">
                <label> <input type="checkbox" class="typeFilter" value="<%=SW360Constants.TYPE_LICENSE%>" name="<portlet:namespace/><%=PortalConstants.TYPE_MASK%>"   <core_rt:if test="<%=typeMask.contains(SW360Constants.TYPE_LICENSE)%>">   checked="" </core_rt:if> > <img src="<%=request.getContextPath()%>/images/legend_L.png" class="typePic" title="License"/>&ensp;Licenses</label>
            </li>
            <li style="height: 35px;margin-left: 3%;">
                <label> <input type="checkbox" class="typeFilter" value="<%=SW360Constants.TYPE_RELEASE%>" name="<portlet:namespace/><%=PortalConstants.TYPE_MASK%>"   <core_rt:if test="<%=typeMask.contains(SW360Constants.TYPE_RELEASE)%>">   checked="" </core_rt:if> > <img src="<%=request.getContextPath()%>/images/legends_R.png" class="typePic" title="Release"/>&ensp;Releases</label>
            </li>
            <li style="height: 35px;margin-left: 3%;">
                <label> <input type="checkbox" class="typeFilter" value="<%=SW360Constants.TYPE_USER%>" name="<portlet:namespace/><%=PortalConstants.TYPE_MASK%>"      <core_rt:if test="<%=typeMask.contains(SW360Constants.TYPE_USER)%>">      checked="" </core_rt:if> > <img src="<%=request.getContextPath()%>/images/legend_U.png" class="typePic" title="User"/>&ensp;Users</label>
            </li>
            <li style="height: 35px;margin-left: 3%;">
                <label> <input type="checkbox" class="typeFilter"  value="<%=SW360Constants.TYPE_VENDOR%>" name="<portlet:namespace/><%=PortalConstants.TYPE_MASK%>"   <core_rt:if test="<%=typeMask.contains(SW360Constants.TYPE_VENDOR)%>">    checked="" </core_rt:if> > <img src="<%=request.getContextPath()%>/images/legend_V.png" class="typePic" title="Vendor"/>&ensp;Vendors</label>
            </li>
        </ul>

        <button type="button" onclick="deselectAllTypes()">Deselect all</button> <button type="button" onclick="toggleSelection()">Toggle selection</button>
        <br/>
        <b>Note: No type restriction is the same as looking for all types, also types that are not in the list</b>
    </form>
</div>
<div id="searchTableDiv" class="SW360content2">
    <table id="searchTable" cellpadding="0" cellspacing="0" border="0" class="display">
        <tfoot>
        <tr>
            <th style="width:50%;"></th>
            <th style="width:50%;"></th>
        </tr>
        </tfoot>
    </table>
</div>

<script type="text/javascript" src="<%=request.getContextPath()%>/webjars/jquery/1.12.4/jquery.min.js"></script>
<script type="text/javascript" src="<%=request.getContextPath()%>/webjars/datatables/1.10.15/js/jquery.dataTables.min.js"></script>

<link rel="stylesheet" href="<%=request.getContextPath()%>/css/dataTable_Siemens.css">
<link rel="stylesheet" href="<%=request.getContextPath()%>/css/search.css">

<script>

    Liferay.on('allPortletsReady', function() {
        createSearchTable();
    });

    function deselectAllTypes() {
        $('.typeFilter').prop("checked", false);
        return false;
    }

    function toggleSelection() {
        $('.typeFilter').prop("checked", function (i, val) {
            return !val;
        });
        return false;
    }

    function typeColumn(data, type, full) {
        if (data === '<%=SW360Constants.TYPE_PROJECT%>')
            return '<img src="<%=request.getContextPath()%>/images/legends_P.png" class="typePic"  title="Project"/>';

        if (data === '<%=SW360Constants.TYPE_COMPONENT%>')
            return '<img src="<%=request.getContextPath()%>/images/legend_C.png" class="typePic" title="Components"/>';

        if (data === '<%=SW360Constants.TYPE_LICENSE%>')
            return '<img src="<%=request.getContextPath()%>/images/legend_L.png" class="typePic" title="License"/>';

        if (data === '<%=SW360Constants.TYPE_RELEASE%>')
            return '<img src="<%=request.getContextPath()%>/images/legends_R.png" class="typePic" title="Release" />';

        if (data === '<%=SW360Constants.TYPE_USER%>')
            return '<img src="<%=request.getContextPath()%>/images/legend_U.png"  class="typePic" title="User" />';

        if (data === '<%=SW360Constants.TYPE_VENDOR%>')
            return '<img src="<%=request.getContextPath()%>/images/legend_V.png" class="typePic" title="Vendor" />';

        else return data;
    }

    function createSearchTable() {

        var result = [];

        <core_rt:forEach items="${documents}" var="doc">
        result.push({
            "DT_RowId": '${doc.id}',
            "0": '${doc.type}',
            <core_rt:choose>
                <core_rt:when test="${doc.type.equals('project')
                                   || doc.type.equals('component')
                                   || doc.type.equals('release')
                                   || doc.type.equals('license')}">
                    "1":  "<sw360:DisplaySearchResultLink searchResult="${doc}" />"
                </core_rt:when>
                <core_rt:otherwise>
                    "1":  "<sw360:out value="${doc.name}" />"
                </core_rt:otherwise>
            </core_rt:choose>
        });
        </core_rt:forEach>

        $('#searchTable').DataTable({
            pagingType: "simple_numbers",
            dom: "lrtip",
            data: result,
            columns: [
                { "title": "Type",
                    "render": function ( data, type, full ) {
                        return typeColumn( data, type, full );
                    }
                },
                { "title": "Text" }
            ],
            autoWidth: false
        });
    }
</script>


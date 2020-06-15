<%--
  ~ Copyright Siemens AG, 2013-2017, 2019. Part of the SW360 Portal Project.
  ~ With contributions by Bosch Software Innovations GmbH, 2016.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
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

<jsp:useBean id="searchtext" type="java.lang.String" scope="request"/>
<jsp:useBean id="documents" type="java.util.List<org.eclipse.sw360.datahandler.thrift.search.SearchResult>" scope="request"/>
<jsp:useBean id="typeMask" type="java.util.List<java.lang.String>" scope="request"/>

<div class="container">
	<div class="row">
		<div class="col-3 sidebar">
			<div class="card-deck">
                <div id="keyword-search" class="card">
                    <div class="card-header">
                        <liferay-ui:message key="keyword.search" />
                    </div>
                    <div class="card-body">
                        <form action="${edit}" method="post">
                            <div class="form-group">
                                <input type="text" class="form-control form-control-sm" id="keywordsearchinput" name="<portlet:namespace/><%=KEY_SEARCH_TEXT%>" value="${searchtext}" />
                            </div>
                            <h4><liferay-ui:message key="restrict.to.type" /> <span class="info" title="<liferay-ui:message key="no.type.restriction.is.the.same.as.looking.for.all.types.even.on.types.that.are.not.in.the.list" />"><clay:icon symbol="info-circle-open"/></span></h4>
                            <div class="form-check">
                                <input id="keyword-search-projects" type="checkbox" class="form-check-input" value="<%=SW360Constants.TYPE_PROJECT%>" name="<portlet:namespace/><%=PortalConstants.TYPE_MASK%>"   <core_rt:if test="<%=typeMask.contains(SW360Constants.TYPE_PROJECT)%>">   checked="" </core_rt:if> >
                                <label for="keyword-search-projects" class="form-check-label"><sw360:icon title="projects" icon="project" className="type-icon type-icon-project"/> <liferay-ui:message key="projects" /></label>
                            </div>
                            <div class="form-check">
                                <input id="keyword-search-components" type="checkbox" class="form-check-input" value="<%=SW360Constants.TYPE_COMPONENT%>" name="<portlet:namespace/><%=PortalConstants.TYPE_MASK%>" <core_rt:if test="<%=typeMask.contains(SW360Constants.TYPE_COMPONENT)%>"> checked="" </core_rt:if> >
                                <label for="keyword-search-components" class="form-check-label"><sw360:icon title="components" icon="component" className="type-icon type-icon-component"/> <liferay-ui:message key="components" /></label>
                            </div>
                            <div class="form-check">
                                <input id="keyword-search-licenses" type="checkbox" class="form-check-input" value="<%=SW360Constants.TYPE_LICENSE%>" name="<portlet:namespace/><%=PortalConstants.TYPE_MASK%>"   <core_rt:if test="<%=typeMask.contains(SW360Constants.TYPE_LICENSE)%>">   checked="" </core_rt:if> >
                                <label for="keyword-search-licenses" class="form-check-label"><sw360:icon title="licenses" icon="license" className="type-icon type-icon-license"/> <liferay-ui:message key="licenses" /></label>
                            </div>
                            <div class="form-check">
                                <input id="keyword-search-releases" type="checkbox" class="form-check-input" value="<%=SW360Constants.TYPE_RELEASE%>" name="<portlet:namespace/><%=PortalConstants.TYPE_MASK%>"   <core_rt:if test="<%=typeMask.contains(SW360Constants.TYPE_RELEASE)%>">   checked="" </core_rt:if> >
                                <label for="keyword-search-releases" class="form-check-label"><sw360:icon title="releases" icon="release" className="type-icon type-icon-release"/> <liferay-ui:message key="releases" /></label>
                            </div>
                            <div class="form-check">
                                <input id="keyword-search-obligations" type="checkbox" class="form-check-input" value="<%=SW360Constants.TYPE_OBLIGATIONS%>" name="<portlet:namespace/><%=PortalConstants.TYPE_MASK%>"   <core_rt:if test="<%=typeMask.contains(SW360Constants.TYPE_OBLIGATIONS)%>">   checked="" </core_rt:if> >
                                <label for="keyword-search-obligations" class="form-check-label"><sw360:icon title="obligations" icon="oblig" className="type-icon type-icon-oblig"/> <liferay-ui:message key="obligations" /></label>
                            </div>
                            <div class="form-check">
                                <input id="keyword-search-users" type="checkbox" class="form-check-input" value="<%=SW360Constants.TYPE_USER%>" name="<portlet:namespace/><%=PortalConstants.TYPE_MASK%>"      <core_rt:if test="<%=typeMask.contains(SW360Constants.TYPE_USER)%>">      checked="" </core_rt:if> >
                                <label for="keyword-search-users" class="form-check-label"><sw360:icon title="users" icon="user" className="type-icon type-icon-user"/> <liferay-ui:message key="users" /></label>
                            </div>
                            <div class="form-check">
                                <input id="keyword-search-vendors" type="checkbox" class="form-check-input" value="<%=SW360Constants.TYPE_VENDOR%>" name="<portlet:namespace/><%=PortalConstants.TYPE_MASK%>"   <core_rt:if test="<%=typeMask.contains(SW360Constants.TYPE_VENDOR)%>"> checked="" </core_rt:if> >
                                <label for="keyword-search-vendors" class="form-check-label"><sw360:icon title="vendors" icon="vendor" className="type-icon type-icon-vendor"/> <liferay-ui:message key="vendors" /></label>
                            </div>
                            <div class="form-group">
                                <div class="btn-group btn-group-sm" role="group">
                                    <button class="btn btn-secondary" type="button" data-action="toggle"><liferay-ui:message key="toggle" /></button>
                                    <button class="btn btn-secondary" type="button" data-action="deselect"><liferay-ui:message key="deselect.all" /></button>
                                </div>
                            </div>
                            <button type="submit" class="btn btn-primary btn-sm btn-block"><liferay-ui:message key="search" /></button>
                        </form>
                    </div>
                </div>
            </div>
		</div>
		<div class="col">
            <div class="row portlet-toolbar">
                <div class="col portlet-title text-truncate" title="<liferay-ui:message key="search.results" /> (${documents.size()})">
					<liferay-ui:message key="search.results" /> (${documents.size()})
				</div>
            </div>

            <div class="row">
                <div class="col">
                    <%@ include file="/html/utils/includes/pageSpinner.jspf" %>
			        <table id="searchTable" class="table table-bordered">
                        <colgroup>
                            <col style="width: 3rem" />
                            <col />
                        </colgroup>
                    </table>
                </div>
            </div>

		</div>
	</div>
</div>

<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
    require(['jquery', 'modules/autocomplete', 'modules/dialog', 'bridges/datatables', 'utils/render'], function($, autocomplete, dialog, datatables, render) {

        createSearchTable();

        $('#keyword-search button[data-action="deselect"]').on("click", function() {
            $('#keyword-search .form-check-input').prop("checked", false);
            return false;
        });

        $('#keyword-search button[data-action="toggle"]').on("click", function() {
            $('#keyword-search .form-check-input').prop("checked", function (i, val) {
                return !val;
            });
            return false;
        });

        function typeColumn(data, type, full) {
            if(type === 'display') {
                if (data === '<%=SW360Constants.TYPE_PROJECT%>') {
                    return '<svg class="lexicon-icon type-icon type-icon-project"><title><liferay-ui:message key="project" /></title><use href="<%=request.getContextPath()%>/images/icons.svg#project"/></svg>';
                } else if (data === '<%=SW360Constants.TYPE_COMPONENT%>') {
                    return '<svg class="lexicon-icon type-icon type-icon-component"><title>Component</title><use href="<%=request.getContextPath()%>/images/icons.svg#component"/></svg>';
                } else if (data === '<%=SW360Constants.TYPE_LICENSE%>') {
                    return '<svg class="lexicon-icon type-icon type-icon-license"><title><liferay-ui:message key="license" /></title><use href="<%=request.getContextPath()%>/images/icons.svg#license"/></svg>';
                } else if (data === '<%=SW360Constants.TYPE_RELEASE%>') {
                    return '<svg class="lexicon-icon type-icon type-icon-release"><title><liferay-ui:message key="release" /></title><use href="<%=request.getContextPath()%>/images/icons.svg#release"/></svg>';
                } else if (data === '<%=SW360Constants.TYPE_OBLIGATIONS%>') {
                    return '<svg class="lexicon-icon type-icon type-icon-oblig"><title>ToDo</title><use href="<%=request.getContextPath()%>/images/icons.svg#oblig"/></svg>';
                } else if (data === '<%=SW360Constants.TYPE_USER%>') {
                    return '<svg class="lexicon-icon type-icon type-icon-user"><title><liferay-ui:message key="user" /></title><use href="<%=request.getContextPath()%>/images/icons.svg#user"/></svg>'
                } else if (data === '<%=SW360Constants.TYPE_VENDOR%>') {
                    return '<svg class="lexicon-icon type-icon type-icon-vendor"><title><liferay-ui:message key="vendor" /></title><use href="<%=request.getContextPath()%>/images/icons.svg#vendor"/></svg>'
                } else {
                    return data;
                }
            } else if(type == 'type') {
                return 'string';
            } else {
                return data;
            }
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

            datatables.create('#searchTable', {
                data: result,
                columns: [
                    {
                        title: "<liferay-ui:message key="type" />",
                        render: function ( data, type, full ) {
                            return typeColumn( data, type, full );
                        },
                        className: 'content-center'
                    },
                    {
                        title: '<liferay-ui:message key="text" />'
                    }
                ],
                language: {
                    url: "<liferay-ui:message key="datatables.lang" />",
                    //emptyTable: "<liferay-ui:message key="no.results.found.please.refine.your.search" />",
                    loadingRecords: "<liferay-ui:message key="loading" />"
                },
                initComplete: function() {
                    $('#searchTable').parents('.container').find('.container-spinner').hide();
                }
            });
        }
    });
</script>

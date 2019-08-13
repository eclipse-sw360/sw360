<%--
  ~ Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
  - With contributions by Bosch Software Innovations GmbH, 2016-2017.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
  --%>
<%@include file="/html/init.jsp" %>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<div class="container">
    <div class="row portlet-toolbar">
        <div class="col portlet-title text-truncate" title="Administration">
            Administration
        </div>
    </div>
    <div class="row">
        <div class="col">
            <div class="btn-group justify-content-center" role="group" aria-label="Administration">
                <a class="btn btn-secondary" href="${baseUrl}/../users">
                    <clay:icon symbol="users"/> User
                </a>
                <a class="btn btn-secondary" href="${baseUrl}/../vendors">
                    <clay:icon symbol="suitcase"/> Vendors
                </a>

                <a class="btn btn-secondary" href="${baseUrl}/../bulkreleaseedit">
                    <clay:icon symbol="pencil"/> Bulk Release Edit
                </a>

                <a class="btn btn-secondary" href="${baseUrl}/../license-admin">
                    <clay:icon symbol="document"/> Licenses
                </a>

                <a class="btn btn-secondary" href="${baseUrl}/../todos">
                    <clay:icon symbol="list"/> ToDos
                </a>

                <a class="btn btn-secondary" href="${baseUrl}/../schedule">
                    <clay:icon symbol="calendar"/> Schedule
                </a>

                <a class="btn btn-secondary" href="${baseUrl}/../fossology">
                    <svg class="lexicon-icon"><use href="<%=request.getContextPath()%>/images/icons.svg#fossology" /></svg> Fossology
                </a>

                <a class="btn btn-secondary" href="${baseUrl}/../importexport">
                    <clay:icon symbol="import-export"/> Import & Export
                </a>

                <a class="btn btn-secondary" href="${baseUrl}/../attachmentcleanup">
                    <clay:icon symbol="filter"/> Attachment Cleanup
                </a>

                <a class="btn btn-secondary" href="${baseUrl}/../databasesanitation">
                    <clay:icon symbol="search"/> Database Sanitation
                </a>
            </div>
        </div>
    </div>
</div>

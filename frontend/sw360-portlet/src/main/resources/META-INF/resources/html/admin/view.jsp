<%--
  ~ Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
  - With contributions by Bosch Software Innovations GmbH, 2016-2017.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>
<%@include file="/html/init.jsp" %>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<div class="container">
    <div class="row portlet-toolbar">
        <div class="col portlet-title text-truncate" title="<liferay-ui:message key="administration" />">
            <liferay-ui:message key="administration" />
        </div>
    </div>
    <div class="row">
        <div class="col">
            <div class="btn-group justify-content-center" role="group" aria-label="Administration">
                <a class="btn btn-secondary" href="${baseUrl}/../users">
                    <clay:icon symbol="users"/> <liferay-ui:message key="user" />
                </a>
                <a class="btn btn-secondary" href="${baseUrl}/../vendors">
                    <clay:icon symbol="suitcase"/> <liferay-ui:message key="vendors" />
                </a>

                <a class="btn btn-secondary" href="${baseUrl}/../bulkreleaseedit">
                    <clay:icon symbol="pencil"/> <liferay-ui:message key="bulk.release.edit" />
                </a>

                <a class="btn btn-secondary" href="${baseUrl}/../license-admin">
                    <clay:icon symbol="document"/> <liferay-ui:message key="licenses" />
                </a>

                <a class="btn btn-secondary" href="${baseUrl}/../obligations">
                    <clay:icon symbol="list"/> <liferay-ui:message key="obligations" />
                </a>

                <a class="btn btn-secondary" href="${baseUrl}/../schedule">
                    <clay:icon symbol="calendar"/> <liferay-ui:message key="schedule" />
                </a>

                <a class="btn btn-secondary" href="${baseUrl}/../fossology">
                    <svg class="lexicon-icon"><use href="<%=request.getContextPath()%>/images/icons.svg#fossology" /></svg> <liferay-ui:message key="fossology" />
                </a>

                <a class="btn btn-secondary" href="${baseUrl}/../importexport">
                    <clay:icon symbol="import-export"/> <liferay-ui:message key="import.export" />
                </a>

                <a class="btn btn-secondary" href="${baseUrl}/../attachmentcleanup">
                    <clay:icon symbol="filter"/> <liferay-ui:message key="attachment.cleanup" />
                </a>

                <a class="btn btn-secondary" href="${baseUrl}/../databasesanitation">
                    <clay:icon symbol="search"/> <liferay-ui:message key="database.sanitation" />
                </a>

                <a class="btn btn-secondary" href="${baseUrl}/../oauthclient">
                    <clay:icon symbol="documents-and-media"/> <liferay-ui:message key="oauth.client" />
                </a>
            </div>
        </div>
    </div>
</div>

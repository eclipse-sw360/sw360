<%--
  ~ Copyright TOSHIBA CORPORATION, 2023. Part of the SW360 Portal Project.
  ~ Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2023. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
--%>

<jsp:useBean id="projectList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.projects.ProjectLink>" scope="request"/>
<jsp:useBean id="parentScopeGroupId" class="java.lang.String" scope="request"/>

<%@include file="/html/utils/ajax/linkedProjectRowsDependencyNetwork.jspf"%>

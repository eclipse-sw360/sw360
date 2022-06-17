<%--
  ~ Copyright Siemens AG, 2018. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
--%>

<jsp:useBean id="projectList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.projects.ProjectLink>" scope="request"/>
<jsp:useBean id="parentScopeGroupId" class="java.lang.String" scope="request"/>
<jsp:useBean id="licInfoAttUsages" type="java.util.Map<java.lang.String, org.eclipse.sw360.datahandler.thrift.attachments.AttachmentUsage>" scope="request"/>
<jsp:useBean id="sourceAttUsages" type="java.util.Map<java.lang.String, org.eclipse.sw360.datahandler.thrift.attachments.AttachmentUsage>" scope="request"/>
<jsp:useBean id="manualAttUsages" type="java.util.Map<java.lang.String, org.eclipse.sw360.datahandler.thrift.attachments.AttachmentUsage>" scope="request"/>

<%@include file="/html/projects/includes/attachmentUsagesRows.jspf"%>

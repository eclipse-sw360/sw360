<%--
  ~ Copyright Siemens AG, 2018. Part of the SW360 Portal Project.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
--%>

<jsp:useBean id="projectList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.projects.ProjectLink>" scope="request"/>
<jsp:useBean id="parentScopeGroupId" class="java.lang.String" scope="request"/>
<jsp:useBean id="licInfoAttUsages" type="java.util.Map<java.lang.String, org.eclipse.sw360.datahandler.thrift.attachments.AttachmentUsage>" scope="request"/>
<jsp:useBean id="sourceAttUsages" type="java.util.Map<java.lang.String, org.eclipse.sw360.datahandler.thrift.attachments.AttachmentUsage>" scope="request"/>
<jsp:useBean id="manualAttUsages" type="java.util.Map<java.lang.String, org.eclipse.sw360.datahandler.thrift.attachments.AttachmentUsage>" scope="request"/>

<%@include file="/html/projects/includes/attachmentUsagesRows.jspf"%>

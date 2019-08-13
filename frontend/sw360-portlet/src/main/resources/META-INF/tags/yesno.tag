<%@ attribute name="value" type="java.lang.Boolean" required="true" %>
<%@ taglib prefix="cYesNo" uri="http://java.sun.com/jsp/jstl/core"%>
<%--
  ~ Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
  --%>

<cYesNo:choose><cYesNo:when test="${value}">yes</cYesNo:when><cYesNo:otherwise>no</cYesNo:otherwise></cYesNo:choose>
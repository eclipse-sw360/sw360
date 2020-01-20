<%@ attribute name="value" type="java.lang.Boolean" required="true" %>
<%@ taglib prefix="cYesNo" uri="http://java.sun.com/jsp/jstl/core"%>
<%--
  ~ Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>

<cYesNo:choose><cYesNo:when test="${value}">yes</cYesNo:when><cYesNo:otherwise>no</cYesNo:otherwise></cYesNo:choose>
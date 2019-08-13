<%--
  ~ Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
  ~
  ~ This tag file MUST NOT have any line breaks outside of this multiline comment.
  ~ It is surely possible to implement it in Java to avoid having this weird restriction,
  ~ but this way was perceived as quickest by me at the time.
  --%>

  <%@ attribute name="input" type="java.lang.String" required="true" %>
  <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
  <%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

  <c:set var="newline" value='<%="\n"%>' />${fn:replace(input, newline, '')}

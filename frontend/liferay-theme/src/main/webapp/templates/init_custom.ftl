<!--
  ~ Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
  ~
  ~ SPDX-License-Identifier: EPL-1.0
  ~
  ~ All rights reserved. This program and the accompanying materials
  ~ are made available under the terms of the Eclipse Public License v1.0
  ~ which accompanies this distribution, and is available at
  ~ http://www.eclipse.org/legal/epl-v10.html
  -->

<#assign
	show_breadcrumbs = getterUtil.getBoolean(themeDisplay.getThemeSetting("show-breadcrumbs"))
	hide_portlet_edit_decorators_css = getterUtil.getBoolean(themeDisplay.getThemeSetting("hide-portlet-edit-decorators"))?then("hide-portlet-edit-decorators", "")
/>

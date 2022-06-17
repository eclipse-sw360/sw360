<!--
  ~ Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  -->

<#assign
	show_breadcrumbs = getterUtil.getBoolean(themeDisplay.getThemeSetting("show-breadcrumbs"))
	hide_portlet_edit_decorators_css = getterUtil.getBoolean(themeDisplay.getThemeSetting("hide-portlet-edit-decorators"))?then("hide-portlet-edit-decorators", "")
/>

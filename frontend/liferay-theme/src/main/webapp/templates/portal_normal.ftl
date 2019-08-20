<!DOCTYPE html>

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

<!--
  ~ Taken and modifed from Liferay Theme Toolkit (https://github.com/liferay/liferay-js-themes-toolkit).
  ~
  ~ Copyright (c) 2017 Liferay, Inc.
  ~
  ~ Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
  ~ associated documentation files (the "Software"), to deal in the Software without restriction,
  ~ including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
  ~ and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
  ~ subject to the following conditions:
  ~
  ~ The above copyright notice and this permission notice shall be included in all copies or substantial
  ~ portions of the Software.
  ~
  ~ THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
  ~ NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
  ~ IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
  ~ WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
  ~ SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
  -->


<!DOCTYPE html>

<#include init />

<#assign preferences = freeMarkerPortletPreferences.getPreferences({"portletSetupPortletDecoratorId": "sw360", "destination": "/search"}) />
<#assign login_css = is_signed_in?then("signed-in", "not-signed-in") />
<html class="${root_css_class}" dir="<@liferay.language key="lang.dir" />" lang="${w3c_language_id}">

<head>
	<title>${the_title} - ${company_name}</title>

	<meta content="initial-scale=1.0, width=device-width" name="viewport" />

	<@liferay_util["include"] page=top_head_include />
</head>

<body class="${css_class}">

<@liferay_ui["quick-access"] contentId="#main-content" />

<@liferay_util["include"] page=body_top_include />

<@liferay.control_menu />

<div id="wrapper" class="${hide_portlet_edit_decorators_css} ${login_css}">
	<header id="banner" role="banner">
		<div id="heading" class="container">
			<div class="row">
				<div class="col-3">
					<a class="${logo_css_class}" href="${site_default_url}" title="<@liferay.language_format arguments="${site_name}" key="go-to-x" />">
						<img alt="${logo_description}" height="56" src="${site_logo}" />
					</a>
				</div>
				<div class="col">
					<div class="header-toolbar">
						<#if is_signed_in>
							<@liferay.search_bar default_preferences="${preferences}" />
						</#if>

						<@liferay.user_personal_bar />
					</div>
				</div>
			</div>
			<#if has_navigation && is_setup_complete>
				<div class="row">
					<div class="col">
						<div class="navbar">
							<@liferay.navigation_menu default_preferences="${preferences}" />
						</div>
					</div>
				</div>
			</#if>
		</div>
	</header>

	<#if show_breadcrumbs>
		<section id="breadcrumbs">
			<div class="container">
				<div class="row">
					<div class="col">
						<nav aria-label="breadcrumb">
							<@liferay.breadcrumbs />
						</nav>
					</div>
				</div>
			</div>
		</section>
	</#if>

	<section id="content">
		<#if selectable>
			<@liferay_util["include"] page=content_include />
		<#else>
			${portletDisplay.recycle()}

			${portletDisplay.setTitle(the_title)}

			<@liferay_theme["wrap-portlet"] page="portlet.ftl">
				<@liferay_util["include"] page=content_include />
			</@>
		</#if>
	</section>

	<footer id="footer" role="contentinfo">
		<div class="powered-by">
			<@liferay.language key="powered-by" />
			<a href="http://www.github.com/eclipse/sw360" rel="external" target="_blank">SW360</a> |
			<a href="/resource/docs/api-guide.html" rel="external" target="_blank">REST API Docs</a> |
			<a href="https://github.com/eclipse/sw360/issues" rel="external" target="_blank"> Report an issue.</a>
		</div>
		<div class="build-info text-muted">
			<#if sw360_build_info??>
				Version: ${sw360_build_info.sw360Version} | Branch: ${sw360_build_info.gitBranch} (${sw360_build_info.buildNumber}) | Build time: ${sw360_build_info.buildTime}
			<#else>
				No build information available.
			</#if>
		</div>
	</footer>
</div>

<@liferay_util["include"] page=body_bottom_include />

<@liferay_util["include"] page=bottom_include />

</body>

</html>

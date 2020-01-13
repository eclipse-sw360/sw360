/*
 * Copyright Siemens AG, 2015. Part of the SW360 Portal Project.
 * With contributions by Bosch Software Innovations GmbH, 2016.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.tags.urlutils;

import org.eclipse.sw360.portal.common.page.PortletPage;
import org.eclipse.sw360.portal.portlets.LinkToPortletConfiguration;

import javax.servlet.jsp.JspException;

/**
 * @author daniele.fognini@tngtech.com
 */
public interface UrlWriter {
    UrlWriter withParam(String name, String value) throws JspException;

    UrlWriter toPortlet(LinkToPortletConfiguration portlet, Long scopeGroupId) throws JspException;

    UrlWriter toPage(PortletPage page) throws JspException;

    void writeUrlToJspWriter() throws JspException;
}

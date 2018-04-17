/*
 * Copyright Siemens AG, 2015. Part of the SW360 Portal Project.
 * With contributions by Bosch Software Innovations GmbH, 2016.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.tags.urlutils;

import com.liferay.taglib.portlet.ActionURLTag;
import com.liferay.taglib.portlet.RenderURLTag;
import com.liferay.taglib.portlet.ResourceURLTag;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.common.page.PortletPage;
import org.eclipse.sw360.portal.portlets.LinkToPortletConfiguration;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

/**
 * @author daniele.fognini@tngtech.com
 */
public class UrlWriterImpl implements UrlWriter {

    private final ActionURLTag urlTag;
    private boolean done = false;

    private UrlWriterImpl(PageContext pageContext, ActionURLTag urlTag) {
        this.urlTag = urlTag;
        this.urlTag.setPageContext(pageContext);
    }

    @Override
    public UrlWriter withParam(String name, String value) throws JspException {
        checkNotDone();
        urlTag.addParam(name, value);
        return this;
    }

    @Override
    public UrlWriter toPortlet(LinkToPortletConfiguration portlet, Long scopeGroupId) throws JspException {
        checkNotDone();
        urlTag.setPortletName(portlet.portletName());
        urlTag.setPlid(portlet.findPlid(scopeGroupId));
        return this;
    }

    @Override
    public UrlWriter toPage(PortletPage page) throws JspException {
        return withParam(PortalConstants.PAGENAME, page.pagename());
    }

    @Override
    public void writeUrlToJspWriter() throws JspException {
        checkNotDone();
        urlTag.doStartTag();
        urlTag.doEndTag();
        done = true;
    }

    private void checkNotDone() throws JspException {
        if (done) {
            throw new JspException("this url writer has already been written");
        }
    }

    public static UrlWriter resourceUrl(PageContext pageContext) {
        return new UrlWriterImpl(pageContext, new ResourceURLTag());
    }

    public static UrlWriter renderUrl(PageContext pageContext) {
        return new UrlWriterImpl(pageContext, new RenderURLTag());
    }
}

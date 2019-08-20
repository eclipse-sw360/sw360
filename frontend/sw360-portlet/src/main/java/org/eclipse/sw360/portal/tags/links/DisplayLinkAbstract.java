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
package org.eclipse.sw360.portal.tags.links;

import com.liferay.taglib.TagSupport;
import org.eclipse.sw360.portal.tags.OutTag;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import java.io.IOException;

/**
 * Tag to create a link to a portlet.
 * Can link to portlet on different pages (layouts in liferay terminology).
 * Sets the current scopeGroupId on doStartTag for derived classes.
 * @author daniele.fognini@tngtech.com
 */
public abstract class DisplayLinkAbstract extends TagSupport {
    public Boolean bare = false;
    protected Long scopeGroupId;

    public void setScopeGroupId(Long scopeGroupId) {
        if(scopeGroupId != null && scopeGroupId != 0) {
            this.scopeGroupId = scopeGroupId;
        }
    }

    @Override
    public int doStartTag() throws JspException {
        Long scopeGroupIdAttribute = (Long) pageContext.getAttribute("scopeGroupId");
        if (scopeGroupIdAttribute != null && scopeGroupIdAttribute != 0 && (scopeGroupId == null || scopeGroupId == 0)) {
            this.scopeGroupId = scopeGroupIdAttribute;
        }
        try {
            JspWriter jspWriter = pageContext.getOut();
            if (!bare) jspWriter.write("<a href='");
            writeUrl();
            if (!bare) jspWriter.write("'>");

            String value = getTextDisplay();
            if (value != null) {
                OutTag outTag = new OutTag();
                outTag.setPageContext(pageContext);
                outTag.setValue(value);

                outTag.doStartTag();
                outTag.doEndTag();
            }
        } catch (IOException e) {
            throw new JspException("cannot write", e);
        }

        return EVAL_BODY_INCLUDE;
    }

    @Override
    public int doEndTag() throws JspException {
        try {
            if (!bare) {
                JspWriter jspWriter = pageContext.getOut();
                jspWriter.write("</a>");
            }
        } catch (IOException e) {
            throw new JspException("cannot write", e);
        }
        return super.doEndTag();
    }

    public void setBare(Boolean bare) {
        this.bare = bare;
    }

    protected abstract void writeUrl() throws JspException;

    protected abstract String getTextDisplay();
}

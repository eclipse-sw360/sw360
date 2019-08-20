/*
 * Copyright Siemens AG, 2013-2017, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.tags.links;

import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.tags.ContextAwareTag;
import org.eclipse.sw360.portal.tags.urlutils.UrlWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;

import static java.lang.String.format;
import static org.eclipse.sw360.portal.tags.urlutils.UrlWriterImpl.resourceUrl;

/**
 * Base class to generate download links for one or more attachments. Derived
 * classes can overwrite methods to control the behaviour and output of the
 * link.
 */
abstract class DisplayDownloadAbstract extends ContextAwareTag {
    protected final static String DOWNLOAD_IMAGE_ENABLED = "download";

    protected String contextType;
    protected String contextId;

    /**
     * @return the title text for the image of the download link. Displayed in a
     *         tooltip. MUST be html escaped.
     */
    protected abstract String getTitleText();

    /**
     * Used to configure the url writer for the download url. This method is called
     * at the end therefore all values may be overridden.
     * 
     * Derived classes MUST set {@link PortalConstants#ATTACHMENT_ID} value for the
     * url writer.
     * 
     * @param urlWriter
     *            the url writer to configure
     * 
     * @throws JspException
     */
    protected abstract void configureUrlWriter(UrlWriter urlWriter) throws JspException;

    /**
     * @return the filename of the image to display. You may use the constant
     *         {@link #DOWNLOAD_IMAGE_ENABLED}.
     */
    protected String getImage() {
        return DOWNLOAD_IMAGE_ENABLED;
    }

    @Override
    public int doStartTag() throws JspException {
        try {
            JspWriter jspWriter = pageContext.getOut();
            
            jspWriter.write("<a href='");
            UrlWriter urlWriter = resourceUrl(pageContext)
                    .withParam(PortalConstants.ACTION, PortalConstants.ATTACHMENT_DOWNLOAD)
                    .withParam(PortalConstants.CONTEXT_TYPE, contextType)
                    .withParam(PortalConstants.CONTEXT_ID, contextId);
            configureUrlWriter(urlWriter);
            urlWriter.writeUrlToJspWriter();
            jspWriter.write(format(
                    "'><svg class='lexicon-icon'><title>%s</title><use href='/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#%s'/></svg>",
                    getTitleText(),
                    getImage()));
            jspWriter.write("</a>");
        } catch (Exception e) {
            throw new JspException(e);
        }

        return SKIP_BODY;
    }

    public void setContextType(String contextType) {
        this.contextType = contextType;
    }

    public void setContextId(String contextId) {
        this.contextId = contextId;
    }
}

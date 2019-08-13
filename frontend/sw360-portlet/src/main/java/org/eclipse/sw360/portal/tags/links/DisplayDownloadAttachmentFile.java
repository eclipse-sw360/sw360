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

import org.apache.log4j.Logger;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.tags.urlutils.UrlWriter;

import javax.servlet.jsp.JspException;

import static org.eclipse.sw360.portal.tags.TagUtils.escapeAttributeValue;

/**
 * Displays a download link for a given attachment.
 */
public class DisplayDownloadAttachmentFile extends DisplayDownloadAbstract {
    private static final Logger LOGGER = Logger.getLogger(DisplayDownloadAttachmentFile.class);

    private Attachment attachment;

    @Override
    protected String getTitleText() {
        return escapeAttributeValue("Download " + attachment.getFilename());
    }

    @Override
    protected void configureUrlWriter(UrlWriter urlWriter) throws JspException {
        urlWriter.withParam(PortalConstants.ATTACHMENT_ID, attachment.attachmentContentId);
    }

    @Override
    public int doStartTag() throws JspException {
        if(attachment == null) {
            LOGGER.error("No attachment given!");
            return SKIP_BODY;
        }

        return super.doStartTag();
    }

    public void setAttachment(Attachment attachment) {
        this.attachment = attachment;
    }
}

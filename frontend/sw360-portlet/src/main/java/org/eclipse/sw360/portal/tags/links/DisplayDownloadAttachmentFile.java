/*
 * Copyright Siemens AG, 2013-2017, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.tags.links;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.tags.urlutils.UrlWriter;

import javax.servlet.jsp.JspException;

import static org.eclipse.sw360.portal.tags.TagUtils.escapeAttributeValue;

/**
 * Displays a download link for a given attachment.
 */
public class DisplayDownloadAttachmentFile extends DisplayDownloadAbstract {
    private static final Logger LOGGER = LogManager.getLogger(DisplayDownloadAttachmentFile.class);

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

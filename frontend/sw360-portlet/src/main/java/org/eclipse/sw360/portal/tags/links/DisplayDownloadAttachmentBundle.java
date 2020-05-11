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
import org.eclipse.sw360.portal.common.AttachmentPortletUtils;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.tags.urlutils.UrlWriter;

import javax.servlet.jsp.JspException;

import java.util.Collections;
import java.util.Set;

import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptySet;
import static org.eclipse.sw360.portal.tags.TagUtils.escapeAttributeValue;

/**
 * Displays a download link for a bundle of attachments.
 */
public class DisplayDownloadAttachmentBundle extends DisplayDownloadAbstract {
    private static final Logger LOGGER = LogManager.getLogger(DisplayDownloadAttachmentBundle.class);

    protected String name;
    protected Set<Attachment> attachments;

    @Override
    protected String getTitleText() {
        return escapeAttributeValue("Download " + name);
    }

    @Override
    protected void configureUrlWriter(UrlWriter urlWriter) throws JspException {
        for (Attachment attachment : attachments) {
            urlWriter.withParam(PortalConstants.ATTACHMENT_ID, attachment.attachmentContentId);
        }
        urlWriter.withParam(PortalConstants.ALL_ATTACHMENTS, "true");
    }

    @Override
    public int doStartTag() throws JspException {
        if (attachments == null || attachments.isEmpty()) {
            LOGGER.error("No attachments given!");
            return SKIP_BODY;
        }

        if (name == null) {
            name = AttachmentPortletUtils.DEFAULT_ATTACHMENT_BUNDLE_NAME;
        }

        return super.doStartTag();
    }

    public void setName(String name) {
    	this.name = name;
    }

    public void setAttachments(Set<Attachment> attachments) {
        this.attachments = Collections.unmodifiableSet(nullToEmptySet(attachments));
    }
}

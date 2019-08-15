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
package org.eclipse.sw360.portal.tags;

import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.jstl.core.LoopTagStatus;
import javax.servlet.jsp.jstl.core.LoopTagSupport;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;

import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptySet;

/**
 * Displays a list of download links for attachments of the specified type.
 */
public class DisplayDownloadReport extends LoopTagSupport {

    protected Predicate<AttachmentType> attachmentTypePredicate;
    protected Set<Attachment> unfilteredAttachments = Collections.emptySet();
    protected Iterator<Attachment> attachmentIterator = unfilteredAttachments.iterator();

    @Override
    protected void prepare() throws JspTagException {
        this.attachmentIterator = unfilteredAttachments.stream().filter((attachment) -> {
            return attachmentTypePredicate.test(attachment.getAttachmentType());
        }).iterator();
    }

    @Override
    protected Object next() throws JspTagException {
        return attachmentIterator.next();
    }

    @Override
    protected boolean hasNext() throws JspTagException {
        return attachmentIterator.hasNext();
    }

    @Override
    public int doStartTag() throws JspException {
        try {
            JspWriter jspWriter = pageContext.getOut();
            jspWriter.write("<span>");
        } catch (IOException e) {
            throw new JspException(e);
        }

        return super.doStartTag();
    }

    @Override
    public int doEndTag() throws JspException {
        JspWriter jspWriter = pageContext.getOut();
        try {
            LoopTagStatus status = getLoopStatus();
            if (status.getCount() <= 1) {
                jspWriter.print("no report");
            }

            jspWriter.write("</span>");
        } catch (IOException e) {
            throw new JspException(e);
        }

        return super.doEndTag();
    }

    public void setAttachments(Set<Attachment> attachments) {
        this.unfilteredAttachments = Collections.unmodifiableSet(nullToEmptySet(attachments));
    }

    public void setAttachmentTypePredicate(Predicate<AttachmentType> attachmentTypePredicate) {
        this.attachmentTypePredicate = attachmentTypePredicate;
    }
}

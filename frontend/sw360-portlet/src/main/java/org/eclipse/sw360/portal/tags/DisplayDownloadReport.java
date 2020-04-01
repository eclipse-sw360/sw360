/*
 * Copyright Siemens AG, 2013-2017, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.tags;

import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;

import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.util.ResourceBundleUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.jstl.core.LoopTagStatus;
import javax.servlet.jsp.jstl.core.LoopTagSupport;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.ResourceBundle;
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
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        ResourceBundle resourceBundle = ResourceBundleUtil.getBundle("content.Language", request.getLocale(), getClass());
        JspWriter jspWriter = pageContext.getOut();
        try {
            LoopTagStatus status = getLoopStatus();
            if (status.getCount() <= 1) {
                jspWriter.print(LanguageUtil.get(resourceBundle,"no.report"));
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

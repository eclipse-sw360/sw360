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

import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DisplayDownloadAbstractTest {

    @Test
    public void testThatAllAbstractMethodsAreCalled() throws Exception {
        HttpServletRequest servletRequest = Mockito.mock(HttpServletRequest.class);
        JspWriter jspWriter = Mockito.mock(JspWriter.class);
        PageContext pageContext = Mockito.mock(PageContext.class);
        when(pageContext.getOut()).thenReturn(jspWriter);
        when(pageContext.getRequest()).thenReturn(servletRequest);

        DisplayDownloadAbstract displayDownloadAbstractAttachment = Mockito
                .spy(DisplayDownloadAbstract.class);
        displayDownloadAbstractAttachment.setPageContext(pageContext);
        displayDownloadAbstractAttachment.doStartTag();

        verify(displayDownloadAbstractAttachment, times(1)).configureUrlWriter(any());
        verify(displayDownloadAbstractAttachment, times(1)).getImage();
        verify(displayDownloadAbstractAttachment, times(1)).getTitleText();
    }
}

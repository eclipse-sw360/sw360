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

import org.junit.Test;
import org.mockito.Mockito;

import com.liferay.portal.kernel.util.PortalClassLoaderUtil;

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
        PortalClassLoaderUtil.setClassLoader(this.getClass().getClassLoader());
        DisplayDownloadAbstract displayDownloadAbstractAttachment = Mockito
                .spy(DisplayDownloadAbstract.class);
        displayDownloadAbstractAttachment.setPageContext(pageContext);
        displayDownloadAbstractAttachment.doStartTag();

        verify(displayDownloadAbstractAttachment, times(1)).configureUrlWriter(any());
        verify(displayDownloadAbstractAttachment, times(1)).getImage();
        verify(displayDownloadAbstractAttachment, times(1)).getTitleText();
    }
}

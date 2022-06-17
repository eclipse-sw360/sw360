/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.portal.tags;

import com.liferay.portal.kernel.util.JavaConstants;
import com.liferay.taglib.TagSupport;

import javax.portlet.PortletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

/**
 * Heavily inspired by com.liferay.taglib.portlet.NamespaceTag
 *
 * @author Daniele.Fognini@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 */
public class NameSpaceAwareTag extends TagSupport {

    protected String getNamespace() throws JspException {
        try {
            HttpServletRequest request =
                    (HttpServletRequest) pageContext.getRequest();

            PortletResponse portletResponse =
                    (PortletResponse) request.getAttribute(
                            JavaConstants.JAVAX_PORTLET_RESPONSE);

            if (portletResponse != null) {
                return  portletResponse.getNamespace();
            } else {
                throw new JspException("Not in a portlet?");
            }
        } catch (Exception e) {
            throw new JspException(e);
        }
    }

}

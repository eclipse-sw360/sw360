/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.portal.tags;

import com.liferay.portal.kernel.servlet.taglib.TagSupport;
import com.liferay.portal.kernel.util.JavaConstants;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserService;
import org.apache.thrift.TException;

import javax.portlet.PortletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

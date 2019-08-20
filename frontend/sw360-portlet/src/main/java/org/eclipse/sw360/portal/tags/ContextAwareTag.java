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

import com.liferay.taglib.TagSupport;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

/**
 * @author Daniele.Fognini@tngtech.com
 */
public class ContextAwareTag extends TagSupport {

    protected String getContext() throws JspException {
        try {
            HttpServletRequest request =
                    (HttpServletRequest) pageContext.getRequest();

            return request.getContextPath();
        } catch (Exception e) {
            throw new JspException(e);
        }
    }
}

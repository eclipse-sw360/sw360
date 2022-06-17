/*
 * Copyright Siemens AG, 2013-2015, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.tags;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;

import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.util.ResourceBundleUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import java.io.IOException;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * This displays a subscribe button
 *
 * @author Johannes.Najjar@tngtech.com
 */
public class DisplaySubscribeButton extends SimpleTagSupport {

    private Object object;
    private String email;
    private String id = "SubscribeButtonID";

    public void setId(String id) {
        this.id = id;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    public void doTag() throws JspException, IOException {

        PageContext pageContext = (PageContext) getJspContext();
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        ResourceBundle resourceBundle = ResourceBundleUtil.getBundle("content.Language", request.getLocale(), getClass());

        StringBuilder builder = new StringBuilder();

        Set<String> subscribers = null;

        if(object instanceof Release) {
            subscribers = ((Release) object).getSubscribers();
        } else if ( object instanceof Component) {
            subscribers = ((Component) object).getSubscribers();
        }

        subscribers= CommonUtils.nullToEmptySet(subscribers);

        if (subscribers.contains(email)) {
            builder.append(String.format("<button type=\"button\" id=\"%s\" class=\"btn btn-outline-danger subscribed\">"+LanguageUtil.get(resourceBundle, "unsubscribe")+"</button>", id));
        } else {
            builder.append(String.format("<button type=\"button\" id=\"%s\" class=\"btn btn-outline-success\">"+LanguageUtil.get(resourceBundle, "subscribe")+"</button>", id));
        }

        getJspContext().getOut().print(builder.toString());
    }
}

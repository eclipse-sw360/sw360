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

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import java.io.IOException;
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

        StringBuilder builder = new StringBuilder();

        Set<String> subscribers = null;

        if(object instanceof Release) {
            subscribers = ((Release) object).getSubscribers();
        } else if ( object instanceof Component) {
            subscribers = ((Component) object).getSubscribers();
        }

        subscribers= CommonUtils.nullToEmptySet(subscribers);

        if (subscribers.contains(email)) {
            builder.append(String.format("<input type=\"button\" id=\"%s\" value=\"Unsubscribe\" class=\"addButton subscribed\" />", id));
        } else {
            builder.append(String.format("<input type=\"button\" id=\"%s\" value=\"Subscribe\" class=\"addButton\" />", id));
        }

        getJspContext().getOut().print(builder.toString());
    }
}

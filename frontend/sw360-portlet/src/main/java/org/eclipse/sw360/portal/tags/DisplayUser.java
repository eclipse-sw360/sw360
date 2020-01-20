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

import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.portal.users.UserUtils;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import java.io.IOException;

/**
 * This displays a user
 *
 * @author Johannes.Najjar@tngtech.com
 */
public class DisplayUser extends SimpleTagSupport {

    private User user;

    public void setUser(User value) {
        this.user = value;
    }

    public void doTag() throws JspException, IOException {
        getJspContext().getOut().print(UserUtils.displayUser("-unknown User-", user));
    }
}

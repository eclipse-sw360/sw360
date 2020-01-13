/*
 * Copyright Siemens AG, 2016. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.tags;

import com.google.common.base.Strings;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.portal.users.UserCacheHolder;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import java.io.IOException;

import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

/**
 * This displays a user's group. It makes a DB roundtrip for each invocation. Use with caution!
 *
 * @author alex.borodin@evosoft.com
 */
public class DisplayUserGroup extends SimpleTagSupport {
    private String email;

    public void setEmail(String email) {
        this.email = email;
    }

    public void doTag() throws JspException, IOException {
        User user;

        if (!Strings.isNullOrEmpty(email)) {
            user = UserCacheHolder.getUserFromEmail(email);
        } else {
            user = UserCacheHolder.EMPTY_USER;
        }

        getJspContext().getOut().print(escapeHtml(user.getDepartment()));
    }

}

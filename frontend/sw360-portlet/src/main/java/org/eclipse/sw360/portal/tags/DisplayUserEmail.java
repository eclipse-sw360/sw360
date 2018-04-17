/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.tags;

import com.google.common.base.Strings;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserService;
import org.eclipse.sw360.portal.users.UserUtils;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import java.io.IOException;

/**
 * This displays a user
 *
 * @author Johannes.Najjar@tngtech.com
 * @author alex.borodin@evosoft.com
 */
public class DisplayUserEmail extends SimpleTagSupport {
    private String email;
    private Boolean bare = false;
    Logger log = Logger.getLogger(DisplayUserEmail.class);

    private static final UserService.Iface client = new ThriftClients().makeUserClient();

    public void setEmail(String email) {
        this.email = email;
    }

    public void setBare(Boolean bare) {
        this.bare = bare;
    }

    public void doTag() throws JspException, IOException {
        User user = null;

        if (!Strings.isNullOrEmpty(email)) {
            if(client != null && !bare) {
                try {
                    user = client.getByEmail(email);
                } catch(TException e) {
                    log.info("User with email=" + email + " not found in DB");
                }
            }
        }

        getJspContext().getOut().print(UserUtils.displayUser(email, user));
    }
}

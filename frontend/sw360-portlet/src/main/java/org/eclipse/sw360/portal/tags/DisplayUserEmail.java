/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.tags;

import com.google.common.base.Strings;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserService;
import org.eclipse.sw360.portal.users.UserUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
    Logger log = LogManager.getLogger(DisplayUserEmail.class);

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

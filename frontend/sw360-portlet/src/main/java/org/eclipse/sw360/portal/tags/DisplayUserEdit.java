/*
 * Copyright Siemens AG, 2013-2015, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.tags;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserService;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This displays a user in Edit mode
 *
 * @author Johannes.Najjar@tngtech.com
 */
public class DisplayUserEdit extends NameSpaceAwareTag {

    private List<String> emails;
    private String email;
    private String id;
    private String description;
    private Boolean multiUsers;
    private Boolean readonly = false;
    private Logger log = Logger.getLogger(DisplayUserEdit.class);

    public void setMultiUsers(Boolean multiUsers) {
        this.multiUsers = multiUsers;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setEmails(Collection<String> emails) {
        this.emails = new ArrayList<>(CommonUtils.nullToEmptyCollection(emails));
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setReadonly(Boolean readonly) {
        this.readonly = readonly;
    }

    public String getString(String input) {
        if (Strings.isNullOrEmpty(input)) {
            input = "";
        }
        return input;
    }

    public int doStartTag() throws JspException {

        JspWriter jspWriter = pageContext.getOut();
        StringBuilder display = new StringBuilder();

        List<String> userList = new ArrayList<>();
        List<String> emailList = new ArrayList<>();

        List<String> emailInput;

        if (multiUsers) {
            emailInput = emails;
        } else {
            emailInput = new ArrayList<>();
            emailInput.add(email);
        }

        String namespace = getNamespace();

        try {
            UserService.Iface client = new ThriftClients().makeUserClient();

            for (String email : emailInput) {
                User user = null;
                try {
                    if (!Strings.isNullOrEmpty(email))
                        user = client.getByEmail(email);
                } catch (TException e) {
                    log.info("User with email=" + email + " not found in DB");
                }
                emailList.add(email);
                if (user != null) {
                    userList.add(user.getFullname());
                } else {
                    userList.add(email);
                }
            }

            Joiner commaJoiner = Joiner.on(", ");
            String mails = getString(commaJoiner.join(emailList));
            String userNames = getString(commaJoiner.join(userList));

            display.append("<div class=\"form-group\">");
            display.append(String.format("<label for=\"%sDisplay\">%s</label>", id, description))
                    .append(String.format("<input type=\"hidden\" readonly=\"\" value=\"%s\"  id=\"%s\" name=\"%s%s\"/>", mails, id, namespace, id))
                    .append(String.format("<input type=\"text\" readonly value=\"%s\" id=\"%sDisplay\" ", userNames, id));

            if (!readonly) {
                display.append(String.format(" placeholder=\"Click to edit\" class=\"form-control clickable userSearchDialogInteractive\" data-id=\"%s\" data-multi-user=\"%s\"", id,  multiUsers ? "true" : "false"));
            } else {
                display.append(" placeholder=\"Will be set automatically\" class=\"form-control\"");
            }

            display.append("/>");
            display.append("</div>");

            jspWriter.print(display.toString());
        } catch (Exception e) {
            throw new JspException(e);
        }
        return SKIP_BODY;
    }
}

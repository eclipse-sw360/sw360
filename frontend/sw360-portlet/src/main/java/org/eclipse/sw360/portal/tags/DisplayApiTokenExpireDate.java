/*
 * Copyright Siemens AG, 2018.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.tags;

import org.apache.commons.lang.time.DateUtils;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.users.RestApiToken;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import java.io.IOException;
import java.util.Date;

public class DisplayApiTokenExpireDate extends SimpleTagSupport {

    RestApiToken token;

    public void setToken(RestApiToken token) {
        this.token = token;
    }

    public void doTag() throws JspException, IOException {
        Date createdOn = SW360Utils.getDateFromTimeString(token.getCreatedOn());
        String output = SW360Utils.getDateTimeString(DateUtils.addDays(createdOn, token.getNumberOfDaysValid()));
        getJspContext().getOut().print(output);
    }
}

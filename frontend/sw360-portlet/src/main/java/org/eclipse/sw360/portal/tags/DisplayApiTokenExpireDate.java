/*
 * Copyright Siemens AG, 2018.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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

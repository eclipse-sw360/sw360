/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.tags;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserService;
import org.eclipse.sw360.portal.users.UserCacheHolder;

import javax.servlet.jsp.JspException;
import java.util.Optional;

/**
 * Superclass for tags that need the current user from the page context
 *
 * @author: alex.borodin@evosoft.com
 */
public abstract class UserAwareTag extends NameSpaceAwareTag {
    protected User getUserFromContext(String errorMessage) throws JspException, TException {
        Optional<String> userEmailOpt = UserCacheHolder.getUserEmailFromRequest(pageContext.getRequest());
        String userEmail = userEmailOpt.orElseThrow(() -> new JspException(errorMessage));
        UserService.Iface userClient = new ThriftClients().makeUserClient();
        return userClient.getByEmail(userEmail);
    }
}


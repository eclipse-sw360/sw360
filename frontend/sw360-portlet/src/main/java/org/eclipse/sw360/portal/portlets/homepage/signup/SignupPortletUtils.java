/*
 * Copyright Siemens AG, 2013-2016. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.portlets.homepage.signup;

import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.portal.common.PortletUtils;

import javax.portlet.PortletRequest;

/**
 * Signup portlet utils
 *
 * @author alex.borodin@evosoft.com
 */

public abstract class SignupPortletUtils {
    private SignupPortletUtils() {
        // Utility class with only static functions
    }

    static void updateUserFromRequest(PortletRequest request, User user) {
        for (User._Fields field : User._Fields.values()) {
            switch (field) {
                default:
                    setFieldValue(request, user, field);
            }
        }
    }

    private static void setFieldValue(PortletRequest request, User user , User._Fields field) {
        PortletUtils.setFieldValue(request, user, field, User.metaDataMap.get(field), "");
    }

}

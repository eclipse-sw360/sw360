/*
 * Copyright Siemens AG, 2013-2016. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.portlets.homepage.signup;

import com.google.common.base.Strings;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.util.Validator;

import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.portal.common.ErrorMessages;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.users.UserPortletUtils;
import org.eclipse.sw360.portal.users.UserUtils;

import javax.portlet.ActionRequest;
import javax.portlet.PortletRequest;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Registrant class for SignupPortlet
 *
 * @author alex.borodin@evosoft.com
 */
class Registrant extends User {

    private boolean isMale;
    private String password;
    private String password2;

    public Registrant(ActionRequest request) {
        super();
        isMale = false;
        password = request.getParameter(PortalConstants.PASSWORD);
        password2 = request.getParameter(PortalConstants.PASSWORD_REPEAT);
        setWantsMailNotification(true);
    }

    public com.liferay.portal.kernel.model.User addLifeRayUser(PortletRequest request) throws PortalException, SystemException {
        return UserPortletUtils.addLiferayUser(request, getGivenname(), getLastname(), getEmail(),
                getDepartment(), UserUtils.getRoleConstantFromUserGroup(getUserGroup()), isMale, getExternalid(), password, false, false);

    }

    public boolean validateUserData(ActionRequest request) {
        if (isNullOrEmpty(getGivenname())) {
            SessionErrors.add(request, ErrorMessages.FIRST_NAME_CANNOT_BE_EMPTY);
            return false;
        }
        if (isNullOrEmpty(getLastname())) {
            SessionErrors.add(request, ErrorMessages.LAST_NAME_CANNOT_BE_EMPTY);
            return false;
        }
        if (!Validator.isEmailAddress(getEmail())) {
            SessionErrors.add(request, ErrorMessages.EMAIL_NOT_VALID);
            return false;
        }
        if (isNullOrEmpty(getDepartment())) {
            SessionErrors.add(request, ErrorMessages.DEPARTMENT_CANNOT_BE_EMPTY);
            return false;
        }
        if (isNullOrEmpty(getExternalid())) {
            SessionErrors.add(request, ErrorMessages.EXTERNAL_ID_CANNOT_BE_EMPTY);
            return false;
        }
        if (isNullOrEmpty(password)) {
            SessionErrors.add(request, ErrorMessages.PASSWORD_CANNOT_BE_EMPTY);
            return false;
        }
        if(!Strings.nullToEmpty(password).equals(password2)) {
            SessionErrors.add(request, ErrorMessages.PASSWORDS_DONT_MATCH);
            return false;
        }

        return true;
    }
}

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
package org.eclipse.sw360.portal.users;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.model.User;

import org.apache.commons.csv.CSVRecord;

import javax.portlet.PortletRequest;

import static org.eclipse.sw360.portal.users.UserUtils.getRoleConstantFromUserGroup;
import static org.eclipse.sw360.portal.users.UserUtils.userGroupFromString;

/**
 * Created by heydenrb on 01.03.16.
 *
 * @author birgit.heydenreich@tngtech.com
 */
public class UserCSV {

    private String givenname;
    private String lastname;
    private String email;
    private String department;
    private String group;
    private String gid;
    private boolean isMale;
    private String hash;
    private boolean wantsMailNotification = true;

    public String getGivenname(){
        return givenname;
    }

    public String getLastname() {
        return lastname;
    }

    public String getEmail() {
        return email;
    }

    public String getDepartment() {
        return department;
    }

    public String getGroup() {
        return group;
    }

    public String getGid() {
        return gid;
    }

    public boolean wantsMailNotification() {
        return wantsMailNotification;
    }

    public UserCSV(CSVRecord record) {
        givenname = record.get(0);
        lastname = record.get(1);
        email = record.get(2);
        department = record.get(3);
        group = record.get(4);
        gid = record.get(5);
        isMale = Boolean.parseBoolean(record.get(6));
        hash = record.get(7);
        if (record.size() > 8) {
            wantsMailNotification = Boolean.parseBoolean((record.get(8)));
        }
    }

    public User addLifeRayUser(PortletRequest request) throws PortalException, SystemException {
        return UserPortletUtils.addLiferayUser(request, givenname, lastname, email,
                department, getRoleConstantFromUserGroup(userGroupFromString(group)), isMale, gid, hash, true, true);

    }

}

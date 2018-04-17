/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
 * With modifications by Bosch Software Innovations GmbH, 2016.
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
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.model.*;
import com.liferay.portal.model.User;
import com.liferay.portal.service.OrganizationLocalServiceUtil;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.theme.ThemeDisplay;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.users.*;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;

import javax.portlet.PortletRequest;
import javax.portlet.RenderRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.common.SW360Constants.TYPE_USER;

/**
 * Class with helper utils to convert Liferay users to Thrift users
 *
 * @author johannes.najjar@tngtech.com
 * @author alex.borodin@evosoft.com
 */
public class UserUtils {

    private static final Logger log = Logger.getLogger(UserUtils.class);
    private final ThriftClients thriftClients;

    public UserUtils(ThriftClients thriftClients) {
        this.thriftClients = thriftClients;
    }

    public UserUtils() {
        thriftClients = new ThriftClients();
    }

    public static <T> org.eclipse.sw360.datahandler.thrift.users.User synchronizeUserWithDatabase(T source, ThriftClients thriftClients, Supplier<String> emailSupplier, BiConsumer<org.eclipse.sw360.datahandler.thrift.users.User, T> synchronizer) {
        UserService.Iface client = thriftClients.makeUserClient();

        org.eclipse.sw360.datahandler.thrift.users.User thriftUser = null;

        try {
            thriftUser = client.getByEmail(emailSupplier.get());
        } catch (TException e) {
            //This occurs for every new user, so there is not necessarily something wrong
            log.trace("User does not exist in DB yet.");
        }

        try {
            if (thriftUser == null) {
                log.info("Creating new user.");
                thriftUser = new org.eclipse.sw360.datahandler.thrift.users.User();
                synchronizer.accept(thriftUser, source);
                client.addUser(thriftUser);
            } else {
                synchronizer.accept(thriftUser, source);
                client.updateUser(thriftUser);
            }
        } catch (TException e) {
            log.error("Thrift exception when saving the user", e);
        }
        return thriftUser;
    }

    public static String displayUser(String email, org.eclipse.sw360.datahandler.thrift.users.User user) {
        String userString;
        if (user != null) {
            userString = "<a href=\"mailto:" + user.getEmail() + "\">" + user.getGivenname() + " " + user.getLastname() + "</a>";
        } else {
            userString = "<a href=\"mailto:" + email + "\">" + email + "</a>";
        }
        return userString;
    }

    public static List<Organization> getOrganizations(RenderRequest request) {
        long companyId = getCompanyId(request);
        List<Organization> organizations = Collections.emptyList();
        try {
            // This only gives top-level organizations, not the whole tree. TODO: check whether it's necessary to load all organizations
            organizations = OrganizationLocalServiceUtil.getOrganizations(companyId, OrganizationConstants.DEFAULT_PARENT_ORGANIZATION_ID);
        } catch (SystemException e) {
            log.error("Couldn't find top-level organizations", e);
        }
        return organizations;
    }

    public static void activateLiferayUser(PortletRequest request, org.eclipse.sw360.datahandler.thrift.users.User user){
        Optional<User> liferayUser = findLiferayUser(request, user);
        try {
            if (liferayUser.isPresent()) {
                UserLocalServiceUtil.updateStatus(liferayUser.get().getUserId(), WorkflowConstants.STATUS_APPROVED);
            }
        } catch (SystemException | PortalException e) {
            log.error("Could not activate Liferay user", e);
        }

    }

    public static void deleteLiferayUser(PortletRequest request, org.eclipse.sw360.datahandler.thrift.users.User user){
        Optional<User> liferayUser = findLiferayUser(request, user);
        try {
            if (liferayUser.isPresent()){
                UserLocalServiceUtil.deleteUser(liferayUser.get());
            }
        } catch (PortalException | SystemException e) {
            log.error("Could not delete Liferay user", e);
        }

    }

    private static Optional<User> findLiferayUser(PortletRequest request, org.eclipse.sw360.datahandler.thrift.users.User user) {
        long companyId = getCompanyId(request);
        User liferayUser = null;
        try {
            liferayUser = UserLocalServiceUtil.getUserByEmailAddress(companyId, user.getEmail());
        } catch (PortalException | SystemException e) {
            log.error("Could not find Liferay user", e);
        }
        return Optional.ofNullable(liferayUser);
    }

    private static long getCompanyId(PortletRequest request) {
        ThemeDisplay themeDisplay = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
        return themeDisplay.getCompanyId();
    }

    public void synchronizeUserWithDatabase(User user) {
        String userEmailAddress = user.getEmailAddress();

        org.eclipse.sw360.datahandler.thrift.users.User refreshed = UserCacheHolder.getRefreshedUserFromEmail(userEmailAddress);
        if (!equivalent(refreshed, user)) {
            synchronizeUserWithDatabase(user, thriftClients, user::getEmailAddress, UserUtils::fillThriftUserFromLiferayUser);
            UserCacheHolder.getRefreshedUserFromEmail(userEmailAddress);
        }
    }

    private boolean equivalent(org.eclipse.sw360.datahandler.thrift.users.User refreshed, User user) {
        final org.eclipse.sw360.datahandler.thrift.users.User thriftUser = new org.eclipse.sw360.datahandler.thrift.users.User();
        fillThriftUserFromLiferayUser(thriftUser, user);
        return thriftUser.equals(refreshed);
    }

    public static void fillThriftUserFromUserCSV(final org.eclipse.sw360.datahandler.thrift.users.User thriftUser, final UserCSV userCsv) {
        thriftUser.setEmail(userCsv.getEmail());
        thriftUser.setId(userCsv.getEmail());
        thriftUser.setType(TYPE_USER);
        thriftUser.setUserGroup(UserGroup.valueOf(userCsv.getGroup()));
        thriftUser.setExternalid(userCsv.getGid());
        thriftUser.setFullname(userCsv.getGivenname()+" "+userCsv.getLastname());
        thriftUser.setGivenname(userCsv.getGivenname());
        thriftUser.setLastname(userCsv.getLastname());
        thriftUser.setDepartment(userCsv.getDepartment());
        thriftUser.setWantsMailNotification(userCsv.wantsMailNotification());
    }

    public static void fillThriftUserFromLiferayUser(final org.eclipse.sw360.datahandler.thrift.users.User thriftUser, final User user) {
        thriftUser.setEmail(user.getEmailAddress());
        thriftUser.setId(user.getEmailAddress());
        thriftUser.setType(TYPE_USER);
        thriftUser.setUserGroup(getUserGroupFromLiferayUser(user));
        thriftUser.setExternalid(user.getOpenId());
        thriftUser.setFullname(user.getFullName());
        thriftUser.setGivenname(user.getFirstName());
        thriftUser.setLastname(user.getLastName());
        thriftUser.setDepartment(getDepartment(user));
    }

    public static void fillThriftUserFromThriftUser(final org.eclipse.sw360.datahandler.thrift.users.User thriftUser, final org.eclipse.sw360.datahandler.thrift.users.User user) {
        thriftUser.setEmail(user.getEmail());
        thriftUser.setId(user.getEmail());
        thriftUser.setType(TYPE_USER);
        thriftUser.setUserGroup(user.getUserGroup());
        thriftUser.setExternalid(user.getExternalid());
        thriftUser.setFullname(user.getGivenname()+" "+user.getLastname());
        thriftUser.setGivenname(user.getGivenname());
        thriftUser.setLastname(user.getLastname());
        thriftUser.setDepartment(user.getDepartment());
        thriftUser.setWantsMailNotification(user.isWantsMailNotification());
    }

    public static UserGroup getUserGroupFromLiferayUser(com.liferay.portal.model.User user) {

        try {
            List<String> roleNames = user.getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.toList());

            if (roleNames.contains(PortalConstants.ROLENAME_ADMIN)) {
                return UserGroup.ADMIN;
            } else if (roleNames.contains(PortalConstants.ROLENAME_SW360_ADMIN)) {
                return UserGroup.SW360_ADMIN;
            } else if (roleNames.contains(PortalConstants.ROLENAME_CLEARING_ADMIN)) {
                return UserGroup.CLEARING_ADMIN;
            } else if (roleNames.contains(PortalConstants.ROLENAME_ECC_ADMIN)) {
                return UserGroup.ECC_ADMIN;
            } else if (roleNames.contains(PortalConstants.ROLENAME_SECURITY_ADMIN)) {
                return UserGroup.SECURITY_ADMIN;
            }

        } catch (SystemException e) {
            log.error("Problem retrieving UserGroup", e);
        }
        return UserGroup.USER;
    }

    public static String getDepartment(User user) {
        String department = "";
        try {
            List<Organization> organizations = user.getOrganizations();
            if (!organizations.isEmpty()) {
                Organization organization = organizations.get(0);
                department = organization.getName();
            }
        } catch (PortalException | SystemException e) {
            log.error("Error getting department", e);
        }
        return department;
    }

    public static String getRoleConstantFromUserGroup(UserGroup group) {
        switch (group) {
            case ADMIN:
                return RoleConstants.ADMINISTRATOR;
            case SW360_ADMIN:
                return PortalConstants.ROLENAME_SW360_ADMIN;
            case CLEARING_ADMIN:
                return PortalConstants.ROLENAME_CLEARING_ADMIN;
            case ECC_ADMIN:
                return PortalConstants.ROLENAME_ECC_ADMIN;
            case SECURITY_ADMIN:
                return PortalConstants.ROLENAME_SECURITY_ADMIN;
            case USER:
                return RoleConstants.USER;
        }
        return RoleConstants.USER;
    }

    public static UserGroup userGroupFromString(String s) {
        try {
            return UserGroup.valueOf(s);
        } catch (IllegalArgumentException e) {
            log.error("Illegal Argument Exception from " + s, e);
            return UserGroup.USER;
        }
    }
}

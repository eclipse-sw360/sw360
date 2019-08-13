/*
 * Copyright Siemens AG, 2013-2017, 2019. Part of the SW360 Portal Project.
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

import com.liferay.portal.kernel.exception.NoSuchUserException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.model.*;
import com.liferay.portal.kernel.service.OrganizationLocalServiceUtil;
import com.liferay.portal.kernel.service.ServiceContextFactory;
import com.liferay.portal.kernel.service.UserLocalServiceUtil;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.workflow.WorkflowConstants;

import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.datahandler.thrift.users.UserService;
import org.eclipse.sw360.portal.common.PortalConstants;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.jetbrains.annotations.NotNull;

import javax.portlet.PortletRequest;
import javax.portlet.RenderRequest;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptySet;
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

    public static <T> org.eclipse.sw360.datahandler.thrift.users.User synchronizeUserWithDatabase(
            T source, ThriftClients thriftClients, Supplier<String> emailSupplier,
            Supplier<String> extIdSupplier, BiConsumer<org.eclipse.sw360.datahandler.thrift.users.User, T> synchronizer) {
        UserService.Iface client = thriftClients.makeUserClient();

        org.eclipse.sw360.datahandler.thrift.users.User existingThriftUser = null;

        String email = emailSupplier.get();
        try {
            existingThriftUser = client.getByEmailOrExternalId(email, extIdSupplier.get());
        } catch (TException e) {
            //This occurs for every new user, so there is not necessarily something wrong
            log.trace("User not found by email or external ID");
        }

        org.eclipse.sw360.datahandler.thrift.users.User resultUser = null;
        try {
            if (existingThriftUser == null) {
                log.info("Creating new user.");
                resultUser = new org.eclipse.sw360.datahandler.thrift.users.User();
                synchronizer.accept(resultUser, source);
                client.addUser(resultUser);
            } else {
                resultUser = existingThriftUser;
                if (!existingThriftUser.getEmail().equals(email)) { // email has changed
                    resultUser.setFormerEmailAddresses(prepareFormerEmailAddresses(existingThriftUser, email));
                }
                synchronizer.accept(resultUser, source);
                client.updateUser(resultUser);
            }
        } catch (TException e) {
            log.error("Thrift exception when saving the user", e);
        }
        return resultUser;
    }

    @NotNull
    private static Set<String> prepareFormerEmailAddresses(org.eclipse.sw360.datahandler.thrift.users.User thriftUser, String email) {
        Set<String> formerEmailAddresses = nullToEmptySet(thriftUser.getFormerEmailAddresses()).stream()
                .filter(e -> !e.equals(email)) // make sure the current email is not in the former addresses
                .collect(Collectors.toCollection(HashSet::new));
        formerEmailAddresses.add(thriftUser.getEmail());
        return formerEmailAddresses;
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

    public static void activateLiferayUser(PortletRequest request, org.eclipse.sw360.datahandler.thrift.users.User user) {
        try {
            User liferayUser = findLiferayUser(request, user);
            UserLocalServiceUtil.updateStatus(liferayUser.getUserId(), WorkflowConstants.STATUS_APPROVED,
                ServiceContextFactory.getInstance(request));
        } catch (SystemException | PortalException e) {
            log.error("Could not activate Liferay user", e);
        }

    }

    public static void deleteLiferayUser(PortletRequest request, org.eclipse.sw360.datahandler.thrift.users.User user) {
        try {
            User liferayUser = findLiferayUser(request, user);
            UserLocalServiceUtil.deleteUser(liferayUser);
        } catch (PortalException | SystemException e) {
            log.error("Could not delete Liferay user", e);
        }

    }

    public static User findLiferayUser(PortletRequest request, org.eclipse.sw360.datahandler.thrift.users.User user) throws PortalException, SystemException {
        return findLiferayUser(new PortletRequestAdapter(request), user.getEmail(), user.getExternalid());
    }

    public static User findLiferayUser(RequestAdapter requestAdapter, String email, String externalId) throws SystemException, PortalException {
        long companyId = requestAdapter.getCompanyId();
        try {
            return UserLocalServiceUtil.getUserByEmailAddress(companyId, email);
        } catch (NoSuchUserException e) {
            log.info("Could not find user with email: '" + email + "'. Will try searching by external id.");
            try {
                return UserLocalServiceUtil.getUserByScreenName(companyId, externalId);
            } catch (NoSuchUserException nsue) {
                log.info("Could not find user with externalId: '" + externalId);
                throw new NoSuchUserException("Couldn't find user either with email or external id", nsue);
            }
        }
    }


    public static long getCompanyId(PortletRequest request) {
        ThemeDisplay themeDisplay = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
        return themeDisplay.getCompanyId();
    }

    public void synchronizeUserWithDatabase(User user) {
        String userEmailAddress = user.getEmailAddress();
        org.eclipse.sw360.datahandler.thrift.users.User refreshed = UserCacheHolder.getRefreshedUserFromEmail(userEmailAddress);
        if (!equivalent(refreshed, user)) {
            synchronizeUserWithDatabase(user, thriftClients, user::getEmailAddress, user::getScreenName, UserUtils::fillThriftUserFromLiferayUser);
            UserCacheHolder.getRefreshedUserFromEmail(userEmailAddress);
        }
    }

    private boolean equivalent(org.eclipse.sw360.datahandler.thrift.users.User userInSW360, User user) {
        final org.eclipse.sw360.datahandler.thrift.users.User userFromLiferay = new org.eclipse.sw360.datahandler.thrift.users.User();
        fillThriftUserFromLiferayUser(userFromLiferay, user);
        return userFromLiferay.equals(userInSW360);
    }

    public static void fillThriftUserFromUserCSV(final org.eclipse.sw360.datahandler.thrift.users.User thriftUser, final UserCSV userCsv) {
        thriftUser.setEmail(userCsv.getEmail());
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
        thriftUser.setType(TYPE_USER);
        thriftUser.setUserGroup(getUserGroupFromLiferayUser(user));
        thriftUser.setExternalid(user.getScreenName());
        thriftUser.setFullname(user.getFullName());
        thriftUser.setGivenname(user.getFirstName());
        thriftUser.setLastname(user.getLastName());
        thriftUser.setDepartment(getDepartment(user));
    }

    public static void fillThriftUserFromThriftUser(final org.eclipse.sw360.datahandler.thrift.users.User thriftUser, final org.eclipse.sw360.datahandler.thrift.users.User user) {
        thriftUser.setEmail(user.getEmail());
        thriftUser.setId(user.getId());
        thriftUser.setType(TYPE_USER);
        thriftUser.setUserGroup(user.getUserGroup());
        thriftUser.setExternalid(user.getExternalid());
        thriftUser.setFullname(user.getGivenname()+" "+user.getLastname());
        thriftUser.setGivenname(user.getGivenname());
        thriftUser.setLastname(user.getLastname());
        thriftUser.setDepartment(user.getDepartment());
        thriftUser.setWantsMailNotification(user.isWantsMailNotification());
    }

    public static UserGroup getUserGroupFromLiferayUser(User user) {

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

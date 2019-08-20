/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.portlets.admin;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.model.*;
import com.liferay.portal.kernel.portlet.PortletResponseUtil;
import com.liferay.portal.kernel.service.*;
import com.liferay.portal.kernel.upload.UploadPortletRequest;
import com.liferay.portal.kernel.util.PortalUtil;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.datahandler.thrift.users.UserService;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.common.UsedAsLiferayAction;
import org.eclipse.sw360.portal.portlets.Sw360Portlet;
import org.eclipse.sw360.portal.users.UserCSV;
import org.eclipse.sw360.portal.users.UserCacheHolder;
import org.eclipse.sw360.portal.users.UserUtils;

import org.apache.commons.csv.*;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import java.io.*;
import java.util.*;

import javax.portlet.*;
import javax.portlet.Portlet;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.eclipse.sw360.portal.common.PortalConstants.USER_ADMIN_PORTLET_NAME;
import static org.eclipse.sw360.portal.users.UserUtils.getRoleConstantFromUserGroup;

@Component(
    immediate = true,
    properties = {
            "/org/eclipse/sw360/portal/portlets/base.properties",
            "/org/eclipse/sw360/portal/portlets/admin.properties"
    },
    property = {
        "javax.portlet.name=" + USER_ADMIN_PORTLET_NAME,

        "javax.portlet.display-name=User Administration",
        "javax.portlet.info.short-title=User",
        "javax.portlet.info.title=User Administration",

        "javax.portlet.init-param.view-template=/html/admin/user/view.jsp",
    },
    service = Portlet.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class UserPortlet extends Sw360Portlet {
    private static final Logger log = Logger.getLogger(UserPortlet.class);

    @Override
    public void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {

        List<org.eclipse.sw360.datahandler.thrift.users.User> missingUsers = new ArrayList<>();
        List<org.eclipse.sw360.datahandler.thrift.users.User> backEndUsers;

        List<User> liferayUsers;
        List<User> liferayUsers2;
        try {
            liferayUsers2 = UserLocalServiceUtil.getUsers(QueryUtil.ALL_POS, QueryUtil.ALL_POS);
        } catch (SystemException e) {
            log.error("Could not get user List from liferay", e);
            liferayUsers2 = Collections.emptyList();
        }
        liferayUsers = FluentIterable.from(liferayUsers2).filter(new Predicate<User>() {
            @Override
            public boolean apply(User liferayUser) {

                String firstName = liferayUser.getFirstName();
                String lastName = liferayUser.getLastName();
                String emailAddress = liferayUser.getEmailAddress();
                List<Organization> organizations;
                try {
                    organizations = liferayUser.getOrganizations();
                } catch (PortalException | SystemException e) {
                    return false;
                }

                String department = "";

                if (organizations != null && organizations.size() > 0) {
                    department = organizations.get(0).getName();
                }

                String userGroup = "";

                List<Role> roles;
                try {
                    roles = liferayUser.getRoles();
                } catch (SystemException e) {
                    return false;
                }
                List<String> roleNames = new ArrayList<>();

                for (Role role : roles) {
                    roleNames.add(role.getName());
                }

                for (UserGroup group : UserGroup.values()) {
                    String roleConstantFromUserGroup = getRoleConstantFromUserGroup(group);
                    if (roleNames.contains(roleConstantFromUserGroup)) {
                        userGroup = group.toString();
                        break;
                    }
                }

                String gid = liferayUser.getScreenName();
                String passwordHash = liferayUser.getPassword();

                return !(isNullOrEmpty(firstName) || isNullOrEmpty(lastName) || isNullOrEmpty(emailAddress) || isNullOrEmpty(department) || isNullOrEmpty(userGroup) || isNullOrEmpty(gid) || isNullOrEmpty(passwordHash));
            }
        }).toList();


        try {
            UserService.Iface client = thriftClients.makeUserClient();
            backEndUsers = CommonUtils.nullToEmptyList(client.searchUsers(null));
        } catch (TException e) {
            log.error("Problem with user client", e);
            backEndUsers = Collections.emptyList();
        }


        if (backEndUsers.size() > 0) {

            final ImmutableSet<String> lifeRayMails = FluentIterable.from(liferayUsers).transform(new Function<User, String>() {
                @Override
                public String apply(User input) {
                    return input.getEmailAddress();
                }
            }).toSet();

            missingUsers = FluentIterable.from(backEndUsers).filter(new Predicate<org.eclipse.sw360.datahandler.thrift.users.User>() {
                @Override
                public boolean apply(org.eclipse.sw360.datahandler.thrift.users.User input) {
                    return !lifeRayMails.contains(input.getEmail());
                }
            }).toList();

        }

        request.setAttribute(PortalConstants.USER_LIST, liferayUsers);
        request.setAttribute(PortalConstants.MISSING_USER_LIST, missingUsers);

        // Proceed with page rendering
        super.doView(request, response);
    }


    @Override
    public void serveResource(ResourceRequest request, ResourceResponse response) throws IOException, PortletException {
        String action = request.getParameter(PortalConstants.ACTION);

        if (PortalConstants.USER_LIST.equals(action)) {

            try {
                backUpUsers(request, response);
            } catch (SystemException | PortalException e) {
                log.error("Something went wrong with the user backup", e);
            }
        }
    }


    public void backUpUsers(ResourceRequest request, ResourceResponse response) throws IOException, SystemException, PortalException {
        List<User> liferayUsers;
        try {
            liferayUsers = UserLocalServiceUtil.getUsers(QueryUtil.ALL_POS, QueryUtil.ALL_POS);
        } catch (SystemException e) {
            log.error("Could not get user List from liferay", e);
            liferayUsers = Collections.emptyList();
        }


        final ByteArrayOutputStream outB = new ByteArrayOutputStream();
        Writer out = new BufferedWriter(new OutputStreamWriter(outB));

        CSVPrinter csvPrinter = new CSVPrinter(out, CommonUtils.sw360CsvFormat);

        csvPrinter.printRecord("GivenName", "Lastname", "Email", "Department", "UserGroup", "GID", "isMale", "PasswdHash","wantsMailNotification");
        for (User liferayUser : liferayUsers) {

            String firstName = liferayUser.getFirstName();
            String lastName = liferayUser.getLastName();
            String emailAddress = liferayUser.getEmailAddress();
            List<Organization> organizations = liferayUser.getOrganizations();

            String department = "";

            if (organizations != null && organizations.size() > 0) {
                department = organizations.get(0).getName();
            }

            String gid = liferayUser.getScreenName();
            boolean isMale = liferayUser.isMale();
            String passwordHash = liferayUser.getPassword();
            if (isNullOrEmpty(emailAddress) || isNullOrEmpty(department)) {
                continue;
            }
            org.eclipse.sw360.datahandler.thrift.users.User sw360user = UserCacheHolder.getUserFromEmail(emailAddress);
            boolean wantsMailNotification =
                    sw360user.isSetWantsMailNotification() ? sw360user.wantsMailNotification : true;
            String userGroup = sw360user.getUserGroup() != null ? sw360user.getUserGroup().toString() : null;

            csvPrinter.printRecord(firstName, lastName, emailAddress, department, userGroup, gid, isMale, passwordHash, wantsMailNotification);
        }

        csvPrinter.flush();
        csvPrinter.close();

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(outB.toByteArray());
        PortletResponseUtil.sendFile(request, response, "Users.csv", byteArrayInputStream, "text/csv");
    }

    @UsedAsLiferayAction
    public void updateUsers(ActionRequest request, ActionResponse response) throws IOException {

        List<UserCSV> users = getUsersFromRequest(request, "file");

        try {
            createOrganizations(request, users);
        } catch (SystemException | PortalException e) {
            log.error("Error creating organizations", e);
        }

        for (UserCSV user : users) {
            dealWithUser(request, user);
        }
    }

    private String extractHeadDept(String input) {
        String[] split = input.split(" ");
        if (split.length > 1) {
            return split[0] + " " + split[1];
        } else return split[0];

    }

    private void createOrganizations(PortletRequest request, List<UserCSV> users) throws SystemException, PortalException {

        /* Find the departments of the users, create the head departments and then create the organizations */

        ImmutableSet<String> departments = FluentIterable.from(users).transform(input -> input.getDepartment()).toSet();

        createOrganizations(request, departments);
    }

    private void createOrganizations(PortletRequest request, Iterable<String> departments) throws PortalException, SystemException {
        ImmutableSet<String> headDepartments = FluentIterable.from(departments).transform(department -> extractHeadDept(department)).toSet();

        Map<String, Long> organizationIds = new HashMap<>();
        ServiceContext serviceContext = ServiceContextFactory.getInstance(request);
        long companyId = UserUtils.getCompanyId(request);
        for (String headDepartment : headDepartments) {

            long organizationId;
            try {
                organizationId = OrganizationLocalServiceUtil.getOrganizationId(companyId, headDepartment);
            } catch (SystemException e) {
                organizationId = 0;
            }

            if (organizationId == 0) { // The organization does not yet exist
                Organization organization = createOrganization(serviceContext, headDepartment, OrganizationConstants.DEFAULT_PARENT_ORGANIZATION_ID);

                organizationId = organization.getOrganizationId();
            }
            organizationIds.put(headDepartment, organizationId);
        }

        for (String department : departments) {
            long organizationId;
            try {
                organizationId = OrganizationLocalServiceUtil.getOrganizationId(companyId, department);
            } catch (SystemException e) {
                organizationId = 0;
            }
            if (organizationId == 0) { // The organization does not yet exist
                createOrganization(serviceContext, department, organizationIds.get(extractHeadDept(department)).intValue());
            }
        }
    }

    private Organization createOrganization(ServiceContext serviceContext, String headDepartment, int parentId) throws PortalException, SystemException {
        return OrganizationServiceUtil.addOrganization(
                parentId,
                headDepartment,
                OrganizationConstants.TYPE_ORGANIZATION,
                RegionConstants.DEFAULT_REGION_ID,
                CountryConstants.DEFAULT_COUNTRY_ID,
                ListTypeConstants.ORGANIZATION_STATUS_DEFAULT,
                "",
                false,
                serviceContext
        );
    }

    private List<UserCSV> getUsersFromRequest(PortletRequest request, String fileUploadFormId) throws IOException {

        final UploadPortletRequest uploadPortletRequest = PortalUtil.getUploadPortletRequest(request);

        final InputStream stream = uploadPortletRequest.getFileAsStream(fileUploadFormId);
        Reader reader = new InputStreamReader(stream);
        CSVFormat format = CommonUtils.sw360CsvFormat;
        CSVParser parser = new CSVParser(reader, format);
        List<CSVRecord> records;
        records = parser.getRecords();
        if (records.size() > 0) {
            records.remove(0); // Remove header
        }

        return getUsersFromCSV(records);

    }

    private List<UserCSV> getUsersFromCSV(List<CSVRecord> records) {
        List<UserCSV> users = new ArrayList<>();

        for (CSVRecord record : records) {
            try {
                UserCSV user = new UserCSV(record);
                users.add(user);
            } catch (IndexOutOfBoundsException e) {
                log.error("Broken csv record");
            }
        }

        return users;
    }

    private User dealWithUser(PortletRequest request, UserCSV userRec) {
        User user = null;
        try {
            user = userRec.addLifeRayUser(request);
            if (user != null) {
                UserUtils.synchronizeUserWithDatabase(userRec, thriftClients, userRec::getEmail, userRec::getGid, UserUtils::fillThriftUserFromUserCSV);
            }
        } catch (SystemException | PortalException e) {
            log.error("Error creating a new user", e);
        }

        return user;
    }
}

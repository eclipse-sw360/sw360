/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.portlets.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.PwdEncryptorException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.model.*;
import com.liferay.portal.kernel.portlet.PortletResponseUtil;
import com.liferay.portal.kernel.security.auto.login.AutoLoginException;
import com.liferay.portal.kernel.security.pwd.PasswordEncryptorUtil;
import com.liferay.portal.kernel.service.*;
import com.liferay.portal.kernel.service.persistence.CountryUtil;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.upload.UploadPortletRequest;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.WebKeys;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.common.ThriftEnumUtils;
import org.eclipse.sw360.datahandler.couchdb.lucene.LuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.users.ClientMetadata;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.datahandler.thrift.users.UserService;
import org.eclipse.sw360.portal.common.ErrorMessages;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.common.PortletUtils;
import org.eclipse.sw360.portal.common.UsedAsLiferayAction;
import org.eclipse.sw360.portal.common.datatables.PaginationParser;
import org.eclipse.sw360.portal.common.datatables.data.PaginationParameters;
import org.eclipse.sw360.portal.portlets.Sw360Portlet;
import org.eclipse.sw360.portal.tags.DisplayMapOfSecondaryGroupAndRoles;
import org.eclipse.sw360.portal.users.HttpServletRequestAdapter;
import org.eclipse.sw360.portal.users.OrganizationHelper;
import org.eclipse.sw360.portal.users.SSOAutoLogin;
import org.eclipse.sw360.portal.users.UserCSV;
import org.eclipse.sw360.portal.users.UserCacheHolder;
import org.eclipse.sw360.portal.users.UserPortletUtils;
import org.eclipse.sw360.portal.users.UserUtils;
import org.apache.commons.csv.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.portlet.*;
import javax.portlet.Portlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.liferay.portal.kernel.json.JSONFactoryUtil.createJSONArray;
import static com.liferay.portal.kernel.json.JSONFactoryUtil.createJSONObject;
import static org.eclipse.sw360.portal.common.PortalConstants.DATATABLE_DISPLAY_DATA;
import static org.eclipse.sw360.portal.common.PortalConstants.DATATABLE_RECORDS_FILTERED;
import static org.eclipse.sw360.portal.common.PortalConstants.DATATABLE_RECORDS_TOTAL;
import static org.eclipse.sw360.portal.common.PortalConstants.PAGENAME;
import static org.eclipse.sw360.portal.common.PortalConstants.PAGENAME_DETAIL;
import static org.eclipse.sw360.portal.common.PortalConstants.PAGENAME_EDIT;
import static org.eclipse.sw360.portal.common.PortalConstants.USER_ADMIN_PORTLET_NAME;
import static org.eclipse.sw360.portal.users.UserUtils.getRoleConstantFromUserGroup;
import static org.eclipse.sw360.portal.users.UserUtils.userGroupFromString;

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
        "javax.portlet.resource-bundle=content.Language",
        "javax.portlet.init-param.view-template=/html/admin/user/view.jsp",
    },
    service = Portlet.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class UserPortlet extends Sw360Portlet {
    private static final Logger log = LogManager.getLogger(UserPortlet.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final Set<UserGroup> SET_OF_USERGROUP_EXLUDED = ImmutableSet.of(UserGroup.ADMIN);
    private static final OrganizationHelper orgHelper = new OrganizationHelper();
    private static final int USER_NO_SORT = -1;

    private static final ImmutableList<org.eclipse.sw360.datahandler.thrift.users.User._Fields> USER_FILTERED_FIELDS = ImmutableList
            .of(org.eclipse.sw360.datahandler.thrift.users.User._Fields.GIVENNAME,
                    org.eclipse.sw360.datahandler.thrift.users.User._Fields.LASTNAME,
                    org.eclipse.sw360.datahandler.thrift.users.User._Fields.EMAIL,
                    org.eclipse.sw360.datahandler.thrift.users.User._Fields.DEPARTMENT,
                    org.eclipse.sw360.datahandler.thrift.users.User._Fields.USER_GROUP,
                    org.eclipse.sw360.datahandler.thrift.users.User._Fields.SECONDARY_DEPARTMENTS_AND_ROLES);

    private void prepareUserDetails(RenderRequest request) throws UnsupportedEncodingException {
        String email = request.getParameter(PortalConstants.USER_EMAIL);
        fetchUsersFromDBAndSetRequestAttributes(request, email);
    }

    private Object[] fetchUsersFromDBAndSetRequestAttributes(PortletRequest request, String email) {
        UserService.Iface userClient = thriftClients.makeUserClient();
        org.eclipse.sw360.datahandler.thrift.users.User userByEmailFromCouchDB = null;
        Object[] userFromCouchAndLiferayDB = new Object[2];
        try {
            userByEmailFromCouchDB = userClient.getByEmail(email);
        } catch (TException e) {
            log.error("Error occured while fetching user from DB: ");
        }
        userFromCouchAndLiferayDB[0] = userByEmailFromCouchDB;

        if (userByEmailFromCouchDB == null) {
            log.warn("User not found in CouchDB: " + email);
        }
        User liferayUserByEmailAddress = null;
        try {
            HttpServletRequest httpRequest = PortalUtil
                    .getOriginalServletRequest(PortalUtil.getHttpServletRequest(request));
            liferayUserByEmailAddress = UserUtils.findLiferayUser(new HttpServletRequestAdapter(httpRequest), email,
                    userByEmailFromCouchDB == null ? "dummy" : userByEmailFromCouchDB.getExternalid());
        } catch (SystemException | PortalException e) {
            log.warn("User not found in LiferayDB: " + email);
        }

        userFromCouchAndLiferayDB[1] = liferayUserByEmailAddress;

        if (userByEmailFromCouchDB == null && liferayUserByEmailAddress != null) {
            userByEmailFromCouchDB = new org.eclipse.sw360.datahandler.thrift.users.User()
                    .setEmail(liferayUserByEmailAddress.getEmailAddress())
                    .setGivenname(liferayUserByEmailAddress.getFirstName())
                    .setLastname(liferayUserByEmailAddress.getLastName()).setDeactivated(false)
                    .setExternalid(liferayUserByEmailAddress.getScreenName())
                    .setDepartment(UserUtils.getDepartment(liferayUserByEmailAddress))
                    .setUserGroup(UserUtils.getUserGroupFromLiferayUser(liferayUserByEmailAddress));
        }

        if (liferayUserByEmailAddress != null && userByEmailFromCouchDB != null) {
            List<String> primaryRoles = liferayUserByEmailAddress.getRoles().stream().map(role -> role.getName())
                    .collect(Collectors.toList());
            userByEmailFromCouchDB.setPrimaryRoles(primaryRoles);
        }
        if (userByEmailFromCouchDB != null) {
            request.setAttribute(PortalConstants.USER_OBJ, userByEmailFromCouchDB);
            if (liferayUserByEmailAddress == null && userByEmailFromCouchDB.getUserGroup() != null) {
                List<String> primaryRoles = new ArrayList<String>();
                primaryRoles.add(ThriftEnumUtils.enumToString(userByEmailFromCouchDB.getUserGroup()));
                userByEmailFromCouchDB.setPrimaryRoles(primaryRoles);
            }
        }

        if (userByEmailFromCouchDB != null && liferayUserByEmailAddress != null) {
            if (userByEmailFromCouchDB.getId() != null) {
                request.setAttribute(PortalConstants.USER_ACTIVATE_DEACTIVATE, true);
            } else {
                request.setAttribute(PortalConstants.USER_MISSING_COUCHDB, true);
            }
        }
        if (liferayUserByEmailAddress != null) {
            request.setAttribute(PortalConstants.IS_PASSWORD_OPTIONAL, true);
        } else {
            request.setAttribute(PortalConstants.USER_MISSING_LIFERAY, true);
        }

        return userFromCouchAndLiferayDB;
    }

    @UsedAsLiferayAction
    public void deactivate(ActionRequest request, ActionResponse response)
            throws PortletException, IOException, PwdEncryptorException, TException {
        String email = request.getParameter(PortalConstants.USER_EMAIL);
        if (CommonUtils.isNotNullEmptyOrWhitespace(email)) {
            UserService.Iface userClient = thriftClients.makeUserClient();
            org.eclipse.sw360.datahandler.thrift.users.User userByEmailFromCouchDB;
            User liferayUser = null;
            try {
                userByEmailFromCouchDB = userClient.getByEmail(email);
                userByEmailFromCouchDB.setDeactivated(!userByEmailFromCouchDB.isDeactivated());
                userClient.updateUser(userByEmailFromCouchDB);
                try {
                    HttpServletRequest httpRequest = PortalUtil
                            .getOriginalServletRequest(PortalUtil.getHttpServletRequest(request));
                    liferayUser = UserUtils.findLiferayUser(new HttpServletRequestAdapter(httpRequest), email,
                            userByEmailFromCouchDB == null ? "dummy" : userByEmailFromCouchDB.getExternalid());
                } catch (SystemException | PortalException e) {
                    log.warn("User not found in LiferayDB: " + email);
                }
                User deactivateLiferayUser = UserPortletUtils.deactivateLiferayUser(liferayUser,
                        userByEmailFromCouchDB.isDeactivated());
                if (deactivateLiferayUser == null) {
                    response.setRenderParameter(PAGENAME, PAGENAME_EDIT);
                    setSW360SessionError(request, ErrorMessages.ERROR_USER_ACTIVATE_DEACTIVATE);
                }
                response.setRenderParameter(PAGENAME, PAGENAME_DETAIL);
            } catch (TException e) {
                response.setRenderParameter(PAGENAME, PAGENAME_EDIT);
                log.error("Error occured while fetching user from DB: ", e);
            }

            request.removeAttribute(PortalConstants.USER_MISSING_COUCHDB);
            request.removeAttribute(PortalConstants.USER_MISSING_LIFERAY);
            response.setRenderParameter(PortalConstants.USER_EMAIL, email);
        }
    }

    @UsedAsLiferayAction
    public void update(ActionRequest request, ActionResponse response)
            throws PortletException, IOException, PwdEncryptorException, TException {
        String email = request.getParameter(PortalConstants.USER_EMAIL);
        if (CommonUtils.isNotNullEmptyOrWhitespace(email)) {
            Object[] userFromCouchAndLiferayDB = fetchUsersFromDBAndSetRequestAttributes(request, email);
            org.eclipse.sw360.datahandler.thrift.users.User userByEmailFromCouchDB = userFromCouchAndLiferayDB[0] == null
                    ? new org.eclipse.sw360.datahandler.thrift.users.User()
                    : (org.eclipse.sw360.datahandler.thrift.users.User) userFromCouchAndLiferayDB[0];
            if (userFromCouchAndLiferayDB[1] != null) {
                User liferayUserByEmailAddress = (User) userFromCouchAndLiferayDB[1];
                try {
                    if (updateUserObjectFromRequest(request, response, null, userByEmailFromCouchDB, false, false,
                            liferayUserByEmailAddress)) {
                        return;
                    }
                } catch (TException | SystemException | PortalException e) {
                    setErrorResponse(request, response, e, email);
                    return;
                }
            } else {
                try {
                    if (updateUserObjectFromRequest(request, response, new UserCSV(), userByEmailFromCouchDB, false,
                            true, null)) {
                        return;
                    }
                } catch (TException | SystemException | PortalException e) {
                    setErrorResponse(request, response, e, email);
                    return;
                }
            }

            request.removeAttribute(PortalConstants.USER_MISSING_COUCHDB);
            request.removeAttribute(PortalConstants.USER_MISSING_LIFERAY);
            response.setRenderParameter(PAGENAME, PAGENAME_DETAIL);
        } else {
            UserCSV userCSV = new UserCSV();
            org.eclipse.sw360.datahandler.thrift.users.User userByEmailFromCouchDB = new org.eclipse.sw360.datahandler.thrift.users.User();
            try {
                if (updateUserObjectFromRequest(request, response, userCSV, userByEmailFromCouchDB, true, true, null)) {
                    return;
                }
            } catch (TException | SystemException | PortalException e) {
                setSW360SessionError(request, ErrorMessages.ERROR_USER_CREATE);
                log.error("Error while creating user: ", e);
            }

            response.setRenderParameter(PAGENAME, PAGENAME_EDIT);
        }
    }

    private void setErrorResponse(ActionRequest request, ActionResponse response, Exception e, String email) {
        setSW360SessionError(request, ErrorMessages.ERROR_USER_UPDATE);
        response.setRenderParameter(PAGENAME, PAGENAME_EDIT);
        response.setRenderParameter(PortalConstants.USER_EMAIL, email);
        log.error("Error while updating user: ", e);
    }

    private User updateLiferayUser(ActionRequest request, User liferayUserByEmailAddress, String givenNameFromReq,
            String lastNameFromReq, String emailFromReq, String departmentFromReq, String externalIdFromReq,
            String pwdFromReq, String primaryRoleFromReq) throws PwdEncryptorException, SW360Exception {
        String oldEmailId = liferayUserByEmailAddress.getEmailAddress();
        String oldExternalId = liferayUserByEmailAddress.getScreenName();
        liferayUserByEmailAddress.setFirstName(givenNameFromReq);
        liferayUserByEmailAddress.setLastName(lastNameFromReq);
        liferayUserByEmailAddress.setEmailAddress(emailFromReq);
        liferayUserByEmailAddress.setScreenName(externalIdFromReq);
        if (!PortalConstants.SSO_LOGIN_ENABLED && CommonUtils.isNotNullEmptyOrWhitespace(pwdFromReq)) {
            String encryptedPwd = PasswordEncryptorUtil.encrypt(pwdFromReq);
            liferayUserByEmailAddress.setPassword(encryptedPwd);
        }
        User updateLiferayUser = UserPortletUtils.updateLiferayUser(request, liferayUserByEmailAddress,
                departmentFromReq, getRoleConstantFromUserGroup(userGroupFromString(primaryRoleFromReq)), oldEmailId,
                oldExternalId);
        if (updateLiferayUser == null) {
            log.error("Error while updating the user : " + emailFromReq);
            throw new SW360Exception("Error while updating the user : " + emailFromReq);
        }

        return updateLiferayUser;
    }

    private boolean updateUserObjectFromRequest(ActionRequest request, ActionResponse response, UserCSV userCSV,
            org.eclipse.sw360.datahandler.thrift.users.User userByEmailFromCouchDB, boolean isCouchDBUserNew,
            boolean isLiferayUserNew, User liferayUserByEmailAddress)
            throws TException, SystemException, PortalException, IOException {
        org.eclipse.sw360.datahandler.thrift.users.User requestingUser = UserCacheHolder.getUserFromRequest(request);
        UserService.Iface userClient = thriftClients.makeUserClient();
        String givenNameFromReq = request
                .getParameter(org.eclipse.sw360.datahandler.thrift.users.User._Fields.GIVENNAME.toString());
        String lastNameFromReq = request
                .getParameter(org.eclipse.sw360.datahandler.thrift.users.User._Fields.LASTNAME.toString());
        String emailFromReq = request
                .getParameter(org.eclipse.sw360.datahandler.thrift.users.User._Fields.EMAIL.toString());
        String departmentFromReq = request
                .getParameter(org.eclipse.sw360.datahandler.thrift.users.User._Fields.DEPARTMENT.toString());
        String externalIdFromReq = request
                .getParameter(org.eclipse.sw360.datahandler.thrift.users.User._Fields.EXTERNALID.toString());
        String primaryRoleFromReq = request
                .getParameter(org.eclipse.sw360.datahandler.thrift.users.User._Fields.USER_GROUP.toString());
        String pwdFromReq = request.getParameter(PortalConstants.PASSWORD);

        if (emailFromReq != null) {
            emailFromReq = emailFromReq.toLowerCase();
        }
        if (externalIdFromReq != null) {
            externalIdFromReq = externalIdFromReq.toLowerCase();
        }

        Map<String, Set<UserGroup>> secondaryDepartmentAndRolesMapFromRequest = PortletUtils
                .getSecondaryDepartmentAndRolesMapFromRequest(request, departmentFromReq);
        Map<String, ClientMetadata> userAccessFromRequest = PortletUtils
                .getOidcClientMapFromRequest(request);
        String originalEmail = CommonUtils.nullToEmptyString(userByEmailFromCouchDB.getEmail());
        org.eclipse.sw360.datahandler.thrift.users.User updatedUserForDisplay = userByEmailFromCouchDB.deepCopy();
        updatedUserForDisplay.setGivenname(givenNameFromReq).setLastname(lastNameFromReq)
                .setDepartment(departmentFromReq).setExternalid(externalIdFromReq)
                .setUserGroup(UserGroup.valueOf(primaryRoleFromReq)).setPrimaryRoles(null)
                .setSecondaryDepartmentsAndRoles(secondaryDepartmentAndRolesMapFromRequest)
                .setOidcClientInfos(userAccessFromRequest);
        request.setAttribute(PortalConstants.USER_OBJ, updatedUserForDisplay);
        if (isLiferayUserNew) {
            if (!emailFromReq.equals(originalEmail)) {
                try {
                    org.eclipse.sw360.datahandler.thrift.users.User userByEmail = userClient.getByEmail(emailFromReq);
                    if (userByEmail != null) {
                        log.error("Another user with same email id exists:" + emailFromReq);
                        throw new SW360Exception("Another user with same email id exists in couch db:" + emailFromReq);
                    }
                } catch (TException exp) {
                    log.debug("No user with same email id found " + emailFromReq);
                }

                if (!externalIdFromReq.equals(userByEmailFromCouchDB.getExternalid())) {
                    try {
                        org.eclipse.sw360.datahandler.thrift.users.User userByExtId = userClient
                                .getByEmailOrExternalId("dummy@dummy.org", externalIdFromReq);
                        if (userByExtId != null) {
                            log.error("Another user with same external id exists in couch db:" + externalIdFromReq);
                            throw new SW360Exception("Another user with same external id exists:" + externalIdFromReq);
                        }
                    } catch (TException exp) {
                        log.debug("No user with same external id found " + externalIdFromReq);
                    }
                }
            }
        }

        departmentFromReq = orgHelper.mapOrganizationName(departmentFromReq);
        User liferayCreatedOrUpdated = null;
        if (PortalConstants.SSO_LOGIN_ENABLED) {
            long companyId = PortalUtil.getCompanyId(request);
            HttpServletRequest httpRequest = PortalUtil
                    .getOriginalServletRequest(PortalUtil.getHttpServletRequest(request));

            if (isLiferayUserNew) {
                SSOAutoLogin sso = new SSOAutoLogin();
                orgHelper.addOrGetOrganization(departmentFromReq, companyId);

                try {
                    liferayCreatedOrUpdated = sso.createLiferayUser(httpRequest, emailFromReq, externalIdFromReq,
                            givenNameFromReq, lastNameFromReq, companyId, departmentFromReq);
                } catch (AutoLoginException ale) {
                    log.error("Error while creating user in Liferay: ", ale);
                    throw new SW360Exception(ale.getMessage());
                }
                if (liferayCreatedOrUpdated == null) {
                    log.error("Error while creating new liferay user, SSO login enabled : " + emailFromReq);
                    throw new SW360Exception(
                            "Error while creating new liferay user, SSO login enabled : " + emailFromReq);
                }

                if (!primaryRoleFromReq.equalsIgnoreCase(UserGroup.USER.name())) {
                    liferayCreatedOrUpdated = updateLiferayUser(request, liferayCreatedOrUpdated, givenNameFromReq,
                            lastNameFromReq, emailFromReq, departmentFromReq, externalIdFromReq, pwdFromReq,
                            primaryRoleFromReq);
                }
            } else {
                liferayCreatedOrUpdated = updateLiferayUser(request, liferayUserByEmailAddress, givenNameFromReq,
                        lastNameFromReq, emailFromReq, departmentFromReq, externalIdFromReq, pwdFromReq,
                        primaryRoleFromReq);
            }
        } else {
            try {
                ArrayList<UserCSV> users = new ArrayList<UserCSV>();
                UserCSV userCSVToCreateOrg = new UserCSV();
                userCSVToCreateOrg.setDepartment(departmentFromReq);
                users.add(userCSVToCreateOrg);
                createOrganizations(request, users);
            } catch (SystemException | PortalException e) {
                log.error("Error creating organizations " + departmentFromReq, e);
                throw new SW360Exception("Error creating organizations");
            }

            if (isLiferayUserNew) {
                userCSV.setGivenname(givenNameFromReq);
                userCSV.setDepartment(departmentFromReq);
                userCSV.setEmail(emailFromReq);
                userCSV.setGid(externalIdFromReq);
                userCSV.setGroup(primaryRoleFromReq);
                userCSV.setLastname(lastNameFromReq);

                if (!primaryRoleFromReq.equalsIgnoreCase(UserGroup.ADMIN.name())
                        && !primaryRoleFromReq.equalsIgnoreCase(UserGroup.USER.name())) {
                    userCSV.setGroup(UserGroup.USER.name());
                }
                if (CommonUtils.isNotNullEmptyOrWhitespace(pwdFromReq)) {
                    String encryptedPwd = PasswordEncryptorUtil.encrypt(pwdFromReq);
                    userCSV.setHash(encryptedPwd);
                }

                userCSV.setMale(false);
                liferayCreatedOrUpdated = dealWithUser(request, userCSV);
                if (liferayCreatedOrUpdated == null) {
                    log.error("Error while creating the user : " + emailFromReq);
                    throw new SW360Exception("Error while creating the user : " + emailFromReq);
                }

                if (!primaryRoleFromReq.equalsIgnoreCase(UserGroup.ADMIN.name())
                        && !primaryRoleFromReq.equalsIgnoreCase(UserGroup.USER.name())) {
                    liferayCreatedOrUpdated = updateLiferayUser(request, liferayCreatedOrUpdated, givenNameFromReq, lastNameFromReq, emailFromReq,
                            departmentFromReq, externalIdFromReq, pwdFromReq, primaryRoleFromReq);
                }

                org.eclipse.sw360.datahandler.thrift.users.User newlyCreatedUser = userClient.getByEmail(emailFromReq);
                newlyCreatedUser.setSecondaryDepartmentsAndRoles(secondaryDepartmentAndRolesMapFromRequest)
                .setOidcClientInfos(userAccessFromRequest)
                .setFullname(liferayCreatedOrUpdated.getFullName())
                        .setPrimaryRoles(null).setUserGroup(userGroupFromString(primaryRoleFromReq));
                userClient.updateUser(newlyCreatedUser);
                response.setRenderParameter(PortalConstants.USER_EMAIL, emailFromReq);
                return false;
            } else {
                liferayCreatedOrUpdated = updateLiferayUser(request, liferayUserByEmailAddress, givenNameFromReq, lastNameFromReq, emailFromReq,
                        departmentFromReq, externalIdFromReq, pwdFromReq, primaryRoleFromReq);
            }
        }

        userByEmailFromCouchDB.setGivenname(givenNameFromReq).setLastname(lastNameFromReq)
                .setDepartment(departmentFromReq).setExternalid(externalIdFromReq)
                .setFullname(liferayCreatedOrUpdated.getFullName()).setUserGroup(UserGroup.valueOf(primaryRoleFromReq))
                .setPrimaryRoles(null);
        userByEmailFromCouchDB.setSecondaryDepartmentsAndRoles(secondaryDepartmentAndRolesMapFromRequest)
        .setOidcClientInfos(userAccessFromRequest);
        if (isCouchDBUserNew || userByEmailFromCouchDB.getId() == null) {
            userClient.addUser(userByEmailFromCouchDB.setEmail(emailFromReq));
        } else {
            if (!userByEmailFromCouchDB.getEmail().equals(emailFromReq)) {
                userByEmailFromCouchDB.setFormerEmailAddresses(
                        UserUtils.prepareFormerEmailAddresses(userByEmailFromCouchDB, emailFromReq));
            }
            userClient.updateUser(userByEmailFromCouchDB.setEmail(emailFromReq));
        }

        if (originalEmail.equals(requestingUser.getEmail())) {
            HttpServletRequest httprequest = PortalUtil.getHttpServletRequest(request);
            httprequest.getSession().invalidate();
            ThemeDisplay themeDisplay = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
            response.sendRedirect(themeDisplay.getURLSignOut());
            return true;
        } else {
            response.setRenderParameter(PortalConstants.USER_EMAIL, emailFromReq);
        }

        return false;
    }

    private void prepareUserEdit(RenderRequest request) {
        String email = request.getParameter(PortalConstants.USER_EMAIL);
        Set<String> setOfDepartments = new TreeSet<String>();
        try {
            UserService.Iface client = thriftClients.makeUserClient();
            setOfDepartments = new TreeSet<String>(CommonUtils.nullToEmptySet(client.getUserDepartments()));
        } catch (TException e) {
            log.error("Problem getting departments of all user.", e);
        }

        if (email != null) {
            fetchUsersFromDBAndSetRequestAttributes(request, email);
        } else if(request.getAttribute(PortalConstants.USER_OBJ) == null){
            request.setAttribute(PortalConstants.USER_OBJ, new org.eclipse.sw360.datahandler.thrift.users.User());
        }
        request.setAttribute("grpsKeys", setOfDepartments);
        generateSecondaryRolesOption(request);
    }

    @Override
    public void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        String pageName = request.getParameter(PAGENAME);
        if (PAGENAME_EDIT.equals(pageName)) {
            prepareUserEdit(request);
            include("/html/admin/user/edit.jsp", request, response);
        } else if (PAGENAME_DETAIL.equals(pageName)) {
            prepareUserDetails(request);
            include("/html/admin/user/detail.jsp", request, response);
        } else {
            prepareStandardView(request);
            super.doView(request, response);
        }
    }

    private void prepareStandardView(RenderRequest request) {
        Set<String> setOfDepartments = new TreeSet<String>();
        Set<String> userEmails = new HashSet<String>();
        try {
            UserService.Iface client = thriftClients.makeUserClient();
            setOfDepartments = new TreeSet(CommonUtils.nullToEmptySet(client.getUserDepartments()));
            userEmails = client.getUserEmails();
        } catch (TException e) {
            log.error("Problem getting departments of all user.", e);
        }

        request.setAttribute(PortalConstants.SECONDARY_GROUPS_LIST, setOfDepartments);
        request.setAttribute(PortalConstants.COUCH_DB_USER_COUNT, userEmails.size());
        generateSecondaryRolesOption(request);
    }

    private void generateSecondaryRolesOption(RenderRequest request) {
        Set<UserGroup> listofUserGroupOptions = new TreeSet<UserGroup>(Comparator.comparing(UserGroup::name));

        Stream.of(UserGroup.values()).forEach(ug -> {
            if (SET_OF_USERGROUP_EXLUDED.contains(ug)) {
                return;
            }
            listofUserGroupOptions.add(ug);
        });

        request.setAttribute(PortalConstants.SECONDARY_ROLES_OPTIONS, listofUserGroupOptions);
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
        } else if (PortalConstants.EDIT_SECONDARY_GROUP_FOR_USER.equals(action)) {
            editSecondaryGroupAndRolesForUser(request, response);
        } else if (PortalConstants.USERS_PRESENT_IN_COUCH_DB.equals(action)) {
            serveUsersListPresentInCouchDB(request, response);
        } else if (PortalConstants.USERS_ABSENT_IN_COUCH_DB.equals(action)) {
            serveUsersListAbsentInCouchDB(request, response);
        }
    }

    private void serveUsersListAbsentInCouchDB(ResourceRequest request, ResourceResponse response) {
        Set<String> setOfEmailOfUsersPresentInCouchDB = new TreeSet<String>();
        try {
            UserService.Iface client = thriftClients.makeUserClient();
            setOfEmailOfUsersPresentInCouchDB = CommonUtils.nullToEmptySet(client.getUserEmails());
        } catch (TException e) {
            log.error("Problem getting departments/emails of all user.", e);
        }
        final Set<String> setOfEmailOfUsersPresentInCouchDBFinal = setOfEmailOfUsersPresentInCouchDB;
        List<org.eclipse.sw360.datahandler.thrift.users.User> usersAbsentInCouchDB;
        List<User> liferayUsers;
        try {
            liferayUsers = UserLocalServiceUtil.getUsers(QueryUtil.ALL_POS, QueryUtil.ALL_POS);
        } catch (SystemException e) {
            log.error("Could not get user List from liferay", e);
            liferayUsers = Collections.emptyList();
        }
        usersAbsentInCouchDB = liferayUsers.stream()
                .filter(liferayUser -> !setOfEmailOfUsersPresentInCouchDBFinal.contains(liferayUser.getEmailAddress()))
                .filter(liferayUser -> {
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

                    return !(isNullOrEmpty(firstName) || isNullOrEmpty(lastName) || isNullOrEmpty(emailAddress)
                            || isNullOrEmpty(department) || isNullOrEmpty(userGroup) || isNullOrEmpty(gid)
                            || isNullOrEmpty(passwordHash));
                }).map(liferayUser -> {
                    String emailAddress = liferayUser.getEmailAddress();
                    String department = null;
                    List<String> primaryRoles = liferayUser.getRoles().stream().map(role -> role.getName())
                            .collect(Collectors.toList());
                    try {
                        department = liferayUser.getOrganizations().get(0).getName();
                    } catch (PortalException pe) {
                        log.error("Error occured while retrieving Organisation name of the user. ", pe);
                    }
                    return new org.eclipse.sw360.datahandler.thrift.users.User()
                            .setGivenname(liferayUser.getFirstName()).setLastname(liferayUser.getLastName())
                            .setDepartment(department).setPrimaryRoles(primaryRoles).setEmail(emailAddress);
                }).collect(Collectors.toList());

        JSONArray userRequestData = createJSONArray();
        for (org.eclipse.sw360.datahandler.thrift.users.User user : usersAbsentInCouchDB) {
            createUserJsonObject(user, userRequestData, true);
        }
        JSONObject jsonResult = createJSONObject();
        jsonResult.put(DATATABLE_RECORDS_TOTAL, usersAbsentInCouchDB.size());
        jsonResult.put(DATATABLE_RECORDS_FILTERED, usersAbsentInCouchDB.size());
        jsonResult.put(DATATABLE_DISPLAY_DATA, userRequestData);

        try {
            writeJSON(request, response, jsonResult);
        } catch (IOException e) {
            log.error("Problem rendering list of users absent in couch db", e);
        }
    }

    private void serveUsersListPresentInCouchDB(ResourceRequest request, ResourceResponse response) {
        HttpServletRequest originalServletRequest = PortalUtil
                .getOriginalServletRequest(PortalUtil.getHttpServletRequest(request));
        PaginationParameters paginationParameters = PaginationParser.parametersFrom(originalServletRequest);
        PortletUtils.handlePaginationSortOrder(request, paginationParameters, USER_FILTERED_FIELDS, USER_NO_SORT);
        UserService.Iface client = thriftClients.makeUserClient();
        PaginationData pageData = new PaginationData();
        pageData.setRowsPerPage(paginationParameters.getDisplayLength());
        pageData.setDisplayStart(paginationParameters.getDisplayStart());
        pageData.setAscending(paginationParameters.isAscending().get());
        if (paginationParameters.getSortingColumn().isPresent()) {
            int sortParam = paginationParameters.getSortingColumn().get();
            if (sortParam == 0 && Integer.valueOf(paginationParameters.getEcho()) == 1) {
                pageData.setSortColumnNumber(-1);
            } else {
                pageData.setSortColumnNumber(paginationParameters.getSortingColumn().get());
            }
        } else {
            pageData.setSortColumnNumber(-1);
        }
        Map<String, Set<String>> filterMap = getUserFilterMap(originalServletRequest);
        Map<PaginationData, List<org.eclipse.sw360.datahandler.thrift.users.User>> usersWithPageData = getFilteredUsersList(
                request, pageData, client, filterMap);
        List<org.eclipse.sw360.datahandler.thrift.users.User> users = new ArrayList<>();
        PaginationData pgDt = new PaginationData();
        if (!CommonUtils.isNullOrEmptyMap(usersWithPageData)) {
            users = usersWithPageData.values().iterator().next();
            pgDt = usersWithPageData.keySet().iterator().next();
        }
        JSONArray jsonUsers = getUserData(users, paginationParameters, filterMap);
        JSONObject jsonResult = createJSONObject();
        jsonResult.put(DATATABLE_RECORDS_TOTAL, pgDt.getTotalRowCount());
        jsonResult.put(DATATABLE_RECORDS_FILTERED, pgDt.getTotalRowCount());
        jsonResult.put(DATATABLE_DISPLAY_DATA, jsonUsers);
        try {
            writeJSON(request, response, jsonResult);
        } catch (IOException e) {
            log.error("Problem rendering list of users present in couch db", e);
        }
    }

    private JSONArray getUserData(List<org.eclipse.sw360.datahandler.thrift.users.User> userList,
            PaginationParameters paginationParameters, Map<String, Set<String>> filterMap) {
        JSONArray userRequestData = createJSONArray();
        final int start = filterMap.isEmpty() ? 0 : paginationParameters.getDisplayStart();
        int upperLimit = paginationParameters.getDisplayLength() == -1 ? userList.size()
                : start + paginationParameters.getDisplayLength();
        for (int i = start; i < upperLimit; i++) {
            if (i == userList.size()) {
                break;
            }
            createUserJsonObject(userList.get(i), userRequestData, false);
        }

        return userRequestData;
    }

    private void createUserJsonObject(org.eclipse.sw360.datahandler.thrift.users.User user, JSONArray userRequestData,
            boolean isAbsentInCouchDB) {
        JSONObject jsonObject = JSONFactoryUtil.createJSONObject();
        jsonObject.put("id", user.getId());
        jsonObject.put("givenname", user.getGivenname());
        jsonObject.put("lastname", user.getLastname());
        jsonObject.put("email", user.getEmail());
        jsonObject.put("deactivated", user.isDeactivated() ? "Inactive" : "Active");
        jsonObject.put("primaryDepartment", user.getDepartment());
        if (isAbsentInCouchDB) {
            jsonObject.put("primaryDepartmentRole",
                    CommonUtils.nullToEmptyList(user.getPrimaryRoles()).stream().collect(Collectors.joining(", ")));
        } else {
            jsonObject.put("primaryDepartmentRole", ThriftEnumUtils.enumToString(user.getUserGroup()));
        }

        Map<String, Set<UserGroup>> secondaryDepartmentsAndRoles = user.getSecondaryDepartmentsAndRoles();
        String secondaryDepartmentsAndRolesStr = "";
        if (!CommonUtils.isNullOrEmptyMap(secondaryDepartmentsAndRoles)) {
            secondaryDepartmentsAndRolesStr = DisplayMapOfSecondaryGroupAndRoles
                    .getMapAsString(new TreeMap<String, Set<UserGroup>>(secondaryDepartmentsAndRoles));
        }

        jsonObject.put("secondaryDepartmentsAndRoles", secondaryDepartmentsAndRolesStr);
        userRequestData.put(jsonObject);
    }

    private Map<PaginationData, List<org.eclipse.sw360.datahandler.thrift.users.User>> getFilteredUsersList(
            PortletRequest request, PaginationData pageData, UserService.Iface client, Map<String, Set<String>> filterMap) {
        org.eclipse.sw360.datahandler.thrift.users.User user = UserCacheHolder.getUserFromRequest(request);
        Map<PaginationData, List<org.eclipse.sw360.datahandler.thrift.users.User>> usersWithPageData = Maps
                .newHashMap();
        try {
            if (filterMap.isEmpty()) {
                usersWithPageData = client.getUsersWithPagination(user, pageData);
                Set<String> userEmails = client.getUserEmails();
                usersWithPageData.keySet().iterator().next().setTotalRowCount(userEmails.size());
            } else {
                List<org.eclipse.sw360.datahandler.thrift.users.User> users = CommonUtils
                        .nullToEmptyList(client.refineSearch(null, filterMap));
                usersWithPageData.put(pageData.setTotalRowCount(users.size()), users);
            }
        } catch (TException e) {
            log.error("Could not fetch users from backend!", e);
        }
        return usersWithPageData;
    }

    private Map<String, Set<String>> getUserFilterMap(HttpServletRequest originalServletRequest) {
        Map<String, String[]> requestParam = originalServletRequest.getParameterMap();
        Map<String, Set<String>> filterMap = new HashMap<>();
        for (org.eclipse.sw360.datahandler.thrift.users.User._Fields filteredField : USER_FILTERED_FIELDS) {
            String[] paramValArr = requestParam.get(filteredField.toString());
            if (paramValArr == null) {
                continue;
            }
            String parameter = paramValArr[0];
            if (!isNullOrEmpty(parameter)
                    && !((filteredField.equals(org.eclipse.sw360.datahandler.thrift.users.User._Fields.USER_GROUP)
                            || filteredField.equals(org.eclipse.sw360.datahandler.thrift.users.User._Fields.DEPARTMENT))
                            && parameter.equals(PortalConstants.NO_FILTER))) {
                Set<String> values = CommonUtils.splitToSet(parameter);
                if (filteredField.equals(org.eclipse.sw360.datahandler.thrift.users.User._Fields.GIVENNAME)
                        || filteredField.equals(org.eclipse.sw360.datahandler.thrift.users.User._Fields.LASTNAME)
                        || filteredField.equals(org.eclipse.sw360.datahandler.thrift.users.User._Fields.EMAIL)) {
                    values = values.stream().map(LuceneAwareDatabaseConnector::prepareWildcardQuery)
                            .collect(Collectors.toSet());
                }
                filterMap.put(filteredField.getFieldName(), values);
            }
        }
        return filterMap;
    }

    private void editSecondaryGroupAndRolesForUser(ResourceRequest request, ResourceResponse response)
            throws UnsupportedEncodingException, IOException {
        StringBuilder reqBodySb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String readLine = null;
            while ((readLine = reader.readLine()) != null) {
                reqBodySb.append(readLine);
            }
        }
        Map<String, Object> reqBody = OBJECT_MAPPER.readValue(reqBodySb.toString(), Map.class);
        String email = reqBody.get("email").toString();
        Map<String, List<String>> formData = (Map) reqBody.get("formData");
        Map<String, Set<UserGroup>> secGroupAndRoles = formData.entrySet().stream()
                .collect(Collectors.toMap(Entry::getKey, entry -> {
                    List<String> rolesInString = entry.getValue();
                    return rolesInString.stream().map(role -> {
                        return UserGroup.valueOf(role);
                    }).collect(Collectors.toSet());
                }));

        UserService.Iface client = thriftClients.makeUserClient();
        RequestStatus updateUserStatus = null;
        try {
            org.eclipse.sw360.datahandler.thrift.users.User userByEmailToBeEdited = client.getByEmail(email);
            secGroupAndRoles.remove(userByEmailToBeEdited.getDepartment());
            userByEmailToBeEdited.setSecondaryDepartmentsAndRoles(secGroupAndRoles.isEmpty() ? null : secGroupAndRoles);
            updateUserStatus = client.updateUser(userByEmailToBeEdited);
        } catch (TException e) {
            log.error("Error occured while getting user and updating it.", e);
            response.setProperty(ResourceResponse.HTTP_STATUS_CODE,
                    Integer.toString(HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
            return;
        }
        if (updateUserStatus != RequestStatus.SUCCESS) {
            log.error("Error occured while getting user and updating it.");
            response.setProperty(ResourceResponse.HTTP_STATUS_CODE,
                    Integer.toString(HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
        }
        response.setProperty(ResourceResponse.HTTP_STATUS_CODE, Integer.toString(HttpServletResponse.SC_OK));
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
        Country country = CountryServiceUtil.getCountryByName(companyId, PortalConstants.DEFAULT_COUNTRY_NAME);
        for (String headDepartment : headDepartments) {

            long organizationId;
            try {
                organizationId = OrganizationLocalServiceUtil.getOrganizationId(companyId, headDepartment);
            } catch (SystemException e) {
                organizationId = 0;
            }

            if (organizationId == 0) { // The organization does not yet exist
                Organization organization = createOrganization(serviceContext, headDepartment, country.getCountryId(), OrganizationConstants.DEFAULT_PARENT_ORGANIZATION_ID);

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
                createOrganization(serviceContext, department, country.getCountryId(), organizationIds.get(extractHeadDept(department)).intValue());
            }
        }
    }

    private Organization createOrganization(ServiceContext serviceContext, String headDepartment, long countryId, int parentId) throws PortalException, SystemException {
        return OrganizationServiceUtil.addOrganization(
                parentId,
                headDepartment,
                OrganizationConstants.TYPE_ORGANIZATION,
                RegionConstants.DEFAULT_REGION_ID,
                countryId,
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

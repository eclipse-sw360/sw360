/*
 * Copyright Siemens AG, 2014-2017. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.permissions;

import java.util.Properties;
import java.util.Set;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.Vulnerability;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.SPDXDocument;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.DocumentCreationInformation;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageInformation;

/**
 * Created by bodet on 16/02/15.
 *
 * @author cedric.bodet@tngtech.com
 * @author stefan.jaeger@evosoft.com
 * @author alex.borodin@evosoft.com
 * @author thomas.maier@evosoft.com
 */
public class PermissionUtils {

    public static final String PROPERTIES_FILE_PATH = "/sw360.properties";
    public static final boolean IS_COMPONENT_VISIBILITY_RESTRICTION_ENABLED;
    public static final boolean IS_ADMIN_PRIVATE_ACCESS_ENABLED;

    static {
        Properties props = CommonUtils.loadProperties(DatabaseSettings.class, PROPERTIES_FILE_PATH);
        IS_COMPONENT_VISIBILITY_RESTRICTION_ENABLED = Boolean.parseBoolean(
            System.getProperty("RunComponentVisibilityRestrictionTest", props.getProperty("component.visibility.restriction.enabled", "false")));
        IS_ADMIN_PRIVATE_ACCESS_ENABLED = Boolean.parseBoolean(
            System.getProperty("RunPrivateProjectAccessTest", props.getProperty("admin.private.project.access.enabled", "false")));
    }

    public static boolean isNormalUser(User user) {
        return isInGroup(user, UserGroup.USER);
    }

    public static boolean isNormalUserBySecondaryRoles(Set<UserGroup> roles) {
        return roles.contains(UserGroup.USER);
    }

    public static boolean isAdmin(User user) {
        return isInGroup(user, UserGroup.SW360_ADMIN) || isInGroup(user, UserGroup.ADMIN);
    }

    public static boolean isAdminBySecondaryRoles(Set<UserGroup> roles) {
        return roles.contains(UserGroup.SW360_ADMIN) || roles.contains(UserGroup.ADMIN);
    }

    public static boolean isClearingAdmin(User user) {
        return isInGroup(user, UserGroup.CLEARING_ADMIN) || isInGroup(user, UserGroup.CLEARING_EXPERT);
    }

    public static boolean isClearingAdminBySecondaryRoles(Set<UserGroup> roles) {
        return roles.contains(UserGroup.CLEARING_ADMIN) || roles.contains(UserGroup.CLEARING_EXPERT);
    }

    public static boolean isClearingExpert(User user) {
        return isInGroup(user, UserGroup.CLEARING_EXPERT);
    }

    public static boolean isClearingExpertBySecondaryRoles(Set<UserGroup> roles) {
        return roles.contains(UserGroup.CLEARING_EXPERT);
    }

    public static boolean isEccAdmin(User user) {
        return isInGroup(user, UserGroup.ECC_ADMIN);
    }

    public static boolean isEccAdminBySecondaryRoles(Set<UserGroup> roles) {
        return roles.contains(UserGroup.ECC_ADMIN);
    }

    public static boolean isSecurityAdmin(User user) {
        return isInGroup(user, UserGroup.SECURITY_ADMIN);
    }

    public static boolean isSecurityAdminBySecondaryRoles(Set<UserGroup> roles) {
        return roles.contains(UserGroup.SECURITY_ADMIN);
    }

    private static boolean isInGroup(User user, UserGroup userGroup) {
        return user != null && user.isSetUserGroup() && user.getUserGroup() == userGroup;
    }

    public static boolean isUserAtLeast(UserGroup group, User user) {
        switch (group) {
            case USER:
                return isNormalUser(user) || isAdmin(user) || isClearingAdmin(user) || isEccAdmin(user) || isSecurityAdmin(user);
            case CLEARING_ADMIN:
                return isClearingAdmin(user) || isAdmin(user);
            case CLEARING_EXPERT:
                return isClearingExpert(user) || isAdmin(user);
            case ECC_ADMIN:
                return isEccAdmin(user) || isAdmin(user);
            case SECURITY_ADMIN:
                return isSecurityAdmin(user) || isAdmin(user);
            case SW360_ADMIN:
                return isAdmin(user);
            case ADMIN:
                return isAdmin(user);
            default:
                throw new IllegalArgumentException("Unknown group: " + group);
        }
    }

    public static boolean isUserAtLeastDesiredRoleInSecondaryGroup(UserGroup role, Set<UserGroup> secondaryRoles) {
        switch (role) {
            case USER:
                return isNormalUserBySecondaryRoles(secondaryRoles) || isAdminBySecondaryRoles(secondaryRoles) ||
                        isClearingAdminBySecondaryRoles(secondaryRoles) || isEccAdminBySecondaryRoles(secondaryRoles) || isSecurityAdminBySecondaryRoles(secondaryRoles);
            case CLEARING_ADMIN:
                return isClearingAdminBySecondaryRoles(secondaryRoles) || isAdminBySecondaryRoles(secondaryRoles);
            case CLEARING_EXPERT:
                return isClearingExpertBySecondaryRoles(secondaryRoles) || isAdminBySecondaryRoles(secondaryRoles);
            case ECC_ADMIN:
                return isEccAdminBySecondaryRoles(secondaryRoles) || isAdminBySecondaryRoles(secondaryRoles);
            case SECURITY_ADMIN:
                return isSecurityAdminBySecondaryRoles(secondaryRoles) || isAdminBySecondaryRoles(secondaryRoles);
            case SW360_ADMIN:
                return isAdminBySecondaryRoles(secondaryRoles);
            case ADMIN:
                return isAdminBySecondaryRoles(secondaryRoles);
            default:
                throw new IllegalArgumentException("Unknown role: " + role);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> DocumentPermissions<T> makePermission(T document, User user) {
        if (document instanceof License) {
            return (DocumentPermissions<T>) new LicensePermissions((License) document, user);
        } else if (document instanceof Component) {
            return (DocumentPermissions<T>) new ComponentPermissions((Component) document, user);
        } else if (document instanceof Release) {
            return (DocumentPermissions<T>) new ReleasePermissions((Release) document, user);
        } else if (document instanceof Project) {
            return (DocumentPermissions<T>) new ProjectPermissions((Project) document, user);
        } else if (document instanceof Vendor) {
            return (DocumentPermissions<T>) new VendorPermissions((Vendor) document, user);
        } else if (document instanceof User) {
            return (DocumentPermissions<T>) new UserPermissions((User) document, user);
        } else if (document instanceof Vulnerability) {
            return (DocumentPermissions<T>) new VulnerabilityPermissions((Vulnerability) document, user);
        } else if (document instanceof SPDXDocument) {
            return (DocumentPermissions<T>) new SpdxDocumentPermissions((SPDXDocument) document, user);
        } else if (document instanceof DocumentCreationInformation) {
            return (DocumentPermissions<T>) new SpdxDocumentCreationInfoPermissions((DocumentCreationInformation) document, user);
        } else if (document instanceof PackageInformation) {
            return (DocumentPermissions<T>) new SpdxPackageInfoPermissions((PackageInformation) document, user);
        } else {
            throw new IllegalArgumentException("Invalid input type!");
        }
    }

}

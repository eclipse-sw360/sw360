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

import java.util.*;

import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectClearingState;
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
    public static final Set<String> CLOSED_PROJECT_EDITABLE_PARAMS = Set.of(
            "enableSvm",
            "enableVulnerabilitiesDisplay",
            "projectManager",
            "projectOwner",
            "securityResponsibles",
            "externalIds",
            "state",
            "phaseOutSince"
    );

    public static final UserGroup DEFAULT_USER_GROUP = UserGroup.USER;

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

    public static boolean isSecurityUser(User user) {
        return isInGroup(user, UserGroup.SECURITY_USER);
    }

    public static boolean isSecurityAdminBySecondaryRoles(Set<UserGroup> roles) {
        return roles.contains(UserGroup.SECURITY_ADMIN);
    }

    private static boolean isInGroup(User user, UserGroup userGroup) {
        return user != null && user.isSetUserGroup() && user.getUserGroup() == userGroup;
    }

    public static boolean isUserAtLeastClearingAdminOrExpert(User user) {
        return isUserAtLeast(UserGroup.CLEARING_ADMIN, user) || isUserAtLeast(UserGroup.CLEARING_EXPERT, user);
    }

    public static boolean isUserAtLeast(UserGroup group, User user) {
        switch (group) {
            case USER:
                return isNormalUser(user) || isAdmin(user) || isClearingAdmin(user) || isEccAdmin(user) || isSecurityAdmin(user) || isSecurityUser(user);
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
            case SECURITY_USER:
                return isSecurityUser(user);
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

    public static boolean checkEditablePermission(String name, User user, Map<String, Object> reqBodyMap, Project sw360Project) {
        if (!name.equals(ProjectClearingState.CLOSED.name()) || PermissionUtils.isAdmin(user)) {
            return true;
        } else {
            if ((reqBodyMap.containsKey("attachments") || reqBodyMap.containsKey("obligationsText")
                    || reqBodyMap.containsKey("linkedObligationId")) && !PermissionUtils.isAdmin(user)) {
                return false;
            }
            String createdBy = sw360Project.getCreatedBy();
            String projectResponsible = sw360Project.getProjectResponsible();
            Set<String> projModerators = sw360Project.getModerators();
            Set<String> projContributors = sw360Project.getContributors();
            String leadArchitect = sw360Project.getLeadArchitect();
            Optional<String> match = CLOSED_PROJECT_EDITABLE_PARAMS.stream()
                    .filter(reqBodyMap::containsKey)
                    .findAny();
            if (match.isPresent() && (PermissionUtils.isAdmin(user)
                    || PermissionUtils.isClearingAdmin(user)
                    || user.getUserGroup().name().equalsIgnoreCase(UserGroup.CLEARING_EXPERT.name())
                    || PermissionUtils.isClearingExpert(user)) || user.getEmail().equals(createdBy)
                    || user.getEmail().equals(projectResponsible) || user.getEmail().equals(leadArchitect)
                    || projModerators.contains(user.getEmail()) || projContributors.contains(user.getEmail())) {
                return true;
            }
        }
        return false;
    }
}

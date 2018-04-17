/*
 * Copyright Siemens AG, 2014-2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.datahandler.permissions;

import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.Vulnerability;

/**
 * Created by bodet on 16/02/15.
 *
 * @author cedric.bodet@tngtech.com
 * @author stefan.jaeger@evosoft.com
 * @author alex.borodin@evosoft.com
 * @author thomas.maier@evosoft.com
 */
public class PermissionUtils {

    public static boolean isNormalUser(User user) {
        return isInGroup(user, UserGroup.USER);
    }

    public static boolean isAdmin(User user) {
        return isInGroup(user, UserGroup.SW360_ADMIN) || isInGroup(user, UserGroup.ADMIN);
    }

    public static boolean isClearingAdmin(User user) {
        return isInGroup(user, UserGroup.CLEARING_ADMIN);
    }

    public static boolean isEccAdmin(User user) {
        return isInGroup(user, UserGroup.ECC_ADMIN);
    }

    public static boolean isSecurityAdmin(User user) {
        return isInGroup(user, UserGroup.SECURITY_ADMIN);
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
        } else {
            throw new IllegalArgumentException("Invalid input type!");
        }
    }

}

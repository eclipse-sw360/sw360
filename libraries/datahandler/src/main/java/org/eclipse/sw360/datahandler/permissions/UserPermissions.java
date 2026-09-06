/*
 * Copyright Siemens AG, 2014-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.permissions;

import org.eclipse.sw360.datahandler.services.users.User;
import org.eclipse.sw360.datahandler.services.users.UserGroup;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Permissions for the user document. Actor may be thrift or POJO; both are
 * normalized to service-api role checks.
 *
 * @author cedric.bodet@tngtech.com
 */
public class UserPermissions extends DocumentPermissions<User> {

    private final User pojoActor;

    protected UserPermissions(User document, org.eclipse.sw360.datahandler.thrift.users.User user) {
        super(document, user);
        this.pojoActor = toPojoActor(user);
    }

    protected UserPermissions(User document, User actor) {
        super(document, toThriftActor(actor));
        this.pojoActor = actor;
    }

    @Override
    public void fillPermissions(User other, Map<RequestedAction, Boolean> permissions) {
        // User document has no permissions map (unlike License)
    }

    @Override
    public boolean isActionAllowed(RequestedAction action) {
        switch (action) {
            case READ:
                return true;
            case WRITE:
            case DELETE:
                return PermissionUtils.isUserAtLeast(UserGroup.ADMIN, pojoActor);
            default:
                return false;
        }
    }

    @Override
    protected Set<String> getContributors() {
        return Collections.emptySet();
    }

    @Override
    protected Set<String> getModerators() {
        return Collections.emptySet();
    }

    @Override
    protected Set<String> getSecurityResponsibles() {
        return Collections.emptySet();
    }

    private static User toPojoActor(org.eclipse.sw360.datahandler.thrift.users.User thrift) {
        if (thrift == null) {
            return null;
        }
        User pojo = new User().setEmail(thrift.getEmail());
        if (thrift.isSetUserGroup()) {
            pojo.setUserGroup(UserGroup.valueOf(thrift.getUserGroup().name()));
        }
        return pojo;
    }

    private static org.eclipse.sw360.datahandler.thrift.users.User toThriftActor(User pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.users.User thrift =
                new org.eclipse.sw360.datahandler.thrift.users.User();
        thrift.setEmail(pojo.getEmail());
        if (pojo.getUserGroup() != null) {
            thrift.setUserGroup(
                    org.eclipse.sw360.datahandler.thrift.users.UserGroup.valueOf(pojo.getUserGroup().name()));
        }
        return thrift;
    }
}

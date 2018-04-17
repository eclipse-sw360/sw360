/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal;

import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.portal.users.UserCacheHolder;

import javax.portlet.PortletRequest;

/**
 * Class to be used in tests to mock behavior of the original
 * {@link UserCacheHolder}.
 *
 * Use {@link #enable()} and {{@link #disable()} to activate or deactivate test
 * behavior for the {@link UserCacheHolder}.
 */
public class TestUserCacheHolder extends UserCacheHolder {

    protected User user = UserCacheHolder.EMPTY_USER;

    /**
     * Overrides the instance of the {@link UserCacheHolder} with the own instance.
     * See the other methods in this class to see for the new bahvior.
     */
    public void enable() {
        UserCacheHolder.instance = this;
    }

    /**
     * Overrides the instance of the {@link UserCacheHolder} with the own instance.
     * See the other methods in this class to see for the new bahvior.
     *
     * @param user
     *            a user that should be returned if the holder is asked for the
     *            current user
     */
    public void enable(User user) {
        this.user = user;
        UserCacheHolder.instance = this;
    }

    /**
     * Resets the instance of the {@link UserCacheHolder} to null in order to allow
     * the normal behavior.
     */
    public void disable() {
        UserCacheHolder.instance = null;
    }

    @Override
    /**
     * Returns an empty user regardless of the request.
     */
    protected User getCurrentUser(PortletRequest request) {
        return user;
    }
}

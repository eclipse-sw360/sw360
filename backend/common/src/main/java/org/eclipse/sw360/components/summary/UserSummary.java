/*
 * Copyright Siemens AG, 2014-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.components.summary;

import org.eclipse.sw360.datahandler.thrift.users.User;

/**
 * Created by jn on 16.03.15.
 *  @author johannes.najjar@tngtech.com
 */
public class UserSummary extends DocumentSummary<User> {

    @Override
    protected User summary(SummaryType type, User user) {
        return user;
    }
}

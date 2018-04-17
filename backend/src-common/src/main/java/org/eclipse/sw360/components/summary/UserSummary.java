/*
 * Copyright Siemens AG, 2014-2015. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

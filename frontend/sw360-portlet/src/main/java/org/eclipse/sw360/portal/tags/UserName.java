/*
 * Copyright Siemens AG, 2013-2016. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.tags;

import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.users.User;

/**
 * This prints user's display name
 *
 * @author alex.borodin@evosoft.com
 */
public class UserName extends OutTag {
    public void setUser(User user) {
        this.value = SW360Utils.printName(user);
    }
}

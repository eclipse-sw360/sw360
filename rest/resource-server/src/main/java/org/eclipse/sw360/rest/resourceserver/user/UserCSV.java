/*
 * Copyright Siemens AG, 2013-2016. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.user;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.csv.CSVRecord;
import org.jetbrains.annotations.NotNull;

/**
 * Created by heydenrb on 01.03.16.
 *
 * @author birgit.heydenreich@tngtech.com
 */
@Setter
@Getter
public class UserCSV {
    private String givenname;
    private String lastname;
    private String email;
    private String department;
    private String group;
    private String gid;
    private String hash;
    private boolean wantsMailNotification = true;

    public UserCSV(@NotNull CSVRecord record) {
        givenname = record.get(0);
        lastname = record.get(1);
        email = record.get(2);
        department = record.get(3);
        group = record.get(4);
        gid = record.get(5);
        hash = record.get(6);
        if (record.size() > 7) {
            wantsMailNotification = Boolean.parseBoolean((record.get(7)));
        }
    }

    public UserCSV() {
    }
}

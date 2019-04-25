/*
 * Copyright Siemens AG, 2013-2018. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.datahandler.db;

import org.eclipse.sw360.components.summary.UserSummary;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.couchdb.SummaryAwareRepository;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.ektorp.support.View;
import org.ektorp.support.Views;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * CRUD access for the User class
 *
 * @author cedric.bodet@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 * @author thomas.maier@evosoft.com
 */

@Views({
        @View(name = "all",
                map = "function(doc) { if (doc.type == 'user') emit(null, doc._id) }"),
        @View(name = "byExternalId",
                map = "function(doc) { if (doc.type == 'user' && doc.externalid) emit(doc.externalid.toLowerCase(), doc._id) }"),
        @View(name = "byApiToken",
                map = "function(doc) { if (doc.type == 'user') " +
                        "  for (var i in doc.restApiTokens) {" +
                        "    emit(doc.restApiTokens[i].token, doc._id)" +
                        "  }" +
                        "}"),
        @View(name = "byEmail",
                map = "function(doc) { " +
                        "  if (doc.type == 'user') {" +
                        "    emit(doc.email, doc._id); " +
                        "    if (doc.formerEmailAddresses && Array.isArray(doc.formerEmailAddresses)) {" +
                        "      var arr = doc.formerEmailAddresses;" +
                        "      for (var i = 0; i < arr.length; i++){" +
                        "        emit(arr[i], doc._id);" +
                        "      }" +
                        "    }" +
                        "  }" +
                        "}"),
})
public class UserRepository extends SummaryAwareRepository<User> {
    public UserRepository(DatabaseConnector databaseConnector) {
        super(User.class, databaseConnector, new UserSummary());
        initStandardDesignDocument();
    }

    @Override
    public List<User> get(Collection<String> ids) {
        return getConnector().get(User.class, ids, true);
    }

    public User getByExternalId(String externalId) {
        if(externalId == null || "".equals(externalId)) {
            // liferay contains the setup user with externalId=="" and we do not want to match him or any other one with empty externalID
            return null;
        }
        final Set<String> userIds = queryForIdsAsValue("byExternalId", externalId.toLowerCase());
        return getUserFromIds(userIds);
    }

    public User getByEmail(String email) {
        final Set<String> userIds = queryForIdsAsValue("byEmail", email);
        return getUserFromIds(userIds);
    }

    public User getByApiToken(String token) {
        final Set<String> userIds = queryForIdsAsValue("byApiToken", token);
        return getUserFromIds(userIds);
    }

    private User getUserFromIds(Set<String> userIds) {
        if (userIds != null && !userIds.isEmpty()) {
            return get(CommonUtils.getFirst(userIds));
        } else {
            return null;
        }
    }
}

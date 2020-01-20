/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.users;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserService;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * In-memory cache for user data
 *
 * @author cedric.bodet@tngtech.com
 */
public class UserCache {

    LoadingCache<String, User> cache;

    public UserCache() {
        // Initialize user loader
        UserLoader loader = new UserLoader();

        List<User> allUsers;
        try {
            allUsers = loader.getAllUsers();
        } catch (TException ignored) {
            allUsers= Collections.emptyList();
        }

        // Initialize user cache
        cache = CacheBuilder.newBuilder()
                .maximumSize(allUsers.size() + 100)
                .expireAfterWrite(1, TimeUnit.DAYS)
                .build(loader);

        if(!allUsers.isEmpty()) {
            cache.putAll(Maps.uniqueIndex(allUsers, User::getEmail));
        }
    }

    public User get(String email) throws ExecutionException {
        return cache.get(email);
    }

    public User getRefreshed (String email)  throws ExecutionException {
        cache.refresh(email);
        return cache.get(email);
    }

    private static class UserLoader extends CacheLoader<String, User> {

        private UserService.Iface createUserClient() {
            ThriftClients thriftClients =  new ThriftClients();
            return thriftClients.makeUserClient();
        }

        @Override
        public User load(String email) throws TException {
            UserService.Iface client = createUserClient();
            return client.getByEmail(email);
        }

        private List<User> getAllUsers() throws TException {
            UserService.Iface client = createUserClient();
            return client.getAllUsers();
        }
    }
}

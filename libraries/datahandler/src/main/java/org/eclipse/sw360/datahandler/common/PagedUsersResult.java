/*
 * Copyright Siemens AG, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.common;

import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.users.User;

import java.util.List;
import java.util.Map;

/**
 * REST-friendly wrapper replacing the awkward {@code Map<PaginationData, List<User>>}
 * return type used by the former Thrift user service pagination methods.
 *
 * <p>Used as the JSON body for paginated user responses between the
 * {@code backend/users} REST controller and the {@code resource-server}
 * REST client, removing the need for Thrift as a transport.
 */
public class PagedUsersResult {

    private PaginationData paginationData;
    private List<User> users;

    public PagedUsersResult() {
    }

    public PagedUsersResult(PaginationData paginationData, List<User> users) {
        this.paginationData = paginationData;
        this.users = users;
    }

    /**
     * Convenience factory that converts the {@code Map<PaginationData, List<User>>}
     * returned by the database handler layer into this DTO.
     */
    public static PagedUsersResult from(Map<PaginationData, List<User>> map) {
        if (map == null || map.isEmpty()) {
            return new PagedUsersResult(new PaginationData(), List.of());
        }
        Map.Entry<PaginationData, List<User>> entry = map.entrySet().iterator().next();
        return new PagedUsersResult(entry.getKey(), entry.getValue());
    }

    public PaginationData getPaginationData() {
        return paginationData;
    }

    public void setPaginationData(PaginationData paginationData) {
        this.paginationData = paginationData;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }
}

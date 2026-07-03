/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.users;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.sw360.common.utils.converter.common.AddDocumentRequestSummaryConverter;
import org.eclipse.sw360.common.utils.converter.common.PaginationDataConverter;
import org.eclipse.sw360.common.utils.converter.common.RequestStatusConverter;
import org.eclipse.sw360.common.utils.converter.common.RequestSummaryConverter;
import org.eclipse.sw360.common.utils.converter.users.UserConverter;
import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.RequestSummary;
import org.eclipse.sw360.datahandler.services.users.User;
import org.eclipse.sw360.datahandler.services.users.UserGroup;

final class UserRestMapper {

    private UserRestMapper() {}

    static org.eclipse.sw360.datahandler.thrift.PaginationData toThriftPagination(PaginationData pojo) {
        return PaginationDataConverter.toThrift(pojo);
    }

    static PaginationData fromThriftPagination(org.eclipse.sw360.datahandler.thrift.PaginationData thrift) {
        return PaginationDataConverter.fromThrift(thrift);
    }

    static User fromThriftUser(org.eclipse.sw360.datahandler.thrift.users.User thrift) {
        return UserConverter.fromThrift(thrift);
    }

    static org.eclipse.sw360.datahandler.thrift.users.User toThriftUser(User pojo) {
        return UserConverter.toThrift(pojo);
    }

    static List<User> fromThriftUsers(List<org.eclipse.sw360.datahandler.thrift.users.User> thriftList) {
        if (thriftList == null) {
            return List.of();
        }
        return thriftList.stream().map(UserConverter::fromThrift).collect(Collectors.toList());
    }

    static List<org.eclipse.sw360.datahandler.thrift.users.User> toThriftUsers(List<User> pojoList) {
        if (pojoList == null) {
            return List.of();
        }
        return pojoList.stream().map(UserConverter::toThrift).collect(Collectors.toList());
    }

    static org.eclipse.sw360.datahandler.thrift.users.UserGroup toThriftUserGroup(UserGroup pojo) {
        if (pojo == null) {
            return null;
        }
        return org.eclipse.sw360.datahandler.thrift.users.UserGroup.valueOf(pojo.name());
    }

    static AddDocumentRequestSummary fromThriftAddSummary(
            org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary thrift) {
        return AddDocumentRequestSummaryConverter.fromThrift(thrift);
    }

    static RequestStatus fromThriftRequestStatus(org.eclipse.sw360.datahandler.thrift.RequestStatus thrift) {
        return RequestStatusConverter.fromThrift(thrift);
    }

    static RequestSummary fromThriftRequestSummary(org.eclipse.sw360.datahandler.thrift.RequestSummary thrift) {
        return RequestSummaryConverter.fromThrift(thrift);
    }

    static Map<PaginationData, List<User>> fromThriftPaginatedUsers(
            Map<org.eclipse.sw360.datahandler.thrift.PaginationData, List<org.eclipse.sw360.datahandler.thrift.users.User>> thriftMap) {
        if (thriftMap == null || thriftMap.isEmpty()) {
            return Map.of();
        }
        Map.Entry<org.eclipse.sw360.datahandler.thrift.PaginationData, List<org.eclipse.sw360.datahandler.thrift.users.User>> entry =
                thriftMap.entrySet().iterator().next();
        return Map.of(fromThriftPagination(entry.getKey()), fromThriftUsers(entry.getValue()));
    }
}

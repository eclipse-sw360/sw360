/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.sw360.common.utils.converter.users.UserConverter;
import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.services.users.User;

/**
 * Helpers for test code to convert Thrift maps/lists into the service-api POJO
 * shape that {@code Sw360UserService} now returns.
 *
 * <p>The user service migration moved all {@code Sw360UserService} return
 * values to {@code org.eclipse.sw360.datahandler.services.users.User}; many
 * existing tests still build Thrift {@code User} instances for sharing with
 * other thrift-typed service mocks. This helper bridges the two at the stub
 * boundary without churning every test fixture.</p>
 */
public final class TestUserConverters {

    private TestUserConverters() {}

    /**
     * Convert a paginated map of thrift PaginationData → list of thrift Users
     * to the POJO equivalent used by {@code Sw360UserService}.
     */
    public static Map<PaginationData, List<User>> toPojoMap(
            Map<org.eclipse.sw360.datahandler.thrift.PaginationData,
                    List<org.eclipse.sw360.datahandler.thrift.users.User>> thriftMap) {
        if (thriftMap == null) {
            return Map.of();
        }
        return thriftMap.entrySet().stream().collect(Collectors.toMap(
                e -> toPojoPaginationData(e.getKey()),
                e -> e.getValue() == null
                        ? List.of()
                        : e.getValue().stream().map(UserConverter::fromThrift).toList(),
                (a, b) -> a,
                java.util.LinkedHashMap::new
        ));
    }

    private static PaginationData toPojoPaginationData(
            org.eclipse.sw360.datahandler.thrift.PaginationData thrift) {
        if (thrift == null) {
            return new PaginationData();
        }
        return new PaginationData()
                .setRowsPerPage(thrift.getRowsPerPage())
                .setDisplayStart(thrift.getDisplayStart())
                .setTotalRowCount(thrift.getTotalRowCount())
                .setSortColumnNumber(thrift.getSortColumnNumber())
                .setAscending(thrift.isAscending());
    }
}

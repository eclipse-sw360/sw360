/*
 * Copyright Siemens AG, 2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.common.datatables.data;


import java.util.Optional;

/**
 * @author daniele.fognini@tngtech.com
 */
public class DataTablesColumn {

    private final Optional<DataTablesSearch> search;

    public DataTablesColumn(DataTablesSearch search) {
        this.search = Optional.of(search);
    }

    public DataTablesSearch getSearch() {
        return search.get();
    }

    public boolean isSearchable() {
        return search.isPresent();
    }
}

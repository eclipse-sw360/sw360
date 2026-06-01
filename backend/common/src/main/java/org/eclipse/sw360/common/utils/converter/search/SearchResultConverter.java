/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.search;

import org.eclipse.sw360.datahandler.services.search.SearchResult;
import org.eclipse.sw360.common.utils.converter.common.ThriftCollectionConverter;

public final class SearchResultConverter {

    private SearchResultConverter() {}

    public static SearchResult fromThrift(org.eclipse.sw360.datahandler.thrift.search.SearchResult thrift) {
        if (thrift == null) {
            return null;
        }
        SearchResult pojo = new SearchResult();
        if (thrift.isSetDetails()) {
            pojo.setDetails(ThriftCollectionConverter.mapList(thrift.getDetails(), e -> org.eclipse.sw360.common.utils.converter.search.ResultDetailConverter.fromThrift(e)));
        }
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetName()) {
            pojo.setName(thrift.getName());
        }
        if (thrift.isSetScore()) {
            pojo.setScore(thrift.getScore());
        }
        if (thrift.isSetType()) {
            pojo.setType(thrift.getType());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.search.SearchResult toThrift(SearchResult pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.search.SearchResult thrift = new org.eclipse.sw360.datahandler.thrift.search.SearchResult();
        if (pojo.getDetails() != null) {
            thrift.setDetails(ThriftCollectionConverter.mapList(pojo.getDetails(), e -> org.eclipse.sw360.common.utils.converter.search.ResultDetailConverter.toThrift(e)));
        }
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getName() != null) {
            thrift.setName(pojo.getName());
        }
        if (pojo.getScore() != null) {
            thrift.setScore(pojo.getScore());
        }
        if (pojo.getType() != null) {
            thrift.setType(pojo.getType());
        }
        return thrift;
    }
}

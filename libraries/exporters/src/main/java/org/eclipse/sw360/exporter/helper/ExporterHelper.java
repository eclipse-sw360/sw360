/*
 * Copyright Siemens AG, 2014-2017. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.exporter.helper;

import com.google.common.collect.Lists;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.exporter.utils.SubTable;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by bodet on 06/02/15.
 *
 * @author cedric.bodet@tngtech.com
 */
public interface ExporterHelper<T> {

    static List<String> addSubheadersWithPrefixesAsNeeded(List<String> headers, List<String> subheaders, String prefix) {
        List<String> prefixedSubheaders = subheaders
                .stream()
                .map(h -> headers.contains(h) ? prefix + h : h)
                .collect(Collectors.toList());
        List<String> copy = Lists.newArrayList(headers);
        copy.addAll(prefixedSubheaders);
        return copy;
    }

    int getColumns();

    List<String> getHeaders();

    SubTable makeRows(T document) throws SW360Exception;

}

/*
 * Copyright Siemens AG, 2014-2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.exporter;

import com.google.common.collect.Lists;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;

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

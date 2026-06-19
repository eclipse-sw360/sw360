/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.common.utils.converter.common;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ThriftCollectionConverter {

    private ThriftCollectionConverter() {}

    public static <S, T> List<T> mapList(List<S> source, Function<S, T> mapper) {
        if (source == null) {
            return null;
        }
        return source.stream().map(mapper).collect(Collectors.toList());
    }

    public static <S, T> Set<T> mapSet(Set<S> source, Function<S, T> mapper) {
        if (source == null) {
            return null;
        }
        return source.stream().map(mapper).collect(Collectors.toSet());
    }

    public static <SK, SV, TK, TV> Map<TK, TV> mapMap(
            Map<SK, SV> source,
            Function<SK, TK> keyMapper,
            Function<SV, TV> valueMapper) {
        if (source == null) {
            return null;
        }
        Map<TK, TV> target = new HashMap<>();
        source.forEach((key, value) -> target.put(keyMapper.apply(key), valueMapper.apply(value)));
        return target;
    }
}

/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.licenseinfo.util;

import java.util.Collection;
import java.util.Map;

/**
 * Thrift {@code getXSize()} counted an unset field as 0. POJOs use null for unset.
 */
public final class PojoSizes {

    private PojoSizes() {}

    public static int size(Collection<?> values) {
        return values == null ? 0 : values.size();
    }

    public static int size(Map<?, ?> values) {
        return values == null ? 0 : values.size();
    }
}

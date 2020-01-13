/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.datahandler.common;

import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.components.RepositoryType;
import org.apache.thrift.TEnum;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class ThriftEnumUtilsTest {

    @Test
    public void testToString() {
        assertThat(ThriftEnumUtils.enumToString(AttachmentType.DESIGN), is("Design document"));
        assertThat(ThriftEnumUtils.enumToString(RepositoryType.GIT), is("Git"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAllMaps() throws Exception {
        for (Map.Entry<Class<? extends TEnum>, Map<? extends TEnum, String>> mapEntry : ThriftEnumUtils.MAP_ENUMTYPE_MAP.entrySet()) {
            Map<TEnum, String> value = (Map<TEnum, String>) mapEntry.getValue();
            testGenericMap((Class<TEnum>) mapEntry.getKey(), value);
        }
    }

    private <T extends TEnum> void testGenericMap(Class<T> type, Map<T, String> input) {
        for (T val : type.getEnumConstants()) {
            assertNotNull(type.getSimpleName() + "." + val.toString() + " [" + val.getValue() + "] has no string associated", input.get(val));
        }
    }
}
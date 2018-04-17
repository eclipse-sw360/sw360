/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.datahandler.couchdb;

import com.google.common.collect.ImmutableList;
import junit.framework.TestCase;
import org.apache.thrift.TBase;
import org.apache.thrift.TFieldIdEnum;

import java.lang.reflect.Field;

// test that the wrapper class W correctly wraps class C, which has fields C (F is always C._Fields)
public abstract class DocumentWrapperTest<W extends DocumentWrapper<C>, C extends TBase<C, F>, F extends Enum<F> & TFieldIdEnum> extends TestCase {
    private static final ImmutableList<String> NOT_COPIED_FIELDS = ImmutableList.of("id", "revision");

    protected void assertTFields(C source, W attachmentWrapper, Class<W> wrapperClass, Class<F> fieldClass) throws IllegalAccessException {
        for (F thriftField : fieldClass.getEnumConstants()) {
            final String sourceFieldName = thriftField.getFieldName();
            final Object sourceFieldValue = source.getFieldValue(thriftField);

            if (!NOT_COPIED_FIELDS.contains(sourceFieldName))
                assertNotNull("please set the field " + sourceFieldName + " in this test", sourceFieldValue);

            final Field field = getField(wrapperClass, sourceFieldName);
            assertNotNull("field " + sourceFieldName + " is not defined in " + wrapperClass.getName(), field);

            field.setAccessible(true);
            final Object copyFiledValue = field.get(attachmentWrapper);

            if (field.getType().isPrimitive()) {
                assertEquals(copyFiledValue, sourceFieldValue);
            } else {
                assertSame(copyFiledValue, sourceFieldValue);
            }
        }
    }

    private Field getField(Class<W> wrapperClass, String sourceFieldName) {
        return getField(wrapperClass, sourceFieldName, 0);
    }

    private Field getField(Class attachmentWrapperClass, String sourceFieldName, int depth) {
        if (attachmentWrapperClass == null) {
            return null;
        }
        try {
            return attachmentWrapperClass.getDeclaredField(sourceFieldName);
        } catch (NoSuchFieldException e) {
            final Class superclass = attachmentWrapperClass.getSuperclass();
            return getField(superclass, sourceFieldName, depth + 1);
        }
    }

}
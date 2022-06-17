/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.datahandler.test;

import org.eclipse.sw360.testthrift.TestObject;
import org.eclipse.sw360.testthrift.TestService;
import org.apache.thrift.TException;

/**
 * Created by bodet on 19/09/14.
 */
public class TestServiceHandler implements TestService.Iface {

    public static final String testText = "This is some nice text!";

    @Override
    public TestObject test(TestObject user) throws TException {
        TestObject copy = new TestObject(user);
        copy.setText(testText);
        return copy;
    }
}

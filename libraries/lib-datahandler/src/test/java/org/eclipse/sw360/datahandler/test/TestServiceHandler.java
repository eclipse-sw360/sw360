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

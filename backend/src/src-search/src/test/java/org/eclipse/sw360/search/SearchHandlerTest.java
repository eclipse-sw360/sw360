/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.search;

import org.apache.thrift.TException;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class SearchHandlerTest {

    SearchHandler handler;

    @Before
    public void setUp() throws Exception {
        handler = new SearchHandler();
    }

    @Test(expected = TException.class)
    public void testSearchNull() throws Exception {
        handler.search(null, null);
    }

    public void testSearchEmpty() throws Exception {
        assertThat(handler.search("", null).size(), is(0));
    }
}

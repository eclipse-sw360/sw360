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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SW360UtilsTest {

    @Test
    public void testGetBUFromOrganisation() throws Exception {
        assertEquals("CT BE OSS", SW360Utils.getBUFromOrganisation("CT BE OSS NE"));
        assertEquals("CT BE", SW360Utils.getBUFromOrganisation("CT BE"));
    }
}
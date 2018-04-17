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
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

package org.eclipse.sw360.importer;

import org.eclipse.sw360.commonIO.ConvertUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ConvertUtilTest {

    @Test
    public void testParseDate() throws Exception {
        String parsedDate = ConvertUtil.parseDate("CAST(0xC9370B00 AS Date)");
        assertEquals("2013-11-05", parsedDate);
    }
}
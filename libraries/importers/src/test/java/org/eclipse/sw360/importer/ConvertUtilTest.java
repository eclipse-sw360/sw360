/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
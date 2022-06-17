/*
 * Copyright Bosch Software Innovations GmbH, 2018.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.datahandler.resourcelists;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class PaginationResultTest {

    @Test
    public void createPaginationResult() {
        PaginationOptions paginationOptions = new PaginationOptions(0, 10, null);
        PaginationResult paginationResult = new PaginationResult(new ArrayList(), 23, paginationOptions);
        assertEquals(paginationResult.getTotalPageCount(), 3);
    }

}

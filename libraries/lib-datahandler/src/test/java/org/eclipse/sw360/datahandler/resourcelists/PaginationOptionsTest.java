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

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class PaginationOptionsTest {

    @Test
    public void createPaginationOptionsFirstPage() {
        PaginationOptions paginationOptions = new PaginationOptions(0, 10, null);
        assertEquals(paginationOptions.getOffset(), 0);
    }

    @Test
    public void createPaginationOptionsSecondPage() {
        PaginationOptions paginationOptions = new PaginationOptions(1, 10, null);
        assertEquals(paginationOptions.getOffset(), 10);
    }

}

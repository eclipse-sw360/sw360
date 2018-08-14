/*
 * Copyright Bosch Software Innovations GmbH, 2018.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

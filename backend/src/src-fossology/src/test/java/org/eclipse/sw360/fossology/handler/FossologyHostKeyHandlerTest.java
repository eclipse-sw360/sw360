/*
 * Copyright Siemens AG, 2015. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.fossology.handler;

import org.eclipse.sw360.datahandler.thrift.fossology.FossologyHostFingerPrint;
import org.eclipse.sw360.fossology.db.FossologyFingerPrintRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FossologyHostKeyHandlerTest {

    FossologyHostKeyHandler fossologyHostKeyHandler;

    @Mock
    FossologyFingerPrintRepository fossologyHostKeyConnector;

    @Before
    public void setUp() {
        fossologyHostKeyHandler = new FossologyHostKeyHandler(fossologyHostKeyConnector);
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(fossologyHostKeyConnector);
    }

    @Test
    public void testGetFingerPrints() throws Exception {
        List<FossologyHostFingerPrint> fingerPrints = Collections.emptyList();
        when(fossologyHostKeyConnector.getAll()).thenReturn(fingerPrints);

        assertThat(fossologyHostKeyHandler.getFingerPrints(), sameInstance(fingerPrints));

        verify(fossologyHostKeyConnector).getAll();
    }

    @Test
    public void testGetFingerPrintsReturnEmptyOnFailure() throws Exception {

        when(fossologyHostKeyConnector.getAll()).thenReturn(null);

        assertThat(fossologyHostKeyHandler.getFingerPrints(),
                is(emptyCollectionOf(FossologyHostFingerPrint.class)));

        verify(fossologyHostKeyConnector).getAll();
    }

    @Test
    public void testSetFingerPrints() throws Exception {
        List<FossologyHostFingerPrint> fingerPrints = Collections.emptyList();
        fossologyHostKeyHandler.setFingerPrints(fingerPrints);

        verify(fossologyHostKeyConnector).executeBulk(fingerPrints);
    }
}
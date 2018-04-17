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

import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.fossology.ssh.FossologyUploader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.InputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FossologyScriptsHandlerTest {

    FossologyScriptsHandler fossologyScriptsHandler;

    @Mock
    FossologyUploader fossologyUploader;

    @Before
    public void setUp() throws Exception {
        fossologyScriptsHandler = new FossologyScriptsHandler(fossologyUploader);
    }

    @After
    public void tearDown() throws Exception {
        verifyNoMoreInteractions(fossologyUploader);
    }

    @Test
    public void testDeployScriptsCanOpenAllResourceFiles() throws Exception {
        when(fossologyUploader.copyToFossology(anyString(), any(InputStream.class), anyBoolean())).thenReturn(true);
        assertThat(fossologyScriptsHandler.deployScripts(), is(RequestStatus.SUCCESS));

        verify(fossologyUploader, times(5)).copyToFossology(anyString(), any(InputStream.class), anyBoolean());

    }

    @Test
    public void testDeployScriptsReportsErrorOnUploadError() throws Exception {
        when(fossologyUploader.copyToFossology(anyString(), any(InputStream.class), anyBoolean())).thenReturn(true, false);
        assertThat(fossologyScriptsHandler.deployScripts(), is(RequestStatus.FAILURE));

        verify(fossologyUploader, times(5)).copyToFossology(anyString(), any(InputStream.class), anyBoolean());

    }
}
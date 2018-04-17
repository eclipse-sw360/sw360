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
package org.eclipse.sw360.fossology.ssh;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Session;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.fossology.config.FossologySettings;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.io.InputStream;
import java.io.OutputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FossologySshConnectorTest {

    FossologySshConnector fossologySshConnector;

    @Mock
    private FossologySettings fossologySettings;
    @Mock
    private JSchSessionProvider jSchSessionProvider;

    @Mock
    private Session session;

    @Mock
    private ChannelExec channel;


    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private int connectionTimeout = 10000;
    private long executionTimeout = 203004;

    @Before
    public void setUp() throws Exception {
        when(jSchSessionProvider.getSession(connectionTimeout)).thenReturn(session);
        when(session.openChannel(eq("exec"))).thenReturn(channel);

        when(channel.isConnected()).thenReturn(true);
        when(session.isConnected()).thenReturn(true);

        when(fossologySettings.getFossologyConnectionTimeout()).thenReturn(connectionTimeout);
        when(fossologySettings.getFossologyExecutionTimeout()).thenReturn(executionTimeout);

        fossologySshConnector = new FossologySshConnector(jSchSessionProvider, fossologySettings);

        fossologySshConnector = spy(fossologySshConnector);
        doNothing().when(fossologySshConnector).waitCompletion(channel, executionTimeout);
    }

    @After
    public void tearDown() {
        verify(fossologySettings).getFossologyConnectionTimeout();
        verify(fossologySettings).getFossologyExecutionTimeout();

        verifyNoMoreInteractions(jSchSessionProvider, fossologySettings);
    }

    @Test
    public void testRunInFossologyViaSsh() throws Exception {
        final int exitCode = fossologySshConnector.runInFossologyViaSsh("cmd");

        assertThat(exitCode, greaterThanOrEqualTo(0));

        verify(jSchSessionProvider).getSession(connectionTimeout);

        verify(jSchSessionProvider, atLeastOnce()).closeSession(session); // isConnected is mocked: it is disconnected twice
        verify(channel, atLeastOnce()).disconnect(); // isConnected is mocked: it is disconnected twice
    }

    @Test
    public void testRunInFossologyViaSsh1() throws Exception {
        InputStream stdin = mock(InputStream.class);
        final int exitCode = fossologySshConnector.runInFossologyViaSsh("cmd", stdin);

        assertThat(exitCode, greaterThanOrEqualTo(0));

        verify(jSchSessionProvider).getSession(connectionTimeout);

        verify(jSchSessionProvider, atLeastOnce()).closeSession(any(Session.class)); // isConnected is mocked: it is disconnected twice
        verify(channel, atLeastOnce()).disconnect(); // isConnected is mocked: it is disconnected twice
    }

    @Test
    public void testRunInFossologyViaSsh2() throws Exception {
        InputStream stdin = mock(InputStream.class);
        OutputStream stdout = mock(OutputStream.class);


        final int exitCode = fossologySshConnector.runInFossologyViaSsh("cmd", stdin, stdout);

        assertThat(exitCode, greaterThanOrEqualTo(0));

        verify(jSchSessionProvider).getSession(connectionTimeout);

        verify(channel, atLeastOnce()).disconnect(); // isConnected is mocked: it is disconnected twice
        verify(jSchSessionProvider, atLeastOnce()).closeSession(session); // isConnected is mocked: it is disconnected twice
    }

    @Test
    public void testRunInFossologyViaSsh3() throws Exception {
        OutputStream stdout = mock(OutputStream.class);

        doAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                final Object[] arguments = invocation.getArguments();
                final InputStream inputStream = (InputStream) arguments[1];

                assertThat(inputStream.read(), is(-1));
                return 1;
            }
        }).when(fossologySshConnector).runInFossologyViaSsh(anyString(), any(InputStream.class), any(OutputStream.class));

        final int exitCode = fossologySshConnector.runInFossologyViaSsh("cmd", stdout);

        assertThat(exitCode, greaterThanOrEqualTo(0));

        verify(fossologySshConnector).runInFossologyViaSsh(eq("cmd"), any(InputStream.class), eq(stdout));
    }

    @Test
    public void testRunInFossologyViaSshReturnErrorOnTimeout() throws Exception {
        doThrow(new SW360Exception()).when(fossologySshConnector).waitCompletion(channel, executionTimeout);

        final int exitCode = fossologySshConnector.runInFossologyViaSsh("cmd");

        assertThat(exitCode, lessThan(0));

        verify(jSchSessionProvider).getSession(connectionTimeout);
        verify(jSchSessionProvider).getServerString();

        verify(channel, atLeastOnce()).disconnect(); // isConnected is mocked: it is disconnected twice
        verify(jSchSessionProvider, atLeastOnce()).closeSession(session); // isConnected is mocked: it is disconnected twice
    }

    @Test
    public void testWaitCompletion() throws SW360Exception {
        executionTimeout = 100;

        doCallRealMethod().when(fossologySshConnector).waitCompletion(channel, executionTimeout); // reset spy

        when(channel.isClosed()).thenReturn(true);
        fossologySshConnector.waitCompletion(channel, executionTimeout);
    }

    @Test
    public void testWaitCompletionThrowsOnTimeout() throws SW360Exception {
        executionTimeout = 100;

        doCallRealMethod().when(fossologySshConnector).waitCompletion(channel, executionTimeout); // reset spy

        expectedException.expect(SW360Exception.class);

        when(channel.isClosed()).thenReturn(false);
        fossologySshConnector.waitCompletion(channel, executionTimeout);
    }
}
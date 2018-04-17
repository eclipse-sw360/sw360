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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class ConcatClosingInputStreamTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testReadIsEmptyForEmptyIterator() throws Exception {

        ImmutableList<InputStream> inputStreams = ImmutableList.of();

        ConcatClosingInputStream concatClosingInputStream = new ConcatClosingInputStream(inputStreams.iterator());

        assertThat(concatClosingInputStream.read(), is(-1));
        assertThat(concatClosingInputStream.read(), is(-1));
        assertThat(concatClosingInputStream.read(), is(-1));
    }

    @Test
    public void testReadCorrectlyProxiesForASingle() throws Exception {
        InputStream a = mock(InputStream.class);
        when(a.read()).thenReturn(1, 2, -1);

        ImmutableList<InputStream> inputStreams = ImmutableList.of(a);

        ConcatClosingInputStream concatClosingInputStream = new ConcatClosingInputStream(inputStreams.iterator());

        assertThat(concatClosingInputStream.read(), is(1));
        assertThat(concatClosingInputStream.read(), is(2));
        assertThat(concatClosingInputStream.read(), is(-1));
        assertThat(concatClosingInputStream.read(), is(-1));
        assertThat(concatClosingInputStream.read(), is(-1));
    }

    @Test
    public void testReadCorrectlyProxies() throws Exception {
        InputStream a = mock(InputStream.class);
        when(a.read()).thenReturn(1, 2, -1);

        InputStream b = mock(InputStream.class);
        when(b.read()).thenReturn(3, 4, 5, -1);

        InputStream c = mock(InputStream.class);
        when(c.read()).thenReturn(-1);

        InputStream d = mock(InputStream.class);
        when(d.read()).thenReturn(6, -1);

        ImmutableList<InputStream> inputStreams = ImmutableList.of(a, b, c, d);

        ConcatClosingInputStream concatClosingInputStream = new ConcatClosingInputStream(inputStreams.iterator());

        assertThat(concatClosingInputStream.read(), is(1));
        assertThat(concatClosingInputStream.read(), is(2));
        assertThat(concatClosingInputStream.read(), is(3));
        verify(a).close();
        assertThat(concatClosingInputStream.read(), is(4));
        assertThat(concatClosingInputStream.read(), is(5));
        assertThat(concatClosingInputStream.read(), is(6));
        verify(b).close();
        verify(c).close();
        assertThat(concatClosingInputStream.read(), is(-1));
        verify(d).close();
        assertThat(concatClosingInputStream.read(), is(-1));
        assertThat(concatClosingInputStream.read(), is(-1));
    }

    @Test
    public void testCloseDoesNotThrowForEmptyIterator() throws Exception {
        ImmutableList<InputStream> inputStreams = ImmutableList.of();

        ConcatClosingInputStream concatClosingInputStream = new ConcatClosingInputStream(inputStreams.iterator());

        concatClosingInputStream.close();
    }

    @Test
    public void testCloseClosesUnderlyingStreamForASingle() throws Exception {
        InputStream a = mock(InputStream.class);
        ImmutableList<InputStream> inputStreams = ImmutableList.of(a);

        ConcatClosingInputStream concatClosingInputStream = new ConcatClosingInputStream(inputStreams.iterator());

        concatClosingInputStream.close();

        verify(a).close();
    }

    @Test
    public void testCloseClosesUnderlyingStreams() throws Exception {
        InputStream a = mock(InputStream.class);
        InputStream b = mock(InputStream.class);
        InputStream c = mock(InputStream.class);

        ImmutableList<InputStream> inputStreams = ImmutableList.of(a, b, c);

        ConcatClosingInputStream concatClosingInputStream = new ConcatClosingInputStream(inputStreams.iterator());

        concatClosingInputStream.close();

        verify(a).close();
        verify(b).close();
        verify(c).close();
    }

    @Test
    public void testExceptionSafetyAndAutoClosing() throws Exception {
        IOException readException = new IOException("stream read exception");
        IOException closingException = new IOException("stream close exception");

        InputStream a = mock(InputStream.class);
        when(a.read()).thenReturn(1, 2, -1);

        InputStream b = mock(InputStream.class);
        when(b.read()).thenReturn(3, 4, 5).thenThrow(readException);
        doThrow(closingException).when(b).close();

        InputStream c = mock(InputStream.class);
        when(c.read()).thenReturn(-1);

        InputStream d = mock(InputStream.class);
        when(d.read()).thenReturn(6, -1);

        ImmutableList<InputStream> inputStreams = ImmutableList.of(a, b, c, d);

        ConcatClosingInputStream concatClosingInputStream = new ConcatClosingInputStream(inputStreams.iterator());

        List<Integer> reads = Lists.newArrayList();
        try {
            int read = concatClosingInputStream.read();
            while (read >= 0) {
                reads.add(read);
                read = concatClosingInputStream.read();
            }
            reads.add(read);
        } catch (IOException e) {
            assertSame(readException, e);
            assertThat(e.getSuppressed(), is(arrayContaining((Throwable) closingException)));
        }

        assertThat(reads, contains(1, 2, 3, 4, 5));

        verify(a, times(3)).read();
        verify(b, times(4)).read();

        verify(a).close();
        verify(b).close();
        verify(c).close();
        verify(d).close();

        try {
            concatClosingInputStream.close();
        } catch (Exception e) {
            fail("trying to close again did not ignore it");
        }
        // verify that an extra close does no harm
        verifyNoMoreInteractions(a, b, c, d);
    }

    @Test
    public void testReportingFirstExceptionAndSuppressingLater() throws Exception {
        IOException readException = new IOException("stream read exception");
        IOException closingException = new IOException("stream close exception");
        IOException closingException2 = new IOException("stream close exception for stream c");
        IOException closingException3 = new IOException("stream close exception for stream d");

        InputStream a = mock(InputStream.class);
        when(a.read()).thenReturn(1, 2, -1);

        InputStream b = mock(InputStream.class);
        when(b.read()).thenReturn(3, 4, 5).thenThrow(readException);
        doThrow(closingException).when(b).close();

        InputStream c = mock(InputStream.class);
        doThrow(closingException2).when(c).close();

        InputStream d = mock(InputStream.class);
        doThrow(closingException3).when(d).close();

        ImmutableList<InputStream> inputStreams = ImmutableList.of(a, b, c, d);

        ConcatClosingInputStream concatClosingInputStream = new ConcatClosingInputStream(inputStreams.iterator());

        List<Integer> reads = Lists.newArrayList();
        try {
            int read = concatClosingInputStream.read();
            while (read >= 0) {
                reads.add(read);
                read = concatClosingInputStream.read();
            }
            reads.add(read);
        } catch (IOException e) {
            assertSame(readException, e);
            // when reading we get the closing exception as suppressed
            assertThat(e.getSuppressed(), is(arrayContaining((Throwable) closingException)));
            // and the exception from the following streams are suppressed of the first
            assertThat(closingException.getSuppressed(),
                    is(arrayContaining((Throwable) closingException2, closingException3)));
        }

        assertThat(reads, contains(1, 2, 3, 4, 5));

        verify(a, times(3)).read();
        verify(b, times(4)).read();

        verify(a).close();
        verify(b).close();
        verify(c).close();
        verify(d).close();

        verifyNoMoreInteractions(a, b, c, d);
    }

    @Test
    public void testReportingFirstExceptionAndSuppressingLaterOnClose() throws Exception {
        IOException closingException = new IOException("stream close exception");
        IOException closingException2 = new IOException("stream close exception for stream c");
        IOException closingException3 = new IOException("stream close exception for stream d");

        InputStream a = mock(InputStream.class);

        InputStream b = mock(InputStream.class);
        doThrow(closingException).when(b).close();

        InputStream c = mock(InputStream.class);
        doThrow(closingException2).when(c).close();

        InputStream d = mock(InputStream.class);
        doThrow(closingException3).when(d).close();

        ImmutableList<InputStream> inputStreams = ImmutableList.of(a, b, c, d);

        ConcatClosingInputStream concatClosingInputStream = new ConcatClosingInputStream(inputStreams.iterator());

        try {
            concatClosingInputStream.close();
        } catch (IOException e) {
            assertSame(closingException, e);
            // when reading we get the closing exception as suppressed
            assertThat(e.getSuppressed(),
                    is(arrayContaining((Throwable) closingException2, closingException3)));
        }

        verify(a).close();
        verify(b).close();
        verify(c).close();
        verify(d).close();

        verifyNoMoreInteractions(a, b, c, d);
    }

    @Test
    public void testThrowingOnNullStreams() throws IOException {
        @SuppressWarnings("unchecked")
        Iterator<InputStream> inputStreams = mock(Iterator.class);

        when(inputStreams.hasNext()).thenReturn(true, false);
        when(inputStreams.next()).thenReturn(null);

        ConcatClosingInputStream concatClosingInputStream = new ConcatClosingInputStream(inputStreams);

        expectedException.expect(IOException.class);
        expectedException.expectMessage("cannot read from null Stream");
        @SuppressWarnings("unused")
        int unused = concatClosingInputStream.read();
        fail("expected exception");
    }

}
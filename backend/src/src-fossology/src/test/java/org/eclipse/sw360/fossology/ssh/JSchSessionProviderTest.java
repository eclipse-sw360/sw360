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

import com.jcraft.jsch.Session;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * @author daniele.fognini@tngtech.com
 */
public class JSchSessionProviderTest {

    private JSchSessionProvider jSchSessionProvider;

    @Before
    public void setUp() throws Exception {
        jSchSessionProvider = RealSshConnectionUtils.getTrustingJSchSessionFactory();
        RealSshConnectionUtils.assumeCanOpenSshSessions(jSchSessionProvider);
        jSchSessionProvider.destroy();
    }

    @Test
    public void testGetSessionRecyclesSessions() throws Exception {
        Session session = jSchSessionProvider.getSession(2000);

        assertThat(session.isConnected(), is(true));

        jSchSessionProvider.closeSession(session);

        Session newSession = jSchSessionProvider.getSession(1);

        assertThat(newSession, is(sameInstance(session)));
    }

    @Test
    public void testGetsNewSessionIfOtherIsClosed() throws Exception {
        Session session = jSchSessionProvider.getSession(2000);
        session.disconnect();

        jSchSessionProvider.closeSession(session);

        Session newSession = jSchSessionProvider.getSession(2000);

        assertThat(newSession.isConnected(), is(true));
        assertThat(newSession, is(not(sameInstance(session))));
    }

    @Test
    public void testConcurrentAlwaysManuallyClosing() throws Exception {
        final ExecutorService service = newFixedThreadPool(12);

        List<Future<Session>> futures = newArrayList();

        for (int i = 0; i < 12; i++) {
            Future<Session> future = service.submit(new Callable<Session>() {
                @Override
                public Session call() throws Exception {
                    Thread.sleep(ThreadLocalRandom.current().nextInt(1000));
                    Session session = jSchSessionProvider.getSession(2000);
                    session.disconnect();
                    jSchSessionProvider.closeSession(session);
                    return session;
                }
            });
            futures.add(future);
        }

        service.shutdown();
        service.awaitTermination(10, TimeUnit.SECONDS);

        Set<Session> sessionSet = newHashSet();
        for (Future<Session> future : futures) {
            sessionSet.add(future.get());
        }

        assertThat(sessionSet, hasSize(12));
    }

    @Test
    public void testConcurrentRecycling() throws Exception {
        final ExecutorService service = newFixedThreadPool(12);

        List<Future<Session>> futures = newArrayList();

        for (int i = 0; i < 12; i++) {
            Future<Session> future = service.submit(new Callable<Session>() {
                @Override
                public Session call() throws Exception {
                    Thread.sleep(ThreadLocalRandom.current().nextInt(1000));
                    Session session = jSchSessionProvider.getSession(2000);
                    jSchSessionProvider.closeSession(session);
                    return session;
                }
            });
            futures.add(future);
        }

        service.shutdown();
        service.awaitTermination(10, TimeUnit.SECONDS);

        Set<Session> sessionSet = newHashSet();
        for (Future<Session> future : futures) {
            sessionSet.add(future.get());
        }

        assertThat(sessionSet, hasSize(lessThan(12))); // assert that some session were recycled
    }
}
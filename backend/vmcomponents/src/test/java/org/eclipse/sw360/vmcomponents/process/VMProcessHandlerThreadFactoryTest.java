/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.vmcomponents.process;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.concurrent.ThreadFactory;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Verifies that {@link VMProcessHandler}'s worker threads are named with the
 * {@code sw360-vmprocessor-} prefix so they are identifiable in stack traces,
 * profilers, and Log4j2 thread-name patterns.
 */
public class VMProcessHandlerThreadFactoryTest {

    @Test
    public void namedThreadFactoryAssignsSw360VmprocessorPrefix() throws Exception {
        Method m = VMProcessHandler.class.getDeclaredMethod("namedThreadFactory");
        m.setAccessible(true);
        ThreadFactory factory = (ThreadFactory) m.invoke(null);

        Thread t1 = factory.newThread(() -> { /* no-op */ });
        Thread t2 = factory.newThread(() -> { /* no-op */ });

        assertNotNull(t1);
        assertNotNull(t2);
        assertTrue("Expected sw360-vmprocessor-* prefix but was: " + t1.getName(),
                t1.getName().startsWith("sw360-vmprocessor-"));
        assertTrue("Expected sw360-vmprocessor-* prefix but was: " + t2.getName(),
                t2.getName().startsWith("sw360-vmprocessor-"));
        assertNotEquals("Counter should produce distinct names",
                t1.getName(), t2.getName());
    }
}

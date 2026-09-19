/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.exporter;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/**
 * A {@link ByteArrayOutputStream} subclass that exposes the internal {@code buf} array directly,
 * allowing callers to obtain a zero-copy {@link ByteBuffer} view instead of calling
 * {@link #toByteArray()}, which always performs an {@code Arrays.copyOf}.
 */
class ExposedByteArrayOutputStream extends ByteArrayOutputStream {

    ExposedByteArrayOutputStream() {
        super();
    }

    ExposedByteArrayOutputStream(int initialCapacity) {
        super(initialCapacity);
    }

    /**
     * Returns a {@link ByteBuffer} backed directly by the internal buffer — no copy is made.
     * The buffer's position is 0 and limit equals the number of bytes written so far.
     */
    ByteBuffer asByteBuffer() {
        return ByteBuffer.wrap(buf, 0, count);
    }
}

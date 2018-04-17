/*
 * Copyright Siemens AG, 2014-2015. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.datahandler.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;

/**
 * @author daniele.fognini@tngtech.com
 */
public class ConcatClosingInputStream extends InputStream {
    private final Iterator<InputStream> streams;
    private boolean closed = false;
    private InputStream current;

    /* streams.next() and .hasNext() must not throw exceptions */
    public ConcatClosingInputStream(Iterator<InputStream> streams) {
        if (streams == null)
            streams = Collections.emptyIterator();

        if (streams.hasNext()) {
            this.current = streams.next();
        } else {
            this.closed = true;
        }

        this.streams = streams;
    }

    @Override
    public int read() throws IOException {
        if (closed)
            return -1;

        if (current == null) {
            return safeCloseAndThrow(new IOException("cannot read from null Stream"));
        }

        int read;
        try {
            read = current.read();
        } catch (IOException e) {
            return safeCloseAndThrow(e);
        }

        if (read >= 0)
            return read;

        else {
            try {
                current.close();
            } catch (IOException e) {
                return safeCloseAndThrow(e);
            }
            if (streams.hasNext()) {
                current = streams.next();
                return read();
            } else {
                closed = true;
                return -1;
            }
        }
    }

    private int safeCloseAndThrow(IOException e) throws IOException {
        try {
            close();
        } catch (IOException close) {
            e.addSuppressed(close);
        }
        throw e;
    }

    @Override
    public void close() throws IOException {
        if (closed)
            return;

        IOException ioException = tryClose(current);
        while (streams.hasNext()) {
            IOException exception = tryClose(streams.next());
            ioException = asSuppressedOf(ioException, exception);
        }
        closed = true;

        if (ioException != null)
            throw ioException;
    }

    private IOException asSuppressedOf(IOException ioException, IOException exception) {
        if (exception != null) {
            if (ioException == null) {
                ioException = exception;
            } else {
                ioException.addSuppressed(exception);
            }
        }
        return ioException;
    }

    private IOException tryClose(InputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                return e;
            }
        }
        return null;
    }
}

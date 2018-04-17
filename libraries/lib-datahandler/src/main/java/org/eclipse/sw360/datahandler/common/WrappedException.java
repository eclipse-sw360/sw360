/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.datahandler.common;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;

/**
 * This class should help catching checked exceptions especially in lambda
 * function e.g. on using streams. It contains generic and specialized wrappers.
 *
 * <h1>Generic example:</h1>
 *
 * <pre>
 * try {
 *     x.stream.filter(element -> wrap(() -> element.test())).collect(Collectors.toList());
 * } catch (WrappedException e) {
 *     // e.getCause() contains original exception
 * }
 * </pre>
 *
 * <h1>TException example:</h1>
 *
 * <pre>
 * try {
 *     x.stream.filter(element -> wrapTException(() -> element.test())).collect(Collectors.toList());
 * } catch (WrappedTException e) {
 *     // e.getCause() contains original TException (and is returned as type
 *     // TException e.g. for rethrow
 * }
 * </pre>
 */
public class WrappedException extends RuntimeException {

    public WrappedException(Throwable cause) {
        super(cause);
    }

    @FunctionalInterface
    public interface ExceptionWrapperFunction<R, E extends Throwable> {
        R run() throws E;
    }

    @FunctionalInterface
    public interface ExceptionWrapperFunctionNotReturn<E extends Throwable> {
        void run() throws E;
    }

    // Generic
    public static <R> R wrapException(ExceptionWrapperFunction<R, Exception> function) {
        try {
            return function.run();
        } catch (Exception exception) {
            throw new WrappedException(exception);
        }
    }

    public static void wrapException(ExceptionWrapperFunctionNotReturn<Exception> function) {
        try {
            function.run();
        } catch (Exception exception) {
            throw new WrappedException(exception);
        }
    }

    // wrapper for TException
    public static class WrappedTException extends WrappedException {
        public WrappedTException(TException exception) {
            super(exception);
        }

        public TException getCause() {
            return (TException) super.getCause();
        }
    }

    public static <R> R wrapTException(ExceptionWrapperFunction<R, TException> function) {
        try {
            return function.run();
        } catch(TException exception) {
            throw new WrappedTException(exception);
        }
    }

    public static void wrapTException(ExceptionWrapperFunctionNotReturn<TException> function) {
        try {
            function.run();
        } catch (TException exception) {
            throw new WrappedTException(exception);
        }
    }

    // wrapper for SW360Exception
    public static class WrappedSW360Exception extends WrappedException {
        public WrappedSW360Exception(SW360Exception exception) {
            super(exception);
        }

        public SW360Exception getCause() {
            return (SW360Exception) super.getCause();
        }
    }

    public static <R> R wrapSW360Exception(ExceptionWrapperFunction<R, SW360Exception> function) {
        try {
            return function.run();
        } catch (SW360Exception exception) {
            throw new WrappedSW360Exception(exception);
        }
    }

    public static void wrapSW360Exception(ExceptionWrapperFunctionNotReturn<SW360Exception> function) {
        try {
            function.run();
        } catch (SW360Exception exception) {
            throw new WrappedSW360Exception(exception);
        }
    }
}

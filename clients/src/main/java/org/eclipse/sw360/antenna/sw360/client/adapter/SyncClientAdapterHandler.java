/*
 * Copyright (c) Bosch.IO GmbH 2020.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.antenna.sw360.client.adapter;

import org.eclipse.sw360.antenna.sw360.client.utils.FutureUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;

/**
 * <p>
 * An {@code InvocationHandler} implementation that maps an asynchronous client
 * adapter implementation to a synchronous one.
 * </p>
 * <p>
 * An instance of this class is configured with an asynchronous SW360 client
 * adapter object and implements the corresponding synchronous interface.
 * Method invocations are handled by delegating to the asynchronous adapter and
 * waiting for the resulting {@code CompletableFuture} to complete. This is
 * possible as the synchronous and asynchronous adapter interface exactly
 * correspond to each other and only differ in the return types of their
 * methods.
 * </p>
 */
final class SyncClientAdapterHandler implements InvocationHandler {
    /**
     * Name of the equals() method, which needs a special treatment.
     */
    private static final String METHOD_EQUALS = "equals";

    /**
     * The class of the asynchronous delegate.
     */
    private final Class<?> asyncIfcClass;

    /**
     * The object to delegate calls to.
     */
    private final Object delegate;

    /**
     * Creates a new implementation of {@code SyncClientAdapterHandler} that
     * delegates to the specified object.
     *
     * @param asyncIfcClass the asynchronous class of the delegate
     * @param delegate      the delegate object
     */
    private SyncClientAdapterHandler(Class<?> asyncIfcClass, Object delegate) {
        this.asyncIfcClass = asyncIfcClass;
        this.delegate = delegate;
    }

    /**
     * Creates a new instance of {@code BlockingClientAdapterHandler} that
     * implements a specific synchronous interface by delegating to a given
     * asynchronous adapter object.
     *
     * @param syncIfcClass    the (synchronous) interface class to be implemented
     * @param asyncIfcClass   the (asynchronous) interface class of the delegate
     * @param delegateAdapter the (asynchronous) adapter to delegate to
     * @param <S>             the type of the synchronous interface to be implemented
     * @param <A>             the type of the asynchronous delegate adapter
     * @return the newly created {@code BlockingClientAdapterHandler}
     */
    public static <S, A> S newHandler(Class<? extends S> syncIfcClass, Class<? super A> asyncIfcClass,
                                      A delegateAdapter) {
        SyncClientAdapterHandler handler = new SyncClientAdapterHandler(asyncIfcClass, delegateAdapter);
        Class<?>[] ifcClasses = new Class<?>[]{syncIfcClass};
        return syncIfcClass.cast(Proxy.newProxyInstance(syncIfcClass.getClassLoader(), ifcClasses, handler));
    }

    /**
     * {@inheritDoc} This implementation invokes the method to be handled on
     * the delegate object. If this results in a {@code CompletableFuture}, it
     * waits for the future to complete. Otherwise, the result is returned
     * directly. (That way, some special methods - e.g. the ones returning the
     * underlying SW360 client - can be handled correctly as well.)
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass().equals(Object.class)) {
            return handleMethodFromObject(method, args);
        }

        Method asyncMethod = asyncIfcClass.getMethod(method.getName(), method.getParameterTypes());
        Object result = asyncMethod.invoke(delegate, args);
        if (result instanceof CompletableFuture) {
            CompletableFuture<?> futResult = (CompletableFuture<?>) result;
            return FutureUtils.block(futResult);
        }
        return result;
    }

    /**
     * Handles a method that is declared by the Object class. Such methods
     * typically also need to be delegated to the delegate object; however,
     * they cannot be found in the asynchronous interface class. The equals()
     * is special though. We consider two proxy objects equal if they refer to
     * the same delegate.
     *
     * @param method the method to be handled
     * @return the result of the invocation
     * @throws IllegalAccessException    if a method is not accessible
     * @throws InvocationTargetException if a method invocation fails
     */
    private Object handleMethodFromObject(Method method, Object[] args)
            throws IllegalAccessException, InvocationTargetException {
        if (METHOD_EQUALS.equals(method.getName())) {
            Object other = args[0];
            if (!Proxy.isProxyClass(other.getClass())) {
                return false;
            }
            InvocationHandler otherHandler = Proxy.getInvocationHandler(other);
            return otherHandler instanceof SyncClientAdapterHandler &&
                    ((SyncClientAdapterHandler) otherHandler).delegate == delegate;
        } else {
            return method.invoke(delegate, args);
        }
    }
}

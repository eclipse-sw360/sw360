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

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

/**
 * A test class to verify that the synchronous client adapter interfaces are
 * correctly aligned with the asynchronous interfaces.
 * <p>
 * As the synchronous interfaces are implemented by a dynamic proxy and
 * delegated to asynchronous interfaces using reflection, there are no compiler
 * checks that guarantee that the interfaces are consistent. So this is
 * verified manually by this test. The test compares the asynchronous and
 * synchronous interfaces of the different client adapters. The methods in the
 * asynchronous adapters returning a future are iterated over, and it is
 * checked that there is a corresponding method in the synchronous interface
 * with the same name, method parameters, and return type.
 */
public class SyncClientAdaptersTest {
    /**
     * Verifies that a synchronous client adapter interface is consistent with
     * its asynchronous counterpart. All asynchronous methods are checked
     * whether they have a correct synchronous counterpart.
     *
     * @param asyncIfc the class of the asynchronous interface
     * @param syncIfc  the class for the synchronous interface
     */
    private static void checkAdapterInterfaces(Class<?> asyncIfc, Class<?> syncIfc) {
        Arrays.stream(asyncIfc.getDeclaredMethods())
                .filter(method -> CompletableFuture.class.equals(method.getReturnType()))
                .forEach(method -> checkMethod(syncIfc, method));
    }

    /**
     * Checks whether a synchronous client adapter interface has a method that
     * corresponds to the given asynchronous method. The method must have the
     * same name and parameter types, but instead of a future, it must return
     * the generic type of the future (which can itself be a generic type). By
     * checking the actual values of type variables in generic types, it can be
     * verified that the return types actually are compliant with each other.
     *
     * @param syncIfc the class for the synchronous interface
     * @param method  the method to be verified
     */
    private static void checkMethod(Class<?> syncIfc, Method method) {
        Method syncMethod = findMethod(syncIfc, method);
        if (syncMethod == null) {
            throw new AssertionError("Could not find match for method " + method +
                    " in synchronous interface " + syncIfc);
        }
        ParameterizedType type = (ParameterizedType) method.getGenericReturnType();
        Type actualTypeArgument = type.getActualTypeArguments()[0];
        if (!checkMethodReturnTypes(actualTypeArgument, syncMethod.getGenericReturnType())) {
            throw new AssertionError("Incompatible return type of method " + syncMethod +
                    ". Expected " + actualTypeArgument);
        }
    }

    /**
     * Compares the return types of a corresponding pair of an asynchronous and
     * synchronous method. Here some corner cases have to be taken into
     * account; for instance, the case that the return type is generic.
     *
     * @param asyncReturnType the return type of the asynchronous method (which
     *                        has already been extracted from the future)
     * @param syncReturnType  the return type of the synchronous method
     * @return a flag whether both types are compatible
     */
    private static boolean checkMethodReturnTypes(Type asyncReturnType, Type syncReturnType) {
        if (asyncReturnType.equals(syncReturnType)) {
            return true;
        }

        // Deal with primitive and wrapper types
        if (unbox(asyncReturnType).equals(syncReturnType)) {
            return true;
        }

        // Special case generic return type
        if (asyncReturnType instanceof TypeVariable<?> && syncReturnType instanceof TypeVariable<?>) {
            TypeVariable<?> asyncVar = (TypeVariable<?>) asyncReturnType;
            TypeVariable<?> syncVar = (TypeVariable<?>) syncReturnType;
            if (asyncVar.getName().equals(syncVar.getName()) &&
                    Arrays.equals(asyncVar.getBounds(), syncVar.getBounds())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Searches for a method in a class with a given name and parameters.
     *
     * @param clazz  the class that should declare the method
     * @param method the method to be looked up
     * @return the method that was found or <strong>null</strong> if there was
     * no match
     */
    private static Method findMethod(Class<?> clazz, Method method) {
        try {
            return clazz.getDeclaredMethod(method.getName(), method.getParameterTypes());
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Returns the unboxed primitive type if the passed in class is a primitive
     * wrapper class. Otherwise, the class is returned unchanged. This is
     * necessary to match methods with primitive return types or void correctly.
     *
     * @param type the type to be unboxed
     * @return the unboxed type
     */
    private static Type unbox(Type type) {
        if (!(type instanceof Class)) {
            return type;
        }
        Class<?> clazz = (Class<?>) type;
        if (Void.class.equals(clazz)) {
            return Void.TYPE;  // not handled by wrapperToPrimitive()
        }
        return ObjectUtils.defaultIfNull(ClassUtils.wrapperToPrimitive(clazz), clazz);
    }

    @Test
    public void testComponentClientAdapterInterfaces() {
        checkAdapterInterfaces(SW360ComponentClientAdapterAsync.class, SW360ComponentClientAdapter.class);
    }

    @Test
    public void testReleaseClientAdapterInterfaces() {
        checkAdapterInterfaces(SW360ReleaseClientAdapterAsync.class, SW360ReleaseClientAdapter.class);
    }

    @Test
    public void testProjectClientAdapterInterfaces() {
        checkAdapterInterfaces(SW360ProjectClientAdapterAsync.class, SW360ProjectClientAdapter.class);
    }

    @Test
    public void testLicenseClientAdapterInterfaces() {
        checkAdapterInterfaces(SW360LicenseClientAdapterAsync.class, SW360LicenseClientAdapter.class);
    }
}

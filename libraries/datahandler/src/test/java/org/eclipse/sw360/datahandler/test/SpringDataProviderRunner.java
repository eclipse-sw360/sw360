/*
 * Copied from https://www.lenar.io/junit-dataprovider-with-spring-example/
 * No license provided when last accessed at 2025-11-26, thus assuming EPL-2.0
 * would be applicable.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.datahandler.test;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderFilter;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import com.tngtech.java.junit.dataprovider.internal.DataConverter;
import com.tngtech.java.junit.dataprovider.internal.TestGenerator;
import com.tngtech.java.junit.dataprovider.internal.TestValidator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.TestClass;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Character.toUpperCase;

/**
 * A custom runner for JUnit that allows the usage of <a href="http://testng.org/">TestNG</a>-like dataproviders. Data
 * providers are public, static methods that return an {@link Object}{@code [][]} (see {@link DataProvider}).
 * <p>
 * Your test method must be annotated with {@code @}{@link UseDataProvider} or {@code @}{@link DataProvider},
 * additionally.
 */
public class SpringDataProviderRunner extends SpringJUnit4ClassRunner {

    /**
     * The {@link DataConverter} to be used to convert from supported return types of any dataprovider to {@link List}
     * {@code <}{@link Object}{@code []>} such that data can be further handled.
     */
    protected DataConverter dataConverter;

    /**
     * The {@link TestGenerator} to be used to generate all framework methods to be executed as test (enhanced by data
     * providers data if desired).
     */
    protected TestGenerator testGenerator;

    /**
     * The {@link TestValidator} to be used to validate all test methods to be executed as test and all dataprovider to
     * be used to explode tests.
     */
    protected TestValidator testValidator;

    /**
     * Cached result of {@link #computeTestMethods()}.
     * <p>
     * This field is package private (= visible) for testing.
     * </p>
     */
    List<FrameworkMethod> computedTestMethods;

    /**
     * Creates a DataProviderRunner to run supplied {@code clazz}.
     *
     * @param clazz the test {@link Class} to run
     * @throws InitializationError if the test {@link Class} is malformed.
     */
    public SpringDataProviderRunner(Class<?> clazz) throws InitializationError {
        super(clazz);
    }

    @Override
    protected void collectInitializationErrors(List<Throwable> errors) {
        // initialize testValidator, testGenerator and dataConverter here because "super" in constructor already calls
        // this, i.e. fields are not initialized yet but required in super.collectInitializationErrors(errors) ...
        initializeHelpers();

        super.collectInitializationErrors(errors);
    }

    /**
     * Initialize and/or override {@link DataConverter}, {@link TestGenerator} and/or {@link TestValidator} helper classes.
     */
    protected void initializeHelpers() {
        dataConverter = new DataConverter();
        testGenerator = new TestGenerator(dataConverter);
        testValidator = new TestValidator(dataConverter);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Is copied from {@link org.junit.runners.BlockJUnit4ClassRunner#validateInstanceMethods} because {@link #computeTestMethods()} must
     * not be called if validation already found errors!
     */
    @Override
    @Deprecated
    protected void validateInstanceMethods(List<Throwable> errors) {
        validatePublicVoidNoArgMethods(After.class, false, errors);
        validatePublicVoidNoArgMethods(Before.class, false, errors);
        validateTestMethods(errors);

        if (errors.isEmpty() && computeTestMethods().isEmpty()) {
            errors.add(new Exception("No runnable methods"));
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Additionally validates dataproviders.
     *
     * @param errors that are added to this list
     * @throws NullPointerException if given {@code errors} is {@code null}
     */
    @Override
    protected void validateTestMethods(List<Throwable> errors) {
        if (errors == null) {
            throw new NullPointerException("errors must not be null");
        }

        // This method cannot use the result of "computeTestMethods()" because the method ignores invalid test methods
        // and dataproviders silently (except if a dataprovider method cannot be called). However, the common errors
        // are not raised as {@link RuntimeException} to go the JUnit way of detecting errors. This implies that we have
        // to browse the whole class for test methods and dataproviders again :-(.

        for (FrameworkMethod testMethod : getTestClassInt().getAnnotatedMethods(Test.class)) {
            testValidator.validateTestMethod(testMethod, errors);
        }
        for (FrameworkMethod testMethod : getTestClassInt().getAnnotatedMethods(UseDataProvider.class)) {
            FrameworkMethod dataProviderMethod = getDataProviderMethod(testMethod);
            if (dataProviderMethod == null) {
                errors.add(new Exception(String.format("No valid dataprovider found for test %s. By convention the dataprovider method name must either be equal to the test methods name, have a prefix of 'dataProvider' instead of 'test' or is overridden by using @UseDataProvider#value().", testMethod.getName())));

            } else {
                DataProvider dataProvider = dataProviderMethod.getAnnotation(DataProvider.class);
                if (dataProvider == null) {
                    throw new IllegalStateException(String.format("@%s annotaion not found on dataprovider method %s", DataProvider.class.getSimpleName(), dataProviderMethod.getName()));
                }
                testValidator.validateDataProviderMethod(dataProviderMethod, dataProvider, errors);
            }
        }
    }

    /**
     * Generates the exploded list of methods that run tests. All methods annotated with {@code @Test} on this class and
     * super classes that are not overridden are checked if they use a {@code @}{@link DataProvider} or not. If yes, for
     * each row of the {@link DataProvider}s result a specific, parameterized test method will be added. If not, the
     * original test method is added.
     * <p>
     * Additionally, caches the result as is call multiple times while test execution by
     * the JUnit framework (to validate, to filter, to execute, ...).
     *
     * @return the exploded list of test methods (never {@code null})
     */
    @Override
    protected List<FrameworkMethod> computeTestMethods() {
        if (computedTestMethods == null) {
            // Further method for generation is required due to stubbing of "super.computeTestMethods()" is not possible
            computedTestMethods = generateExplodedTestMethodsFor(super.computeTestMethods());
        }
        return computedTestMethods;
    }

    /**
     * {@inheritDoc}
     * <p>
     * If possible the given {@code filter} is wrapped by {@link DataProviderFilter} to enable filtering of tests using
     * a dataprovider.
     *
     * @param filter the {@link Filter} to be wrapped or apply, respectively
     */
    @Override
    public void filter(Filter filter) throws NoTestsRemainException {
        if (filter == null) {
            throw new NullPointerException("filter must not be null");
        }
        super.filter(new DataProviderFilter(filter));
    }

    /**
     * Returns a {@link TestClass} object wrapping the class to be executed. This method is required for testing because
     * {@link #getTestClass()} is final and therefore cannot be stubbed :(
     */
    TestClass getTestClassInt() {
        return getTestClass();
    }

    /**
     * Generates the exploded list of test methods for the given {@code testMethods}. Each of the given
     * {@link FrameworkMethod}s is checked if it uses a {@code @}{@link DataProvider} or not. If yes, for each line of
     * the {@link DataProvider}s result a specific, parameterized test method will be added. If no, the original test
     * method is added.
     * <p>
     * This method is package private (= visible) for testing.
     * </p>
     *
     * @param testMethods the original test methods
     * @return the exploded list of test methods (never {@code null})
     */
    List<FrameworkMethod> generateExplodedTestMethodsFor(List<FrameworkMethod> testMethods) {
        List<FrameworkMethod> result = new ArrayList<FrameworkMethod>();
        if (testMethods == null) {
            return result;
        }
        for (FrameworkMethod testMethod : testMethods) {
            FrameworkMethod dataProviderMethod = getDataProviderMethod(testMethod);
            result.addAll(testGenerator.generateExplodedTestMethodsFor(testMethod, dataProviderMethod));
        }
        return result;
    }

    /**
     * Returns the dataprovider method that belongs to the given test method or {@code null} if no such dataprovider
     * exists or the test method is not marked for usage of a dataprovider
     * <p>
     * This method is package private (= visible) for testing.
     * </p>
     *
     * @param testMethod test method that uses a dataprovider
     * @return the dataprovider or {@code null} (if dataprovider does not exist or test method does not use any)
     * @throws IllegalArgumentException if given {@code testMethod} is {@code null}
     */
    FrameworkMethod getDataProviderMethod(FrameworkMethod testMethod) {
        if (testMethod == null) {
            throw new IllegalArgumentException("testMethod must not be null");
        }
        UseDataProvider useDataProvider = testMethod.getAnnotation(UseDataProvider.class);
        if (useDataProvider == null) {
            return null;
        }

        TestClass dataProviderLocation = findDataProviderLocation(useDataProvider);
        List<FrameworkMethod> dataProviderMethods = dataProviderLocation.getAnnotatedMethods(DataProvider.class);
        return findDataProviderMethod(dataProviderMethods, useDataProvider.value(), testMethod.getName());
    }

    /**
     * <p>
     * This method is package private (= visible) for testing.
     * </p>
     */
    TestClass findDataProviderLocation(UseDataProvider useDataProvider) {
        if (useDataProvider.location().length == 0) {
            return getTestClassInt();
        }
        return new TestClass(useDataProvider.location()[0]);
    }

    private FrameworkMethod findDataProviderMethod(List<FrameworkMethod> dataProviderMethods, String useDataProviderValue, String testMethodName) {
        if (!UseDataProvider.DEFAULT_VALUE.equals(useDataProviderValue)) {
            return findMethod(dataProviderMethods, useDataProviderValue);
        }

        FrameworkMethod result = findMethod(dataProviderMethods, testMethodName);
        if (result == null) {
            String dataProviderMethodName = testMethodName.replaceAll("^test", "dataProvider");
            result = findMethod(dataProviderMethods, dataProviderMethodName);
        }
        if (result == null) {
            String dataProviderMethodName = testMethodName.replaceAll("^test", "data");
            result = findMethod(dataProviderMethods, dataProviderMethodName);
        }
        if (result == null) {
            String dataProviderMethodName = "dataProvider" + toUpperCase(testMethodName.charAt(0)) + testMethodName.substring(1);
            result = findMethod(dataProviderMethods, dataProviderMethodName);
        }
        if (result == null) {
            String dataProviderMethodName = "data" + toUpperCase(testMethodName.charAt(0)) + testMethodName.substring(1);
            result = findMethod(dataProviderMethods, dataProviderMethodName);
        }
        return result;
    }

    private FrameworkMethod findMethod(List<FrameworkMethod> methods, String methodName) {
        for (FrameworkMethod method : methods) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        return null;
    }
}

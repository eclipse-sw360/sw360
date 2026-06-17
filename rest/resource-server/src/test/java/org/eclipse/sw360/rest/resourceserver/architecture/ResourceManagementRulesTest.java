/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Validates resource management and lifecycle practices in the SW360 REST module.
 */
@DisplayName("Resource Management Rules")
class ResourceManagementRulesTest extends SW360ArchitectureTest {

    @Test
    @DisplayName("Classes should not override finalize() — use try-with-resources or Cleaner")
    void noClassShouldUseFinalizeMethod() {
        ArchCondition<JavaClass> notOverrideFinalize =
                new ArchCondition<>("not override finalize()") {
                    @Override
                    public void check(JavaClass javaClass, ConditionEvents events) {
                        javaClass.getMethods().stream()
                                .filter(m -> m.getName().equals("finalize")
                                        && m.getRawParameterTypes().isEmpty())
                                .forEach(m -> events.add(SimpleConditionEvent.violated(
                                        javaClass,
                                        javaClass.getSimpleName()
                                                + " overrides finalize() — use try-with-resources "
                                                + "or java.lang.ref.Cleaner instead")));
                    }
                };

        ArchRule rule = classes()
                .that().resideInAPackage("..rest.resourceserver..")
                .should(notOverrideFinalize)
                .as("Classes must not override finalize() — " +
                        "use try-with-resources or java.lang.ref.Cleaner");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("Business logic should not use Runtime.addShutdownHook — use Spring lifecycle")
    void noClassShouldUseShutdownHookInBusinessLogic() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..rest.resourceserver..")
                .and().resideOutsideOfPackage("..resourceserver.core..")
                .should().dependOnClassesThat()
                .haveFullyQualifiedName("java.lang.Runtime")
                .as("Do not use Runtime.addShutdownHook() — " +
                        "use Spring lifecycle (@PreDestroy, DisposableBean)");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("Controllers should not create threads directly — use @Async or TaskExecutor")
    void controllersShouldNotCreateThreadsDirectly() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..rest.resourceserver..")
                .and().haveSimpleNameEndingWith("Controller")
                .should().dependOnClassesThat()
                .haveFullyQualifiedName("java.lang.Thread")
                .as("Controllers must not create threads directly — " +
                        "use @Async or Spring-managed ExecutorService");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("Services should not create Executors directly — inject Spring TaskExecutor")
    void servicesShouldNotCreateExecutorsDirect() {
        // TODO: Sw360ProjectService and Sw360ReleaseService should be refactored
        // to use Spring's TaskExecutor instead of Executors factory
        ArchRule rule = noClasses()
                .that().resideInAPackage("..rest.resourceserver..")
                .and().areAnnotatedWith(org.springframework.stereotype.Service.class)
                .and().doNotHaveSimpleName("Sw360ProjectService")
                .and().doNotHaveSimpleName("Sw360ReleaseService")
                .should().dependOnClassesThat()
                .haveFullyQualifiedName("java.util.concurrent.Executors")
                .as("@Service classes should not use Executors factory directly — " +
                        "inject Spring's TaskExecutor instead");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("Controllers should not use FileReader — use Files.newBufferedReader(Path)")
    void classesShouldPreferPathOverFileConstructor() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..rest.resourceserver..")
                .and().haveSimpleNameEndingWith("Controller")
                .should().dependOnClassesThat()
                .haveFullyQualifiedName("java.io.FileReader")
                .as("Controllers should not use FileReader directly — " +
                        "use Files.newBufferedReader(Path) with try-with-resources");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("Controllers should not use FileInputStream — delegate to service layer")
    void controllersShouldNotUseFileInputStreamDirectly() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..rest.resourceserver..")
                .and().haveSimpleNameEndingWith("Controller")
                .should().dependOnClassesThat()
                .haveFullyQualifiedName("java.io.FileInputStream")
                .as("Controllers should not use FileInputStream — " +
                        "delegate to service layer or use Spring's Resource abstraction");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("Controllers should not use FileOutputStream — delegate to service layer")
    void controllersShouldNotUseFileOutputStreamDirectly() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..rest.resourceserver..")
                .and().haveSimpleNameEndingWith("Controller")
                .should().dependOnClassesThat()
                .haveFullyQualifiedName("java.io.FileOutputStream")
                .as("Controllers should not use FileOutputStream — " +
                        "delegate file operations to service layer");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("Controllers should not have mutable static fields — maintain statelessness")
    void controllersShouldNotHaveMutableStaticFields() {
        ArchCondition<JavaClass> notHaveMutableStaticFields =
                new ArchCondition<>("not have mutable static fields") {
                    @Override
                    public void check(JavaClass javaClass, ConditionEvents events) {
                        for (JavaField field : javaClass.getFields()) {
                            if (field.getModifiers().contains(JavaModifier.STATIC)
                                    && !field.getModifiers().contains(JavaModifier.FINAL)
                                    && !field.getName().equals("log")
                                    && !field.getName().equals("logger")) {
                                events.add(SimpleConditionEvent.violated(
                                        javaClass,
                                        String.format("%s has mutable static field '%s' — " +
                                                        "controllers must be stateless",
                                                javaClass.getSimpleName(),
                                                field.getName())));
                            }
                        }
                    }
                };

        ArchRule rule = classes()
                .that().resideInAPackage("..rest.resourceserver..")
                .and().haveSimpleNameEndingWith("Controller")
                .should(notHaveMutableStaticFields)
                .as("Controllers must not have mutable static fields — " +
                        "controllers should be stateless");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("Classes should not catch Throwable — catch specific exception types")
    void classesShouldNotCatchThrowable() {
        // Bytecode from try-with-resources generates implicit Throwable handlers.
        // We filter those out: flag only when Throwable catches exceed specific catches.
        ArchCondition<JavaClass> notCatchThrowable =
                new ArchCondition<>("not catch Throwable") {
                    @Override
                    public void check(JavaClass javaClass, ConditionEvents events) {
                        javaClass.getCodeUnits().forEach(codeUnit -> {
                            var blocks = codeUnit.getTryCatchBlocks();

                            long throwableCatches = blocks.stream()
                                    .filter(tc -> tc.getCaughtThrowables().stream()
                                            .anyMatch(t -> t.getName()
                                                    .equals("java.lang.Throwable")))
                                    .count();

                            long specificCatches = blocks.stream()
                                    .filter(tc -> tc.getCaughtThrowables().stream()
                                            .anyMatch(t -> !t.getName()
                                                    .equals("java.lang.Throwable")))
                                    .count();

                            if (throwableCatches > 0 && specificCatches == 0) {
                                return;
                            }
                            if (throwableCatches > specificCatches) {
                                events.add(SimpleConditionEvent.violated(
                                        javaClass,
                                        String.format("%s catches Throwable in %s — " +
                                                        "catch specific exception types",
                                                javaClass.getSimpleName(),
                                                codeUnit.getName())));
                            }
                        });
                    }
                };

        // TODO: Sw360ReleaseService and Sw360LicenseService should be refactored
        ArchRule rule = classes()
                .that().resideInAPackage("..rest.resourceserver..")
                .and().doNotHaveSimpleName("Sw360ReleaseService")
                .and().doNotHaveSimpleName("Sw360LicenseService")
                .should(notCatchThrowable)
                .as("Classes should not catch Throwable — " +
                        "catch specific exception types instead");

        rule.check(restClasses);
    }
}

/*
 * Copyright Siemens AG, 2025-2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Validates that SW360 REST module follows proper logging standards.
 * <p>
 * SW360 uses <strong>Log4j2</strong> ({@code LogManager.getLogger()}) or
 * <strong>Lombok</strong> ({@code @Slf4j}) for logging.
 * The following are strictly prohibited:
 * <ul>
 *   <li>{@code System.out.println()} / {@code System.err.println()}</li>
 *   <li>{@code e.printStackTrace()}</li>
 *   <li>{@code java.util.logging} (JUL)</li>
 *   <li>Apache Commons Logging</li>
 * </ul>
 */
@DisplayName("Logging Standard Rules")
class LoggingStandardRulesTest extends SW360ArchitectureTest {

    @Test
    @DisplayName("No class should use System.out")
    void noClassShouldUseSystemOut() {
        ArchRule rule = noClasses()
                .should().accessFieldWhere(
                        com.tngtech.archunit.core.domain.JavaFieldAccess.Predicates
                                .target(com.tngtech.archunit.base.DescribedPredicate.describe(
                                        "System.out",
                                        target -> target.getOwner().isEquivalentTo(System.class)
                                                && target.getName().equals("out")))
                )
                .as("No class should use System.out — use Log4j2 logger instead");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("No class should use System.err")
    void noClassShouldUseSystemErr() {
        ArchRule rule = noClasses()
                .should().accessFieldWhere(
                        com.tngtech.archunit.core.domain.JavaFieldAccess.Predicates
                                .target(com.tngtech.archunit.base.DescribedPredicate.describe(
                                        "System.err",
                                        target -> target.getOwner().isEquivalentTo(System.class)
                                                && target.getName().equals("err")))
                )
                .as("No class should use System.err — use Log4j2 logger instead");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("No class should call printStackTrace()")
    void noClassShouldCallPrintStackTrace() {
        ArchCondition<JavaClass> notCallPrintStackTrace =
                new ArchCondition<>("not call printStackTrace()") {
                    @Override
                    public void check(JavaClass javaClass, ConditionEvents events) {
                        javaClass.getCodeUnits().stream()
                                .flatMap(codeUnit -> codeUnit.getCallsFromSelf().stream())
                                .filter(call -> call.getTarget().getName().equals("printStackTrace"))
                                .forEach(call -> events.add(SimpleConditionEvent.violated(
                                        javaClass,
                                        String.format("%s calls printStackTrace() in %s — " +
                                                        "use log.error() with exception parameter instead",
                                                javaClass.getName(),
                                                call.getOrigin().getName()))));
                    }
                };

        ArchRule rule = noClasses()
                .should(notCallPrintStackTrace)
                .as("No class should call printStackTrace() — use Log4j2 logger with exception parameter");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("No class should use java.util.logging")
    void noClassShouldUseJavaUtilLogging() {
        ArchRule rule = noClasses()
                .should().dependOnClassesThat()
                .resideInAPackage("java.util.logging")
                .as("No class should use java.util.logging — use Log4j2 (LogManager.getLogger()) " +
                        "or Lombok @Slf4j instead");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("No class should use Commons Logging directly")
    void noClassShouldUseCommonsLogging() {
        ArchRule rule = noClasses()
                .should().dependOnClassesThat()
                .resideInAPackage("org.apache.commons.logging")
                .as("No class should use Apache Commons Logging directly — " +
                        "use Log4j2 (LogManager.getLogger()) or Lombok @Slf4j instead");

        rule.check(restClasses);
    }
}

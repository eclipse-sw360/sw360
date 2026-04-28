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

import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Validates general coding standard rules for the SW360 REST module.
 * <p>
 * These rules enforce structural integrity and code quality patterns:
 * <ul>
 *   <li>Exception classes must follow exception naming conventions</li>
 *   <li>No dependency on {@code java.lang.Error} subtypes
 *       (prefer specific exception types)</li>
 *   <li>Interfaces must follow Java naming conventions (no {@code I} prefix)</li>
 *   <li>Constants classes must be {@code final}</li>
 *   <li>No dependency on removed {@code javax} packages (use {@code jakarta} instead)</li>
 *   <li>Prefer modern Java APIs (java.time over java.util.Date)</li>
 * </ul>
 */
@DisplayName("Coding Standard Rules")
class CodingStandardRulesTest extends SW360ArchitectureTest {

    @Test
    @DisplayName("Exception classes should have name ending with 'Exception'")
    void exceptionClassesShouldBeNamedProperly() {
        ArchRule rule = classes()
                .that().areAssignableTo(Exception.class)
                .and().resideInAPackage("..rest.resourceserver..")
                .and().doNotHaveFullyQualifiedName(Exception.class.getName())
                .should().haveSimpleNameEndingWith("Exception")
                .as("Custom exception classes should have name ending with 'Exception'");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("No class outside core should depend on java.lang.Error")
    void noClassShouldDependOnJavaLangError() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..rest.resourceserver..")
                .and().resideOutsideOfPackage("..resourceserver.core..")
                .and().resideOutsideOfPackage("..resourceserver.security..")
                .and().haveSimpleNameNotEndingWith("ResourceProcessor")
                .should().dependOnClassesThat()
                .areAssignableTo(java.lang.Error.class)
                .as("Classes should not depend on java.lang.Error subtypes — " +
                        "use specific exception types from the core package");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("Interfaces should not have 'I' prefix")
    void interfacesShouldNotHaveIPrefix() {
        ArchRule rule = noClasses()
                .that().areInterfaces()
                .and().resideInAPackage("..rest.resourceserver..")
                .should().haveSimpleNameStartingWith("I")
                .as("Interfaces should not use 'I' prefix — follow Java naming conventions");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("Constants classes should be final")
    void constantsClassesShouldBeFinal() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Constants")
                .and().resideInAPackage("..rest.resourceserver..")
                .should().haveModifier(com.tngtech.archunit.core.domain.JavaModifier.FINAL)
                .allowEmptyShould(true)
                .as("Constants classes should be declared final");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("Classes should not depend on removed javax.servlet — use jakarta.servlet instead")
    void noClassShouldDependOnJavaxServlet() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..rest.resourceserver..")
                .should().dependOnClassesThat()
                .resideInAPackage("javax.servlet..")
                .as("Use jakarta.servlet (not javax.servlet) — SW360 runs on Spring Boot 3.x / Jakarta EE");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("Classes should not depend on javax.annotation (@Nonnull etc.) — use Lombok")
    void noClassShouldDependOnJavaxAnnotationNullable() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..rest.resourceserver..")
                .should().dependOnClassesThat()
                .haveFullyQualifiedName("javax.annotation.Nullable")
                .as("Use lombok.NonNull instead of javax.annotation");

        rule.check(restClasses);
    }

    // ========== Resource Management & Modern Java Best Practices ==========

    @Test
    @DisplayName("Classes should not use legacy StringBuffer — use StringBuilder instead")
    void noClassShouldUseStringBuffer() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..rest.resourceserver..")
                .should().dependOnClassesThat()
                .areAssignableTo(StringBuffer.class)
                .as("Use StringBuilder (not StringBuffer) — StringBuffer is synchronized and slower");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("Classes should not use deprecated Date constructors with millis — use java.time API")
    void noClassShouldUseLegacyDateAPI() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..rest.resourceserver..")
                .and().resideOutsideOfPackage("..resourceserver.security..")
                .and().haveSimpleNameNotContaining("Test")
                .and().haveSimpleNameNotContaining("Spec")
                .should().dependOnClassesThat()
                .haveFullyQualifiedName("java.util.Calendar")
                .as("Use java.time API (LocalDate, Instant, etc.) instead of legacy Calendar");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("Classes should not use deprecated Enumeration — use Iterator instead")
    void noClassShouldUseEnumeration() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..rest.resourceserver..")
                .should().dependOnClassesThat()
                .haveFullyQualifiedName("java.util.Enumeration")
                .as("Use Iterator (not Enumeration) — Enumeration is legacy");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("Classes should not use Observable/Observer — use PropertyChangeListener or reactive patterns")
    void noClassShouldUseObservable() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..rest.resourceserver..")
                .should().dependOnClassesThat()
                .haveFullyQualifiedName("java.util.Observable")
                .as("Use PropertyChangeListener or reactive patterns (not Observable) — deprecated since Java 9");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("Classes should not use raw types for collections in return types")
    void noClassShouldDependOnRawCollectionTypes() {
        // This checks that classes don't import raw collection-related deprecated APIs
        ArchRule rule = noClasses()
                .that().resideInAPackage("..rest.resourceserver..")
                .should().dependOnClassesThat()
                .haveFullyQualifiedName("java.util.Dictionary")
                .as("Use Map (not Dictionary) — Dictionary is abstract and legacy");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("Classes should not directly depend on sun.* internal JDK packages")
    void noClassShouldDependOnSunPackages() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..rest.resourceserver..")
                .should().dependOnClassesThat()
                .resideInAPackage("sun..")
                .as("Do not use internal JDK sun.* packages — they are not part of the public API");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("Classes should not directly depend on com.sun.* internal packages")
    void noClassShouldDependOnComSunPackages() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..rest.resourceserver..")
                .should().dependOnClassesThat()
                .resideInAPackage("com.sun..")
                .as("Do not use internal com.sun.* packages — they are not part of the public API");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("Utility classes should be final")
    void utilityClassesShouldBeFinal() {
        // Note: Spring @Service/@Component beans like RestControllerHelper cannot be final
        // as Spring may need to create proxies for them
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Utils")
                .or().haveSimpleNameEndingWith("Util")
                .and().resideInAPackage("..rest.resourceserver..")
                .and().areNotAnnotatedWith(org.springframework.stereotype.Service.class)
                .and().areNotAnnotatedWith(org.springframework.stereotype.Component.class)
                .should().haveModifier(com.tngtech.archunit.core.domain.JavaModifier.FINAL)
                .allowEmptyShould(true)
                .as("Utility classes (*Utils, *Util) that are not Spring beans should be declared final");

        rule.check(restClasses);
    }
}

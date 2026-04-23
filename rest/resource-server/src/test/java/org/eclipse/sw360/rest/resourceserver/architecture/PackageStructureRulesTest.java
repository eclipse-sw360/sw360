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
 * Validates the package structure conventions of the SW360 REST module.
 * <p>
 * Each domain entity (project, component, release, etc.) has its own
 * sub-package under {@code ..rest.resourceserver.<entity>}. This test
 * ensures structural consistency across domain packages.
 * <p>
 * Note: SW360 domain packages have intentional cross-dependencies
 * (e.g., a project references releases, components reference projects, etc.)
 * so cyclic dependency checks are not applicable at the REST module level.
 */
@DisplayName("Package Structure Rules")
class PackageStructureRulesTest extends SW360ArchitectureTest {

    @Test
    @DisplayName("Controller classes should reside in their domain sub-package, not in core")
    void controllersShouldResideInDomainPackages() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Controller")
                .should().resideOutsideOfPackage("..resourceserver.core..")
                .as("Controller classes should reside in domain packages " +
                        "(e.g., ..project.ProjectController), not in the core package");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("Serializer classes should reside in the core.serializer package")
    void serializersShouldResideInCoreSerializerPackage() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Serializer")
                .should().resideInAPackage("..resourceserver.core.serializer..")
                .as("Custom JSON serializer classes should reside in the core.serializer package");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("Exception classes should reside in the core package")
    void exceptionClassesShouldResideInCore() {
        ArchRule rule = classes()
                .that().areAssignableTo(Exception.class)
                .and().resideInAPackage("..rest.resourceserver..")
                .and().doNotHaveFullyQualifiedName(Exception.class.getName())
                .should().resideInAPackage("..resourceserver.core..")
                .as("Custom exception classes should reside in the core package");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("Security-related classes should reside in the security package")
    void securityClassesShouldResideInSecurityPackage() {
        ArchRule rule = classes()
                .that().haveSimpleNameContaining("Authentication")
                .and().resideInAPackage("..rest.resourceserver..")
                .should().resideInAPackage("..resourceserver.security..")
                .orShould().resideInAPackage("..resourceserver.core..")
                .as("Authentication-related classes should reside in the security or core package");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("No class should depend on internal JDK sun.* packages")
    void noClassesShouldDependOnInternalJdkPackages() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..rest.resourceserver..")
                .should().dependOnClassesThat()
                .resideInAPackage("sun..")
                .as("No class should depend on internal JDK (sun..) packages");

        rule.check(restClasses);
    }
}

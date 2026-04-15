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

import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Validates the layered architecture of the SW360 REST resource-server module.
 * <p>
 * The canonical layer flow is:
 * <pre>
 *   Controller  →  Service  →  Core / Security / Filter
 * </pre>
 * The {@code security} and {@code filter} packages should not depend on specific
 * domain packages (project, component, release, etc.).
 * <p>
 * Note: The {@code core} package contains {@code JacksonCustomizations} which
 * intentionally references domain-specific mixin types for JSON serialization.
 * Therefore core-to-domain dependency checks exclude {@code JacksonCustomizations}.
 */
@DisplayName("Layered Architecture Rules")
class LayerDependencyRulesTest extends SW360ArchitectureTest {

    @Test
    @DisplayName("Security package should not depend on any specific controller")
    void securityShouldNotDependOnControllers() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..resourceserver.security..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "..resourceserver.project..",
                        "..resourceserver.component..",
                        "..resourceserver.release..",
                        "..resourceserver.license..",
                        "..resourceserver.vulnerability..",
                        "..resourceserver.packages..",
                        "..resourceserver.obligation..",
                        "..resourceserver.vendor..",
                        "..resourceserver.attachment..",
                        "..resourceserver.changelog..",
                        "..resourceserver.clearingrequest..",
                        "..resourceserver.moderationrequest..",
                        "..resourceserver.ecc..",
                        "..resourceserver.report..",
                        "..resourceserver.schedule..",
                        "..resourceserver.search..",
                        "..resourceserver.department..",
                        "..resourceserver.databasesanitation..",
                        "..resourceserver.importexport..",
                        "..resourceserver.licenseinfo..",
                        "..resourceserver.admin.."
                )
                .as("Security classes should not depend on any domain-specific controller or service package");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("Core package (except JacksonCustomizations and RestControllerHelper) should not depend on domain packages")
    void coreShouldNotDependOnDomainPackages() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..resourceserver.core..")
                .and().doNotHaveSimpleName("RestControllerHelper")
                .and().doNotHaveSimpleName("AwareOfRestServices")
                .and().doNotHaveSimpleName("ThriftServiceProvider")
                .and().haveNameNotMatching(".*JacksonCustomizations.*")
                .and().haveNameNotMatching(".*Serializer")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "..resourceserver.project..",
                        "..resourceserver.component..",
                        "..resourceserver.release..",
                        "..resourceserver.license..",
                        "..resourceserver.vulnerability..",
                        "..resourceserver.packages..",
                        "..resourceserver.obligation..",
                        "..resourceserver.vendor..",
                        "..resourceserver.changelog..",
                        "..resourceserver.clearingrequest..",
                        "..resourceserver.moderationrequest.."
                )
                .as("Core classes (except JacksonCustomizations, RestControllerHelper, AwareOfRestServices, " +
                        "and custom Serializers) should not depend on domain-specific packages");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("Filter package should not depend on domain-specific packages")
    void filterShouldNotDependOnDomainPackages() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..resourceserver.filter..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "..resourceserver.project..",
                        "..resourceserver.component..",
                        "..resourceserver.release..",
                        "..resourceserver.license..",
                        "..resourceserver.vulnerability..",
                        "..resourceserver.packages..",
                        "..resourceserver.obligation.."
                )
                .as("Filter classes should not depend on domain-specific packages");

        rule.check(restClasses);
    }
}

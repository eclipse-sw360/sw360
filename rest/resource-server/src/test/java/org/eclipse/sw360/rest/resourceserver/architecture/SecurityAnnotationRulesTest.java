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
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Validates security annotation patterns in the SW360 REST module.
 * <p>
 * SW360 enforces authorization at two levels:
 * <ul>
 *   <li><strong>Spring Security filter chain</strong>: HTTP-method-based access control in
 *       {@code ResourceServerConfiguration} — GET requires {@code READ}, while
 *       POST/PUT/DELETE/PATCH require {@code WRITE}</li>
 *   <li><strong>Thrift service level</strong>: Permission checks inside backend handlers
 *       (e.g., {@code PermissionUtils.makePermission(doc, user).isActionAllowed(RequestedAction.WRITE)})</li>
 * </ul>
 * <p>
 * <strong>Important:</strong> Class-level {@code @PreAuthorize} should be avoided on
 * most controllers because it overrides the filter chain's per-HTTP-method rules and
 * would block READ-only users from accessing GET endpoints.
 * <p>
 * These rules focus on:
 * <ul>
 *   <li>Consistent use of {@code @PreAuthorize} (not deprecated alternatives)</li>
 *   <li>Valid authority values ({@code ADMIN}, {@code WRITE}, {@code READ})</li>
 *   <li>Proper security configuration annotations</li>
 * </ul>
 */
@DisplayName("Security Annotation Rules")
class SecurityAnnotationRulesTest extends SW360ArchitectureTest {


    @Test
    @DisplayName("No class should use deprecated @Secured — use @PreAuthorize instead")
    void noClassShouldUseSecuredAnnotation() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..rest.resourceserver..")
                .should().beAnnotatedWith(
                        org.springframework.security.access.annotation.Secured.class)
                .as("Use @PreAuthorize (not @Secured) — SW360 standardizes on @PreAuthorize " +
                        "for method-level security");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("No class should use @RolesAllowed — use @PreAuthorize instead")
    void noClassShouldUseRolesAllowedAnnotation() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..rest.resourceserver..")
                .should().dependOnClassesThat()
                .haveFullyQualifiedName("jakarta.annotation.security.RolesAllowed")
                .as("Use @PreAuthorize (not @RolesAllowed) — SW360 standardizes on @PreAuthorize " +
                        "for method-level security");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("No class should use deprecated @EnableGlobalMethodSecurity — use @EnableMethodSecurity")
    void noClassShouldUseDeprecatedMethodSecurityAnnotation() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..rest.resourceserver..")
                .should().dependOnClassesThat()
                .haveFullyQualifiedName(
                        "org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity")
                .as("Use @EnableMethodSecurity (not deprecated @EnableGlobalMethodSecurity) — " +
                        "SW360 uses Spring Security 6.x");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("@PreAuthorize values should only use known SW360 authorities")
    void preAuthorizeValuesShouldUseKnownAuthorities() {
        ArchCondition<JavaClass> useOnlyKnownAuthorities =
                new ArchCondition<>("use only known SW360 authorities (ADMIN, WRITE, READ) in @PreAuthorize") {
                    @Override
                    public void check(JavaClass javaClass, ConditionEvents events) {
                        // Check class-level @PreAuthorize
                        if (javaClass.isAnnotatedWith(PreAuthorize.class)) {
                            String value = javaClass.getAnnotationOfType(PreAuthorize.class).value();
                            validatePreAuthorizeValue(javaClass, value, "class-level", events);
                        }

                        // Check method-level @PreAuthorize
                        javaClass.getMethods().stream()
                                .filter(method -> method.isAnnotatedWith(PreAuthorize.class))
                                .forEach(method -> {
                                    String value = method.getAnnotationOfType(PreAuthorize.class).value();
                                    validatePreAuthorizeValue(javaClass, value,
                                            method.getName() + "()", events);
                                });
                    }

                    private void validatePreAuthorizeValue(JavaClass javaClass, String value,
                                                           String location, ConditionEvents events) {
                        if (!value.contains("ADMIN") && !value.contains("WRITE")
                                && !value.contains("READ")) {
                            events.add(SimpleConditionEvent.violated(javaClass,
                                    String.format("%s has @PreAuthorize(\"%s\") at %s — " +
                                                    "only 'ADMIN', 'WRITE', or 'READ' authorities are valid in SW360",
                                            javaClass.getSimpleName(), value, location)));
                        }
                    }
                };

        ArchRule rule = classes()
                .that().resideInAPackage("..rest.resourceserver..")
                .should(useOnlyKnownAuthorities)
                .as("@PreAuthorize annotations should only reference known SW360 authorities: " +
                        "ADMIN, WRITE, or READ");

        rule.check(restClasses);
    }
}

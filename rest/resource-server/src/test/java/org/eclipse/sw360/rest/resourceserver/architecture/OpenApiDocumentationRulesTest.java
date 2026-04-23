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
import io.swagger.v3.oas.annotations.Operation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.*;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Validates OpenAPI documentation standards in the SW360 REST module.
 * <p>
 * SW360 uses SpringDoc OpenAPI for API documentation. Every public REST
 * endpoint should be annotated with {@code @Operation} to ensure
 * comprehensive, auto-generated API documentation.
 */
@DisplayName("OpenAPI Documentation Rules")
class OpenApiDocumentationRulesTest extends SW360ArchitectureTest {

    @Test
    @DisplayName("REST endpoint methods should be annotated with @Operation")
    void endpointMethodsShouldHaveOperationAnnotation() {
        ArchCondition<JavaClass> haveOperationAnnotationOnEndpoints =
                new ArchCondition<>("have @Operation annotation on all REST endpoint methods") {
                    @Override
                    public void check(JavaClass javaClass, ConditionEvents events) {
                        javaClass.getMethods().stream()
                                .filter(method ->
                                        method.isAnnotatedWith(GetMapping.class)
                                                || method.isAnnotatedWith(PostMapping.class)
                                                || method.isAnnotatedWith(PatchMapping.class)
                                                || method.isAnnotatedWith(DeleteMapping.class)
                                                || method.isAnnotatedWith(PutMapping.class))
                                .filter(method -> !method.isAnnotatedWith(Operation.class))
                                .forEach(method -> events.add(SimpleConditionEvent.violated(
                                        javaClass,
                                        String.format("%s.%s() is a REST endpoint but missing " +
                                                        "@Operation annotation for OpenAPI documentation",
                                                javaClass.getSimpleName(), method.getName()))));
                    }
                };

        ArchRule rule = classes()
                .that().areAnnotatedWith(RestController.class)
                .should(haveOperationAnnotationOnEndpoints)
                .as("All REST endpoint methods should have @Operation annotation for OpenAPI docs");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("REST controllers should declare @SecurityRequirement for Swagger UI auth")
    void controllersShouldDeclareSecurityRequirementForSwagger() {
        ArchCondition<JavaClass> declareSecurityRequirements =
                new ArchCondition<>("declare @SecurityRequirement at class or method level") {
                    @Override
                    public void check(JavaClass javaClass, ConditionEvents events) {
                        boolean hasClassLevel = javaClass.isAnnotatedWith(
                                io.swagger.v3.oas.annotations.security.SecurityRequirement.class)
                                || javaClass.isAnnotatedWith(
                                io.swagger.v3.oas.annotations.security.SecurityRequirements.class);

                        if (!hasClassLevel) {
                            boolean anyMethodLevel = javaClass.getMethods().stream()
                                    .anyMatch(m -> m.isAnnotatedWith(
                                            io.swagger.v3.oas.annotations.security.SecurityRequirement.class));

                            if (!anyMethodLevel) {
                                events.add(SimpleConditionEvent.violated(javaClass,
                                        String.format("%s has no @SecurityRequirement — " +
                                                        "OpenAPI docs should show authentication requirements",
                                                javaClass.getSimpleName())));
                            }
                        }
                    }
                };

        ArchRule rule = classes()
                .that().areAnnotatedWith(RestController.class)
                .and().areAnnotatedWith(
                        org.springframework.data.rest.webmvc.BasePathAwareController.class)
                .and().doNotHaveSimpleName("VersionController")
                .and().doNotHaveSimpleName("AttachmentCleanUpController")
                .should(declareSecurityRequirements)
                .as("REST controllers should declare @SecurityRequirement for OpenAPI authentication docs");

        rule.check(restClasses);
    }
}

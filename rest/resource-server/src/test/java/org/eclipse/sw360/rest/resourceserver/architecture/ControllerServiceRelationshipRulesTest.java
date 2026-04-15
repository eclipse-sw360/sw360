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

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Validates the SW360 REST module's controller–service–helper class relationships.
 * <p>
 * SW360 REST follows a consistent pattern where:
 * <ul>
 *   <li>Each domain package has a Controller and a Service class</li>
 *   <li>Controllers declare a static URL constant (e.g., {@code PROJECTS_URL})</li>
 *   <li>Controllers inject their corresponding service(s)</li>
 *   <li>Controllers share common helpers via {@code RestControllerHelper}</li>
 * </ul>
 */
@DisplayName("Controller-Service Relationship Rules")
class ControllerServiceRelationshipRulesTest extends SW360ArchitectureTest {

    @Test
    @DisplayName("Controllers should declare a static URL constant")
    void controllersShouldDeclareUrlConstant() {
        ArchCondition<JavaClass> declareUrlConstant =
                new ArchCondition<>("declare a static *_URL constant") {
                    @Override
                    public void check(JavaClass javaClass, ConditionEvents events) {
                        boolean hasUrlConstant = javaClass.getFields().stream()
                                .anyMatch(field -> field.getName().endsWith("_URL")
                                        && field.getModifiers().contains(
                                        com.tngtech.archunit.core.domain.JavaModifier.STATIC));

                        if (!hasUrlConstant) {
                            events.add(SimpleConditionEvent.violated(javaClass,
                                    String.format("%s does not declare a static *_URL constant — " +
                                                    "controllers should define their base URL path",
                                            javaClass.getSimpleName())));
                        }
                    }
                };

        ArchRule rule = classes()
                .that().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                .and().areAnnotatedWith(
                        org.springframework.data.rest.webmvc.BasePathAwareController.class)
                .and().doNotHaveSimpleName("VersionController")
                .and().doNotHaveSimpleName("SW360ConfigurationsController")
                .should(declareUrlConstant)
                .as("REST controllers should declare a static *_URL constant for their base path");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("Controllers should inject RestControllerHelper")
    void controllersShouldInjectRestControllerHelper() {
        ArchCondition<JavaClass> dependOnRestControllerHelper =
                new ArchCondition<>("depend on RestControllerHelper") {
                    @Override
                    public void check(JavaClass javaClass, ConditionEvents events) {
                        boolean hasHelper = javaClass.getFields().stream()
                                .anyMatch(field -> field.getRawType().getSimpleName()
                                        .equals("RestControllerHelper"));

                        if (!hasHelper) {
                            events.add(SimpleConditionEvent.violated(javaClass,
                                    String.format("%s does not inject RestControllerHelper — " +
                                                    "use restControllerHelper.getSw360UserFromAuthentication() " +
                                                    "for user resolution",
                                            javaClass.getSimpleName())));
                        }
                    }
                };

        ArchRule rule = classes()
                .that().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                .and().areAnnotatedWith(
                        org.springframework.data.rest.webmvc.BasePathAwareController.class)
                .and().doNotHaveSimpleName("VersionController")
                .should(dependOnRestControllerHelper)
                .as("REST controllers should inject RestControllerHelper for user authentication and pagination");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("Services (except RestControllerHelper and Sw360ProjectService) should not depend on controller classes")
    void servicesShouldNotDependOnControllers() {
        ArchRule rule = noClasses()
                .that().areAnnotatedWith(org.springframework.stereotype.Service.class)
                .and().doNotHaveSimpleName("RestControllerHelper")
                .and().doNotHaveSimpleName("Sw360ProjectService")
                .should().dependOnClassesThat()
                .areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                .as("@Service classes (except RestControllerHelper, Sw360ProjectService) should not have " +
                        "a direct dependency on @RestController classes — services are a lower layer");

        rule.check(restClasses);
    }
}

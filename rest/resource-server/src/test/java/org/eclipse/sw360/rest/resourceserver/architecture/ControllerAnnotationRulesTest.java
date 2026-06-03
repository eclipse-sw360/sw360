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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Validates that REST controllers follow SW360's Spring annotation standards.
 * <p>
 * Every REST controller in SW360 must:
 * <ul>
 *   <li>Be annotated with both {@code @BasePathAwareController} and {@code @RestController}</li>
 *   <li>Declare {@code @SecurityRequirement} for OpenAPI documentation</li>
 *   <li>Implement {@code RepresentationModelProcessor&lt;RepositoryLinksResource&gt;}
 *       for HAL resource registration</li>
 *   <li>Use Spring HATEOAS types ({@code EntityModel}, {@code CollectionModel},
 *       {@code HalResource}) in endpoint return types for HAL+JSON responses</li>
 *   <li>Not extend other controller classes (prefer composition over inheritance)</li>
 * </ul>
 */
@DisplayName("Controller Annotation Rules")
class ControllerAnnotationRulesTest extends SW360ArchitectureTest {

    @Test
    @DisplayName("Controllers annotated with @BasePathAwareController should also be @RestController")
    void basePathAwareControllerShouldBeRestController() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(BasePathAwareController.class)
                .and().doNotHaveSimpleName("LicenseInfoController")
                .should().beAnnotatedWith(RestController.class)
                .as("@BasePathAwareController classes must also be annotated with @RestController");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("REST controllers should be annotated with @SecurityRequirement")
    void controllersShouldHaveSecurityRequirement() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(RestController.class)
                .and().areAnnotatedWith(BasePathAwareController.class)
                .and().doNotHaveSimpleName("VersionController")
                .and().doNotHaveSimpleName("AttachmentCleanUpController")
                .should().beAnnotatedWith(SecurityRequirement.class)
                .orShould().beAnnotatedWith(io.swagger.v3.oas.annotations.security.SecurityRequirements.class)
                .as("All REST controllers with @BasePathAwareController should declare " +
                        "@SecurityRequirement for OpenAPI documentation");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("REST controllers should implement RepresentationModelProcessor")
    void controllersShouldImplementRepresentationModelProcessor() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(RestController.class)
                .and().areAnnotatedWith(BasePathAwareController.class)
                .and().doNotHaveSimpleName("VersionController")
                .should().implement(org.springframework.hateoas.server.RepresentationModelProcessor.class)
                .as("All REST controllers should implement RepresentationModelProcessor " +
                        "for HAL resource link registration");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("@ControllerAdvice classes should reside in core package")
    void controllerAdviceShouldResideInCore() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(org.springframework.web.bind.annotation.ControllerAdvice.class)
                .should().resideInAPackage("..resourceserver.core..")
                .as("@ControllerAdvice exception handlers should reside in the core package");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("REST controllers should not extend other controllers")
    void controllersShouldNotExtendOtherControllers() {
        ArchCondition<JavaClass> notExtendAnotherController =
                new ArchCondition<>("not extend another @RestController class") {
                    @Override
                    public void check(JavaClass javaClass, ConditionEvents events) {
                        javaClass.getAllRawSuperclasses().stream()
                                .filter(superClass -> superClass.isAnnotatedWith(RestController.class))
                                .forEach(superClass -> events.add(SimpleConditionEvent.violated(
                                        javaClass,
                                        String.format("%s extends %s which is also a @RestController — " +
                                                        "prefer composition via service injection",
                                                javaClass.getSimpleName(),
                                                superClass.getSimpleName()))));
                    }
                };

        ArchRule rule = classes()
                .that().areAnnotatedWith(RestController.class)
                .should(notExtendAnotherController)
                .as("REST controllers should not extend other controllers — " +
                        "prefer composition via service injection");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("REST controllers should depend on Spring HATEOAS for HAL+JSON responses")
    void controllersShouldUseHateoasTypes() {
        ArchCondition<JavaClass> dependOnHateoas =
                new ArchCondition<>("depend on Spring HATEOAS types for HAL+JSON responses") {
                    @Override
                    public void check(JavaClass javaClass, ConditionEvents events) {
                        boolean usesHateoas = javaClass.getDirectDependenciesFromSelf().stream()
                                .anyMatch(dep ->
                                        dep.getTargetClass().getPackageName()
                                                .startsWith("org.springframework.hateoas")
                                                || dep.getTargetClass().getSimpleName()
                                                .equals("HalResource"));

                        if (!usesHateoas) {
                            events.add(SimpleConditionEvent.violated(javaClass,
                                    String.format("%s does not use Spring HATEOAS types " +
                                                    "(EntityModel, CollectionModel, HalResource) — " +
                                                    "SW360 APIs should return HAL+JSON responses",
                                            javaClass.getSimpleName())));
                        }
                    }
                };

        ArchRule rule = classes()
                .that().areAnnotatedWith(RestController.class)
                .and().areAnnotatedWith(BasePathAwareController.class)
                .and().doNotHaveSimpleName("VersionController")
                .should(dependOnHateoas)
                .as("REST controllers should use Spring HATEOAS types (EntityModel, CollectionModel, " +
                        "HalResource) for HAL+JSON response structure");

        rule.check(restClasses);
    }
}

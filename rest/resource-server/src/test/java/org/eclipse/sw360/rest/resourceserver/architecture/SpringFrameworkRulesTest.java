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
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Validates Spring Framework best practices in the SW360 REST module.
 * <p>
 * These rules ensure proper use of Spring stereotypes, configuration,
 * web-layer annotations, and bean definition patterns.
 */
@DisplayName("Spring Framework Best Practice Rules")
class SpringFrameworkRulesTest extends SW360ArchitectureTest {

    @Test
    @DisplayName("No class should use @Controller — use @RestController instead")
    void noClassShouldUseControllerAnnotation() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..rest.resourceserver..")
                .should().beAnnotatedWith(org.springframework.stereotype.Controller.class)
                .as("Use @RestController (not @Controller) for REST API endpoints");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("@SpringBootApplication should only exist in the root package")
    void springBootApplicationShouldBeInRootPackage() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(SpringBootApplication.class)
                .should().resideInAPackage("..rest.resourceserver")
                .as("@SpringBootApplication class should reside in the root resourceserver package");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("@Configuration classes should not implement business logic interfaces")
    void configurationClassesShouldNotBeServices() {
        ArchRule rule = noClasses()
                .that().areAnnotatedWith(Configuration.class)
                .should().beAnnotatedWith(Service.class)
                .as("@Configuration classes should not also be @Service — separate concerns");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("@Service classes should not be annotated with @RestController")
    void servicesShouldNotBeControllers() {
        ArchRule rule = noClasses()
                .that().areAnnotatedWith(Service.class)
                .should().beAnnotatedWith(RestController.class)
                .as("@Service classes should not also be @RestController — keep layers separate");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("@Component classes should not be annotated with @Service or @RestController")
    void componentsShouldNotBeServicesOrControllers() {
        ArchRule rule = noClasses()
                .that().areAnnotatedWith(Component.class)
                .should().beAnnotatedWith(Service.class)
                .orShould().beAnnotatedWith(RestController.class)
                .as("@Component should not also be @Service or @RestController — " +
                        "use the most specific stereotype");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("@RestController classes should not use @ResponseBody — it is implied")
    void restControllersShouldNotUseResponseBody() {
        ArchRule rule = noClasses()
                .that().areAnnotatedWith(RestController.class)
                .should().beAnnotatedWith(
                        org.springframework.web.bind.annotation.ResponseBody.class)
                .as("@RestController already implies @ResponseBody — do not add it explicitly");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("@RestController classes should not define @Bean methods — use @Configuration instead")
    void controllersShouldNotDefineBeans() {
        ArchCondition<JavaClass> notDeclareBeanMethods =
                new ArchCondition<>("not declare @Bean methods") {
                    @Override
                    public void check(JavaClass javaClass, ConditionEvents events) {
                        javaClass.getMethods().stream()
                                .filter(method -> method.isAnnotatedWith(
                                        org.springframework.context.annotation.Bean.class))
                                .forEach(method -> events.add(SimpleConditionEvent.violated(
                                        javaClass,
                                        String.format("%s.%s() is annotated with @Bean — " +
                                                        "define beans in @Configuration classes, not controllers",
                                                javaClass.getSimpleName(), method.getName()))));
                    }
                };

        ArchRule rule = classes()
                .that().areAnnotatedWith(RestController.class)
                .should(notDeclareBeanMethods)
                .as("@RestController classes should not define @Bean methods — " +
                        "use @Configuration classes for bean definitions");

        rule.check(restClasses);
    }
}

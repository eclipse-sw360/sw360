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
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Validates the naming conventions established by the SW360 project.
 * <p>
 * These rules enforce the naming patterns documented in
 * {@code .github/instructions/sw360_backend.instructions.md}:
 * <ul>
 *   <li>Controllers: {@code *Controller.java}</li>
 *   <li>Services: {@code Sw360*Service.java} or {@code SW360*Service.java}</li>
 *   <li>Configuration: {@code *Configuration.java} or {@code *Customizations.java}</li>
 *   <li>Resource Processors: {@code *ResourceProcessor.java}</li>
 * </ul>
 */
@DisplayName("Naming Convention Rules")
class NamingConventionRulesTest extends SW360ArchitectureTest {

    @Test
    @DisplayName("Classes annotated with @RestController should have name ending with 'Controller'")
    void restControllersShouldBeNamedController() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(RestController.class)
                .should().haveSimpleNameEndingWith("Controller")
                .as("All @RestController classes should be named *Controller");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("Classes annotated with @BasePathAwareController should have name ending with 'Controller'")
    void basePathAwareControllersShouldBeNamedController() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(BasePathAwareController.class)
                .should().haveSimpleNameEndingWith("Controller")
                .as("All @BasePathAwareController classes should be named *Controller");


        rule.check(restClasses);
    }

    @Test
    @DisplayName("Service classes in domain packages should have name ending with 'Service'")
    void serviceClassesShouldBeNamedWithService() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(org.springframework.stereotype.Service.class)
                .and().resideOutsideOfPackage("..resourceserver.core..")
                .and().resideOutsideOfPackage("..resourceserver.security..")
                .and().resideOutsideOfPackage("..resourceserver.cache..")
                .should().haveSimpleNameEndingWith("Service")
                .orShould().haveSimpleNameEndingWith("Services")
                .as("@Service classes in domain packages should have a name ending with 'Service' or 'Services'");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("Configuration classes should have name ending with 'Configuration' or 'Customizations'")
    void configurationClassesShouldBeNamedProperly() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(org.springframework.context.annotation.Configuration.class)
                .and().resideOutsideOfPackage("..resourceserver")
                .and().doNotHaveSimpleName("ReleaseCacheCondition")
                .should().haveSimpleNameEndingWith("Configuration")
                .orShould().haveSimpleNameEndingWith("Customizations")
                .orShould().haveSimpleNameEndingWith("Condition")
                .as("@Configuration classes should be named *Configuration, *Customizations, or *Condition");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("Resource Processors should have name ending with 'ResourceProcessor'")
    void resourceProcessorsShouldBeNamedProperly() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("ResourceProcessor")
                .should().beAnnotatedWith(org.springframework.stereotype.Component.class)
                .as("ResourceProcessor classes should be Spring @Component beans");

        rule.check(restClasses);
    }
}

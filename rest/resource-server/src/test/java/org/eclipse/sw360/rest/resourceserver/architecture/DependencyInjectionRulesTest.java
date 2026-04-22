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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

/**
 * Validates dependency injection patterns in the SW360 REST module.
 * <p>
 * SW360 prefers <strong>constructor injection</strong> via Lombok's
 * {@code @RequiredArgsConstructor} over field-level {@code @Autowired}.
 * <p>
 * Note: Lombok annotations ({@code @RequiredArgsConstructor}, {@code @NonNull})
 * have {@code @Retention(SOURCE)} and are erased after compilation, so ArchUnit
 * cannot verify their presence. Instead, we validate the <em>absence</em> of
 * field-level {@code @Autowired} as a proxy for constructor injection.
 */
@DisplayName("Dependency Injection Rules")
class DependencyInjectionRulesTest extends SW360ArchitectureTest {

    @Test
    @DisplayName("Service classes should not use field injection with @Autowired")
    void serviceClassesShouldNotUseFieldInjection() {
        ArchRule rule = noFields()
                .that().areDeclaredInClassesThat().areAnnotatedWith(Service.class)
                .and().areDeclaredInClassesThat().resideOutsideOfPackage("..resourceserver.security..")
                .should().beAnnotatedWith(Autowired.class)
                .as("@Service classes (outside security package) should not use field-level @Autowired; " +
                        "use constructor injection via @RequiredArgsConstructor instead");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("Spring beans should prefer constructor injection over field injection")
    void springBeansShouldPreferConstructorInjection() {
        ArchCondition<JavaClass> notHaveTooManyAutowiredFields =
                new ArchCondition<>("not have more than one @Autowired field") {
                    @Override
                    public void check(JavaClass javaClass, ConditionEvents events) {
                        long autowiredFieldCount = javaClass.getFields().stream()
                                .filter(field -> field.isAnnotatedWith(Autowired.class))
                                .count();
                        if (autowiredFieldCount > 1) {
                            events.add(SimpleConditionEvent.violated(javaClass,
                                    String.format("%s has %d @Autowired fields; " +
                                                    "consider using @RequiredArgsConstructor",
                                            javaClass.getName(), autowiredFieldCount)));
                        }
                    }
                };

        ArchRule rule = classes()
                .that().areAnnotatedWith(RestController.class)
                .should(notHaveTooManyAutowiredFields)
                .as("Controllers should not have multiple @Autowired fields; " +
                        "use constructor injection via @RequiredArgsConstructor");

        rule.check(restClasses);
    }
}

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
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Enforces that every production Controller and Service class has a
 * corresponding test class.
 * <p>
 * <b>This is the "forced test co-evolution" rule.</b> When a developer adds or
 * modifies a Controller or Service, the build will fail unless a matching test
 * class exists. This guarantees that production code cannot drift away from its
 * tests.
 * <p>
 * Matching rules:
 * <ul>
 *   <li>{@code FooController} -> requires a test class whose name contains
 *       {@code "Foo"} and ends with {@code "Test"} or {@code "SpecTest"}
 *       <br>(e.g., {@code ComponentTest}, {@code ComponentSpecTest})</li>
 *   <li>{@code Sw360FooService} -> requires a test class whose name contains
 *       {@code "Foo"} and ends with {@code "Test"} or {@code "SpecTest"}
 *       <br>(e.g., {@code ComponentTest}, {@code ComponentSpecTest})</li>
 *   <li>For controllers: the number of HTTP-exercising {@code @Test} methods
 *       (those calling {@code TestRestTemplate} or {@code MockMvc}) must be
 *       >= the number of HTTP endpoint methods in the controller</li>
 * </ul>
 * <p>
 * Controllers/services that are explicitly excluded (infra-level or
 * cross-cutting) are listed in {@link #EXCLUDED_CLASSES}.
 */
@DisplayName("Test Coverage Completeness Rules")
class TestCoverageCompletenessRulesTest extends SW360ArchitectureTest {

    /**
     * All test classes imported from the resource-server test sources.
     */
    private static Set<JavaClass> testClasses;

    /**
     * Spring MVC HTTP mapping annotations that define REST endpoints.
     */
    private static final Set<String> HTTP_MAPPING_ANNOTATIONS = Set.of(
            "org.springframework.web.bind.annotation.GetMapping",
            "org.springframework.web.bind.annotation.PostMapping",
            "org.springframework.web.bind.annotation.PutMapping",
            "org.springframework.web.bind.annotation.PatchMapping",
            "org.springframework.web.bind.annotation.DeleteMapping",
            "org.springframework.web.bind.annotation.RequestMapping"
    );

    /**
     * JUnit test annotations (both JUnit 4 and JUnit 5).
     */
    private static final Set<String> TEST_ANNOTATIONS = Set.of(
            "org.junit.Test",
            "org.junit.jupiter.api.Test"
    );

    /**
     * HTTP client class/method pairs that indicate a test is actually
     * exercising a REST endpoint -- not just a trivial assertion.
     * <p>
     * Key = fully qualified class name, Value = set of method names.
     */
    private static final Map<String, Set<String>> HTTP_CLIENT_METHODS = Map.of(
            "org.springframework.boot.resttestclient.TestRestTemplate",
            Set.of("exchange", "getForEntity", "getForObject",
                    "postForEntity", "postForObject", "put", "delete",
                    "patchForObject"),
            "org.springframework.test.web.servlet.MockMvc",
            Set.of("perform")
    );

    /**
     * Production classes that are infrastructure-level and do not require a
     * dedicated domain-level test class.
     */
    private static final Set<String> EXCLUDED_CLASSES = Set.of(
            // Infrastructure / framework controller (no domain logic)
            "VersionController",
            // Cross-cutting service tested transitively
            "Sw360CustomUserDetailsService",
            // TODO: Add tests for these classes (pre-existing gaps)
            "LicenseInfoController",        // 0 endpoints -- stub controller
            "SW360ReportController",        // 2 endpoints -- no tests
            "Sw360LicenseInfoService",
            "SW360ReportService",
            "SW360SPDXDocumentService"
    );

    /**
     * Controllers excluded from the endpoint-to-test ratio check (Rule 3)
     * because they have known gaps that predate this rule.
     * <p>
     * Each entry is the simple class name with a comment showing the gap.
     * As tests are added, entries should be removed from this list.
     */
    private static final Set<String> ENDPOINT_RATIO_EXCLUDED = Set.of(
            // TODO: Add 1 more test to cover all 3 endpoints
            "FossologyAdminController",  // 3 endpoints, 2 tests -- 1 gap
            // Test exists (CleanUpAttachmentSpecTest) but reversed naming
            "AttachmentCleanUpController"
    );

    @BeforeAll
    static void importTestClasses() {
        testClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.ONLY_INCLUDE_TESTS)
                .importPackages("org.eclipse.sw360.rest.resourceserver")
                .stream()
                .filter(c -> c.getSimpleName().endsWith("Test")
                        || c.getSimpleName().endsWith("SpecTest"))
                .collect(Collectors.toSet());
    }

    // =========================================================================
    //  Rule 1: Every Controller must have a corresponding test class
    // =========================================================================

    @Test
    @DisplayName("Every REST controller must have a corresponding test class")
    void everyControllerShouldHaveATest() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Controller")
                .and().resideInAPackage("..rest.resourceserver..")
                .and().areNotInterfaces()
                .should(haveCorrespondingTestClass("Controller"))
                .as("Every *Controller class must have a matching *Test or *SpecTest " +
                        "-- add or update the test when modifying the controller");

        rule.check(restClasses);
    }

    // =========================================================================
    //  Rule 2: Every Service (Sw360* or SW360*) must have a corresponding test
    // =========================================================================

    @Test
    @DisplayName("Every Sw360*Service must have a corresponding test class")
    void everyServiceShouldHaveATest() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Service")
                .and().resideInAPackage("..rest.resourceserver..")
                .and().areNotInterfaces()
                .should(haveCorrespondingTestClass("Service"))
                .as("Every *Service class must have a matching *Test or *SpecTest " +
                        "-- add or update the test when modifying the service");

        rule.check(restClasses);
    }

    // =========================================================================
    //  Rule 3: Every API endpoint method must have at least one test method
    // =========================================================================

    @Test
    @DisplayName("Every controller endpoint should have at least one test method")
    void everyEndpointShouldHaveAtLeastOneTest() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Controller")
                .and().resideInAPackage("..rest.resourceserver..")
                .and().areNotInterfaces()
                .should(haveTestMethodsForEveryEndpoint())
                .as("The number of HTTP-exercising @Test methods must be " +
                        ">= the number of HTTP endpoint methods in the controller");

        rule.check(restClasses);
    }

    // =========================================================================
    //  Helpers
    // =========================================================================

    /**
     * Derives the domain keyword from a production class name by stripping
     * the {@code Sw360}/{@code SW360} prefix and the given suffix.
     * <p>
     * Examples:
     * <ul>
     *   <li>{@code ComponentController} -> {@code "Component"}</li>
     *   <li>{@code Sw360ComponentService} -> {@code "Component"}</li>
     *   <li>{@code SW360PackageService} -> {@code "Package"}</li>
     *   <li>{@code ScheduleAdminController} -> {@code "ScheduleAdmin"}</li>
     * </ul>
     */
    private static String deriveDomainKeyword(String simpleName, String suffix) {
        String keyword = simpleName;
        for (String prefix : List.of("Sw360", "SW360")) {
            if (keyword.startsWith(prefix)) {
                keyword = keyword.substring(prefix.length());
                break;
            }
        }
        if (keyword.endsWith(suffix)) {
            keyword = keyword.substring(0, keyword.length() - suffix.length());
        }
        return keyword;
    }

    /**
     * Builds a stream of keywords to match against test class names.
     * For compound names ending with "Admin" (e.g., "ScheduleAdmin"),
     * also tries the core keyword ("Schedule") to match test classes
     * like {@code ScheduleTest}.
     */
    private static Stream<String> expandKeywords(String keyword) {
        if (keyword.endsWith("Admin") && keyword.length() > "Admin".length()) {
            return Stream.of(keyword, keyword.substring(0, keyword.length() - "Admin".length()));
        }
        return Stream.of(keyword);
    }

    /**
     * Returns all test classes whose name contains any of the expanded keywords.
     */
    private static Stream<JavaClass> findMatchingTestClasses(String keyword) {
        List<String> keywords = expandKeywords(keyword).toList();
        return testClasses.stream()
                .filter(tc -> keywords.stream().anyMatch(kw -> tc.getSimpleName().contains(kw)));
    }

    /**
     * Checks whether any annotation on the method belongs to the given set.
     */
    private static boolean hasAnyAnnotation(JavaMethod method, Set<String> annotationNames) {
        return method.getAnnotations().stream()
                .anyMatch(ann -> annotationNames.contains(ann.getRawType().getName()));
    }

    /**
     * Rule 1 & 2: Checks that a corresponding test class exists.
     */
    private static ArchCondition<JavaClass> haveCorrespondingTestClass(String suffix) {
        return new ArchCondition<>("have a corresponding test class") {
            @Override
            public void check(JavaClass productionClass, ConditionEvents events) {
                String simpleName = productionClass.getSimpleName();
                if (EXCLUDED_CLASSES.contains(simpleName)) {
                    return;
                }

                String keyword = deriveDomainKeyword(simpleName, suffix);
                if (keyword.isEmpty()) {
                    return;
                }

                if (findMatchingTestClasses(keyword).findAny().isEmpty()) {
                    events.add(SimpleConditionEvent.violated(
                            productionClass,
                            String.format(
                                    "%s has no corresponding test class -- " +
                                            "expected a test class containing '%s' " +
                                            "and ending with 'Test' or 'SpecTest' " +
                                            "(e.g., %sTest, %sSpecTest)",
                                    simpleName, keyword, keyword, keyword)));
                }
            }
        };
    }

    /**
     * Checks whether a {@code @Test} method actually makes an HTTP call by
     * inspecting its bytecode-level method calls for known HTTP client
     * invocations ({@code TestRestTemplate.exchange},
     * {@code MockMvc.perform}, etc.).
     * <p>
     * This prevents gaming the endpoint ratio rule by adding trivial tests
     * that don't exercise any REST endpoint.
     */
    private static boolean isHttpTestMethod(JavaMethod method) {
        if (!hasAnyAnnotation(method, TEST_ANNOTATIONS)) {
            return false;
        }
        return method.getMethodCallsFromSelf().stream()
                .anyMatch(call -> {
                    String ownerName = call.getTargetOwner().getName();
                    String methodName = call.getTarget().getName();
                    Set<String> methods = HTTP_CLIENT_METHODS.get(ownerName);
                    return methods != null && methods.contains(methodName);
                });
    }

    /**
     * Rule 3: Checks that the number of HTTP-exercising {@code @Test} methods
     * across matching test classes is >= the number of HTTP endpoint methods
     * in the controller.
     * <p>
     * Only test methods that invoke {@code TestRestTemplate} or
     * {@code MockMvc} are counted -- trivial tests without HTTP calls are
     * excluded. This ensures you cannot satisfy the rule by adding tests
     * that don't actually exercise REST endpoints.
     * <p>
     * Controllers with pre-existing gaps are tracked in
     * {@link #ENDPOINT_RATIO_EXCLUDED}.
     */
    private static ArchCondition<JavaClass> haveTestMethodsForEveryEndpoint() {
        return new ArchCondition<>("have HTTP-exercising test methods for every endpoint") {
            @Override
            public void check(JavaClass controllerClass, ConditionEvents events) {
                String simpleName = controllerClass.getSimpleName();
                if (EXCLUDED_CLASSES.contains(simpleName)
                        || ENDPOINT_RATIO_EXCLUDED.contains(simpleName)) {
                    return;
                }

                String keyword = deriveDomainKeyword(simpleName, "Controller");
                if (keyword.isEmpty()) {
                    return;
                }

                long endpointCount = controllerClass.getMethods().stream()
                        .filter(m -> hasAnyAnnotation(m, HTTP_MAPPING_ANNOTATIONS))
                        .count();
                if (endpointCount == 0) {
                    return;
                }

                long httpTestCount = findMatchingTestClasses(keyword)
                        .flatMap(tc -> tc.getMethods().stream())
                        .filter(TestCoverageCompletenessRulesTest::isHttpTestMethod)
                        .count();

                if (httpTestCount < endpointCount) {
                    events.add(SimpleConditionEvent.violated(
                            controllerClass,
                            String.format(
                                    "%s has %d endpoint(s) but only %d HTTP-exercising " +
                                            "test method(s) -- each endpoint needs a @Test " +
                                            "that calls TestRestTemplate or MockMvc",
                                    simpleName,
                                    endpointCount,
                                    httpTestCount)));
                }
            }
        };
    }
}

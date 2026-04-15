<!--
Part of the SW360 Portal Project.

SPDX-License-Identifier: EPL-2.0
-->

# SW360 ArchUnit Test Summary

> **Last Updated:** April 15, 2026
> **Module:** `rest/resource-server`

This document provides a comprehensive overview of all ArchUnit architecture tests in the SW360 REST resource-server module. These tests enforce architectural patterns, coding standards, and best practices.

---

## Test Suite Overview

| Test Suite | Test Count | Purpose |
|------------|------------|---------|
| [Controller Annotation Rules](#1-controller-annotation-rules) | 6 | Validates REST controller annotations |
| [Controller-Service Relationship Rules](#2-controller-service-relationship-rules) | 3 | Ensures proper controller-service patterns |
| [Dependency Injection Rules](#3-dependency-injection-rules) | 2 | Enforces constructor injection |
| [Layered Architecture Rules](#4-layered-architecture-rules) | 3 | Maintains layer separation |
| [Logging Standard Rules](#5-logging-standard-rules) | 5 | Enforces Log4j2 logging standards |
| [Naming Convention Rules](#6-naming-convention-rules) | 5 | Validates class naming patterns |
| [OpenAPI Documentation Rules](#7-openapi-documentation-rules) | 2 | Ensures OpenAPI documentation |
| [Package Structure Rules](#8-package-structure-rules) | 5 | Enforces package organization |
| [Security Annotation Rules](#9-security-annotation-rules) | 4 | Validates security annotations |
| [Spring Framework Rules](#10-spring-framework-rules) | 7 | Enforces Spring best practices |
| [Thrift Service Boundary Rules](#11-thrift-service-boundary-rules) | 3 | Prevents bypassing Thrift layer |
| [Coding Standard Rules](#12-coding-standard-rules) | 14 | General coding standards & modern Java |
| [Resource Management Rules](#13-resource-management-rules) | 9 | Resource lifecycle & concurrency |
| [Dependency Governance Rules](#14-dependency-governance-rules) | 6 | Third-party dependency allowlist & bans |
| [Test Coverage Completeness Rules](#15-test-coverage-completeness-rules) | 3 | Forced test co-evolution |
| **Total** | **77** | **Complete architecture validation** |

---

## Detailed Test Descriptions

### 1. Controller Annotation Rules
**File:** `ControllerAnnotationRulesTest.java`

| Test Name | Description |
|-----------|-------------|
| `basePathAwareControllerShouldBeRestController` | Controllers with `@BasePathAwareController` must also have `@RestController` (except `LicenseInfoController`) |
| `controllersShouldHaveSecurityRequirement` | Controllers must declare `@SecurityRequirement` or `@SecurityRequirements` for OpenAPI docs (except `VersionController` and `AttachmentCleanUpController`) |
| `controllersShouldImplementRepresentationModelProcessor` | Controllers must implement `RepresentationModelProcessor` for HAL resource link registration (except `VersionController`) |
| `controllerAdviceShouldResideInCore` | `@ControllerAdvice` exception handlers must reside in the `core` package |
| `controllersShouldNotExtendOtherControllers` | REST controllers should not extend other controllers — prefer composition via service injection |
| `controllersShouldUseHateoasTypes` | REST controllers must depend on Spring HATEOAS types (`EntityModel`, `CollectionModel`, `HalResource`) for HAL+JSON response structure (except `VersionController`) |

---

### 2. Controller-Service Relationship Rules
**File:** `ControllerServiceRelationshipRulesTest.java`

| Test Name | Description |
|-----------|-------------|
| `controllersShouldDeclareUrlConstant` | Controllers must declare a static `*_URL` constant for their base path (except `VersionController` and `SW360ConfigurationsController`) |
| `controllersShouldInjectRestControllerHelper` | Controllers must inject `RestControllerHelper` for user authentication and pagination (except `VersionController`) |
| `servicesShouldNotDependOnControllers` | `@Service` classes (except `RestControllerHelper` and `Sw360ProjectService`) must not depend on `@RestController` classes |

---

### 3. Dependency Injection Rules
**File:** `DependencyInjectionRulesTest.java`

| Test Name | Description |
|-----------|-------------|
| `serviceClassesShouldNotUseFieldInjection` | `@Service` classes (outside security package) must not use field-level `@Autowired`; prefer constructor injection via `@RequiredArgsConstructor` |
| `springBeansShouldPreferConstructorInjection` | Controllers should not have more than one `@Autowired` field; use `@RequiredArgsConstructor` instead |

---

### 4. Layered Architecture Rules
**File:** `LayerDependencyRulesTest.java`

| Test Name | Description |
|-----------|-------------|
| `securityShouldNotDependOnControllers` | Security package must not depend on any domain-specific controller or service package |
| `coreShouldNotDependOnDomainPackages` | Core package (except `JacksonCustomizations`, `RestControllerHelper`, `AwareOfRestServices`, `ThriftServiceProvider`, and custom `Serializer` classes) must not depend on domain-specific packages |
| `filterShouldNotDependOnDomainPackages` | Filter package must not depend on domain-specific packages |

---

### 5. Logging Standard Rules
**File:** `LoggingStandardRulesTest.java`

| Test Name | Description |
|-----------|-------------|
| `noClassShouldUseSystemOut` | No class should use `System.out` — use Log4j2 logger instead |
| `noClassShouldUseSystemErr` | No class should use `System.err` — use Log4j2 logger instead |
| `noClassShouldCallPrintStackTrace` | No class should call `printStackTrace()` — use `log.error()` with exception parameter |
| `noClassShouldUseJavaUtilLogging` | No class should use `java.util.logging` — use Log4j2 (`LogManager.getLogger()`) or Lombok `@Slf4j` |
| `noClassShouldUseCommonsLogging` | No class should use Apache Commons Logging directly — use Log4j2 or Lombok `@Slf4j` |

---

### 6. Naming Convention Rules
**File:** `NamingConventionRulesTest.java`

| Test Name | Description |
|-----------|-------------|
| `restControllersShouldBeNamedController` | Classes with `@RestController` must have names ending with `Controller` |
| `basePathAwareControllersShouldBeNamedController` | Classes with `@BasePathAwareController` must have names ending with `Controller` |
| `serviceClassesShouldBeNamedWithService` | `@Service` classes in domain packages must have names ending with `Service` or `Services` |
| `configurationClassesShouldBeNamedProperly` | `@Configuration` classes should be named `*Configuration` or `*Customizations` |
| `resourceProcessorsShouldBeNamedProperly` | `ResourceProcessor` classes must be Spring `@Component` beans |

---

### 7. OpenAPI Documentation Rules
**File:** `OpenApiDocumentationRulesTest.java`

| Test Name | Description |
|-----------|-------------|
| `endpointMethodsShouldHaveOperationAnnotation` | All REST endpoint methods (`@GetMapping`, `@PostMapping`, etc.) must have `@Operation` annotation for OpenAPI documentation |
| `controllersShouldDeclareSecurityRequirementForSwagger` | REST controllers must declare `@SecurityRequirement` at class or method level (except `VersionController` and `AttachmentCleanUpController`) |

---

### 8. Package Structure Rules
**File:** `PackageStructureRulesTest.java`

| Test Name | Description |
|-----------|-------------|
| `controllersShouldResideInDomainPackages` | Controller classes must reside in domain packages (e.g., `..project.ProjectController`), not in the `core` package |
| `serializersShouldResideInCoreSerializerPackage` | Custom JSON serializer classes must reside in the `core.serializer` package |
| `exceptionClassesShouldResideInCore` | Custom exception classes must reside in the `core` package |
| `securityClassesShouldResideInSecurityPackage` | Authentication-related classes must reside in the `security` or `core` package |
| `noClassesShouldDependOnInternalJdkPackages` | No class should depend on internal JDK (`sun..`) packages |

---

### 9. Security Annotation Rules
**File:** `SecurityAnnotationRulesTest.java`

| Test Name | Description |
|-----------|-------------|
| `noClassShouldUseSecuredAnnotation` | No class should use deprecated `@Secured` — use `@PreAuthorize` instead |
| `noClassShouldUseRolesAllowedAnnotation` | No class should use `@RolesAllowed` — use `@PreAuthorize` instead |
| `noClassShouldUseDeprecatedMethodSecurityAnnotation` | No class should use deprecated `@EnableGlobalMethodSecurity` — use `@EnableMethodSecurity` (Spring Security 6.x) |
| `preAuthorizeValuesShouldUseKnownAuthorities` | `@PreAuthorize` annotations must only reference known SW360 authorities: `ADMIN`, `WRITE`, or `READ` |

---

### 10. Spring Framework Rules
**File:** `SpringFrameworkRulesTest.java`

| Test Name | Description |
|-----------|-------------|
| `noClassShouldUseControllerAnnotation` | No class should use `@Controller` — use `@RestController` instead |
| `springBootApplicationShouldBeInRootPackage` | `@SpringBootApplication` class must reside in the root `resourceserver` package |
| `configurationClassesShouldNotBeServices` | `@Configuration` classes must not also be `@Service` — separate concerns |
| `servicesShouldNotBeControllers` | `@Service` classes must not also be `@RestController` — keep layers separate |
| `componentsShouldNotBeServicesOrControllers` | `@Component` should not also be `@Service` or `@RestController` — use the most specific stereotype |
| `restControllersShouldNotUseResponseBody` | `@RestController` already implies `@ResponseBody` — do not add it explicitly |
| `controllersShouldNotDefineBeans` | `@RestController` classes should not define `@Bean` methods — use `@Configuration` classes for bean definitions |

---

### 11. Thrift Service Boundary Rules
**File:** `ThriftServiceBoundaryRulesTest.java`

| Test Name | Description |
|-----------|-------------|
| `restModuleShouldNotAccessDatabaseHandlers` | REST module must not bypass Thrift services to access database handlers directly |
| `restModuleShouldNotAccessRepositories` | REST module must not access backend repository classes in the `datahandler.db` package |
| `restModuleShouldNotAccessCouchDbDirectly` | REST module (except `SW360RestHealthIndicator`) must not access CouchDB/Cloudant client classes directly — use Thrift services via `ThriftClients` |

---

### 12. Coding Standard Rules
**File:** `CodingStandardRulesTest.java`

| Test Name | Description |
|-----------|-------------|
| `exceptionClassesShouldBeNamedProperly` | Custom exception classes must have names ending with `Exception` |
| `noClassShouldDependOnJavaLangError` | Classes outside `core`, `security` packages and `ResourceProcessor` classes should not depend on `java.lang.Error` subtypes — use specific exception types |
| `interfacesShouldNotHaveIPrefix` | Interfaces should not use `I` prefix — follow Java naming conventions |
| `constantsClassesShouldBeFinal` | Constants classes must be declared `final` (allows empty — validates future additions) |
| `noClassShouldDependOnJavaxServlet` | Use `jakarta.servlet` (not `javax.servlet`) — SW360 runs on Spring Boot 3.x / Jakarta EE |
| `noClassShouldDependOnJavaxAnnotationNullable` | Use `lombok.NonNull` instead of `javax.annotation.Nullable` |
| `noClassShouldUseStringBuffer` | Use `StringBuilder` (not `StringBuffer`) — `StringBuffer` is synchronized and slower |
| `noClassShouldUseLegacyDateAPI` | Use `java.time` API (`LocalDate`, `Instant`, etc.) instead of legacy `Calendar` |
| `noClassShouldUseEnumeration` | Use `Iterator` (not `Enumeration`) — `Enumeration` is legacy |
| `noClassShouldUseObservable` | Use `PropertyChangeListener` or reactive patterns (not `Observable`) — deprecated since Java 9 |
| `noClassShouldDependOnRawCollectionTypes` | Use `Map` (not `Dictionary`) — `Dictionary` is abstract and legacy |
| `noClassShouldDependOnSunPackages` | Do not use internal JDK `sun.*` packages — not part of the public API |
| `noClassShouldDependOnComSunPackages` | Do not use internal `com.sun.*` packages — not part of the public API |
| `utilityClassesShouldBeFinal` | Utility classes (`*Utils`, `*Util`) that are not Spring beans should be declared `final` |

---

### 13. Resource Management Rules
**File:** `ResourceManagementRulesTest.java`

| Test Name | Description |
|-----------|-------------|
| `noClassShouldUseFinalizeMethod` | Classes must not override `finalize()` — use try-with-resources or `java.lang.ref.Cleaner` instead |
| `noClassShouldUseShutdownHookInBusinessLogic` | Business logic classes must not use `Runtime.addShutdownHook()` — use Spring lifecycle (`@PreDestroy`, `DisposableBean`) |
| `controllersShouldNotCreateThreadsDirectly` | Controllers must not directly instantiate `Thread` — use `@Async` or Spring-managed `ExecutorService` |
| `servicesShouldNotCreateExecutorsDirect` | `@Service` classes should not use `Executors` factory directly — inject Spring's `TaskExecutor` (⚠️ `Sw360ProjectService` and `Sw360ReleaseService` excluded pending refactoring) |
| `classesShouldPreferPathOverFileConstructor` | Controllers should not use `FileReader` directly — use `Files.newBufferedReader(Path)` with try-with-resources |
| `controllersShouldNotUseFileInputStreamDirectly` | Controllers should not use `FileInputStream` — delegate to service layer or use Spring's `Resource` abstraction |
| `controllersShouldNotUseFileOutputStreamDirectly` | Controllers should not use `FileOutputStream` — delegate file operations to service layer |
| `controllersShouldNotHaveMutableStaticFields` | Controllers must not have mutable static fields (excluding loggers) — controllers must be stateless |
| `classesShouldNotCatchThrowable` | Classes should not catch `Throwable` — catch specific exception types. Compiler-generated Throwable catches from try-with-resources are automatically filtered out (⚠️ `Sw360ReleaseService` and `Sw360LicenseService` excluded pending refactoring) |

---

### 14. Dependency Governance Rules
**File:** `DependencyGovernanceRulesTest.java`

| Test Name | Description |
|-----------|-------------|
| `restModuleShouldOnlyUseApprovedLibraries` | REST module must only depend on approved third-party libraries (allowlist approach) — any new dependency must be explicitly added to `ALLOWED_PACKAGE_PREFIXES` |
| `noClassShouldUseApacheHttpClient` | Do not use Apache HttpClient directly — use Spring's `RestTemplate` or `WebClient` for HTTP calls |
| `noClassShouldUseOkHttp` | Do not use OkHttp directly — use Spring's `RestTemplate` or `WebClient` for HTTP calls |
| `noClassShouldUseGsonDirectly` | Do not use Gson directly — Jackson is the project standard for JSON processing (⚠️ `ProjectController` and `SW360ReportController` excluded pending refactoring) |
| `noClassShouldUseOrgJson` | Do not use `org.json` directly — Jackson is the project standard for JSON processing |
| `noClassShouldUseJUnit4Assertions` | Do not use JUnit 4 assertions — use JUnit 5 Assertions or AssertJ |

**Approved Third-Party Libraries (Allowlist):**

| Package Prefix | Library | Justification |
|----------------|---------|---------------|
| `java.` | Java Platform | JDK standard library |
| `jakarta.` | Jakarta EE | Servlet, validation, annotations (Spring Boot 3.x) |
| `org.eclipse.sw360.` | SW360 | Project's own packages |
| `org.springframework.` | Spring Framework | Core framework |
| `org.springdoc.` | SpringDoc OpenAPI | OpenAPI documentation generation |
| `org.apache.thrift.` | Apache Thrift | Thrift RPC protocol |
| `org.apache.logging.` | Log4j2 | Logging framework |
| `org.apache.commons.` | Apache Commons | Utilities (lang3, io, csv, text) |
| `org.slf4j.` | SLF4J | Logging facade |
| `com.fasterxml.jackson.` | Jackson | JSON serialization (project standard) |
| `com.google.common.` | Guava | Google utilities |
| `com.google.gson.` | Gson | JSON (transitive via Cloudant SDK) |
| `io.swagger.` | Swagger/OpenAPI | OpenAPI annotations and models |
| `lombok.` | Lombok | Boilerplate reduction |
| `org.jetbrains.` | JetBrains Annotations | `@NotNull`, `@Nullable` annotations |
| `org.jose4j.` | jose4j | JWT/JWKS validation |
| `tools.jackson.` | Jackson 3.x | JSON serialization (Spring Boot 4.x) |

---

### 15. Test Coverage Completeness Rules
**File:** `TestCoverageCompletenessRulesTest.java`

| Test Name | Description |
|-----------|-------------|
| `everyControllerShouldHaveATest` | Every `*Controller` class must have a corresponding test class whose name contains the domain keyword and ends with `Test` or `SpecTest` (e.g., `ComponentController` -> `ComponentTest`, `ComponentSpecTest`) |
| `everyServiceShouldHaveATest` | Every `Sw360*Service` / `SW360*Service` class must have a corresponding test class (same naming convention as controllers, after stripping the `Sw360`/`SW360` prefix) |
| `everyEndpointShouldHaveAtLeastOneTest` | The number of HTTP-exercising `@Test` methods (those calling `TestRestTemplate` or `MockMvc`, detected via bytecode method-call analysis) must be >= the number of `@XxxMapping` endpoint methods in each controller. Trivial tests without HTTP calls are not counted |

**Pre-existing exclusions:**

| Constant | Classes | Reason |
|----------|---------|--------|
| `EXCLUDED_CLASSES` | `VersionController`, `Sw360CustomUserDetailsService`, `LicenseInfoController`, `SW360ReportController`, `Sw360LicenseInfoService`, `SW360ReportService`, `SW360SPDXDocumentService` | Infrastructure or pre-existing gaps (TODO markers) |
| `ENDPOINT_RATIO_EXCLUDED` | `FossologyAdminController`, `AttachmentCleanUpController` | Known endpoint-to-test ratio gaps |

---

## Running the Tests

### Run All Architecture Tests
```bash
mvn test -pl rest/resource-server -Dtest="*ArchitectureTest,*RulesTest"
```

### Run Specific Test Suite
```bash
# Example: Run only Controller Annotation Rules
mvn test -pl rest/resource-server -Dtest="ControllerAnnotationRulesTest"
```


---

## Common Exclusions

Some classes are intentionally excluded from certain rules due to legacy patterns or special requirements:

| Class | Excluded From | Reason |
|-------|---------------|--------|
| `LicenseInfoController` | Controller annotations | Uses `@BasePathAwareController` without `@RestController` |
| `VersionController` | Security requirements, URL constants | Public endpoint, minimal controller |
| `AttachmentCleanUpController` | Security requirements | Internal admin utility |
| `JacksonCustomizations` | Core-to-domain dependency | Intentionally references domain mixins for JSON serialization |
| `Json*Serializer` | Core-to-domain dependency | Custom serializers in `core.serializer` reference domain controllers for link building |
| `RestControllerHelper` | Service-to-controller dependency | Helper class that bridges layers |
| `Sw360ProjectService` | Service-to-controller dependency, Executors | Builds embedded HAL resources; uses `Executors.newFixedThreadPool()` |
| `Sw360ReleaseService` | Catch Throwable, Executors | Uses `Executors.newSingleThreadScheduledExecutor()` for FOSSology; explicit `catch(Throwable)` |
| `Sw360LicenseService` | Catch Throwable | Explicit `catch(Throwable)` in bulk download |
| `ProjectController` | Gson ban | Uses Gson directly (pending refactoring to Jackson) |
| `SW360ReportController` | Gson ban | Uses Gson directly (pending refactoring to Jackson) |
| `ApiResponseCacheManager` | Service naming | Cache manager, not a typical service |
| `SW360RestHealthIndicator` | CouchDB direct access | Needs direct DB connection for health checks |
| `SW360ConfigurationsController` | URL constant | Configuration endpoint |
| `ReleaseCacheCondition` | Configuration naming | Spring `Condition` class, not a traditional configuration |

---

## Adding New ArchUnit Tests

When adding new architecture rules:

1. **Create a new test class** extending `SW360ArchitectureTest`
2. **Add `@DisplayName` annotations** for clear test descriptions
3. **Use meaningful test method names** following the pattern: `what + should + constraint`
4. **Update this summary document** with the new test details
5. **Document exclusions** in the test class JavaDoc if needed

### Example Template
```java
@DisplayName("Your Rule Category")
class YourRulesTest extends SW360ArchitectureTest {

    @Test
    @DisplayName("Clear description of what is being validated")
    void descriptiveTestMethodName() {
        ArchRule rule = classes()
                .that()...
                .should()...
                .as("Human-readable rule description");

        rule.check(restClasses);
    }
}
```

---

## Related Documentation

- [SW360 Backend Instructions](../../../../../../../../.github/instructions/sw360_backend.instructions.md)
- [Git Commit Instructions](../../../../../../../../.github/instructions/git-commit.instructions.md)
- [ArchUnit User Guide](https://www.archunit.org/userguide/html/000_Index.html)

---

**Note:** This summary is regenerated based on actual test files. Keep it up to date when adding or modifying architecture tests.

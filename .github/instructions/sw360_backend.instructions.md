---
applyTo: "backend/**,rest/**,libraries/**,keycloak/**,clients/**"
---

# SW360 Backend Instructions

> **Optimized for GitHub Copilot ASK, EDIT, and Agent modes — Java 21 · Spring Boot 4.1 · Spring Security 7.1**

---

## Project Overview

| Attribute   | Details |
|-------------|---------|
| **Purpose** | Eclipse SW360 — SBOM and license compliance management for open-source software |
| **Version** | 20.1.x (current stable: 20.1.0) |
| **Stack**   | Java 21 · Spring Boot 4.1 · Spring Security 7.1 · Maven · CouchDB (Cloudant Java SDK) · Thrift 0.20.0 · Docker |
| **License** | EPL-2.0 |
| **Repo**    | [github.com/eclipse-sw360/sw360](https://github.com/eclipse-sw360/sw360) |

### Module Structure
```
sw360/
├── backend/          # Thrift services (components, licenses, projects, vulnerabilities, etc.)
├── libraries/        # Shared libs (datahandler, thrift, commonIO, exporters, importers)
├── rest/             # REST API (resource-server, authorization-server)
├── clients/          # Java client SDK for SW360 REST API
├── keycloak/         # Keycloak 26.x user-storage provider and event listeners
├── config/           # CouchDB, Keycloak, SW360 configurations
├── scripts/          # Build, test, migration, and utility scripts
└── third-party/      # Thrift compiler, licenses
```

---

## Architecture & Layering

```
React UI → REST Controllers → Sw360*Service → ThriftClients → Backend Handlers → Repositories → CouchDB
                                    │
                                    └── Uses Thrift interfaces for cross-service communication
```

> **Rule:** Only Repositories communicate with CouchDB directly. Backend Handlers must go through Repositories.

### Layer Naming Conventions

| Layer | Pattern | Example |
|-------|---------|---------|
| REST Controller | `*Controller.java` | `ProjectController`, `ComponentController` |
| REST Service | `Sw360*Service.java` | `Sw360ProjectService`, `Sw360ReleaseService` |
| Thrift Handler | `*Handler.java` | `ComponentHandler`, `ProjectHandler` |
| DB Handler | `*DatabaseHandler.java` | `ComponentDatabaseHandler` |
| Repository | `*Repository.java` | `ComponentRepository`, `ProjectRepository` |
| Search Handler | `*SearchHandler.java` | `ComponentSearchHandler` |

### Design Conventions

- **DI:** Constructor injection with `@RequiredArgsConstructor` (Lombok) — never `@Autowired` on fields
- **Logging:** Log4j2 `LogManager.getLogger()` — never `System.out` or `e.printStackTrace()`
- **DTOs:** Thrift-generated objects in `libraries/datahandler/src/main/thrift/`
- **Exception Handling:** Global `@ControllerAdvice` in `RestExceptionHandler.java`
- **Security:** `@PreAuthorize("hasAuthority('WRITE')")` or `hasAuthority('ADMIN')` on write/delete endpoints

### User Roles (`UserGroup` enum)

```
ADMIN  >  SW360_ADMIN  >  SECURITY_ADMIN / ECC_ADMIN / CLEARING_ADMIN  >  CLEARING_EXPERT  >  USER
```

---

## Developer Workflows

### Build Commands

```bash
# Full build (skip tests)
mvn package -P deploy -DskipTests

# Build with Tomcat deploy directory
mvn package -P deploy -DskipTests -Dbase.deploy.dir=$TOMCAT_HOME

# Docker build
./docker_build.sh
./docker_build.sh --cvesearch-host http://cve-search:5000

# Run with Docker Compose
docker-compose up -d
```

### Test Commands

```bash
./scripts/startCouchdbForTests.sh   # Start CouchDB (Docker required)
mvn test                             # All tests
mvn test -pl rest/resource-server    # Module-specific
mvn test -pl backend/components

# Run ArchUnit coverage check
mvn -pl rest/resource-server test \
  -Dtest="org.eclipse.sw360.rest.resourceserver.architecture.TestCoverageCompletenessRulesTest"
```

### Code Quality

```bash
pip install pre-commit && pre-commit install   # One-time setup
mvn spotless:apply                             # Format changed files (default: vs origin/main)
mvn spotless:check                             # Check changed files
mvn spotless:check -Dspotless.ratchetFrom=    # Audit entire repo (use sparingly)
mvn license:check                              # Verify EPL-2.0 headers
```

> **Spotless rules (minimal):** tabs → 4 spaces, trim trailing whitespace, newline at EOF. No full formatter rewrite.

### Thrift Generation

```bash
./scripts/install-thrift.sh                  # Install Thrift 0.20.0 if needed
mvn generate-sources -pl libraries/datahandler  # Regenerate after .thrift changes
# Thrift definitions: libraries/datahandler/src/main/thrift/
```

---

## Domain Concepts & Key Types

### Core Entities (Thrift-generated)

| Entity | Description | Key Fields |
|--------|-------------|------------|
| `Component` | Software component | name, componentType, vendor |
| `Release` | Versioned component with source | version, clearingState, mainLicenseIds |
| `Project` | Collection of releases | name, version, state, linkedReleases |
| `License` | License definition | shortName, fullName, obligations |
| `Obligation` | License obligation | title, text, obligationLevel |
| `Vulnerability` | CVE/security issue | externalId, cvss, references |
| `Package` | Release distribution with pURL | name, version, purl |
| `Attachment` | File attachment | filename, attachmentType, checkStatus |
| `User` | Application user | email, department, userGroup |

### Key Enums

- `ClearingState`: `NEW_CLEARING`, `UNDER_CLEARING`, `REPORT_AVAILABLE`, `APPROVED`
- `ProjectClearingState`: `OPEN`, `IN_PROGRESS`, `CLOSED`
- `AttachmentType`: `SOURCE`, `BINARY`, `CLEARING_REPORT`, `COMPONENT_LICENSE_INFO_XML`
- `ReleaseRelationship`: `CONTAINED`, `REFERRED`, `UNKNOWN`, `DYNAMICALLY_LINKED`, `STATICALLY_LINKED`

---

## REST API Structure

### Endpoint Map: `/api/<resource>`

| Resource | Controller | Service |
|----------|------------|---------|
| `/api/projects` | `ProjectController` | `Sw360ProjectService` |
| `/api/components` | `ComponentController` | `Sw360ComponentService` |
| `/api/releases` | `ReleaseController` | `Sw360ReleaseService` |
| `/api/licenses` | `LicenseController` | `Sw360LicenseService` |
| `/api/vulnerabilities` | `VulnerabilityController` | `Sw360VulnerabilityService` |
| `/api/packages` | `PackageController` | `SW360PackageService` |
| `/api/users` | `UserController` | `Sw360UserService` |
| `/api/vendors` | `VendorController` | `Sw360VendorService` |
| `/api/obligations` | `ObligationController` | `Sw360ObligationService` |
| `/api/ecc` | `EccController` | `Sw360ReleaseService` |

> **Note on legacy prefix:** most services use `Sw360*` (mixed-case).
> A few older ones use the all-caps `SW360*` prefix
> (`SW360PackageService`, `SW360ReportService`, `SW360SPDXDocumentService`,
> `SW360ConfigurationsService`). The legacy prefix is grandfathered — **new
> services must use `Sw360*`**. ArchUnit matches both.

### Recent additions (v20.1.0)

- `GET  /api/ecc` — list releases with ECC status (optional `?eccStatus=` filter) — `EccController`
- `PATCH /api/ecc/{releaseId}` — update ECC status — `EccController`
- `GET  /api/projects/{id}/tabCounts` — per-tab counts for project detail view — `ProjectController`
- `?luceneSearch=true` query param on `/api/users`, `/api/releases`, and export
  endpoints — enables Nouveau/Lucene full-text search
- Report export (`/api/reports`) supports project/release/component modules
  with configurable output formats

> Verify against the actual controller before quoting a path in generated code.

### OpenAPI Documentation

- Global config: `Sw360ResourceServer.customOpenAPI()`
- Security schemes: `tokenAuth` (Bearer) and `basic`
- Pagination: `OpenAPIPaginationHelper` replaces raw `Pageable` params
- AsciiDoc sources: `rest/resource-server/src/docs/asciidoc/`
- Generated at build time with Spring REST Docs

### JacksonCustomizations.java — Critical File

Location: `rest/resource-server/.../core/JacksonCustomizations.java`

Every mixin **must be registered twice**:
1. `setMixInAnnotation()` — for runtime Jackson serialization
2. `SpringDocUtils.getConfig().replaceWithClass()` — for OpenAPI schema

Update this file when: adding REST fields, renaming fields, hiding internal Thrift fields.

---

## Thrift Service Layer

### Thrift File Structure

```
libraries/datahandler/src/main/thrift/
  sw360.thrift           # Common types, enums, exceptions
  components.thrift      # Component, Release
  projects.thrift        # Project
  licenses.thrift        # License, Obligation
  vulnerabilities.thrift # Vulnerability
  users.thrift           # User
  attachments.thrift     # Attachment
  moderation.thrift      # Moderation requests
```

### Thrift Service Definition Pattern

```thrift
namespace java org.eclipse.sw360.datahandler.thrift.components

service ComponentService {
    Component getComponentById(1: string id, 2: User user) throws (1: SW360Exception exp);
    AddDocumentRequestStatus addComponent(1: Component component, 2: User user) throws (1: SW360Exception exp);
    RequestStatus updateComponent(1: Component component, 2: User user) throws (1: SW360Exception exp);
    RequestStatus deleteComponent(1: string id, 2: User user) throws (1: SW360Exception exp);
}
```

### Handler Implementation Pattern

```java
public class ComponentHandler implements ComponentService.Iface {
    private final ComponentDatabaseHandler handler;

    public ComponentHandler() throws MalformedURLException {
        handler = new ComponentDatabaseHandler(
            DatabaseSettings.getConfiguredClient(),
            DatabaseSettings.COUCH_DB_DATABASE);
    }

    @Override
    public Component getComponentById(String id, User user) throws SW360Exception {
        assertId(id);
        assertUser(user);
        return handler.getComponent(id, user);
    }
}
```

### Using ThriftClients in REST Services

`ThriftClients` factory methods are **static** — do not inject an instance.

```java
@Service
@RequiredArgsConstructor
public class Sw360ComponentService {
    // No ThriftClients field — the factory methods are static.

    public Component getComponentById(String id, User user) throws TException {
        return ThriftClients.makeComponentClient().getComponentById(id, user);
    }
}
```

For unit tests, mock the static factory with Mockito's `MockedStatic`:

```java
try (MockedStatic<ThriftClients> mocked = mockStatic(ThriftClients.class)) {
    mocked.when(ThriftClients::makeComponentClient).thenReturn(componentClient);
    // ... exercise service and assert
}
```

### Common Request/Response Types

| Type | Values | Usage |
|------|--------|-------|
| `RequestStatus` | SUCCESS, FAILURE, IN_USE, SENT_TO_MODERATOR, ACCESS_DENIED | General operation result |
| `AddDocumentRequestStatus` | SUCCESS, DUPLICATE, FAILURE, NAMINGERROR | Document creation |
| `SW360Exception` | `why` (message), `errorCode` (HTTP-like) | Error signalling |

**Error code mapping:** 404 = not found · 403 = access denied · 409 = conflict · 400 = bad request

---

## Key Files Quick Reference

| Purpose | Path |
|---------|------|
| Main POM | `pom.xml` |
| Backend services | `backend/*/src/main/java/org/eclipse/sw360/*/` |
| REST controllers | `rest/resource-server/src/main/java/.../resourceserver/*/` |
| Thrift definitions | `libraries/datahandler/src/main/thrift/` |
| Exception handler | `rest/resource-server/.../core/RestExceptionHandler.java` |
| Jackson customizations | `rest/resource-server/.../core/JacksonCustomizations.java` |
| Thrift clients | `libraries/datahandler/.../thrift/ThriftClients.java` |
| CouchDB handlers | `libraries/datahandler/.../db/*DatabaseHandler.java` |
| Keycloak provider | `keycloak/user-storage-provider/` |
| Docker config | `docker-compose.yml`, `Dockerfile`, `docker_build.sh` |
| Test resources | `build-configuration/test-resources/` |

---

## Common Utility Classes

### SW360Utils — `libraries/datahandler/.../common/SW360Utils.java`

Domain-specific helpers (dates, versioning, project/release manipulation).

```java
SW360Utils.getCreatedOn()                      // current timestamp string (yyyy-MM-dd)
SW360Utils.getVersionedName(name, version)     // "name (version)"
// Also: printName(...), getReleaseIds(...), getBUFromOrganisation(...),
// getComponentIds(...), getVulnerabilityLinkedReleases(...) — see the class.
```

### CommonUtils — `libraries/datahandler/.../common/CommonUtils.java`

Generic null/collection/string helpers. **Prefer these over hand-rolled null checks.**

```java
CommonUtils.nullToEmptyList(list)              // List<T>
CommonUtils.nullToEmptySet(set)                // Set<T>
CommonUtils.nullToEmptyMap(map)                // Map<K,V>
CommonUtils.nullToEmptyString(str)             // String
CommonUtils.isNullOrEmptyCollection(collection)
CommonUtils.isNullEmptyOrWhitespace(str)       // note: not `isNullOrEmpty`
CommonUtils.isNotNullEmptyOrWhitespace(str)
CommonUtils.splitToSet("a,b,c")                // → Set<String>
CommonUtils.joinStrings(collection)            // comma-joined
CommonUtils.loadProperties(MyClass.class, "/path/to/file.properties")
```

### SW360Assert — `libraries/datahandler/.../common/SW360Assert.java`

```java
import static org.eclipse.sw360.datahandler.common.SW360Assert.*;
assertId(id);                    // non-empty document ID
assertUser(user);                // valid user object
assertNotNull(object, message);  // null check
assertNotEmpty(string, message); // empty string check
```

### ThriftClients — `libraries/datahandler/.../thrift/ThriftClients.java`

All factory methods are `public static` — call them on the class, not on an injected instance.

```java
ThriftClients.makeComponentClient()
ThriftClients.makeProjectClient()
ThriftClients.makeLicenseClient()
ThriftClients.makeUserClient()
ThriftClients.makeVulnerabilityClient()
ThriftClients.makeModerationClient()
ThriftClients.makeAttachmentClient()
ThriftClients.makeVendorClient()
ThriftClients.makeScheduleClient()
```

### RestControllerHelper — `rest/resource-server/.../core/RestControllerHelper.java`

```java
User user = restControllerHelper.getSw360UserFromAuthentication();
PaginationResult<T> page = restControllerHelper.paginateResult(list, pageable);
// For OpenAPI-documented paged endpoints, prefer OpenAPIPaginationHelper
// instead of raw Pageable — see existing usages in ProjectController.
restControllerHelper.addEmbeddedReleases(halResource, releases);
restControllerHelper.addEmbeddedModerators(halResource, moderators);
```

---

## Code Generation Standards

### Naming Conventions

| Type | Convention | Example |
|------|------------|---------|
| Classes | PascalCase | `ProjectController`, `ComponentHandler` |
| Methods | camelCase, verb-first | `getProjectById()`, `createRelease()` |
| Constants | UPPER_SNAKE_CASE | `COMPONENTS_URL`, `DEFAULT_PAGE_SIZE` |
| Packages | lowercase | `org.eclipse.sw360.rest.resourceserver.project` |
| REST endpoints | lowercase, plural, kebab-case | `/api/components`, `/api/clearing-requests` |
| Test classes | `*Test.java` or `*SpecTest.java` | `Sw360ProjectServiceTest.java` |

### Class Structure

```java
// For controllers, add class-level:
//   @BasePathAwareController
//   @SecurityRequirement(name = "tokenAuth")
// For services, just @Service is enough.

@Service
@RequiredArgsConstructor
public class Sw360ExampleService {

    // 1. Static fields (logger, constants)
    private static final Logger log = LogManager.getLogger(Sw360ExampleService.class);

    // 2. Constructor-injected dependencies (final + @NonNull)
    //    Note: ThriftClients is NOT injected — its factories are static.
    @NonNull private final RestControllerHelper restControllerHelper;

    // 3. Config values
    @Value("${sw360.example.config:default}")
    private String configValue;

    // 4. Public methods
    // 5. Private helpers
}
```

> Spring Boot 4 detects the single constructor for autowiring automatically —
> **do not add `@Autowired` on the constructor** and do not use the legacy
> Lombok `onConstructor = @__(@Autowired)` workaround.

### Controller Endpoint Pattern

```java
@Operation(summary = "Get resource by ID", tags = {"Resources"})
@ApiResponse(responseCode = "200", description = "Resource found")
@ApiResponse(responseCode = "404", description = "Not found")
@ApiResponse(responseCode = "403", description = "Access denied")
@GetMapping("/{id}")
public ResponseEntity<EntityModel<Resource>> getById(
        @Parameter(description = "Resource ID") @PathVariable String id) throws TException {
    User user = restControllerHelper.getSw360UserFromAuthentication();
    Resource resource = resourceService.getResourceById(id, user);
    return ResponseEntity.ok(EntityModel.of(resource));
}
```

### Service Method Pattern

```java
public Component getComponentById(String id, User user) throws TException {
    assertNotEmpty(id, "Component ID cannot be empty");
    assertUser(user);
    try {
        Component component = ThriftClients.makeComponentClient().getComponentById(id, user);
        if (component == null) throw new ResourceNotFoundException("Component not found: " + id);
        return component;
    } catch (SW360Exception e) {
        if (e.getErrorCode() == 404) throw new ResourceNotFoundException("Component not found: " + id);
        if (e.getErrorCode() == 403) throw new AccessDeniedException("Access denied: " + id);
        throw e;
    }
}
```

### Exception Handling Standards

| Exception | HTTP | When |
|-----------|------|------|
| `ResourceNotFoundException` | 404 | Entity not found |
| `AccessDeniedException` | 403 | Insufficient permission |
| `BadRequestClientException` | 400 | Invalid input |
| `DataIntegrityViolationException` | 409 | Duplicate / constraint |
| `TException` | 500 | Thrift communication failure |

### Logging Standards

```java
private static final Logger log = LogManager.getLogger(ClassName.class);
// Log4j2 auto-appends stack trace when Throwable is last arg:
log.debug("Processing id: {}", id);
log.info("Created component: {}", name);
log.warn("Deprecated API called by: {}", email);
log.error("DB operation failed for: {}", id, exception);
```

---

## Agent Mode — Step-by-Step Recipes

### Add a New REST Endpoint

```
1. rest/resource-server/.../resourceserver/<entity>/<Entity>Controller.java  → Add endpoint method
2. rest/resource-server/.../resourceserver/<entity>/Sw360<Entity>Service.java → Add service method
3. rest/resource-server/src/docs/asciidoc/<entity>.adoc                       → Document endpoint
4. rest/resource-server/src/test/.../integration/<Entity>Test.java            → HTTP test (MANDATORY — ArchUnit enforced)
5. rest/resource-server/src/test/.../restdocs/<Entity>SpecTest.java           → REST docs spec test
```

> Step 4 **must** call `TestRestTemplate` or `MockMvc` — a trivial test without an HTTP call will fail the ArchUnit build check.

### Add a New Field to an Entity

```
1. libraries/datahandler/src/main/thrift/<entity>.thrift   → Add field definition
2. mvn generate-sources -pl libraries/datahandler           → Regenerate Thrift classes
3. backend/<entity>/src/.../db/<Entity>DatabaseHandler.java → Handle new field in logic
4. rest/.../core/JacksonCustomizations.java                 → Update mixin if field needs REST exposure
5. scripts/migrations/0XX_migrate_<description>.py          → Create migration script if needed
```

### Add a New Thrift Service Method

```
1. libraries/datahandler/src/main/thrift/<service>.thrift → Add method signature
2. mvn generate-sources -pl libraries/datahandler          → Regenerate
3. backend/<service>/src/.../<Service>Handler.java         → Implement handler
4. rest/.../Sw360<Service>Service.java                     → Expose via REST service (if needed)
```

---

## DO's and DON'Ts

### ✅ DO
- Use constructor injection with `@RequiredArgsConstructor`
- Add `@Operation` and `@ApiResponse` for every REST endpoint
- Use `@PreAuthorize` for all write and delete operations
- Validate inputs at service layer with `SW360Assert`
- Log with Log4j2 at appropriate levels
- Add EPL-2.0 file headers on all new files
- Create a test class for every new Controller or Service (ArchUnit enforced)
- Add HTTP-exercising tests for every new endpoint (ArchUnit enforced)
- Use `Optional` for nullable returns
- Reuse the pooled Cloudant client from `DatabaseSettings.getConfiguredClient()` — avoid creating new connections per request

### ❌ DON'T
- Use field injection (`@Autowired` on fields)
- Use `System.out.println()` or `e.printStackTrace()`
- Catch generic `Exception` without re-throwing
- Expose `_id`, `_rev`, stack traces, or internal Thrift fields in API responses
- Use raw types (`List` instead of `List<String>`)
- Skip null checks on Thrift-returned objects
- Hardcode configuration values — use `@Value` or properties files
- Use raw SQL — use CouchDB views/Cloudant queries
- Skip OpenAPI documentation for new endpoints
- Write N+1 query patterns — use bulk operations or views

---

## Configuration Reference

### Configuration Files

| File | Location | Purpose |
|------|----------|---------|
| `sw360.properties` | `/etc/sw360/sw360.properties` | Main SW360 config |
| `couchdb.properties` | `/etc/sw360/couchdb.properties` | CouchDB connection |
| `application.yml` | `rest/resource-server/src/main/resources/` | Spring Boot / REST |

### Key sw360.properties Settings

```properties
backend.url=http://localhost:8080
backend.timeout.connection=5000
backend.timeout.read=600000
backend.thrift.max.message.size=104857600
rest.apitoken.write.generator.enable=true
rest.apitoken.read.validity.days=90
rest.apitoken.write.validity.days=30
rest.write.access.usergroup=SW360_ADMIN
enable.flexible.project.release.relationship=true
fossology.url=http://fossology:8081
cvesearch.host=https://cve.circl.lu
```

### Key couchdb.properties Settings

```properties
couchdb.url=http://localhost:5984
couchdb.user=admin
couchdb.password=password
couchdb.database=sw360db
couchdb.usersdb=sw360users
couchdb.attachments=sw360attachments
```

### Docker Environment Variables

```bash
COUCHDB_URL=http://couchdb:5984
COUCHDB_USER=sw360
COUCHDB_PASSWORD=sw360fossie
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=admin
SW360_THRIFT_SERVER_URL=http://localhost:8080
SW360_CONFIG_DIR=/etc/sw360
POSTGRES_USER=keycloak
POSTGRES_PASSWORD=keycloak
POSTGRES_DB=keycloak
```

---

## CI/CD & GitHub Workflows

| Workflow | Trigger | Purpose |
|----------|---------|---------|
| `build_and_test.yml` | PR to main | Build, test, lint, license check |
| `codeql.yml` | PR / push | Security analysis |
| `dependency-review.yml` | PR | Dependency vulnerability check |
| `sw360_container.yml` | Release | Build Docker images |

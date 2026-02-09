---
applyTo: "**"
---

# SW360 Copilot Instructions

> **Optimized for GitHub Copilot ASK, EDIT, and Agent modes**

## Project Overview

| Attribute | Details |
|-----------|---------|
| **Purpose** | Eclipse SW360 - SBOM and license compliance management platform for open source software |
| **Version** | 20.0.0-beta |
| **Stack** | Java 21, Spring Boot 3.5.x, Maven, CouchDB (Cloudant SDK), Thrift 0.20.0, Docker |
| **License** | EPL-2.0 |
| **Repository** | [github.com/eclipse-sw360/sw360](https://github.com/eclipse-sw360/sw360) |

### Module Structure
```
sw360/
├── backend/          # Thrift services (components, licenses, projects, vulnerabilities, etc.)
├── libraries/        # Shared libs (datahandler, thrift, commonIO, exporters, importers)
├── rest/             # REST API (resource-server, authorization-server)
├── clients/          # Java client SDK for SW360 REST API
├── keycloak/         # Keycloak user storage provider and event listeners
├── config/           # CouchDB, Keycloak, SW360 configurations
├── scripts/          # Build, test, migration, and utility scripts
└── third-party/      # Thrift compiler, licenses
```

---

## Architecture & Patterns

### Layering
```
UI (React) → REST Controllers → Sw360*Service → ThriftClients → Backend Handlers → Repositories → CouchDB
                                   │
                                   └── Uses Thrift interfaces for cross-service communication
```

> **Note:** Only Repositories are allowed to communicate directly with CouchDB. Backend Handlers must use Repositories for all database operations.

### Key Classes Pattern
| Layer | Naming Convention | Example |
|-------|-------------------|---------|
| REST Controller | `*Controller.java` | `ProjectController.java`, `ComponentController.java` |
| REST Service | `Sw360*Service.java` | `Sw360ProjectService.java`, `Sw360ReleaseService.java` |
| Thrift Handler | `*Handler.java` | `ComponentHandler.java`, `ProjectHandler.java` |
| DB Handler | `*DatabaseHandler.java` | `ComponentDatabaseHandler.java` |
| Repository | `*Repository.java` | `ComponentRepository.java`, `ProjectRepository.java` |
| Search Handler | `*SearchHandler.java` | `ComponentSearchHandler.java` |

### Design Conventions
- **Dependency Injection**: Constructor injection with `@RequiredArgsConstructor` (Lombok)
- **Logging**: Log4j2 via `LogManager.getLogger()` – never use `System.out`
- **DTOs**: Thrift-generated objects in `libraries/datahandler/src/main/thrift/`
- **Exception Handling**: Global handler in `RestExceptionHandler.java` with `@ControllerAdvice`
- **Security**: `@PreAuthorize("hasAuthority('WRITE')")` or `hasAuthority('ADMIN')` on endpoints

### User Roles (UserGroup enum)
- `USER` – Basic access
- `CLEARING_ADMIN` – License clearing workflows
- `CLEARING_EXPERT` – Advanced clearing
- `ECC_ADMIN` – Export control
- `SECURITY_ADMIN` – Vulnerability management
- `SW360_ADMIN` – Full admin
- `ADMIN` – System admin

---

## Developer Workflows

### Build Commands
```bash
# Full build (skip tests)
mvn package -P deploy -DskipTests

# Build with specific deploy directories
mvn package -P deploy -DskipTests \
    -Dbackend.deploy.dir=webapps \
    -Drest.deploy.dir=webapps \
    -Djars.deploy.dir=deploy

# Docker build
./docker_build.sh
./docker_build.sh --cvesearch-host http://cve-search:5000
```

### Test Commands
```bash
# Start CouchDB for tests (requires Docker)
./scripts/startCouchdbForTests.sh

# Run all tests
mvn test

# Run specific module tests
mvn test -pl rest/resource-server
mvn test -pl backend/components
```

### Code Quality
```bash
# Install pre-commit hooks (Spotless formatting)
pip install pre-commit && pre-commit install

# Format code manually
mvn spotless:apply

# Check formatting
mvn spotless:check
```

### Thrift Generation
```bash
# Install Thrift 0.20.0 if needed
./scripts/install-thrift.sh

# Thrift files location
libraries/datahandler/src/main/thrift/
```

---

## Domain Concepts & Key Types

### Core Entities (Thrift-generated)
| Entity | Description | Key Fields |
|--------|-------------|------------|
| `Component` | Software component | name, componentType, vendor |
| `Release` | Specific version of component with source | version, clearingState, mainLicenseIds |
| `Project` | Collection of releases | name, version, state, linkedReleases |
| `License` | License definition | shortName, fullName, obligations |
| `Obligation` | License obligation | title, text, obligationLevel |
| `Vulnerability` | CVE/security issue | externalId, cvss, references |
| `Package` | Distribution of Release with a pURL reference | name, version, purl |
| `Attachment` | File attachment | filename, attachmentType, checkStatus |
| `User` | Application user | email, department, userGroup |

### Important Enums
- `ClearingState`: NEW_CLEARING, UNDER_CLEARING, REPORT_AVAILABLE, APPROVED
- `ProjectClearingState`: OPEN, IN_PROGRESS, CLOSED
- `AttachmentType`: SOURCE, BINARY, CLEARING_REPORT, COMPONENT_LICENSE_INFO_XML
- `ReleaseRelationship`: CONTAINED, REFERRED, UNKNOWN, DYNAMICALLY_LINKED, STATICALLY_LINKED

---

## REST API Structure

### Endpoints Pattern: `/api/resource-name`
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

### OpenAPI Documentation
- **Configuration**: Global OpenAPI settings are defined in `Sw360ResourceServer.customOpenAPI()` method
- **Security Schemes**: Two authentication methods are configured:
  - `tokenAuth`: API key-based Bearer token authentication (`Authorization` header)
  - `basic`: HTTP Basic authentication (username/password)
- **API Info**: Title, license (EPL-2.0), and version are set programmatically
- **Version Generation**: REST API version is derived from build version and git commit count
- **Health Endpoint**: Custom `/health` endpoint documentation with example response
- **Pagination**: `Pageable` parameters are replaced with `OpenAPIPaginationHelper` for cleaner docs
- **AsciiDoc Sources**: `rest/resource-server/src/docs/asciidoc/`
- **Generated**: At build time with Spring REST Docs

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
| CouchDB secrets | `config/couchdb/default_secrets` |
| Test resources | `build-configuration/test-resources/` |

> **Note:** `JacksonCustomizations.java` is critical for REST API responses and OpenAPI documentation:
> - **Purpose**: Contains Jackson mixins that control JSON serialization/deserialization for Thrift-generated objects
> - **Dual Registration**: Each mixin must be registered twice:
>   1. `setMixInAnnotation()` - For Jackson ObjectMapper (runtime JSON serialization)
>   2. `SpringDocUtils.getConfig().replaceWithClass()` - For OpenAPI schema generation
> - **Common Annotations Used**:
>   - `@JsonIgnoreProperties` - Hide internal/Thrift fields from API responses (e.g., `setId`, `*IsSet`, `*Iterator`)
>   - `@JsonProperty` - Rename fields for cleaner API (e.g., `fullname` → `fullName`)
>   - `@JsonInclude(Include.NON_NULL)` - Omit null values from responses
>   - `@JsonSerialize` - Custom serializers (e.g., `JsonProjectRelationSerializer`)
>   - `@Schema` - OpenAPI documentation annotations
> - **When to Update**: Add/update mixin when adding new REST fields, changing field names, or hiding internal fields

---

## CouchDB/Cloudant Database Layer

### Core Database Classes

| Class | Purpose | Location |
|-------|---------|----------|
| `DatabaseConnectorCloudant` | Low-level CouchDB operations (CRUD, queries, attachments) | `libraries/datahandler/.../cloudantclient/` |
| `DatabaseRepositoryCloudantClient<T>` | Base class for all repositories with generic CRUD | `libraries/datahandler/.../cloudantclient/` |
| `DatabaseInstanceCloudant` | Manages CouchDB connection and database instances | `libraries/datahandler/.../cloudantclient/` |

### Repository Pattern

All repositories extend `DatabaseRepositoryCloudantClient<T>`:

```java
public class ComponentRepository extends DatabaseRepositoryCloudantClient<Component> {

    public ComponentRepository(DatabaseConnectorCloudant db) {
        super(db, Component.class);
        // Initialize design documents and indexes
        initStandardDesignDocument(getViews(), db);
    }

    // Custom query methods
    public List<Component> getByName(String name) {
        return queryView("byName", name);
    }
}
```

### Query Operators

Use static imports from `DatabaseConnectorCloudant` for building queries:

```java
import static org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant.*;

// Available operators
Map<String, Object> selector = and(
    eq("type", "component"),           // $eq - equals
    or(                                 // $or - logical OR
        eq("name", "Apache"),
        eq("name", "Spring")
    )
);

// Other operators
eq("field", "value")                   // $eq - exact match
in("field", List.of("a", "b"))         // $in - match any in list
all("field", List.of("a", "b"))        // $all - match all in list
exists("field", true)                  // $exists - field exists
elemMatch("field", condition)          // $elemMatch - array element match
and(condition1, condition2)            // $and - logical AND
or(condition1, condition2)             // $or - logical OR
```

### Creating Views (Design Documents)

```java
private Map<String, DesignDocumentViewsMapReduce> getViews() {
    return Map.of(
        "all", createMapReduce(
            "function(doc) { if(doc.type == 'component') emit(doc._id, null); }",
            "_count"
        ),
        "byName", createMapReduce(
            "function(doc) { if(doc.type == 'component') emit(doc.name, null); }",
            null
        )
    );
}
```

### Creating Indexes

```java
// Simple index
createIndex("component-idx", "componentIndex",
    new String[]{"name", "type"}, db);

// Partial index with type filter (better performance)
createPartialTypeIndex("component-partial-idx", "componentPartialIndex",
    "component", new String[]{"name", "createdOn"}, db);
```

### Pagination with PaginationData

```java
public Map<PaginationData, List<Component>> getComponentsWithPagination(
        PaginationData pageData) {

    // pageData contains: rowsPerPage, displayStart, sortColumnNumber, ascending
    List<Component> results = db.queryViewWithPagination(
        "all", pageData, Component.class);

    // Update total row count
    pageData.setTotalRowCount(db.getDocumentCount("component"));

    return Map.of(pageData, results);
}
```

### Common Database Operations

```java
// Get by ID
Component component = db.get(Component.class, componentId);

// Add new document
DocumentResult result = db.add(component);
String newId = result.getId();

// Update document
db.update(component);  // Uses _id and _rev from object

// Delete document
db.remove(componentId, revision);

// Bulk operations
List<DocumentResult> results = db.executeBulk(listOfComponents);

// Query with selector
List<Component> components = db.queryBySelector(selector, Component.class);
```

---

## Thrift Service Layer

### Thrift File Structure

Thrift definitions are in `libraries/datahandler/src/main/thrift/`:

```
sw360.thrift          # Common types, enums, exceptions
components.thrift     # Component, Release entities and service
projects.thrift       # Project entity and service
licenses.thrift       # License, Obligation entities and service
vulnerabilities.thrift # Vulnerability entities and service
users.thrift          # User entity and service
attachments.thrift    # Attachment handling
moderation.thrift     # Moderation requests
```

### Thrift Service Definition Pattern

```thrift
// In components.thrift
namespace java org.eclipse.sw360.datahandler.thrift.components

service ComponentService {
    // Get operations
    Component getComponentById(1: string id, 2: User user) throws (1: SW360Exception exp);
    list<Component> getComponentsByName(1: string name) throws (1: SW360Exception exp);

    // Write operations
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
            DatabaseSettings.COUCH_DB_DATABASE
        );
    }

    @Override
    public Component getComponentById(String id, User user) throws SW360Exception {
        assertId(id);
        assertUser(user);
        return handler.getComponent(id, user);
    }

    @Override
    public AddDocumentRequestStatus addComponent(Component component, User user)
            throws SW360Exception {
        assertUser(user);
        assertNotNull(component);
        return handler.addComponent(component, user.getEmail());
    }
}
```

### Using ThriftClients in REST Services

```java
@Service
@RequiredArgsConstructor
public class Sw360ComponentService {

    @NonNull
    private final ThriftClients thriftClients;

    public Component getComponentById(String id, User user) throws TException {
        ComponentService.Iface client = thriftClients.makeComponentClient();
        return client.getComponentById(id, user);
    }
}
```

### Common Request/Response Types

| Type | Values | Usage |
|------|--------|-------|
| `RequestStatus` | SUCCESS, FAILURE, IN_USE, SENT_TO_MODERATOR, ACCESS_DENIED | General operation result |
| `AddDocumentRequestStatus` | SUCCESS, DUPLICATE, FAILURE, NAMINGERROR | Document creation result |
| `SW360Exception` | `why` (message), `errorCode` (HTTP-like) | Thrown on errors |

### Error Code Mapping

```java
// In SW360Exception
errorCode = 404  → Resource not found
errorCode = 403  → Access denied
errorCode = 409  → Conflict/duplicate
errorCode = 400  → Bad request
```

---

## Security & Authentication

### Authentication Methods

SW360 supports two authentication methods:

1. **Keycloak JWT (Primary)**
   - OAuth2/OIDC flow via Keycloak
   - JWT tokens validated against Keycloak realm
   - Configured in `application.yml`:
   ```yaml
   spring:
     security:
       oauth2:
         resourceserver:
           jwt:
             issuer-uri: http://localhost:8083/realms/sw360
             jwk-set-uri: http://localhost:8083/realms/sw360/protocol/openid-connect/certs
   ```

2. **API Token (Bearer)**
   - Generated per-user tokens stored in CouchDB
   - Format: `Authorization: Bearer <token>`
   - Configured via `sw360.properties`:
   ```properties
   rest.apitoken.write.generator.enable=true
   rest.apitoken.read.validity.days=90
   rest.apitoken.write.validity.days=30
   ```

3. **Basic Auth (Development)**
   - Username/password authentication
   - Only for development/testing

### Authorization Patterns

**Controller-Level Security:**
```java
@PreAuthorize("hasAuthority('WRITE')")
@PostMapping
public ResponseEntity<Resource> create(@RequestBody Resource resource) {
    // Only users with WRITE authority can access
}

@PreAuthorize("hasAuthority('ADMIN')")
@DeleteMapping("/{id}")
public ResponseEntity<Void> delete(@PathVariable String id) {
    // Only ADMIN users can delete
}
```

**Service-Level Permission Checks:**
```java
import static org.eclipse.sw360.datahandler.permissions.PermissionUtils.*;

public void updateProject(Project project, User user) throws SW360Exception {
    // Check if user has at least CLEARING_ADMIN role
    if (!isUserAtLeast(UserGroup.CLEARING_ADMIN, user)) {
        throw new AccessDeniedException("Insufficient permissions");
    }

    // Check document-level permissions
    if (!makePermission(project, user).isActionAllowed(RequestedAction.WRITE)) {
        throw new AccessDeniedException("No write access to project");
    }
}
```

### User Role Hierarchy

```
ADMIN (highest)
  └── SW360_ADMIN
        └── SECURITY_ADMIN
        └── ECC_ADMIN
        └── CLEARING_ADMIN
              └── CLEARING_EXPERT
                    └── USER (lowest)
```

### Getting Current User

```java
// In REST Controllers
User user = restControllerHelper.getSw360UserFromAuthentication();

// In Services (from SecurityContext)
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
String email = auth.getName();
User user = userService.getUserByEmail(email);
```

### Document-Level Permissions

```java
// Permission types
enum RequestedAction {
    READ, WRITE, DELETE, USERS, CLEARING, ATTACHMENTS
}

// Check permissions on a document
DocumentPermissions<Project> permissions = makePermission(project, user);
if (permissions.isActionAllowed(RequestedAction.WRITE)) {
    // User can modify this project
}

// Visibility levels
enum Visibility {
    PRIVATE,        // Only creator
    ME_AND_MODERATORS,  // Creator + moderators
    BUISNESSUNIT_AND_MODERATORS,  // Same business unit
    EVERYONE        // All authenticated users
}
```

---

## Configuration Properties Reference

### Configuration File Locations

| File | Location | Purpose |
|------|----------|---------|
| `sw360.properties` | `/etc/sw360/sw360.properties` | Main SW360 configuration |
| `couchdb.properties` | `/etc/sw360/couchdb.properties` | CouchDB connection |
| `application.yml` | `rest/resource-server/src/main/resources/` | Spring Boot / REST config |

### sw360.properties Keys

```properties
# Backend URL
backend.url=http://localhost:8080
backend.proxy.url=                          # Optional proxy
backend.timeout.connection=5000             # Connection timeout (ms)
backend.timeout.read=600000                 # Read timeout (ms)

# Thrift transport settings
backend.thrift.max.message.size=104857600   # 100MB
backend.thrift.max.frame.size=16384000      # 16MB

# REST API token settings
rest.apitoken.write.generator.enable=true
rest.apitoken.read.validity.days=90
rest.apitoken.write.validity.days=30
rest.apitoken.hash.salt=$2a$04$Software360RestApiSalt

# Access control
rest.write.access.usergroup=SW360_ADMIN
rest.admin.access.usergroup=SW360_ADMIN

# Feature flags
enable.flexible.project.release.relationship=true

# External integrations
fossology.url=http://fossology:8081
fossology.token=<token>
cvesearch.host=https://cve.circl.lu

# JWKS validation (optional)
jwks.issuer.url=http://localhost:8083/realms/sw360
jwks.endpoint.url=http://localhost:8083/realms/sw360/protocol/openid-connect/certs
jwks.validation.enabled=false
```

### couchdb.properties Keys

```properties
couchdb.url=http://localhost:5984
couchdb.user=admin
couchdb.password=password
couchdb.database=sw360db
couchdb.usersdb=sw360users
couchdb.attachments=sw360attachments
```

### Environment Variables (Docker)

```bash
# CouchDB
COUCHDB_URL=http://couchdb:5984
COUCHDB_USER=sw360
COUCHDB_PASSWORD=sw360fossie

# Keycloak
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=admin

# SW360 Backend
SW360_THRIFT_SERVER_URL=http://localhost:8080
SW360_CONFIG_DIR=/etc/sw360

# PostgreSQL (for Keycloak)
POSTGRES_USER=keycloak
POSTGRES_PASSWORD=keycloak
POSTGRES_DB=keycloak
```

### Keycloak Configuration (keycloak.conf)

```properties
# Database
db=postgres
db-username=keycloak
db-password=password
db-url=jdbc:postgresql://localhost/keycloak

# SW360 integration (custom providers)
spi-events-listener-sw360-add-user-to-couchdb-thrift=http://localhost:8080
spi-storage-sw360-user-storage-jpa-thrift=http://localhost:8080
```

---

## CI/CD & GitHub Workflows

### GitHub Actions Workflows

| Workflow | Trigger | Purpose |
|----------|---------|---------|
| `build_and_test.yml` | PR to main | Build, test, lint, license check |
| `codeql.yml` | PR/push | Security analysis |
| `dependency-review.yml` | PR | Dependency vulnerability check |
| `sw360_container.yml` | Release | Build Docker images |

### CI Pipeline Stages

```yaml
# build_and_test.yml structure
jobs:
  build:
    steps:
      - Checkout code
      - Verify conventional commits
      - Check license headers
      - Set up Java 21
      - Install Thrift 0.20.0
      - Start CouchDB (service container)
      - Build with Maven
      - Run tests
      - Check code formatting (Spotless)
```

### Required PR Checks

Before merging, PRs must pass:
- ✅ Build succeeds (`mvn package`)
- ✅ All tests pass (`mvn test`)
- ✅ Code formatting (`mvn spotless:check`)
- ✅ License headers present
- ✅ Conventional commit format
- ✅ ECA (Eclipse Contributor Agreement) signed
- ✅ No dependency vulnerabilities (critical)

### Running CI Locally

```bash
# Simulate CI build
mvn clean package -DskipTests

# Run tests with CouchDB
./scripts/startCouchdbForTests.sh
mvn test

# Check formatting
mvn spotless:check

# Verify license headers
mvn license:check
```

### Docker Build

```bash
# Build all images
./docker_build.sh

# Build with custom CVE search
./docker_build.sh --cvesearch-host http://cve-search:5000

# Run with docker-compose
docker-compose up -d
```

---

## Common Utility Classes

### SW360Utils

Location: `libraries/datahandler/.../common/SW360Utils.java`

```java
import org.eclipse.sw360.datahandler.common.SW360Utils;

// String utilities
SW360Utils.isNullOrEmpty(string)              // Null or empty check
SW360Utils.nullToEmptyString(string)          // Convert null to ""
SW360Utils.nullToEmptyList(list)              // Convert null to empty list
SW360Utils.nullToEmptySet(set)                // Convert null to empty set
SW360Utils.nullToEmptyMap(map)                // Convert null to empty map

// Document utilities
SW360Utils.getCreatedOn()                     // Current timestamp string
SW360Utils.getVersionedName(name, version)    // "name (version)"
SW360Utils.printName(component)               // Formatted component name

// Collection utilities
SW360Utils.unionSets(set1, set2)              // Merge sets
SW360Utils.getDistinctSortedStringStream(list) // Distinct sorted stream
```

### CommonUtils

Location: `libraries/datahandler/.../common/CommonUtils.java`

```java
import org.eclipse.sw360.datahandler.common.CommonUtils;

// Null-safe operations
CommonUtils.nullToEmptyString(str)
CommonUtils.nullToEmptyList(list)
CommonUtils.nullToEmptySet(set)
CommonUtils.nullToEmptyMap(map)
CommonUtils.isNullOrEmptyCollection(collection)
CommonUtils.isNullOrEmptyMap(map)

// Properties loading
Properties props = CommonUtils.loadProperties(MyClass.class, "/path/to/file.properties");

// String operations
CommonUtils.splitToSet("a,b,c")               // Split to Set<String>
CommonUtils.joinStrings(collection)           // Join with comma

// Collection operations
CommonUtils.getFirstSet(collection)           // Get first element or null
CommonUtils.filterEmptyStrings(list)          // Remove empty strings
```

### SW360Constants

Location: `libraries/datahandler/.../common/SW360Constants.java`

```java
import static org.eclipse.sw360.datahandler.common.SW360Constants.*;

// Document types
TYPE_COMPONENT      // "component"
TYPE_RELEASE        // "release"
TYPE_PROJECT        // "project"
TYPE_LICENSE        // "license"
TYPE_USER           // "user"
TYPE_VENDOR         // "vendor"
TYPE_ATTACHMENT     // "attachment"

// Field names
TYPE                // "_type"
ID                  // "_id"
REVISION            // "_rev"

// Defaults
DEFAULT_COMPONENT_TYPE   // ComponentType.OSS
```

### SW360Assert

Location: `libraries/datahandler/.../common/SW360Assert.java`

```java
import static org.eclipse.sw360.datahandler.common.SW360Assert.*;

// Validation methods (throw SW360Exception on failure)
assertId(id);                    // Non-empty document ID
assertUser(user);                // Valid user object
assertNotNull(object, message);  // Not null check
assertNotEmpty(string, message); // Non-empty string
assertNotEmpty(collection, msg); // Non-empty collection

// Example usage
public void processComponent(String id, User user) throws SW360Exception {
    assertId(id);                                    // Throws if id is null/empty
    assertUser(user);                                // Throws if user invalid
    assertNotNull(component, "Component required");  // Throws with message
}
```

### RestControllerHelper

Location: `rest/resource-server/.../core/RestControllerHelper.java`

```java
@Autowired
private RestControllerHelper restControllerHelper;

// Get current user from security context
User user = restControllerHelper.getSw360UserFromAuthentication();

// Pagination helpers
PaginationResult<T> paginationResult = restControllerHelper.paginateResult(
    allResources, pageable);

// Create HAL links
Link selfLink = restControllerHelper.createSelfLink(resource);

// Check write access
restControllerHelper.checkForCyclicOrInvalidDependencies(client, releases, user);

// Add embedded resources
restControllerHelper.addEmbeddedContributors(halResource, contributors);
restControllerHelper.addEmbeddedModerators(halResource, moderators);
restControllerHelper.addEmbeddedReleases(halResource, releases);
```

### ThriftClients

Location: `libraries/datahandler/.../thrift/ThriftClients.java`

```java
@Autowired
private ThriftClients thriftClients;

// Get service clients
ComponentService.Iface componentClient = thriftClients.makeComponentClient();
ProjectService.Iface projectClient = thriftClients.makeProjectClient();
LicenseService.Iface licenseClient = thriftClients.makeLicenseClient();
UserService.Iface userClient = thriftClients.makeUserClient();
VendorService.Iface vendorClient = thriftClients.makeVendorClient();
VulnerabilityService.Iface vulnClient = thriftClients.makeVulnerabilityClient();
ModerationService.Iface moderationClient = thriftClients.makeModerationClient();
AttachmentService.Iface attachmentClient = thriftClients.makeAttachmentClient();
```

---

## SW360 Code Generation Standards

> **Copilot MUST follow these standards when generating code suggestions**

### File Headers (Required for ALL new files)

**Java Files:**
```java
/*
 * Copyright <Copyright Holder>, <year>.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
```

**Configuration/Script Files:**
```bash
#
# Copyright <Copyright Holder>, <year>.
# Part of the SW360 Portal Project.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
```

### Naming Conventions

| Type | Convention | Example |
|------|------------|---------|
| Classes | PascalCase | `ProjectController`, `ComponentHandler` |
| Methods | camelCase, verb-first | `getProjectById()`, `createRelease()` |
| Variables | camelCase | `projectId`, `releaseList` |
| Constants | UPPER_SNAKE_CASE | `COMPONENTS_URL`, `DEFAULT_PAGE_SIZE` |
| Packages | lowercase | `org.eclipse.sw360.rest.resourceserver.project` |
| Test Classes | `*Test.java` suffix | `Sw360ProjectServiceTest.java` |
| REST Endpoints | lowercase, plural, kebab-case | `/api/components`, `/api/clearing-requests` |

### Class Structure Pattern

```java
@Service  // or @RestController, @BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@SecurityRequirement(name = "tokenAuth")  // For controllers
public class Sw360ExampleService {

    // 1. Static fields (loggers, constants)
    private static final Logger log = LogManager.getLogger(Sw360ExampleService.class);
    public static final String EXAMPLE_URL = "/examples";

    // 2. Injected dependencies (final, via constructor)
    @NonNull
    private final RestControllerHelper restControllerHelper;

    @NonNull
    private final Sw360UserService userService;

    // 3. Instance fields
    @Value("${sw360.example.config:default}")
    private String configValue;

    // 4. Public methods
    // 5. Private/helper methods
}
```

### Method Patterns

**Controller Endpoint:**
```java
@Operation(
        summary = "Get resource by ID",
        description = "Retrieves a specific resource by its unique identifier.",
        tags = {"Resources"}
)
@ApiResponse(responseCode = "200", description = "Resource found")
@ApiResponse(responseCode = "404", description = "Resource not found")
@ApiResponse(responseCode = "403", description = "Access denied")
@GetMapping(value = "/{id}")
public ResponseEntity<EntityModel<Resource>> getById(
        @Parameter(description = "Resource ID") @PathVariable String id
) throws TException {
    User sw360User = restControllerHelper.getSw360UserFromAuthentication();
    Resource resource = resourceService.getResourceById(id, sw360User);
    return ResponseEntity.ok(EntityModel.of(resource));
}
```

**Service Method:**
```java
public Resource getResourceById(String resourceId, User user) throws TException {
    assertNotEmpty(resourceId, "Resource ID cannot be empty");
    assertUser(user);

    ResourceService.Iface client = getThriftResourceClient();
    try {
        Resource resource = client.getResourceById(resourceId, user);
        if (resource == null) {
            throw new ResourceNotFoundException("Resource not found: " + resourceId);
        }
        return resource;
    } catch (SW360Exception e) {
        if (e.getErrorCode() == 404) {
            throw new ResourceNotFoundException("Resource not found: " + resourceId);
        } else if (e.getErrorCode() == 403) {
            throw new AccessDeniedException("Access denied to resource: " + resourceId);
        }
        throw e;
    }
}
```

### Exception Handling Standards

| Exception Type | HTTP Status | When to Use |
|---------------|-------------|-------------|
| `ResourceNotFoundException` | 404 | Entity not found in database |
| `AccessDeniedException` | 403 | User lacks permission |
| `BadRequestClientException` | 400 | Invalid input parameters |
| `DataIntegrityViolationException` | 409 | Duplicate or constraint violation |
| `TException` | 500 | Thrift communication error |

### Logging Standards

```java
// Use Log4j2 logger
private static final Logger log = LogManager.getLogger(ClassName.class);
```

| Level | Usage | Example |
|-------|-------|---------|
| `debug` | Detailed debugging info | `log.debug("Processing id: {}", id);` |
| `info` | Business events | `log.info("Created component: {}", name);` |
| `warn` | Potential issues | `log.warn("Deprecated API used by: {}", email);` |
| `error` | Errors/failures | `log.error("Operation failed for: {}", id);` |

**Logging with Exceptions:**
```java
// Log4j2 detects Throwable as last argument and logs stack trace automatically
log.debug("Failed to parse id: {}", id, exception);
log.warn("Retry failed for: {}", resourceId, exception);
log.error("DB operation failed: {}", userId, exception);
```

**❌ NEVER use:**
- `System.out.println()` / `System.err.println()`
- `e.printStackTrace()`

### Input Validation

```java
// Use SW360Assert for validation
import static org.eclipse.sw360.datahandler.common.SW360Assert.*;

public void processResource(String id, User user) throws SW360Exception {
    assertId(id);                    // Validates non-empty ID
    assertUser(user);                // Validates user object
    assertNotNull(resource);         // Null check
    assertNotEmpty(name);            // Empty string check
}
```

### Commit Message Format

> **See [git-commit.instructions.md](git-commit.instructions.md) for complete commit message guidelines, branch naming conventions, and PR requirements.**

### DO's and DON'Ts

#### ✅ DO:
- Use constructor injection with `@RequiredArgsConstructor`
- Add `@Operation` and `@ApiResponse` for all REST endpoints
- Use `@PreAuthorize` for write/admin operations
- Validate inputs at service layer using `SW360Assert`
- Log with appropriate levels using Log4j2
- Handle exceptions with specific types
- Add file headers with EPL-2.0 license
- Write unit tests for new code
- Use `Optional` for nullable returns
- Follow existing patterns in the codebase

#### ❌ DON'T:
- Use field injection (`@Autowired` on fields)
- Use `System.out.println()` or `e.printStackTrace()`
- Catch generic `Exception` without re-throwing
- Expose internal IDs or stack traces in API responses
- Use raw types (e.g., `List` instead of `List<String>`)
- Skip null checks on Thrift-returned objects
- Hardcode configuration values
- Create mutable DTOs or expose public fields
- Use raw SQL queries (use CouchDB views/queries)
- Skip OpenAPI documentation for new endpoints

### Test Requirements

**Unit Test Pattern:**
```java
@ExtendWith(MockitoExtension.class)
class Sw360ResourceServiceTest {

    @Mock
    private ThriftClients thriftClients;

    @Mock
    private ResourceService.Iface resourceClient;

    @InjectMocks
    private Sw360ResourceService service;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User().setEmail("test@example.com").setUserGroup(UserGroup.USER);
    }

    @Test
    void shouldReturnResource_whenResourceExists() throws TException {
        // Given
        String resourceId = "resource123";
        Resource expectedResource = new Resource().setId(resourceId);
        when(thriftClients.makeResourceClient()).thenReturn(resourceClient);
        when(resourceClient.getResourceById(resourceId, testUser)).thenReturn(expectedResource);

        // When
        Resource result = service.getResourceById(resourceId, testUser);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(resourceId);
        verify(resourceClient).getResourceById(resourceId, testUser);
    }

    @Test
    void shouldThrowNotFoundException_whenResourceDoesNotExist() throws TException {
        // Given
        String resourceId = "nonexistent";
        when(thriftClients.makeResourceClient()).thenReturn(resourceClient);
        when(resourceClient.getResourceById(resourceId, testUser)).thenReturn(null);

        // When/Then
        assertThatThrownBy(() -> service.getResourceById(resourceId, testUser))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
```

---

## AI Copilot Guidance

### For ASK Mode (Questions & Explanations)
When answering questions, always reference specific locations:
- **Architecture**: See `Architecture & Patterns` section above
- **Entities**: Check `libraries/datahandler/src/main/thrift/*.thrift` for data structures
- **REST patterns**: Reference `rest/resource-server/.../resourceserver/project/ProjectController.java`
- **DB patterns**: Reference `backend/components/src/.../db/ComponentDatabaseHandler.java`
- **Config**: See `Configuration Properties Reference` section above

### For EDIT Mode (Code Changes)
Before generating code, verify:
1. **Which layer?** Controller → Service → Handler → Repository
2. **Existing patterns?** Find similar code in the same module first
3. **Required annotations?** `@Operation`, `@PreAuthorize`, `@NonNull`

After generating code:
- Run `mvn spotless:apply` to format
- Run `mvn test -pl <module>` to verify

### For Agent Mode (Complex Tasks)

**Adding a New REST Endpoint:**
```
1. rest/resource-server/.../resourceserver/<entity>/<Entity>Controller.java  → Add endpoint method
2. rest/resource-server/.../resourceserver/<entity>/Sw360<Entity>Service.java → Add service method
3. rest/resource-server/src/docs/asciidoc/<entity>.adoc → Document endpoint
4. rest/resource-server/src/test/.../<Entity>ControllerTest.java → Add tests
```

**Adding a New Field to Entity:**
```
1. libraries/datahandler/src/main/thrift/<entity>.thrift → Add field definition
2. Run: mvn generate-sources -pl libraries/datahandler
3. backend/<entity>/src/.../db/<Entity>DatabaseHandler.java → Handle new field
4. rest/.../core/JacksonCustomizations.java → Update mixin if needed for REST
5. scripts/migrations/0XX_migrate_<description>.py → Create migration if needed
```

**Adding a New Thrift Service Method:**
```
1. libraries/datahandler/src/main/thrift/<service>.thrift → Add method signature
2. Run: mvn generate-sources -pl libraries/datahandler
3. backend/<service>/src/.../<Service>Handler.java → Implement handler method
4. rest/.../Sw360<Service>Service.java → Add REST service method (if exposed)
```

### Code Generation Patterns

**New REST Endpoint:**
```java
@Operation(summary = "Description here")
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Success"),
    @ApiResponse(responseCode = "404", description = "Not found")
})
@GetMapping("/{id}")
public ResponseEntity<EntityModel<Resource>> getById(
        @PathVariable String id) throws TException {
    User user = restControllerHelper.getSw360UserFromAuthentication();
    // Implementation
}
```

**Service Method:**
```java
public Resource getResourceById(String id, User user) throws TException {
    ResourceService.Iface client = getThriftClient();
    try {
        return client.getById(id, user);
    } catch (SW360Exception e) {
        if (e.getErrorCode() == 404) {
            throw new ResourceNotFoundException("Resource not found");
        }
        throw e;
    }
}
```

### Common Task Examples

| Task | Key Files to Modify |
|------|---------------------|
| Add REST endpoint | `*Controller.java`, `Sw360*Service.java`, AsciiDoc |
| Add new field | Thrift `.thrift` file, regenerate, update handlers |
| Fix pagination | Check `PaginationData`, service method, controller params |
| Add validation | Service layer, use `SW360Assert.*` methods |
| Add security check | `@PreAuthorize` or `PermissionUtils.isUserAtLeast()` |

---


## Testing Patterns

### Unit Test Structure
```java
@ExtendWith(MockitoExtension.class)
class Sw360ServiceTest {
    @Mock private ThriftClients thriftClients;
    @InjectMocks private Sw360Service service;

    @Test
    void shouldReturnResource_whenExists() throws TException {
        // given
        when(thriftClients.makeClient()).thenReturn(mockClient);
        // when
        var result = service.getById("id", user);
        // then
        assertThat(result).isNotNull();
    }
}
```

### Integration Test
```java
@SpringBootTest
@AutoConfigureMockMvc
class ControllerIntegrationTest {
    @Autowired private MockMvc mockMvc;

    @Test
    void shouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/resource/1"))
            .andExpect(status().isOk());
    }
}
```

---

## Recent Development Context (Jan 2026)
- Version 20.0.0-beta in development
- Spring Boot 3.5.x with Spring Security 6.5.x
- Keycloak 26.x integration for authentication
- DB-side pagination for components, projects, vulnerabilities
- New React UI in parallel development
- Focus on OpenAPI documentation improvements

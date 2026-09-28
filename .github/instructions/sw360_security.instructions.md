---
applyTo: "**/*Security*,**/*Auth*,**/application.yml,**/keycloak/**,**/*Controller.java"
---

# SW360 Security & Authentication Instructions

> **Spring Security 7.1 · Keycloak 26.x · Unified Bearer Authentication (20.1.x)**

---

## Authentication Methods

### 1. Keycloak JWT — Primary (Production)

OAuth2/OIDC via Keycloak 26.x with a single trusted issuer.

```yaml
# application.yml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8083/realms/sw360
```

> **Multi-issuer support:** as of 20.1.x SW360 relies on the single
> `spring.security.oauth2.resourceserver.jwt.issuer-uri` property. Multi-issuer
> JWT (multiple `JwtDecoder`s behind an `IssuerAuthenticationManagerResolver`)
> is **not** wired up by default. If you need it, add it explicitly in
> `ResourceServerConfiguration` — do not invent config keys.

### 2. API Token — Bearer (Per-user, stored in CouchDB)

```
Authorization: Bearer <token>
```

```properties
# sw360.properties
rest.apitoken.write.generator.enable=true
rest.apitoken.read.validity.days=90
rest.apitoken.write.validity.days=30
rest.apitoken.hash.salt=$2a$04$Software360RestApiSalt
```

### 3. Basic Auth — Development / Testing Only

Configurable via `sw360.properties`. **Never enable in production.**

### Unified Bearer Auth (20.1.x)

Both Keycloak JWT and API tokens are handled through a unified Bearer
authentication filter. The system distinguishes token types automatically —
do **not** implement custom token parsing.

---

## Controller-Level Authorization

```java
// Require WRITE authority for mutating operations
@PreAuthorize("hasAuthority('WRITE')")
@PostMapping
public ResponseEntity<EntityModel<Component>> createComponent(
        @RequestBody Component component) throws TException { ... }

// Require ADMIN authority for destructive operations
@PreAuthorize("hasAuthority('ADMIN')")
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteComponent(@PathVariable String id) throws TException { ... }

// Sensitive read-only or admin sub-resource endpoints — require READ
@PreAuthorize("hasAuthority('READ')")
@GetMapping("/status")
public ResponseEntity<Status> status() { ... }

// Ordinary read-only endpoints — authenticated user is sufficient, no @PreAuthorize needed
@GetMapping("/{id}")
public ResponseEntity<EntityModel<Component>> getById(@PathVariable String id) throws TException { ... }
```

---

## Service-Level Permission Checks

```java
import static org.eclipse.sw360.datahandler.permissions.PermissionUtils.*;

public void updateProject(Project project, User user) throws SW360Exception {
    // Role-level check
    if (!isUserAtLeast(UserGroup.CLEARING_ADMIN, user)) {
        throw new AccessDeniedException("Insufficient role: CLEARING_ADMIN required");
    }
    // Document-level permission check
    if (!makePermission(project, user).isActionAllowed(RequestedAction.WRITE)) {
        throw new AccessDeniedException("No write access to project: " + project.getId());
    }
}
```

### RequestedAction enum
```java
enum RequestedAction { READ, WRITE, DELETE, USERS, CLEARING, ATTACHMENTS }
```

### Visibility enum
```java
enum Visibility {
    PRIVATE,                       // Creator only
    ME_AND_MODERATORS,             // Creator + moderators
    BUISNESSUNIT_AND_MODERATORS,   // Same business unit
    EVERYONE                       // All authenticated users
}
```

---

## User Role Hierarchy

```
ADMIN (highest)
  └── SW360_ADMIN
        ├── SECURITY_ADMIN    (vulnerability management)
        ├── ECC_ADMIN         (export control)
        └── CLEARING_ADMIN
              └── CLEARING_EXPERT
                    └── USER (lowest)
```

Use `PermissionUtils.isUserAtLeast(UserGroup.X, user)` for role checks.

---

## Getting the Current User

```java
// In REST Controllers (preferred)
User user = restControllerHelper.getSw360UserFromAuthentication();

// In Services (direct SecurityContext access)
// Spring Security 7 note: for reactive-safe access use
// SecurityContextHolder.getContextHolderStrategy().getContext() instead.
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
String email = auth.getName();
User user = userService.getUserByEmail(email);
```

---

## Testing Authenticated Endpoints

Do **not** hand-craft `Authorization: Bearer <token>` headers in MockMvc —
they won't clear the Spring Security 7 filter chain. Use the Spring Security
test helpers:

```java
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

mockMvc.perform(get("/api/components/comp123")
        .with(jwt().authorities(new SimpleGrantedAuthority("READ"))))
        .andExpect(status().isOk());

// For write endpoints
mockMvc.perform(post("/api/components")
        .with(jwt().authorities(new SimpleGrantedAuthority("WRITE")))
        .contentType(APPLICATION_JSON).content(json))
        .andExpect(status().isCreated());
```

---

## Keycloak Configuration

```properties
# keycloak.conf
db=postgres
db-username=keycloak
db-password=password
db-url=jdbc:postgresql://localhost/keycloak

# SW360 custom providers
spi-events-listener-sw360-add-user-to-couchdb-thrift=http://localhost:8080
spi-storage-sw360-user-storage-jpa-thrift=http://localhost:8080
```

---

## Security DO's and DON'Ts

### ✅ DO
- Use `@PreAuthorize("hasAuthority('WRITE')")` on all POST / PUT / PATCH / DELETE endpoints
- Use `@PreAuthorize("hasAuthority('ADMIN')")` on admin-only endpoints
- Check document-level permissions with `makePermission(doc, user).isActionAllowed(action)`
- Use `isUserAtLeast(UserGroup.X, user)` for role-level service checks
- Rely on unified Bearer auth — do not implement custom token parsing

### ❌ DON'T
- Expose `_id`, `_rev`, internal Thrift fields, or stack traces in API responses
- Enable Basic Auth in production
- Hardcode secrets or tokens
- Bypass `@PreAuthorize` for convenience
- Implement custom authentication filters without team review

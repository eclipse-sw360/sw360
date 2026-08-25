<!--
Part of the SW360 Portal Project.
SPDX-License-Identifier: EPL-2.0
-->

# Instructions for AI Agents & Coding Assistants

<!-- Token-efficiency: Load only the instruction files whose `applyTo` glob
     matches the changed file(s). Do NOT preload all files at once. -->

Welcome to **Eclipse SW360** (v20.1.x · Java 21 · Spring Boot 4.1 · Spring Security 7.1 · JUnit 6).

All AI agents, Copilot modes (Ask / Edit / Agent), and automated coding tools
**must** follow these guidelines before suggesting or applying any change.

---

## 1. Quick Reference — Which File to Read First

| Task | Instruction file |
|------|-----------------|
| Architecture, REST endpoints, Thrift, utilities, DO/DON'T rules | [sw360_backend.instructions.md](.github/instructions/sw360_backend.instructions.md) |
| Writing or fixing tests (JUnit 6, MockMvc, ArchUnit) | [sw360_testing.instructions.md](.github/instructions/sw360_testing.instructions.md) |
| Security, authentication, authorization, Keycloak | [sw360_security.instructions.md](.github/instructions/sw360_security.instructions.md) |
| CouchDB repositories, views, indexes, pagination, Nouveau/Lucene search | [sw360_db.instructions.md](.github/instructions/sw360_db.instructions.md) |
| Git commits, branch names, PR checklist | [git-commit.instructions.md](.github/instructions/git-commit.instructions.md) |

> **Token-efficiency note:** Each file uses `applyTo` front-matter so it is
> only loaded by the AI when working on matching files. Do not load all files
> at once unless the task spans multiple layers.

### File-selection precedence (when multiple `applyTo` patterns match)

Apply rules in this order — later rules override earlier ones on conflict:

1. `sw360_backend.instructions.md` — baseline architecture, naming, layering
2. `sw360_db.instructions.md` — CouchDB access patterns (overrides backend if you touch a repository)
3. `sw360_security.instructions.md` — `@PreAuthorize`, auth flow (overrides backend for controllers)
4. `sw360_testing.instructions.md` — test framework choices (JUnit 6 is absolute)
5. `git-commit.instructions.md` — always applies at commit time

---

## 2. Mandatory Pre-Code Checklist

Before generating or modifying any code, an AI agent **must** verify:

- [ ] Which architectural layer is affected? (Controller → Service → Handler → Repository)
- [ ] Does a similar pattern already exist in the same module?
- [ ] Will the change require a new test class or new HTTP-exercising test? (ArchUnit enforces this — build fails otherwise)
- [ ] Does the change touch security or authentication? → Read `sw360_security.instructions.md` first
- [ ] Does the change touch CouchDB queries, indexes, or Nouveau/Lucene search? → Read `sw360_db.instructions.md` first
- [ ] Is the EPL-2.0 file header present on every new file?

---

## 3. Autonomy Boundaries

### ✅ Agent MAY do autonomously
- Generate new code following existing patterns in the same module
- Add/update unit tests and HTTP-exercising integration tests
- Apply `mvn spotless:apply` formatting
- Add OpenAPI `@Operation` / `@ApiResponse` annotations
- Add `@PreAuthorize` to new endpoints per the security guide (WRITE/ADMIN/READ)
- Refactor within a single layer **without** changing public method signatures
  on `@Service`, `@RestController`, `*Handler`, or `*Repository` beans

### ⚠️ Agent MUST ask before doing
- Changing Thrift `.thrift` service definitions (impacts all consumers)
- Modifying `JacksonCustomizations.java` (affects all REST API responses)
- **Removing or weakening** an existing `@PreAuthorize` annotation
- Changing database views or indexes (requires migration assessment)
- Bumping **major** versions in `pom.xml` (Spring Boot, Spring Security, Thrift,
  Jackson, JUnit). Dependabot-style minor/patch bumps are fine autonomously.

### ❌ Agent MUST NOT do
- Push or commit directly to `main` — PRs only
- Expose stack traces, internal IDs, or `_rev` fields in API responses
- Use `System.out.println()` or `e.printStackTrace()` anywhere
- Use JUnit 4 or JUnit 5 APIs — JUnit 6 only for new tests
- Use field injection (`@Autowired` on fields) — constructor injection only
- Add `@Autowired` on constructors — Spring Boot 4 auto-wires single constructors
- Inject `ThriftClients` as a bean — use static factory methods on the class
- Hardcode secrets, passwords, or configuration values

---

## 4. Required PR Checks (all must pass)

- ✅ `mvn package` — build succeeds
- ✅ `mvn test` — all tests pass (CouchDB required: `./scripts/startCouchdbForTests.sh`)
- ✅ `mvn spotless:check` — formatting clean
- ✅ `mvn license:check` — EPL-2.0 headers present
- ✅ Conventional commit format (see `git-commit.instructions.md`)
- ✅ ECA (Eclipse Contributor Agreement) signed
- ✅ No critical dependency vulnerabilities (`dependency-review.yml`)
- ✅ ArchUnit test-coverage rules pass (see `sw360_testing.instructions.md`)

---

## 5. File Header (Required on ALL new files)

**Java:**
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

**YAML / Properties / Shell:**
```
# Copyright <Copyright Holder>, <year>.
# Part of the SW360 Portal Project.
# SPDX-License-Identifier: EPL-2.0
```

---

Following these instructions is mandatory to maintain the quality and integrity
of the SW360 codebase.

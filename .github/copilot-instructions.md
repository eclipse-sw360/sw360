<!--
Part of the SW360 Portal Project.
SPDX-License-Identifier: EPL-2.0
-->

# GitHub Copilot Instructions — Eclipse SW360

> **Stack:** Java 21 · Spring Boot 4.1 · Spring Security 7.1 · JUnit 6 · Thrift 0.20.0 · CouchDB (Cloudant SDK)

Copilot (Ask / Edit / Agent modes) and all other AI coding assistants **must**
follow the project-wide agent rules in [`AGENTS.md`](../AGENTS.md) and the
scoped instruction files under [`.github/instructions/`](instructions/).

## Instruction files (loaded on demand via `applyTo` globs)

| Scope | File |
|-------|------|
| Architecture, REST, Thrift, utilities, DO/DON'T rules | [`.github/instructions/sw360_backend.instructions.md`](instructions/sw360_backend.instructions.md) |
| Tests (JUnit 6, MockMvc, ArchUnit coverage rules) | [`.github/instructions/sw360_testing.instructions.md`](instructions/sw360_testing.instructions.md) |
| Security, authentication, authorization, Keycloak | [`.github/instructions/sw360_security.instructions.md`](instructions/sw360_security.instructions.md) |
| CouchDB repositories, views, indexes, Nouveau/Lucene search | [`.github/instructions/sw360_db.instructions.md`](instructions/sw360_db.instructions.md) |
| Git commits, branch names, PR checklist | [`.github/instructions/git-commit.instructions.md`](instructions/git-commit.instructions.md) |

## Rules of engagement

- Load only the instruction file(s) whose `applyTo` glob matches the file(s)
  you are editing — do **not** preload all files at once.
- On conflict, the precedence defined in [`AGENTS.md` § 1](../AGENTS.md) applies
  (backend → db → security → testing → git-commit).
- Read [`AGENTS.md` § 2–3](../AGENTS.md) for the mandatory pre-code checklist
  and autonomy boundaries before generating or modifying code.

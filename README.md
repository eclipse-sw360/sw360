# SW360

[![Website](https://img.shields.io/badge/website-SW360-blue)](https://eclipse.dev/sw360/)
[![Eclipse Public License 2.0](https://img.shields.io/badge/license-EPL--2.0-green.svg "Eclipse Public License 2.0")](LICENSE)
[![GitHub release (latest by date)](https://img.shields.io/github/v/release/eclipse/sw360)](https://github.com/eclipse/sw360/releases/latest)
[![Slack Channel](https://img.shields.io/badge/slack-sw360chat-blue.svg?longCache=true&logo=slack)](https://join.slack.com/t/sw360chat/shared_invite/enQtNzg5NDQxMTQyNjA5LThiMjBlNTRmOWI0ZjJhYjc0OTk3ODM4MjBmOGRhMWRmN2QzOGVmMzQwYzAzN2JkMmVkZTI1ZjRhNmJlNTY4ZGI)
[![Changelog](https://badgen.net/badge/changelog/%E2%98%85/blue)](https://github.com/eclipse/sw360/blob/master/CHANGELOG.md)
[![SW360 Build and Test](https://github.com/eclipse-sw360/sw360/workflows/SW360%20Build%20and%20Test/badge.svg)](https://github.com/eclipse-sw360/sw360/actions?query=workflow:"SW360+Build+and+Test")
[![OpenSSF Best Practices](https://www.bestpractices.dev/projects/9485/badge)](https://www.bestpractices.dev/projects/9485)

**SW360** is a comprehensive software component catalog application designed to act as a central hub for managing software components, licenses, obligations, vulnerabilities, and software bill of materials (SBOM) metadata across projects.

Visit the [official SW360 website](https://eclipse.dev/sw360/) for more information and documentation.

<img width="1280" alt="SW360 Home Interface" src="https://github.com/user-attachments/assets/3c2e6712-97a7-4637-80b5-915cdd3af1e8" />

---

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [System Architecture](#system-architecture)
- [Project Structure](#project-structure)
- [Getting Started & Development](#getting-started--development)
  - [Prerequisites](#prerequisites)
  - [Option A: Docker Setup (Recommended)](#option-a-docker-setup-recommended)
  - [Option B: Local Bare Metal Build](#option-b-local-bare-metal-build)
- [Security Configuration](#security-configuration)
  - [HTTP Basic Authentication](#http-basic-authentication)
  - [Spring Profiles](#spring-profiles)
- [Contributing & Community](#contributing--community)
- [Reporting Issues](#reporting-issues)
- [License](#license)

---

## Overview

SW360 provides an enterprise-grade backend service and REST API for maintaining projects, products, and software components throughout their lifecycle. It allows organizations to:
- Track software components and dependencies across multiple projects.
- Manage SPDX files and software bill of materials (SBOM) data.
- Ensure license compliance and maintain obligations and legal clearance workflows.
- Integrate with security analysis engines (such as FOSSology and CVE search engines).

The reference deployment platform is **Ubuntu Server 22.04 LTS**.

---

## Key Features

- **Component & Release Catalog:** Central repository for tracking software components, versions, and origins.
- **License Management & SPDX:** Support for importing and processing SPDX documents for software clearing.
- **REST API & Integrations:** Built-in REST API supporting OAuth2/JWT authorization for third-party tooling integration.
- **Extensible Architecture:** Tomcat-based Apache Thrift services paired with CouchDB persistence.

---

## System Architecture

SW360 consists of four primary subsystems:

1. **Frontend (SW360 Portal):** Web user interface for catalog management and administration.
2. **Backend Services:** Tomcat-based Apache Thrift microservices serving core business logic.
3. **Database Layer:** Apache CouchDB for storing component metadata, attachments, and relationships.
4. **REST API:** Spring-based REST API providing OAuth2/JWT secured endpoints for external tools.

---

## Project Structure

SW360 is structured as a multi-module Maven project:

```text
sw360/
├── backend/              # Thrift-based RPC services & business logic handlers
├── rest/                 # REST API, Authorization Server, and Resource Server
├── client/               # Client libraries for interacting with SW360 services
├── libraries/            # Core utilities, data models, and CouchDB persistence layers
├── keycloak/             # Keycloak integration and authentication configurations
├── build-configuration/  # Shared Maven checkstyle, spotless, and build plugins
├── scripts/              # Auxiliary setup, deployment, and configuration scripts
└── third-party/          # External definitions and Thrift code generation scripts
```

---

## Getting Started & Development

### Prerequisites

Before building or running SW360 locally, ensure you have the following installed:

- **Java JDK 21**
- **Maven 3.8.7+**
- **Docker & Docker Compose** (Recommended for local setup)
- **Python 3.x** (for running [`pre-commit`](https://pre-commit.com/))
- **Apache Thrift 0.20.0** runtime (Required for local bare-metal builds)

---

### Option A: Docker Setup (Recommended)

The fastest way to get SW360 running locally is via Docker:

1. Clone the repository:
   ```bash
   git clone https://github.com/eclipse-sw360/sw360.git
   cd sw360
   ```

2. Build the Docker images using the provided script:
   ```bash
   ./docker_build.sh
   ```

For comprehensive details on container configuration, environment variables, secrets, and Compose references, consult the [SW360 Docker Setup Guide](README_DOCKER.md) and the [Container Deployment Docs](https://eclipse.dev/sw360/docs/deployment/containers/).

---

### Option B: Local Bare Metal Build

If you prefer building and running SW360 directly on host hardware:

#### Step 1: Install Pre-Commit Hooks
SW360 enforces code formatting rules via Spotless. Install `pre-commit` to auto-format changes:
```bash
pip install pre-commit
pre-commit install
```

#### Step 2: Install Thrift Runtime
If Apache Thrift 0.20.0 is not installed natively on your system, install CMake and a C++ compiler, then run:
```bash
./third-party/thrift/install-thrift.sh
```

#### Step 3: Build Source Code
SW360 Maven builds require setting the `base.deploy.dir` deployment target property (pointing to your Tomcat installation path):

```bash
mvn clean package -P deploy \
    -Dhelp-docs=false \
    -DskipTests \
    -Dbase.deploy.dir=$TOMCAT_HOME
```

*Note: Running unit & integration tests requires a running local CouchDB instance via Docker.*

For complete bare-metal setup steps, see the [Bare Metal Deployment Documentation](https://eclipse.dev/sw360/docs/deployment/baremetal/).

---

## Security Configuration

Review security configurations prior to staging or production deployments. For further details, refer to the [Securing SW360 Deployment Guide](https://eclipse.dev/sw360/docs/administrationguide/securing-sw360/).

### HTTP Basic Authentication

HTTP Basic Auth is **disabled by default** in production profiles, but can be enabled for local development.

| Deployment Mode | How to Enable HTTP Basic Auth |
|---|---|
| **Docker** | Set `SW360_SECURITY_HTTP_BASIC_ENABLED=true` in `config/sw360/.env.backend` |
| **Bare Metal** | Set `sw360.security.http-basic.enabled=true` in `application.yml` (or via JVM arg) |
| **Spring Profile** | Activate the `prod` profile (sets flag to `false`). Omit `prod` for dev defaults. |

> ⚠️ **Security Warning:** Do not enable HTTP Basic Authentication in production environments. Use OAuth2/JWT or Keycloak authentication.

### Spring Profiles

| Profile | Description |
|---|---|
| *(default / none)* | Development mode — HTTP Basic authentication enabled, permissive defaults |
| `prod` | Production mode — HTTP Basic authentication disabled, strict security overrides |

Activate the production profile:
```bash
# Via JVM Argument
-Dspring.profiles.active=prod

# Via Environment Variable
export SPRING_PROFILES_ACTIVE=prod
```

---

## Contributing & Community

Contributions are welcome! To get involved:

1. **Sign the Eclipse Contributor Agreement (ECA):** All contributors must sign the [Eclipse ECA](https://www.eclipse.org/legal/ECA.php).
2. **Review Guidelines:** Read [CONTRIBUTING.md](CONTRIBUTING.md) for details on feature branch workflows, commit signatures (`-s`), and licensing requirements.
3. **Join Community Channels:** Connect with maintainers and contributors on the [SW360 Slack Channel](https://join.slack.com/t/sw360chat/shared_invite/enQtNzg5NDQxMTQyNjA5LThiMjBlNTRmOWI0ZjJhYjc0OTk3ODM4MjBmOGRhMWRmN2QzOGVmMzQwYzAzN2JkMmVkZTI1ZjRhNmJlNTY4ZGI) or the `sw360-dev@eclipse.org` mailing list.

---

## Reporting Issues

If you encounter bugs, documentation typos, or security vulnerabilities:
- **Bugs & Feature Requests:** Open an issue on [GitHub Issues](https://github.com/eclipse-sw360/sw360/issues).
- **Security Vulnerabilities:** Do not report security flaws publicly. Follow the [Eclipse Vulnerability Reporting Policy](https://www.eclipse.org/security/) as outlined in [SECURITY.md](SECURITY.md).

---

## License

SW360 is licensed under the [Eclipse Public License 2.0 (EPL-2.0)](LICENSE).

```text
SPDX-License-Identifier: EPL-2.0

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
https://www.eclipse.org/legal/epl-2.0/
```


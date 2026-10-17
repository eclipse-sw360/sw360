<!--
Copyright Siemens AG, 2026. Part of the SW360 Portal Project.

This program and the accompanying materials are made
available under the terms of the Eclipse Public License 2.0
which is available at https://www.eclipse.org/legal/epl-2.0/

SPDX-License-Identifier: EPL-2.0
-->

# SW360 Java Client SDK

A Java SDK for building clients that communicate with the SW360 REST API. Its
primary purpose is to let applications and integrations talk to an SW360
instance programmatically. End-to-end system testing in CI is one additional
use case it supports. Clients can authenticate against a `PROD` or `stage`
environment with either a bearer token or user credentials.

## Modules

| Module | Purpose |
|--------|---------|
| `http-support` | Async HTTP transport and request/response DSL |
| `client` | REST clients, sync/async adapters, and the connection factory |

## Bootstrap

`SW360ConnectionFactory` is the single entry point. It returns an
`SW360Connection` that exposes one adapter per supported domain. The core
resource domains expose synchronous and asynchronous flavors; the public version
endpoint is currently exposed as a synchronous adapter only.

## Authentication

You need **either** a bearer token **or** credentials — not both. Pick one of
the two options below.

### Option 1 — Token-first (recommended)

Supply a pre-generated SW360 REST API token. No authentication URL or
credentials are required in this mode.

```java
SW360ClientConfig config = SW360ClientConfig.builder()
        .baseUrl("https://sw360.example.org/resource/api")
        .token(System.getenv("SW360_TOKEN"))
        .build();

SW360Connection connection = new SW360ConnectionFactory().newConnection(config);

VersionData version = connection.getVersionAdapter().getVersion();
SW360User profile = connection.getUserAdapter().getUserProfile();
```

The HTTP client and JSON object mapper are created automatically when not
supplied. Pass your own with `.httpClient(...)` / `.objectMapper(...)` to reuse
a shared, pooled client.

### Option 2 — Credential exchange

Instead of a token, provide OAuth client credentials and let the SDK obtain a
token on your behalf via the OAuth2 **client-credentials** grant. Do **not**
combine this with `.token(...)`; when a token is present it always wins.

```java
SW360ClientConfig config = SW360ClientConfig.builder()
        .baseUrl("https://sw360.example.org/resource/api")
        .authUrl("https://sw360.example.org/authorization/oauth/token")
        .clientId(System.getenv("SW360_CLIENT_ID"))
        .clientSecret(System.getenv("SW360_CLIENT_SECRET"))
        .build();

SW360Connection connection = new SW360ConnectionFactory().newConnection(config);
```

> **Note:** The SW360 authorization server issues tokens through the OAuth2
> **client-credentials** grant; the legacy password grant (username/password)
> is no longer supported.

## Authentication modes

| Mode | When to use |
|------|-------------|
| Bearer token | Preferred for programmatic and production-like use |
| Credential exchange | Mints a token on demand via the client-credentials grant |

The mode is selected automatically: when a token is present it is used directly;
otherwise the SDK exchanges the OAuth client credentials for a token.

## Working usage: find a project

The following example connects with a bearer token, searches for a project by
name, and resolves a specific project by name and version. Set `SW360_TOKEN` to
a valid API token before running it.

```java
SW360ClientConfig config = SW360ClientConfig.builder()
        .baseUrl("https://sw360.example.org/resource/api")
        .token(System.getenv("SW360_TOKEN"))
        .build();

SW360Connection connection = new SW360ConnectionFactory().newConnection(config);
SW360ProjectClientAdapter projects = connection.getProjectAdapter();

// Search for projects matching a name
List<SW360Project> matches = projects.search(
        ProjectSearchParams.builder().withName("My Project").build());

// Look up one project by its name and version
Optional<SW360Project> project =
        projects.getProjectByNameAndVersion("My Project", "1.0.0");
```

## Support matrix

| Domain | Adapter surface | Status |
|--------|------------------|--------|
| Version | `getVersionAdapter()` | Supported (sync only) |
| Users / profile / API tokens | `getUserAdapter()` / `getUserAdapterAsync()` | Supported |
| Components | `getComponentAdapter()` / `getComponentAdapterAsync()` | Supported |
| Releases | `getReleaseAdapter()` / `getReleaseAdapterAsync()` | Supported |
| Projects | `getProjectAdapter()` / `getProjectAdapterAsync()` | Supported |
| Licenses | `getLicenseAdapter()` / `getLicenseAdapterAsync()` | Supported |
| Vulnerabilities | `getVulnerabilityAdapter()` / `getVulnerabilityAdapterAsync()` | Supported |
| Vendors, Packages | — | Planned |
| Reports, ECC, Clearing requests | — | Out of scope for now |

All currently supported domain adapters except version expose an `...Async`
counterpart returning `CompletableFuture`.

## CI / standalone live integration tests

Real-environment integration tests are opt-in and configured through
`src/test/resources/rest-test.properties` or system properties. They are skipped
by default so the normal build stays hermetic.

```powershell
# Run unit and mock-server tests only (default, no live server)
mvn -pl clients/http-support,clients/client -am test

# Run against a live SW360 environment
mvn -pl clients/client -am verify -DRunRestIntegrationTest=true
```

Provide the environment and credentials in `rest-test.properties`:

```ini
run_rest_integration_test=false
rest_base_url=https://sw360.example.org/resource/api
oauth_base_url=https://sw360.example.org/authorization/oauth/token
oauth_client_id=
oauth_client_secret=
oauth_token=
```

Set `oauth_token` for token-first execution, or the OAuth `oauth_client_id` and
`oauth_client_secret` for client-credentials execution.

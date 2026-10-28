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
environment with a SW360 REST API token, an OAuth 2.0 bearer token, or HTTP
Basic credentials.

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

SW360 supports three distinct authentication modes. Pick **exactly one** — you
never combine them. The SDK selects the matching `Authorization` header scheme
automatically:

| Mode | Builder inputs | Header sent |
|------|----------------|-------------|
| API token | `.token(...)` | `Authorization: Token <token>` |
| OAuth client-credentials | `.authUrl(...)` + `.clientId(...)` + `.clientSecret(...)` | `Authorization: Bearer <jwt>` |
| HTTP Basic | `.username(...)` + `.password(...)` | `Authorization: Basic <base64>` |

A token takes precedence over everything else; Basic authentication is only
available when the server administrator has enabled it.

### Option 1 — SW360 REST API token (recommended)

Supply a per-user SW360 REST API token, generated from the `/users/tokens`
endpoint or the **Preferences** page in the UI. No authentication URL or
credentials are required in this mode. The SDK sends the token with the
`Token ` scheme prefix (`Authorization: Token <token>`), which is what the
SW360 resource server expects for these tokens — **not** `Bearer `.

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

### Option 2 — OAuth bearer token (credential exchange)

Instead of an API token, provide OAuth client credentials and let the SDK
obtain an OAuth 2.0 bearer token (a Keycloak JWT) on your behalf via the
**client-credentials** grant. The resulting token is sent with the `Bearer `
scheme prefix (`Authorization: Bearer <jwt>`). Do **not** combine this with
`.token(...)`; when a token is present it always wins.

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

### Option 3 — HTTP Basic authentication (only if the admin enabled it)

SW360's resource server can accept plain HTTP Basic authentication
(user name + password) against the REST API **when the server administrator
has explicitly enabled it**. This is intended for development and testing
only and must never be relied upon in production. Provide `username` and
`password` and the SDK sends an `Authorization: Basic <...>` header on every
request.

```java
SW360ClientConfig config = SW360ClientConfig.builder()
        .baseUrl("https://sw360.example.org/resource/api")
        .username(System.getenv("SW360_USERNAME"))
        .password(System.getenv("SW360_PASSWORD"))
        .build();

SW360Connection connection = new SW360ConnectionFactory().newConnection(config);
```

> **Warning:** Basic Auth transmits reusable credentials on every call. Only
> use it against a server where the admin has enabled it, preferably over TLS,
> and prefer a bearer token or client-credentials for anything production-like.

## Authentication modes

| Mode | Header scheme | When to use |
|------|---------------|-------------|
| API token | `Token ` | Per-user token from `/users/tokens` (or UI Preferences); preferred for programmatic and production-like use |
| OAuth client-credentials | `Bearer ` | Mints an OAuth 2.0 / Keycloak JWT bearer token on demand via the client-credentials grant |
| Basic authentication | `Basic ` | User name + password; only when the server admin has enabled Basic Auth (dev/test) |

The mode is selected automatically: when a token is present it is used directly
with the `Token ` scheme; otherwise, if a user name and password are supplied,
HTTP Basic authentication is used; otherwise the SDK exchanges the OAuth client
credentials for a `Bearer ` JWT.

## Working usage: find a project

The following example connects with a SW360 REST API token, searches for a
project by name, and resolves a specific project by name and version. Set
`SW360_TOKEN` to a valid API token before running it.

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

# SW360 Backend Docker

> **WARNING**: This readme refers to main branch. [This is the Docker documentation for stable 18.x series](https://github.com/eclipse-sw360/sw360/blob/releases/18/README_DOCKER.md)

This document is focused on the **SW360 backend container image**:
build, backend runtime configuration, secrets semantics, and persistence behavior.

For end-to-end container stack setup (compose file, frontend, Keycloak, Nginx,
and startup flow), use:
**[SW360 Frontend Docker Guide](https://github.com/eclipse-sw360/sw360-frontend/blob/main/README_DOCKER.md)**.

## Table of Contents

* [Building](#building)
* [Runtime Configuration Model](#runtime-configuration-model)
  * [Environment Variables](#environment-variables)
  * [Secrets](#secrets)
  * [Entrypoint Behavior and Config Materialization](#entrypoint-behavior-and-config-materialization)
* [Volumes and Persistence](#volumes-and-persistence)
* [Networking](#networking)
* [FOSSology Integration](#fossology-integration)
* [Compose Reference (Canonical)](#compose-reference-canonical)

## Building

* Install a recent Docker version with buildx support.
* Build images using:

  ```sh
  ./docker_build.sh
  ```

This builds the Thrift image, SW360 binaries image, and SW360 runtime image.

If you need to inject a CVE-Search host at build time:

```sh
./docker_build.sh --cvesearch-host <HOST_URL>
```

`<HOST_URL>` format: `http://<YOUR_SERVER_HOST>:<PORT>`
(or `https://cvepremium.circl.lu` for testing only).

To change image root (default `ghcr.io/eclipse-sw360`):

```sh
DOCKER_IMAGE_ROOT=myregistry.com/sw360 ./docker_build.sh
```

## Runtime Configuration Model

The backend image is configured at startup by environment variables and secrets.
`docker-entrypoint.sh` writes effective runtime config into `/etc/sw360`.

### Environment Variables

#### CouchDB settings
* `COUCHDB_URL`: URL of the CouchDB instance (default: `http://couchdb:5984`).
* `COUCHDB_LUCENESEARCH_LIMIT`: Limit for Lucene search results (default: `1000`).
* `CLOUDANT_ENABLE_RETRIES`: Enable retries in Cloudant (default: `true`).
* `CLOUDANT_MAX_RETRIES`: Max Cloudant retries when enabled (default: `2`).
* `CLOUDANT_MAX_RETRY_INTERVAL`: Max retry interval in seconds (default: `5`).
* `CLOUDANT_POOL_MAX_IDLE_CONNECTIONS`: Optional OkHttp pool idle-connection cap
    (default: `-1`, disabled).
* `CLOUDANT_POOL_KEEPALIVE_SECONDS`: Optional pooled connection keepalive in
    seconds (default: `-1`, disabled).
* `CLOUDANT_MAX_REQUESTS`: Optional OkHttp dispatcher max in-flight requests
    (default: `-1`, disabled).
* `CLOUDANT_MAX_REQUESTS_PER_HOST`: Optional OkHttp dispatcher max in-flight
    requests per host (default: `-1`, disabled).

General configuration variables are stored in
[config/sw360/.env.backend](config/sw360/.env.backend). You can modify this file
to tweak SW360 behaviour.

#### Thrift backend connection pooling
* `BACKEND_THRIFT_MAX_CONNECTIONS_TOTAL`: Max total pooled connections to backend (default: `200`).
* `BACKEND_THRIFT_MAX_CONNECTIONS_PER_ROUTE`: Max pooled connections per backend route (default: `100`).
* `BACKEND_THRIFT_IDLE_EVICT_SECONDS`: Validate idle pooled connections before reuse; set below
    Tomcat's `keepAliveTimeout` (default: `15` seconds).
* `BACKEND_THRIFT_CONNECTION_TTL_SECONDS`: Force-retire pooled connections older than this,
    even if active (default: `60` seconds).

#### Spring controllers / resource server
* `ENABLE_DISKSPACE`: Enable disk space health check (default: `false`).
* `SW360_SECURITY_JWT_ISSUERS_<N>_ISSUER_URI`: Public issuer URL for slot
    `<N>` (0-based). Validated against the `iss` claim of incoming Bearer
    tokens, so the value must exactly match the token issuer (scheme, host,
    port, context path, trailing slash). Configure one slot per trusted
    identity provider, e.g. the built-in SW360 Authorization Server and a
    Keycloak realm. Defaults:
    * `SW360_SECURITY_JWT_ISSUERS_0_ISSUER_URI=http://localhost:8080/authorization`
    * `SW360_SECURITY_JWT_ISSUERS_1_ISSUER_URI=http://localhost:8083/realms/sw360`
* `SW360_SECURITY_JWT_ISSUERS_<N>_JWK_SET_URI`: *(Optional)* JWKS endpoint URL
    for slot `<N>`. When set, SW360 skips OpenID Connect
    discovery and fetches JWKS directly from this URL. Useful when the
    identity provider sits behind a reverse proxy with a self-signed or
    privately-issued certificate; the resource server can reach the JWKS
    endpoint over a loopback or internal URL while clients keep using the
    public issuer URL. Leave unset to use discovery against `_ISSUER_URI`.

    Both variables are bound directly to
    `sw360.security.jwt.issuers[N].{issuer-uri,jwk-set-uri}` via Spring Boot's
    relaxed environment-variable binding; nothing needs to be templated in
    `application.yml`.

    The trusted issuer list is consumed by both `/resource` and
    `/authorization` Bearer JWT validation paths, so the authorization server
    and resource server can validate both local SW360 tokens and external
    Keycloak tokens against the same issuer/JWKS configuration.

#### Email
* `EMAIL_PROPERTIES_HOST`: SMTP host (empty by default). Let it **empty** to
    disable email service.
* `EMAIL_PROPERTIES_PORT`: SMTP port (empty by default).
* `EMAIL_PROPERTIES_STARTTLS`: Enable STARTTLS (default: `false`).
* `EMAIL_PROPERTIES_ENABLE_SSL`: Enable SSL (default: `false`).
* `EMAIL_PROPERTIES_AUTH_REQUIRED`: Authentication required (default: `false`).
* `EMAIL_PROPERTIES_FROM`: Sender email address (default:
    `__No_Reply__@sw360.org`).
* `EMAIL_PROPERTIES_SUPPORT_EMAIL`: Support email address (default:
    `help@sw360.org`).
* `EMAIL_PROPERTIES_TLS_PROTOCOL`: TLS protocol version (default: `TLSv1.2`).
* `EMAIL_PROPERTIES_TLS_TRUST`: Trusted certificates (default: `*`).
* `EMAIL_PROPERTIES_DEBUG`: Enable mail debug logging (default: `false`).

Leave host/port empty to disable email service.

#### SVM
* `SVM_API_BASE_PATH`: Base path of SVM API (default:
    `https://svm.example.org`).
* `SVM_API_ROOT_PATH`: API root path (default: `api/v1`).
* `SVM_SW360_API_URL`: SW360 data API URL for SVM (default:
    `https://svm.example.org/application.json`).
* `SVM_SW360_CERTIFICATE_FILENAME`: Certificate file name to push monitoring
    list information. To use, put the certificate file in the `etc` named
    volume, update this variable and `SVM_SW360_CERTIFICATE_PASSPHRASE` in
    [Secrets](#secrets).

**Note:** Make sure the API URLs are not starting or ending with `/`.

#### Other runtime settings
* `SCHEDULER_AUTOSTART_SERVICES`: Comma-separated list (no spaces) of services
    to autostart (default: `cvesearchService`). Leave empty to not start any
    service.
* `SW360_CORS_ALLOWED_ORIGIN`: CORS allowed origins. By default, it is set to
    `*` for ease of local development. **To secure your deployment for
    production**, you must update this value within
    `config/sw360/.env.backend` to reflect the specific origin(s) of your
    frontend server.
* `SW360_THRIFT_SERVER_URL`: URL where Thrift server is running (default:
    `http://localhost:8080`).
* `SW360_BASE_URL`: Base URL for SW360 server (default: `http://localhost:8080`).

#### Security settings
* `SW360_SECURITY_HTTP_BASIC_ENABLED`: Whether HTTP Basic authentication is
    enabled on both the authorization and resource servers (default: `true`).
    Set this to `false` in production - clients should authenticate via
    OAuth2/JWT or API token. Set to `true` only for local development or
    integration testing where Basic auth is needed for convenience.
* `JWT_SECRETKEY`: Password used by the Authorization Server to open
    `/etc/sw360/jwt-keystore.jks` (default: `sw360SecretKey`).
    **Change this in production** and keep it identical on every SW360 node
    sharing the same JWT signing keystore.

#### JWT Signing Key
* The Authorization Server signs tokens with a JKS keystore stored at
  `/etc/sw360/jwt-keystore.jks` (persisted by the `etc` named volume).
* Startup seed order for `jwt-keystore.jks`:
  1. Docker secret `JWT_KEYSTORE` (mounted at `/run/secrets/JWT_KEYSTORE`)
  2. Existing `/etc/sw360/jwt-keystore.jks`
  3. Bundled fallback `/app/sw360/jwt-keystore.jks`
* To provide your own key, generate one and mount it via the `JWT_KEYSTORE`
  compose secret or place it directly into `/etc/sw360/jwt-keystore.jks`.
* Use `rest/rest-common/tools/generateJwtStore.sh` to generate a
  replacement keystore and keep `JWT_SECRETKEY` aligned with that keystore.

### Secrets

Backend runtime consumes these secret values:

* `COUCHDB_USER`
* `COUCHDB_PASSWORD`
* `SVM_SW360_CERTIFICATE_PASSPHRASE`
* `SVM_SW360_JKS_PASSWORD`
* `REST_APITOKEN_HASH_SALT`
* `EMAIL_PROPERTIES_USERNAME`
* `EMAIL_PROPERTIES_PASSWORD`
* `JWT_KEYSTORE` (binary JKS payload)

`REST_APITOKEN_HASH_SALT` must be OpenBSD bcrypt salt format:
`$2a$<cost>$<22-char-salt>`.
Keep it stable after deployment; changing it invalidates existing API tokens.

Generate a value:

```sh
REST_APITOKEN_HASH_SALT='$2a$04$'$(openssl rand -hex 16 | head -c 22)
printf "REST_APITOKEN_HASH_SALT='%s'\n" "$REST_APITOKEN_HASH_SALT"
```

### Entrypoint Behavior and Config Materialization

At container start, backend config is materialized in `/etc/sw360` by
[`scripts/docker-config/docker-entrypoint.sh`](scripts/docker-config/docker-entrypoint.sh).

JWT signing keystore seed order:
1. Secret `JWT_KEYSTORE` (if provided)
2. Existing persisted `/etc/sw360/jwt-keystore.jks`
3. Bundled fallback keystore from image

To replace JWT signing key material:
* generate a new JKS (helper script:
  `rest/rest-common/tools/generateJwtStore.sh`)
* provide it via `JWT_KEYSTORE` or persist it under `/etc/sw360`
* keep `JWT_SECRETKEY` aligned with that keystore

## Volumes and Persistence

Backend-relevant persistence surfaces:

* `/etc/sw360`:
  generated backend runtime configuration and JWT keystore
* optional additional files expected by runtime:
  * JWT keystore: `/etc/sw360/jwt-keystore.jks`
  * SVM certificate / trust store material (when SVM TLS integration is enabled)

Persist `/etc/sw360` across restarts to retain generated config and keystore
state.

## Networking

The backend container must be able to reach:
* CouchDB (`COUCHDB_URL`)
* trusted issuer discovery/JWKS endpoints for configured JWT issuers
* optional integrations (mail server, SVM, CVE source, FOSSology)

It should also be reachable by:
* frontend (API calls)
* reverse proxy (if used)

## FOSSology Integration

For containerized deployments, the official FOSSology image is recommended:
[fossology/fossology](https://hub.docker.com/r/fossology/fossology/).

Backend-side FOSSology setup is still performed in SW360 Admin UI / API:
* configure FOSSology URL
* folder ID
* API token

## Compose Reference (Canonical)

Compose usage, full stack startup, frontend build-time variables, and Keycloak
bootstrap are maintained in:

* **Main guide**: <https://github.com/eclipse-sw360/sw360-frontend/blob/main/README_DOCKER.md>
* **Stack setup**: <https://github.com/eclipse-sw360/sw360-frontend/blob/main/README_DOCKER.md#stack-setup>
* **Secrets generation**: <https://github.com/eclipse-sw360/sw360-frontend/blob/main/README_DOCKER.md#secrets-and-credential-generation>
* **Keycloak initialization**: <https://github.com/eclipse-sw360/sw360-frontend/blob/main/README_DOCKER.md#4-initialize-keycloak-one-time>

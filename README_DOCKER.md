# SW360 Docker

> **WARNING**: This readme refers to main branch. [This is the Docker documentation for stable 18.x series](https://github.com/eclipse-sw360/sw360/blob/releases/18/README_DOCKER.md)

## Table of Contents

* [Building](#building)
* [Configuration](#configuration)
  * [Environment Variables](#environment-variables)
  * [Secrets](#secrets)
* [Running the Image](#running-the-image)
* [Volumes and Persistence](#volumes-and-persistence)
* [Networking](#networking)
* [FOSSology Integration](#fossology-integration)

## Building

* Install a recent version of Docker build `buildx` support.
* Build images using the provided script:

    ```sh
    ./docker_build.sh
    ```

    This script builds the Thrift image, the SW360 binaries image, and finally
    the SW360 runtime image.

    If you want to specify a
    [CVE-Search](https://github.com/cve-search/cve-search) host at build time
    (modifies properties file before build), run as follows:

    ```sh
    ./docker_build.sh --cvesearch-host <HOST_URL>
    ```

    The `<HOST_URL>` should be in `http://<YOUR_SERVER_HOST>:<PORT>` format,
    or it can be [https://cvepremium.circl.lu](https://cvepremium.circl.lu) for testing purposes only.
* If you want to change the image root (defaults to `ghcr.io/eclipse-sw360`) run
  the script by overriding the flag `DOCKER_IMAGE_ROOT`:

    ```sh
    DOCKER_IMAGE_ROOT=myregistry.com/sw360 ./docker_build.sh
    ```

## Configuration

The SW360 Docker setup uses environment variables and secret files for
configuration at runtime. This allows usage of same base image for various
servers. The entrypoint script
([docker-entrypoint.sh](scripts/docker-config/docker-entrypoint.sh))
reads these variables and updates the configuration files in `/etc/sw360` at
container startup.

### Environment Variables

General configuration variables are stored in
[config/sw360/.env.backend](config/sw360/.env.backend). You can modify this
file to tweak SW360 behaviour.

**CouchDB Settings**
* `COUCHDB_URL`: URL of the CouchDB instance (default: `http://couchdb:5984`).
* `COUCHDB_LUCENESEARCH_LIMIT`: Limit for Lucene search results (default: `1000`).
* `CLOUDANT_ENABLE_RETRIES`: Enable retries in Cloudant (default: `true`).

**Spring Controllers**
* `ENABLE_DISKSPACE`: Enable disk space health check (default: `false`).
* `JWKS_ISSUER_URI`: URI for JWKS issuer (default:
    `http://localhost:8080/authorization/oauth2/jwks`). Use
    `http://localhost:8083/realms/sw360/protocol/openid-connect/certs` for
    KeyCloak based setup.
* `JWKS_SET_URI`: URI for JWKS set (default:
    `http://localhost:8080/authorization/oauth2/jwks`).
* `JWKS_ISSUER`: Issuer URL (default: `http://localhost:8090`).

**Email Configuration**
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

**SVM Configs**
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

**Other Settings**
* `SCHEDULER_AUTOSTART_SERVICES`: Comma-separated list (no spaces) of services
    to autostart (default: `cvesearchService`). Leave empty to not start any
    service.
* `SW360_CORS_ALLOWED_ORIGIN`: CORS allowed origins (default: `*`).
* `SW360_THRIFT_SERVER_URL`: URL where Thrift server is running (default:
    `http://localhost:8080`).
* `SW360_BASE_URL`: Base URL for SW360 server (default: `http://localhost:8080`).

### Secrets

Sensitive information is managed via secret files located in
`config/couchdb/` and `config/sw360/`.

**CouchDB Secrets ([config/couchdb/default_secrets](config/couchdb/default_secrets))**
* `COUCHDB_USER`: CouchDB username.
* `COUCHDB_PASSWORD`: CouchDB password.

**SW360 App Secrets ([config/sw360/default_secrets](config/sw360/default_secrets))**
* `SVM_SW360_CERTIFICATE_PASSPHRASE`: Passphrase for SVM certificate located by
    `SVM_SW360_CERTIFICATE_FILENAME`.
* `SVM_SW360_JKS_PASSWORD`: Password for ca-cert keystore.
* `REST_APITOKEN_HASH_SALT`: Salt for user generated API token hashing.
* `EMAIL_PROPERTIES_USERNAME`: Username for SMTP authentication.
* `EMAIL_PROPERTIES_PASSWORD`: Password for SMTP authentication.

To update these secrets, simply edit the respective files. The
[docker-compose.yml](docker-compose.yml) is configured to mount these secrets
into the containers.

## Running the Image

* Start the services using Docker Compose:

    ```sh
    docker compose up
    ```

    Add `-d` to run in detached mode:

    ```sh
    docker compose up -d
    ```

    To view logs:

    ```sh
    docker compose logs -f sw360
    ```

## Volumes and Persistence

The `docker-compose.yml` defines several volumes to persist data and
configuration.

**SW360 Service**
* `etc` (named volume) mounted to `/etc/sw360`: Persists generated
    configuration files.

**CouchDB Service**
* `couchdb` (named volume) mounted to `/opt/couchdb/data`: Persists the
    database data.
* `config/couchdb/sw360_setup.ini` mounted to
    `/opt/couchdb/etc/local.d/sw360_setup.ini`: Default CouchDB secrets.
* `config/couchdb/sw360_log.ini` mounted to
    `/opt/couchdb/etc/local.d/sw360_log.ini`: CouchDB logging configuration.
* `config/couchdb/nouveau.ini` mounted to
    `/opt/couchdb/etc/local.d/nouveau.ini`: Inform CouchDB about Nouveau service.

## Networking

The services run in a default network called `sw360net`. This allows services to
communicate with each other securely without using host network. External
containers can connect to the services attached to this network.

## FOSSology Integration

For a Docker-based approach, it is recommended to use the official
[FOSSology Docker image](https://hub.docker.com/r/fossology/fossology/).

Run FOSSology connected to the SW360 network:

```sh
docker run \
    --network sw360net \
    -p 8081:80 \
    --name fossology \
    -e FOSSOLOGY_DB_HOST=<your_db_host> \
    -e FOSSOLOGY_DB_USER=<your_db_user> \
    -e FOSSOLOGY_DB_PASSWORD=<your_db_password> \
    -d fossology/fossology
```

### Configure FOSSology

* **On FOSSology**
  * Login to FOSSology.
  * Create an API token for sw360 to use from Admin > Users > Edit User Account.
      Or check their
      [Wiki for REST API](https://github.com/fossology/fossology/wiki/FOSSology-REST-API#token).
  * Note desired folder's ID.
* **On SW360**
  * Go to Admin > Fossology or the endpoint `POST fossology/saveConfig`.
  * Add the FOSSology host URL (e.g., `http://fossology/repo/api/v2/` if using
      container name, or mapped host port).
  * Add the folder ID (default is `1` for **Software Repository**).
  * Add the API Token obtained from FOSSology as **Access Token**.
  
## Troubleshooting
Inside it, replace the current Docker Network Error section with the following content:

### Docker Network Error

Some contributors may encounter the following error when running:

```sh
docker compose up

Error message:

service "sw360" refers to undefined network sw360-network: invalid compose project
services:
  sw360:
    networks:
      - sw360-network

Update the docker-compose.yml to use the default Docker network or define a valid network.

services:
  sw360:
    networks:
      - default

networks:
  default:
    driver: bridge
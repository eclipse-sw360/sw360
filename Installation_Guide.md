# SW360 Installation Guide

SW360 runs on Ubuntu 22.04 LTS (recommended). Choose Docker for quick evaluation/production or native for customization. Tested with v19.2.0+. Always backup data volumes.

## Prerequisites (All Methods)

*   **OS**: Ubuntu 22.04/Debian 11+ (Reference platform).
*   **RAM/CPU**: 8GB+ RAM, 4 cores.
*   **Ports**: 8080 (SW360/Liferay), 5984 (CouchDB), 5432 (Postgres).
*   **Docker**: v20+ with Compose v2+ (for Docker method).
*   **Java**: JDK 21 (Note: `pom.xml` strictly specifies Java 21).

## Method 1: Docker Compose (Recommended - 10 mins)

Simplest for local/dev/prod; auto-manages CouchDB, Postgres, SW360 in isolated network `sw360net`.

### 1. Clone & Prep
```bash
git clone https://github.com/eclipse-sw360/sw360.git
cd sw360
pip install pre-commit && pre-commit install  # Formatting hooks
```

### 2. Download Compose File
Use the `docker-compose.yml` found in the request root.
*   Edit `.env` (or create one) for secrets if needed (e.g., `COUCHDB_USER=admin`, `COUCHDB_PASSWORD=pass`).
*   *Mac/ARM64 Users:* The default build script may encounter platform mismatch errors. If so, consider the manual native install method.

### 3. Launch
```bash
docker compose up -d  # Pulls/Builds images: sw360, couchdb:3-alpine, postgres:15-alpine
docker compose logs -f sw360  # Tail logs for errors
```

### 4. Initial Setup (Liferay Portal)
*   Open [http://localhost:8080](http://localhost:8080).
*   Login: `setup@sw360.org` / `sw360fossy`.
*   Go to **Control Panel > Apps > Publishing Framework > Import**:
    *   Import `Public_Pages_*.lar` (global pages).
    *   Import `Private_Pages_*.lar` (user dashboard).
    *   *Note:* Locate the `Public_Pages_*.lar` and `Private_Pages_*.lar` files in the release artifacts or the `frontend/` directory (if present).

### 5. Verify
*   Create user/component via portal.
*   Check CouchDB connection:
    ```bash
    docker exec -it sw360_couchdb curl localhost:5984/sw360
    ```

**Troubleshooting:**
*   To reset: `docker compose down -v`
*   Check logs: `docker logs sw360_couchdb`
*   Production: Use persistent volumes `/opt/couchdb/data` and HTTPS proxy.

---

## Method 2: Native Bare-Metal (Custom/Dev - 30+ mins)

For non-Docker environments or deep development.

### 1. Install Dependencies
```bash
sudo apt update && sudo apt install openjdk-21-jdk maven postgresql couchdb python3-pip
pip install pre-commit
sudo systemctl start couchdb postgresql
# Setup CouchDB: Admin user via Fauxton http://localhost:5984/_utils
```

### 2. Install Thrift
The project uses Apache Thrift.
```bash
# If missing, install manually from https://thrift.apache.org/
# Note: The `scripts/install-thrift.sh` referenced in older documentation is currently under maintenance. Please install manally.
```

### 3. Download Liferay (Portal Base)
SW360 uses Liferay CE Portal.
```bash
wget https://releases.liferay.com/portal/7.4.3.18-ga18/liferay-ce-portal-tomcat-7.4.3.18-ga18-20220916153929535.tar.gz
tar -xzf liferay-*.tar.gz -C /opt/liferay/
```

### 4. Build & Deploy
```bash
git clone https://github.com/eclipse-sw360/sw360.git && cd sw360
pre-commit install
mvn clean package -P deploy \
  -DskipTests -Dhelp-docs=false \
  -Dbase.deploy.dir=. \
  -Dliferay.deploy.dir=/opt/liferay/deploy/ \
  -Dbackend.deploy.dir=/opt/liferay/tomcat/webapps/ \
  -Drest.deploy.dir=/opt/liferay/tomcat/webapps/
```

### 5. Configure & Start
*   Copy `backend/src/main/resources/couchdb.properties` to `/opt/liferay/tomcat/lib/ext/`.
*   Deploy `couchdb-lucene.war` to `/opt/liferay/deploy/`.
*   Start server: `/opt/liferay/tomcat/bin/startup.sh`.
*   Repeat Liferay import as in Docker (Locate LAR files in release packages).

### 6. Verify
Check logs:
```bash
tail -f /opt/liferay/logs/catalina.out
```

**Troubleshooting:**
*   **Thrift Errors**: Ensure exact version matches.
*   **Maven OOM**: Run `export MAVEN_OPTS="-Xmx4g"`.
*   **Full Guide**: [Native Deployment Docs](https://eclipse.dev/sw360/docs/deployment/baremetal/deploy-natively/).

---

## Optional: Fossology Integration

```bash
docker run --network sw360net -d fossology/fossology
# In SW360 Admin > External Tools: Add Fossology API token/URL.
```

## Upgrades/Notes

*   **Backup CouchDB**: `docker exec sw360_couchdb couchdb-dump`.
*   **CI/CD**: GitHub Actions deploys Docker images.
*   **Issues?**: [GitHub Discussions](https://github.com/eclipse-sw360/sw360/discussions).

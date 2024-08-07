# SPDX-License-Identifier: EPL-2.0 AND Apache-2.0
# SPDX-FileCopyrightText: © CouchDB Developers <dev@couchdb.apache.org>, 2024 Siemens AG
# SPDX-FileContributor: Gaurav Mishra <mishra.gaurav@siemens.com>
#
# Adopted from https://github.com/apache/couchdb-docker/blob/58910ed097489dc588b2a87592406f8faa1bdadf/3.3.3/Dockerfile

ARG JAVA_VERSION=17
ARG UBUNTU_VERSION=jammy

# Use OpenJDK Eclipe Temurin Ubuntu LTS
FROM eclipse-temurin:$JAVA_VERSION-jdk-$UBUNTU_VERSION as builder

RUN --mount=type=cache,target=/var/cache/apt \
    apt-get update -qq \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y \
    build-essential \
    curl \
    debhelper \
    dh-exec \
    devscripts \
    erlang-dev \
    erlang-reltool \
    erlang-src \
    erlang \
    git \
    help2man \
    libcurl4-openssl-dev  \
    libicu-dev \
    libmozjs-78-dev \
    libssl-dev \
    lsb-release \
    npm \
    pkgconf \
    python3 \
    python3-venv \
    po-debconf \
    wget \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /couchdb

# Clone the couchdb-pkg repository and couchdb repository
# The couchdb-pkg repository is used to build the debian packages.
RUN git clone --single-branch --branch main https://github.com/apache/couchdb-pkg.git couchdb-pkg \
    && cd couchdb-pkg  \
    && git reset --hard 8d2a0979070f251d2e361859de195880b8942397  \
    && cd .. \
    && git clone --single-branch --branch main --depth 1 https://github.com/apache/couchdb.git couchdb

# Configure couchdb with nouveau
WORKDIR /couchdb/couchdb

RUN ./configure --enable-nouveau --spidermonkey-version 78

# Build the couchdb debian package
WORKDIR /couchdb/couchdb-pkg

# Build couchdb and cache
RUN make build-couch

# Build Debian packages
RUN make debian-bookworm

## Final image with JRE
FROM eclipse-temurin:$JAVA_VERSION-jre-$UBUNTU_VERSION

WORKDIR /app

# Copy the couchdb debian packages from the builder
COPY --from=builder /couchdb/couchdb/couchdb_*.deb /app/

# Add CouchDB user account to make sure the IDs are assigned consistently
RUN groupadd -g 5984 -r couchdb && useradd -u 5984 -d /opt/couchdb -g couchdb couchdb

# Install the couchdb debian packages
# https://github.com/apache/couchdb-pkg/blob/main/debian/README.Debian
RUN --mount=type=cache,target=/var/cache/apt \
    echo "couchdb couchdb/mode select none" | debconf-set-selections \
    && apt-get update -qq \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --allow-downgrades  \
       --allow-remove-essential --allow-change-held-packages  \
       /app/couchdb_*.deb \
       supervisor \
    || DEBIAN_FRONTEND=noninteractive apt-get install -f \
    && rm -rf /app/* \
    && rm -rf /var/lib/apt/lists/*

# Undo symlinks to /var/log and /var/lib
RUN \
    rmdir /var/lib/couchdb /var/log/couchdb; \
    rm /opt/couchdb/data /opt/couchdb/var/log; \
    mkdir -p /opt/couchdb/data /opt/couchdb/var/log; \
    chown couchdb:couchdb /opt/couchdb/data /opt/couchdb/var/log; \
    chmod 777 /opt/couchdb/data /opt/couchdb/var/log; \
# Remove file that sets logging to a file
    rm /opt/couchdb/etc/default.d/10-filelog.ini; \
# Check we own everything in /opt/couchdb. Matches the command in dockerfile_entrypoint.sh
    find /opt/couchdb \! \( -user couchdb -group couchdb \) -exec chown -f couchdb:couchdb '{}' +; \
# Setup directories and permissions for config. Technically these could be 555 and 444 respectively
# but we keep them as 755 and 644 for consistency with CouchDB defaults and the dockerfile_entrypoint.sh.
    find /opt/couchdb/etc -type d ! -perm 0755 -exec chmod -f 0755 '{}' +; \
    find /opt/couchdb/etc -type f ! -perm 0644 -exec chmod -f 0644 '{}' +; \
# only local.d needs to be writable for the docker_entrypoint.sh
    chmod -f 0777 /opt/couchdb/etc/local.d; \
# Make nouveau root directory as absolute path
    sed -i "s,rootDir:.*,rootDir: /opt/couchdb/data/nouveau,g" /opt/couchdb/etc/nouveau.yaml

# Add configuration
COPY --chown=couchdb:couchdb 10-docker-default.ini /opt/couchdb/etc/default.d/
COPY --chown=couchdb:couchdb vm.args /opt/couchdb/etc/
COPY docker-entrypoint.sh /usr/local/bin
COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf

RUN chmod +x /usr/local/bin/docker-entrypoint.sh

ENTRYPOINT ["/usr/bin/supervisord"]

VOLUME /opt/couchdb/data

# 5984: Main CouchDB endpoint
# 4369: Erlang portmap daemon (epmd)
# 9100: CouchDB cluster communication port
EXPOSE 5984 4369 9100

HEALTHCHECK --interval=10s --timeout=5s --retries=5 CMD curl --fail -s http://localhost:5984/_up

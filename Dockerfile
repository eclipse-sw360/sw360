# syntax=docker/dockerfile:1.4
#
# Copyright Helio Chisisni de Castro, 2023. Part of the SW360 Portal Project.
# Copyright Siemens AG, 2020. Part of the SW360 Portal Project.
# Copyright BMW CarIT GmbH, 2021.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0

#-----------------------------------------------------------------------------------
# Base image
# We need use JDK, JRE is not enough as Liferay do runtime changes and require javac
ARG JAVA_VERSION=11
ARG UBUNTU_VERSION=jammy

# Use OpenJDK Eclipe Temurin Ubuntu LTS
FROM eclipse-temurin:$JAVA_VERSION-jdk-$UBUNTU_VERSION as base

ENV LANG=en_US.UTF-8
ENV LANGUAGE=en_US:en
ENV LC_ALL=en_US.UTF-8

# Set versions as arguments
ARG LIFERAY_VERSION
ARG LIFERAY_SOURCE

ENV LIFERAY_HOME=/app/sw360
ENV LIFERAY_INSTALL=/app/sw360

ARG USERNAME=sw360
ARG USER_ID=1000
ARG USER_GID=$USER_ID
ARG HOMEDIR=/workspace
ENV HOME=$HOMEDIR

# Base system
RUN --mount=type=cache,target=/var/cache/apt \
    apt-get update -qq \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
    ca-certificates \
    curl \
    dos2unix \
    gnupg2 \
    iproute2 \
    iputils-ping \
    less \
    libarchive-tools \
    locales \
    lsof \
    netbase \
    openssl \
    procps \
    tzdata \
    sudo \
    unzip \
    zip \
    && rm -rf /var/lib/apt/lists/*

# Prepare system for non-priv user
RUN groupadd --gid $USER_GID $USERNAME \
    && useradd \
    --uid $USER_ID \
    --gid $USER_GID \
    --shell /bin/bash \
    --home-dir $HOMEDIR \
    --create-home $USERNAME

# sudo support
RUN echo "$USERNAME ALL=(root) NOPASSWD:ALL" > /etc/sudoers.d/$USERNAME \
    && chmod 0440 /etc/sudoers.d/$USERNAME

# Unpack liferay as sw360 and link current tomcat version
# to tomcat to make future proof updates
# Patch common-compress due vulnerability
RUN --mount=type=cache,target=/var/cache/deps \
    mkdir -p /app/sw360 \
    && if [ ! -f /var/cache/deps/"$LIFERAY_SOURCE" ]; then \
    curl -o /var/cache/deps/"$LIFERAY_SOURCE" -JL https://github.com/liferay/liferay-portal/releases/download/"$LIFERAY_VERSION"/"$LIFERAY_SOURCE"; \
    fi \
    && tar -xzf /var/cache/deps/"$LIFERAY_SOURCE" -C /app/sw360 --strip-components=1 \
    && curl -o /app/sw360/tomcat-9.0.56/webapps/ROOT/WEB-INF/shielded-container-lib/commons-compress.jar -JL https://repo1.maven.org/maven2/org/apache/commons/commons-compress/1.26.1/commons-compress-1.26.1.jar \
    && chown -R $USERNAME:$USERNAME /app \
    && ln -s /app/sw360/tomcat-* /app/sw360/tomcat

WORKDIR /app/sw360
ENTRYPOINT [ "/bin/bash" ]

#--------------------------------------------------------------------------------------------------
# Thrift
FROM ubuntu:jammy AS sw360thriftbuild

ARG BASEDIR="/build"
ARG THRIFT_VERSION

RUN --mount=type=cache,target=/var/cache/apt \
    apt-get -qq update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
    bison \
    build-essential \
    ca-certificates \
    cmake \
    curl \
    flex \
    libevent-dev \
    libtool \
    pkg-config \
    && rm -rf /var/lib/apt/lists/*

COPY ./scripts/install-thrift.sh build_thrift.sh

RUN --mount=type=tmpfs,target=/build \
    --mount=type=cache,target=/var/cache/deps \
    ./build_thrift.sh

FROM scratch AS thrift
COPY --from=sw360thriftbuild /usr/local/bin/thrift /usr/local/bin/thrift

#--------------------------------------------------------------------------------------------------
# SW360 Build Test image
# Base image to build with test

FROM maven:3-eclipse-temurin-11 as sw360test

COPY --from=thrift /usr/local/bin/thrift /usr/bin

# Thanks to Liferay, we need fix the java version
ENV _JAVA_OPTIONS='-Djdk.util.zip.disableZip64ExtraFieldValidation=true'

SHELL ["/bin/bash", "-c"]

# Install mkdocs to generate documentation
RUN --mount=type=cache,target=/var/cache/apt \
    apt-get update -qq \
    && DEBIAN_FRONTEND=noninteractive apt-get install -qq -y --no-install-recommends \
    gettext-base \
    git \
    python3-pip \
    python3-wheel \
    zip \
    unzip \
    && rm -rf /var/lib/apt/lists/* \
    && pip install mkdocs-material

#--------------------------------------------------------------------------------------------------
# SW360
# We build sw360 and create real image after everything is ready
# So when decide to use as development, only this last stage
# is triggered by buildkit images

FROM maven:3.9-eclipse-temurin-11 as sw360build

ARG COUCHDB_HOST=localhost

# Thanks to Liferay, we need fix the java version
ENV _JAVA_OPTIONS='-Djdk.util.zip.disableZip64ExtraFieldValidation=true'

WORKDIR /build

SHELL ["/bin/bash", "-c"]

# Install mkdocs to generate documentation
RUN --mount=type=cache,target=/var/cache/apt \
    apt-get update -qq \
    && DEBIAN_FRONTEND=noninteractive apt-get install -qq -y --no-install-recommends \
    gettext-base \
    git \
    python3-pip \
    python3-wheel \
    zip \
    unzip \
    && rm -rf /var/lib/apt/lists/* \
    && pip install mkdocs-material

# Prepare maven from binary to avoid wrong java dependencies and proxy
COPY scripts/docker-config/mvn-proxy-settings.xml /etc
COPY scripts/docker-config/set_proxy.sh /usr/local/bin/setup_maven_proxy
RUN chmod a+x /usr/local/bin/setup_maven_proxy \
    && setup_maven_proxy

COPY --from=thrift /usr/local/bin/thrift /usr/bin

RUN --mount=type=bind,target=/build/sw360,rw \
    --mount=type=cache,target=/root/.m2 \
    --mount=type=secret,id=sw360 \
    cd /build/sw360 \
    && set -a \
    && source /run/secrets/sw360 \
    && envsubst < scripts/docker-config/couchdb.properties.template | tee scripts/docker-config/etc_sw360/couchdb.properties \
    && envsubst < scripts/docker-config/couchdb-lucene.ini | tee third-party/couchdb-lucene/src/main/resources/couchdb-lucene.ini \
    && set +a \
    && cp scripts/docker-config/etc_sw360/couchdb.properties build-configuration/resources/ \
    && cp -a scripts/docker-config/etc_sw360 /etc/sw360 \
    && mkdir /etc/sw360/manager \
    && envsubst < scripts/docker-config/manager/tomcat-users.xml | tee /etc/sw360/manager/tomcat-users.xml \
    && mvn clean package \
    -P deploy \
    -Dtest=org.eclipse.sw360.rest.resourceserver.restdocs.* \
    -Dsurefire.failIfNoSpecifiedTests=false \
    -Dbase.deploy.dir=. \
    -Djars.deploy.dir=/sw360_deploy \
    -Dliferay.deploy.dir=/sw360_deploy \
    -Dbackend.deploy.dir=/sw360_tomcat_webapps \
    -Drest.deploy.dir=/sw360_tomcat_webapps \
    -Dhelp-docs=true

# Generate slim war files
WORKDIR /sw360_tomcat_webapps/

COPY scripts/create-slim-war-files.sh /bin/slim.sh

RUN bash /bin/slim.sh

FROM scratch AS binaries
COPY --from=sw360build /etc/sw360 /etc/sw360
COPY --from=sw360build /sw360_deploy /sw360_deploy
COPY --from=sw360build /sw360_tomcat_webapps /sw360_tomcat_webapps

#--------------------------------------------------------------------------------------------------
# Runtime image
FROM base AS sw360

ARG DEBUG
ARG USERNAME=sw360

WORKDIR /app/

# Make sw360 dir owned byt the user
RUN chown -R $USERNAME:$USERNAME /app/sw360

USER $USERNAME

# Modified etc
COPY --chown=$USERNAME:$USERNAME --from=binaries /etc/sw360 /etc/sw360
# Downloaded jar dependencies
COPY --chown=$USERNAME:$USERNAME --from=binaries /sw360_deploy/* /app/sw360/deploy
# Streamlined wars
COPY --chown=$USERNAME:$USERNAME --from=binaries /sw360_tomcat_webapps/slim-wars/*.war /app/sw360/tomcat/webapps/
# org.eclipse.sw360 jar artifacts
COPY --chown=$USERNAME:$USERNAME --from=binaries /sw360_tomcat_webapps/*.jar /app/sw360/tomcat/webapps/
# Shared streamlined jar libs
COPY --chown=$USERNAME:$USERNAME --from=binaries /sw360_tomcat_webapps/libs/*.jar /app/sw360/tomcat/shared/

# Make catalina understand shared directory
RUN dos2unix /app/sw360/tomcat/conf/catalina.properties \
    && sed -i "s,shared.loader=,shared.loader=/app/sw360/tomcat/shared/*.jar,g" /app/sw360/tomcat/conf/catalina.properties

# Copy liferay/sw360 config files
COPY --chown=$USERNAME:$USERNAME ./scripts/docker-config/portal-ext.properties /app/sw360/portal-ext.properties
COPY --chown=$USERNAME:$USERNAME ./scripts/docker-config/entry_point.sh /app/entry_point.sh

# Tomcat manager for debugging portlets
COPY --chown=$USERNAME:$USERNAME --from=tomcat:9.0.56-jdk11 /usr/local/tomcat/webapps.dist/manager /app/sw360/tomcat/webapps/manager
RUN --mount=type=bind,target=/build/sw360,rw \
    if [  DEBUG ]; then \
    cp /etc/sw360/manager/tomcat-users.xml /app/sw360/tomcat/conf/tomcat-users.xml ; \
    cp /build/sw360/scripts/docker-config/manager/context.xml /app/sw360/tomcat/webapps/manager/META-INF/context.xml ; \
    else \
    mv /app/sw360/tomcat/webapps/manager /app/sw360/tomcat/webapps/manager.disabled ; \
    fi

STOPSIGNAL SIGINT

WORKDIR /app/sw360

ENTRYPOINT [ "/app/entry_point.sh" ]


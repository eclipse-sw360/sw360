#
# Copyright Siemens AG, 2020. Part of the SW360 Portal Project.
# Copyright BMW CarIT GmbH, 2021.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#

#-----------------------------------------------------------------------------------
# Base image
# We need use JDK, JRE is not enough as Liferay do runtime changes and require javac
FROM eclipse-temurin:11-jdk-jammy as sw360base

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
RUN --mount=type=cache,mode=0755,target=/var/cache/apt,sharing=locked \
    --mount=type=cache,mode=0755,target=/var/lib/apt,sharing=locked \
    apt-get update -qq \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
    ca-certificates \
    curl \
    dos2unix \
    gnupg2 \
    iproute2 \
    iputils-ping \
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
RUN mkdir -p /app/sw360 \
    && curl -JL https://github.com/liferay/liferay-portal/releases/download/"$LIFERAY_VERSION"/"$LIFERAY_SOURCE" | tar -xz -C /app/sw360 --strip-components=1 \
    && chown -R $USERNAME:$USERNAME /app \
    && ln -s /app/sw360/tomcat-* /app/sw360/tomcat

#-----------------------------------------------------------------------------------
# Builder image
FROM eclipse-temurin:11-jdk-jammy AS sw360builder

# Set versions as arguments
ARG MAVEN_VERSION

# Prepare maven from binary to avoid wrong java dependencies and proxy
COPY scripts/docker-config/mvn-proxy-settings.xml /etc
COPY scripts/docker-config/set_proxy.sh /usr/local/bin/setup_maven_proxy
RUN curl -JL https://dlcdn.apache.org/maven/maven-3/"$MAVEN_VERSION"/binaries/apache-maven-"$MAVEN_VERSION"-bin.tar.gz | tar -xz --strip-components=1 -C /usr/local
RUN chmod a+x /usr/local/bin/setup_maven_proxy \
    && setup_maven_proxy

RUN --mount=type=cache,mode=0755,target=/var/cache/apt,sharing=locked \
    --mount=type=cache,mode=0755,target=/var/lib/apt,sharing=locked \
    apt-get -qq update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
    gettext-base

#--------------------------------------------------------------------------------------------------
# Thrift
FROM ubuntu:jammy AS sw360thrift

ARG BASEDIR="/build"
ARG THRIFT_VERSION=0.16.0

RUN --mount=type=cache,mode=0755,target=/var/cache/apt,sharing=locked \
    --mount=type=cache,mode=0755,target=/var/lib/apt,sharing=locked \
    apt-get -qq update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
    bison \
    build-essential \
    cmake \
    curl \
    flex \
    libevent-dev \
    libtool \
    pkg-config \
    && rm -rf /var/lib/apt/lists/*

COPY ./scripts/install-thrift.sh build_thrift.sh

RUN --mount=type=tmpfs,target=/build \
    ./build_thrift.sh

#--------------------------------------------------------------------------------------------------
# Couchdb-Lucene
FROM sw360builder as sw360clucene

ARG CLUCENE_VERSION=2.1.0

WORKDIR /build

RUN --mount=type=cache,mode=0755,target=/var/cache/apt,sharing=locked \
    --mount=type=cache,mode=0755,target=/var/lib/apt,sharing=locked \
    apt-get -qq update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
    patch

# Prepare source code
COPY ./scripts/docker-config/couchdb-lucene.ini /var/tmp/couchdb-lucene.ini
COPY ./scripts/patches/couchdb-lucene.patch /var/tmp/couchdb-lucene.patch

# Build CLucene
RUN --mount=type=tmpfs,target=/build \
    --mount=type=cache,mode=0755,target=/root/.m2,rw,sharing=locked \
    curl -JL https://github.com/rnewson/couchdb-lucene/archive/v"$CLUCENE_VERSION".tar.gz | tar -C /build -xz --strip-components=1 \
    && patch -p1 < /var/tmp/couchdb-lucene.patch \
    && cp /var/tmp/couchdb-lucene.ini src/main/resources/couchdb-lucene.ini \
    && mvn -X install war:war \
    && cp ./target/*.war /couchdb-lucene.war

#--------------------------------------------------------------------------------------------------
# SW360
# We build sw360 and create real image after everything is ready
# So when decide to use as development, only this last stage
# is triggered by buildkit images

FROM sw360builder AS sw360build

# Install mkdocs to generate documentation
RUN --mount=type=cache,mode=0755,target=/var/cache/apt,sharing=locked \
    --mount=type=cache,mode=0755,target=/var/lib/apt,sharing=locked \
    apt-get update -qq \
    && DEBIAN_FRONTEND=noninteractive apt-get install -qq -y --no-install-recommends \
    git \
    python3-pip \
    python3-wheel \
    zip \
    unzip \
    && rm -rf /var/lib/apt/lists/* \
    && pip install mkdocs-material

COPY --from=sw360thrift /usr/local/bin/thrift /usr/bin

RUN --mount=type=bind,target=/build/sw360,rw \
    --mount=type=cache,mode=0755,target=/root/.m2,rw,sharing=locked \
    cd /build/sw360 \
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
COPY --from=sw360clucene /couchdb-lucene.war /sw360_tomcat_webapps
RUN bash /bin/slim.sh

#--------------------------------------------------------------------------------------------------
# Runtime image
FROM ghcr.io/eclipse/sw360base:latest as sw360

WORKDIR /app/

USER $USERNAME

# Downloaded jar dependencies
COPY --chown=$USERNAME:$USERNAME --from=sw360build /sw360_deploy/* /app/sw360/deploy
# Streamlined wars
COPY --chown=$USERNAME:$USERNAME --from=sw360build /sw360_tomcat_webapps/slim-wars/*.war /app/sw360/tomcat/webapps/
# org.eclipse.sw360 jar artifacts
COPY --chown=$USERNAME:$USERNAME --from=sw360build /sw360_tomcat_webapps/*.jar /app/sw360/tomcat/webapps/
# Shared streamlined jar libs
COPY --chown=$USERNAME:$USERNAME --from=sw360build /sw360_tomcat_webapps/libs/*.jar /app/sw360/tomcat/shared/

# Make catalina understand shared directory
RUN dos2unix /app/sw360/tomcat/conf/catalina.properties \
    && sed -i "s,shared.loader=,shared.loader=/app/sw360/tomcat/shared/*.jar,g" /app/sw360/tomcat/conf/catalina.properties

# Copy liferay/sw360 config files
COPY --chown=$USERNAME:$USERNAME ./scripts/docker-config/portal-ext.properties /app/sw360/portal-ext.properties
COPY --chown=$USERNAME:$USERNAME ./scripts/docker-config/etc_sw360 /etc/sw360
COPY --chown=$USERNAME:$USERNAME ./scripts/docker-config/entry_point.sh /app/entry_point.sh

STOPSIGNAL SIGINT

WORKDIR /app/sw360

ENTRYPOINT [ "/app/entry_point.sh" ]

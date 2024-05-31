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

ARG TOMCAT_VERSION=10-jre17-temurin-jammy

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

FROM scratch AS localthrift
COPY --from=sw360thriftbuild /usr/local/bin/thrift /usr/local/bin/thrift

#--------------------------------------------------------------------------------------------------
# SW360 Build Test image

FROM maven:3-eclipse-temurin-17 as sw360test

COPY --from=localthrift /usr/local/bin/thrift /usr/bin

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

FROM maven:3-eclipse-temurin-17 as sw360build

ARG COUCHDB_HOST=localhost

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

COPY --from=localthrift /usr/local/bin/thrift /usr/bin

RUN --mount=type=bind,target=/build/sw360,rw \
    --mount=type=cache,target=/root/.m2 \
    --mount=type=secret,id=sw360 \
    cd /build/sw360 \
    && set -a \
    && source /run/secrets/sw360 \
    && envsubst < scripts/docker-config/couchdb.properties.template | tee scripts/docker-config/etc_sw360/couchdb.properties \
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
FROM tomcat:$TOMCAT_VERSION as sw360

ARG TOMCAT_DIR=/usr/local/tomcat

# Modified etc
COPY --from=binaries /etc/sw360 /etc/sw360
# Streamlined wars
COPY --from=binaries /sw360_tomcat_webapps/slim-wars/*.war ${TOMCAT_DIR}/webapps/
# org.eclipse.sw360 jar artifacts
COPY --from=binaries /sw360_tomcat_webapps/*.jar ${TOMCAT_DIR}/webapps/
# Shared streamlined jar libs
COPY --from=binaries /sw360_tomcat_webapps/libs/*.jar ${TOMCAT_DIR}/lib/

# Tomcat manager for debugging portlets
RUN --mount=type=bind,target=/build/sw360,rw \
    mv ${TOMCAT_DIR}/webapps.dist/manager ${TOMCAT_DIR}/webapps/manager \
    && cp /etc/sw360/manager/tomcat-users.xml ${TOMCAT_DIR}/conf/tomcat-users.xml \
    && cp /build/sw360/scripts/docker-config/manager/context.xml ${TOMCAT_DIR}/webapps/manager/META-INF/context.xml

WORKDIR ${TOMCAT_DIR}

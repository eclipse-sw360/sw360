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
FROM eclipse-temurin:11-jdk-jammy as baseimage

ENV LANG=en_US.UTF-8
ENV LANGUAGE=en_US:en
ENV LC_ALL=en_US.UTF-8

# Set versions as arguments
ARG CLUCENE_VERSION
ARG THRIFT_VERSION
ARG MAVEN_VERSION
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

#-----------------------------------------------------------------------------------
# Builder image
FROM baseimage AS builder

# Set versions as arguments
ARG CLUCENE_VERSION
ARG THRIFT_VERSION
ARG MAVEN_VERSION
ARG LIFERAY_VERSION
ARG LIFERAY_SOURCE

RUN --mount=type=cache,mode=0755,target=/var/cache/apt,sharing=locked \
    --mount=type=cache,mode=0755,target=/var/lib/apt,sharing=locked \
    apt-get -qq update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
    bison \
    build-essential \
    cmake \
    curl \
    flex \
    gettext-base \
    git \
    libevent-dev \
    libtool \
    pkg-config \
    wget \
    && rm -rf /var/lib/apt/lists/*

# Prepare maven from binary to avoid wrong java dependencies and proxy
COPY scripts/docker-config/mvn-proxy-settings.xml /etc
COPY scripts/docker-config/set_proxy.sh /usr/local/bin/setup_maven_proxy
RUN --mount=type=bind,source=deps,target=/var/cache/deps,readonly \
    tar -xzf "/var/cache/deps/apache-maven-$MAVEN_VERSION-bin.tar.gz" --strip-components=1 -C /usr/local
RUN chmod a+x /usr/local/bin/setup_maven_proxy

#--------------------------------------------------------------------------------------------------
# Thrift
FROM builder AS thriftbuild

ARG BASEDIR="/build"
ARG THRIFT_VERSION=0.16.0

COPY ./scripts/install-thrift.sh build_thrift.sh

RUN --mount=type=bind,source=deps,target=/var/cache/deps,readonly \
    --mount=type=tmpfs,target=/build \
    ./build_thrift.sh

#--------------------------------------------------------------------------------------------------
# Couchdb-Lucene
FROM builder as clucenebuild

ARG CLUCENE_VERSION=2.1.0

WORKDIR /build

# Prepare source code
COPY ./scripts/docker-config/couchdb-lucene.ini /var/tmp/couchdb-lucene.ini
COPY ./scripts/patches/couchdb-lucene.patch /var/tmp/couchdb-lucene.patch

# Build CLucene
RUN --mount=type=bind,source=deps,target=/var/cache/deps,readonly \
    --mount=type=tmpfs,target=/build \
    --mount=type=cache,mode=0755,target=/root/.m2,rw,sharing=locked \
    tar -C /build -xf /var/cache/deps/couchdb-lucene-$CLUCENE_VERSION.tar.gz --strip-components=1 \
    && patch -p1 < /var/tmp/couchdb-lucene.patch \
    && cp /var/tmp/couchdb-lucene.ini src/main/resources/couchdb-lucene.ini \
    && setup_maven_proxy \
    && mvn dependency:go-offline -B \
    && mvn install war:war \
    && cp ./target/*.war /couchdb-lucene.war

#--------------------------------------------------------------------------------------------------
# SW360
# We build sw360 and create real image after everything is ready
# So when decide to use as development, only this last stage
# is triggered by buildkit images

FROM thriftbuild AS sw360build

# Install mkdocs to generate documentation
RUN --mount=type=cache,mode=0755,target=/var/cache/apt,sharing=locked \
    --mount=type=cache,mode=0755,target=/var/lib/apt,sharing=locked \
    apt-get update -qq \
    && DEBIAN_FRONTEND=noninteractive apt-get install -qq -y --no-install-recommends \
    python3-pip \
    python3-wheel \
    && rm -rf /var/lib/apt/lists/* \
    && pip install mkdocs-material

RUN --mount=type=bind,target=/build/sw360,rw \
    --mount=type=cache,mode=0755,target=/root/.m2,rw,sharing=locked \
    cd /build/sw360 \
    && setup_maven_proxy \
    && mvn clean package \
    -P deploy \
    -Dtest=org.eclipse.sw360.rest.resourceserver.restdocs.* \
    -Dsurefire.failIfNoSpecifiedTests=false \
    -Dbase.deploy.dir=. \
    -Djars.deploy.dir=/sw360_deploy \
    -Dliferay.deploy.dir=/sw360_tomcat_webapps \
    -Dbackend.deploy.dir=/sw360_tomcat_webapps \
    -Drest.deploy.dir=/sw360_tomcat_webapps \
    -Dhelp-docs=true

# # Generate slim war files
WORKDIR /sw360_tomcat_webapps/

COPY scripts/create-slim-war-files.sh /bin/slim.sh
COPY --from=clucenebuild /couchdb-lucene.war /sw360_tomcat_webapps
RUN bash /bin/slim.sh

#--------------------------------------------------------------------------------------------------
# Runtime image
FROM baseimage as sw360

WORKDIR /app/

ARG LIFERAY_SOURCE

# Unpack liferay as sw360 and link current tomcat version
# to tomcat to make future proof updates
RUN --mount=type=bind,source=deps,target=/var/cache/deps,readonly \
    mkdir sw360 \
    && tar xzf /var/cache/deps/$LIFERAY_SOURCE -C $USERNAME --strip-components=1 \
    && chown -R $USERNAME:$USERNAME sw360 \
    && ln -s /app/sw360/tomcat-* /app/sw360/tomcat

USER $USERNAME

COPY --chown=$USERNAME:$USERNAME --from=sw360build /sw360_deploy/* /app/sw360/deploy
COPY --chown=$USERNAME:$USERNAME --from=sw360build /sw360_tomcat_webapps/slim-wars/*.war /app/sw360/tomcat/webapps/
COPY --chown=$USERNAME:$USERNAME --from=sw360build /sw360_tomcat_webapps/libs/*.jar /app/sw360/tomcat/shared/

# Copy dependencies to deploy
RUN --mount=type=bind,source=deps,target=/var/cache/deps,readonly \
    cp /var/cache/deps/jars/* /app/sw360/deploy

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


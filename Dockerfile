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

FROM maven:3.8-eclipse-temurin-11 AS builder

RUN --mount=type=cache,target=/var/cache/apt,sharing=locked apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
    automake \
    bison \
    build-essential \
    curl \
    flex \
    gettext-base \
    git \
    libboost-dev \
    libboost-test-dev \
    libboost-program-options-dev \
    libevent-dev \
    libtool \
    libssl-dev \
    pkg-config \
    wget \
    && rm -rf /var/lib/apt/lists/*

# Configure proxy script
COPY .tmp/mvn-proxy-settings.xml /root/.m2/settings.xml

#-------------------------------------
# Thrift
FROM builder AS thriftbuild

ARG THRIFT_VERSION=0.14.0

COPY deps/thrift-*.tar.gz /deps/
COPY ./scripts/docker-config/install_scripts/build_thrift.sh build_thrift.sh

RUN --mount=type=tmpfs,target=/build \
    tar -xzf "deps/thrift-$THRIFT_VERSION.tar.gz" --strip-components=1 -C /build \
    && ./build_thrift.sh \
    && rm -rf /deps

#-------------------------------------
# Couchdb-Lucene
FROM builder as clucenebuild

ARG CLUCENE_VERSION=2.1.0

WORKDIR /build

COPY deps/couchdb* /deps/

# Prepare source code
RUN --mount=type=tmpfs,target=/build \
    --mount=type=cache,target=/root/.m2,rw,sharing=locked \
    tar -C /build -xvf /deps/couchdb-lucene-$CLUCENE_VERSION.tar.gz --strip-components=1 \
    && cd /build \
    && patch -p1 < /deps/couchdb-lucene.patch \
    && sed -i "s/allowLeadingWildcard=false/allowLeadingWildcard=true/" src/main/resources/couchdb-lucene.ini \
    && sed -i "s/localhost:5984/$COUCHDB_USER:$COUCHDB_PASSWORD@couchdb:5984/" ./src/main/resources/couchdb-lucene.ini \
    && mvn dependency:go-offline \
    && mvn install war:war \
    && cp ./target/*.war /couchdb-lucene.war \
    && rm -rf /deps

#-------------------------------------
# Main base container
# We need use JDK, JRE is not enough as Liferay do runtime changes and require javac
FROM eclipse-temurin:11-jdk as imagebase

WORKDIR /app/

ARG LIFERAY_SOURCE="liferay-ce-portal-tomcat-7.3.4-ga5-20200811154319029.tar.gz"

RUN --mount=type=cache,target=/var/cache/apt,sharing=locked apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        tzdata \
        gnupg2 \
        && rm -rf /var/lib/apt/lists/*

COPY deps/jars/* /deps/jars/
COPY deps/liferay-ce* /deps/

COPY --from=thriftbuild /thrift-bin.tar.gz .
RUN tar xzf thrift-bin.tar.gz -C / \
    && rm thrift-bin.tar.gz

# Unpack liferay as sw360 and link current tomcat version
# to tomcat to make future proof updates
RUN mkdir sw360 \
    && tar xzf /deps/$LIFERAY_SOURCE -C sw360 --strip-components=1 \
    && cp /deps/jars/* sw360/deploy \ 
    && ln -s /app/sw360/tomcat-* /app/sw360/tomcat \
    && rm -rf /deps

COPY --from=clucenebuild /couchdb-lucene.war /app/sw360/tomcat/webapps/

#-------------------------------------
# SW360
# We build sw360 and create real image after everything is ready
# So when decide to use as development, only this last stage
# is triggered by buildkit images

FROM builder AS sw360build

COPY --from=thriftbuild /thrift-bin.tar.gz /deps/
COPY scripts/docker-config/mvn-proxy-settings.xml /tmp
RUN envsubst </tmp/mvn-proxy-settings.xml > root/.m2/settings.xml

RUN tar xzf /deps/thrift-bin.tar.gz -C /

RUN --mount=type=cache,target=/var/cache/apt,sharing=locked apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        mkdocs \
        python3-pip \
        python3-wheel \
        && rm -rf /var/lib/apt/lists/* \
        && pip install mkdocs-material


COPY deps/sw360.tar /deps/

RUN --mount=type=tmpfs,target=/build \
    --mount=type=cache,target=/root/.m2,rw,sharing=locked \
    tar -C /build -xf /deps/sw360.tar \
    && cd /build/sw360 \
    && mvn package \
    -P deploy -Dtest=org.eclipse.sw360.rest.resourceserver.restdocs.* \
    -DfailIfNoTests=false \
    -Dbase.deploy.dir=. \
    -Dliferay.deploy.dir=/sw360_deploy \
    -Dbackend.deploy.dir=/sw360_tomcat_webapps \
    -Drest.deploy.dir=/sw360_tomcat_webapps \
    -Dhelp-docs=true \
    && rm -rf /deps

# And the main final
FROM imagebase

COPY --from=sw360build /sw360_deploy/* /app/sw360/deploy
COPY --from=sw360build /sw360_tomcat_webapps/* /app/sw360/tomcat/webapps/

# We will copy the scripts on entrypoint to persists, otherwise volume will override it
COPY ./scripts/docker-config/etc_sw360 /etc_sw360
COPY ./scripts/docker-config/entry_point.sh .
COPY ./scripts/docker-config/setenv.sh /app/sw360/tomcat/bin
COPY ./scripts/docker-config/portal-ext.properties /app/sw360/portal-ext.properties

STOPSIGNAL SIGQUIT

ENTRYPOINT ./entry_point.sh


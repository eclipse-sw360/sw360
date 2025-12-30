
# Copyright Helio Chisisni de Castro, 2023. Part of the SW360 Portal Project.
# Copyright Siemens AG, 2020. Part of the SW360 Portal Project.
# Copyright BMW CarIT GmbH, 2021.
# Copyright Cariad SE, 2024. Part of the SW360 Portal Project.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0

#--------------------------------------------------------------------------------------------------
# SW360
# We build sw360 and create real image after everything is ready
# So when decide to use as development, only this last stage
# is triggered by buildkit images

# 3-eclipse-temurin-21-noble
FROM maven@sha256:89086b81ff2ec9c65739b1763ffb729b59b48c569fe13e5c81a54e128b6827a7 AS sw360build

ARG COUCHDB_HOST=localhost

WORKDIR /build

RUN rm -f /etc/apt/apt.conf.d/docker-clean
RUN apt-get update -qq \
    && DEBIAN_FRONTEND=noninteractive apt-get install -qq -y --no-install-recommends \
    gettext-base \
    git \
    unzip \
    zip

# Copy external built and Check if thrift is installed
COPY --from=ghcr.io/eclipse-sw360/thrift:0.20.0-noble /usr/local/bin/thrift /usr/bin
RUN /usr/bin/thrift --version


COPY . /build/sw360
WORKDIR /build/sw360

# Prepare maven from binary to avoid wrong java dependencies and proxy
COPY config/container/mvn-proxy-settings.xml /etc
COPY config/container/set_proxy.sh /usr/local/bin/setup_maven_proxy
RUN chmod a+x /usr/local/bin/setup_maven_proxy \
    && setup_maven_proxy

# Configure credentials
RUN --mount=type=secret,id=couchdb \
    set -a \
    && . /run/secrets/couchdb \
    && set +a \
    && cp -a config/container/etc_sw360 /etc/sw360 \
    && envsubst \
    '$COUCHDB_USER $COUCHDB_PASSWORD $COUCHDB_HOST' \
    < config/couchdb/couchdb.properties.template \
    > /etc/sw360/couchdb.properties \
    && mkdir /etc/sw360/manager \
    && envsubst \
    '$COUCHDB_USER $COUCHDB_PASSWORD $COUCHDB_HOST' \
    < config/container/manager/tomcat-users.xml \
    > /etc/sw360/manager/tomcat-users.xml

RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package \
    -P deploy \
    -Dbase.deploy.dir="${PWD}" \
    -Dtest=org.eclipse.sw360.rest.resourceserver.restdocs.* \
    -Dsurefire.failIfNoSpecifiedTests=false \
    -Djars.deploy.dir=/sw360_tomcat_webapps \
    -Dbackend.deploy.dir=/sw360_tomcat_webapps \
    -Drest.deploy.dir=/sw360_tomcat_webapps \
    -Dhelp-docs=true


# Generate slim war files
WORKDIR /sw360_tomcat_webapps/

COPY scripts/create-slim-war-files.sh /bin/slim.sh

RUN bash /bin/slim.sh

RUN find /sw360_tomcat_webapps

#--------------------------------------------------------------------------------------------------
# Runtime image

# 11-jre21-temurin-noble
FROM tomcat@sha256:ba0d8041e7c6d51bb8f82949ee77c1fdc8b01df8a8ff311e2f7c0e516105e139 AS sw360

ARG TOMCAT_DIR=/usr/local/tomcat

# Modified etc
COPY --from=sw360build /etc/sw360 /etc/sw360
# org.eclipse.sw360 jar artifacts
COPY --from=sw360build /sw360_tomcat_webapps/libs/*.jar ${TOMCAT_DIR}/lib/
# Streamlined wars
COPY --from=sw360build /sw360_tomcat_webapps/slim-wars/*.war ${TOMCAT_DIR}/webapps/

# Tomcat manager for debugging portlets
RUN mv ${TOMCAT_DIR}/webapps.dist/manager ${TOMCAT_DIR}/webapps/manager
COPY --from=sw360build /etc/sw360/manager/tomcat-users.xml ${TOMCAT_DIR}/conf/tomcat-users.xml
COPY --from=sw360build /build/sw360/config/container/manager/context.xml ${TOMCAT_DIR}/webapps/manager/META-INF/context.xml

WORKDIR ${TOMCAT_DIR}

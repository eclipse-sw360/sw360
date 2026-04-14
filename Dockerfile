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

# Where code compiles
FROM maven:3-eclipse-temurin-21-noble@sha256:08733049ae318e8af58235278ff2f5fdfc81958ec11e7bc34635b2e0537fcfad AS sw360build

ARG COUCHDB_HOST=localhost

WORKDIR /build

SHELL ["/bin/bash", "-c"]

RUN rm -f /etc/apt/apt.conf.d/docker-clean
RUN apt-get update -qq \
    && DEBIAN_FRONTEND=noninteractive apt-get install -qq -y --no-install-recommends \
    gettext-base \
    git \
    unzip \
    zip

# Prepare maven from binary to avoid wrong java dependencies and proxy
COPY scripts/docker-config/mvn-proxy-settings.xml /etc
COPY scripts/docker-config/set_proxy.sh /usr/local/bin/setup_maven_proxy
RUN chmod a+x /usr/local/bin/setup_maven_proxy \
    && setup_maven_proxy

COPY --from=ghcr.io/eclipse-sw360/thrift:0.20.0-noble /usr/local/bin/thrift /usr/bin

# Check if thrift is installed
RUN /usr/bin/thrift --version

WORKDIR /build/sw360

RUN --mount=type=bind,target=/build/sw360,rw \
    --mount=type=cache,target=/root/.m2 \
    mvn clean package \
    -P deploy \
    -Dbase.deploy.dir="${PWD}" \
    -Dtest=org.eclipse.sw360.rest.resourceserver.restdocs.* \
    -Dsurefire.failIfNoSpecifiedTests=false \
    -Dbackend.deploy.dir=/sw360_tomcat_webapps \
    -Dlistener.deploy.dir=/sw360_keycloak_listener \
    -Dhelp-docs=true

# Generate slim war files
WORKDIR /sw360_tomcat_webapps/

COPY scripts/create-slim-war-files.sh /bin/slim.sh

RUN bash /bin/slim.sh

FROM scratch AS binaries
COPY --from=sw360build /sw360_tomcat_webapps /sw360_tomcat_webapps
COPY --from=sw360build /sw360_keycloak_listener /sw360_keycloak_listener

#--------------------------------------------------------------------------------------------------
# Runtime SW360 image

FROM tomcat:11-jre21-temurin-noble@sha256:59cb924b1a76508eb7769f102299293d6abcd0e62d22b1b2ba18324090e3b38a AS sw360

# Default environment variables that can be overridden at runtime
# For more information, please check the documentation.
#
# CouchDB settings
ENV COUCHDB_URL="http://couchdb:5984"
ENV COUCHDB_LUCENESEARCH_LIMIT="1000"
ENV CLOUDANT_ENABLE_RETRIES="true"
#
# Spring controllers
ENV ENABLE_DISKSPACE="false"
ENV JWKS_ISSUER_URI="http://localhost:8080/authorization/oauth2/jwks"
ENV JWKS_SET_URI="http://localhost:8080/authorization/oauth2/jwks"
ENV JWKS_ISSUER="http://localhost:8090"
#
# Email configs
ENV EMAIL_PROPERTIES_HOST=""
ENV EMAIL_PROPERTIES_PORT=""
ENV EMAIL_PROPERTIES_STARTTLS="false"
ENV EMAIL_PROPERTIES_ENABLE_SSL="false"
ENV EMAIL_PROPERTIES_AUTH_REQUIRED="false"
ENV EMAIL_PROPERTIES_FROM="__No_Reply__@sw360.org"
ENV EMAIL_PROPERTIES_SUPPORT_EMAIL="help@sw360.org"
ENV EMAIL_PROPERTIES_TLS_PROTOCOL="TLSv1.2"
ENV EMAIL_PROPERTIES_TLS_TRUST="*"
ENV EMAIL_PROPERTIES_DEBUG="false"
#
# SVM Configs
ENV SVM_API_BASE_PATH="https://svm.example.org"
ENV SVM_API_ROOT_PATH="api/v1"
ENV SVM_SW360_API_URL="https://svm.example.org/application.json"
ENV SVM_SW360_CERTIFICATE_FILENAME="not-configured.pfx"
#
# Other settings
ENV SCHEDULER_AUTOSTART_SERVICES="cvesearchService"
ENV SW360_CORS_ALLOWED_ORIGIN="*"
ENV SW360_THRIFT_SERVER_URL="http://localhost:8080"
ENV SW360_BASE_URL="http://localhost:8080"

# Install dependencies for entrypoint
RUN apt-get update -qq \
    && DEBIAN_FRONTEND=noninteractive apt-get install -qq -y --no-install-recommends \
    gettext-base \
    && rm -rf /var/lib/apt/lists/*

# Streamlined wars
COPY --from=binaries /sw360_tomcat_webapps/slim-wars/*.war ${CATALINA_HOME}/webapps/
# org.eclipse.sw360 jar artifacts
COPY --from=binaries /sw360_tomcat_webapps/*.jar ${CATALINA_HOME}/webapps/
# Shared streamlined jar libs
COPY --from=binaries /sw360_tomcat_webapps/libs/*.jar ${CATALINA_HOME}/lib/

WORKDIR /app/sw360

# Copy the configuration files
COPY ./scripts/docker-config .

# Tomcat manager for debugging portlets
# Make entrypoint executable
RUN mv ${CATALINA_HOME}/webapps.dist/manager ${CATALINA_HOME}/webapps/manager \
    && mv ./manager/context.xml ${CATALINA_HOME}/webapps/manager/META-INF/context.xml \
    && chmod a+x ./docker-entrypoint.sh

EXPOSE 8080

ENTRYPOINT ["/app/sw360/docker-entrypoint.sh"]

#--------------------------------------------------------------------------------------------------
# Build custom Keycloak with SW360 providers
# For guide, see https://www.keycloak.org/server/containers

FROM quay.io/keycloak/keycloak:26.5.7@sha256:45ae20191531eb608ddb0b775d012b40d3e4f942697f3214694887dd7c108d13 AS keycloak-build

# Enable health and metrics support
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true

# Configure a database vendor
ENV KC_DB=postgres

WORKDIR /opt/keycloak

# Copy always does root:root with 644. Thus cp within container to get
# keycloak:root with 644
COPY --from=binaries /sw360_keycloak_listener /tmp/providers/

RUN cp /tmp/providers/*jar /opt/keycloak/providers/ \
 && /opt/keycloak/bin/kc.sh build

# Copy the optimized KC
FROM quay.io/keycloak/keycloak:26.5.7@sha256:45ae20191531eb608ddb0b775d012b40d3e4f942697f3214694887dd7c108d13 AS keycloak

# Default environment variables that can be overridden at runtime
# For more information, please check the documentation.
#
# CouchDB settings
ENV COUCHDB_URL="http://couchdb:5984"
ENV COUCHDB_USER="admin"
ENV COUCHDB_LUCENESEARCH_LIMIT="1000"
ENV CLOUDANT_ENABLE_RETRIES="true"

# Create the /etc/sw360
USER root

RUN mkdir -p /etc/sw360 \
 && chown -R keycloak:keycloak /etc/sw360

USER keycloak

# Copy the configs required in /etc/sw360
WORKDIR /app/docker-config

# Copy the configs and entrypoint
COPY --chown=keycloak ./scripts/docker-config/docker-entrypoint-keycloak.sh .

# Make entrypoint executable
RUN chmod a+x ./docker-entrypoint-keycloak.sh

# Copy the optimized KC
COPY --from=keycloak-build /opt/keycloak/ /opt/keycloak/

ENTRYPOINT ["/app/docker-config/docker-entrypoint-keycloak.sh"]

CMD ["start", "--optimized"]

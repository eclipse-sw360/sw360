#
# Copyright Siemens AG, 2020. Part of the SW360 Portal Project.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#

FROM maven:3.6.3-openjdk-11-slim as builder

WORKDIR /app/build/sw360

COPY . .

RUN ./scripts/install-thrift.sh

RUN DEBIAN_FRONTEND=noninteractive apt-get install git -y --no-install-recommends \
 && DEBIAN_FRONTEND=noninteractive apt-get install wget -y --no-install-recommends

RUN mvn -s /app/build/sw360/scripts/docker-config/mvn-proxy-settings.xml clean package -P deploy -Dtest=org.eclipse.sw360.rest.resourceserver.restdocs.* -DfailIfNoTests=false -Dbase.deploy.dir=. -Dliferay.deploy.dir=/app/build/sw360/deployables/deploy -Dbackend.deploy.dir=/app/build/sw360/deployables/webapps -Drest.deploy.dir=/app/build/sw360/deployables/webapps

RUN ./scripts/docker-config/install_scripts/build_couchdb_lucene.sh

RUN ./scripts/docker-config/install_scripts/download_liferay_and_dependencies.sh


FROM ubuntu:18.04

WORKDIR /app/

USER root

COPY ./scripts/install-thrift.sh .

COPY --from=builder /app/build/sw360/liferay-ce-portal-7.3.3-ga4 /app/liferay-ce-portal-7.3.3-ga4

COPY --from=builder /app/build/sw360/deployables/webapps /app/liferay-ce-portal-7.3.3-ga4/tomcat-9.0.33/webapps

COPY --from=builder /app/build/sw360/deployables/deploy /app/liferay-ce-portal-7.3.3-ga4/deploy

COPY ./scripts/docker-config/portal-ext.properties /app/liferay-ce-portal-7.3.3-ga4

COPY ./scripts/docker-config/etc_sw360 /etc/sw360/

COPY ./scripts/docker-config/install_scripts .

COPY ./scripts/docker-config/setenv.sh /app/liferay-ce-portal-7.3.3-ga4/tomcat-9.0.33/bin

RUN ./install-thrift.sh

RUN ./install_init_postgres_script.sh

RUN ./install_configure_couchdb.sh

RUN DEBIAN_FRONTEND=noninteractive apt-get install openjdk-11-jdk -y --no-install-recommends

ENTRYPOINT ./entry_point.sh && bash

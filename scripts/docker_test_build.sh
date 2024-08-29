#!/bin/bash

# -----------------------------------------------------------------------------
#
# Copyright Helio Chissini de Castro, 2024. Part of the SW360 Portal Project.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# script for setting debugging options while deploying SW360
#
# author: heliocastro@gmail.com
# -----------------------------------------------------------------------------

COUCHDB_URL="${COUCHDB_URL:-http://localhost:5984}"
COUCHDB_USER="${COUCHDB_USER:-sw360}"
COUCHDB_PASSWORD="${COUCHDB_PASSWORD:-sw360fossie}"

export COUCHDB_URL COUCHDB_USER COUCHDB_PASSWORD

script_dir="$(dirname "$(readlink -f "$0")")"

# Start couchdb
"${script_dir}"/startCouchdbForTests.sh || return

envsubst <scripts/docker-config/couchdb.properties.template | tee scripts/docker-config/etc_sw360/couchdb.properties
envsubst <scripts/docker-config/couchdb-test.properties.template | tee scripts/docker-config/etc_sw360/couchdb-test.properties

docker run -it --rm \
    --mount type=volume,source=maven_cache,target=/root/.m2 \
    --mount type=bind,src="${PWD}"/scripts/docker-config/etc_sw360/couchdb.properties,dst=/etc/sw360/couchdb.properties \
    --mount type=bind,src="${PWD}"/scripts/docker-config/etc_sw360/couchdb-test.properties,dst=/etc/sw360/couchdb-test.properties \
    --mount type=bind,src="${PWD}"/scripts/sw360BackendRestDockerConfig/etc_sw360/rest-test.properties,dst=/etc/sw360/rest-test.properties \
    -v "$(pwd)":/usr/src/mymaven \
    -w /usr/src/mymaven \
    --expose 5984 \
    --network host \
    -e COUCHDB_USER="${COUCHDB_USER}" \
    -e COUCHDB_PASSWORD="${COUCHDB_PASSWORD}" \
    -e COUCHDB_URL="${COUCHDB_URL}" \
    ghcr.io/eclipse-sw360/sw360/test \
    mvn clean package \
    -P deploy \
    -Dhelp-docs=true \
    -Dbase.deploy.dir=. \
    -Djars.deploy.dir="${PWD}/deploy" \
    -Dbackend.deploy.dir="${PWD}/deploy/webapps" \
    -Drest.deploy.dir="${PWD}/deploy/webapps"

#!/usr/bin/env bash
# Copyright (c) Bosch Software Innovations GmbH 2019.
# Part of the SW360 Portal Project.
#
# SPDX-License-Identifier: EPL-2.0
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License v2.0
# which is available at http://www.eclipse.org/legal/epl-2.0/

set -e -o pipefail

NAME=couchdb-for-sw360-testing
NOUVEAU_NAME=couchdb-nouveau-for-sw360-testing
NETWORK_NAME=sw360-test-net

# Get SW360 directory
current_dir=$(realpath "$(dirname "$0")")
sw360_dir=$(dirname "$current_dir")

if [[ "$(docker ps -q -f name=$NAME)" ]]; then
    echo "Test container is running, shutting it down ..."
    docker stop "$NAME"
fi

if [[ "$(docker ps -q -f name=$NOUVEAU_NAME)" ]]; then
    echo "Nouveau test container is running, shutting it down ..."
    docker stop "$NOUVEAU_NAME"
fi

# Create network if it doesn't exist
docker network create "$NETWORK_NAME" 2>/dev/null || true

# Start Nouveau sidecar
docker run \
    -d \
    --rm \
    -p 5987:5987 \
    -e JAVA_TOOL_OPTIONS="-Ddw.rootDir=/opt/nouveau/data/nouveau" \
    --network "$NETWORK_NAME" \
    --name "$NOUVEAU_NAME" \
    couchdb:3.5-nouveau

echo "Nouveau sidecar is started and listening on 5987."

# Start CouchDB
docker run \
    -d \
    -v "$sw360_dir"/config/couchdb/sw360_setup.ini:/opt/couchdb/etc/local.d/sw360_setup.ini \
    --rm \
    -p 5984:5984 \
    --network "$NETWORK_NAME" \
    --name "$NAME" \
    couchdb:3.5

echo "Waiting for CouchDB to be ready..."
timeout 30 bash -c 'until curl -fsS http://localhost:5984/_up >/dev/null 2>&1; do sleep 1; done'

# Configure Nouveau in CouchDB
echo "Configuring Nouveau in CouchDB..."
curl -sS -X PUT "http://localhost:5984/_node/_local/_config/nouveau/enable" -d '"true"'
curl -sS -X PUT "http://localhost:5984/_node/_local/_config/nouveau/url" -d "\"http://${NOUVEAU_NAME}:5987\""
echo ""

echo "Test containers are started. CouchDB on 5984, Nouveau on 5987."

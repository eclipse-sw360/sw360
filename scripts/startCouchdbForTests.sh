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
COUCHDB_USER=${COUCHDB_USER:-admin}
COUCHDB_PASSWORD=${COUCHDB_PASSWORD:-password}
COUCHDB_HOST=${COUCHDB_HOST:-localhost}

wait_couchdb() {
  # Wait for it to be up
  until curl -s http://${COUCHDB_HOST}:5984 >/dev/null 2>&1; do
      sleep 1
  done

  # Check id database is already created
  error=$(curl --head http://${COUCHDB_USER}:${COUCHDB_PASSWORD}@${COUCHDB_HOST}:5984/_users | head -n 1 | cut -d' ' -f2)
  [ ! "$error" == "404" ] && return

  # Couchdb docker no cluster
  curl -X PUT http://${COUCHDB_USER}:${COUCHDB_PASSWORD}@${COUCHDB_HOST}:5984/_users
  curl -X PUT http://${COUCHDB_USER}:${COUCHDB_PASSWORD}@${COUCHDB_HOST}:5984/_replicator
  curl -X PUT http://${COUCHDB_USER}:${COUCHDB_PASSWORD}@${COUCHDB_HOST}:5984/_global_changes
}

if [[ "$(docker ps -q -f name=$NAME)" ]]; then
    echo "Test container is running, shutting it down ..."
    docker kill "$NAME"
fi

echo "Test container is not running, starting it ..."

docker run \
    -e COUCHDB_USER="${COUCHDB_USER}" \
    -e COUCHDB_PASSWORD="${COUCHDB_PASSWORD}" \
        --rm \
        -p 5984:5984 \
        -d \
        --name "$NAME" \
        couchdb:3.1

wait_couchdb

echo "Test container is started and listening on 5984."



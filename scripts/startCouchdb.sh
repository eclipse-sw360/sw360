#!/usr/bin/env bash
# Copyright (c) Bosch Software Innovations GmbH 2019.
# Part of the SW360 Portal Project.
#
# SPDX-License-Identifier: EPL-2.0
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License v2.0
# which is available at http://www.eclipse.org/legal/epl-2.0/

set -e

NAME=sw360_development_couchdb
COUCHDB_VERSION=${COUCHDB_VERSION:-3}
COUCHDB_HOST=${COUCHDB_HOST:-localhost}

wait_couchdb() {
  # Wait for it to be up
  until curl --noproxy ${COUCHDB_HOST} -s http://${COUCHDB_HOST}:5984 >/dev/null 2>&1; do
      sleep 1
  done

  # Check if database is already created
  error=$(curl --noproxy localhost --head http://admin:password@${COUCHDB_HOST}:5984/_bla | head -n 1 | cut -d' ' -f2)
  [ ! "$error" == "404" ] && return

  # Couchdb docker no cluster
  curl --noproxy ${COUCHDB_HOST} -X PUT http://admin:password@${COUCHDB_HOST}:5984/_users
  curl --noproxy ${COUCHDB_HOST} -X PUT http://admin:password@${COUCHDB_HOST}:5984/_replicator
  curl --noproxy ${COUCHDB_HOST} -X PUT http://admin:password@${COUCHDB_HOST}:5984/_global_changes
}


if [[ ! "$(docker ps -q -f name=$NAME)" ]]; then
    if netstat -tln | grep -q -e ':5984.*LISTEN' 2>/dev/null; then
        echo "port is already bocked, probably by another sw360 deployment"
        exit 1
    fi
else
    echo "Test container is running, shutting it down ..."
    docker stop "$NAME"
    echo "Test container is stopped."
fi

echo "Test container is not running, starting it ..."
docker run \
    -e COUCHDB_USER=admin \
    -e COUCHDB_PASSWORD=password \
        --rm \
        -p 5984:5984 \
        -d \
        --name "$NAME" \
        couchdb:${COUCHDB_VERSION}
echo "Test container is started and listening on 5984."

wait_couchdb

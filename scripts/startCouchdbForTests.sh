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

# Get SW360 directory
current_dir=$(realpath "$(dirname "$0")")
sw360_dir=$(dirname "$current_dir")

if [[ "$(docker ps -q -f name=$NAME)" ]]; then
    echo "Test container is running, shutting it down ..."
    docker stop "$NAME"
fi

docker run \
    -d \
    -v "$sw360_dir"/config/couchdb/sw360_setup.ini:/opt/couchdb/etc/local.d/sw360_setup.ini \
    --rm \
    -p 5984:5984 \
    --name "$NAME" \
    couchdb:3

echo "Test container is started and listening on 5984."

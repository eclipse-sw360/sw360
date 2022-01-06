#!/bin/bash -x

# -----------------------------------------------------------------------------
# Copyright BMW CarIT GmbH 2021
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# This script is executed on startup of Docker container.
# (execution of docker run cmd) starts couchdb and tomcat.
# -----------------------------------------------------------------------------

set -e

GIT_ROOT=$(git rev-parse --show-toplevel)

# Download dependencies outside container
"$GIT_ROOT"/scripts/docker-config/download_dependencies.sh

# To avoid excessive copy, we will export the git archive of the sources to deps
git archive --output=deps/sw360.tar --format=tar --prefix=sw360/ HEAD

COMPOSE_DOCKER_CLI_BUILD=1
DOCKER_BUILDKIT=1
export DOCKER_BUILDKIT COMPOSE_DOCKER_CLI_BUILD

if [ -n "$FOSSOLOGY" ]; then
    #shellcheck disable=SC2086
    docker-compose -f "$GIT_ROOT"/fossology-docker-compose.yml build
else
    #shellcheck disable=SC2086
    docker-compose -f "$GIT_ROOT"/docker-compose.yml build
fi

#!/bin/bash

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

# Set default versions
CLUCENE_VERSION=${CLUCENE_VERSION:-2.1.0}
THRIFT_VERSION=${THRIFT_VERSION:-0.16.0}
MAVEN_VERSION=${MAVEN_VERSION:-3.8.6}

GIT_ROOT=$(git rev-parse --show-toplevel)

COMPOSE_DOCKER_CLI_BUILD=1
DOCKER_BUILDKIT=1
export DOCKER_BUILDKIT COMPOSE_DOCKER_CLI_BUILD

usage() {
    echo "Usage:"
    echo "--help This messaqge"
    echo "--verbose Verbose build"
    echo "--no-cache Invalidate buildkit cache"
    exit 0;
}

for arg in "$@"; do
    if [ "$arg" == "--help" ]; then
        usage
    elif [ "$arg" == "--verbose" ]; then
        docker_verbose="--progress=plain"
    elif [ "$arg" == "--no-cache" ]; then
        docker_no_cache="--no-cache"
    else
        echo "Unsupported parameter: $arg"
        usage
    fi
    shift
done

#shellcheck disable=SC2086
docker compose \
    --file "$GIT_ROOT"/docker-compose.yml \
    build \
    --build-arg BUILDKIT_INLINE_CACHE=1 \
    --build-arg CLUCENE_VERSION="$CLUCENE_VERSION" \
    --build-arg THRIFT_VERSION="$THRIFT_VERSION" \
    --build-arg MAVEN_VERSION="$MAVEN_VERSION" \
    $docker_verbose \
    $docker_no_cache

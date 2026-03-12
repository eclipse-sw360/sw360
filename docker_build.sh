#!/bin/bash

# -----------------------------------------------------------------------------
# Copyright BMW CarIT GmbH 2021
# Copyright Helio Chissini de Castro 2022-2025
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
#
# Usage:
#   ./docker_build.sh [options]
#
# Options:
#   --cvesearch-host <url>   Set the CVE Search host URL
#
# Examples:
#   ./docker_build.sh
#   ./docker_build.sh --cvesearch-host https://cvesearch.sw360.org
#   DOCKER_IMAGE_ROOT=myregistry.com/sw360 ./docker_build.sh
#

set -e -o pipefail

# Parse command-line arguments
other_args=()
while [[ "$#" -gt 0 ]]; do
    case $1 in
        --cvesearch-host)
            shift
            cvesearch_host="$1"
        ;;
        *)
            other_args+=("$1")  # store other arguments
        ;;
    esac
    shift
done

# Validate the --cvesearch-host value is a URL
if [ -n "$cvesearch_host" ]; then
    if [[ $cvesearch_host =~ ^https?:// ]]; then
        sed -i "s@cvesearch.host=.*@cvesearch.host=$cvesearch_host@g"\
        ./backend/src/src-cvesearch/src/main/resources/cvesearch.properties
    else
        echo "Warning: CVE Search host is not a URL: $cvesearch_host"
    fi
fi

# Restore other arguments
set -- "${other_args[@]}"

DOCKER_IMAGE_ROOT="${DOCKER_IMAGE_ROOT:-ghcr.io/eclipse-sw360}"
VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
GIT_REV=$(git rev-parse --short=8 HEAD)
SW360_VERSION="$VERSION-$GIT_REV"
THRIFT_VERSION=$(cat third-party/thrift/version)

export DOCKER_PLATFORM DOCKER_IMAGE_ROOT GIT_REVISION SW360_VERSION THRIFT_VERSION

echo "Building docker image for SW360 ${SW360_VERSION}"

# Check for docker buildx support
if docker buildx version >/dev/null 2>&1; then
    DOCKER_BUILD_CMD="docker buildx build"
    LOAD_ARG="--load"
else
    # Fallback for standard docker or podman alias
    DOCKER_BUILD_CMD="docker build"
    LOAD_ARG=""
fi

# ---------------------------
# image_build function
# Usage ( position parameters):
# image_build <target_name> <tag_name> <version> <dockerfile> <context> <extra_args...>
image_build() {
    local target="$1"
    shift
    local name="$1"
    shift
    local version="$1"
    shift
    local dockerfile="$1"
    shift
    local context="$1"
    shift

    # Construct the command array
    # shellcheck disable=SC2206
    local cmd=($DOCKER_BUILD_CMD)

    if [ -n "$target" ]; then
        cmd+=("--target" "$target")
    fi

    if [ -n "$name" ]; then
         cmd+=("--tag" "${DOCKER_IMAGE_ROOT}/$name:$version")
         cmd+=("--tag" "${DOCKER_IMAGE_ROOT}/$name:latest")
    fi

    if [ -n "$LOAD_ARG" ]; then
        cmd+=("$LOAD_ARG")
    fi

    # Append remaining arguments and context
    cmd+=("$@" "-f" "$dockerfile" "$context")

    echo "Running: ${cmd[*]}"
    "${cmd[@]}"
}

# 1. Build Thrift image
echo "Building Thrift image..."
image_build "" "thrift" "${THRIFT_VERSION}-noble" "third-party/thrift/Dockerfile" "." \
--build-arg THRIFT_VERSION="${THRIFT_VERSION}"

# 2. Build SW360 Binaries
echo "Building SW360 Binaries..."
image_build "binaries" "sw360/binaries" "$SW360_VERSION" "Dockerfile" "."

# 3. Build SW360 Runtime
echo "Building SW360 Runtime..."
# For runtime build, we need to pass the build context for binaries if using buildx
# If using standard build, we might need a different approach, but let's assume modern docker/podman behavior
BUILD_CONTEXT_ARGS=("--build-context" "binaries=docker-image://${DOCKER_IMAGE_ROOT}/sw360/binaries:latest")

image_build "sw360" "sw360" "$SW360_VERSION" "Dockerfile" "." \
"${BUILD_CONTEXT_ARGS[@]}" \
"$@"

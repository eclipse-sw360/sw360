#!/bin/bash

# -----------------------------------------------------------------------------
# Copyright BMW CarIT GmbH 2021
# Copyright Helio Chissini de Castro 2022-2023
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

# Parse command-line arguments
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

set -- "${other_args[@]}"  # restore other arguments

set -e -o pipefail

DOCKER_IMAGE_ROOT="${DOCKER_IMAGE_ROOT:-ghcr.io/eclipse-sw360}"
SECRETS=${SECRETS:-"$PWD/config/couchdb/default_secrets"}
VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
GIT_REV=$(git rev-parse --short=8 HEAD)
SW360_VERSION="$VERSION-$GIT_REV"
export DOCKER_PLATFORM DOCKER_IMAGE_ROOT GIT_REVISION SECRETS SW360_VERSION

echo "Building docker image for SW360 ${SW360_VERSION}"

# ---------------------------
# image_build function
# Usage ( position paramenters):
# image_build <target_name> <tag_name> <version> <extra_args...>

image_build() {
    local target
    local name
    local version
    target="$1"
    shift
    name="$1"
    shift
    version="$1"
    shift

    docker buildx build \
    --target "$target" \
    --tag "${DOCKER_IMAGE_ROOT}/$name:$version" \
    --tag "${DOCKER_IMAGE_ROOT}/$name:latest" \
    --load \
    "$@" .
}

image_build binaries sw360/binaries "$SW360_VERSION" \
--secret id=couchdb,src="$SECRETS"

image_build sw360 sw360 "$SW360_VERSION" \
--build-context "binaries=docker-image://${DOCKER_IMAGE_ROOT}/sw360/binaries:latest" "$@"

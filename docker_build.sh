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
if [ ! -z "$cvesearch_host" ]; then
    if [[ $cvesearch_host =~ ^https?:// ]]; then
        sed -i "s@cvesearch.host=.*@cvesearch.host=$cvesearch_host@g"\
            ./backend/src/src-cvesearch/src/main/resources/cvesearch.properties
    else
        echo "Warning: CVE Search host is not a URL: $cvesearch_host"
    fi
fi

set -- "${other_args[@]}"  # restore other arguments


set -e -o pipefail

# Source the version
# shellcheck disable=SC1091
. .versions

DOCKER_IMAGE_ROOT="${DOCKER_IMAGE_ROOT:-eclipse-sw360}"
SECRETS=${SECRETS:-"$PWD/scripts/docker-config/default_secrets"}
SW360_VERSION=${SW360_VERSION:-18-development}
export DOCKER_PLATFORM DOCKER_IMAGE_ROOT GIT_REVISION SECRETS

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
        --tag "ghcr.io/${DOCKER_IMAGE_ROOT}/$name:$version" \
        --tag "ghcr.io/${DOCKER_IMAGE_ROOT}/$name:latest" \
        "$@" .
}

image_build base base "$SW360_VERSION" --build-arg LIFERAY_VERSION="$LIFERAY_VERSION" --build-arg LIFERAY_SOURCE="$LIFERAY_SOURCE" "$@"

image_build sw360thrift thrift "$THRIFT_VERSION" --build-arg THRIFT_VERSION="$THRIFT_VERSION" "$@"

image_build sw360 binaries "$SW360_VERSION" --build-arg MAVEN_VERSION="$MAVEN_VERSION" \
    --secret id=sw360,src="$SECRETS" \
    --build-context "sw360thrift=docker-image://${DOCKER_IMAGE_ROOT}/thrift:latest" "$@"

image_build runtime sw360 "$SW360_VERSION" \
    --build-context "base=docker-image://${DOCKER_IMAGE_ROOT}/base:latest" \
    --build-context "sw360=docker-image://${DOCKER_IMAGE_ROOT}/binaries:latest" "$@"

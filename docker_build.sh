#!/bin/bash

# -----------------------------------------------------------------------------
# Copyright BMW CarIT GmbH 2021
# Copyright Helio Chissini de Castro 2022
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

set -e -o  pipefail

# Source the version
# shellcheck disable=SC1091
. .versions

DOCKER_IMAGE_ROOT="${DOCKER_IMAGE_ROOT:-eclipse-sw360}"
GIT_REVISION=$(git describe --abbrev=6 --always --tags --match=[0-9]*)
SECRETS=${SECRETS:-"$PWD/scripts/docker-config/default_secrets"}
export DOCKER_PLATFORM DOCKER_IMAGE_ROOT GIT_REVISION SECRETS

# ---------------------------
# image_build function
# Usage ( position paramenters):
# image_build <target_name> <tag_name> <version> <extra_args...>

image_build() {
    local target
    local name
    local version
    target="$1"; shift
    name="$1"; shift
    version="$1"; shift

    docker buildx build \
        --target "$target" \
        --tag "${DOCKER_IMAGE_ROOT}/$name:$version" \
        --tag "${DOCKER_IMAGE_ROOT}/$name:latest" \
        --tag "ghcr.io/${DOCKER_IMAGE_ROOT}/$name:$version" \
        --tag "ghcr.io/${DOCKER_IMAGE_ROOT}/$name:latest" \
        "$@" .
}

image_build base base "$GIT_REVISION" --build-arg LIFERAY_VERSION="$LIFERAY_VERSION" --build-arg LIFERAY_SOURCE="$LIFERAY_SOURCE" "$@"

image_build sw360thrift thrift "$THRIFT_VERSION" --build-arg THRIFT_VERSION="$THRIFT_VERSION" "$@"

image_build sw360clucene clucene "$CLUCENE_VERSION" --build-arg CLUCENE_VERSION="$CLUCENE_VERSION" --build-arg MAVEN_VERSION="$MAVEN_VERSION" "$@"

image_build sw360 binaries "$GIT_REVISION" --build-arg MAVEN_VERSION="$MAVEN_VERSION" \
    --secret id=sw360,src=$SECRETS \
    --build-context "sw360thrift=docker-image://${DOCKER_IMAGE_ROOT}/thrift:latest" \
    --build-context "sw360clucene=docker-image://${DOCKER_IMAGE_ROOT}/clucene:latest" "$@"

image_build runtime sw360 "$GIT_REVISION" \
    --build-context "base=docker-image://${DOCKER_IMAGE_ROOT}/base:latest" \
    --build-context "sw360=docker-image://${DOCKER_IMAGE_ROOT}/binaries:latest" "$@"

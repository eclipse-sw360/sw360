#!/bin/bash -x

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

PODMAN_BUILD=""

while [[ "$#" -gt 0 ]]; do
    case $1 in
        --cvesearch-host)
            shift
            cvesearch_host="$1"
        ;;
        --podman)
            shift
            PODMAN_BUILD="podman build"
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

CONTAINER_BUILD="${PODMAN_BUILD:-docker buildx build}"
CONTAINER_IMAGE_ROOT="${CONTAINER_IMAGE_ROOT:-ghcr.io/eclipse-sw360}"
SECRETS=${SECRETS:-"$PWD/config/couchdb/default_secrets"}
VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
GIT_REV=$(git rev-parse --short=8 HEAD)
SW360_VERSION="$VERSION-$GIT_REV"

echo "Building container image for SW360 ${SW360_VERSION} using ${CONTAINER_BUILD}."

${CONTAINER_BUILD} \
    --target sw360 \
    --secret id=couchdb,src="$SECRETS" \
    --tag "${CONTAINER_IMAGE_ROOT}/sw360:${SW360_VERSION}" \
    --tag "${CONTAINER_IMAGE_ROOT}/sw360:latest" \
    "$@" .

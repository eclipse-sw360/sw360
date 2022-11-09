#!/bin/bash

# -----------------------------------------------------------------------------
# Copyright Siemens AG, 2020. Part of the SW360 Portal Project.
# Copyright BMW CarIT GmbH. 2021
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# This script downloads liferay and OSGi modules.
# -----------------------------------------------------------------------------

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

SW360_DEPS_DIR="${SW360_DEPS_DIR:-deps}"
export SW360_DEPS_DIR

# source the versions
# shellcheck disable=SC1091
source "${SCRIPT_DIR}/versions.sh"

dependencies=(
  https://github.com/liferay/liferay-portal/releases/download/"$LIFERAY_VERSION"/"$LIFERAY_SOURCE"
  https://github.com/rnewson/couchdb-lucene/archive/v"$CLUCENE_VERSION".tar.gz
  http://archive.apache.org/dist/thrift/"$THRIFT_VERSION"/thrift-"$THRIFT_VERSION".tar.gz
  https://dlcdn.apache.org/maven/maven-3/"$MAVEN_VERSION"/binaries/apache-maven-"$MAVEN_VERSION"-bin.tar.gz
)

download_dependency() {
  if [ ! -f "$(basename "$1")" ]; then
    curl -k -O -J -L "$1"
  fi
}

# Create directory if not available
mkdir -p "$SW360_DEPS_DIR" || exit 1

# Direct tarball deps
mkdir -p "$SW360_DEPS_DIR" && cd "$SW360_DEPS_DIR" || exit 1
for dep in "${dependencies[@]}"; do
  download_dependency "$dep"
done
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

jar_dependencies=(
  com/fasterxml/jackson/core/jackson-annotations/2.13.4/jackson-annotations-2.13.4.jar
  com/fasterxml/jackson/core/jackson-core/2.13.4/jackson-core-2.13.4.jar
  com/fasterxml/jackson/core/jackson-databind/2.13.4.2/jackson-databind-2.13.4.2.jar
  com/google/code/gson/gson/2.8.9/gson-2.8.9.jar
  com/google/guava/guava/31.1-jre/guava-31.1-jre.jar
  commons-codec/commons-codec/1.15/commons-codec-1.15.jar
  commons-io/commons-io/2.11.0/commons-io-2.11.0.jar
  commons-logging/commons-logging/1.2/commons-logging-1.2.jar
  org/apache/commons/commons-collections4/4.4/commons-collections4-4.4.jar
  org/apache/commons/commons-csv/1.9.0/commons-csv-1.9.0.jar
  org/apache/commons/commons-lang3/3.12.0/commons-lang3-3.12.0.jar
)

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

# JAR dependencies from maven repository
mkdir -p jars && cd jars || exit 1
for dep in "${jar_dependencies[@]}"; do
  download_dependency "https://search.maven.org/remotecontent?filepath=$dep"
done

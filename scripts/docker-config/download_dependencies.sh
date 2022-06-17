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

SW360_DEPS_DIR="${SW360_DEPS_DIR:-deps}"

jar_dependencies=(
  https://search.maven.org/remotecontent?filepath=commons-codec/commons-codec/1.12/commons-codec-1.12.jar
  https://search.maven.org/remotecontent?filepath=org/apache/commons/commons-collections4/4.4/commons-collections4-4.4.jar
  https://search.maven.org/remotecontent?filepath=org/apache/commons/commons-csv/1.4/commons-csv-1.4.jar
  https://search.maven.org/remotecontent?filepath=commons-io/commons-io/2.7/commons-io-2.7.jar
  https://search.maven.org/remotecontent?filepath=commons-lang/commons-lang/2.4/commons-lang-2.4.jar
  https://search.maven.org/remotecontent?filepath=commons-logging/commons-logging/1.2/commons-logging-1.2.jar
  https://search.maven.org/remotecontent?filepath=com/google/code/gson/gson/2.8.9/gson-2.8.9.jar
  https://search.maven.org/remotecontent?filepath=com/google/guava/guava/31.0.1-jre/guava-31.0.1-jre.jar
  https://search.maven.org/remotecontent?filepath=com/fasterxml/jackson/core/jackson-annotations/2.13.2/jackson-annotations-2.13.2.jar
  https://search.maven.org/remotecontent?filepath=com/fasterxml/jackson/core/jackson-core/2.13.2/jackson-core-2.13.2.jar
  https://search.maven.org/remotecontent?filepath=com/fasterxml/jackson/core/jackson-databind/2.13.2.2/jackson-databind-2.13.2.2.jar
  https://repo1.maven.org/maven2/org/apache/commons/commons-compress/1.20/commons-compress-1.20.jar
  https://repo1.maven.org/maven2/org/apache/thrift/libthrift/0.14.0/libthrift-0.14.0.jar
)

dependencies=(
  https://github.com/liferay/liferay-portal/releases/download/7.3.4-ga5/liferay-ce-portal-tomcat-7.3.4-ga5-20200811154319029.tar.gz
  https://sourceforge.net/projects/lportal/files/Liferay%20Portal/7.3.4%20GA5/liferay-ce-portal-tomcat-7.3.4-ga5-20200811154319029.tar.gz
  http://archive.apache.org/dist/thrift/0.14.0/thrift-0.14.0.tar.gz
  https://github.com/rnewson/couchdb-lucene/archive/v2.1.0.tar.gz
  https://raw.githubusercontent.com/sw360/sw360vagrant/master/shared/couchdb-lucene.patch
)

download_dependency() {
  if [ ! -f "$(basename "$1")" ]; then
    curl -k -O -J -L "$1"
  fi
}

# Main deps
mkdir -p "$SW360_DEPS_DIR" && cd "$SW360_DEPS_DIR" || exit 1
for dep in "${dependencies[@]}"; do
  download_dependency "$dep"
done

mkdir -p jars && cd jars || exit 1
for dep in "${jar_dependencies[@]}"; do
  download_dependency "$dep"
done

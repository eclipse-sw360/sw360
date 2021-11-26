#!/bin/bash
# -----------------------------------------------------------------------------
# Copyright Siemens AG, 2020. Part of the SW360 Portal Project.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# This script downloads couchdb-lucene package from github,
# applies required patch and builds the war file.
# -----------------------------------------------------------------------------

VERSION=${THRIFT_VERSION:-2.1.0}
CLUCENE_SOURCE="deps/couchdb-lucene-$VERSION.tar.gz"
BUILDDIR=$(mktemp -d)

[ ! -f "$CLUCENE_SOURCE" ] && exit 1
[ ! -f "deps/couchdb-lucene.patch" ] && exit 1

tar -xzf "$CLUCENE_SOURCE" --strip-components=1 -C "$BUILDDIR"
cp deps/couchdb-lucene.patch "$BUILDDIR"

cd "$BUILDDIR" || exit 1

# Replace defaults
sed -i "s/allowLeadingWildcard=false/allowLeadingWildcard=true/" ./src/main/resources/couchdb-lucene.ini
sed -i "s/localhost:5984/admin:password@localhost:5984/" ./src/main/resources/couchdb-lucene.ini

patch -p1 < couchdb-lucene.patch
mvn clean install war:war
cp ./target/*.war /couchdb-lucene.war

# We remove the build dir to avoid keep the docker layer big
cd || return
rm -rf "$BUILDDIR"

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

build_couchdb_lucene() {
  wget https://github.com/rnewson/couchdb-lucene/archive/v2.1.0.tar.gz -O couchdb-lucene.tar.gz
  tar -xzf couchdb-lucene.tar.gz
  cd couchdb-lucene-2.1.0
  sed -i "s/allowLeadingWildcard=false/allowLeadingWildcard=true/" ./src/main/resources/couchdb-lucene.ini
  wget https://raw.githubusercontent.com/sw360/sw360vagrant/master/shared/couchdb-lucene.patch
  patch -p1 < couchdb-lucene.patch
  mvn -s /app/build/sw360/scripts/docker-config/mvn-proxy-settings.xml clean install war:war
  cp ./target/*.war /app/build/sw360/deployables/webapps/couchdb-lucene.war
}

build_couchdb_lucene

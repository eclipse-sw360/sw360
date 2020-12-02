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
# This script installs couchdb, configures couchdb-lucene
# and accessibility from host machine.
# -----------------------------------------------------------------------------

install_configure_couchdb() {
  install_couchdb
  configure_couchdb_db
}

install_couchdb() {
  apt-get install curl -y --no-install-recommends
  curl -L https://couchdb.apache.org/repo/bintray-pubkey.asc  | apt-key add
  echo "deb https://apache.bintray.com/couchdb-deb bionic main" | tee -a /etc/apt/sources.list
  apt-get update
  DEBIAN_FRONTEND=noninteractive apt-get install -y couchdb=2.1.2~bionic
}

configure_couchdb_db() {
  sed -i "s/bind_address = 127.0.0.1/bind_address = 0.0.0.0/" /opt/couchdb/etc/default.ini
}

install_configure_couchdb

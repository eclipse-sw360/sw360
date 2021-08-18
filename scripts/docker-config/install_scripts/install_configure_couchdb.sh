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
  curl https://couchdb.apache.org/repo/keys.asc | gpg --dearmor |  tee /usr/share/keyrings/couchdb-archive-keyring.gpg >/dev/null 2>&1
  source /etc/os-release
  echo "deb [signed-by=/usr/share/keyrings/couchdb-archive-keyring.gpg] https://apache.jfrog.io/artifactory/couchdb-deb/ ${VERSION_CODENAME} main" \
    |  tee /etc/apt/sources.list.d/couchdb.list >/dev/null
  apt-get update
  COUCHDB_PASSWORD=password
  echo "couchdb couchdb/mode select standalone
  couchdb couchdb/mode seen true
  couchdb couchdb/bindaddress string 127.0.0.1
  couchdb couchdb/bindaddress seen true
  couchdb couchdb/adminpass password ${COUCHDB_PASSWORD}
  couchdb couchdb/adminpass seen true
  couchdb couchdb/adminpass_again password ${COUCHDB_PASSWORD}
  couchdb couchdb/adminpass_again seen true" | debconf-set-selections
  DEBIAN_FRONTEND=noninteractive apt-get install -y --force-yes couchdb
}

configure_couchdb_db() {
  sed -i "s/bind_address = 127.0.0.1/bind_address = 0.0.0.0/" /opt/couchdb/etc/default.ini
}

install_configure_couchdb

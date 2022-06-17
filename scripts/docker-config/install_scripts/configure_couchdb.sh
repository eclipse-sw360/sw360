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

COUCHDB_ADMIN="${COUCHDB_ADMIN:-admin}"
COUCHDB_PASSWD="${COUCHDB_PASSWD:-password}"

configure_sw360() {
  cat <<EOF > /opt/couchdb/etc/local.d/sw360.ini
[admins]
${COUCHDB_ADMIN} = ${COUCHDB_PASSWD}

[chttpd]
port = 5984
bind_address = 0.0.0.0
EOF
}

configure_sw360

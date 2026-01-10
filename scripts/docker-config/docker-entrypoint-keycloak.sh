#!/bin/env bash
# SPDX-License-Identifier: EPL-2.0
# Simplified version of docker-entrypoint.sh for KeyCloak image

set -o errexit -o nounset -o pipefail

# Set default values for environment variables
export COUCHDB_URL="${COUCHDB_URL:-http://couchdb:5984}"
export COUCHDB_USER="${COUCHDB_USER:-admin}"
export COUCHDB_LUCENESEARCH_LIMIT="${COUCHDB_LUCENESEARCH_LIMIT:-1000}"

# Read secrets from Docker secrets if available
if [ -f "/run/secrets/COUCHDB_PASSWORD" ]; then
  export COUCHDB_PASSWORD=$(cat /run/secrets/COUCHDB_PASSWORD)
else
  export COUCHDB_PASSWORD="${COUCHDB_PASSWORD:-admin}"
fi

mkdir -p /etc/sw360/

# Write configuration
printf "couchdb.url = %s\n\
couchdb.user = %s\n\
couchdb.password = %s\n\
couchdb.database = sw360db\n\
couchdb.usersdb = sw360users\n\
couchdb.attachments = sw360attachments\n\
couchdb.change_logs = sw360changelogs\n\
couchdb.config = sw360config\n\
couchdb.vulnerability_management = sw360vm\n\
lucenesearch.limit = %d\n\
lucenesearch.leading.wildcard = false\n" \
"$COUCHDB_URL" "$COUCHDB_USER" "$COUCHDB_PASSWORD" "$COUCHDB_LUCENESEARCH_LIMIT" > /etc/sw360/couchdb.properties

# Start KeyCloak server
exec /opt/keycloak/bin/kc.sh "$@"

#!/bin/env bash
# Part of the SW360 Portal Project.
# SPDX-License-Identifier: EPL-2.0
# Simplified version of docker-entrypoint.sh for KeyCloak image

set -o errexit -o nounset -o pipefail

# Read secrets from Docker secrets if available
if [ -f "/run/secrets/COUCHDB_SECRETS" ]; then
  source /run/secrets/COUCHDB_SECRETS
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

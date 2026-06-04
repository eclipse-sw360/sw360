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
cloudant.enable.retries = %s\n\
cloudant.max.retries = %s\n\
cloudant.max.retry.interval = %s\n\
cloudant.pool.max.idle.connections = %s\n\
cloudant.pool.keepalive.seconds = %s\n\
cloudant.max.requests = %s\n\
cloudant.max.requests.per.host = %s\n" \
"$COUCHDB_URL" "$COUCHDB_USER" "$COUCHDB_PASSWORD" "$COUCHDB_LUCENESEARCH_LIMIT" "$CLOUDANT_ENABLE_RETRIES" \
"$CLOUDANT_MAX_RETRIES" "$CLOUDANT_MAX_RETRY_INTERVAL" "$CLOUDANT_POOL_MAX_IDLE_CONNECTIONS" "$CLOUDANT_POOL_KEEPALIVE_SECONDS" \
"$CLOUDANT_MAX_REQUESTS" "$CLOUDANT_MAX_REQUESTS_PER_HOST" > /etc/sw360/couchdb.properties

# Start KeyCloak server
exec /opt/keycloak/bin/kc.sh "$@"

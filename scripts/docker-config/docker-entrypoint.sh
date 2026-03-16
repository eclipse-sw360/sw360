#!/bin/env bash
# Part of the SW360 Portal Project.
# SPDX-License-Identifier: EPL-2.0

set -o errexit -o nounset -o pipefail

# Source secrets if available. This allows overriding the default ENV values.
if [ -f "/run/secrets/COUCHDB_SECRETS" ]; then
  source /run/secrets/COUCHDB_SECRETS
fi
if [ -f "/run/secrets/SW360_SECRETS" ]; then
  source /run/secrets/SW360_SECRETS
fi

mkdir -p /etc/sw360/authorization /etc/sw360/rest

# Write configuration from environment variables
/usr/bin/envsubst < /app/sw360/couchdb.properties.template > /etc/sw360/couchdb.properties
/usr/bin/envsubst < /app/sw360/etc_sw360/authorization/application.yml.template > /etc/sw360/authorization/application.yml
/usr/bin/envsubst < /app/sw360/etc_sw360/rest/application.yml.template > /etc/sw360/rest/application.yml
/usr/bin/envsubst < /app/sw360/etc_sw360/sw360.properties.template > /etc/sw360/sw360.properties
/usr/bin/envsubst < /app/sw360/manager/tomcat-users.xml > "$CATALINA_HOME"/conf/tomcat-users.xml

# Wait for DB
test_for_couchdb() {
  curl -s "$COUCHDB_URL"/_up | grep -q '"status":"ok"'
  return $?
}
until test_for_couchdb; do
  >&2 echo "CouchDB is unavailable - sleeping"
  sleep 1
done

# Start Tomcat
echo
echo 'SW360 configuration complete; Starting up...'
echo
"$CATALINA_HOME"/bin/catalina.sh run

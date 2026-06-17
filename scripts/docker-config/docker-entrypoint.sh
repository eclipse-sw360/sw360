#!/bin/env bash
# Part of the SW360 Portal Project.
# SPDX-License-Identifier: EPL-2.0

set -o errexit -o nounset -o pipefail

# Export sourced secrets so envsubst can see them when generating configs.
set -o allexport
if [ -f "/run/secrets/COUCHDB_SECRETS" ]; then
  source /run/secrets/COUCHDB_SECRETS
fi
if [ -f "/run/secrets/SW360_SECRETS" ]; then
  source /run/secrets/SW360_SECRETS
fi
set +o allexport

mkdir -p /etc/sw360/authorization /etc/sw360/rest

# Seed JWT signing keystore with explicit source precedence:
# 1) Docker secret JWT_KEYSTORE (operator override)
# 2) Existing persisted /etc/sw360/jwt-keystore.jks
# 3) Bundled fallback /app/sw360/jwt-keystore.jks
if [ -f /run/secrets/JWT_KEYSTORE ]; then
  cp /run/secrets/JWT_KEYSTORE /etc/sw360/jwt-keystore.jks
  chmod 600 /etc/sw360/jwt-keystore.jks
  echo "Seeded /etc/sw360/jwt-keystore.jks from Docker secret JWT_KEYSTORE."
elif [ -f /etc/sw360/jwt-keystore.jks ]; then
  echo "Using existing /etc/sw360/jwt-keystore.jks from persisted volume."
elif [ -f /app/sw360/jwt-keystore.jks ]; then
  cp /app/sw360/jwt-keystore.jks /etc/sw360/jwt-keystore.jks
  chmod 600 /etc/sw360/jwt-keystore.jks
  echo "Seeded /etc/sw360/jwt-keystore.jks from bundled fallback."
else
  echo "WARNING: No JWT keystore found at /etc/sw360/jwt-keystore.jks and no" \
       "Docker secret JWT_KEYSTORE or bundled fallback at /app/sw360/jwt-keystore.jks." >&2
  echo "WARNING: Authorization server startup may fail if no classpath fallback is available." >&2
fi

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

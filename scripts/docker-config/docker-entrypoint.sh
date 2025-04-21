#!/bin/env bash
# SPDX-License-Identifier: EPL-2.0

set -o errexit -o nounset -o pipefail

# Set default values for environment variables
COUCHDB_URL="${COUCHDB_URL:-http://couchdb:5984}"
COUCHDB_USER="${COUCHDB_USER:-admin}"
COUCHDB_LUCENESEARCH_LIMIT="${COUCHDB_LUCENESEARCH_LIMIT:-1000}"
ENABLE_DISKSPACE="${ENABLE_DISKSPACE:-false}"
JWKS_ISSUER_URI="${JWKS_ISSUER_URI:-http://localhost:8080/authorization/oauth2/jwks}"
JWKS_SET_URI="${JWKS_SET_URI:-http://localhost:8080/authorization/oauth2/jwks}"
SVM_SW360_API_URL="${SVM_SW360_API_URL:-https://svmtest.cert.siemens.com}"
JWKS_ISSUER="${JWKS_ISSUER:-http://localhost:8090}"

# Read secrets from Docker secrets if available
if [ -f "/run/secrets/COUCHDB_PASSWORD" ]; then
  COUCHDB_PASSWORD=$(cat /run/secrets/COUCHDB_PASSWORD)
else
  COUCHDB_PASSWORD="${COUCHDB_PASSWORD:-admin}"
fi
if [ -f "/run/secrets/SVM_SW360_CERTIFICATE_PASSPHRASE" ]; then
  SVM_SW360_CERTIFICATE_PASSPHRASE=$(cat /run/secrets/SVM_SW360_CERTIFICATE_PASSPHRASE)
else
  SVM_SW360_CERTIFICATE_PASSPHRASE="${SVM_SW360_CERTIFICATE_PASSPHRASE:-}"
fi
if [ -f "/run/secrets/SVM_SW360_JKS_PASSWORD" ]; then
  SVM_SW360_JKS_PASSWORD=$(cat /run/secrets/SVM_SW360_JKS_PASSWORD)
else
  SVM_SW360_JKS_PASSWORD="${SVM_SW360_JKS_PASSWORD:-}"
fi
if [ -f "/run/secrets/REST_APITOKEN_HASH_SALT" ]; then
  REST_APITOKEN_HASH_SALT=$(cat /run/secrets/REST_APITOKEN_HASH_SALT)
else
  REST_APITOKEN_HASH_SALT="${REST_APITOKEN_HASH_SALT:-}"
fi

mkdir -p /etc/sw360/authorization /etc/sw360/rest

# Write configuration
/usr/bin/envsubst < /app/docker-config/couchdb.properties.template > /etc/sw360/couchdb.properties
/usr/bin/envsubst < /app/docker-config/etc_sw360/authorization/application.yml.template > /etc/sw360/authorization/application.yml
/usr/bin/envsubst < /app/docker-config/etc_sw360/rest/application.yml.template > /etc/sw360/rest/application.yml
/usr/bin/envsubst < /app/docker-config/etc_sw360/sw360.properties.template > /etc/sw360/sw360.properties

# Wait for DB
test_for_couchdb() {
  curl -s $COUCHDB_URL/_up | grep -q '"status":"ok"'
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
$CATALINA_HOME/catalina.sh run

#!/bin/env bash
# SPDX-License-Identifier: EPL-2.0

set -o errexit -o nounset -o pipefail

COUCHDB_URL="${COUCHDB_URL:-http://couchdb:5984}"
COUCHDB_USER="${COUCHDB_USER:-admin}"
COUCHDB_PASSWORD="${COUCHDB_PASSWORD:-admin}"
COUCHDB_LUCENESEARCH_LIMIT="${COUCHDB_LUCENESEARCH_LIMIT:-1000}"
ENABLE_DISKSPACE="${ENABLE_DISKSPACE:-false}"
JWKS_ISSUER_URI="${JWKS_ISSUER_URI:-http://localhost:8080/authorization/oauth2/jwks}"
JWKS_SET_URI="${JWKS_SET_URI:-http://localhost:8080/authorization/oauth2/jwks}"
SVM_SW360_API_URL="${SVM_SW360_API_URL:-https://svmtest.cert.siemens.com}"
SVM_SW360_CERTIFICATE_PASSPHRASE="${SVM_SW360_CERTIFICATE_PASSPHRASE:-}"
SVM_SW360_JKS_PASSWORD="${SVM_SW360_JKS_PASSWORD:-}"
REST_APITOKEN_HASH_SALT="${REST_APITOKEN_HASH_SALT:-}"
JWKS_ISSUER="${JWKS_ISSUER:-http://localhost:8090}"

mkdir -p /etc/sw360/authorization /etc/sw360/rest

# Write configuration
envsubst < /app/docker-config/couchdb.properties.template | tee /etc/sw360/couchdb.properties
envsubst < /app/docker-config/etc_sw360/authorization/application.yml.template | tee /etc/sw360/authorization/application.yml
envsubst < /app/docker-config/etc_sw360/rest/application.yml.template | tee /etc/sw360/rest/application.yml
envsubst < /app/docker-config/etc_sw360/sw360.properties.template | tee /etc/sw360/sw360.properties

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

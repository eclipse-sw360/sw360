#!/usr/bin/env bash
# Copyright (c) Bosch Software Innovations GmbH 2019.
# Copyright (c) Siemens AG 2025.
# Part of the SW360 Portal Project.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Usage:
# addUnsafeDefaultClient.sh [-d] [-du] [--host http://127.0.0.1:5984] [--user admin] [--pass admin]
#
# if no argument is passed: create the default client(client id: "trusted-sw360-client" and client_secret: "sw360-secret") directly in the DB
# if the argument "-d" is passed: delete the default client if it is in the DB
#
# --host: the host of the CouchDB server (defaults to http://127.0.0.1:5984)
# --user: the user to authenticate with (defaults to admin)
# --pass: the password to authenticate with (defaults to admin)
#
# this needs the couchdb to be accessible on http://127.0.0.1:5984/ and the database sw360oauthclients has to be created.
# Extend the script to include functionality for adding a default user to CouchDB

set -e

TEMP=$(getopt -o du --long host:,user:,pass: \
              -n 'addUnsafeDefaultClient.sh' -- "$@")

# shellcheck disable=SC2181
if [ $? != 0 ] ; then echo "Terminating..." >&2 ; exit 1 ; fi

eval set -- "$TEMP"

D_COUNT=0
DELETE_CLIENT=false
DELETE_USER=false
HOST="http://127.0.0.1:5984"
USER="admin"
PASS="admin"
while true; do
  case "$1" in
    -d ) ((D_COUNT+=1)); shift ;;
    -u ) DELETE_USER=true; shift 2 ;;
    --host ) HOST="$2"; shift 2 ;;
    --user ) USER="$2"; shift 2 ;;
    --pass ) PASS="$2"; shift 2 ;;
    -- ) shift; break ;;
    * ) break ;;
  esac
done

## Set the DELETE_CLIENT and DELETE_USER based on input being -d or -du or -d -du
if [ $D_COUNT -gt 1 ]; then
    DELETE_CLIENT=true
elif [ $D_COUNT -eq 1 ] && [ "$DELETE_USER" == false ]; then
    DELETE_CLIENT=true
fi

# URLs for OAuth clients and users in the CouchDB database
AUTH_CLIENTS_URL="${HOST}/sw360oauthclients"
USERS_DB_URL="${HOST}/sw360users"
TRUSTED_CLIENT_URL="${AUTH_CLIENTS_URL}/trusted-sw360-client"
DEFAULT_USER_URL="${USERS_DB_URL}/admin-sw360-user"

# Function to get the revision ID of the default OAuth client
getDefaultAppRev() {
    if ! type jq &>/dev/null; then
        >&2 echo "This script needs the 'jq' tool, to parse JSON"
        exit 1
    fi

    curl --silent \
         --user "${USER}:${PASS}" \
         -X GET "$TRUSTED_CLIENT_URL" |
        jq -r '._rev'
}

# Function to create or update the default OAuth client
createDefaultApp() {
    curl --silent \
         --output /dev/null \
         --user "${USER}:${PASS}" \
         -X PUT "$TRUSTED_CLIENT_URL" \
         -d @- <<EOF
{
   "_id": "trusted-sw360-client",
   "authorities": [
       "BASIC"
   ],
   "secretRequired": true,
   "scoped": true,
   "description": "the default unsafe client",
   "scope": [
       "READ", "WRITE", "ADMIN", "openid"
   ],
   "client_id": "trusted-sw360-client",
   "client_secret": "\$2a\$10\$9kklCmCgQYKBdfesJzxCru92gC1oT/wSBhvnXuA7RXx7G70864wzu",
   "access_token_validity": 3600,
   "refresh_token_validity": 3600,
   "authorized_grant_types": [
       "refresh_token",
       "client_credentials",
       "authorization_code"
   ],
   "autoapprove": [
       "true"
   ],
   "resource_ids": [
       "sw360-REST-API"
   ],
   "redirect_uri": [
       "https://oauth.pstmn.io/v1/callback"
   ]
}
EOF
}

# Function to delete the default OAuth client
deleteDefaultApp() {
    curl --silent \
         --output /dev/null \
         --user "${USER}:${PASS}" \
         -X DELETE "${TRUSTED_CLIENT_URL}?rev=${oldRev}"
}

# Function to check if the default user exists
getDefaultUserRev() {
    curl --silent \
         --user "${USER}:${PASS}" \
         -X GET "$DEFAULT_USER_URL" | jq -r '._rev'
}

# Function to create the default user
createDefaultUser() {
  echo 'Default User'
  curl --silent \
       --output /dev/null \
       --user "${USER}:${PASS}" \
       -X PUT "$DEFAULT_USER_URL" \
       -d @- <<EOF
{
    "_id": "admin-sw360-user",
    "type": "user",
    "email": "admin@sw360.org",
    "userGroup": "ADMIN",
    "department": "DEPARTMENT",
    "fullname": "Admin Test",
    "givenname": "Admin",
    "lastname": "Test",
    "password":"\$2a\$10\$KcGk3lFG1JkS05sCt1TtaeLy11Xy8HNUkn7JvD2Nsqikhdqn8dLaq"
}
EOF
}

# Function to delete the default user
deleteDefaultUser() {
    curl --silent \
         --output /dev/null \
         --user "${USER}:${PASS}" \
         -X DELETE "${DEFAULT_USER_URL}?rev=${userRev}"
}

oldRev="$(getDefaultAppRev)"
userRev="$(getDefaultUserRev)"

# Create or delete the default OAuth client based on the input argument
if [[ "$oldRev" == "null" ]]; then
    if ! $DELETE_CLIENT; then
        createDefaultApp
        >&2 echo "The unsafe client with the name=[trusted-sw360-client] and the secret=[sw360-secret] was successfully created."
    else
        >&2 echo "The unsafe client is not in the DB, nothing to do."
    fi
else
    if $DELETE_CLIENT; then
        deleteDefaultApp
        >&2 echo "The unsafe client with the name=[trusted-sw360-client] was successfully removed."
    else
        >&2 echo "The unsafe client with the name=[trusted-sw360-client] and the secret=[sw360-secret] was already created previously."
    fi
fi

# Create or delete the default user based on the input argument
if [[ "$userRev" == "null" ]]; then
    if ! $DELETE_USER; then
        createDefaultUser
        >&2 echo  "The default user[admin@sw360.org] with password 12345 was successfully created."
    else
        >&2 echo  "The default user is not in the DB, nothing to do."
    fi
else
    if $DELETE_USER; then
        deleteDefaultUser
        >&2 echo "The default user is successfully removed."
    else
        >&2 echo  "The default user is already created previously."
    fi
fi

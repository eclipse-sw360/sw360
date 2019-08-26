#!/usr/bin/env bash
# Copyright (c) Bosch Software Innovations GmbH 2019.
# Part of the SW360 Portal Project.
#
# SPDX-License-Identifier: EPL-1.0
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html

# Usage:
# addUnsafeDefaultClient.sh [-d]
#
# if no argument is passed: create the default client directly in the DB
# if the argument "-d" is passed: delete the default client if it is in the DB
#
# this needs the couchdb to be accessible on http://127.0.0.1:5984/ and the database sw360oauthclients has to be created.

set -e

AUTH_CLIENTS_URL="http://127.0.0.1:5984/sw360oauthclients"
TRUSTED_CLIENT_URL="${AUTH_CLIENTS_URL}/trusted-sw360-client"

getDefaultAppRev() {
    if ! type jq &>/dev/null; then
        >&2 echo "This script needs the 'jq' tool, to parse JSON"
        exit 1
    fi

    curl --silent \
         -X GET "$TRUSTED_CLIENT_URL" |
        jq -r '._rev'
}

createDefaultApp() {
    curl --silent \
         --output /dev/null \
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
       "READ", "WRITE", "ADMIN"
   ],
   "client_id": "trusted-sw360-client",
   "client_secret": "sw360-secret",
   "access_token_validity": 3600,
   "refresh_token_validity": 3600,
   "authorized_grant_types": [
       "refresh_token",
       "client_credentials",
       "password"
   ],
   "autoapprove": [
       "true"
   ],
   "resource_ids": [
       "sw360-REST-API"
   ]
}
EOF
}

deleteDefaultApp() {
    curl --silent \
         --output /dev/null \
         -X DELETE "${TRUSTED_CLIENT_URL}?rev=${oldRev}"
}

oldRev="$(getDefaultAppRev)"

if [[ "$oldRev" == "null" ]]; then
    if [[ "$1" != "-d" ]]; then
        createDefaultApp
        >&2 echo "The unsafe client with the name=[trusted-sw360-client] and the secret=[sw360-secret] was successfully created."
    else
        >&2 echo "The unsafe client is not in the DB, nothing to do."
    fi
else
    if [[ "$1" == "-d" ]]; then
        deleteDefaultApp
        >&2 echo "The unsafe client with the name=[trusted-sw360-client] was successfully removed."
    else
        >&2 echo "The unsafe client with the name=[trusted-sw360-client] and the secret=[sw360-secret] was already created previously."
    fi
fi

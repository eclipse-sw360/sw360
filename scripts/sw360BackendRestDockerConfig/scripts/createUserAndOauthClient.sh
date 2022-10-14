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
# This script creates user and oauth client in DB.
# -----------------------------------------------------------------------------

COUCHDB_USER=${COUCHDB_USER:-admin}
COUCHDB_PASSWORD=${COUCHDB_PASSWORD:-password}
COUCHDB_HOST=${COUCHDB_HOST:-localhost}

create_user_oauth_client() {
    create_user_response=$(curl -H "Content-Type: application/json" -X PUT http://${COUCHDB_USER}:${COUCHDB_PASSWORD}@${COUCHDB_HOST}:5984/sw360users/affe4d6ad1c14018916dca301f263f5a -d @./scripts/sw360BackendRestDockerConfig/couchdb_documents/testClientUser.json);
    echo "$create_user_response"
    if [[ "$create_user_response" != *"\"ok\":true"* ]]; then
        echo "Error occured while creating user"
        exit 1;
    fi

    create_oauth_client_response=$(curl -H "Content-Type: application/json" -X PUT http://${COUCHDB_USER}:${COUCHDB_PASSWORD}@${COUCHDB_HOST}:5984/sw360oauthclients/e82d846d5cf00995f944651c23001f91 -d @./scripts/sw360BackendRestDockerConfig/couchdb_documents/oauthClient.json);
    echo "$create_oauth_client_response"
    if [[ "$create_oauth_client_response" != *"\"ok\":true"* ]]; then
        echo "Error occured while creating oauth client"
        exit 2;
    fi
}

create_user_oauth_client

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

create_user_oauth_client() {
    user_error = $(curl -H "Content-Type: application/json" -X PUT http://${COUCHDB_USER}:${COUCHDB_PASSWORD}@127.0.0.1:5984/sw360users/affe4d6ad1c14018916dca301f263f5a -d @./.github/sw360ClientConfig/couchdb_documents/testClientUser.json | python -c 'import json,sys;obj=json.load(sys.stdin); print obj.get("error")');
    if [[ "$user_error" != "None" ]]; then
        echo "Error occured while creating user"
        exit 1;
    fi

    oauth_client_error = $(curl -H "Content-Type: application/json" -X PUT http://${COUCHDB_USER}:${COUCHDB_PASSWORD}@127.0.0.1:5984/sw360oauthclients/e82d846d5cf00995f944651c23001f91 -d @./.github/sw360ClientConfig/couchdb_documents/oauthClient.json | python -c 'import json,sys;obj=json.load(sys.stdin); print obj.get("error")')
    if [[ "$oauth_client_error" != "None" ]]; then
        echo "Error occured while creating oauth client"
        exit 2;
    fi
}

create_user_oauth_client

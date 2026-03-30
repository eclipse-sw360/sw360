# Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0

"""
This script helps export OAuth clients from Liferay setup into new KeyCloak
setup using Terraform/OpenTofu.
The script expects tokens to have description in following format:
"<user_group>-<user_email>-<creator_email>-<creation_date>"
"""

import os
import re
import sys

# pylint: disable=import-error
from ibmcloudant.cloudant_v1 import CloudantV1
from ibm_cloud_sdk_core.authenticators import BasicAuthenticator


def fetch_documents():
    """Fetches all client documents from CouchDB."""
    # User can override these via environment variables if needed
    couch_url = os.environ.get("COUCHDB_URL", "http://localhost:5984")
    couch_user = os.environ.get("COUCHDB_USER", "admin")
    couch_pass = os.environ.get("COUCHDB_PASSWORD", "admin")

    authenticator = BasicAuthenticator(couch_user, couch_pass)
    service = CloudantV1(authenticator=authenticator)
    service.set_service_url(couch_url)

    db_name = "sw360oauthclients"

    print(f"Connecting to CouchDB at {couch_url} to fetch database: "
          f"{db_name}...", file=sys.stderr)

    try:
        response = service.post_all_docs(
            db=db_name, include_docs=True
        ).get_result()
        return response.get('rows', [])
    except Exception as err: # pylint: disable=broad-exception-caught
        print(f"Error fetching docs from CouchDB: {err}", file=sys.stderr)
        sys.exit(1)


def process_documents(rows):
    """Parses raw CouchDB rows and returns deduplicated list of clients."""
    clients_by_email = {}
    pattern = re.compile(
        r'^([A-Z0-9-]+)-([a-z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-z]{2,})-'
        r'([a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,})-(\d{4}-?\d{2}-?\d{2})'
        r'(?:-.*)?$')

    for row in rows:
        doc = row.get('doc', {})
        description = doc.get("description", "")
        if not description:
            continue

        match = pattern.match(description)
        if not match:
            print(f"Skipping doc {doc.get('_id')} - unrecognized description "
                  f"format: {description}", file=sys.stderr)
            continue

        creation_date = match.group(4).replace("-", "")
        user_email = match.group(2)

        client_info = {
            "user_email": user_email,
            "creator_email": match.group(3),
            "user_group": match.group(1),
            "creation_date": creation_date,
            "client_id": doc.get("client_id", ""),
            "client_secret": doc.get("client_secret", ""),
            "is_write": "WRITE" in doc.get("scope", [])
        }

        existing = clients_by_email.get(user_email)
        if not existing or existing['creation_date'] < creation_date:
            clients_by_email[user_email] = client_info

    return list(clients_by_email.values())


def format_client(client):
    """Formats a client dictionary into Terraform HCL snippet."""
    return f"""    {{
      user_email    = "{client['user_email']}"
      creator_email = "{client['creator_email']}"
      user_group    = "{client['user_group']}"
      creation_date = "{client['creation_date']}"
      client_id     = "{client['client_id']}"
      client_secret = "{client['client_secret']}"
    }}"""


def main():
    """Main function to export clients."""
    rows = fetch_documents()
    clients = process_documents(rows)

    # Sort deterministically by email for output
    clients = sorted(clients, key=lambda x: x["user_email"])

    read_clients = []
    write_clients = []

    for client_item in clients:
        is_write = client_item.pop("is_write")
        if is_write:
            write_clients.append(client_item)
        else:
            read_clients.append(client_item)

    # Generate the Terraform snippet
    print("locals {")
    print("  sw360_read_clients = [")
    if read_clients:
        print(",\n".join(format_client(client_item) for client_item in read_clients))
    print("  ]")
    print()
    print("  sw360_write_clients = [")
    if write_clients:
        print(",\n".join(format_client(client_item) for client_item in write_clients))
    print("  ]")
    print()
    print("  sw360_clients = concat(local.sw360_read_clients,"
          " local.sw360_write_clients)")
    print("}")


if __name__ == "__main__":
    main()

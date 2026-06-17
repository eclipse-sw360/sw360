#!/usr/bin/env python3
# pylint: disable=invalid-name
# (Module name starts with a digit by SW360 migration-script convention;
#  see scripts/migrations/README.md.)
# -----------------------------------------------------------------------------
# Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
# -----------------------------------------------------------------------------
"""Backfill ``owner_email`` on existing OAuth client documents in the
``sw360oauthclients`` database.

Context: starting with version 20.1.0, the auth server's
``/client-management`` controller persists an ``owner_email`` field on every
OAuth client doc it creates and mirrors the ``client_id`` into that user's
``oidcClientInfos`` map (database ``sw360users``). The resource server's
``Sw360JWTAccessTokenConverter`` then resolves the acting SW360 user via the
``client_id`` claim for ``client_credentials`` tokens.

Clients created before 20.1.0 have no ``owner_email``. This script tries to
infer it by querying ``sw360users`` for a user whose ``oidcClientInfos``
already contains the ``client_id`` (Mango selector
``oidcClientInfos.<clientId> $exists``). When exactly one match is found the
``owner_email`` is set on the OAuth client doc. Bootstrap clients that
service interactive flows only (e.g. ``trusted-sw360-client`` consumed by
the ``sw360oauth`` / ``oauth-password-grant`` frontend providers) are
skipped: they have ``authorization_code`` in ``authorized_grant_types``, do
not need an owner mirror, and must remain untouched.

Implementation notes:
 * Uses the IBM Cloudant Python SDK (``ibmcloudant``); the legacy
   ``python-couchdb`` package is unmaintained.
 * Updates are flushed in batches of ``BATCH_SIZE`` via ``post_bulk_docs``
   so each round-trip handles up to 50 docs at a time.

Configure ``DRY_RUN`` below (or via env var ``DRY_RUN=true/false``) to
preview without writing.
"""

import os
import sys
from datetime import datetime

try:
    from ibmcloudant.cloudant_v1 import CloudantV1
    from ibm_cloud_sdk_core.authenticators import (
        BasicAuthenticator,
        NoAuthAuthenticator,
    )
except ImportError:
    print("Missing dependency: pip install 'ibmcloudant'")
    sys.exit(1)

# ---------------------------------------
# constants
# ---------------------------------------

DRY_RUN = os.environ.get("DRY_RUN", "true").lower() in ("1", "true", "yes")
COUCHSERVER = os.environ.get("COUCHDB_URL", "http://localhost:5984")
COUCHDB_USER = os.environ.get("COUCHDB_USER")
COUCHDB_PASSWORD = os.environ.get("COUCHDB_PASSWORD")

CLIENTS_DBNAME = "sw360oauthclients"
USERS_DBNAME = "sw360users"

BATCH_SIZE = 50

# Bootstrap clients are intentionally left without owner_email so they can
# continue to service authorization_code / password flows where the JWT
# carries the resource owner's email.
SKIP_CLIENT_IDS = {"trusted-sw360-client"}


# ---------------------------------------
# helpers
# ---------------------------------------


def make_client():
    """Construct and return a configured :class:`CloudantV1` client.

    Authentication credentials, the CouchDB URL, and SSL behaviour are
    read from the ``COUCHDB_USER`` / ``COUCHDB_PASSWORD`` / ``COUCHDB_URL``
    / ``COUCHDB_DISABLE_SSL_VERIFICATION`` environment variables.
    """
    if COUCHDB_USER and COUCHDB_PASSWORD:
        authenticator = BasicAuthenticator(COUCHDB_USER, COUCHDB_PASSWORD)
    else:
        authenticator = NoAuthAuthenticator()
    client = CloudantV1(authenticator=authenticator)
    client.set_service_url(COUCHSERVER)
    if os.environ.get("COUCHDB_DISABLE_SSL_VERIFICATION", "").lower() in ("1", "true", "yes"):
        client.set_disable_ssl_verification(True)
    return client


def is_interactive_only_client(doc):
    """Return True when the OAuth client doc supports ``authorization_code``.

    Such clients service interactive logins (Postman, the ``sw360oauth`` /
    ``oauth-password-grant`` frontend providers) and intentionally have no
    ``owner_email`` — the JWT carries the real human's email instead.
    """
    grants = doc.get("authorized_grant_types") or []
    if isinstance(grants, str):
        grants = [grants]
    return "authorization_code" in grants


def list_oauth_clients(client):
    """Return all OAuth client docs (skipping CouchDB design docs)."""
    response = client.post_all_docs(db=CLIENTS_DBNAME, include_docs=True).get_result()
    docs = []
    for row in response.get("rows", []):
        doc = row.get("doc")
        if not doc:
            continue
        if doc.get("_id", "").startswith("_design/"):
            continue
        docs.append(doc)
    return docs


def find_owner_email(client, client_id):
    """Return the email of the user whose oidcClientInfos contains client_id,
    or None when zero/multiple matches.

    Uses a Mango selector against the `sw360users` database. limit=2 is
    enough to disambiguate "exactly one" from "more than one"."""
    selector = {f"oidcClientInfos.{client_id}": {"$exists": True}}
    response = client.post_find(
        db=USERS_DBNAME,
        selector=selector,
        fields=["email"],
        limit=2,
    ).get_result()
    docs = response.get("docs") or []
    if len(docs) != 1:
        return None
    return docs[0].get("email")


def flush_batch(client, batch, stats):
    """Send a batch of updated docs via post_bulk_docs and tally the result."""
    if not batch:
        return
    if DRY_RUN:
        for doc in batch:
            print(f"  DRY    would write {doc['_id']} -> owner_email={doc['owner_email']}")
        stats["pending_dry_run"] += len(batch)
        batch.clear()
        return
    response = client.post_bulk_docs(
        db=CLIENTS_DBNAME,
        bulk_docs={"docs": batch},
    ).get_result()
    # Response is a list of {id, rev?} on success or {id, error, reason}
    # on conflict / failure.
    for entry in response:
        if entry.get("error"):
            stats["failed"] += 1
            print(
                f"  ERROR  {entry.get('id')}: "
                f"{entry.get('error')} - {entry.get('reason')}"
            )
        else:
            stats["updated"] += 1
            print(f"  WROTE  {entry.get('id')} -> rev={entry.get('rev')}")
    batch.clear()


# ---------------------------------------
# main
# ---------------------------------------


def main():
    """Entry point: scan ``sw360oauthclients`` and backfill ``owner_email``."""
    print(
        f"[{datetime.now().isoformat()}] "
        f"server={COUCHSERVER} dry_run={DRY_RUN} batch_size={BATCH_SIZE}"
    )
    try:
        client = make_client()
    except Exception as e:  # pylint: disable=broad-exception-caught
        # Cloudant SDK raises a variety of unrelated exception types here
        # (network errors, auth failures, missing TLS bundle, ...); a single
        # catch keeps the CLI failure path uniform.
        print(f"Error connecting to CouchDB: {e}")
        sys.exit(1)

    stats = {
        "updated": 0,
        "failed": 0,
        "pending_dry_run": 0,
        "already_set": 0,
        "bootstrap": 0,
        "no_match": 0,
    }
    batch = []

    for doc in list_oauth_clients(client):
        client_id = doc.get("client_id") or doc.get("_id")
        if not client_id:
            continue

        if doc.get("owner_email"):
            stats["already_set"] += 1
            continue
        if client_id in SKIP_CLIENT_IDS or is_interactive_only_client(doc):
            stats["bootstrap"] += 1
            print(f"  SKIP   bootstrap/interactive client: {client_id}")
            continue

        owner = find_owner_email(client, client_id)
        if not owner:
            stats["no_match"] += 1
            print(
                f"  SKIP   no unique owner found for {client_id}; "
                f"admin must set owner_email manually (PUT the doc) "
                f"or recreate the client via /client-management"
            )
            continue

        doc["owner_email"] = owner
        batch.append(doc)
        if len(batch) >= BATCH_SIZE:
            flush_batch(client, batch, stats)

    # final flush
    flush_batch(client, batch, stats)

    print()
    print(
        f"[summary] updated={stats['updated']} "
        f"failed={stats['failed']} "
        f"pending_dry_run={stats['pending_dry_run']} "
        f"already_set={stats['already_set']} "
        f"bootstrap={stats['bootstrap']} "
        f"no_match={stats['no_match']} "
        f"dry_run={DRY_RUN}"
    )
    if stats["failed"] > 0:
        sys.exit(2)


if __name__ == "__main__":
    main()

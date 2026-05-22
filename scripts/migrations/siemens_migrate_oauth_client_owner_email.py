#!/usr/bin/env python3
# -----------------------------------------------------------------------------
# Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
# -----------------------------------------------------------------------------
"""Siemens-specific backfill of ``owner_email`` on OAuth client docs in the
``sw360oauthclients`` database **and** of the matching ``oidcClientInfos``
map entries on the owner's user doc in ``sw360users``.

This is a stricter variant of ``063_migrate_oauth_client_owner_email.py``
tailored to the Siemens-internal SW360 deployment, where every legacy
OAuth client doc has its ``description`` field set to::

    <user_group>-<user_email>-<creator_email>-<yyyy-mm-dd>[-<anything>]

(See ``sw360-keycloak-tf/export_clients.py`` for the same convention used
by the Keycloak Terraform pipeline.) The ``user_email`` field is the
authoritative owner — i.e. the SW360 user on whose behalf the
``client_credentials`` token will be minted — and is the value we need
to copy into ``owner_email`` so that the v20 resource server's
``Sw360JWTAccessTokenConverter`` can resolve the acting SW360 user from
the ``client_id`` claim.

In parallel, every migrated client is also registered on the owner's
``sw360users`` document under ``oidcClientInfos``::

    "oidcClientInfos": {
        "<client_id>": {
            "name":   "<description>",
            "access": "READ" | "WRITE" | "READ_WRITE"
        },
        ...
    }

The ``access`` value is computed from the client's ``scope`` array:
``READ`` + ``WRITE`` → ``READ_WRITE``; ``WRITE`` alone → ``WRITE``;
anything else (including the default-only client) → ``READ``.

Compared with 063 this script:
 * Does **not** query ``sw360users.oidcClientInfos.<clientId>`` to
   *discover* the owner. The description regex is the single source of
   truth and is unambiguous for Siemens-issued clients.
 * Writes ``oidcClientInfos`` entries back on the user side, so the
   frontend's "my tokens" view sees the legacy clients without any
   further migration.
 * Reports every document it could **not** migrate (no description,
   malformed description, missing user, bootstrap client) so the
   operator can fix the remainder manually with ``curl`` /
   ``/client-management`` PUTs.

Behaviour shared with 063:
 * Bootstrap / interactive-only clients (``authorized_grant_types``
   containing ``authorization_code``, plus the hard-coded
   ``trusted-sw360-client``) are left untouched on purpose: the JWT
   carries the resource owner's email for those flows.
 * ``DRY_RUN`` (default ``true``) previews changes without writing.
 * Writes are flushed in batches of ``BATCH_SIZE`` via
   ``post_bulk_docs`` (separately for each database).
 * Already-populated ``owner_email`` / ``oidcClientInfos[client_id]``
   are never overwritten with a different value.

Configure via env vars: ``DRY_RUN`` (``true``/``false``), ``COUCHDB_URL``,
``COUCHDB_USER``, ``COUCHDB_PASSWORD``, ``COUCHDB_DISABLE_SSL_VERIFICATION``.
"""

import os
import re
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

# Siemens description format:
#   <user_group>-<user_email>-<creator_email>-<yyyy[-]mm[-]dd>[-<anything>]
# Identical to sw360-keycloak-tf/export_clients.py so the two stay in
# lock-step.
DESCRIPTION_PATTERN = re.compile(
    r'^([A-Z0-9-]+)-([a-z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-z]{2,})-'
    r'([a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,})-(\d{4}-?\d{2}-?\d{2})'
    r'(?:-.*)?$'
)


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


def parse_owner_email(description):
    """Return ``user_email`` parsed from the Siemens description format,
    or ``None`` when the description is empty or doesn't match the
    expected layout.
    """
    if not description:
        return None
    match = DESCRIPTION_PATTERN.match(description)
    if not match:
        return None
    return match.group(2)


def compute_access(scope):
    """Translate the OAuth client ``scope`` array to the ``access`` token
    used by ``oidcClientInfos`` entries on the SW360 user doc.

    Mapping:
     * READ + WRITE → ``READ_WRITE``
     * WRITE only   → ``WRITE``
     * everything else (including missing/empty scope, READ-only) → ``READ``
    """
    if not scope:
        return "READ"
    if isinstance(scope, str):
        scope = [scope]
    normalised = {str(s).upper() for s in scope}
    has_read = "READ" in normalised
    has_write = "WRITE" in normalised
    if has_read and has_write:
        return "READ_WRITE"
    if has_write:
        return "WRITE"
    return "READ"


def find_user_by_email(client, email):
    """Return the full user doc whose ``email`` field matches ``email``,
    or ``None`` when zero/multiple matches.

    Uses a Mango selector against the ``sw360users`` database. ``limit=2``
    is enough to disambiguate "exactly one" from "more than one".
    """
    selector = {"email": email, "type": "user"}
    response = client.post_find(
        db=USERS_DBNAME,
        selector=selector,
        limit=2,
    ).get_result()
    docs = response.get("docs") or []
    if len(docs) != 1:
        return None
    return docs[0]


def upsert_oidc_client_info(user_doc, client_id, name, access):
    """Add or update ``oidcClientInfos[client_id]`` on the user doc in
    place. Return ``True`` when the user doc was actually changed,
    ``False`` when it already carried the same entry.
    """
    infos = user_doc.get("oidcClientInfos")
    if not isinstance(infos, dict):
        infos = {}
        user_doc["oidcClientInfos"] = infos

    existing = infos.get(client_id)
    desired = {"name": name, "access": access}
    if isinstance(existing, dict) and existing.get("name") == name and existing.get("access") == access:
        return False
    infos[client_id] = desired
    return True


def flush_batch(client, db, batch, dry_run_label, stats_key_updated, stats_key_failed, stats):
    """Send a batch of updated docs to ``db`` via ``post_bulk_docs`` and
    tally the result. ``dry_run_label`` is the human-readable tag used
    in DRY_RUN preview lines (e.g. ``owner_email`` or ``oidcClientInfos``).
    """
    if not batch:
        return
    if DRY_RUN:
        for doc in batch:
            print(f"  DRY    would write {db}:{doc['_id']} [{dry_run_label}]")
        stats["pending_dry_run"] += len(batch)
        batch.clear()
        return
    response = client.post_bulk_docs(
        db=db,
        bulk_docs={"docs": batch},
    ).get_result()
    # Response is a list of {id, rev?} on success or {id, error, reason}
    # on conflict / failure.
    for entry in response:
        if entry.get("error"):
            stats[stats_key_failed] += 1
            print(
                f"  ERROR  {db}:{entry.get('id')}: "
                f"{entry.get('error')} - {entry.get('reason')}"
            )
        else:
            stats[stats_key_updated] += 1
            print(f"  WROTE  {db}:{entry.get('id')} -> rev={entry.get('rev')}")
    batch.clear()


def print_pending_report(pending):
    """Print a human-readable list of every doc that the script could not
    migrate, grouped by reason. Operators use this list to fix the
    remainder by hand (e.g. ``curl -X PUT`` against
    ``/client-management/<client_id>`` or a direct user-doc edit) before
    going live.
    """
    if not pending:
        return
    print()
    print("=" * 72)
    print("PENDING — manual action required for the following client docs:")
    print("=" * 72)
    by_reason = {}
    for entry in pending:
        by_reason.setdefault(entry["reason"], []).append(entry)
    for reason, entries in sorted(by_reason.items()):
        print()
        print(f"-- {reason} ({len(entries)}) --")
        for entry in entries:
            description = entry.get("description") or "<empty>"
            # Truncate long descriptions to keep the report readable.
            if len(description) > 100:
                description = description[:97] + "..."
            extras = ""
            if entry.get("owner_email"):
                extras = f"  owner_email={entry['owner_email']}"
            print(
                f"  _id={entry['_id']}  client_id={entry.get('client_id') or '<none>'}"
                f"  description={description!r}{extras}"
            )


# ---------------------------------------
# main
# ---------------------------------------


def main():
    """Entry point: scan ``sw360oauthclients``, backfill ``owner_email``,
    and mirror each client into the owner's ``oidcClientInfos`` map on
    the matching ``sw360users`` document.
    """
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
        "no_description": 0,
        "unparseable_description": 0,
        "user_updated": 0,
        "user_failed": 0,
        "user_already_linked": 0,
        "user_not_found": 0,
    }
    client_batch = []
    pending = []

    # Cache user docs by email so we can coalesce multiple client updates
    # for the same owner into a single bulk_docs entry, and avoid stale
    # _rev conflicts. The cache stores the live (possibly-mutated) user
    # doc; we serialise it into the batch only at flush time.
    users_cache = {}
    users_changed_emails = set()

    for doc in list_oauth_clients(client):
        client_id = doc.get("client_id") or doc.get("_id")
        if not client_id:
            # No client_id at all — not an OAuth client doc. Report it
            # so the operator can decide whether to delete it.
            pending.append({
                "_id": doc.get("_id"),
                "client_id": None,
                "description": doc.get("description"),
                "reason": "no client_id — not an OAuth client doc",
            })
            continue

        if doc.get("owner_email"):
            stats["already_set"] += 1
            continue
        if client_id in SKIP_CLIENT_IDS or is_interactive_only_client(doc):
            stats["bootstrap"] += 1
            print(f"  SKIP   bootstrap/interactive client: {client_id}")
            continue

        description = doc.get("description")
        if not description:
            stats["no_description"] += 1
            pending.append({
                "_id": doc.get("_id"),
                "client_id": client_id,
                "description": description,
                "reason": "no description — cannot infer owner_email",
            })
            continue

        owner_email = parse_owner_email(description)
        if not owner_email:
            stats["unparseable_description"] += 1
            pending.append({
                "_id": doc.get("_id"),
                "client_id": client_id,
                "description": description,
                "reason": "description does not match Siemens "
                          "<group>-<user>-<creator>-<date> pattern",
            })
            continue

        # Look up the user doc (cached) so we can mirror the client into
        # their oidcClientInfos. We do this *before* queueing the oauth
        # client doc write so that a missing user surfaces as a single
        # pending entry without corrupting the client side.
        user_doc = users_cache.get(owner_email)
        if user_doc is None and owner_email not in users_cache:
            user_doc = find_user_by_email(client, owner_email)
            users_cache[owner_email] = user_doc  # may be None — cache the miss too

        if user_doc is None:
            stats["user_not_found"] += 1
            pending.append({
                "_id": doc.get("_id"),
                "client_id": client_id,
                "description": description,
                "owner_email": owner_email,
                "reason": "owner user not found in sw360users — create the "
                          "user first or fix the description, then re-run",
            })
            continue

        access = compute_access(doc.get("scope"))
        if upsert_oidc_client_info(user_doc, client_id, description, access):
            users_changed_emails.add(owner_email)
        else:
            stats["user_already_linked"] += 1

        doc["owner_email"] = owner_email
        client_batch.append(doc)
        if len(client_batch) >= BATCH_SIZE:
            flush_batch(
                client, CLIENTS_DBNAME, client_batch,
                "owner_email", "updated", "failed", stats,
            )

    # Final flush for OAuth client docs.
    flush_batch(
        client, CLIENTS_DBNAME, client_batch,
        "owner_email", "updated", "failed", stats,
    )

    # Build and flush the user-side batch. Each user appears at most once
    # in this list, with all of their newly-mirrored clients merged into
    # a single doc revision.
    user_batch = []
    for email in users_changed_emails:
        user_doc = users_cache.get(email)
        if user_doc is None:
            continue
        user_batch.append(user_doc)
        if len(user_batch) >= BATCH_SIZE:
            flush_batch(
                client, USERS_DBNAME, user_batch,
                "oidcClientInfos", "user_updated", "user_failed", stats,
            )
    flush_batch(
        client, USERS_DBNAME, user_batch,
        "oidcClientInfos", "user_updated", "user_failed", stats,
    )

    # Report manual-action list.
    print_pending_report(pending)

    print()
    print(
        f"[summary] "
        f"client_updated={stats['updated']} "
        f"client_failed={stats['failed']} "
        f"user_updated={stats['user_updated']} "
        f"user_failed={stats['user_failed']} "
        f"user_already_linked={stats['user_already_linked']} "
        f"user_not_found={stats['user_not_found']} "
        f"pending_dry_run={stats['pending_dry_run']} "
        f"already_set={stats['already_set']} "
        f"bootstrap={stats['bootstrap']} "
        f"no_description={stats['no_description']} "
        f"unparseable_description={stats['unparseable_description']} "
        f"manual_action_required={len(pending)} "
        f"dry_run={DRY_RUN}"
    )
    if stats["failed"] > 0 or stats["user_failed"] > 0:
        sys.exit(2)


if __name__ == "__main__":
    main()

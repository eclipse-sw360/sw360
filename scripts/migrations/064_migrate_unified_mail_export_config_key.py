#!/usr/bin/python
# -----------------------------------------------------------------------------
# Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# This is a manual database migration script. It is assumed that a
# dedicated framework for automatic migration will be written in the
# future. When that happens, this script should be refactored to conform
# to the framework's prerequisites to be run by the framework. For
# example, server address and db name should be parameterized, the code
# reorganized into a single class or function, etc.
#
# This script migrates the two separate mail export configuration keys:
#   send.project.spreadsheet.export.to.mail.enabled
#   send.component.spreadsheet.export.to.mail.enabled
# into a single unified key:
#   send.export.to.mail.enabled
# in the SW360_CONFIGURATION document stored in CouchDB.
#
# Document structure (actual):
#   {
#     "_id": "...",
#     "configFor": "SW360_CONFIGURATION",
#     "configKeyToValues": {
#       "send.project.spreadsheet.export.to.mail.enabled": ["false"],
#       "send.component.spreadsheet.export.to.mail.enabled": ["false"],
#       ...
#     }
#   }
# -----------------------------------------------------------------------------

import couchdb
import getpass
import json
import time
import sys
from datetime import datetime

# ---------------------------------------
# constants
# ---------------------------------------

DRY_RUN = False
DBNAME = 'sw360config'

OLD_KEY_PROJECT   = "send.project.spreadsheet.export.to.mail.enabled"
OLD_KEY_COMPONENT = "send.component.spreadsheet.export.to.mail.enabled"
NEW_KEY           = "send.export.to.mail.enabled"

# The document is identified by the configFor field, not a type field
CONFIG_FOR_VALUE  = "SW360_CONFIGURATION"
# Field name in the document that holds the configuration key-value pairs
CONFIG_FIELD      = "configKeyToValues"

# ---------------------------------------
# database connection
# ---------------------------------------

print("--- CouchDB Connection Setup ---")
couchdb_host = input("Enter CouchDB host [default: http://localhost:5984]: ").strip() or "http://localhost:5984"
print(f"  Host     : {couchdb_host}")

couchdb_user = input("Enter CouchDB username: ").strip()
print(f"  Username : {couchdb_user}")

couchdb_pass = getpass.getpass("Enter CouchDB password: ")
print("  Password : ****")
print("--------------------------------")

scheme, rest = couchdb_host.rstrip("/").split("://", 1)
COUCHSERVER = f"{scheme}://{couchdb_user}:{couchdb_pass}@{rest}/"

try:
    couch = couchdb.Server(COUCHSERVER)
    db = couch[DBNAME]
except Exception as e:
    print(f"Error connecting to CouchDB: {e}")
    sys.exit(1)

# ---------------------------------------
# functions
# ---------------------------------------

def create_log_entry(doc_id, status, error=None):
    return {
        "timestamp": datetime.now().isoformat(),
        "docId": doc_id,
        "status": status,
        "error": str(error) if error else None,
    }


def migrate_mail_export_config_key():
    log = {
        "migrationDate": datetime.now().isoformat(),
        "dryRun": DRY_RUN,
        "statistics": {
            "totalProcessed": 0,
            "successCount": 0,
            "errorCount": 0,
            "skippedCount": 0
        },
        "entries": []
    }

    try:
        print('Fetching SW360 configuration documents...')

        # Query by configFor field (actual document structure uses configFor, not type)
        query = {
            "selector": {
                "configFor": {"$eq": CONFIG_FOR_VALUE}
            },
            "limit": 99999
        }
        config_docs = list(db.find(query))
        total_docs = len(config_docs)
        print(f'Found {total_docs} configuration document(s) to process')

        for doc in config_docs:
            doc_id = doc.get('_id', 'unknown')
            try:
                # Values are stored in configKeyToValues, not configMap
                config_map = doc.get(CONFIG_FIELD, {})

                project_val   = config_map.get(OLD_KEY_PROJECT)   # list or None
                component_val = config_map.get(OLD_KEY_COMPONENT) # list or None

                # Skip if neither old key is present
                if project_val is None and component_val is None:
                    print(f'Skipping document {doc_id}: old keys not present')
                    log["statistics"]["skippedCount"] += 1
                    log["entries"].append(create_log_entry(doc_id, "SKIPPED"))
                    continue

                # Values are arrays (e.g. ["false"]); use the first non-None list as the new value
                new_val = project_val if project_val is not None else component_val

                # Ensure the value is stored as a list (preserve array structure)
                if not isinstance(new_val, list):
                    new_val = [new_val]

                print(f'Processing document: {doc_id}')
                print(f'  [SET]    \'{NEW_KEY}\' = {new_val}')

                # Write new unified key (as array, matching the existing document format)
                config_map[NEW_KEY] = new_val

                # Remove old keys
                for old_key in (OLD_KEY_PROJECT, OLD_KEY_COMPONENT):
                    if old_key in config_map:
                        del config_map[old_key]
                        print(f'  [REMOVE] \'{old_key}\'')

                doc[CONFIG_FIELD] = config_map

                if not DRY_RUN:
                    db.save(doc)

                log["statistics"]["successCount"] += 1
                log["entries"].append(create_log_entry(doc_id, "SUCCESS"))

            except Exception as e:
                log["statistics"]["errorCount"] += 1
                print(f"Error processing document {doc_id}: {str(e)}")
                log["entries"].append(create_log_entry(doc_id, "ERROR", e))

            log["statistics"]["totalProcessed"] += 1

    except Exception as e:
        print(f"Fatal error during migration: {str(e)}")
        log["fatalError"] = str(e)
    finally:
        try:
            with open('migrate_unified_mail_export_config_key.log', 'w') as log_file:
                json.dump(log, log_file, indent=2)
        except Exception as e:
            print(f"Error writing log file: {str(e)}")

        print('\n------------------------------------------')
        print(f"\nMigration Summary:")
        print(f"Total processed      : {log['statistics']['totalProcessed']}")
        print(f"Successfully updated : {log['statistics']['successCount']}")
        print(f"Skipped              : {log['statistics']['skippedCount']}")
        print(f"Errors               : {log['statistics']['errorCount']}")
        print(f"DryRun               : {DRY_RUN}")
        print('\n------------------------------------------')


if __name__ == "__main__":
    start_time = time.time()
    migrate_mail_export_config_key()
    print(f'\nMigration completed in {time.time() - start_time:.2f}s')

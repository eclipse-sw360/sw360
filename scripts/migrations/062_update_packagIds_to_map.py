#!/usr/bin/python
# -----------------------------------------------------------------------------
# Copyright Siemens AG, 2025. Part of the SW360 Portal Project.
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
# This script is to change the packageIds from set to  map and add comment,createdBy,createdOn  fields to it.
# ---------------------------------------------------------------------------------------------------------------------------------------------------------------

#!/usr/bin/python
import couchdb
import json
import time
import sys
from datetime import datetime

# ---------------------------------------
# constants
# ---------------------------------------

DRY_RUN = False
COUCHSERVER = "http://localhost:5984/"
DBNAME = 'sw360db'
BATCH_SIZE = 1000

# ---------------------------------------
# database connection
# ---------------------------------------

try:
    couch = couchdb.Server(COUCHSERVER)
    db = couch[DBNAME]
except Exception as e:
    print(f"Error connecting to CouchDB: {e}")
    sys.exit(1)


# ---------------------------------------
# functions
# ---------------------------------------

def create_log_entry(project, status, error=None):
    return {
        "timestamp": datetime.now().isoformat(),
        "status": status,
        "error": str(error) if error else None,
    }

def update_package_ids_structure():
    log = {
        "migrationDate": datetime.now().isoformat(),
        "dryRun": DRY_RUN,
        "statistics": {
            "totalProcessed": 0,
            "successCount": 0,
            "errorCount": 0
        },
        "entries": [],
        "project_details": [],
        "project_id_name": []
    }

    try:
        print('Fetching all projects with packageIds...')

        # Fetch all projects at once
        query = {
            "selector": {
                "type": {"$eq": "project"},
                "packageIds": {"$exists": True}
            },
            "limit": 99999
        }
        all_projects = list(db.find(query))
        total_projects = len(all_projects)
        print(f'Found {total_projects} projects to process')

        # Process projects in batches from the stored list
        for i in range(0, total_projects, BATCH_SIZE):
            batch_projects = all_projects[i:i + BATCH_SIZE]
            print(f'\nProcessing batch of {len(batch_projects)} projects (offset: {i})')

            for project in batch_projects:
                try:
                    project_id = project.get('_id', 'unknown')
                    project_name = project.get('name', 'unknown')

                    # Store original project state
                    original_project = dict(project)

                    # Transform packageIds list into a map
                    package_ids = project['packageIds']
                    package_ids_map = {}
                    for package_id in package_ids:
                        package_ids_map[package_id] = {
                            "comment": "",
                            "createdBy": project.get("createdBy"),
                            "createdOn": project.get("createdOn", time.strftime("%Y-%m-%d"))
                        }

                    # Update the project document
                    project['packageIds'] = package_ids_map
                    log["project_details"].append(project)
                    print(f'Processing project: {project_name} ({project_id})')
                    log["project_id_name"].append(f'Processing project: {project_name} ({project_id})')

                    if not DRY_RUN:
                        db.save(project)

                    log["statistics"]["successCount"] += 1
                    log["entries"].append(create_log_entry(original_project, "SUCCESS"))

                except Exception as e:
                    log["statistics"]["errorCount"] += 1
                    print(f"Error processing project {project_id}: {str(e)}")
                    log["entries"].append(create_log_entry(project, "ERROR", e))

            log["statistics"]["totalProcessed"] += len(batch_projects)
            print(f'Completed {log["statistics"]["totalProcessed"]} of {total_projects} projects')

    except Exception as e:
        print(f"Fatal error during migration: {str(e)}")
        log["fatalError"] = str(e)
    finally:
        # Write log file
        try:
            with open('update_package_ids_migration.log', 'w') as log_file:
                json.dump(log, log_file, indent=2)
        except Exception as e:
            print(f"Error writing log file: {str(e)}")

        print('\n------------------------------------------')
        print(f"\nMigration Summary:")
        print(f"Total processed: {log['statistics']['totalProcessed']}")
        print(f"Successfully updated: {log['statistics']['successCount']}")
        print(f"Errors: {log['statistics']['errorCount']}")
        print(f"DryRun: {DRY_RUN}")
        print('\n------------------------------------------')

if __name__ == "__main__":
    start_time = time.time()
    update_package_ids_structure()
    print(f'\nMigration completed in {time.time() - start_time:.2f}s')

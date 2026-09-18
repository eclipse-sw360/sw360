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

"""Move SVM mapping from additionalData to externalIds for selected projects."""

import fnmatch
import json
import re
import time

import couchdb


# ---------------------------------------
# constants (same style as other scripts in this repo)
# ---------------------------------------

DRY_RUN = True

COUCHUSERNAME = "****"
COUCHPWD = "*****"
COUCHSERVER = "http://" + COUCHUSERNAME + ":" + COUCHPWD + "@localhost:5984/"
DBNAME = 'sw360db'

PROJECT_STATE = 'ACTIVE'
CLEARING_STATES = ['OPEN', 'IN_PROGRESS', 'CLOSED']
TAG_PATTERN = '*SI EA R&D*'
SVM_KEY_REGEX = r'svm'
LIMIT = 200000
LOG_FILE = '065_move_svm_mapping_additionaldata_to_externalids.log'

couch = couchdb.Server(COUCHSERVER)
db = couch[DBNAME]

# ----------------------------------------
# queries
# ----------------------------------------

projects_all_query = {
    "selector": {
        "type": {"$eq": "project"},
        "state": {"$eq": PROJECT_STATE},
        "clearingState": {"$in": CLEARING_STATES}
    },
    "limit": LIMIT
}


def tag_matches(tag_value, wildcard_pattern):
    if not wildcard_pattern:
        return True
    if tag_value is None:
        return False
    return fnmatch.fnmatch(str(tag_value).lower(), wildcard_pattern.lower())


def find_svm_keys(additional_data, key_regex):
    matches = []
    for key, value in additional_data.items():
        if key is None:
            continue
        if key_regex.search(str(key)):
            matches.append((key, value))
    return matches


def get_case_insensitive_key_value(data, target_key):
    if not isinstance(data, dict):
        return None, None
    for key, value in data.items():
        if key is None:
            continue
        if str(key).lower() == target_key.lower():
            return key, value
    return None, None


def migrate_project(project, key_regex):
    additional_data = project.get("additionalData")
    if not isinstance(additional_data, dict) or not additional_data:
        return {"changed": False, "moved": [], "status": "no_svm"}

    svm_entries = find_svm_keys(additional_data, key_regex)
    if not svm_entries:
        return {"changed": False, "moved": [], "status": "no_svm"}

    external_ids = project.get("externalIds")
    if not isinstance(external_ids, dict):
        external_ids = {}

    # Keep PowerShell parity for safety: skip when externalIds already has an SVM
    # value that differs from what is in additionalData.
    first_svm_key, first_svm_value = svm_entries[0]
    existing_ext_key, existing_ext_value = get_case_insensitive_key_value(
        external_ids, str(first_svm_key)
    )
    if (
        existing_ext_key is not None
        and existing_ext_value is not None
        and str(existing_ext_value) != str(first_svm_value)
    ):
        return {
            "changed": False,
            "moved": [],
            "status": "conflict",
            "conflict": {
                "additionalDataKey": first_svm_key,
                "additionalDataValue": first_svm_value,
                "externalIdsKey": existing_ext_key,
                "externalIdsValue": existing_ext_value,
            },
        }

    moved = []
    for key, value in svm_entries:
        existing_key, _ = get_case_insensitive_key_value(external_ids, str(key))
        target_key = existing_key if existing_key is not None else key
        external_ids[target_key] = value
        del additional_data[key]
        moved.append({"key": key, "value": value})

    project["externalIds"] = external_ids
    project["additionalData"] = additional_data

    return {"changed": True, "moved": moved, "status": "migrate"}


def run():
    key_regex = re.compile(SVM_KEY_REGEX, flags=re.IGNORECASE)
    log = {}
    log['updatedProjects'] = []
    log['conflicts'] = []
    log['summary'] = {
        'projectState': PROJECT_STATE,
        'clearingStates': CLEARING_STATES,
        'tagPattern': TAG_PATTERN,
        'svmKeyRegex': SVM_KEY_REGEX,
        'dryRun': DRY_RUN,
        'candidatesByState': 0,
        'candidatesByTag': 0,
        'withSvmInAdditionalData': 0,
        'updated': 0,
        'conflicts': 0
    }

    print('Getting all projects by state -> ' + PROJECT_STATE)
    all_projects = list(db.find(projects_all_query))
    log['summary']['candidatesByState'] = len(all_projects)
    print('Received ' + str(len(all_projects)) + ' candidate projects')

    for project in all_projects:
        tag_value = project.get('tag', '')
        if not tag_matches(tag_value, TAG_PATTERN):
            continue

        log['summary']['candidatesByTag'] += 1
        result = migrate_project(project, key_regex)

        if result.get('status') == 'no_svm':
            continue

        if result.get('status') == 'conflict':
            conflict_log = {
                'id': project.get('_id'),
                'name': project.get('name'),
                'version': project.get('version'),
                'tag': project.get('tag'),
                'conflict': result.get('conflict')
            }
            log['conflicts'].append(conflict_log)
            log['summary']['conflicts'] += 1
            print('\tConflict project ID -> ' + str(project.get('_id')) + ', Project Name -> ' + str(project.get('name')))
            continue

        if result.get('status') != 'migrate':
            continue

        log['summary']['withSvmInAdditionalData'] += 1
        updated_project = {}
        updated_project['id'] = project.get('_id')
        updated_project['name'] = project.get('name')
        updated_project['version'] = project.get('version')
        updated_project['tag'] = project.get('tag')
        updated_project['movedEntries'] = result.get('moved')

        print('\tUpdating project ID -> ' + str(project.get('_id')) + ', Project Name -> ' + str(project.get('name')))
        if not DRY_RUN:
            db.save(project)

        log['updatedProjects'].append(updated_project)
        log['summary']['updated'] += 1

    result_file = open(LOG_FILE, 'w')
    json.dump(log, result_file, indent=4, sort_keys=True)
    result_file.close()

    print('\n')
    print('------------------------------------------')
    print('Candidates by state: ' + str(log['summary']['candidatesByState']))
    print('Candidates by tag: ' + str(log['summary']['candidatesByTag']))
    print('With SVM in additionalData: ' + str(log['summary']['withSvmInAdditionalData']))
    print('Updated projects: ' + str(log['summary']['updated']))
    print('Conflict projects: ' + str(log['summary']['conflicts']))
    print('------------------------------------------')
    print('Please check log file "' + LOG_FILE + '" in this directory for details')
    print('------------------------------------------')


startTime = time.time()
run()
print('\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's')

#!/usr/bin/python
# -----------------------------------------------------------------------------
# Copyright Siemens AG, 2022. Part of the SW360 Portal Project.
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
# This script is for changing the ecc status to Approved for the affected releases (releases with source code download url and component type OSS).
# ---------------------------------------------------------------------------------------------------------------------------------------------------------------

import time
import couchdb
import json
from webbrowser import get

# ---------------------------------------
# constants
# ---------------------------------------

DRY_RUN = True

COUCHSERVER = "http://localhost:5984/"
DBNAME = 'sw360db'

couch=couchdb.Server(COUCHSERVER)
db = couch[DBNAME]

COMPONENT_ID = '_id'
RELEASE_ID = '_id'
COMP_ID_IN_RELEASE = 'componentId'
ECC_INFORMATION = 'eccInformation'
ECC_STATUS = 'eccStatus'


# ----------------------------------------
# queries
# ----------------------------------------

# get all releases having source_code_download_url and get all OSS components

all_releases = { "selector": { "type": { "$eq": "release" }, "sourceCodeDownloadurl": { "$ne": "" }, "eccInformation": { "eccStatus": { "$ne": "APPROVED" } } }, "limit": 99999 }
all_oss_components = {"selector": {"type": {"$eq": "component"}, "componentType": {"$eq": "OSS"}, "releaseIds": {"$ne": []} }, "limit": 99999}

# ----------------------------------------
# functions
# ----------------------------------------

# get all releases having source_code_download_url and component_type OSS

def get_all_releases(log, releases_with_source_code_url, oss_components):
    log['updated releases'] = []
    new_list = list(releases_with_source_code_url)

    for component in oss_components:
        for release in new_list:
            if (((component[COMPONENT_ID]) == (release[COMP_ID_IN_RELEASE])) and (release[ECC_INFORMATION][ECC_STATUS] == "OPEN")):
                print ('-> Affected releaseIDs with source_code_download_url and oss component_type:  ' + release[RELEASE_ID])
                release[ECC_INFORMATION][ECC_STATUS] = "APPROVED"
                log['updated releases'].append(release)

                if not DRY_RUN:
                    db.save(release)

def run():
    log = {}
    logFile = open('051_change_eccStatus.log', 'w')

    print ('Getting all the affected releases with source_code_dwonload_url and oss component type')
    print ('\n')
    releases_with_source_code_url = db.find(all_releases)
    oss_components = db.find(all_oss_components)
    get_all_releases(log, releases_with_source_code_url, oss_components)

    json.dump(log, logFile, indent = 4, sort_keys = True)
    logFile.close()

    print ('\n')
    print ('------------------------------------------')
    print ('Please check log file "051_change_eccStatus.log" in this directory for details')

# --------------------------------

startTime = time.time()
run()
print ('\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's')


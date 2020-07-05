#!/usr/bin/python
# -----------------------------------------------------------------------------
# Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
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
# This script updates releases with empty clearingState to default value - NEW_CLEARING.
# -----------------------------------------------------------------------------

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

couch = couchdb.Server(COUCHSERVER)
db = couch[DBNAME]

# set default values for clearingState field of Release
defaultValue = "NEW_CLEARING"

# ----------------------------------------
# queries
# ----------------------------------------

# get all the releases with empty clearingState
releases_all_query = '''function(doc){
    if (doc.type == "release" && doc.clearingState == null){
        emit(doc._id, doc)
    }
}'''

# ---------------------------------------
# functions
# ---------------------------------------

def run():
    log = {}
    log['updatedReleases'] = []
    print 'Getting all releases with empty clearingState'
    releases_all = db.query(releases_all_query)
    print 'Received ' + str(len(releases_all)) + ' releases'
    log['totalCount'] = len(releases_all)

    for releaseRow in releases_all:
        release = releaseRow.value
        release['clearingState'] = defaultValue
        print '\tUpdating release ID -> ' + release.get('_id') + ', Release Name -> ' + release.get('name')
        updatedRelease = {}
        updatedRelease['id'] = release.get('_id')
        updatedRelease['name'] = release.get('name')
        log['updatedReleases'].append(updatedRelease)
        if not DRY_RUN:
            db.save(release)
    resultFile = open('017_migration.log', 'w')
    json.dump(log, resultFile, indent = 4, sort_keys = True)
    resultFile.close()

    print '\n'
    print '------------------------------------------'
    print 'Total Releases with empty clearingState : ' + str(len(releases_all))
    print '------------------------------------------'
    print 'Please check log file "017_migration.log" in this directory for details'
    print '------------------------------------------'

# --------------------------------

startTime = time.time()
run()
print '\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's'

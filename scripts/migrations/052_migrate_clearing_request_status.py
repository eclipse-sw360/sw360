#!/usr/bin/python
# -----------------------------------------------------------------------------
# Copyright Siemens Healthineers, 2022. Part of the SW360 Portal Project.
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
# This script is for migrating Obligation status.
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

couch = couchdb.Server(COUCHSERVER)
db = couch[DBNAME]

# ----------------------------------------
# queries
# ----------------------------------------

# get all the Clearing Request with status "On Hold"
all_On_Hold_Clearing_Requests = {"selector": {"type": {"$eq": "clearingRequest"}, "clearingState": {"$eq": "ON_HOLD"}}, "limit": 99999}

# ---------------------------------------
# functions
# ---------------------------------------
def migrateAndUpdateClearingRequestStatus(logFile, docs):
    log = {}
    log['total CR With LinkedObligationStatus'] = len(docs)
    log['Updated CR'] = []
    log['CR to be updated(Dry run)'] = []
    for cr in docs:
        crState = cr.get("clearingState")
        print "CR Id: " + cr.get("_id")
        cr["clearingState"] = "AWAITING_RESPONSE"
        if DRY_RUN:
            updateDocId_Dry_Run = {}
            updateDocId_Dry_Run['crId'] = cr.get('_id')
            updateDocId_Dry_Run['projectId'] = cr.get('projectId', None)
            log['CR to be updated(Dry run)'].append(updateDocId_Dry_Run)
        if not DRY_RUN:
            db.save(cr);
            updatedDocId = {}
            updatedDocId['id'] = cr.get('_id')
            updatedDocId['projectId'] = cr.get('projectId', None)
            log['Updated CR'].append(updatedDocId)

    json.dump(log, logFile, indent = 4, sort_keys = True)


def run():
    logFile = open('052_migrate_clearing_request_status.log', 'w')
    print 'Getting all the Clearing Request in "On Hold" state'
    onHold_ClearingRequests = db.find(all_On_Hold_Clearing_Requests);
    print 'found ' + str(len(onHold_ClearingRequests)) + ' CR with On HOld status\n'
    migrateAndUpdateClearingRequestStatus(logFile, onHold_ClearingRequests);
    logFile.close()
    print '------------------------------------------'
    print 'Please check log file "052_migrate_clearing_request_status.log" in this directory for details'
    print '------------------------------------------'

# --------------------------------

startTime = time.time()
run()
print '\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's'

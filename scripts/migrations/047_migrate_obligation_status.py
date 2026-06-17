#!/usr/bin/python
# -----------------------------------------------------------------------------
# Copyright Siemens Healthineers, 2021. Part of the SW360 Portal Project.
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

couch=couchdb.Server(COUCHSERVER)
db = couch[DBNAME]

# ----------------------------------------
# queries
# ----------------------------------------

# get all the Obligations with field "linkedObligationStatus"
all_Obligations_with_linkedObligationStatus = {"selector": {"type": {"$eq": "obligationList"}, "linkedObligationStatus": {"$exists": True}}, "limit": 99999}

# ---------------------------------------
# functions
# ---------------------------------------

def migrateAndUpdateObligationStatus(logFile, docs):
    log = {}
    log['total Obligations With LinkedObligationStatus'] = len(docs)
    log['Updated Obligations'] = []
    log['Obligations to be updated(Dry run)'] = []
    for obl in docs:
        oblStatus = obl.get("linkedObligationStatus")
        print "\n\nDoc Id: " + obl.get("_id")
        for k, v in oblStatus.items():

            if bool(v) and bool(v.get("status")):
                print (k)
                if v.get("status").upper() == "IN_PROGRESS":
                    v["status"] = "WILL_BE_FULFILLED_BEFORE_RELEASE"
                if v.get("status").upper() == "FULFILLED":
                    v["status"] = "ACKNOWLEDGED_OR_FULFILLED"
                if v.get("status").upper() == "TO_BE_FULFILLED_BY_PARENT_PROJECT":
                    v["status"] = "DEFERRED_TO_PARENT_PROJECT"

                obl["linkedObligationStatus"] = oblStatus

        if DRY_RUN:
            updateDocId_Dry_Run = {}
            updateDocId_Dry_Run['id'] = obl.get('_id')
            updateDocId_Dry_Run['projectId'] = obl.get('projectId', None)
            log['Obligations to be updated(Dry run)'].append(updateDocId_Dry_Run)
        if not DRY_RUN:
            db.save(obl);
            updatedDocId = {}
            updatedDocId['id'] = obl.get('_id')
            updatedDocId['projectId'] = obl.get('projectId', None)
            log['Updated Obligations'].append(updatedDocId)

    json.dump(log, logFile, indent = 4, sort_keys = True)

def run():
    logFile = open('047_migrate_obligation_status.log', 'w')
    print 'Getting all the Obligations with field "linkedObligationStatus"'
    obligations_with_linkedObligationStatus = db.find(all_Obligations_with_linkedObligationStatus);
    print 'found ' + str(len(obligations_with_linkedObligationStatus)) + ' obligations with linkedObligationStatus'
    migrateAndUpdateObligationStatus(logFile, obligations_with_linkedObligationStatus);
    logFile.close()

    print '\n'
    print '------------------------------------------'
    print 'Please check log file "047_migrate_obligation_status.log" in this directory for details'
    print '------------------------------------------'

# --------------------------------

startTime = time.time()
run()
print '\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's'

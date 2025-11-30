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

couch=couchdb.Server(COUCHSERVER)
db = couch[DBNAME]

# ----------------------------------------
# queries
# ----------------------------------------

# get all the Obligations with field "linkedObligationStatus"
all_Obligations_with_linkedObligationStatus = {"selector": {"type": {"$eq": "obligationList"}, "linkedObligationStatus": {"$exists": True, "$ne": {}}}, "limit": 99999}

#get all the obligation from Admin section
all_Obligations_from_AdminSection = {"selector": {"type": {"$eq": "obligation"}}, "limit": 99999}


# ---------------------------------------
# functions
# ---------------------------------------
def migrateAndUpdateAdminObligationStatus(logFile, obligationListWithStatus, adminObligations):
    log = {}
    log['Total No of Obligations With LinkedObligationStatus'] = len(obligationListWithStatus)
    log['Total No LinkedObligationStatus with Admin Level obligations in it'] = 0
    log['mismatch-obligations-total-count'] = 0
    log['matched-obligations-total-count'] = 0
    log['null-obligations-count'] = 0
    log['mismatch-obligations-DocId-To-ProjectId'] = []
    log['obligations-updated-successfully'] = []
    log['obligations-to-be-updated-dry-run'] = []
    log['obligations-to-be-updated-manually'] = []
    log['obligations-removed-due-to-null-key'] = []
    mismatchLog = {}
    for obl in obligationListWithStatus:
        oblStatus = obl.get("linkedObligationStatus")
        docId = obl.get("_id")
        projectId = obl.get('projectId', None)
        print "\n\nDoc Id: " + docId
        for k, v in oblStatus.items():
            if bool(v) and bool(v.get("obligationLevel")):
                print "Obligation Title: " + k
                isNotFound = True
                updatedDocId = {}
                updatedDocId['id'] = docId
                updatedDocId['project-id'] = projectId
                updatedDocId['obligation-title'] = k
                log['Total No LinkedObligationStatus with Admin Level obligations in it'] += 1
                for adminObl in adminObligations:
                    oblText = adminObl.get("text").upper()
                    oblLevel = adminObl.get("obligationLevel").upper()
                    if v.get("obligationLevel").upper() == oblLevel and k.upper() == oblText:
                        oblStatus[adminObl.get("title")] = oblStatus[k]
                        del oblStatus[k]
                        isNotFound = False
                        log['matched-obligations-total-count'] += 1
                        if DRY_RUN:
                            log['obligations-to-be-updated-dry-run'].append(updatedDocId)

                        if not DRY_RUN:
                            log['obligations-updated-successfully'].append(updatedDocId)

                if isNotFound:
                    print "\nNo Admin level obligation is found for Obligation Text: " + k + ", Obl level: " + v.get("obligationLevel")
                    log['mismatch-obligations-total-count'] += 1
                    mismatchLog[docId] = projectId
                    if k == 'null':
                        del oblStatus[k]
                        log['null-obligations-count'] += 1
                        log['obligations-removed-due-to-null-key'].append(updatedDocId)
                    else:
                        log['obligations-to-be-updated-manually'].append(updatedDocId)

                obl["linkedObligationStatus"] = oblStatus

        if DRY_RUN:
            log['mismatch-obligations-DocId-To-ProjectId'] = mismatchLog
        if not DRY_RUN:
            db.save(obl);
            log['mismatch-obligations-DocId-To-ProjectId'] = mismatchLog

    json.dump(log, logFile, indent = 4, sort_keys = True)


def run():
    logFile = open('049_migrate_admin_obligation.log', 'w')
    print 'Getting all the Obligations with field "linkedObligationStatus"'
    obligations_with_linkedObligationStatus = db.find(all_Obligations_with_linkedObligationStatus);
    print 'found ' + str(len(obligations_with_linkedObligationStatus)) + ' obligations with linkedObligationStatus\n'
    print 'Getting all the Obligations (License/Project/Component/Organisation) from Admin section'
    obligations_from_AdminSection = db.find(all_Obligations_from_AdminSection);
    print 'found ' + str(len(obligations_from_AdminSection)) + ' obligations in Admin Section\n'
    migrateAndUpdateAdminObligationStatus(logFile, obligations_with_linkedObligationStatus, obligations_from_AdminSection);
    logFile.close()

    print '\n'
    print '------------------------------------------'
    print 'Please check log file "049_migrate_admin_obligation.log" in this directory for details'
    print '------------------------------------------'

# --------------------------------

startTime = time.time()
run()
print '\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's'

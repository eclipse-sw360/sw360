#!/usr/bin/python
# -----------------------------------------------------------------------------
# Copyright Siemens AG, 2020. Part of the SW360 Portal Project.
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
# This script renames the type to obligationType and converts the existing type to upper case and saves it to DB
# -------------------------------------------------------------------------------------------------------------

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

# get all obligationlist with field linkedObligationStatus
all_obligationlist_with_field_linkedObligationStatus = {"selector": {"type": {"$eq": "obligationList"}, "linkedObligationStatus": {"$exists": True}}, "limit":20000}

# ---------------------------------------
# functions
# ---------------------------------------

def convertTypeNPopulatedata(resultFile, all_obligationlist):
    log = {}
    log['updatedObligationLists'] = []

    for oblList in all_obligationlist:
        linkedOblStatus = oblList.get("linkedObligationStatus")
        for lOblSts in linkedOblStatus.values():
            type = lOblSts.get("type")
            if type is not None:
                type = type.upper()
                lOblSts["obligationType"] = type
                del lOblSts["type"]

        updatedObligationLists = {}
        updatedObligationLists['id'] = oblList.get('_id')
        log['updatedObligationLists'].append(updatedObligationLists)
        if not DRY_RUN:
            db.save(oblList);

    json.dump(log, resultFile, indent = 4, sort_keys = True)


def run():
    logFile = open('038_convert_ObligationStatusInfo_type_to_obligationType.log', 'w')
    
    print 'Getting all obligationlist with field linkedObligationStatus'
    all_obligationlist = db.find(all_obligationlist_with_field_linkedObligationStatus)
    print 'found ' + str(len(all_obligationlist)) + ' projects with field todos in db!'

    convertTypeNPopulatedata(logFile, all_obligationlist)

    logFile.close()

    print '\n'
    print '------------------------------------------'
    print 'Please check log file "038_convert_ObligationStatusInfo_type_to_obligationType.log" in this directory for details'
    print '------------------------------------------'

# --------------------------------

startTime = time.time()
run()
print '\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's'

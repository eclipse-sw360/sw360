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
# This script finds and copies "text" field value from Obligation to licenseObligation
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

# get all license obligation with field "obligationId"
licenseObligation_with_obligationId_query = {"selector": {"type": {"$eq": "licenseObligation"},"obligationId": {"$exists": True}}}

# ---------------------------------------
# functions
# ---------------------------------------


def run():
    log = {}
    text = "";
    print 'Getting all licenseObligation with field obligationId'
    licenseObligation_with_obligationId = db.find(licenseObligation_with_obligationId_query)
    print 'found ' + str(len(licenseObligation_with_obligationId)) + ' licenseObligation with field obligationId in db!'
    log['totalCount'] = len(licenseObligation_with_obligationId)
    for entity_row in licenseObligation_with_obligationId:
        obligationId = entity_row["obligationId"]
        obligation_with_obligationDatabaseIds_query = {"selector": { "type": { "$eq": "obligation" }, "obligationDatabaseIds": { "$exists": True, "$elemMatch": { "$eq": ""+obligationId+"" } } }}
        obligation_with_obligationDatabaseIds = db.find(obligation_with_obligationDatabaseIds_query)
        for entity_r in obligation_with_obligationDatabaseIds:
            text = entity_r["text"];
        entity_row["text"] = text
        if not DRY_RUN:
            db.save(entity_row);
    
    print "Done"
        
    resultFile = open('026_licenseObligation_populate_text.log', 'w')
    json.dump(log, resultFile, indent = 4, sort_keys = True)
    resultFile.close()

    print '\n'
    print '------------------------------------------'
    print 'Please check log file "026_licenseObligation_populate_text.log" in this directory for details'
    print '------------------------------------------'

# --------------------------------

startTime = time.time()
run()
print '\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's'

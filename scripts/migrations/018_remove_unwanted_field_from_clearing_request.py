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
# This script removes the field "clearingTeamComment" from Clearing Request
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


# set fieldValue
fieldValue = "clearingTeamComment"


# ----------------------------------------
# queries
# ----------------------------------------

# get all the clearing requests
cr_all_query = '''function(doc){
    if (doc.type == "clearingRequest" && "'''+fieldValue+'''" in doc){
        emit(doc._id, doc)
    }
}'''

# ---------------------------------------
# functions
# ---------------------------------------

def run():
    log = {}
    count = 0;
    log['updatedCR'] = []
    print 'Getting all clearing requests'
    clearing_request_all = db.query(cr_all_query)
    print 'found ' + str(len(clearing_request_all)) + ' clearing requests with clearingTeamComment in db!'
    log['totalCount'] = len(clearing_request_all)

    for crRow in clearing_request_all:
        cr = crRow.value
        print '\tUpdating Clearing Request with ID -> ' + cr.get('_id')
        del cr[fieldValue]
        updatedCR = {}
        updatedCR['id'] = cr.get('_id')
        log['updatedCR'].append(updatedCR)
        count = count + 1
        if not DRY_RUN:
            db.save(cr)
            print '\tUpdated Clearing Request with ID -> ' + cr.get('_id')
    resultFile = open('018_clearing_request_migration_'+fieldValue+'.log', 'w')
    json.dump(log, resultFile, indent = 4, sort_keys = True)
    resultFile.close()

    print '\n'
    print '------------------------------------------'
    print 'Total Clearing Request updated : ' + str(count)
    print '------------------------------------------'
    print 'Please check log file "018_clearing_request_migration_'+fieldValue+'.log" in this directory for details'
    print '------------------------------------------'

# --------------------------------

startTime = time.time()
run()
print '\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's'

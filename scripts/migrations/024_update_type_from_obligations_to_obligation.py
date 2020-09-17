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
# This script updates type of document from obligations to obligation
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

# set fieldName
newValue = "obligation"

# ----------------------------------------
# queries
# ----------------------------------------

# get all obligations
all_obligations_query = {"selector": {"type": {"$eq": "obligations"}}}

# ---------------------------------------
# functions
# ---------------------------------------

def run():
    log = {}
    log['updatedObligation'] = []
    print 'Getting all obligations'
    all_obligations = db.find(all_obligations_query)
    print 'found ' + str(len(all_obligations)) + ' obligations in db!'
    log['totalCount'] = len(all_obligations)

    for obligation in all_obligations:
        print '\tUpdating type of document from obligations to '+newValue+' for ID -> ' + obligation.get('_id')
        obligation['type'] = newValue
        updatedObligation = {}
        updatedObligation['id'] = obligation.get('_id')
        log['updatedObligation'].append(updatedObligation)
        if not DRY_RUN:
            db.save(obligation)
            print '\tUpdated type of document from obligations to '+newValue+' for ID -> ' + obligation.get('_id')

    resultFile = open('024_obligations_type_migration.log', 'w')
    json.dump(log, resultFile, indent = 4, sort_keys = True)
    resultFile.close()

    print '\n'
    print '------------------------------------------'
    print 'Please check log file "024_obligations_type_migration.log" in this directory for details'
    print '------------------------------------------'

# --------------------------------

startTime = time.time()
run()
print '\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's'

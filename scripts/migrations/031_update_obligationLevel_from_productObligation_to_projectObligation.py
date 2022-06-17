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
# This script updates the value of field "obligationLevel" in Obligation 
# from "PRODUCT_OBLIGATION" to "PROJECT_OBLIGATION"
# ------------------------------------------------------------------------------

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
oldFieldValue = "PRODUCT_OBLIGATION"
newFieldValue = "PROJECT_OBLIGATION"

# ----------------------------------------
# queries
# ----------------------------------------

# get all obligations with field "obligationLevel" as "PRODUCT_OBLIGATION"
product_obligations_query = {"selector": {"type": {"$eq": "obligation"},"obligationLevel": {"$eq": oldFieldValue}}}

# ---------------------------------------
# functions
# ---------------------------------------

def run():
    log = {}
    log['updatedObligation'] = []
    print 'Getting all obligations with field "obligationLevel" as ' + oldFieldValue
    product_obligations = db.find(product_obligations_query)
    print 'found ' + str(len(product_obligations)) + ' obligations with field obligationLevel as ' + oldFieldValue + ' in db!'
    log['totalCount'] = len(product_obligations)

    for obligation in product_obligations:
        print '\tUpdating obligationLevel of document from ' + oldFieldValue + ' to ' + newFieldValue + ' for ID -> ' + obligation.get('_id')
        obligation['obligationLevel'] = newFieldValue
        updatedObligation = {}
        updatedObligation['id'] = obligation.get('_id')
        log['updatedObligation'].append(updatedObligation)
        if not DRY_RUN:
            db.save(obligation)
            print '\tUpdated obligationLevel of document from ' + oldFieldValue + ' to ' + newFieldValue + ' for ID -> ' + obligation.get('_id')
    resultFile = open('031_update_obligationLevel_from_' + oldFieldValue + '_to_' + newFieldValue + '.log', 'w')
    json.dump(log, resultFile, indent = 4, sort_keys = True)
    resultFile.close()

    print '\n'
    print '------------------------------------------'
    print 'Please check log file "031_update_obligationLevel_from_' + oldFieldValue + '_to_' + newFieldValue + '.log" in this directory for details'
    print '------------------------------------------'

# --------------------------------

startTime = time.time()
run()
print '\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's'

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
# This script updates type of document from license 0obligation to obligation and add ObligationLevel as LICENSE_OBLIGATION
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

# get all license obligation
all_license_obligation_query = {"selector": {"type": {"$eq": "licenseObligation"}}}

# ---------------------------------------
# functions
# ---------------------------------------

def run():
    log = {}
    log['updatedObligation'] = []
    print 'Getting all license obligation'
    all_license_obligations = db.find(all_license_obligation_query)
    print 'found ' + str(len(all_license_obligations)) + ' license obligations in db!'
    log['totalCount'] = len(all_license_obligations)

    for obligation in all_license_obligations:
        print '\tUpdating type of document from licenseObligation to '+newValue+' for ID -> ' + obligation.get('_id')
        obligation['type'] = newValue
        obligation['obligationLevel'] = 'LICENSE_OBLIGATION'
        updatedObligation = {}
        updatedObligation['id'] = obligation.get('_id')
        log['updatedObligation'].append(updatedObligation)
        if not DRY_RUN:
            db.save(obligation)
            print '\tUpdated type of document from licenseObligation to '+newValue+' for ID -> ' + obligation.get('_id')

    resultFile = open('028_licenseobligation_type_migration.log', 'w')
    json.dump(log, resultFile, indent = 4, sort_keys = True)
    resultFile.close()

    print '\n'
    print '------------------------------------------'
    print 'Please check log file "028_licenseobligation_type_migration.log" in this directory for details'
    print '------------------------------------------'

# --------------------------------

startTime = time.time()
run()
print '\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's'

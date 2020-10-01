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
# This script updates type of document from projectObligation to obligationList
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
newValue = "obligationList"

# ----------------------------------------
# queries
# ----------------------------------------

# get all projectObligations
all_projectObligations_query = {"selector": {"type": {"$eq": "projectObligation"}}}

# ---------------------------------------
# functions
# ---------------------------------------

def run():
    log = {}
    log['updatedProjectObligations'] = []
    print 'Getting all projectObligations'
    all_projectObligations = db.find(all_projectObligations_query)
    print 'found ' + str(len(all_projectObligations)) + ' projectObligations in db!'
    log['totalCount'] = len(all_projectObligations)

    for projectObligation in all_projectObligations:
        print '\tUpdating type of document from obligations to '+newValue+' for ID -> ' + projectObligation.get('_id')
        projectObligation['type'] = newValue
        updatedProjectObligation = {}
        updatedProjectObligation['id'] = projectObligation.get('_id')
        log['updatedProjectObligations'].append(updatedProjectObligation)
        if not DRY_RUN:
            db.save(projectObligation)
            print '\tUpdated type of document from projectObligation to '+newValue+' for ID -> ' + projectObligation.get('_id')

    resultFile = open('033_projectObligation_type_migration.log', 'w')
    json.dump(log, resultFile, indent = 4, sort_keys = True)
    resultFile.close()

    print '\n'
    print '------------------------------------------'
    print 'Please check log file "033_projectObligation_type_migration.log" in this directory for details'
    print '------------------------------------------'

# --------------------------------

startTime = time.time()
run()
print '\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's'

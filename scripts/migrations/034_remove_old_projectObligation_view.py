#!/usr/bin/python
# -----------------------------------------------------------------------------
# Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
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
# This script removes old design document for ProjectObligation
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

doc_id = "_design/ProjectObligation"

# ---------------------------------------
# functions
# ---------------------------------------

def run():
    log = {}
    log['docId'] = doc_id
    print 'Getting Document by ID : ' + doc_id
    doc = db.get(doc_id, None)
    if doc is not None:
        print 'Received document.Deleting Document.'
        print 'Deleting Document with ID : ' + doc_id
        log['result'] = 'Deleted Document with ID : ' + doc_id
        if not DRY_RUN:
            db.delete(doc)
    else:
        print 'No document found with this ID.'
        log['result'] = 'No document found with this ID.'
    
    
    resultFile = open('034_remove_old_projectObligation_view.log', 'w')
    json.dump(log, resultFile, indent = 4)
    resultFile.close()

    print '\n'
    print '------------------------------------------'
    print 'Please check log file "034_remove_old_projectObligation_view.log" in this directory for details'
    print '------------------------------------------'

# --------------------------------

startTime = time.time()
run()
print '\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's'

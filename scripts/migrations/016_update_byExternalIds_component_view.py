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

doc_id = "_design/Component"
nested_field_key = ["views", "byExternalIds", "map"]
new_value = "function(doc) {  if (doc.type == 'component') {    for (var externalId in doc.externalIds) {       emit( [externalId, doc.externalIds[externalId]] , doc._id);    }  }}"

# ---------------------------------------
# functions
# ---------------------------------------

def run():
    log = {}
    log['docId'] = doc_id
    print 'Getting Document by ID : ' + doc_id
    doc = db.get(doc_id, None)
    if doc is not None:
        print 'Received document.'
        old_value = updateValue(doc, nested_field_key, new_value)
        if old_value is not None:
            field_to_update = "->"
            field_to_update = field_to_update.join(nested_field_key)
            
            print 'Field to update : ' + field_to_update
            print 'Old Value : ' + old_value
            print 'Updating Value for Document : ' + doc_id
            
            log['field_to_update'] = field_to_update
            log['old_value'] = old_value
            log['new_value'] = new_value
            if not DRY_RUN:
                db.save(doc)
        else:
            print 'Key Not Found.'
            log['result'] = 'Key Not Found.'
    else:
        print 'No document found with this ID.'
        log['result'] = 'No document found with this ID.'
    
    
    resultFile = open('016_migration.log', 'w')
    json.dump(log, resultFile, indent = 4)
    resultFile.close()

    print '\n'
    print '------------------------------------------'
    print 'Please check log file "016_migration.log" in this directory for details'
    print '------------------------------------------'

def updateValue(input_dictionary, nested_key, new_value):
    dictionary_value = input_dictionary
    for key in nested_key[:-1]:
        dictionary_value = dictionary_value.get(key, None)
        if dictionary_value is None:
            return None
    last_key = nested_key[-1]
    old_value = dictionary_value.get(last_key, None)
    dictionary_value[last_key] = new_value
    
    return old_value

# --------------------------------

startTime = time.time()
run()
print '\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's'

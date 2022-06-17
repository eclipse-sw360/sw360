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
# This script updates document type from 'todo' to 'obligations',
# rename field 'obligations' to 'listOfobligation', 'todoDatabaseIds' to 'obligationDatabaseIds'
# and 'todos' to 'obligations' in license
# ---------------------------------------------------------------------------------------------------------------------------------------------------------------
import time
import couchdb
import json
from webbrowser import get

COUCHSERVER = "http://localhost:5984/"
DBNAME = 'sw360db'

couch=couchdb.Server(COUCHSERVER)
db = couch[DBNAME]

DRY_RUN = True

get_all_todos_query = '''function(doc){
    if (doc.type == "todo"){
        emit(doc._id, doc)
    }
}'''

get_all_obligation_query = '''function(doc){
    if (doc.type == "obligation"){
        emit(doc._id, doc)
    }
}'''

licenses_with_todoDatabaseIds = '''function(doc){
    if (doc.type=="license" && doc.todoDatabaseIds){
        emit(doc._id, doc)
    }
}'''

obligations_with_field_obligations = '''function(doc){
    if (doc.type=="obligations" && doc.obligations){
        emit(doc._id, doc)
    }
}'''

licenses_with_obligations = '''function(doc){
    if (doc.type=="license" && doc.todos){
        emit(doc._id, doc)
    }
}'''


licenses_tdDbIds = db.query(licenses_with_todoDatabaseIds)
obligations_obligs = db.query(obligations_with_field_obligations)
licenses_obligs = db.query(licenses_with_obligations)

log = {}

todo_designview_doc_id = "_design/Todo"
obligation__designview_doc_id = "_design/Obligation"

def updateFieldNames(qryResult, oldValue, newValue):
    print 'updating field name from '+oldValue+' to '+newValue
    log['updated fields from '+oldValue+' to '+newValue] = []
    for entity_row in qryResult:
        entity = entity_row.value
        entity[''+newValue+''] = entity[''+oldValue+'']
        del entity[''+oldValue+'']
        if not DRY_RUN:
            db.save(entity)
        updatedField = {}
        updatedField['id'] = entity.get('_id')
        log['updated fields from '+oldValue+' to '+newValue].append(updatedField)
    print 'updation of field name from '+oldValue+' to '+newValue+' done'

def updateDocType(query):
    print 'updating document type from "todo" to "obligations" starts'
    log['updatedDocuments'] = []
    todos_all = db.query(query)
    for todoRow in todos_all:
        todo = todoRow.value
        todo['obligationType'] = 'ORGANISATION_OBLIGATION'
        todo['type'] = "obligations"
        if not DRY_RUN:
            db.save(todo)
        updatedDocs = {}
        updatedDocs['id'] = todo.get('_id')
        log['updatedDocuments'].append(updatedDocs)
    print 'updation done'

def updateDocTypeObligation(query):
    print 'updating document type from "obligation" to "licenseObligation" starts'
    log['updatedDocuments'] = []
    obligation_all = db.query(query)
    for obligationRow in obligation_all:
        obligation = obligationRow.value
        obligation['type'] = "licenseObligation"
        if not DRY_RUN:
            db.save(obligation)
        updatedDocs = {}
        updatedDocs['id'] = obligation.get('_id')
        log['updatedDocuments'].append(updatedDocs)
    print 'updation done'

def deleteTodoView():
    if not DRY_RUN:
        if todo_designview_doc_id in db:
            del db[todo_designview_doc_id]

def deleteObligationView():
    if not DRY_RUN:
        if obligation__designview_doc_id in db:
            del db[obligation__designview_doc_id]

def run():
    updateDocType(get_all_todos_query);
    updateDocTypeObligation(get_all_obligation_query);
    updateFieldNames(licenses_tdDbIds, 'todoDatabaseIds', 'obligationDatabaseIds');
    updateFieldNames(obligations_obligs, 'obligations', 'listOfobligation');
    updateFieldNames(licenses_obligs, 'todos', 'obligations');
    deleteTodoView();
    deleteObligationView();

    resultFile = open('022_migration.log', 'w')
    json.dump(log, resultFile, indent = 4, sort_keys = True)
    resultFile.close()

    print '\n'
    print '------------------------------------------'
    print 'Please check log file "022_migration.log" in this directory for details'
    print '------------------------------------------'


startTime = time.time()
run()
print '\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's'

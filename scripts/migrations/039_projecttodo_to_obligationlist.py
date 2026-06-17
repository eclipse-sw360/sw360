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
# This script merge copy todos from project to obligationList model and and removes the todos field from project
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
USER_DBNAME = 'sw360users'

couch = couchdb.Server(COUCHSERVER)
db = couch[DBNAME]
userdb = couch[USER_DBNAME]

# ----------------------------------------
# queries
# ----------------------------------------

# get all projects with todos
all_project_with_todos = {"selector": {"type": {"$eq": "project"}, "todos": {"$exists": True}}, "limit":20000}

# ---------------------------------------
# functions
# ---------------------------------------

def getOblTextNLevel(oblId):
    obltextNLevel = {}
    get_obligation_using_id = {"selector": {"type": {"$eq": "obligation"}, "_id": {"$eq": ""+oblId+"" }}}
    obligation = db.find(get_obligation_using_id)
    for obl in obligation:
        text = obl.get("text")
        obllevel = obl.get("obligationLevel")
        obltextNLevel["oblText"] = text
        obltextNLevel["oblLevel"] = obllevel
    
    return obltextNLevel

def getDateOnly(dateWithTimes):
    return dateWithTimes.split()[0]

def getEmailFromUserId(userid):
    emailId = ""
    get_user_by_id = {"selector": {"type": {"$eq": "user"}, "_id": {"$eq": ""+userid+"" }}}
    user = userdb.find(get_user_by_id)
    for usr in user:
        emailId = usr.get("email")

    return emailId

def getLinkedObligationList(linkedOblId):
    oblList = []
    get_obligationList_by_id = {"selector": {"type": {"$eq": "obligationList"}, "_id": {"$eq": ""+linkedOblId+"" }}}
    obligationList = db.find(get_obligationList_by_id)
    for obList in obligationList:
        oblList = obList
    
    return oblList

def mergeTodoWithObligationList(resultFile, all_projects):
    log = {}
    log['totalCount'] = len(all_projects)
    log['updatedProjectsWithobligationList'] = []

    oblList = {}
    obligationStatusInfo = {}
    obligationStatusInfos = {}

    for project in all_projects:
        linkedOblId = project.get("linkedObligationId")
        todos = project.get("todos")
        if len(todos) > 0:
            if linkedOblId is None:
                oblList["type"] = 'obligationList'
                oblList["projectId"] = project.get("_id")
                for todo in todos:
                    todotext = getOblTextNLevel(todo.get("todoId")).get("oblText")
                    obligationLevel = getOblTextNLevel(todo.get("todoId")).get("oblLevel")
                    obligationStatusInfo["obligationLevel"] = obligationLevel
                    obligationStatusInfo["text"] = todotext
                    obligationStatusInfo["status"] = 'FULFILLED' if todo.get("fulfilled") == True else 'OPEN'
                    todocomment = todo.get("comments")
                    obligationStatusInfo["comment"] = todocomment if todocomment is not None else ""
                    obligationStatusInfo["modifiedOn"] = getDateOnly(todo.get("updated")) if todo.get("updated") != "" else ""
                    obligationStatusInfo["modifiedBy"] = getEmailFromUserId(todo.get("userId"))
                    obligationStatusInfos[todotext] = {}
                    obligationStatusInfos[todotext] = obligationStatusInfo
                oblList["linkedObligationStatus"] = obligationStatusInfos
                
                updatedProjectsWithobligationList = {}
                updatedProjectsWithobligationList['id'] = project.get('_id')
                log['updatedProjectsWithobligationList'].append(updatedProjectsWithobligationList)

                if not DRY_RUN:
                    doc_id, doc_rev = db.save(oblList)
                    project["linkedObligationId"] = doc_id
                    db.save(project)
            else:
                obligList = getLinkedObligationList(project.get("linkedObligationId"))
                for todo in todos:
                    todotext = getOblTextNLevel(todo.get("todoId")).get("oblText")
                    obligationStatusInfo["text"] = todotext
                    obligationLevel = getOblTextNLevel(todo.get("todoId")).get("oblLevel")
                    obligationStatusInfo["obligationLevel"] = obligationLevel
                    obligationStatusInfo["status"] = 'FULFILLED' if todo.get("fulfilled") == True else 'OPEN'
                    todocomment = todo.get("comments")
                    obligationStatusInfo["comment"] = todocomment if todocomment is not None else ""
                    obligationStatusInfo["modifiedOn"] = getDateOnly(todo.get("updated")) if todo.get("updated") != "" else ""
                    obligationStatusInfo["modifiedBy"] = getEmailFromUserId(todo.get("userId"))
                    obligationStatusInfos[todotext] = {}
                    obligationStatusInfos[todotext] = obligationStatusInfo
                    exstingOblStatusInfo = obligList.get("linkedObligationStatus")
                    obligationStatusInfos.update(exstingOblStatusInfo)
                obligList["linkedObligationStatus"] = obligationStatusInfos

                updatedProjectsWithobligationList = {}
                updatedProjectsWithobligationList['id'] = project.get('_id')
                log['updatedProjectsWithobligationList'].append(updatedProjectsWithobligationList)

                if not DRY_RUN:
                    doc_id, doc_rev = db.save(obligList)

    json.dump(log, resultFile, indent = 4)

def removeFieldName(resultFile, qryResult, fieldToBeRemoved):
    log = {}
    log['totalCount'] = len(qryResult)
    print 'Removing field name '+fieldToBeRemoved
    log['Updated project fields '+fieldToBeRemoved] = []
    for entity in qryResult:
        del entity[''+fieldToBeRemoved+'']
        if not DRY_RUN:
            db.save(entity)
            print 'Removing field name '+fieldToBeRemoved+' done for '+entity.get('_id')
        updatedDocId = {}
        updatedDocId['id'] = entity.get('_id')
        log['Updated project fields '+fieldToBeRemoved].append(updatedDocId)
    
    json.dump(log, resultFile, indent = 4, sort_keys = True)

def run():
    logFile = open('039_projecttodo_to_obligationlist.log', 'w')
    
    print 'Getting all projects with field todos'
    all_projects = db.find(all_project_with_todos)
    print 'found ' + str(len(all_projects)) + ' projects with field todos in db!'

    mergeTodoWithObligationList(logFile, all_projects)

    removeFieldName(logFile, all_projects, "todos")
    logFile.close()

    print '\n'
    print '------------------------------------------'
    print 'Please check log file "039_projecttodo_to_obligationlist.log" in this directory for details'
    print '------------------------------------------'

# --------------------------------

startTime = time.time()
run()
print '\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's'

#!/usr/bin/python
# -----------------------------------------------------------------------------
# Copyright Siemens AG, 2024. Part of the SW360 Portal Project.
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
# This script is for adding additional fields (releaseName and releaseVersion) to releaseIdToUsage in project document.
# ---------------------------------------------------------------------------------------------------------------------------------------------------------------

import time
import datetime
import couchdb
import json
from webbrowser import get

# ---------------------------------------
# constants
# ---------------------------------------

DRY_RUN = True

COUCHSERVER = 'http://username:pwd@localhost:5984/'
SW360_DB = 'sw360db'

couch = couchdb.Server(COUCHSERVER)
sw360db = couch[SW360_DB]

#Project data
RELEASE_TO_USAGE = 'releaseIdToUsage'
PROJECT_ID = '_id'

#Release data
RELEASE_NAME = 'releaseName'
RELEASE_VERSION = 'version'

# ----------------------------------------
# queries
# ----------------------------------------

# get all the projects
projects_query = {
   "selector": {
      "type": {
         "$eq": "project"
      },
      "releaseIdToUsage": {
         "$ne": {}
      }
   },
   "limit": 999999
}

# ---------------------------------------
# functions
# ---------------------------------------

#Add Additional fields to releaseIdToUsage in project document
def addFieldsToReleaseIdToUsage(log, projectList):
    print ("Project Info")
    print ("################")
    log['Updated releaseIdToUsage field'] = []
    
    for project in projectList:
        print("projectId: ", project.get("_id"))
        for release_id in project[RELEASE_TO_USAGE].keys():
            relName= ""
            relVersion= ""
            get_release_using_id = {"selector": {"type": {"$eq": "release"}, "_id": {"$eq": ""+release_id+"" }}}
            release = sw360db.find(get_release_using_id)
            for rel in release:
                relName = rel.get("name")
                relVersion = rel.get("version")
            project[RELEASE_TO_USAGE][release_id][RELEASE_NAME] = relName
            project[RELEASE_TO_USAGE][release_id][RELEASE_VERSION] = relVersion
        log['Updated releaseIdToUsage field'].append(project[PROJECT_ID])
        log['Updated releaseIdToUsage field'].append(project[RELEASE_TO_USAGE])   
        
        if not DRY_RUN:
            sw360db.save(project)

def run():
    log = {}
    logFile = open('updatedProjects.log', 'w')

    print ('Updated Projects detail in updatedProjectsDoc.log file:')
    print ('\n')
    projects = sw360db.find(projects_query)
    projectList = list(projects)

    addFieldsToReleaseIdToUsage(log, projectList)
    print ('\n')

    json.dump(log, logFile, indent = 4, sort_keys = True)
    logFile.close()

    print ('\n')
    print ('------------------------------------------')
    print ('Please check log file "updatedProjectsDoc.log" in this directory for details')

# --------------------------------

startTime = time.time()
run()
print ('\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's')

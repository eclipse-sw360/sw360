#!/usr/bin/python
# -----------------------------------------------------------------------------
# Copyright Siemens AG, 2023. Part of the SW360 Portal Project.
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
# This script will delete the Releases that are not linked to any project and does not have any clearing results and will also delete the components that does not have any releases..
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
sw360Db = couch[SW360_DB]

#enter the date, all the releases not linked to any project before this date will get deleted
bufferDate = '2022-06-01'

RELEASE_IDS = 'releaseIdToUsage'
ID= '_id'
RELEASE_IDS_COMP = 'releaseIds'

# ----------------------------------------
# queries
# ----------------------------------------

#add createdOn field
comp_query = {
   "selector": {
      "type": {
         "$eq": "component"
      },
      "releaseIds": {
         "$size": 0
      },
      "createdOn": {
         "$lte": bufferDate
      }
   },
   "limit": 999999
}

proj_query = {
   "selector": {
      "type": {
         "$eq": "project"
      }
   },
   "fields": [
      "releaseIdToUsage",
      "_id"
   ],
   "limit": 999999
}

release_query = {
   "selector": {
      "type": {
         "$eq": "release"
      },
      "createdOn": {
         "$lte": bufferDate
      },
      "clearingState": {
         "$eq": "NEW_CLEARING"
      }
   },
   "limit": 999999
}

# ---------------------------------------
# functions
# ---------------------------------------

def deleteCompWithoutReleases(log, comp_data_list):
    log['Component Ids'] = []
    print ('Total Components without any releases')
    print(len(comp_data_list))
    print ('\n')

    #deleting the components without any releases
    for component in comp_data_list:
        log['Component Ids'].append(component[ID])
        if not DRY_RUN:
            sw360Db.delete(component)

def deleteReleasesNotLinkedToProjects(log, project_data_list, release_data_list):
    log['Release Ids'] = []
    releaseIds_set = set()
    totalReleaseIds_set = set()
    r_set = set()
    unlinkedIds_set = set()

    #getting list of total linked releases till date
    for project in project_data_list:
        if (project.get(RELEASE_IDS) and len(project[RELEASE_IDS]) > 0):
            for iteration, item in enumerate(project[RELEASE_IDS]):
                releaseIds_set.add(item)
    print ('Total Releases linked to Project(s) till date')
    print(len(releaseIds_set))
    releaseIds_list = list(releaseIds_set)

    #getting list of releases based on the createdOn field
    print ('Total Releases created before ' + bufferDate)
    print(len(release_data_list))

    #getting list of linked releases based on the createdOn field
    for release in release_data_list:
        r_set.add(release[ID])
        for r_id in releaseIds_list:
            if (release[ID] == r_id):
                totalReleaseIds_set.add(release[ID])
    print('Total Releases created before ' + bufferDate + ' that are linked to Project(s)')
    print(len(totalReleaseIds_set))

    #getting list of unlinked releases based on the createdOn field
    if(r_set ^ totalReleaseIds_set):
        unlinkedIds_set = (r_set ^ totalReleaseIds_set)
    print ('Total Releases created before ' + bufferDate + ' that are not linked to Project(s)')
    unlinkedIds_list = list(unlinkedIds_set)
    print(len(unlinkedIds_list))
    log['Release Ids'].append(unlinkedIds_list)

    #deleting the releaseIds that are not linked to any project based on the createdOn filed
    for rId in release_data_list:
        for releaseId in unlinkedIds_list:
            if (releaseId == rId[ID]):
                print('deleting ' + releaseId)

                if not DRY_RUN:
                    sw360Db.delete(rId)

def run():
    log = {}
    logFile = open('componentsAndReleases.log', 'w')

    print ('Script will help us to get and delete the following Ids :')
    print ('1. Ids of component that does not have any releases')
    print ('2. Ids of releases that are not linked to any projects')
    print ('\n')
    
    comp_data = sw360Db.find(comp_query)
    comp_data_list = list(comp_data)
    print('query done for components')
    deleteCompWithoutReleases(log, comp_data_list)

    project_data = sw360Db.find(proj_query)
    project_data_list = list(project_data)
    print('query done for projects')
    release_data = sw360Db.find(release_query)
    release_data_list = list(release_data)
    print('query done for releases')
    deleteReleasesNotLinkedToProjects(log, project_data_list, release_data_list)
    
    print ('\n')

    json.dump(log, logFile, indent = 4, sort_keys = True)
    logFile.close()

    print ('\n')
    print ('------------------------------------------')
    print ('Please check log file "componentsAndReleases.log" in this directory for details')

# --------------------------------

startTime = time.time()
run()
print ('\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's')

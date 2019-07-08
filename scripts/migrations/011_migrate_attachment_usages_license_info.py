#!/usr/bin/python
# -----------------------------------------------------------------------------
# Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
#
# All rights reserved.   This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# This is a manual database migration script. It is assumed that a
# dedicated framework for automatic migration will be written in the
# future. When that happens, this script should be refactored to conform
# to the framework's prerequisites to be run by the framework. For
# example, server address and db name should be parametrized, the code
# reorganized into a single class or function, etc.
# -----------------------------------------------------------------------------

import time
import couchdb
import json

# ---------------------------------------
# constants
# ---------------------------------------

DRY_RUN = True

COUCHSERVER = "http://localhost:5984/"
DBNAME = 'sw360db'

couch = couchdb.Server(COUCHSERVER)
db = couch[DBNAME]

# ----------------------------------------
# queries
# ----------------------------------------

projects_all_query = '''function(doc){
    if (doc.type == "project"){
        emit(doc._id, doc)
    }
}'''

releases_all_query = '''function(doc){
    if (doc.type == "release"){
        emit(doc._id, doc)
    }
}'''

license_infos_all_query = '''function(doc){
    if (doc.type == "attachmentUsage" && doc.usageData != null && doc.usageData.setField_ == "LICENSE_INFO"){
        emit(doc._id, doc)
    }
}'''

# ---------------------------------------
# functions
# ---------------------------------------

# ATTENTION:
# - recursion in path
# - moderation requests
# -

def run():
    log = {}
    log['count'] = 0
    log['success'] = list()
    log['norelease'] = list()
    log['noproject'] = list()
    log['nolinkedreleases'] = list()
    log['nolinkedprojects'] = list()

    print 'Getting all projects'
    projects_all = {}
    for projectRow in db.query(projects_all_query):
        projects_all[projectRow.key] = projectRow.value
    print 'Received ' + str(len(projects_all)) + ' projects'

    print 'Getting all releases'
    releases_all = {}
    for releaseRow in db.query(releases_all_query):
        releases_all[releaseRow.key] = releaseRow.value
    print 'Received ' + str(len(releases_all)) + ' releases'

    print 'Getting all license infos'
    license_infos_all = db.query(license_infos_all_query)
    print 'Received ' + str(len(license_infos_all)) + ' license infos'

    print '\n'
    for licenseInfoRow in license_infos_all:
        log['count'] += 1
        migrateLicenseInfo(log, licenseInfoRow.value, projects_all, releases_all)
        if log['count'] % 1000 == 0:
            print 'Checked ' + str(log['count']) + ' license infos'
        #if log['count'] >= 500:
        #    break

    resultFile = open('011_migration.log', 'w')
    json.dump(log, resultFile, indent = 4, sort_keys = True)
    resultFile.close()

    print '\n'
    print '------------------------------------------'
    print 'License infos successfully migrated: ' + str(len(log['success']))
    print 'License infos with not existing releaseId: ' + str(len(log['norelease']))
    print 'License infos with not existing projectId: ' + str(len(log['noproject']))
    print 'Total license infos with known known reason for outcome (might be less then total number of license infos though): ' + str(len(log['success']) + len(log['norelease']) + len(log['noproject']))
    print '------------------------------------------'
    print 'Projects without releaseIdToUsage field: ' + str(len(log['nolinkedreleases']))
    print 'Projects without linkedProjects field: ' + str(len(log['nolinkedprojects']))
    print '------------------------------------------'

def migrateLicenseInfo(log, licenseInfo, projects, releases):
    licenseInfoId = licenseInfo['_id']
    releaseId = licenseInfo['owner']['value_']
    projectId = licenseInfo['usedBy']['value_']

    if releaseId in releases:
        release = releases[releaseId]
    else:
        log['norelease'].append({ licenseInfoId: releaseId })
        return None

    projectPath = createProjectPathForReleaseIdInProject(log, projectId, licenseInfoId, releaseId, projectId, projects)

    if projectPath is not None:
        licenseInfo['usageData']['value_']['projectPath'] = projectPath
        log['success'].append(licenseInfoId)
        if not DRY_RUN:
            db.save(licenseInfo)

def createProjectPathForReleaseIdInProject(log, projectPath, licenseInfoId, releaseId, projectId, projects):
    if projectId in projects:
        project = projects[projectId]
    else:
        log['noproject'].append({ licenseInfoId: projectId })
        return None

    if 'releaseIdToUsage' in project:
        linkedReleases = project['releaseIdToUsage']
    else:
        log['nolinkedreleases'].append(projectId)

    if linkedReleases is not None and releaseId in linkedReleases.keys():
        return projectPath
    else:
        if 'linkedProjects' in project:
            linkedProjects = project['linkedProjects']

            if len(linkedProjects) > 0:
                for linkedProject in linkedProjects.keys():
                    if linkedProject not in projectPath:
                        tempPath = createProjectPathForReleaseIdInProject(log, projectPath + ':' + linkedProject, licenseInfoId, releaseId, linkedProject, projects)
                        if tempPath is not None:
                            return tempPath
                    else:
                        print str(log['count']) + ': Project dependency cycle detected!'
                        return None
            else:
                return None
        else:
            log['nolinkedprojects'].append(projectId)
            return None

# --------------------------------

startTime = time.time()
run()
print '\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's'

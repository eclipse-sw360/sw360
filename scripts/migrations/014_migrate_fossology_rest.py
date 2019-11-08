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
import datetime
import couchdb
import json


# ---------------------------------------
# constants
# ---------------------------------------

DRY_RUN = True

COUCHSERVER = "http://localhost:5984/"
DBNAME = 'sw360db'
DBNAME_DELETE = 'sw360fossologykeys'

couch = couchdb.Server(COUCHSERVER)
db = couch[DBNAME]


# ----------------------------------------
# queries
# ----------------------------------------

releases_all_query = '''function(doc){
    if (doc.type == "release"){
        emit(doc._id, doc)
    }
}'''


# ---------------------------------------
# functions
# ---------------------------------------

def run():
    log = {}
    log['count'] = 0
    log['success-externaltool'] = list()
    log['success-clearingstate'] = list()
    log['exceptions'] = list()
    log['untouched'] = list()
    
    print 'Deleting old fossology ssh keys database'
    deleteDatabase(DBNAME_DELETE)

    print 'Getting all releases'
    releases_all = db.query(releases_all_query)
    print 'Received ' + str(len(releases_all)) + ' releases'

    print '\n'
    for releaseRow in releases_all:
        log['count'] += 1
        migrateRelease(log, releaseRow.value)
        if log['count'] % 500 == 0:
            print 'Checked ' + str(log['count']) + ' releases'
        #if log['count'] > 500:
        #    break

    resultFile = open('014_migration.log', 'w')
    json.dump(log, resultFile, indent = 4, sort_keys = True)
    resultFile.close()

    print '\n'
    print '------------------------------------------'
    print 'Releases successfully migrated external tool requests: ' + str(len(log['success-externaltool']))
    print 'Releases successfully migrated clearing states (SENT_TO_FOSSOLOGY): ' + str(len(log['success-clearingstate']))
    print 'Releases not migrated because of errors: ' + str(len(log['exceptions']))
    print 'Releases untouched: ' + str(len(log['untouched']))
    print '------------------------------------------'
    print 'Please check log file "014_migration.log" in this directory for details'
    print '------------------------------------------'


def deleteDatabase(dbname):
    if dbname in couch:
        if not DRY_RUN:
            del couch[dbname]
        else:
            print 'Found database with name ' + dbname + ' which would now be deleted without dry run!'
    else:
        print 'No database with name ' + dbname + ' found, so nothing to delete!'


def migrateRelease(log, release):
    touched = False
    releaseId = release['_id']

    try:
        if 'externalToolRequests' in release:
            release['externalToolProcesses'] = list()

            for externalToolRequest in release['externalToolRequests']:
                if externalToolRequest['externalTool'] == 'FOSSOLOGY':
                    externalToolProcess = {}
                    externalToolProcess['externalTool'] = 'FOSSOLOGY'
                    externalToolProcess['attachmentId'] = externalToolRequest['attachmentId']
                    externalToolProcess['attachmentHash'] = externalToolRequest['attachmentHash']
                    externalToolProcess['processStatus'] = 'IN_WORK'
                    externalToolProcess['processSteps'] = list()
                    # externalToolProcess['processIdInTool'] not needed for FOSSology

                    processStep = {}
                    processStep['stepName'] = '01_upload'
                    processStep['stepStatus'] = 'DONE'
                    processStep['startedBy'] = externalToolRequest['createdBy']
                    processStep['startedByGroup'] = externalToolRequest['createdByGroup']
                    processStep['startedOn'] = externalToolRequest['createdOn']
                    processStep['processStepIdInTool'] = externalToolRequest['toolId']
                    processStep['finishedOn'] = datetime.datetime.now().strftime('%Y-%m-%dT%H:%M:%SZ')
                    processStep['result'] = externalToolRequest['toolId']
                    # processStep['linkToStep'] not needed for FOSSology
                    # processStep['userIdInTool'] not needed for FOSSology
                    # processStep['userCredentialsInTool'] not needed for FOSSology

                    externalToolProcess['processSteps'].append(processStep)
                    release['externalToolProcesses'].append(externalToolProcess)

                    # there might have been more than 1 fossology process since they were separated by team,
                    # but now only one per release makes sense, so we should migrate at most one
                    break

            del release['externalToolRequests']

            touched = True
            log['success-externaltool'].append(str(release) + '\n\n')


        if 'clearingState' in release:
            if release['clearingState'] == 'SENT_TO_FOSSOLOGY':
                release['clearingState'] = 'SENT_TO_CLEARING_TOOL'

                touched = True
                log['success-clearingstate'].append(str(release) + '\n\n')

        if not DRY_RUN:
            db.save(release)
        #else:
            #print release

    except Exception as exc:
        touched = False
        log['exceptions'].append(str(exc))

    if touched == False:
        log['untouched'].append(releaseId)


# --------------------------------

startTime = time.time()
run()
print '\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's'

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

releases_all_query = '''function(doc){
    if (doc.type == "release"){
        emit(doc._id, doc)
    }
}'''


# ---------------------------------------
# functions
# ---------------------------------------

# ATTENTION:
# - moderation requests

def run():
    log = {}
    log['count'] = 0
    log['success'] = list()
    log['exceptions'] = list()
    log['untouched'] = list()

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

    resultFile = open('013_migration.log', 'w')
    json.dump(log, resultFile, indent = 4, sort_keys = True)
    resultFile.close()

    print '\n'
    print '------------------------------------------'
    print 'Releases successfully migrated: ' + str(len(log['success']))
    print 'Releases not migrated because of errors: ' + str(len(log['exceptions']))
    print 'Releases untouched: ' + str(len(log['untouched']))
    print 'Total releases with known reason for outcome: ' + str(len(log['success']) + len(log['exceptions']) + len(log['untouched']))
    print '------------------------------------------'
    print 'Please check log file "013_migration.log" in this directory for details'
    print '------------------------------------------'

def migrateRelease(log, release):
    releaseId = release['_id']

    if 'fossologyId' in release or 'attachmentInFossology' in release or 'clearingTeamToFossologyStatus' in release:

        try:
            if 'fossologyId' in release and 'attachmentInFossology' in release and 'clearingTeamToFossologyStatus' in release:

                release['externalToolRequests'] = list()

                for clearingTeam, state in release['clearingTeamToFossologyStatus'].iteritems():
                    externalToolRequest = {}

                    externalToolRequest['externalTool'] = 'FOSSOLOGY'
                    externalToolRequest['createdOn'] = release['createdOn'] + 'T00:00:00Z'
                    externalToolRequest['createdBy'] = release['createdBy']
                    externalToolRequest['toolId'] = release['fossologyId']
                    externalToolRequest['attachmentId'] = release['attachmentInFossology']
                    externalToolRequest['attachmentHash'] = findAttachmentHash(release)
                    externalToolRequest['createdByGroup'] = clearingTeam
                    externalToolRequest['toolUserGroup'] = clearingTeam
                    mapFossologyStatus(state, externalToolRequest)

                    # currently not used for fossology flow:
                    # externalToolRequest['id'] =
                    # externalToolRequest['toolUserId'] =
                    # externalToolRequest['linkToJob'] =

                    release['externalToolRequests'].append(externalToolRequest)
            else:
                raise Exception('Not all necessary attributes (fossologyId, attachmentInFossology, clearingTeamToFossologyStatus) set in release with id {}.'.format(release['_id']))

            # remove obsolete attributes

            if 'attachmentInFossology':
                del release['attachmentInFossology']

            if 'fossologyId' in release:
                del release['fossologyId']

            if 'clearingTeamToFossologyStatus' in release:
                del release['clearingTeamToFossologyStatus']

            log['success'].append(str(release) + '\n\n')

            if not DRY_RUN:
                db.save(release)
            #else:
                #print release
        except Exception as exc:
            log['exceptions'].append(str(exc))

    else:
        log['untouched'].append(releaseId)

def findAttachmentHash(release):
    for attachment in release['attachments']:
        if attachment['attachmentContentId'] == release['attachmentInFossology']:
            if 'sha1' in attachment:
                return attachment['sha1']
            else:
                raise Exception('No sha1 in attachment for content id {} found in release with id {}.'.format(release['attachmentInFossology'], release['_id']))
    raise Exception('No attachment for content id {} found in release with id {}.'.format(release['attachmentInFossology'], release['_id']))

def mapFossologyStatus_CONNECTION_FAILED_0(externalToolRequest):
    externalToolRequest['externalToolWorkflowStatus'] = 'CONNECTION_FAILED'
    externalToolRequest['externalToolStatus'] = 'OPEN'

def mapFossologyStatus_ERROR_1(externalToolRequest):
    externalToolRequest['externalToolWorkflowStatus'] = 'SERVER_ERROR'
    externalToolRequest['externalToolStatus'] = 'OPEN'

def mapFossologyStatus_NON_EXISTENT_2(externalToolRequest):
    externalToolRequest['externalToolWorkflowStatus'] = 'NOT_FOUND'
    externalToolRequest['externalToolStatus'] = 'OPEN'

def mapFossologyStatus_NOT_SENT_3(externalToolRequest):
    externalToolRequest['externalToolWorkflowStatus'] = 'NOT_SENT'
    externalToolRequest['externalToolStatus'] = 'OPEN'

def mapFossologyStatus_INACCESSIBLE_4(externalToolRequest):
    externalToolRequest['externalToolWorkflowStatus'] = 'ACCESS_DENIED'
    externalToolRequest['externalToolStatus'] = 'OPEN'

def mapFossologyStatus_SENT_10(externalToolRequest):
    externalToolRequest['externalToolWorkflowStatus'] = 'SENT'
    externalToolRequest['externalToolStatus'] = 'OPEN'

def mapFossologyStatus_SCANNING_11(externalToolRequest):
    externalToolRequest['externalToolWorkflowStatus'] = 'SENT'
    externalToolRequest['externalToolStatus'] = 'IN_PROGRESS'

def mapFossologyStatus_OPEN_20(externalToolRequest):
    externalToolRequest['externalToolWorkflowStatus'] = 'SENT'
    externalToolRequest['externalToolStatus'] = 'OPEN'

def mapFossologyStatus_IN_PROGRESS_21(externalToolRequest):
    externalToolRequest['externalToolWorkflowStatus'] = 'SENT'
    externalToolRequest['externalToolStatus'] = 'IN_PROGRESS'

def mapFossologyStatus_CLOSED_22(externalToolRequest):
    externalToolRequest['externalToolWorkflowStatus'] = 'SENT'
    externalToolRequest['externalToolStatus'] = 'CLOSED'

def mapFossologyStatus_REJECTED_23(externalToolRequest):
    externalToolRequest['externalToolWorkflowStatus'] = 'SENT'
    externalToolRequest['externalToolStatus'] = 'REJECTED'

def mapFossologyStatus_REPORT_AVAILABLE_30(externalToolRequest):
    externalToolRequest['externalToolWorkflowStatus'] = 'SENT'
    externalToolRequest['externalToolStatus'] = 'RESULT_AVAILABLE'


def mapFossologyStatus(fossologyStatus, externalToolRequest):
    switcher = {
        'CONNECTION_FAILED': mapFossologyStatus_CONNECTION_FAILED_0,
        'ERROR': mapFossologyStatus_ERROR_1,
        'NON_EXISTENT': mapFossologyStatus_NON_EXISTENT_2,
        'NOT_SENT': mapFossologyStatus_NOT_SENT_3,
        'INACCESSIBLE': mapFossologyStatus_INACCESSIBLE_4,
        'SENT': mapFossologyStatus_SENT_10,
        'SCANNING': mapFossologyStatus_SCANNING_11,
        'OPEN': mapFossologyStatus_OPEN_20,
        'IN_PROGRESS': mapFossologyStatus_IN_PROGRESS_21,
        'CLOSED': mapFossologyStatus_CLOSED_22,
        'REJECTED': mapFossologyStatus_REJECTED_23,
        'REPORT_AVAILABLE': mapFossologyStatus_REPORT_AVAILABLE_30
    }
    func = switcher.get(fossologyStatus, lambda: "Invalid status")
    func(externalToolRequest)


# --------------------------------

startTime = time.time()
run()
print '\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's'

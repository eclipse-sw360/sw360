#!/usr/bin/python
# -----------------------------------------------------------------------------
# Copyright TOSHIBA CORPORATION, 2022. Part of the SW360 Portal Project.
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
# ------------------------------------------------------------------------------

import time
import couchdb
import json
from webbrowser import get

# ---------------------------------------
# constants
# ---------------------------------------

DRY_RUN = True

COUCHSERVER = 'http://localhost:5984/'
DBNAME = 'sw360db'

couch=couchdb.Server(COUCHSERVER)
db = couch[DBNAME]

# oldField
deleteFieldName = "releaseIdToUsage"

# newField
newFieldName = "releaseRelationNetwork"

log = {}
log['Error updated project'] = []
log['Success updated project'] = []
log['Project will be update with DRY RUN'] = []
# query get all project with field "releaseIdToUsage"
get_projects_with_releaseIdToUsage_field_query = {"selector": {"type": {"$eq": "project"}, deleteFieldName: {"$exists": True}}, "limit": 100000}

def get_children_node(id, createBy, createOn):
    get_release_by_id = {"selector":{"type": {"$eq":"release"}, "_id":{"$eq":id}}}
    release_by_id = db.find(get_release_by_id)[0]

    children_nodes = []
    if len(release_by_id) > 0:
        if release_by_id.get('releaseIdToRelationship') != None:
            for release_id, release_relation in release_by_id.get('releaseIdToRelationship').items():
                node = {
                    'releaseId': release_id,
                    'releaseRelationship': release_relation,
                    'mainlineState': 'OPEN',
                    'createOn': createOn,
                    'createBy': createBy,
                    'comment': '',
                    'releaseLink': get_children_node(release_id, createBy, createOn)
                }
                children_nodes.append(node)

    return children_nodes


def applyTranferData(project, log):
    updatedDocId = {}
    updatedDocId['id'] = project.get('_id')
    if not DRY_RUN:
        db.save(project)
        print 'Removing field name ' + deleteFieldName + ' and add field ' + newFieldName + ' done for project ' + project.get('_id')
        log['Success updated project'].append(updatedDocId)
    else:
        log['Project will be update with DRY RUN'].append(updatedDocId)


def run():
    print 'Getting all project with field releaseIdToUsage'
    project_with_releaseIdToUsage_field = db.find(get_projects_with_releaseIdToUsage_field_query)

    for project in project_with_releaseIdToUsage_field:
        print 'migrating for project: ' + project["_id"]
        dependency_network = []
        try:
            for release_id, relation_with_project in project.get('releaseIdToUsage').items():
                createOn = relation_with_project.get('createdOn')
                createBy = relation_with_project.get('createdBy')
                node = {
                    'releaseId': release_id,
                    'releaseRelationship':relation_with_project.get('releaseRelation'),
                    'mainlineState': relation_with_project.get('mainlineState'),
                    'createOn': createOn,
                    'createBy': createBy,
                    'comment': relation_with_project.get('comment'),
                    'releaseLink': get_children_node(release_id, createBy, createOn)
                }
                dependency_network.append(node)
            project['releaseRelationNetwork'] = json.dumps(dependency_network, separators=(',', ':'))

            del project['releaseIdToUsage']

            applyTranferData(project, log)
        except:
            log['Error updated project'].append({"id": project['_id']})
# --------------------------------

def writeLog():
    logFile = open('054_migrate_project_dependency_network.log', 'w')
    json.dump(log, logFile, indent = 4, sort_keys = True)
    logFile.close()
    print ('\n')
    print ('------------------------------------------')
    print ('Please check log file "054_migrate_project_dependency_network.log" in this directory for details')
    print ('------------------------------------------')


startTime = time.time()
run()
writeLog()
print '\nTime of migration: ' + "{0:.2f}".format(time.time() - startTime) + 's'


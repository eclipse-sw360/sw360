#!/usr/bin/python
# -----------------------------------------------------------------------------
# Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
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
#
# initial author: alex.borodin@evosoft.com
#
# -----------------------------------------------------------------------------

import couchdb

DRY_RUN = False

REQUESTING_USER = 'requestingUser'

COUCHSERVER = "http://localhost:5984/"
DBNAME = 'sw360db'

couch=couchdb.Server(COUCHSERVER)
db = couch[DBNAME]

RELEASE_USAGE = 'releaseIdToUsage'
PRJ_NAME = 'name'
ID = '_id'
PRJ_RESPONSIBLE = 'projectResponsible'
PROJECT_ADDITIONS = 'projectAdditions'
PROJECT_DELETIONS = 'projectDeletions'
DOCUMENT_NAME = 'documentName'

projects_with_linked_releases_fun = '''function(doc){
    if (doc.type=="project" && doc.releaseIdToUsage){
        emit(doc._id, doc)
    }
}'''

moderations_with_project_additions_fun = '''function(doc){
    if (doc.type=="moderation" && doc.projectAdditions && doc.projectAdditions.releaseIdToUsage){
        emit(doc._id, doc)
    }
}'''

moderations_with_project_deletions_fun = '''function(doc){
    if (doc.type=="moderation" && doc.projectDeletions && doc.projectDeletions.releaseIdToUsage){
        emit(doc._id, doc)
    }
}'''

def convert_to_project_release_relation(relation, context_log_message):
    if isinstance(relation, basestring):
        release_relation = 'UNKNOWN'
        lowercase_rel = relation.lower()
        if lowercase_rel == 'contained' or lowercase_rel == 'contains':
            release_relation = 'CONTAINED'
        elif lowercase_rel == 'refers' or lowercase_rel == 'referred':
            release_relation = 'REFERRED'
        elif lowercase_rel != '':
            print (u'WARN: %s. relation "%s" is not recognizable and has been converted to UNKNOWN' % (context_log_message, relation)).encode('utf-8')
        mainline_state = 'OPEN'
        prr = {'releaseRelation': release_relation, 'mainlineState': mainline_state}
        return prr
    else:
        return relation

def convert_release_id_to_usage_dict(relations, context_log_message):
    changed = False
    for release_id in relations:
        oldval = relations[release_id]
        newval = convert_to_project_release_relation(oldval, context_log_message)
        if oldval != newval:
            print (u'INFO: %s. releaseId:%s, old value:"%s", new value: "%s"' % (context_log_message, release_id, oldval, newval)).encode('utf-8')
            relations[release_id] = newval
            changed = True

    return changed

def convert_project_release_usages():
    print 'Converting project release usages'
    projects_with_linked_releases = db.query(projects_with_linked_releases_fun)
    for project_row in projects_with_linked_releases:
        project = project_row.value
        context_log_message = (u'project "%s" [id:%s, responsible:%s]' % (
            project[PRJ_NAME], project[ID], project[PRJ_RESPONSIBLE] or '<not set>'))
        changed = convert_release_id_to_usage_dict(project[RELEASE_USAGE], context_log_message)
        if changed:
            if DRY_RUN:
                print 'INFO: not saving - DRY_RUN'
            else:
                db.save(project)
                print 'INFO: saved project'

    print 'Done with projects.'

convert_project_release_usages()

# --------------------------------
def convert_moderation_project_additions():
    print 'Converting moderation project additions release usage'
    moderations_with_project_additions = db.query(moderations_with_project_additions_fun)
    for moderation_row in moderations_with_project_additions:
        moderation = moderation_row.value
        project_additions = moderation[PROJECT_ADDITIONS]
        context_log_message = (u'moderation request with addtions for "%s" [id:%s, requestor:%s]' % (
            moderation[DOCUMENT_NAME], moderation[ID], moderation[REQUESTING_USER])).encode('utf-8')
        changed = convert_release_id_to_usage_dict(project_additions[RELEASE_USAGE], context_log_message)
        if changed:
            if DRY_RUN:
                print 'INFO: not saving moderation - DRY_RUN'
            else:
                db.save(moderation)
                print 'INFO: saved moderation'

    print 'Done with moderation project addition.'

convert_moderation_project_additions()

# --------------------------------
def convert_moderation_project_deletions():
    print 'Converting moderation project deletions release usage'
    moderations_with_project_deletions = db.query(moderations_with_project_deletions_fun)
    for moderation_row in moderations_with_project_deletions:
        moderation = moderation_row.value
        project_deletions = moderation[PROJECT_DELETIONS]
        context_log_message = (u'moderation request with deletions for "%s" [id:%s, requestor:%s]' % (
            moderation[DOCUMENT_NAME], moderation[ID], moderation[REQUESTING_USER])).encode('utf-8')
        changed = convert_release_id_to_usage_dict(project_deletions[RELEASE_USAGE], context_log_message)
        if changed:
            if DRY_RUN:
                print 'INFO: not saving moderation - DRY_RUN'
            else:
                db.save(moderation)
                print 'INFO: saved moderation'

    print 'Done with moderation project deletion.'

convert_moderation_project_deletions()

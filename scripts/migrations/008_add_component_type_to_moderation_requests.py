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
# initial author: christoph.niehoff@tngtech.com
#
# -----------------------------------------------------------------------------
import couchdb

# Constants
TYPE = 'type'

DOCUMENT_TYPE_RELEASE = 'release'
DOCUMENT_TYPE_COMPONENT = 'component'
DOCUMENT_TYPE_MODERATION = 'moderation'

PARENT_COMPONENT_ID = 'componentId'

COMPONENT = 'COMPONENT'
RELEASE = 'RELEASE'

COMPONENT_TYPE = 'componentType'
MODERATION_TYPE = 'documentType'

COMPONENT_ID = 'documentId'
RELEASE_ID = 'documentId'


# Set up connection to CouchDB
COUCHSERVER = "http://localhost:5984/"
DBNAME      = 'sw360db'
couch = couchdb.Server(COUCHSERVER)
db    = couch[DBNAME]


# Define functions
def updateDocument(db, doc, newDict):
    doc.update(newDict)
    db.save(doc)

# Get Lists of Components, Releases and Moderations
get_all_components_fun = '''function(doc){
        if (doc.type=='component') {
            emit(doc._id, doc)
        }
    }'''

get_all_releases_fun = '''function(doc){
        if (doc.type=='release') {
            emit(doc._id, doc)
        }
    }'''

get_all_moderation_requests_fun = '''function(doc){
        if (doc.type=='moderation') {
            emit(doc._id, doc)
        }
    }'''

allComponents         = db.query(get_all_components_fun)
allReleases           = db.query(get_all_releases_fun)
allModerationRequests = db.query(get_all_moderation_requests_fun)


print "Retrieve component and release data"
# Create dict for Component data
componentData = {}
for component in allComponents:
    document = component.value
    if COMPONENT_TYPE in document.keys():
        componentData[component.id] = document[COMPONENT_TYPE]

# Create dict for Release data
releaseData = {}
for release in allReleases:
    document = release.value
    if PARENT_COMPONENT_ID in document.keys():
        if document[PARENT_COMPONENT_ID] in componentData.keys():
            releaseData[release.id] = componentData[document[PARENT_COMPONENT_ID]]


print "add component types to moderation requests in the database"
for request in allModerationRequests:
    document = request.value
    if not COMPONENT_TYPE in document.keys():
        if MODERATION_TYPE in document.keys():

            if (document[MODERATION_TYPE] == COMPONENT) and (COMPONENT_ID in document.keys()):
                componentID = document[COMPONENT_ID]
                if componentID in componentData.keys():
                    componentType = componentData[componentID]
                    updateDocument(db, document, {COMPONENT_TYPE: componentType})

            elif (document[MODERATION_TYPE] == RELEASE) and (RELEASE_ID in document.keys()):
                releaseID = document[RELEASE_ID]
                if releaseID in releaseData.keys():
                    componentType = releaseData[releaseID]
                    updateDocument(db, document, {COMPONENT_TYPE: componentType})

print "done."


#!/usr/bin/python
# -----------------------------------------------------------------------------
# Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
#
# All rights reserved.   This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# This script finds all objects that have attachments, but do not have the SHA1
# digest saved in the attachment metadata, and calculates and savesthis missing
# data.
#
# Default option is to perform a dry run. Change the DRY_RUN constant to False
# to actually save the changes to the database
#
# author: alex.borodin@evosoft.com
#
# -----------------------------------------------------------------------------
DRY_RUN = True

import couchdb
import hashlib


COUCHSERVER = "http://localhost:5984/"
DBNAME = 'sw360db'
ATTDBNAME = 'sw360attachments'

couch=couchdb.Server(COUCHSERVER)
db = couch[DBNAME]
attdb = couch[ATTDBNAME]

FIELD_SHA1 = 'sha1'
FIELD_ATTACHMENTS = 'attachments'
FIELD_NAME = 'name'
FIELD_VERSION = 'version'
FIELD_TYPE = 'type'
FIELD_ID = 'id'
FIELD_ATT_CONTENT_ID = 'attachmentContentId'
FIELD_FILENAME = 'filename'
FIELD_PARTS_COUNT = 'partsCount'

objects_with_attachments_without_sha1_fun = '''function(doc){
    if (doc.attachments){
        var has_missing_sha1 = false;
        for(var i=0; i<doc.attachments.length; i++){
            if (!doc.attachments[i].sha1){
                has_missing_sha1 = true;
                break;
            }
        }
        if (has_missing_sha1){
            emit(doc._id, doc)
        }
    }
}'''

def fix_sha1_in_attachment_list(attachments, log_msg):
    changed = False
    for attachment in attachments:
        if FIELD_SHA1 in attachment:
            continue
        if not FIELD_ATT_CONTENT_ID in attachment:
            print (u'WARN: %s - attachmentContentId not found for attachment %s. Skipping' % (log_msg, attachment[FIELD_FILENAME] if FIELD_FILENAME in attachment else u'<not set>' )).encode('utf-8')
            continue
        att_cont_id = attachment[FIELD_ATT_CONTENT_ID]

        try:
            att_content = attdb[att_cont_id]
        except:
            print (u'WARN: %s - attachment with id [%s] not found in the attachment database. Skipping' % (log_msg, att_cont_id)).encode('utf-8')
            continue

        if FIELD_PARTS_COUNT in att_content:
            hasher = hashlib.sha1()
            all_parts_found = True

            for part in range(1, int(att_content[FIELD_PARTS_COUNT]) + 1):
                att_file = attdb.get_attachment(att_cont_id, attachment[FIELD_FILENAME] + '_part' + str(part))
                if att_file is None:
                    print (u'WARN: %s - attachment part %d of attachment id [%s] not found in the attachment database. Skipping' % (log_msg, part, att_cont_id)).encode('utf-8')
                    all_parts_found = False
                    break

                buf = att_file.read()
                hasher.update(buf)
                att_file.close()

            if not all_parts_found:
                continue
            hash = hasher.hexdigest()
            attachment[FIELD_SHA1] = hash
            changed = True
            print (u'INFO: %s - fixed attachment sha1 [%s]=%s' % (log_msg, attachment[FIELD_FILENAME], hash)).encode('utf-8')
        else:
            print (u'INFO: %s - attachment [%s] has no partsCount. Setting fake sha1' % (log_msg, att_cont_id)).encode('utf-8')
            attachment[FIELD_SHA1] = '<not-set>' + att_cont_id
            changed = True

    return changed




def fix_sha1_in_all_objects():
    print 'Fixing missing sha1 hashes'
    broken_objects = db.query(objects_with_attachments_without_sha1_fun)
    for row in broken_objects:
        obj = row.value
        context_log_message = (u'object [id:%s, name:%s, version:%s, type:%s]' % (
             row.id, obj[FIELD_NAME] if FIELD_NAME in obj else "", obj[FIELD_VERSION] if FIELD_VERSION in obj else "", obj[FIELD_TYPE] if FIELD_TYPE in obj else ""))
        changed = fix_sha1_in_attachment_list(obj[FIELD_ATTACHMENTS], context_log_message)
        if changed:
            if DRY_RUN:
                print 'INFO: not saving - DRY_RUN'
            else:
                db.save(obj)
                print 'INFO: saved object'

    print 'Done'

fix_sha1_in_all_objects()


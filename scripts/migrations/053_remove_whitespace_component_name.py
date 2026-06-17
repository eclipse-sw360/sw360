#!/usr/bin/python
# -----------------------------------------------------------------------------
# Copyright Siemens AG, 2022. Part of the SW360 Portal Project.
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
# This script is for removing the trailing and leading whitespaces in component's name
# ---------------------------------------------------------------------------------------------------------------------------------------------------------------

from logging import exception
import couchdb
import pandas as pd
import json
import time

#Constants
DRY_RUN = True

COUCHSERVER = "http://localhost:5984/"
DBNAME = 'sw360db'

def dbConnection():
    print('Connecting to local couchdb stage server....../')
    couch = couchdb.Server(COUCHSERVER)
    return couch[DBNAME]

def queryExecution(db,type):
    print(f'Executing the {type} whitespace query.................../')
    db_query = db.find({"selector":{"type": type,"name": {"$regex": "(^\\s.+)|(.+\\s$)"}},"limit":999999999999999})
    if bool(db_query):
        return list(db_query)
    else:
        return []

def updateDB(db,db_copy_list):
    print('Correcting the component/release names and updating the database......../')

    df = pd.DataFrame(db_copy_list)
    df['name'] =  df['name'].str.strip()   
    df = df.fillna('')    
    db_df_list = df.to_dict('records')
    if not DRY_RUN:
        #Update call
        db.update(db_df_list)

    return db_df_list

def generateLogFile(header, db_copy_list):
    print('Generating log file......../')
    with open("_053_remove_whitespace_component_name.log", "a") as f:
        f.write(header)
    keys_to_extract = ['_id', 'name']
    result = [{key: d[key] for key in keys_to_extract if key in d} for d in db_copy_list]
    logFile = open('_053_remove_whitespace_component_name.log', 'a')
    json.dump(result, logFile, indent = 4, sort_keys = True)
    logFile.close()

def checkDuplicate(db, db_copy_list):
    df = pd.DataFrame(db_copy_list)
    df['name'] =  df['name'].str.strip()   
    df = df.fillna('')    
    results = []
    query = {"selector": {"type": {"$eq": "component"},"$or": [{"name": name} for name in df['name']]},"limit": 99999}
    results = db.find(query)
    return list(results)

def checkDuplicateReleases(db, db_copy_list):
    print('Checking duplicate releases......./')
    df = pd.DataFrame(db_copy_list)
    df['name'] =  df['name'].str.strip()   
    df = df.fillna('') 
    existing_release_list = []
    query = {"selector": {"type": {"$eq": "release"},"$or": [{"name": name} for name in df['name']]},"limit": 99999}
    existing_release_list = db.find(query)
    db_copy_list = df.to_dict('records')
    dup_rel_list = []
    for rel in list(existing_release_list):
        for x in db_copy_list:
            if(rel['name']==x['name'] and rel['componentId']==x['componentId'] and rel['version']==x['version']):
                dup_rel_list.append(x)
    return dup_rel_list

def linkReleaseToComponent(db, db_corrected_comp_list, db_existing_comp_list):

    print('######################')
    print('Linking Releases to Components........./')

    db_duplicate_corrected_comp_list = filter(lambda x: x['name'] in [d['name'] for d in db_existing_comp_list], db_corrected_comp_list)

    db_upd_comp_list = []
    db_upd_releases_list = []
    db_del_comp_Ids = []
    for x in list(db_duplicate_corrected_comp_list):
        for comp in db_existing_comp_list:
            if(x['name'] == comp['name']):
                db_del_comp_Ids.append(x['_id'])
                for i in x["releaseIds"]:
                    query = {"selector": {"type": {"$eq": "release"}, "_id": { "$eq": i }},"limit": 99999}
                    relation = db.find(query)
                    relation_list = list(relation)
                    print(relation_list[0]["componentId"])
                    relation_list[0]["componentId"] = comp['_id']
                    print(relation_list[0]["componentId"])
                    if("releaseIds" not in comp):
                        comp["releaseIds"] = []
                    comp["releaseIds"].append(i)
                    db_upd_releases_list.append(relation_list[0])
                    db_upd_comp_list.append(comp)

    db_dup_release_list = checkDuplicateReleases(db, db_upd_releases_list)

    #Generate log file
    header = (f'Duplicate releases that will be merged - total count :  {len(db_dup_release_list)}')
    generateLogFile(header, db_dup_release_list)

    #Generate log file
    header = (f'Releases that will be merged - total count :  {len(db_upd_releases_list)}')
    generateLogFile(header, db_upd_releases_list)
    
    print('Updating Linkage of Components and Releases............/')
    if not DRY_RUN:
        db.update(db_upd_comp_list)
        db.update(db_upd_releases_list)
                    
    print(db_del_comp_Ids)
    return db_del_comp_Ids

def deleteDuplicateComponents(db, db_del_comp_Ids):
    if not DRY_RUN:
        #Generate log file
        header = (f'Duplicate components that are getting deleted {len(db_del_comp_Ids)}')
        generateLogFile(header, db_del_comp_Ids)
        print('Deleting duplicate components........../')
        for id in db_del_comp_Ids :
            del db[id]

def run():

    #DB connection
    db = dbConnection()

    #Query to fetch the component names with whitespace
    db_comp_list = queryExecution(db,'component')
    #Generate log file
    header = (f'Components with whitespaces - total count :  {len(db_comp_list)}')
    generateLogFile(header, db_comp_list)

    #Query to fetch the release names with whitespaces
    db_rel_list = queryExecution(db,'release')
    #Generate log file
    header = (f'Releases with whitespaces - total count :  {len(db_rel_list)}')
    generateLogFile(header, db_rel_list)

    #check Duplicate
    if len(db_comp_list)!=0:
        db_existing_comp_list = checkDuplicate(db,db_comp_list)
        #Generate log file
        header = (f'Existing components with the same name - total count :  {len(db_existing_comp_list)}')
        generateLogFile(header, db_existing_comp_list)

    #Converting document data to dataframe and removing the whitespace of components
    if len(db_comp_list)!=0:
        db_corrected_comp_list = updateDB(db,db_comp_list)
        #Generate log file
        header = (f'Corrected components - total count :  {len(db_corrected_comp_list)}')
        generateLogFile(header, db_corrected_comp_list)
    else:
        print('No Records found!')

    #Converting document data to dataframe and removing the whitespace of releases
    if len(db_rel_list)!=0:
        db_corrected_rel_list = updateDB(db,db_rel_list)
        #Generate log file
        header = (f'Corrected components - total count :  {len(db_corrected_rel_list)}')
        generateLogFile(header, db_corrected_rel_list)
    else:
        print('No Records found!') 

    #link Releases of duplicate components to the exisiting components
    if len(db_corrected_comp_list)!=0:
        db_del_comp_Ids = linkReleaseToComponent(db, db_corrected_comp_list, db_existing_comp_list)
    
    #delete duplicate components
    if len(db_del_comp_Ids)!=0:
        deleteDuplicateComponents(db, db_del_comp_Ids)

    print('Execution completed....')

if __name__ == '__main__':
    try:
        start_time = time.time()
        run()
        print('\nExecution time: ' + "{0:.2f}".format(time.time() - start_time) + 's')

    except Exception as e:
        print('Exception message ',e)

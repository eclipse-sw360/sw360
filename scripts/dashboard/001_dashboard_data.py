#!/usr/bin/python
# -----------------------------------------------------------------------------
# Copyright Siemens AG, 2016. Part of the SW360 Portal Project.
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
# example, server address and db name should be parametrized, the code
# reorganized into a single class or function, etc.
#
# initial author: alex.borodin@evosoft.com
#
# -----------------------------------------------------------------------------


import couchdb
import pandas as pd
import json
import time
import csv
import re
from collections import defaultdict
from datetime import datetime
from collections import Counter

def dbConnection():
    print('\nConnecting to local couchdb stage server....../')
    couch = couchdb.Server("http://localhost:5984/")
    return couch['sw360db']

def dbUsersConnection():
    print('\nConnecting to local couchdb stage server....../')
    couch = couchdb.Server("http://localhost:5984/")
    return couch['sw360users']

def dbAttachmentConnection():
    print('\nConnecting to local couchdb stage server....../')
    couch = couchdb.Server("http://localhost:5984/")
    return couch['sw360attachments']

#---------------------Number of Components by Type-------------------------------------------
def queryExecutionComponentByType(db):
        print(f'\nExecuting the query for Number of Components by Type.................../')
        design_doc = "_design/Component"
        #Existing View
        url = f"{design_doc}/_view/bycomponenttype"
        result = db.view(url)

        grouped_data = defaultdict(list)
        for entry in list(result):
            if entry["key"] == "":
                entry["key"] = "empty"
            key = entry["key"]
            grouped_data[key].append(entry["value"])

        data = [{"key": key if key else "EMPTY", "count": len(values)} for key, values in grouped_data.items()]

        csv_file_path = "componentType.csv"

        with open(csv_file_path, mode="w", newline='') as csv_file:
             field_names = ["type", "value"]
             writer = csv.DictWriter(csv_file, fieldnames=field_names)
             writer.writeheader()

             for entry in data:
                  writer.writerow({"type": entry["key"], "value": entry["count"]})

        return list(data)

#-------------------Counting total number of comp, proj, rel-------------------------------------    
def queryExecutionCountAll(db):
        print(f'\nExecuting the query for counting total number of comp, proj, rel.................../')
        lengthArr = []

        #Counting total components
        design_doc = "_design/Component"
        url = f"{design_doc}/_view/all" #Existing view
        result = list(db.view(url))

        data = {"key":"Components", "value":len(result)}
        lengthArr.append(data)

        #Counting total projects
        design_doc = "_design/Project"
        url = f"{design_doc}/_view/all" #Existing view
        result = list(db.view(url))

        data = {"key":"Projects", "value":len(result)}
        lengthArr.append(data)

        #Counting total releases
        design_doc = "_design/Release"
        url = f"{design_doc}/_view/all" #Existing view
        result = list(db.view(url))

        data = {"key":"Releases", "value":len(result)}
        lengthArr.append(data)

        grouped_data = defaultdict(list)

        for entry in lengthArr:
            key = entry["key"]
            grouped_data[key] = (entry["value"])

        data = [{"key": key, "count": values} for key, values in grouped_data.items()]

        csv_file_path = "noOfProjCompRel.csv"

        with open(csv_file_path, mode="w", newline="") as csv_file:
             field_names = ["type", "value"]
             writer = csv.DictWriter(csv_file, fieldnames=field_names)
             writer.writeheader()

             for entry in data:
                  writer.writerow({"type": entry["key"], "value": entry["count"]})

        return list(data)


#---------------------------Time Series by Year for Proj, Comp, Rel-----------------------------
def queryCompProjRelTimeSeriesExecution(db):
    print(f'\nExecuting the time-series query.................../')
    print(f'\n  Executing the time-series query for projects.................../')
    design_doc = "_design/Project"
    
    #Creating temporary view byCreatedOn for project
    while True:
       try:
          
          design_docs = db[design_doc]

          # Apply your changes to the 'design_doc' here
          design_docs['views']['byCreatedOn'] = {'map': "function(doc) {  if (doc.type == 'project') {  emit(doc.createdOn, doc._id) }}"}



          db.save(design_docs)

          break  # Exit the loop if the update is successful
       except couchdb.http.ResourceConflict:
        # Handle the conflict by retrying the update
          pass
    
    viewsList = None
    if design_doc in db:
            design_doc = db[design_doc]
            views = design_doc.get('views', {})
            viewsList = design_doc.get('views', {})

    specific_view_name = "byCreatedOn"
    if specific_view_name in viewsList:
        design_doc = "_design/Project"

        url = f"{design_doc}/_view/{specific_view_name}"
        result = db.view(url)
        upd_result = []

        for item in list(result):
            date_string = item["key"]
            date_object = datetime.strptime(date_string, "%Y-%m-%d")
            year = date_object.year
    
            upd_result.append({"key": year, "value": item["value"] })
        
        grouped_data = defaultdict(list)

        for entry in upd_result:
            key = entry["key"]
            grouped_data[key].append(entry["value"])

        # Convert grouped data to the desired format
        dataProj = [{"Year": key, "Project": len(values)} for key, values in grouped_data.items()]
        

#-----------------------------------------ComponentCreatedOn------------------------------------------------------------------
        print(f'\n  Executing the time-series query for components.................../')
        design_doc = "_design/Component"

        #Existing View
        url = f"{design_doc}/_view/byCreatedOn"
        result = db.view(url)
        upd_result = []

        for item in list(result):
            date_string = item["key"]
            date_object = datetime.strptime(date_string, "%Y-%m-%d")
            year = date_object.year
    
            upd_result.append({"key": year, "value": item["value"] })
        
        grouped_data = defaultdict(list)

        for entry in upd_result:
            key = entry["key"]
            grouped_data[key].append(entry["value"])

        dataComp = [{"Year": key, "Component": len(values)} for key, values in grouped_data.items()]

#-----------------------------------------ReleaseCreatedOn------------------------------------------------------------------
        print(f'\n  Executing the time-series query for releases.................../')
        design_doc = "_design/Release"

        #Existing view
        url = f"{design_doc}/_view/byCreatedOn"
        result = db.view(url)
        upd_result = []

        for item in list(result):
            date_string = item["key"]
            date_object = datetime.strptime(date_string, "%Y-%m-%d")
            year = date_object.year
    
            upd_result.append({"key": year, "value": item["value"] })
        
        grouped_data = defaultdict(list)

        for entry in upd_result:
            key = entry["key"]
            grouped_data[key].append(entry["value"])

        dataRel = [{"Year": key, "Release": len(values)} for key, values in grouped_data.items()]

        combined_data = {}

        for item in dataProj + dataComp + dataRel:
            year = item["Year"]
            if year not in combined_data:
                combined_data[year] = {"Year": year}
            combined_data[year].update(item)

        data = list(combined_data.values())

        csv_file_path = "TimeSeries.csv"

        with open(csv_file_path, mode="w", newline="") as csv_file:
             field_names = data[0].keys()
             writer = csv.DictWriter(csv_file, fieldnames=field_names)
             writer.writeheader()

             for entry in data:
                  writer.writerow({"Year": entry["Year"], "Project": entry["Project"], "Component": entry["Component"], "Release": entry["Release"]})

    return []

#------------------------------------------------------------------CLI Size ------------------------------------------
def getCLIList(db,dbattach):
    print(f'Executing the project  query.................../')
    design_doc = "_design/Project"
    view = "byname" #"byreleaseid"
    view_url = f"{db}/{design_doc}/_view/{view}"
    print(f'viewurl: {view_url}')
    viewsList = None
    #function(doc) {  if (doc.type == 'project') {    for(var i in doc.releaseIdToUsage) {      emit(i, {id:doc._id,businessUnit:doc.tag});    }  }}
    #function(doc) {  if (doc.type == 'project') {    for(var i in doc.releaseIdToUsage) {      emit(i, {businessUnit:doc.tag, group:doc.group, id:doc._id});    }  }}
    while True:
       try:
          
          design_docs = db[design_doc]

          design_docs['views']['byReleaseAndBusinessUnit'] = {'map': "function(doc) {  if (doc.type == 'project') {  {   for(var i in doc.releaseIdToUsage) {      emit(i, {businessUnit:doc.tag, group:doc.businessUnit, id:doc._id});  }  }  }}"}

          db.save(design_docs)

          break  # Exit the loop if the update is successful
       except couchdb.http.ResourceConflict:
          pass
       
    if design_doc in db:
            design_doc = db[design_doc]
            views = design_doc.get('views', {})
            print(f'views.keys(): {views.keys()}')
            viewsList = design_doc.get('views', {})
    specific_view_name = "byReleaseAndBusinessUnit"


    if specific_view_name in viewsList:

        #Executing the view such that it returns projects that has releases
        url = f"_design/Project/_view/{specific_view_name}"

        result = db.view(url)

        #Generating length of the original project list
        generateLogarithmFile('NoOfProjectList', len(list(result)))

        for entry_dict in list(result):
            if "value" in entry_dict and "businessUnit" in entry_dict["value"]:
                if entry_dict["value"]["businessUnit"] == "":
                    entry_dict["value"]["businessUnit"] = "empty"
                pattern = r'[^A-Za-z0-9\s]+'
                cleaned_string = re.sub(pattern, ' ', entry_dict["value"]["businessUnit"])
                words = cleaned_string.split()
                
                if words[0] == 'SI':
                    first_two_words = ' '.join(words[:2])
                    entry_dict["value"]["businessUnit"] = first_two_words

        unique_dict_map = {}  # Dictionary to track unique combinations
        unique_dicts = []     # List to store unique dictionaries

        for item in list(result):
            if "value" in item and "businessUnit" in item["value"]:
                key = item["key"]
                group = item["value"]["businessUnit"]
                unique_key = (key, group)  # Combine key and business unit for uniqueness

                if unique_key not in unique_dict_map:
                    unique_dict_map[unique_key] = True
                    unique_dicts.append(item)

        #Gererating length of the original project list
        generateLogarithmFile('NoOfPRojectsGroup', len(unique_dicts))
        result = unique_dicts
        
        
        id_list = [row.key for row in result]
        
        #Executing the release query such that it returns releases that has CLI accepted status attachments...................
        db_query = db.find({"selector":{"type": "release","_id": {"$in": id_list},  "$and": [  {"attachments": {"$exists": True, "$type": "array", "$elemMatch": {"attachmentType": "COMPONENT_LICENSE_INFO_XML","checkStatus": "ACCEPTED" } } } ]}
                                                                                             , "limit":999999999999999}) 
        db_list = list(db_query)

        filtered_data = [
            {
                "_id": item["_id"],
                "type": item["type"],
                "attachments": [
                    attachment for attachment in item["attachments"]
                    if attachment["attachmentType"] == "COMPONENT_LICENSE_INFO_XML" and  "checkStatus" in attachment and
                        attachment["checkStatus"] is not None and attachment["checkStatus"] == "ACCEPTED"
                ]
            }
            for item in db_list
        ]      
        

        #Update the first list and remove releaseId not present based on second list
        ids_to_keep = {doc["_id"] for doc in filtered_data}
        filtered_list = [doc for doc in list(result) if doc.key in ids_to_keep]


        #Gererating length of the filtered project list
        generateLogarithmFile('NoOfFilteredProjectList', len(filtered_list))

        

        attach_id_list = []
        attach_values_list = []
        for doc in filtered_data:
            attach_list = doc.get("attachments", []) #Getting nested attachments list from release documents
            attach_values_list.extend(attach_list)
            content_ids = [attach.get("attachmentContentId") for attach in attach_list] #Getting the attachmentContentId and storing it in a list of the same
            attach_id_list.extend(content_ids)

        #Executing the attachment release query such that it returns the attachment doc list that has the actual length.................../')
        db_query = dbattach.find({"selector":{"type": "attachment","_id": {"$in": attach_id_list}}, "limit":999999999999999})
        db_attach_list = list(db_query)
        

        # Specify the top-level key and the nested key
        top_level_key = "_id"
        nested_key = "_attachments"

        # Create a new list of dictionaries with the specified key-value pairs
        new_list = {} 
        length_list = []
        for doc in db_attach_list:
            
            for key, value in doc.get("_attachments", {}).items():
                    length = value.get("length")
                    new_list[doc["_id"]] = length
                    length_list.append(length)


        content_id_map = {}
        for doc in filtered_data:
            attach_list = doc.get("attachments", []) #Getting the nested attachment content
            for attach in attach_list:
                content_id_map[attach["attachmentContentId"]] = {"releaseId": doc["_id"], "filename": attach["filename"]} #Saving it in a key value pair dicitonary for the lookup 
        
        
        # Merge data based on 'attachid' and 'contentId'
        merged_list = []
        for doc in db_attach_list:
            attachid = doc["_id"]
            if attachid in content_id_map:                   #Comparing attachmentId from attachment doc with attachmentContent from release doc
                content_data = content_id_map[attachid]
                length = None
                for key, value in doc.get("_attachments", {}).items():
                    length = value.get("length")
                #Creating a single document which has releaseId, attachmentId, filename and length
                merged_doc = {
                    "releaseId": content_data["releaseId"] ,
                    "attachid":  attachid,
                    "filename": content_data["filename"],   
                    "length": length
                }
                merged_list.append(merged_doc)

        #Gererating length of the original release list
        generateLogarithmFile('NoOfReleaseList', len(db_list))

        #generateLogarithmFile('merged_list', merged_list)

        

        #Creating a key value pair lookup from the merged list
        name_lookup = {doc['releaseId']: {"attachId":doc['attachid'], "attachLength":doc['length']} for doc in merged_list}
        

        for entry_dict in merged_list:
           if "length" in entry_dict and entry_dict["length"] is None:
                entry_dict["length"] = 0

        updated_list = []
        length_data = {}
        
# Iterate through the second list to calculate total length for each relId
        for entry in merged_list:
            rel_id = entry["releaseId"]
            length = entry["length"]
            if rel_id in length_data:
                length_data[rel_id] += length
            else:
                length_data[rel_id] = length

        # Combine data 
        combined_data = []
        for entry in filtered_list:
            key = entry["key"]
            value = entry["value"]
            id = value["id"]
            
            if key in length_data:
                combined_data.append({"key": key, "value": value, "length": length_data[key]})
                if "businessUnit" in value and value["businessUnit"]=="CT":
                    temp_dict = {"key": key, "value": value, "length": length_data[key]}
            else:
                combined_data.append({"key": key, "value": value, "length": 0})  #Adding another tag to the document returned by the view byReleaseAndBusinessUnit
                                        #updated_list.append(dictionary.copy())
                if value["businessUnit"]=="CT":
                    temp_dict = {"key": key, "value": value, "length": length_data[key]}

        generateLogarithmFile('updated_list_length', len(list(combined_data)))

        #generateLogarithmFile('updated_list', updated_list)


        
        for entry_dict in combined_data:
            if "value" in entry_dict and "businessUnit" in entry_dict["value"]:
                if entry_dict["value"]["businessUnit"] == "":
                    entry_dict["value"]["businessUnit"] = "empty"
                if "length" in entry_dict and entry_dict["length"] is None:
                    entry_dict["length"] = 0
            if "value" in entry_dict and "group" in entry_dict["value"]:
                if entry_dict["value"]["group"] == "":
                    entry_dict["value"]["group"] = "empty"
                
        
        business_unit_info = defaultdict(lambda: {"count": 0, "total_length": 0})
        # Iterate through the documents and aggregate the lengths
        for doc in combined_data:
            if "value" in doc and "businessUnit" in doc["value"] and "length" in doc:
                business_unit = doc["value"]["businessUnit"]
                length = doc["length"]
                business_unit_info[business_unit]["count"] += 1
                business_unit_info[business_unit]["total_length"] += length

        # Convert the defaultdict to a regular dictionary
        resultdict = dict(business_unit_info)
  
        generateLogarithmFile('businessUnitInfo', resultdict)

        csv_file = 'attachmentUsageDept.csv'

        # Extract keys and values from the dictionary
        keys = list(resultdict.keys())
        values = [item['total_length'] for item in resultdict.values()]

        # Write data to CSV file
        with open(csv_file, 'w', newline='') as file:
              writer = csv.writer(file)
              writer.writerow(['Key', 'Value'])  # Write header
              writer.writerows(zip(keys, values))

        group_info = defaultdict(lambda: {"count": 0, "total_length": 0})
        # Iterate through the documents and aggregate the lengths
        for doc in combined_data:
            if "value" in doc and "group" in doc["value"] and "length" in doc and "businessUnit" in doc["value"]:
                business_unit = doc["value"]["group"]
                length = doc["length"]
                group_info[business_unit]["count"] += 1
                group_info[business_unit]["total_length"] += length

        # Convert the defaultdict to a regular dictionary
        resultdict = dict(group_info)

        generateLogarithmFile('groupInfo', resultdict)
        
        csv_file = 'attachmentUsage.csv'

        # Extract keys and values from the dictionary
        keys = list(resultdict.keys())
        values = [item['total_length'] for item in resultdict.values()]

        # Write data to CSV file
        with open(csv_file, 'w', newline='') as file:
              writer = csv.writer(file)
              writer.writerow(['Key', 'Value'])  # Write header
              writer.writerows(zip(keys, values))
        


#-----------------------------Cleared/Not Cleared Releases----------------------------------
def queryExecutionReleasesECCClearedStatus(db):
        print(f'\nExecuting the query for release clearing status.................../')
        design_doc = "_design/Release"

        #Existing view
        url = f"{design_doc}/_view/byStatus"
        result = db.view(url)
        
        grouped_data = defaultdict(list)

        for entry in list(result):
            if entry["key"] == "":
                entry["key"] = "empty"
            key = entry["key"]
            grouped_data[key].append(entry["value"])

        data = [{"key": key if key else "EMPTY", "count": len(values)} for key, values in grouped_data.items()]

        csv_file_path = "releaseStatus.csv"

        with open(csv_file_path, mode="w", newline="") as csv_file:
             field_names = ["type", "value"]
             writer = csv.DictWriter(csv_file, fieldnames=field_names)
             writer.writeheader()

             for entry in data:
                  writer.writerow({"type": entry["key"], "value": entry["count"]})

        return list(data)



#-----------------------------Attachment Disk Usage Total----------------------------------


def queryExecutionAttachmentUsageAll(db):
        print(f'\n Executing the query for getting total attachment disk usage.................../')
        design_doc = "_design/AttachmentContent"

        #Existing View
        url = f"{design_doc}/_view/all"
        result = list(db.view(url))
        
        id_list = [row.value for row in result]

        db_query = list(db.find({"selector":{"type": "attachment","_id": {"$in": id_list}}, "limit":999999999999999}))

        length_list = []
        total_length = 0
        item_count = 0
        for doc in db_query:
            
            for key, value in doc.get("_attachments", {}).items():
                    length = value.get("length")
                    total_length+=length
                    item_count+=1
                    length_list.append(length)

        data = [{"total_length":total_length, "count": item_count} ]

        csv_file_path = "attachmentDiskUsageTotal.csv"

        with open(csv_file_path, mode="w", newline="") as csv_file:
             field_names = ["total_length", "count"]
             writer = csv.DictWriter(csv_file, fieldnames=field_names)
             writer.writeheader()

             for entry in data:
                  writer.writerow({"total_length": entry["total_length"], "count": entry["count"]})

        return list(data)


#------------------------Most Used Components-------------------------------------
def queryExecutionMostUsedComp(db):
    print(f'\n Executing the query for most used components.................../')
    design_doc = "_design/Project"
    #Existing view
    url = f"{design_doc}/_view/byreleaseid"
    result = list(db.view(url))

    id_list = [row.key for row in result]

    db_query = list(db.find({"selector":{"type": "release","_id": {"$in": id_list}}, "limit":999999999999999}))

    key_counts = {}
    for item in db_query:
        key = item["componentId"]
        name = item["name"]
        if key in key_counts:
            key_counts[key]["count"] += 1
        else:
            key_counts[key] = {"key": key, "name": name, "count": 1}

    # Create the new list of dictionaries
    result_list = list(key_counts.values())
    sorted_list = sorted(result_list, key=lambda x: x["count"], reverse=True)
    csv_file_path = "mostUsedComp.csv"

    with open(csv_file_path, mode="w", newline="") as csv_file:
            field_names = ["key", "name", "count"]
            writer = csv.DictWriter(csv_file, fieldnames=field_names)
            writer.writeheader()

            for entry in sorted_list:
                writer.writerow({"key": entry["key"],"name": entry["name"], "count": entry["count"]})

    return list(result_list)



#------------------Most Used Licenses------------------
def queryExecutionMostUsedLicenses(db):
    print(f'\n Executing the query for most used licenses.................../')
    design_doc = "_design/Component"
    #Create temporary view
    # bymainLicenseIdArr function(doc) {    if (doc.type == 'component') {      if(doc.mainLicenseIds) { for(var i in doc.mainLicenseIds){           emit(doc.mainLicenseIds[i], doc._id);      }} else {            emit('', doc._id);      }    }}
    while True:
     try:
          
          design_docs = db[design_doc]

          design_docs['views']['bymainLicenseIdArr'] = {'map': "function(doc) {    if (doc.type == 'component') {      if(doc.mainLicenseIds) { for(var i in doc.mainLicenseIds){           emit(doc.mainLicenseIds[i], doc._id);      }} else {            emit('EMPTY', doc._id);      }   } }"}



          db.save(design_docs)

          break  
     except couchdb.http.ResourceConflict:
          pass

    viewsList = []
    if design_doc in db:
            design_doc = db[design_doc]
            views = design_doc.get('views', {})
            print(f'views.keys(): {views.keys()}')
            viewsList = design_doc.get('views', {})
    specific_view_name = "bymainLicenseIdArr"


    if specific_view_name in viewsList:
        print(f'Inside')
        design_doc = "_design/Component"
        url = f"{design_doc}/_view/bymainLicenseIdArr"
        print(f'Inside url {url}')
        result = list(db.view(url))
        license_count = Counter(item['key'] for item in result)

        license_list = [{'license': lic, 'count': count} for lic, count in license_count.items()]
        sorted_license_list = sorted(license_list, key=lambda x: x["count"], reverse=True)


        csv_file_path = "mostUsedLicenses.csv"

        with open(csv_file_path, mode="w", newline="") as csv_file:
                field_names = ["license", "count"]
                writer = csv.DictWriter(csv_file, fieldnames=field_names)
                writer.writeheader()

                for entry in sorted_license_list:
                    writer.writerow({"license": entry["license"], "count": entry["count"]})

        return list(sorted_license_list)

    return []
    

    
#------------------------Components that are not used-------------------------------------
def queryExecutionCompNotUsed(db):
    print(f'\n Executing the query for components not being used.................../')
    design_doc = "_design/Project"
    #Existing view
    url = f"{design_doc}/_view/byreleaseid"
    result = list(db.view(url))

    proj_rel_id_list = [row.key for row in result]


    #Executing the view for all the releases
    design_doc = "_design/Release"
    url = f"{design_doc}/_view/all"
    result = list(db.view(url))

    rel_id_list = [row.value for row in result]

    proj_rel_id_list = set(proj_rel_id_list)
    rel_id_list = set(rel_id_list)

    # Find values in the original list that are not in the input list
    result_id_list = list(rel_id_list - proj_rel_id_list)


    db_query = list(db.find({"selector":{"type": "release","_id": {"$in": result_id_list}}, "limit":999999999999999}))

    comp_result = {}
    for item in db_query:
        key = item["componentId"]
        name = item["name"]
        comp_result[key] = {"key": key, "name": name}

    result_list = list(comp_result.values())
    csv_file_path = "notUsedComp.csv"

    with open(csv_file_path, mode="w", newline="") as csv_file:
            field_names = ["key", "name"]
            writer = csv.DictWriter(csv_file, fieldnames=field_names)
            writer.writeheader()

            for entry in result_list:
                writer.writerow({"key": entry["key"],"name": entry["name"]})

    return list(result_list)
    



def generateLogarithmFile(header, db_copy_list):
    print(f'Generating log file for {header}......../')
    with open("DashboardDataScript.log", "a") as f:
        f.write('\n-------------------------\n')
        f.write(header)
        f.write('\n-------------------------\n')
    logFile = open('DashboardDataScript.log', 'a')
    json.dump(db_copy_list, logFile, indent = 4, sort_keys = True)
    logFile.close()

def run():
    start_time = time.time()
    
    #DB connection
    db = dbConnection()
    dbUsers = dbUsersConnection()
    dbAttach = dbAttachmentConnection()

    #dbattach = dbConnectionsw360attachments()

    #Query execution
    db_copy_list = queryExecutionComponentByType(db)
    db_copy_list = queryExecutionCountAll(db)
    db_copy_list = queryCompProjRelTimeSeriesExecution(db)
    db_copy_list = queryExecutionReleasesECCClearedStatus(db)
    
    db_copy_list = queryExecutionMostUsedComp(db)
    db_copy_list = queryExecutionMostUsedLicenses(db)
    db_copy_list = queryExecutionCompNotUsed(db)

    db_copy_list = getCLIList(db, dbAttach)
    print('\nExecution time: ' + "{0:.2f}".format(time.time() - start_time) + 's')

    db_copy_list = queryExecutionAttachmentUsageAll(dbAttach)


if __name__ == '__main__':
    try:
        start_time = time.time()
        run()
        print('\nExecution time: ' + "{0:.2f}".format(time.time() - start_time) + 's')

    except Exception as e:
        print('Exception message ',e)


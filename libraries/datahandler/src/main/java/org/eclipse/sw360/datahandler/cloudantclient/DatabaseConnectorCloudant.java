/*
 * Copyright Siemens AG, 2021. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.cloudantclient;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.ArrayList;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TBase;
import org.apache.thrift.TFieldIdEnum;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.ThriftUtils;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.DesignDocumentManager;
import com.cloudant.client.api.model.Response;
import com.cloudant.client.api.query.QueryResult;
import com.cloudant.client.api.views.Key;
import com.cloudant.client.api.views.ViewRequest;
import com.cloudant.client.api.views.ViewRequestBuilder;
import com.cloudant.client.api.views.ViewResponse;
import com.cloudant.client.api.views.ViewResponse.Row;
import com.cloudant.client.org.lightcouch.NoDocumentException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Database Connector to a CouchDB database
 *
 */
public class DatabaseConnectorCloudant {
    
    private final Logger log = LogManager.getLogger(DatabaseConnectorCloudant.class);
    private static final ImmutableList<String> entitiesWithNonMatchingStructType = ImmutableList
            .of("moderation", "attachment", "usedReleaseRelation");

    private final String dbName;
    private final DatabaseInstanceCloudant instance;
    private final Database database;

    public DatabaseConnectorCloudant(Supplier<CloudantClient> client, String dbName) {
        this.instance = new DatabaseInstanceCloudant(client);
        this.dbName = dbName;
        // Create the database if it does not exists yet
        this.database = instance.createDB(dbName);
    }

    public String getDbName() {
        return dbName;
    }

    public void update(Object document) {
        Response resp;
        if (document != null) {
            final Class documentClass = document.getClass();
            if (ThriftUtils.isMapped(documentClass)) {
                AttachmentContent content = (AttachmentContent) document;
                try {
                    InputStream in = getAttachment(content.getId(), content.getFilename());
                    resp = database.update(document);
                    createAttachment(resp.getId(), content.getFilename(), in, content.getContentType());
                } catch (NoDocumentException e) {
                    resp = database.update(document);
                    log.debug("No attachment associated with the document. Updating attachment content non metadata only");
                }
            } else {
                resp = database.update(document);
            }
            if (TBase.class.isAssignableFrom(document.getClass())) {
                TBase tbase = (TBase) document;
                TFieldIdEnum id = tbase.fieldForId(1);
                TFieldIdEnum rev = tbase.fieldForId(2);
                tbase.setFieldValue(id, resp.getId());
                tbase.setFieldValue(rev, resp.getRev());
            }
        } else {
            log.warn("Ignore updating a null document.");
        }
    }

    public DatabaseInstanceCloudant getInstance() {
        return instance;
    }

    public <T> Set<String> getAllIds(Class<T> type) {
        Set<String> ids = Sets.newHashSet();
        try {
            List<Row<String, Object>> rows = database.getViewRequestBuilder(type.getSimpleName(), "all")
                    .newRequest(Key.Type.STRING, Object.class).build().getResponse().getRows();
            for (Row<String, Object> r : rows) {
                String id = r.getId();
                ids.add(id);
            }
        } catch (IOException e) {
            log.error("Error fetching ids", e);
        }
        return ids;
    }

    public <T> T get(Class<T> type, String id) {
        try {
            T obj = (T) database.find(type, id);
            String extractedType = null;
            Field[] f = obj.getClass().getDeclaredFields();
            for (Field fi : f) {
                if (fi.getName().equalsIgnoreCase("type")) {
                    extractedType = (String) fi.get(obj);
                    break;
                }
            }
            if (extractedType != null) {
                final String entityType = extractedType.toLowerCase();
                if (!entitiesWithNonMatchingStructType.stream().map(String::toLowerCase)
                        .anyMatch(tye -> tye.equals(entityType))
                        && !type.getSimpleName().equalsIgnoreCase(extractedType)) {
                    return null;
                }
            }
            return obj;
        } catch (Exception e) {
            log.error("Error fetching document of type " + type.getSimpleName() + " with id " + id + " : "
                    + e.getMessage());
            return null;
        }
    }

    public <T> List<T> getAll(Class<T> type) {
        List<T> list = Lists.newArrayList();
        try {
            list = database.getViewRequestBuilder(type.getSimpleName(), "all").newRequest(Key.Type.STRING, Object.class)
                    .includeDocs(true).build().getResponse().getDocsAs(type);
        } catch (Exception e) {
            log.error("Error getting documents", e);
        }
        return list;
    }

    public boolean remove(String id) {
        Response resp = database.remove(id);
        boolean success = resp.getStatusCode() == HttpStatus.SC_OK ? true : false;
        if (!success) {
            log.error("Could not delete document with id: " + id);
        }
        return success;
    }

    public <T> List<T> get(Class<T> type, Collection<String> ids) {
        if (!CommonUtils.isNotEmpty(ids))
            return Collections.emptyList();
        try {
            Set<String> idSet = new HashSet<>(ids);
            String[] keys = new String[idSet.size()];
            int index = 0;
            for (String str : idSet)
                keys[index++] = str;
            List<T> docs = database.getAllDocsRequestBuilder().includeDocs(true).keys(keys).build().getResponse()
                    .getDocsAs(type);
            return docs.stream().filter(Objects::nonNull).collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Error fetching documents", e);
            return Collections.emptyList();
        }
    }

    public <T> List<T> get(Class<T> type, Collection<String> ids, boolean ignoreNotFound) {
        return get(type, ids);
    }

    public List<Response> executeBulk(Collection<?> list) {
        List<Response> responses = Lists.newArrayList();
        List entities = Lists.newArrayList(list);
        try {
            responses = database.bulk(entities);
            for (int i = 0; i < entities.size(); i++) {
                if (TBase.class.isAssignableFrom(entities.get(i).getClass())) {
                    TBase tbase = (TBase) entities.get(i);
                    TFieldIdEnum id = tbase.fieldForId(1);
                    TFieldIdEnum rev = tbase.fieldForId(2);
                    tbase.setFieldValue(id, responses.get(i).getId());
                    tbase.setFieldValue(rev, responses.get(i).getRev());
                }
            }
        } catch (Exception e) {
            log.error("Error in bulk execution", e);
            return null;
        }
        return responses;
    }

    public List<Response> deleteBulk(Collection<?> deletionCandidates) {
        return deletionCandidates.stream().map(x -> database.remove(x)).collect(Collectors.toList());
    }

    public <T> List<Response> deleteIds(Class<T> type, Collection<String> ids) {
        final List<T> deletionCandidates = get(type, ids);
        return deleteBulk(deletionCandidates);
    }

    public <T> int getDocumentCount(Class<T> type) {
        int count = 0;
        try {
            count = database.getViewRequestBuilder(type.getSimpleName(), "all")
                    .newRequest(Key.Type.STRING, Object.class).includeDocs(false).limit(0).build().getResponse()
                    .getTotalRowCount().intValue();
        } catch (IOException e) {
            log.error("Error getting document count", e);
        }
        return count;
    }

    public <T> int getDocumentCount(Class<T> type, String viewName, String []keys) {
        int count = 0;
        try {
            ViewResponse<String, Integer> response = database.getViewRequestBuilder(type.getSimpleName(), viewName)
                    .newRequest(Key.Type.STRING, Integer.class).keys(keys).includeDocs(false).group(true).reduce(true).build().getResponse();
            count = response.getValues().stream().mapToInt(x->x).sum();
        } catch (IOException e) {
            log.error("Error getting document count", e);
        }
        return count;
    }

    public <T> ViewRequestBuilder createQuery(Class<T> type, String queryName) {
        return database.getViewRequestBuilder(type.getSimpleName(), queryName);
    }

    public InputStream getAttachment(String docId, String attachmentName) {
        return database.getAttachment(docId, attachmentName);
    }

    public void createAttachment(String attachmentContentId, String fileName, InputStream attachmentInputStream,
            String contentType) {
        String revision = database.find(AttachmentContent.class, attachmentContentId).getRevision();
        database.saveAttachment(attachmentInputStream, fileName, contentType, attachmentContentId, revision);
    }

    public <T> boolean deleteById(Class<T> type, String id) {
        Response result = null;
        if (database.contains(id)) {
            T obj = get(type, id);
            result = database.remove(obj);
        }
        return result.getStatusCode() == HttpStatus.SC_OK ? true : false;
    }

    public <T> boolean add(T doc) {
        Response resp = database.save(doc);
        if (TBase.class.isAssignableFrom(doc.getClass())) {
            TBase tbase = (TBase) doc;
            TFieldIdEnum id = tbase.fieldForId(1);
            TFieldIdEnum rev = tbase.fieldForId(2);
            tbase.setFieldValue(id, resp.getId());
            tbase.setFieldValue(rev, resp.getRev());
        }
        return resp.getStatusCode() == HttpStatus.SC_CREATED ? true : false;
    }

    public <T> boolean remove(T doc) {
        Response result = database.remove(doc);
        return result.getStatusCode() == HttpStatus.SC_OK ? true : false;
    }

    public DesignDocumentManager getDesignDocumentManager() {
        return database.getDesignDocumentManager();
    }

    public void createIndex(String indexDefinition) {
        database.createIndex(indexDefinition);
    }

    public <T> QueryResult<T> getQueryResult(String query, Class<T> type) {
        return database.query(query, type);
    }

    public <T> Set<String> getDistinctSortedStringKeys(Class<T> type, String viewName) {
        ViewRequest<String, String> countReq1 = database.getViewRequestBuilder(type.getSimpleName(), viewName)
                .newRequest(Key.Type.STRING, String.class).includeDocs(false).build();
        try {
            ViewResponse<String, String> response = countReq1.getResponse();
            return new TreeSet<String>(response.getKeys());
        } catch (IOException e) {
            log.error("Error in getting project groups", e);
        }
        return Collections.emptySet();
    }

    public <T> List<T> getDocsByListIds(Class<T> type, Collection<String> ids) {
        if (!CommonUtils.isNotEmpty(ids))
            return Collections.emptyList();
        try {
            List<String> idList = new ArrayList<>(ids);
            String[] keys = new String[idList.size()];
            int index = 0;
            for (String str : idList)
                keys[index++] = str;
            List<T> docs = database.getAllDocsRequestBuilder().includeDocs(true).keys(keys).build().getResponse()
                    .getDocsAs(type);
            return docs.stream().filter(Objects::nonNull).collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Error fetching documents", e);
            return Collections.emptyList();
        }
    }
}

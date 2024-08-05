/*
 * Copyright Siemens AG, 2021, 2024. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.cloudantclient;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.ibm.cloud.cloudant.v1.Cloudant;
import com.ibm.cloud.cloudant.v1.model.*;
import com.ibm.cloud.sdk.core.service.exception.ServiceResponseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TBase;
import org.apache.thrift.TFieldIdEnum;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.ThriftUtils;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Database Connector to a CouchDB database
 */
public class DatabaseConnectorCloudant {

    private final Logger log = LogManager.getLogger(DatabaseConnectorCloudant.class);
    private static final ImmutableList<String> entitiesWithNonMatchingStructType = ImmutableList
            .of("moderation", "attachment", "usedReleaseRelation");

    private final String dbName;
    private final DatabaseInstanceCloudant instance;

    public DatabaseConnectorCloudant(Cloudant client, String dbName) {
        this.instance = new DatabaseInstanceCloudant(client);
        this.dbName = dbName;
        // Create the database if it does not exist yet
        instance.createDB(dbName);
    }

    public String getDbName() {
        return dbName;
    }

    public void update(Object document) {
        DocumentResult resp;
        if (document != null) {
            if (ThriftUtils.isMapped(document.getClass())) {
                AttachmentContent content = (AttachmentContent) document;
                InputStream in = getAttachment(content.getId(), content.getFilename());
                resp = this.updateWithResponse(document);
                createAttachment(resp.getId(), content.getFilename(), in, content.getContentType());
            } else {
                resp = this.updateWithResponse(document);
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

    public DocumentResult updateWithResponse(Object document) {
        DocumentResult resp = null;
        Document doc = getDocumentFromPojo(document);
        if (document != null) {
            PutDocumentOptions putDocumentOption = new PutDocumentOptions.Builder()
                    .db(this.dbName)
                    .docId(doc.getId())
                    .document(doc)
                    .build();

            resp = this.instance.getClient().putDocument(putDocumentOption).execute().getResult();
        }
        return resp;
    }

    public DatabaseInstanceCloudant getInstance() {
        return instance;
    }

    public <T> Set<String> getAllIds(@NotNull Class<T> type) {
        Set<String> ids = Sets.newHashSet();
        try {
            PostViewOptions viewOptions = new PostViewOptions.Builder()
                    .db(this.dbName)
                    .ddoc(type.getSimpleName())
                    .view("all")
                    .build();

            ViewResult response = this.instance.getClient().postView(viewOptions).execute().getResult();
            List<ViewResultRow> rows = response.getRows();
            for (ViewResultRow r : rows) {
                String id = r.getId();
                ids.add(id);
            }
        } catch (ServiceResponseException e) {
            log.error("Error fetching ids", e);
        }
        return ids;
    }

    public <T> T get(Class<T> type, String id) {
        try {
            GetDocumentOptions documentOption = new GetDocumentOptions.Builder()
                    .db(this.dbName)
                    .docId(id)
                    .build();
            Document doc = this.instance.getClient().getDocument(documentOption).execute().getResult();
            T obj = this.getPojoFromDocument(doc, type);

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
                if (entitiesWithNonMatchingStructType.stream().map(String::toLowerCase)
                        .noneMatch(tye -> tye.equals(entityType))
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
            PostViewOptions viewOptions = new PostViewOptions.Builder()
                    .db(this.dbName)
                    .ddoc(type.getSimpleName())
                    .view("all")
                    .includeDocs(true)
                    .build();

            ViewResult response = this.instance.getClient().postView(viewOptions).execute().getResult();
            List<ViewResultRow> rows = response.getRows();

            list = rows.stream().map(r -> this.getPojoFromDocument(r.getDoc(), type)).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting documents", e);
        }
        return list;
    }

    public boolean remove(String id) {
        DeleteDocumentOptions deleteOption = new DeleteDocumentOptions.Builder()
                .db(this.dbName)
                .docId(id)
                .build();

        DocumentResult resp = this.instance.getClient().deleteDocument(deleteOption).execute().getResult();
        boolean success = resp.isOk();
        if (!success) {
            log.error("Could not delete document with id: " + id);
        }
        return success;
    }

    public List<Document> getDocuments(Collection<String> ids) {
        if (!CommonUtils.isNotEmpty(ids))
            return Collections.emptyList();
        try {
            Set<String> idSet = new HashSet<>(ids);

            PostAllDocsOptions postDocsOption = new PostAllDocsOptions.Builder()
                    .db(this.dbName)
                    .keys(idSet.stream().toList())
                    .includeDocs(true)
                    .build();

            AllDocsResult resp = this.instance.getClient().postAllDocs(postDocsOption).execute().getResult();

            return resp.getRows().stream()
                    .map(DocsResultRow::getDoc)
                    .filter(Objects::nonNull).collect(Collectors.toList());
        } catch (ServiceResponseException e) {
            log.error("Error fetching documents", e);
            return Collections.emptyList();
        }
    }

    public Document getDocument(String id) {
        try {
            GetDocumentOptions getDocOption = new GetDocumentOptions.Builder()
                    .db(this.dbName)
                    .docId(id)
                    .build();

            return this.instance.getClient().getDocument(getDocOption).execute().getResult();
        } catch (ServiceResponseException e) {
            log.error("Error fetching document", e);
            return new Document();
        }
    }

    public <T> List<T> get(Class<T> type, Collection<String> ids) {
        if (!CommonUtils.isNotEmpty(ids))
            return Collections.emptyList();
        try {
            Set<String> idSet = new HashSet<>(ids);

            PostAllDocsOptions postDocsOption = new PostAllDocsOptions.Builder()
                    .db(this.dbName)
                    .keys(idSet.stream().toList())
                    .includeDocs(true)
                    .build();

            AllDocsResult resp = this.instance.getClient().postAllDocs(postDocsOption).execute().getResult();

            return resp.getRows().stream().map(
                    r -> this.getPojoFromDocument(r.getDoc(), type)
            ).filter(Objects::nonNull).collect(Collectors.toList());
        } catch (ServiceResponseException e) {
            log.error("Error fetching documents", e);
            return Collections.emptyList();
        }
    }

    public <T> List<T> get(Class<T> type, Collection<String> ids, boolean ignoreNotFound) {
        return get(type, ids);
    }

    public List<DocumentResult> executeBulk(@NotNull Collection<?> list) {
        BulkDocs bulkDocs = new BulkDocs.Builder()
                .docs(list.stream().map(this::getDocumentFromPojo).collect(Collectors.toList()))
                .build();

        PostBulkDocsOptions bulkDocsOptions = new PostBulkDocsOptions.Builder()
                .db(this.dbName)
                .bulkDocs(bulkDocs)
                .build();

        List<DocumentResult> responses = Lists.newArrayList();

        Object[] entities = list.toArray();
        try {
            responses = this.instance.getClient().postBulkDocs(bulkDocsOptions).execute().getResult();
            for (int i = 0; i < entities.length; i++) {
                if (TBase.class.isAssignableFrom(entities[i].getClass())) {
                    TBase tbase = (TBase) entities[i];
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

    public List<DocumentResult> deleteBulk(@NotNull Collection<Document> deletionCandidates) {
        BulkDocs bulkDocs = new BulkDocs.Builder()
                .docs(
                        deletionCandidates.stream()
                                .peek(d -> d.setDeleted(true))
                                .collect(Collectors.toList()))
                .build();

        PostBulkDocsOptions bulkDocsOptions = new PostBulkDocsOptions.Builder()
                .db(this.dbName)
                .bulkDocs(bulkDocs)
                .build();

        return this.instance.getClient().postBulkDocs(bulkDocsOptions).execute().getResult();
    }

    public List<DocumentResult> deleteIds(Collection<String> ids) {
        final List<Document> deletionCandidates = getDocuments(ids);
        return deleteBulk(deletionCandidates);
    }

    public <T> int getDocumentCount(@NotNull Class<T> type) {
        int count = 0;
        try {
            PostViewOptions viewOption = new PostViewOptions.Builder()
                    .db(this.dbName)
                    .ddoc(type.getSimpleName())
                    .view("all")
                    .includeDocs(false)
                    .limit(0)
                    .build();

            count = this.instance.getClient().postView(viewOption).execute().getResult().getTotalRows().intValue();
        } catch (ServiceResponseException e) {
            log.error("Error getting document count", e);
        }
        return count;
    }

    public <T> int getDocumentCount(@NotNull Class<T> type, String viewName, String[] keys) {
        int count = 0;
        try {
            PostViewOptions viewOption = new PostViewOptions.Builder()
                    .db(this.dbName)
                    .ddoc(type.getSimpleName())
                    .view(viewName)
                    .keys(Arrays.asList(keys))
                    .includeDocs(false)
                    .group(true)
                    .reduce(true)
                    .build();

            ViewResult resp = this.instance.getClient().postView(viewOption).execute().getResult();

            count = resp.getRows().stream()
                    .mapToInt(r -> Integer.parseInt(r.getValue().toString()))
                    .sum();
        } catch (ServiceResponseException e) {
            log.error("Error getting document count", e);
        }
        return count;
    }

    public <T> PostViewOptions.Builder getPostViewQueryBuilder(
            @NotNull Class<T> type, String queryName) {
        return new PostViewOptions.Builder()
                .db(this.dbName)
                .ddoc(type.getSimpleName())
                .view(queryName);
    }

    public ViewResult getPostViewQueryResponse(PostViewOptions options) {
        return this.instance.getClient().postView(options)
                .execute()
                .getResult();
    }

    public InputStream getAttachment(String docId, String attachmentName) {
        GetAttachmentOptions attachmentOptions =
                new GetAttachmentOptions.Builder()
                        .db(this.dbName)
                        .docId(docId)
                        .attachmentName(attachmentName)
                        .build();

        return this.instance.getClient().getAttachment(attachmentOptions).execute()
                .getResult();
    }

    public void createAttachment(String attachmentContentId, String fileName,
                                 InputStream attachmentInputStream, String contentType) {
        GetDocumentOptions documentOption = new GetDocumentOptions.Builder()
                .db(this.dbName)
                .docId(attachmentContentId)
                .build();
        Document doc = this.instance.getClient().getDocument(documentOption).execute().getResult();
        String revision = doc.getRev();

        PutAttachmentOptions attachmentOptions =
                new PutAttachmentOptions.Builder()
                        .db(this.dbName)
                        .docId(attachmentContentId)
                        .attachmentName(fileName)
                        .attachment(attachmentInputStream)
                        .contentType(contentType)
                        .rev(revision)
                        .build();

        this.instance.getClient().putAttachment(attachmentOptions).execute()
                .getResult();
    }

    public boolean deleteById(String id) {
        if (this.contains(id)) {
            Document doc = getDocument(id);

            DeleteDocumentOptions deleteOption = new DeleteDocumentOptions.Builder()
                    .db(this.dbName)
                    .docId(id)
                    .rev(doc.getRev())
                    .build();
            return this.instance.getClient().deleteDocument(deleteOption).execute().getResult().isOk();
        }
        return true;
    }

    public <T> boolean add(T doc) {
        PostDocumentOptions postDocOption = new PostDocumentOptions.Builder()
                .db(this.dbName)
                .document(this.getDocumentFromPojo(doc))
                .build();

        DocumentResult resp = this.instance.getClient().postDocument(postDocOption).execute().getResult();
        if (TBase.class.isAssignableFrom(doc.getClass())) {
            TBase tbase = (TBase) doc;
            TFieldIdEnum id = tbase.fieldForId(1);
            TFieldIdEnum rev = tbase.fieldForId(2);
            tbase.setFieldValue(id, resp.getId());
            tbase.setFieldValue(rev, resp.getRev());
        }
        return resp.isOk();
    }

    public <T> boolean remove(T doc) {
        return this.remove(this.getDocumentFromPojo(doc).getId());
    }

    public boolean putDesignDocument(DesignDocument designDocument, String docId) {
        PutDesignDocumentOptions designDocumentOptions =
                new PutDesignDocumentOptions.Builder()
                        .db(this.dbName)
                        .designDocument(designDocument)
                        .ddoc(docId)
                        .build();

        DocumentResult response =
                this.instance.getClient()
                        .putDesignDocument(designDocumentOptions).execute()
                        .getResult();
        boolean success = response.isOk();
        if (!success) {
            log.error("Unable to put design document {} to {}. Error: {}",
                    designDocument.getId(), docId, response.getError());
        }
        return success;
    }

    public void createIndex(IndexDefinition indexDefinition, String indexName,
                            String indexType) {
        PostIndexOptions indexOptions = new PostIndexOptions.Builder()
                .db(this.dbName)
                .index(indexDefinition)
                .name(indexName)
                .type(indexType)
                .build();

        try {
            this.instance.getClient().postIndex(indexOptions).execute().getResult();
        } catch (ServiceResponseException e) {
            log.error("Error creating index", e);
        }
    }

    public <T> QueryResult<T> getQueryResult(String query, Class<T> type) {
        return database.query(query, type);
    }

    public <T> Set<String> getDistinctSortedStringKeys(@NotNull Class<T> type, String viewName) {
        PostViewOptions viewOptions = new PostViewOptions.Builder()
                .db(this.dbName)
                .ddoc(type.getSimpleName())
                .view(viewName)
                .includeDocs(false)
                .build();
        try {
            ViewResult response = this.instance.getClient().postView(viewOptions).execute().getResult();
            return response.getRows().stream()
                    .map(r -> r.getKey().toString()).collect(Collectors.toCollection(TreeSet::new));
        } catch (ServiceResponseException e) {
            log.error("Error in getting project groups", e);
        }
        return Collections.emptySet();
    }

    public <T> List<T> getDocsByListIds(Class<T> type, Collection<String> ids) {
        if (!CommonUtils.isNotEmpty(ids))
            return Collections.emptyList();
        try {
            PostAllDocsOptions postAllDocsOption = new PostAllDocsOptions.Builder()
                    .db(this.dbName)
                    .includeDocs(true)
                    .keys(ids.stream().toList())
                    .build();

            AllDocsResult resp = this.instance.getClient().postAllDocs(postAllDocsOption).execute().getResult();
            return resp.getRows().stream()
                    .map(r -> this.getPojoFromDocument(r.getDoc(), type))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (ServiceResponseException e) {
            log.error("Error fetching documents", e);
            return Collections.emptyList();
        }
    }

    public Document getDocumentFromPojo(Object document) {
        if (document instanceof Document) {
            return (Document) document;
        }
        Document doc = new Document();
        Gson gson = this.instance.getGson();
        doc.setProperties(gson.fromJson(gson.toJson(document), Map.class));
        return doc;
    }

    public <T> T getPojoFromDocument(@NotNull Document document, Class<T> type) {
        return this.instance.getGson().fromJson(document.toString(), type);
    }

    public boolean contains(String docId) {
        HeadDocumentOptions documentOptions =
                new HeadDocumentOptions.Builder()
                        .db(this.dbName)
                        .docId(docId)
                        .build();

        return this.instance.getClient().headDocument(documentOptions).execute()
                .getStatusCode() == 200;
    }
}

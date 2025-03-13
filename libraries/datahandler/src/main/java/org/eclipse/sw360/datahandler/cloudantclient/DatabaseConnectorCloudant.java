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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ibm.cloud.cloudant.v1.Cloudant;
import com.ibm.cloud.cloudant.v1.model.*;
import com.ibm.cloud.sdk.core.service.exception.NotFoundException;
import com.ibm.cloud.sdk.core.service.exception.ServiceResponseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TBase;
import org.apache.thrift.TFieldIdEnum;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
            if (document instanceof AttachmentContent content) {
                InputStream in = getAttachment(content.getId(), content.getFilename());
                resp = this.updateWithResponse(document);
                createAttachment(resp.getId(), content.getFilename(), in, content.getContentType());
            } else {
                resp = this.updateWithResponse(document);
            }
            updateIdAndRev(document, resp.getId(), resp.getRev());
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

    /**
     * Get a document from DB and convert to SW360 type.
     * @param type Type to translate the document to.
     * @param id   Document ID
     * @return Document of type if found.
     * @param <T> Type to translate the document to.
     */
    public <T> T get(Class<T> type, String id) {
        try {
            Document doc = getDocument(id);
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
        } catch (IllegalAccessException | SW360Exception e) {
            log.error("Error fetching document of type {} with id {} : {}",
                    type.getSimpleName(), id, e.getMessage());
            return null;
        }
    }

    /**
     * Get a design document from DB with dDoc
     * @param ddoc ddoc of Design Document
     * @return Design Document if found. Null otherwise.
     */
    public DesignDocument getDesignDoc(String ddoc) {
        try {
            GetDesignDocumentOptions designDocumentOptions = new GetDesignDocumentOptions.Builder()
                    .db(this.dbName)
                    .ddoc(ddoc)
                    .latest(true)
                    .build();

            return this.getInstance().getClient().getDesignDocument(designDocumentOptions)
                    .execute()
                    .getResult();
        } catch (NotFoundException e) {
            log.error("Error fetching design document with id _design/{} : {}",
                    ddoc, e.getMessage());
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
        if (!contains(id)) {
            return false;
        }
        DeleteDocumentOptions deleteOption = new DeleteDocumentOptions.Builder()
                .db(this.dbName)
                .docId(id)
                .build();

        return deleteDocumentWithOption(deleteOption);
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

    /**
     * Get Cloudant Document from DB for give ID. Will use GET call for most
     * request and POST for IDs with `+`.
     * @param id Document ID
     * @return Document if found. Empty document otherwise.
     * @see DatabaseConnectorCloudant::getDocumentWithPost()
     */
    public Document getDocument(@NotNull String id) throws SW360Exception {
        if (id.isEmpty()) {
            throw new SW360Exception("Document id cannot be empty");
        }
        if (id.contains("+")) {
            return getDocumentWithPost(id);
        }
        return getDocumentWithGet(id);
    }

    /**
     * Use GET request to get document (with caching)
     * @param id Document ID
     * @return Document if exists.
     * @throws SW360Exception If document is not found for ID
     */
    private Document getDocumentWithGet(String id) throws SW360Exception {
        GetDocumentOptions getDocOption = new GetDocumentOptions.Builder()
                .db(this.dbName)
                .docId(id)
                .build();

        try {
            return this.instance.getClient().getDocument(getDocOption).execute().getResult();
        } catch (NotFoundException e) {
            throw new SW360Exception("Cannot find document: " + id);
        }
    }

    /**
     * Get document with `+` in id with POST call. Known issue with CloudantSDK
     * https://github.com/IBM/cloudant-java-sdk/blob/51b7da64dea925dc1dd0b2a980dba93e0c899297/KNOWN_ISSUES.md#path-elements-containing-the--character
     * @param id Document ID
     * @return Document if found.
     * @throws SW360Exception If document is not found for ID
     */
    private Document getDocumentWithPost(String id) throws SW360Exception {
        List<String> idList = List.of(id);

        PostAllDocsOptions postDocsOption = new PostAllDocsOptions.Builder()
                .db(this.dbName)
                .keys(idList)
                .includeDocs(true)
                .build();

        try {
            AllDocsResult resp = this.instance.getClient().postAllDocs(postDocsOption).execute().getResult();

            if (resp.getRows().isEmpty() || resp.getRows().get(0).getDoc() == null) {
                return new Document();
            }

            return resp.getRows().get(0).getDoc();
        } catch (NotFoundException e) {
            throw new SW360Exception("Cannot find document: " + id);
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

            return resp.getRows().stream().map(r -> {
                if (r.getError() != null && r.getDoc() == null) {
                    return null;
                }
                return this.getPojoFromDocument(r.getDoc(), type);
            }).filter(Objects::nonNull).collect(Collectors.toList());
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
                updateIdAndRev(entities[i], responses.get(i).getId(), responses.get(i).getRev());
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
                    .keys(Collections.singletonList(keys))
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
        if (!contains(id)) {
            return false;
        }

        Document doc;
        try {
            doc = getDocument(id);
        } catch (SW360Exception e) {
            return false;
        }
        DeleteDocumentOptions deleteOption = new DeleteDocumentOptions.Builder()
                .db(this.dbName)
                .docId(id)
                .rev(doc.getRev())
                .build();

        return deleteDocumentWithOption(deleteOption);
    }

    private boolean deleteDocumentWithOption(DeleteDocumentOptions deleteOption) {
        DocumentResult resp;
        boolean success;
        try {
            resp = this.instance.getClient().deleteDocument(deleteOption).execute().getResult();
            success = resp.isOk();
        } catch (ServiceResponseException e) {
            log.error("Error deleting document with id {}", deleteOption.docId(), e);
            success = false;
        }
        if (!success) {
            log.error("Could not delete document with id: {}", deleteOption.docId());
        }
        return success;
    }

    public <T> boolean add(T doc) throws SW360Exception {
        Document document = this.getDocumentFromPojo(doc);
        if (document.getId() != null && this.contains(document.getId())) {
            // Cannot add same document again. Must update.
            return false;
        }

        DocumentResult resp;
        if (doc instanceof AttachmentContent content) {
            if (content.getFilename() == null) {
                throw new SW360Exception("Attachment filename cannot be null.");
            }
            PostDocumentOptions postDocOption = new PostDocumentOptions.Builder()
                    .db(this.dbName)
                    .document(document)
                    .build();

            DocumentResult createResp = this.instance.getClient().postDocument(postDocOption).execute().getResult();
            InputStream in = new ByteArrayInputStream(content.getFilename()
                    .getBytes(StandardCharsets.UTF_8));
            createAttachment(createResp.getId(), content.getFilename(), in, content.getContentType());
            Document updatedDoc = this.getDocument(createResp.getId());
            updateIdAndRev(doc, updatedDoc.getId(), updatedDoc.getRev());
            return true;
        }
        PostDocumentOptions postDocOption = new PostDocumentOptions.Builder()
                .db(this.dbName)
                .document(document)
                .build();

        resp = this.instance.getClient().postDocument(postDocOption).execute().getResult();
        updateIdAndRev(doc, resp.getId(), resp.getRev());
        return resp.isOk();
    }

    public <T> boolean remove(T doc) {
        return this.remove(this.getDocumentFromPojo(doc).getId());
    }

    public boolean putDesignDocument(DesignDocument designDocument, String ddoc) {
        DesignDocument existingDoc = getDesignDocument(ddoc);
        if (existingDoc != null) {
            designDocument.setId(existingDoc.getId());
            designDocument.setRev(existingDoc.getRev());
        }
        PutDesignDocumentOptions designDocumentOptions =
            new PutDesignDocumentOptions.Builder()
                .db(this.dbName)
                .designDocument(designDocument)
                .ddoc(ddoc)
                .build();

        DocumentResult response =
            this.instance.getClient()
                .putDesignDocument(designDocumentOptions).execute()
                .getResult();
        boolean success = response.isOk();
        if (!success) {
            log.error("Unable to put design document {} to {}. Error: {}",
                    designDocument.getId(), ddoc, response.getError());
        } else {
            designDocument.setId(response.getId());
            designDocument.setRev(response.getRev());
        }
        return success;
    }

    public DesignDocument getDesignDocument(String ddoc) {
        GetDesignDocumentOptions designDocumentOptions = new GetDesignDocumentOptions.Builder()
            .db(this.dbName)
            .ddoc(ddoc)
            .latest(true)
            .build();

        try {
            return this.instance.getClient().getDesignDocument(designDocumentOptions).execute()
                .getResult();
        } catch (NotFoundException e) {
            return null;
        }
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

    public PostFindOptions.Builder getQueryBuilder() {
        return new PostFindOptions.Builder().db(this.dbName);
    }

    public <T> List<T> getQueryResult(PostFindOptions query, Class<T> type) {
        List<T> results = new ArrayList<>();
        try {
            FindResult result = this.instance.getClient().postFind(query).execute().getResult();
            if (result != null) {
                results = result.getDocs().stream()
                        .map(r -> getPojoFromDocument(r, type))
                        .toList();
            }
        } catch (ServiceResponseException e) {
            log.error("Error getting query result", e);
        }
        return results;
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
        Map<String, Object> map;

        if (isInstanceOfOAuthClientEntity(document)) {
            ObjectMapper objectMapper = new ObjectMapper();
            map = objectMapper.convertValue(document, new TypeReference<Map<String, Object>>() {});
        } else {
            Gson gson = this.instance.getGson();
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            map = gson.fromJson(gson.toJson(document), type);
        }

        if (map.containsKey("id")) {
            if (!((String) map.get("id")).isEmpty()) {
                doc.setId((String) map.get("id"));
            }
            map.remove("id");
        }
        if (map.containsKey("_id")) {
            if (map.get("_id") != null && !((String) map.get("_id")).isEmpty()) {
                doc.setId((String) map.get("_id"));
            }
            map.remove("_id");
        }
        if (map.containsKey("rev")) {
            if (!((String) map.get("rev")).isEmpty()) {
                doc.setRev((String) map.get("rev"));
            }
            map.remove("rev");
        }
        if (map.containsKey("revision")) {
            if (!((String) map.get("revision")).isEmpty()) {
                doc.setRev((String) map.get("revision"));
            }
            map.remove("revision");
        }
        if (map.containsKey("_rev")) {
            if (map.get("_rev") != null && !((String) map.get("_rev")).isEmpty()) {
                doc.setRev((String) map.get("_rev"));
            }
            map.remove("_rev");
        }
        doc.setProperties(map);
        return doc;
    }

    public <T> T getPojoFromDocument(@NotNull Document document, Class<T> type) {
        T doc = null;
        try {
            if (type.getSimpleName().equals("OAuthClientEntity"))  {
                ObjectMapper objectMapper = new ObjectMapper();
                doc = objectMapper.readValue(document.toString(), type);
            } else {
                doc = this.instance.getGson().fromJson(document.toString(), type);
            }
            updateIdAndRev(doc, document.getId(), document.getRev());
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
        return doc;
    }

    private <T> void updateIdAndRev(@NotNull T doc, String docId, String docRev) {
        if (TBase.class.isAssignableFrom(doc.getClass())) {
            TBase tbase = (TBase) doc;
            TFieldIdEnum id = tbase.fieldForId(1);
            TFieldIdEnum rev = tbase.fieldForId(2);
            tbase.setFieldValue(id, docId);
            tbase.setFieldValue(rev, docRev);
        }  else if (isInstanceOfOAuthClientEntity(doc)) {
            Class<?> clazz = doc.getClass();
            try {
                Method setIdMethod = clazz.getMethod("setId", String.class);
                setIdMethod.invoke(doc, docId);

                Method setRevMethod = clazz.getMethod("setRev", String.class);
                setRevMethod.invoke(doc, docRev);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                log.error(e.getMessage());
            }
        }
    }

    private <T> boolean isInstanceOfOAuthClientEntity(T doc) {    
        return doc.getClass().getSimpleName().equals("OAuthClientEntity");
    }

    public boolean contains(@NotNull String docId) {
        if (docId.isEmpty()) {
            return false;
        }
        HeadDocumentOptions documentOptions =
                new HeadDocumentOptions.Builder()
                        .db(this.dbName)
                        .docId(docId)
                        .build();

        try {
            return this.instance.getClient().headDocument(documentOptions).execute()
                    .getStatusCode() == 200;
        } catch (NotFoundException e) {
            return false;
        }
    }

    /**
     * Generates an $eq selector for given field with given value.
     * @param field Field name
     * @param value Value to match
     * @return New selector
     */
    public static @NotNull Map<String, Object> eq(String field, String value) {
        return Collections.singletonMap(field,
                Collections.singletonMap("$eq", value));
    }

    /**
     * Generates an $exists selector for given field with given value.
     * @param field Field name
     * @param value Value to check
     * @return New selector
     */
    public static @NotNull Map<String, Object> exists(String field, boolean value) {
        return Collections.singletonMap(field,
                Collections.singletonMap("$exists", value));
    }

    /**
     * Generates an $and selector for list of selectors.
     * @param selectors Selectors to combine
     * @return New selector
     */
    public static @NotNull Map<String, Object> and(List<Map<String, Object>> selectors) {
        return Collections.singletonMap("$and",
                selectors);
    }

    /**
     * Generates an $or selector for list of selectors.
     * @param selectors Selectors to combine
     * @return New selector
     */
    public static @NotNull Map<String, Object> or(List<Map<String, Object>> selectors) {
        return Collections.singletonMap("$or",
                selectors);
    }

    /**
     * Generates an $elemMatch selector with and $eq selector for the field and value
     * @param field Field name
     * @param value Value to match
     * @return New selector
     */
    public static @NotNull Map<String, Object> elemMatch(String field, String value) {
        return Collections.singletonMap(field,
                eq("$elemMatch", value));
    }
}

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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TBase;
import org.apache.thrift.TFieldIdEnum;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.Source;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;

import com.ibm.cloud.cloudant.v1.model.DocumentResult;
import com.ibm.cloud.cloudant.v1.model.DesignDocument;
import com.ibm.cloud.cloudant.v1.model.DesignDocumentViewsMapReduce;
import com.ibm.cloud.cloudant.v1.model.IndexDefinition;
import com.ibm.cloud.cloudant.v1.model.IndexField;
import com.ibm.cloud.cloudant.v1.model.PostViewOptions;
import com.ibm.cloud.cloudant.v1.model.ViewResult;
import com.ibm.cloud.cloudant.v1.model.ViewResultRow;
import com.ibm.cloud.sdk.core.service.exception.ServiceResponseException;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.jetbrains.annotations.NotNull;

/**
 * Access the database in a CRUD manner, for a generic class
 *
 */
public class DatabaseRepositoryCloudantClient<T> {

    protected final Logger log = LogManager.getLogger(DatabaseConnectorCloudant.class);
    private static final char HIGH_VALUE_UNICODE_CHARACTER = '\uFFF0';

    private final Class<T> type;
    private DatabaseConnectorCloudant connector;

    public void initStandardDesignDocument(Map<String, DesignDocumentViewsMapReduce> views,
                                           @NotNull DatabaseConnectorCloudant db) {
        String ddocId = type.getSimpleName();
        DesignDocument newDdoc = new DesignDocument.Builder()
                .views(views)
                .build();
        db.putDesignDocument(newDdoc, ddocId);
    }

    public DesignDocumentViewsMapReduce createMapReduce(String map, String reduce) {
        DesignDocumentViewsMapReduce.Builder mrBuilder = new DesignDocumentViewsMapReduce.Builder()
                .map(map);
        if (reduce != null) {
            mrBuilder.reduce(reduce);
        }
        return mrBuilder.build();
    }

    public void createIndex(String indexName, String[] fields, DatabaseConnectorCloudant db) {
        IndexDefinition.Builder indexDefinitionBuilder = new IndexDefinition.Builder();
        for (String fieldName : fields) {
            IndexField field = new IndexField.Builder()
                    .add(fieldName, "asc")
                    .build();
            indexDefinitionBuilder.addFields(field);
        }

        db.createIndex(indexDefinitionBuilder.build(), indexName, "json");
    }

    protected DatabaseConnectorCloudant getConnector() {
        return connector;
    }

    public List<T> queryByIds(String viewName, Collection<String> ids) {
        PostViewOptions query = connector.getPostViewQueryBuilder(type, viewName)
                .includeDocs(true)
                .keys(ids.stream().map(r -> (Object)r).toList())
                .build();
        return queryView(query);
    }

    public DatabaseRepositoryCloudantClient(DatabaseConnectorCloudant connector, Class<T> type) {
        this.type = type;
        this.connector = connector;
    }

    protected DatabaseRepositoryCloudantClient(Class<T> type) {
        this.type = type;
    }

    protected void setConnector(DatabaseConnectorCloudant connector) {
        this.connector = connector;
    }

    public Set<String> queryForIdsAsValue(String queryName, String key) {
        PostViewOptions.Builder query = connector.getPostViewQueryBuilder(type, queryName);
        return queryForIdsAsValue(query, key);
    }

    public Set<String> queryForIdsAsValue(String queryName, Set<String> keys) {
        PostViewOptions.Builder query = connector.getPostViewQueryBuilder(type, queryName);
        return queryForIdsAsValue(query, keys);
    }

    public Set<String> queryForIdsAsValue(PostViewOptions.Builder query, Set<String> keys) {
        PostViewOptions req = query
                .keys(keys.stream().map(r -> (Object)r).toList())
                .build();
        return queryForIdsFromReqBuilder(req);
    }

    public Set<String> queryForIdsAsValue(PostViewOptions.Builder queryBuilder, String key) {
        PostViewOptions query = queryBuilder.includeDocs(true).keys(Collections.singletonList(key)).build();
        ViewResult viewResponse = null;
        try {
            viewResponse = getConnector().getPostViewQueryResponse(query);
        } catch (ServiceResponseException e) {
            log.error("Error executing query for ids as value", e);
        }
        HashSet<Object> ids = new HashSet<>();
        if (viewResponse != null) {
            for (ViewResultRow row : viewResponse.getRows()) {
                ids.add(row.getValue());
            }
        }
        return ids.stream().map(Object::toString).collect(Collectors.toSet());
    }

    public Set<String> queryForIdsAsValue(PostViewOptions.Builder query) {
        PostViewOptions reqBuild = query.includeDocs(true).build();
        return queryForIdsFromReqBuilder(reqBuild);
    }

    public List<T> queryView(String viewName, String startKey, String endKey) {
        PostViewOptions query = connector.getPostViewQueryBuilder(type, viewName)
                .startKey(startKey)
                .endKey(endKey).includeDocs(true).build();
        return queryView(query);
    }

    public List<T> queryView(String viewName, String key) {
        PostViewOptions query = connector.getPostViewQueryBuilder(type, viewName)
                .keys(Collections.singletonList(key))
                .includeDocs(true).build();
        return queryView(query);
    }

    public List<T> queryView(String viewName) {
        PostViewOptions query = connector.getPostViewQueryBuilder(type, viewName)
                .includeDocs(true).build();
        return queryView(query);
    }

    public Set<String> queryForIds(PostViewOptions query) {
        ViewResult response = this.connector.getPostViewQueryResponse(query);
        HashSet<String> ids = new HashSet<>();
        for (ViewResultRow row : response.getRows()) {
            ids.add(row.getId());
        }
        return ids;
    }

    public Set<String> queryForIdsAsValue(PostViewOptions query) {
        ViewResult response = this.connector.getPostViewQueryResponse(query);
        HashSet<Object> ids = new HashSet<>();
        for (ViewResultRow row : response.getRows()) {
            ids.add(row.getValue());
        }
        return ids.stream().map(Object::toString).collect(Collectors.toSet());
    }

    public Set<String> queryForIdsFromReqBuilder(PostViewOptions query) {
        ViewResult response = queryQueryResponse(query);
        HashSet<Object> ids = new HashSet<>();
        for (ViewResultRow row : response.getRows()) {
            ids.add(row.getValue());
        }
        return ids.stream().map(Object::toString).collect(Collectors.toSet());
    }

    public List<T> queryByPrefix(String viewName, String key) {
        return queryView(viewName, key, key + HIGH_VALUE_UNICODE_CHARACTER);
    }

    public Set<String> queryForIdsByPrefix(String viewName, String prefix) {
        return queryForIds(viewName, prefix, prefix + HIGH_VALUE_UNICODE_CHARACTER);
    }

    public Set<String> queryForIdsAsValueByPrefix(String viewName, String prefix) {
        return queryForIdsAsValue(viewName, prefix, prefix + HIGH_VALUE_UNICODE_CHARACTER);
    }

    public Set<String> queryForIds(String queryName, String startKey, String endKey) {
        PostViewOptions query = connector.getPostViewQueryBuilder(type, queryName)
                .startKey(startKey)
                .endKey(endKey)
                .build();
        return queryForIds(query);
    }

    public Set<String> queryForIdsAsValue(String queryName, String startKey, String endKey) {
        PostViewOptions query = connector.getPostViewQueryBuilder(type, queryName)
                .startKey(startKey)
                .endKey(endKey)
                .build();
        return queryForIdsAsValue(query);
    }

    public Set<String> queryForIds(String queryName, String key) {
        PostViewOptions query = connector.getPostViewQueryBuilder(type, queryName)
                .keys(Collections.singletonList(key)).build();
        return queryForIds(query);
    }

    public Set<String> queryForIdsAsComplexValue(String queryName, String... keys) {
        PostViewOptions query = connector.getPostViewQueryBuilder(type, queryName)
                .keys(Collections.singletonList(keys)).build();
        return queryForIds(query);
    }

    public Set<String> queryForIdsAsComplexValues(String queryName, Map<String, Set<String>> keys) {
        List<Object> complexKeys = keys.entrySet().stream()
                .map(DatabaseRepositoryCloudantClient::createComplexKeys)
                .flatMap(Collection::stream)
                .map(r -> (Object)r)
                .collect(Collectors.toList());
        PostViewOptions query = connector.getPostViewQueryBuilder(type, queryName)
                .keys(complexKeys)
                .build();
        return queryForIds(query);
    }

    public Set<String> queryForIdsOnlyComplexKey(String queryName, String key) {
        return queryForIdsOnlyComplexKeys(queryName, Collections.singleton(key));
    }

    public Set<String> queryForIdsOnlyComplexKeys(String queryName, Set<String> keys) {
        Set<String> queryResult = new HashSet<>();
        for (String key : keys) {
            String[] startKeys = new String[] { key };
            String[] endKeys = new String[] { key, "\ufff0" };
            queryResult.addAll(queryForIds(queryName, startKeys, endKeys));
        }
        return queryResult;
    }

    public Collection<? extends String> queryForIds(String queryName, String[] startKeys, String[] endKeys) {
        PostViewOptions query = connector.getPostViewQueryBuilder(type, queryName)
                .startKey(startKeys)
                .endKey(endKeys)
                .build();
        return queryForIds(query);
    }

    private static Set<String[]> createComplexKeys(Map.Entry<String, Set<String>> key) {
        return key.getValue().stream().map(v -> new String[] { key.getKey(), v }).collect(Collectors.toSet());
    }

    public List<T> queryView(PostViewOptions req) {
        List<T> docList = Lists.newArrayList();
        try {
            ViewResult response = getConnector().getPostViewQueryResponse(req);
            docList = getPojoFromViewResponse(response);
        } catch (ServiceResponseException e) {
            log.warn("Error in getting documents", e);
        }
        return docList;
    }

    public @NotNull List<T> getPojoFromViewResponse(@NotNull ViewResult response) {
        return response.getRows().stream()
                .map(r -> connector.getPojoFromDocument(r.getDoc(), type))
                .toList();
    }

    public List<Source> queryViewForSource(PostViewOptions req) {
        List<Source> sources = Lists.newArrayList();
        Gson gson = new Gson();
        try {
            ViewResult response = getConnector().getPostViewQueryResponse(req);
            for (ViewResultRow row : response.getRows()) {
                Type t = new TypeToken<Map<String, String>>() {}.getType();
                Map<String, String> srcMap = gson.fromJson(new Gson().toJson(row.getValue()), t);
                Source._Fields type = Source._Fields.findByName(srcMap.keySet().iterator().next());
                Source source = new Source(type, srcMap.values().iterator().next());
                sources.add(source);
            }
        } catch (ServiceResponseException e) {
            log.warn("Error in getting source", e);
        }
        return sources;
    }

    public List<Attachment> queryViewForAttachment(PostViewOptions req) {
        List<Attachment> attachments = Lists.newArrayList();
        Gson gson = new Gson();
        try {
            ViewResult response = getConnector().getPostViewQueryResponse(req);
            for (ViewResultRow row : response.getRows()) {
                Attachment value = gson.fromJson(new Gson().toJson(row.getValue()), Attachment.class);
                attachments.add(value);
            }
        } catch (ServiceResponseException e) {
            log.warn("Error in getting attachment", e);
        }
        return attachments;
    }

    public ViewResult queryQueryResponse(PostViewOptions req) {
        ViewResult viewResp = null;
        try {
            viewResp = getConnector().getPostViewQueryResponse(req);
        } catch (ServiceResponseException e) {
            log.warn("Error in query execution", e);
        }
        return viewResp;
    }

    public ViewResult queryViewForComplexKeys(PostViewOptions req) {
        ViewResult responses = null;
        try {
            responses = getConnector().getPostViewQueryResponse(req);
        } catch (ServiceResponseException e) {
            log.error("Error executing query view with complex keys", e);
        }
        return responses;
    }

    public PostViewOptions buildRequest(PostViewOptions.Builder viewQuery, Collection<String> ids) {
        return viewQuery.includeDocs(false)
                .keys(ids.stream().map(r -> (Object)r).toList())
                .build();
    }

    public boolean add(T doc) throws SW360Exception {
        return connector.add(doc);
    }

    public void update(T doc) {
        connector.update(doc);
    }

    /**
     * This function should NOT be used for updating document containing Attachment.
     * Ex: Project, Component & Release
     */
    public DocumentResult updateWithResponse(T doc) {
        return connector.updateWithResponse(doc);
    }

    public boolean remove(T doc) {
        if (TBase.class.isAssignableFrom(doc.getClass())) {
            TBase tbase = (TBase) doc;
            TFieldIdEnum id = tbase.fieldForId(1);
            String docId = (String) tbase.getFieldValue(id);
            return connector.deleteById(docId);
        } else if (doc.getClass().getSimpleName().equals("OAuthClientEntity")) {
            Class<?> clazz = doc.getClass();
            try {
                Method getIdMethod = clazz.getMethod("getId");
                String id = (String) getIdMethod.invoke(doc);
                return connector.deleteById(id);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                log.error(e.getMessage());
            }
        }
        return connector.remove(doc);
    }

    public Set<String> getAllIds() {
        return connector.getAllIds(type);
    }

    public T get(String id) {
        return connector.get(type, id);
    }

    public List<T> getAll() {
        return connector.getAll(type);
    }

    public boolean remove(String id) {
        return connector.deleteById(id);
    }

    public List<T> get(Collection<String> ids) {
        return connector.get(type, ids);
    }

    public List<T> get(Collection<String> ids, boolean ignoreNotFound) {
        return get(ids);
    }

    public List<DocumentResult> executeBulk(Collection<?> list) {
        return connector.executeBulk(list);
    }

    public List<DocumentResult> deleteIds(Collection<String> ids) {
        return connector.deleteIds(ids);
    }

    public int getDocumentCount() {
        return connector.getDocumentCount(type);
    }

    public List<T> getDocsByListIds(Collection<String> ids) {
        return connector.getDocsByListIds(type, ids);
    }
}
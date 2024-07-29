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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.thrift.Source;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;

import com.ibm.cloud.cloudant.v1.model.DocumentResult;
import com.ibm.cloud.cloudant.v1.model.DesignDocument;
import com.ibm.cloud.cloudant.v1.model.DesignDocumentViewsMapReduce;
import com.ibm.cloud.cloudant.v1.model.IndexDefinition;
import com.ibm.cloud.cloudant.v1.model.IndexField;
import com.google.common.collect.Lists;
import com.google.gson.Gson;

/**
 * Access the database in a CRUD manner, for a generic class
 *
 */
public class DatabaseRepositoryCloudantClient<T> {

    protected final Logger log = LogManager.getLogger(DatabaseConnectorCloudant.class);
    private static final char HIGH_VALUE_UNICODE_CHARACTER = '\uFFF0';

    private final Class<T> type;
    private final DatabaseConnectorCloudant connector;

    public void initStandardDesignDocument(Map<String, DesignDocumentViewsMapReduce> views,
                                           DatabaseConnectorCloudant db) {
        String ddocId = "_design/" + type.getSimpleName();
        DesignDocument newDdoc = new DesignDocument.Builder()
                .id(ddocId)
                .build();
        DesignDocument ddoc = db.get(DesignDocument.class, ddocId);
        if (ddoc == null) {
            db.add(newDdoc);
        }
        DesignDocument ddocFinal = db.get(DesignDocument.class, ddocId);
        ddocFinal.setViews(views);
        db.update(ddocFinal);
        db.putDesignDocument(ddocFinal, ddocId);
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
        String[] idStrs = new String[ids.size()];
        int index = 0;
        for (String str : ids)
            idStrs[index++] = str;
        ViewRequestBuilder query = connector.createQuery(type, viewName);
        UnpaginatedRequestBuilder reqBuilder = query.newRequest(Key.Type.STRING, Object.class).includeDocs(true)
                .keys(idStrs);
        return queryView(reqBuilder);
    }

    public DatabaseRepositoryCloudantClient(DatabaseConnectorCloudant connector, Class<T> type) {
        this.type = type;
        this.connector = connector;
    }

    public Set<String> queryForIdsAsValue(String queryName, String key) {
        ViewRequestBuilder query = connector.createQuery(type, queryName);
        return queryForIdsAsValue(query, key);
    }

    public Set<String> queryForIdsAsValue(String queryName, Set<String> keys) {
        ViewRequestBuilder query = connector.createQuery(type, queryName);
        return queryForIdsAsValue(query, keys);
    }

    public Set<String> queryForIdsAsValue(ViewRequestBuilder query, Set<String> keys) {
        String[] arrayOfString = new String[keys.size()];
        int index = 0;
        for (String str : keys)
            arrayOfString[index++] = str;
        UnpaginatedRequestBuilder req = query.newRequest(Key.Type.STRING, Object.class).keys(arrayOfString);
        return queryForIdsFromReqBuilder(req);
    }

    public Set<String> queryForIdsAsValue(ViewRequestBuilder query, String key) {
        ViewResponse<String, Object> viewReponse = null;
        try {
            viewReponse = query.newRequest(Key.Type.STRING, Object.class).includeDocs(true).keys(key).build()
                    .getResponse();
        } catch (IOException e) {
            log.error("Error executing query for ids as value", e);
        }
        HashSet<Object> ids = new HashSet<>();
        for (ViewResponse.Row row : viewReponse.getRows()) {
            ids.add(row.getValue());
        }
        return ids.stream().map(Object::toString).collect(Collectors.toSet());
    }

    public Set<String> queryForIdsAsValue(ViewRequestBuilder query) {
        UnpaginatedRequestBuilder reqBuild = query.newRequest(Key.Type.STRING, Object.class).includeDocs(true);
        return queryForIdsFromReqBuilder(reqBuild);
    }

    public List<T> queryView(String viewName, String startKey, String endKey) {
        ViewRequestBuilder query = connector.createQuery(type, viewName);
        UnpaginatedRequestBuilder reqBuild = query.newRequest(Key.Type.STRING, Object.class).startKey(startKey)
                .endKey(endKey).includeDocs(true);
        return queryView(reqBuild);
    }

    public List<T> queryView(String viewName, String key) {
        ViewRequestBuilder query = connector.createQuery(type, viewName);
        UnpaginatedRequestBuilder reqBuild = query.newRequest(Key.Type.STRING, Object.class).keys(key)
                .includeDocs(true);
        return queryView(reqBuild);
    }

    public List<T> queryView(String viewName) {
        ViewRequestBuilder query = connector.createQuery(type, viewName);
        UnpaginatedRequestBuilder reqBuild = query.newRequest(Key.Type.STRING, Object.class).includeDocs(true);
        return queryView(reqBuild);
    }

    public Set<String> queryForIds(UnpaginatedRequestBuilder query) {
        ViewResponse<String, Object> rows = queryQueryResponse(query);
        HashSet<Object> ids = new HashSet<>();
        for (ViewResponse.Row row : rows.getRows()) {
            ids.add(row.getId());
        }
        return ids.stream().map(Object::toString).collect(Collectors.toSet());
    }

    public Set<String> queryForIdsAsValue(UnpaginatedRequestBuilder query) {
        ViewResponse<String, Object> rows = queryQueryResponse(query);
        HashSet<Object> ids = new HashSet<>();
        for (ViewResponse.Row row : rows.getRows()) {
            ids.add(row.getValue());
        }
        return ids.stream().map(Object::toString).collect(Collectors.toSet());
    }

    public Set<String> queryForIdsFromReqBuilder(UnpaginatedRequestBuilder query) {
        ViewResponse<String, Object> rows = queryQueryResponse(query);
        HashSet<Object> ids = new HashSet<>();
        for (ViewResponse.Row row : rows.getRows()) {
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
        ViewRequestBuilder query = connector.createQuery(type, queryName);
        UnpaginatedRequestBuilder req = query.newRequest(Key.Type.STRING, Object.class).startKey(startKey)
                .endKey(endKey);
        return queryForIds(req);
    }

    public Set<String> queryForIdsAsValue(String queryName, String startKey, String endKey) {
        ViewRequestBuilder query = connector.createQuery(type, queryName);
        UnpaginatedRequestBuilder req = query.newRequest(Key.Type.STRING, Object.class).startKey(startKey)
                .endKey(endKey);
        return queryForIdsAsValue(req);
    }

    public Set<String> queryForIds(String queryName, String key) {
        ViewRequestBuilder query = connector.createQuery(type, queryName);
        UnpaginatedRequestBuilder reqBuild = query.newRequest(Key.Type.STRING, Object.class).keys(key);
        return queryForIds(reqBuild);
    }

    public Set<String> queryForIdsAsComplexValue(String queryName, String... keys) {
        ViewRequestBuilder query = connector.createQuery(type, queryName);
        UnpaginatedRequestBuilder reqBuild = query.newRequest(Key.Type.STRING, Object.class).keys(keys);
        return queryForIds(reqBuild);
    }

    public Set<String> queryForIdsAsComplexValues(String queryName, Map<String, Set<String>> keys) {
        Set<String[]> complexKeys = keys.entrySet().stream().map(DatabaseRepositoryCloudantClient::createComplexKeys)
                .flatMap(Collection::stream).collect(Collectors.toSet());
        ViewRequestBuilder query = connector.createQuery(type, queryName);
        Key.ComplexKey[] complexKys = new Key.ComplexKey[complexKeys.size()];
        int index = 0;
        for (String[] keyArr : complexKeys) {
            Key.ComplexKey key = Key.complex(keyArr);
            complexKys[index++] = key;
        }
        UnpaginatedRequestBuilder reqBuild = query.newRequest(Key.Type.COMPLEX, Object.class)
                .keys(complexKys);
        return queryForIds(reqBuild);
    }

    public Set<String> queryForIdsOnlyComplexKey(String queryName, String key) {
        return queryForIdsOnlyComplexKeys(queryName, Collections.singleton(key));
    }

    public Set<String> queryForIdsOnlyComplexKeys(String queryName, Set<String> keys) {
        Set<String> queryResult = new HashSet<>();
        for (String key : keys) {
            Key.ComplexKey startKeys = Key.complex(new String[] { key });
            Key.ComplexKey endKeys = Key.complex(new String[] { key, "\ufff0" });
            queryResult.addAll(queryForIds(queryName, startKeys, endKeys));
        }
        return queryResult;
    }

    public Collection<? extends String> queryForIds(String queryName, ComplexKey startKeys, ComplexKey endKeys) {
        ViewRequestBuilder query = connector.createQuery(type, queryName);
        UnpaginatedRequestBuilder reqBuilder = query.newRequest(Key.Type.COMPLEX, Object.class).startKey(startKeys)
                .endKey(endKeys);
        return queryForIds(reqBuilder);
    }

    private static Set<String[]> createComplexKeys(Map.Entry<String, Set<String>> key) {
        return key.getValue().stream().map(v -> new String[] { key.getKey(), v }).collect(Collectors.toSet());
    }

    public List<T> queryView(UnpaginatedRequestBuilder req) {
        List<T> docList = Lists.newArrayList();
        try {
            docList = req.build().getResponse().getDocsAs(type);
        } catch (NoDocumentException | IOException e) {
            log.warn("Error in getting documents", e);
        }
        return docList;
    }

    public List<Source> queryViewForSource(ViewRequest req) {
        List<Source> sources = Lists.newArrayList();
        Gson gson = new Gson();
        try {
            ViewResponse<String, Object> response = req.getResponse();
            for (ViewResponse.Row row : response.getRows()) {
                Map<String, String> srcMap = gson.fromJson(new Gson().toJson(row.getValue()), Map.class);
                Source._Fields type = Source._Fields.findByName(srcMap.keySet().iterator().next());
                Source source = new Source(type, srcMap.values().iterator().next().toString());
                sources.add(source);
            }
        } catch (NoDocumentException | IOException e) {
            log.warn("Error in getting source", e);
        }
        return sources;
    }

    public List<Attachment> queryViewForAttchmnt(ViewRequest req) {
        List<Attachment> attchmnts = Lists.newArrayList();
        Gson gson = new Gson();
        try {
            ViewResponse<String, Object> response = req.getResponse();
            for (ViewResponse.Row row : response.getRows()) {
                Attachment value = gson.fromJson(new Gson().toJson(row.getValue()), Attachment.class);
                attchmnts.add(value);
            }
        } catch (NoDocumentException | IOException e) {
            log.warn("Error in getting attachment", e);
        }
        return attchmnts;
    }

    public ViewResponse queryQueryResponse(UnpaginatedRequestBuilder req) {
        ViewResponse viewResp = null;
        try {
            viewResp = req.build().getResponse();
        } catch (NoDocumentException | IOException e) {
            log.warn("Error in query execution", e);
        }
        return viewResp;
    }

    public List<T> multiRequestqueryView(MultipleRequestBuilder req) {
        List<T> docList = Lists.newArrayList();
        try {
            List<ViewResponse<String, Object>> responses = req.add().build().getViewResponses();
            for (ViewResponse<String, Object> response : responses) {
                docList.addAll(response.getDocsAs(type));
            }
        } catch (IOException e) {
            log.error("Error executing multi request query view", e);
        }
        return docList;
    }

    public List<ViewResponse<String, Object>> multiRequestqueryViewResponse(MultipleRequestBuilder req) {
        List<ViewResponse<String, Object>> responses = null;
        try {
            responses = req.add().build().getViewResponses();
        } catch (IOException e) {
            log.error("Error executing multi request query view response", e);
        }
        return responses;
    }

    public ViewResponse<ComplexKey, Object> queryViewForComplexKeys(
            UnpaginatedRequestBuilder<Key.ComplexKey, Object> req) {
        ViewResponse<ComplexKey, Object> responses = null;
        try {
            responses = req.build().getResponse();
        } catch (IOException e) {
            log.error("Error executing query view with complex keys", e);
        }
        return responses;
    }

    public ViewRequest buildRequest(ViewRequestBuilder viewQuery, Collection<String> ids) {
        String[] idStrs = new String[ids.size()];
        int index = 0;
        for (String str : ids)
            idStrs[index++] = str;
        return viewQuery.newRequest(Key.Type.STRING, Object.class).includeDocs(false).keys(idStrs).build();
    }

    public boolean add(T doc) {
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
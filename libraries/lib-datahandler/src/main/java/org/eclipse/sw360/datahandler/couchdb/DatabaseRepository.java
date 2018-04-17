/*
 * Copyright Siemens AG, 2014-2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.datahandler.couchdb;

import org.ektorp.*;
import org.ektorp.support.CouchDbRepositorySupport;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Access the database in a CRUD manner, for a generic class
 *
 * @author cedric.bodet@tngtech.com
 * @author stefan.jaeger@evosoft.com
 */
public class DatabaseRepository<T> extends CouchDbRepositorySupport<T> {

    private static final char HIGH_VALUE_UNICODE_CHARACTER = '\uFFF0';

    private final Class<T> type;
    private final DatabaseConnector connector;

    protected DatabaseConnector getConnector() {
        return connector;
    }

    public static Set<String> getIds(ViewResult rows) {
        HashSet<String> ids = new HashSet<>();

        for (ViewResult.Row row : rows.getRows()) {
            ids.add(row.getId());
        }
        return ids;
    }

    private static List<String> getIdList(ViewResult rows) {
        return rows.getRows()
                .stream()
                .map(ViewResult.Row::getId)
                .collect(Collectors.toList());
    }

    public static Set<String> getStringValue(ViewResult rows) {
        HashSet<String> ids = new HashSet<>();

        for (ViewResult.Row row : rows.getRows()) {
            ids.add(row.getValue());
        }
        return ids;
    }

    public DatabaseRepository(Class<T> type, DatabaseConnector databaseConnector) {
        super(type, databaseConnector);

        this.connector = databaseConnector;
        this.type = type;
    }

    public Set<String> queryForIds(ViewQuery query) {
        ViewResult rows = connector.queryView(query.includeDocs(false));
        return getIds(rows);
    }

    public List<String> queryForIdList(ViewQuery query) {
        ViewResult rows = connector.queryView(query.includeDocs(false));
        return getIdList(rows);
    }

    public Set<String> queryForIdsAsValue(ViewQuery query) {
        ViewResult rows = connector.queryView(query.includeDocs(false));
        return getStringValue(rows);
    }


    public Set<String> queryForIds(String queryName, String key) {
        ViewQuery query = createQuery(queryName).key(key);
        return queryForIds(query);
    }

    public Set<String> queryForIdsAsValue(String queryName, String key) {
        ViewQuery query = createQuery(queryName).key(key);
        return queryForIdsAsValue(query);
    }

    public Set<String> queryForIdsAsComplexValue(String queryName, String... keys) {
        ViewQuery query = createQuery(queryName).key(ComplexKey.of(keys));
        return queryForIds(query);
    }

    public Set<String> queryForIdsAsValue(String queryName, Set<String> keys) {
        ViewQuery query = createQuery(queryName).keys(keys);
        return queryForIdsAsValue(query);
    }


    public Set<String> queryForIds(String queryName, String startKey, String endKey) {
        ViewQuery query = createQuery(queryName).startKey(startKey).endKey(endKey);
        return queryForIds(query);
    }

    public List<String> getIdListByView(String queryName, boolean descending, int limit) {
        ViewQuery query = createQuery(queryName).descending(descending).limit(limit);
        return queryForIdList(query);
    }

    public Set<String> getAllIds() {
        ViewQuery query = createQuery("all");
        return queryForIds(query);
    }

    public List<T> queryByIds(String viewName, Collection<String> ids) {
        ViewQuery query = createQuery(viewName).includeDocs(true).keys(ids);
        return queryView(query);
    }

    public List<T> queryByPrefix(String viewName, String key) {
        return queryView(viewName, key, key + HIGH_VALUE_UNICODE_CHARACTER);
    }

    public Set<String> queryForIdsByPrefix(String viewName, String prefix) {
        return queryForIds(viewName, prefix, prefix + HIGH_VALUE_UNICODE_CHARACTER);
    }


    public List<T> queryView(String viewName, String startKey, String endKey) {
        ViewQuery query = createQuery(viewName).startKey(startKey).endKey(endKey).includeDocs(true);
        return queryView(query);
    }

    public List<T> queryView(ViewQuery query) {
        return db.queryView(query, type);
    }

    @Override
    public T get(String id) {
        try {
            return super.get(id);
        } catch (DocumentNotFoundException e) {
            log.error("Document ID " + id + " not found!", e);
            return null;
        } catch (DbAccessException e) {
            log.error("Document ID " + id + " could not be successfully converted.", e);
            return null;
        }
    }


    @Override
    public List<T> getAll() {
        try {
            return super.getAll();
        } catch (DocumentNotFoundException e) {
            log.error("Nothing found!", e);
            return null;
        } catch (DbAccessException e) {
            log.error("Documents could not be successfully converted.", e);
            return null;
        } catch (Exception e) {
            log.error("Problem in getAll", e);
            return null;
        }
    }

    public boolean remove(String id) {
        return connector.deleteById(id);
    }

    public List<T> get(Collection<String> ids) {
        return connector.get(type, ids);
    }


    /**
     * Creates, updates all objects in the supplied collection.
     * <p/>
     * If the object has no revision set, it will be created, otherwise it will be updated.
     * <p/>
     * Some documents may successfully be saved and some may not. The response will tell the application which documents
     * were saved or not. In the case of a power failure, when the database restarts some may have been saved and some
     * not.
     *
     * @param list , all objects will have their id and revision set.
     * @return The list will only contain entries for documents that has any kind of error code returned from CouchDB.
     * i.e. the list will be empty if everything was completed successfully.
     */
    public List<DocumentOperationResult> executeBulk(Collection<?> list) {
        try {
            return connector.executeBulk(list);
        } catch (Exception e) {
            log.error("Problem in executeBulk with " + list, e);
            return null;
        }
    }

    /**
     * Deletes all objects in the supplied collection.
     *
     * @param deletionCandidates , the objects that will be deleted
     * @return The list will only contain entries for documents that has any kind of error code returned from CouchDB.
     * i.e. the list will be empty if everything was completed successfully.
     */
    protected List<DocumentOperationResult> deleteBulk(Collection<?> deletionCandidates) {
        return connector.deleteBulk(deletionCandidates);
    }

    public List<DocumentOperationResult> deleteIds(Collection<String> ids) {
        final List<T> deletionCandidates = get(ids);
        return deleteBulk(deletionCandidates);
    }

    /**
     * Returns the total count of documents in the repository (given by the all view)
     */
    public int getDocumentCount() {
        ViewQuery query = createQuery("all").includeDocs(false).limit(0);
        return db.queryView(query).getTotalRows();
    }
}

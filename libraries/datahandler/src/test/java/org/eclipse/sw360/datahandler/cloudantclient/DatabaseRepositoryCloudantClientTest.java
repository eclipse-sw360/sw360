/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.cloudantclient;

import com.ibm.cloud.cloudant.v1.model.DesignDocument;
import com.ibm.cloud.cloudant.v1.model.DesignDocumentViewsMapReduce;
import com.ibm.cloud.cloudant.v1.model.IndexDefinition;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies that {@link DatabaseRepositoryCloudantClient} performs design-document
 * and index initialisation only once per (database, design-doc) pair within the
 * current JVM/classloader.
 */
@RunWith(MockitoJUnitRunner.class)
public class DatabaseRepositoryCloudantClientTest {

    @Mock
    private DatabaseConnectorCloudant dbA;

    @Mock
    private DatabaseConnectorCloudant dbB;

    @Before
    public void resetCaches() throws Exception {
        // Clear the JVM-wide guard between tests so test order doesn't matter.
        clearStaticSet("INITIALISED_DDOCS");
        clearStaticSet("INITIALISED_INDEXES");
    }

    private static void clearStaticSet(String fieldName) throws Exception {
        Field f = DatabaseRepositoryCloudantClient.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        ((Set<?>) f.get(null)).clear();
    }

    private <T> DatabaseRepositoryCloudantClient<T> repoFor(
            DatabaseConnectorCloudant connector, Class<T> type) {
        return new DatabaseRepositoryCloudantClient<>(connector, type);
    }

    private Map<String, DesignDocumentViewsMapReduce> emptyViews() {
        return Collections.emptyMap();
    }

    @Test
    public void initStandardDesignDocument_isExecutedOnceForSameTypeAndDb() {
        when(dbA.getDbName()).thenReturn("sw360db");

        DatabaseRepositoryCloudantClient<Component> repo = repoFor(dbA, Component.class);

        repo.initStandardDesignDocument(emptyViews(), dbA);
        repo.initStandardDesignDocument(emptyViews(), dbA);
        repo.initStandardDesignDocument(emptyViews(), dbA);

        verify(dbA, times(1)).putDesignDocument(any(DesignDocument.class), eq("Component"));
    }

    @Test
    public void initStandardDesignDocument_runsForEachDistinctType() {
        when(dbA.getDbName()).thenReturn("sw360db");

        repoFor(dbA, Component.class).initStandardDesignDocument(emptyViews(), dbA);
        repoFor(dbA, Project.class).initStandardDesignDocument(emptyViews(), dbA);

        verify(dbA, times(1)).putDesignDocument(any(DesignDocument.class), eq("Component"));
        verify(dbA, times(1)).putDesignDocument(any(DesignDocument.class), eq("Project"));
    }

    @Test
    public void initStandardDesignDocument_runsOncePerDatabaseForSameType() {
        when(dbA.getDbName()).thenReturn("sw360db");
        when(dbB.getDbName()).thenReturn("sw360attachments");

        DatabaseRepositoryCloudantClient<Component> repoA = repoFor(dbA, Component.class);
        DatabaseRepositoryCloudantClient<Component> repoB = repoFor(dbB, Component.class);

        repoA.initStandardDesignDocument(emptyViews(), dbA);
        repoA.initStandardDesignDocument(emptyViews(), dbA);
        repoB.initStandardDesignDocument(emptyViews(), dbB);
        repoB.initStandardDesignDocument(emptyViews(), dbB);

        verify(dbA, times(1)).putDesignDocument(any(DesignDocument.class), eq("Component"));
        verify(dbB, times(1)).putDesignDocument(any(DesignDocument.class), eq("Component"));
    }

    @Test
    public void createIndex_isExecutedOnceForSameTriple() {
        when(dbA.getDbName()).thenReturn("sw360db");

        DatabaseRepositoryCloudantClient<Component> repo = repoFor(dbA, Component.class);

        repo.createIndex("compIdx", "byName", new String[]{"name"}, dbA);
        repo.createIndex("compIdx", "byName", new String[]{"name"}, dbA);

        verify(dbA, times(1)).createIndex(
                any(IndexDefinition.class), eq("compIdx"), eq("byName"), eq("json"));
    }

    @Test
    public void createIndex_runsForDistinctIndexNamesOnSameDdoc() {
        when(dbA.getDbName()).thenReturn("sw360db");

        DatabaseRepositoryCloudantClient<Component> repo = repoFor(dbA, Component.class);

        repo.createIndex("compIdx", "byName", new String[]{"name"}, dbA);
        repo.createIndex("compIdx", "byVendor", new String[]{"vendor"}, dbA);

        verify(dbA, times(1)).createIndex(
                any(IndexDefinition.class), eq("compIdx"), eq("byName"), eq("json"));
        verify(dbA, times(1)).createIndex(
                any(IndexDefinition.class), eq("compIdx"), eq("byVendor"), eq("json"));
    }

    @Test
    public void createPartialTypeIndex_isExecutedOnceForSameTriple() {
        when(dbA.getDbName()).thenReturn("sw360db");

        DatabaseRepositoryCloudantClient<Component> repo = repoFor(dbA, Component.class);

        repo.createPartialTypeIndex("compIdx", "byNameTyped", "component",
                new String[]{"name"}, dbA);
        repo.createPartialTypeIndex("compIdx", "byNameTyped", "component",
                new String[]{"name"}, dbA);

        verify(dbA, times(1)).createIndex(
                any(IndexDefinition.class), eq("compIdx"), eq("byNameTyped"), eq("json"));
    }

    @Test
    public void designDocAndIndexCachesAreIndependent() {
        when(dbA.getDbName()).thenReturn("sw360db");

        DatabaseRepositoryCloudantClient<Component> repo = repoFor(dbA, Component.class);

        // Same ddocId reused for both APIs; design-doc init should not block index init.
        repo.initStandardDesignDocument(emptyViews(), dbA);
        repo.createIndex("Component", "byName", new String[]{"name"}, dbA);

        verify(dbA, times(1)).putDesignDocument(any(DesignDocument.class), eq("Component"));
        verify(dbA, times(1)).createIndex(
                any(IndexDefinition.class), eq("Component"), eq("byName"), eq("json"));
        verify(dbA, never()).createIndex(any(IndexDefinition.class), eq("Component"),
                eq("nonexistent"), anyString());
    }
}

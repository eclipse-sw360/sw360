/*
SPDX-FileCopyrightText: © 2022 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.vmcomponents.process;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMComponent;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.thrift.TBase;
import org.eclipse.sw360.datahandler.db.ComponentDatabaseHandler;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftUtils;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.vmcomponents.common.SVMConstants;
import org.eclipse.sw360.vmcomponents.db.VMDatabaseHandler;
import org.eclipse.sw360.vmcomponents.handler.SVMSyncHandler;

import javax.annotation.Nonnull;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.log4j.Logger.getLogger;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotNull;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertTrue;

/**
 * Created by stefan.jaeger on 10.03.16.
 *
 * Process coordinator for SVM related background tasks.
 */
public class VMProcessHandler {
    private static final Logger log = getLogger(VMProcessHandler.class);

    /**
     * JVM-singleton {@link VMDatabaseHandler}. Repositories underneath are
     * stateless wrappers around the JVM-singleton {@code Cloudant} client
     * (see {@code DatabaseSettings#CLIENT}); the IBM Cloudant SDK uses OkHttp
     * which performs its own thread-safe connection pooling, so all SVM worker
     * threads can safely share a single handler instance. Lazy-initialised so
     * that a misconfigured CouchDB at boot doesn't blow up the entire backend.
     */
    private static volatile VMDatabaseHandler SHARED_DB;

    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            SVMConstants.PROCESSING_CORE_POOL_SIZE,
            SVMConstants.PROCESSING_MAX_POOL_SIZE,
            SVMConstants.PROCESSING_KEEP_ALIVE_SECONDS,
            TimeUnit.SECONDS,
            new PriorityBlockingQueue<>(),
            namedThreadFactory());

    /**
     * Names worker threads {@code sw360-vmprocessor-N} so they are identifiable
     * in {@code jstack}, IDE debug output, {@code top -H}, and Log4j2 {@code %t}
     * patterns.
     */
    private static ThreadFactory namedThreadFactory() {
        return new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(1);
            private final ThreadFactory defaults = Executors.defaultThreadFactory();

            @Override
            public Thread newThread(Runnable r) {
                Thread t = defaults.newThread(r);
                t.setName("sw360-vmprocessor-" + counter.getAndIncrement());
                return t;
            }
        };
    }

    public static VMDatabaseHandler sharedDb() throws MalformedURLException {
        VMDatabaseHandler local = SHARED_DB;
        if (local == null) {
            synchronized (VMProcessHandler.class) {
                local = SHARED_DB;
                if (local == null) {
                    local = new VMDatabaseHandler();
                    SHARED_DB = local;
                }
            }
        }
        return local;
    }

    /**
     * it is very important to use ConcurrentHashMap to be threadsafe
     */
    private static final ConcurrentHashMap<Class<?>, List<SVMSyncHandler>> syncHandlersFree = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, SVMSyncHandler> syncHandlersBusy = new ConcurrentHashMap<>(SVMConstants.PROCESSING_CORE_POOL_SIZE);
    private static final Map<String, Vendor> vendorCache = new HashMap<>();

    private VMProcessHandler(){}

    public static <T extends TBase> void getVulnerabilitiesByComponentId(String componentId, String url,
            boolean triggerVulMasterData, boolean isDeltaSync){
        try {
            queueing(VMComponent.class, componentId, VMProcessType.VULNERABILITIES, url, triggerVulMasterData, isDeltaSync);
        } catch (SW360Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public static <T extends TBase> void getVulnerabilitiesByComponentIds(Collection<String> componentIds, String url,
            boolean triggerVulMasterData, boolean isDeltaSync){
        if (!CommonUtils.isNullOrEmptyCollection(componentIds)) {
            for (String componentId : componentIds) {
                try {
                    queueing(VMComponent.class, componentId, VMProcessType.VULNERABILITIES, url, triggerVulMasterData, isDeltaSync);
                } catch (SW360Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    public static void findComponentMatch(String componentId, boolean triggerGettingVulnerabilities, boolean isDeltaSync){
        if (!StringUtils.isEmpty(componentId)){
            try {
                queueing(VMComponent.class, componentId, VMProcessType.MATCH_SVM, null, triggerGettingVulnerabilities, isDeltaSync);
            } catch (SW360Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Manual reverse-match trigger (not part of an SVM sync chain). isDeltaSync is fixed to false
     * because the request originates from user/admin action, not from a delta or full sync run.
     */
    public static void findReleaseMatch(String releaseId, boolean triggerGettingVulnerabilities){
        if (!StringUtils.isEmpty(releaseId)){
            try {
                queueing(Release.class, releaseId, VMProcessType.MATCH_SW360, null, triggerGettingVulnerabilities, false);
            } catch (SW360Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public static <T extends TBase> void getMasterData(Class<T> elementType, Collection<String> elementIds, String url,
            boolean triggerMatchCpe, boolean isDeltaSync) {
        if (!CommonUtils.isNullOrEmptyCollection(elementIds)) {
            for (String elementId : elementIds) {
                getMasterData(elementType, elementId, url, triggerMatchCpe, isDeltaSync);
            }
        }
    }

    public static <T extends TBase> void getMasterData(Class<T> elementType, String elementId, String url,
            boolean triggerMatchCpe, boolean isDeltaSync){
        if (!StringUtils.isEmpty(elementId)){
            try {
                queueing(elementType, elementId, VMProcessType.MASTER_DATA, url, triggerMatchCpe, isDeltaSync);
            } catch (SW360Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public static <T extends TBase> void storeElements(Class<T> elementType, Collection<String> elementIds, String url,
            boolean triggerMasterData, boolean isDeltaSync) {
        if (!CommonUtils.isNullOrEmptyCollection(elementIds)) {
            for (String elementId : elementIds) {
                try {
                    queueing(elementType, elementId, VMProcessType.STORE_NEW, url, triggerMasterData, isDeltaSync);
                } catch (SW360Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * CLEAN_UP is only ever enqueued from the full-sync branch of GET_IDS in {@link VMProcessor}.
     * The {@code isDeltaSync} flag is therefore intrinsically {@code false} for this task; it is
     * still kept as a constructor field on the processor for defensive checks downstream.
     */
    public static <T extends TBase> void cleanupMissingElements(Class<T> elementType, List<String> elementIds){
        try {
            queueing(elementType, elementIds, VMProcessType.CLEAN_UP, null, false, false);
        } catch (SW360Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Enqueue a full-sync GET_IDS task. Entry point of the sync chain; isDeltaSync is intrinsically
     * {@code false} because this method <em>defines</em> the chain as a full sync.
     */
    public static <T extends TBase> void getElementIds(Class<T> elementType, String url, boolean triggerStoring){
        try {
            log.info("Full sync queued for " + elementType.getSimpleName());
            queueing(elementType, "", VMProcessType.GET_IDS, url, triggerStoring, false);
        } catch (SW360Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Enqueue a delta GET_IDS task with a modified_after parameter. Entry point of the sync chain;
     * isDeltaSync is intrinsically {@code true} when {@code modifiedAfter} is non-empty because
     * this method <em>defines</em> the chain as a delta sync.
     *
     * @param elementType the type of element to fetch (must not be null)
     * @param url the base URL for the SVM API endpoint (must not be null)
     * @param modifiedAfter timestamp for delta sync; if null or empty, falls back to full sync
     * @param triggerStoring whether to trigger element storage if successful
     * @param <T> the element type
     */
    public static <T extends TBase> void getElementIdsWithModifiedAfter(
            @Nonnull Class<T> elementType,
            @Nonnull String url,
            String modifiedAfter,
            boolean triggerStoring
    ) {
        try {
            if (CommonUtils.isNullEmptyOrWhitespace(modifiedAfter)) {
                // Defensive: caller asked for delta without a timestamp; degrade to full sync.
                log.debug("getElementIdsWithModifiedAfter called without timestamp; queuing full sync for "
                        + elementType.getSimpleName());
                queueing(elementType, "", VMProcessType.GET_IDS, url, triggerStoring, false);
                return;
            }
            String separator = url.contains("?") ? "&" : "?";
            String urlWithParam = url + separator + "modified_after=" + modifiedAfter;
            log.info("Delta sync queued for " + elementType.getSimpleName()
                    + " with modified_after=" + modifiedAfter);
            queueing(elementType, "", VMProcessType.GET_IDS, urlWithParam, triggerStoring, true);
        } catch (SW360Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * FINISH task closes out reporting for a sync run. The isDeltaSync flag is not used by the
     * FINISH branch in {@link VMProcessor} (it neither cleans up nor branches on sync mode), so
     * the value passed here is intentionally informational only and fixed to {@code false}.
     */
    public static <T extends TBase> void triggerReport(Class<T> elementType, String startTime){
        try {
            queueing(elementType, startTime, VMProcessType.FINISH, null, false, false);
        } catch (SW360Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private static synchronized <T extends TBase> void queueing(Class<T> elementType, String input, VMProcessType task, String url, boolean triggerNextStep, boolean isDeltaSync) throws SW360Exception {
        queueing(elementType, input == null ? Collections.emptyList() : Collections.singletonList(input), task, url, triggerNextStep, isDeltaSync);
    }

    private static synchronized <T extends TBase> void queueing(Class<T> elementType, List<String> input, VMProcessType task, String url, boolean triggerNextStep, boolean isDeltaSync) throws SW360Exception {
        assertNotNull(elementType);
        assertNotNull(input);
        assertTrue(!input.isEmpty());
        assertNotNull(task);

        switch(task){
            case GET_IDS:
            case CLEAN_UP:
            case STORE_NEW:
            case MASTER_DATA:
            case MATCH_SVM:
            case MATCH_SW360:
            case VULNERABILITIES:
            case FINISH:
                VMProcessor<T> processor = new VMProcessor<>(elementType, input, task, url, triggerNextStep, isDeltaSync);
                executor.execute(processor);
                break;

            default: throw new IllegalArgumentException("unknown task '"+task+"'. do not know what to do :( ");
        }
    }


    public static <T extends TBase> void giveSyncHandlerBack(String syncHandlerId) throws MalformedURLException, SW360Exception {
        handleSyncHandler(null, syncHandlerId, ProcessTask.FINISH);
    }
    public static <T extends TBase> void destroySyncHandler(String syncHandlerId) throws MalformedURLException, SW360Exception {
        handleSyncHandler(null, syncHandlerId, ProcessTask.ERROR);
    }

    public static <T extends TBase> SVMSyncHandler getSyncHandler(Class<T> elementType) throws MalformedURLException, SW360Exception {
        return handleSyncHandler(elementType, null, ProcessTask.START);
    }

    public static void cacheVendors(ComponentDatabaseHandler componentHandler) {
        List<Vendor> allVendors = componentHandler.getAllVendors();
        synchronized (vendorCache) {
            vendorCache.clear();
            vendorCache.putAll(ThriftUtils.getIdMap(allVendors));
            log.info(String.format("Cached %d vendors", vendorCache.size()));
        }
    }

    public static Map<String, Vendor> getVendorCache() {
        return vendorCache;
    }

    private enum ProcessTask{
        START,
        FINISH,
        ERROR
    }

    /**
     * this important method have to be threadsafe(synchronized) to handle the different handlers is a safe way only this method is allowed to exit the ConcurrentHashMaps
     * @param elementType the specific type
     * @param syncHandlerId uuid of the handler
     * @param task trigger for making a {@link SVMSyncHandler} available or getting a used one back
     * @param <T> specification of the element type
     * @return on ProcessTask.START the available {@link SVMSyncHandler} will be returned, otherwise NULL
     * @throws SW360Exception
     * @throws MalformedURLException
     */
    private static synchronized <T extends TBase> SVMSyncHandler handleSyncHandler(Class<T> elementType, String syncHandlerId, ProcessTask task) throws SW360Exception, MalformedURLException {
        assertNotNull(task);
        switch (task){
            case START:
                assertNotNull(elementType);
                SVMSyncHandler<T> syncHandler = null;
                if (!syncHandlersFree.isEmpty()){
                    List<SVMSyncHandler> syncHandlers = syncHandlersFree.get(elementType);
                    if (!CommonUtils.isNullOrEmptyCollection(syncHandlers)) {
                        syncHandler = syncHandlers.removeFirst();
                    }
                }
                if (syncHandler == null){
                    syncHandler = new SVMSyncHandler<T>(elementType);
                }
                syncHandlersBusy.put(syncHandler.getUuid(), syncHandler);
                return syncHandler;

            case FINISH:
                assertNotNull(syncHandlerId);
                syncHandler = syncHandlersBusy.remove(syncHandlerId);
                if (syncHandler != null){
                    List<SVMSyncHandler> syncHandlers = syncHandlersFree.computeIfAbsent(syncHandler.getType(), k -> new ArrayList<>());
                    syncHandlers.add(syncHandler);
                }
                return null;

            case ERROR:
                assertNotNull(syncHandlerId);
                syncHandlersBusy.remove(syncHandlerId);
                return null;

            default: throw new IllegalArgumentException("unknown task '"+task+"'. do not know what to do :( ");
        }
    }

    public static void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
            log.info("VMProcessHandler executor shut down successfully.");
        }
    }
}

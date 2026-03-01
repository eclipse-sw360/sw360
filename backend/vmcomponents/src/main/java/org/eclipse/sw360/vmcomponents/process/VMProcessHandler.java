/*
SPDX-FileCopyrightText: © 2022 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.vmcomponents.process;

import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMComponent;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.thrift.TBase;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.db.ComponentDatabaseHandler;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftUtils;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.vmcomponents.common.SVMConstants;
import org.eclipse.sw360.vmcomponents.handler.SVMSyncHandler;

import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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

    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            SVMConstants.PROCESSING_CORE_POOL_SIZE,
            SVMConstants.PROCESSING_MAX_POOL_SIZE,
            SVMConstants.PROCESSING_KEEP_ALIVE_SECONDS,
            TimeUnit.SECONDS,
            new PriorityBlockingQueue<>());

    /**
     * it is very important to use ConcurrentHashMap to be threadsafe
     */
    private static final ConcurrentHashMap<Class<?>, List<SVMSyncHandler>> syncHandlersFree = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, SVMSyncHandler> syncHandlersBusy = new ConcurrentHashMap<>(SVMConstants.PROCESSING_CORE_POOL_SIZE);
    private static final Map<String, Vendor> vendorCache = new HashMap<>();

    private VMProcessHandler(){}

    public static <T extends TBase> void getVulnerabilitiesByComponentId(String componentId, String url, boolean triggerVulMasterData){
        try {
            queueing(VMComponent.class, componentId, VMProcessType.VULNERABILITIES, url, triggerVulMasterData);
        } catch (SW360Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public static <T extends TBase> void getVulnerabilitiesByComponentIds(Collection<String> componentIds, String url, boolean triggerVulMasterData){
        if (componentIds != null && !componentIds.isEmpty()){
            for (String componentId : componentIds) {
                try {
                    queueing(VMComponent.class, componentId, VMProcessType.VULNERABILITIES, url, triggerVulMasterData);
                } catch (SW360Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    public static void findComponentMatch(String componentId, boolean triggerGettingVulnerabilities){
        if (!StringUtils.isEmpty(componentId)){
            try {
                queueing(VMComponent.class, componentId, VMProcessType.MATCH_SVM, null, triggerGettingVulnerabilities);
            } catch (SW360Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public static void findReleaseMatch(String releaseId, boolean triggerGettingVulnerabilities){
        if (!StringUtils.isEmpty(releaseId)){
            try {
                queueing(Release.class, releaseId, VMProcessType.MATCH_SW360, null, triggerGettingVulnerabilities);
            } catch (SW360Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public static <T extends TBase> void getMasterData(Class<T> elementType, Collection<String> elementIds, String url, boolean triggerMatchCpe){
        if (elementIds != null && !elementIds.isEmpty()){
            for (String elementId : elementIds) {
                getMasterData(elementType, elementId, url, triggerMatchCpe);
            }
        }
    }

    public static <T extends TBase> void getMasterData(Class<T> elementType, String elementId, String url, boolean triggerMatchCpe){
        if (!StringUtils.isEmpty(elementId)){
            try {
                queueing(elementType, elementId, VMProcessType.MASTER_DATA, url, triggerMatchCpe);
            } catch (SW360Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public static <T extends TBase> void storeElements(Class<T> elementType, Collection<String> elementIds, String url, boolean triggerMasterData){
        if (elementIds != null && elementIds.size()>0){
            for (String elementId : elementIds) {
                try {
                    queueing(elementType, elementId, VMProcessType.STORE_NEW, url, triggerMasterData);
                } catch (SW360Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    public static <T extends TBase> void cleanupMissingElements(Class<T> elementType, List<String> elementIds){
        try {
            queueing(elementType, elementIds, VMProcessType.CLEAN_UP, null, false);
        } catch (SW360Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public static <T extends TBase> void getElementIds(Class<T> elementType, String url, boolean triggerStoring){
        try {
            queueing(elementType, "", VMProcessType.GET_IDS, url, triggerStoring);
        } catch (SW360Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Enqueue a GET_IDS task optionally enriched with modified_after parameter for incremental sync.
     */
    public static <T extends TBase> void getElementIdsWithModifiedAfter(Class<T> elementType, String url, String modifiedAfter, boolean triggerStoring){
        try {
            String urlWithParam = url;
            boolean delta = false;
            if (modifiedAfter != null && !modifiedAfter.isEmpty()) {
                String separator = url.contains("?") ? "&" : "?";
                urlWithParam = url + separator + "modified_after=" + modifiedAfter;
                delta = true;
            }
            if (delta) {
                log.info("Delta sync queued for " + elementType.getSimpleName() + " with modified_after=" + modifiedAfter);
            } else {
                log.debug("Full sync queued for " + elementType.getSimpleName());
            }
            queueing(elementType, "", VMProcessType.GET_IDS, urlWithParam, triggerStoring);
        } catch (SW360Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public static <T extends TBase> void triggerReport(Class<T> elementType, String startTime){
        try {
            queueing(elementType, startTime, VMProcessType.FINISH, null, false);
        } catch (SW360Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private static synchronized <T extends TBase> void queueing(Class<T> elementType, String input, VMProcessType task, String url, boolean triggerNextStep) throws SW360Exception {
        queueing(elementType, input == null ? Collections.emptyList() : Collections.singletonList(input), task, url, triggerNextStep);
    }

    private static synchronized <T extends TBase> void queueing(Class<T> elementType, List<String> input, VMProcessType task, String url, boolean triggerNextStep) throws SW360Exception {
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
                VMProcessor<T> processor = new VMProcessor<>(elementType, input, task, url, triggerNextStep);
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
                    if(syncHandlers != null && syncHandlers.size() > 0){
                        syncHandler = syncHandlers.remove(0);
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
                    List<SVMSyncHandler> syncHandlers = syncHandlersFree.get(syncHandler.getType());
                    if (syncHandlers == null){
                        syncHandlers = new ArrayList<>();
                        syncHandlersFree.put(syncHandler.getType(), syncHandlers);
                    }
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
}

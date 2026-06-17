/*
SPDX-FileCopyrightText: © 2022 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.vmcomponents.process;

import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMAction;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMComponent;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMMatch;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMPriority;

import org.apache.log4j.Logger;
import org.apache.thrift.TBase;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.Vulnerability;
import org.eclipse.sw360.vmcomponents.common.SVMConstants;
import org.eclipse.sw360.vmcomponents.common.SVMUtils;
import org.eclipse.sw360.vmcomponents.common.VMResult;
import org.eclipse.sw360.vmcomponents.handler.SVMSyncHandler;

import java.util.Date;
import java.util.List;

import static org.apache.log4j.Logger.getLogger;

/**
 * Created by stefan.jaeger on 10.03.16.
 *
 * @author stefan.jaeger@evosoft.com
 */
public class VMProcessor<T extends TBase> implements Runnable, Comparable<VMProcessor<T>>{
    private static final Logger log = getLogger(VMProcessor.class);

    private Class<T> elementType;
    private List<String> input;
    private VMProcessType task;
    private String url;
    private boolean triggerNextStep;
    private int priority;
    private boolean isDeltaSync;

    public VMProcessor(Class<T> elementType, List<String> input, VMProcessType task, String url, boolean triggerNextStep, boolean isDeltaSync) {
        this.elementType = elementType;
        this.input = input;
        this.task = task;
        this.url = url;
        this.triggerNextStep = triggerNextStep;
        this.isDeltaSync = isDeltaSync;
        this.priority = mapPrio(elementType, task);
        log.debug("Job initialized:      "+this.toString());
    }

    private int mapPrio(Class<T> elementType, VMProcessType task){
        int reporting = 0;
        if (VMProcessType.FINISH.equals(task)){
            reporting = 90;
        }
        if (VMComponent.class.isAssignableFrom(elementType))
            return 30 + reporting;
        else if (VMAction.class.isAssignableFrom(elementType))
            return 10 + reporting;
        else if (VMPriority.class.isAssignableFrom(elementType))
            return 20 + reporting;
        else if (Vulnerability.class.isAssignableFrom(elementType))
            return 40 + reporting;
        else if (Release.class.isAssignableFrom(elementType))
            return 50 + reporting;
        else
            throw new IllegalArgumentException("unknown type "+ elementType.getSimpleName());
    }

    @Override
    public void run() {
        log.debug("Job started          "+this.toString());
        long start = new Date().getTime();

        SVMSyncHandler syncHandler = null;
        try {
            if (task == null){
                throw new IllegalArgumentException("task not defined: "+this.toString());
            }

            syncHandler = VMProcessHandler.getSyncHandler(this.elementType);
            log.debug("Job do               "+this.toString()+" with syncHandler "+syncHandler.getUuid());

            switch (task){

                case GET_IDS:
                    // get element ids; this is a full sync if isDeltaSync=false, or a delta sync otherwise
                    VMResult idsResult = syncHandler.getSMVElementIds(this.url);
                    if (triggerNextStep
                            && idsResult != null
                            && RequestStatus.SUCCESS.equals(idsResult.requestSummary.requestStatus)
                            && idsResult.elements != null
                            && !idsResult.elements.isEmpty()) {
                        // Cleanup only runs for full syncs. For delta syncs, items deleted on SVM are not
                        // observable (they simply don't appear in the modified_after result), so cleanup
                        // would incorrectly delete unchanged local data.
                        // The caller (VMComponentHandler) ensures a full sync is forced periodically
                        // (see CLEANUP_FREQUENCY_DAYS) so deleted items eventually get purged.
                        if (!this.isDeltaSync) {
                            VMProcessHandler.cleanupMissingElements(this.elementType, idsResult.elements);
                        } else {
                            log.debug("Skipping cleanup for delta sync of " + elementType.getSimpleName());
                        }
                        VMProcessHandler.storeElements(this.elementType, idsResult.elements, this.url, triggerNextStep, this.isDeltaSync);
                    }
                    break;

                case CLEAN_UP:
                    // Defensive: CLEAN_UP is only enqueued from a full-sync GET_IDS branch, so reaching
                    // this point already implies a full sync. The flag check is kept as a safeguard.
                    if (this.isDeltaSync) {
                        log.info("Skipping cleanup for delta sync run of " + elementType.getSimpleName());
                        break;
                    }
                    // delete VM elements which are not in the new vmid list anymore
                    VMResult deletionResult = syncHandler.deleteMissingElements(this.input);
                    // delete corresponding matches out of db
                    if (deletionResult != null
                            && RequestStatus.SUCCESS.equals(deletionResult.requestSummary.requestStatus)
                            && deletionResult.elements != null
                            && !deletionResult.elements.isEmpty()){
                        syncHandler.cleanUpMatches(deletionResult.elements);
                    }
                    break;

                case STORE_NEW:
                    // save element into DB
                    VMResult storeResult = syncHandler.storeNewElement(this.input.getFirst());
                    if (triggerNextStep
                            && storeResult != null
                            && RequestStatus.SUCCESS.equals(storeResult.requestSummary.requestStatus)
                            && storeResult.elements != null
                            && !storeResult.elements.isEmpty()){
                        // sanitize base URL (strip query params like modified_after) for master data calls
                        String baseUrl = this.url;
                        if (baseUrl != null && baseUrl.contains("?")) {
                            baseUrl = baseUrl.substring(0, baseUrl.indexOf('?'));
                        }
                        VMProcessHandler.getMasterData(this.elementType, SVMUtils.getId(elementType.cast(storeResult.elements.getFirst())), baseUrl, triggerNextStep, this.isDeltaSync);
                    }
                    break;

                case MASTER_DATA:
                    // get master data from SVM
                    VMResult getResult = syncHandler.getSMVElementMasterDataById(this.input.getFirst(), this.url);
                    // update local element in db
                    if (getResult != null
                            && RequestStatus.SUCCESS.equals(getResult.requestSummary.requestStatus)
                            && getResult.elements != null
                            && !getResult.elements.isEmpty()){
                        syncHandler.syncDatabase(elementType.cast(getResult.elements.getFirst()));

                        // trigger Match CPE for VMComponent elements
                        if(triggerNextStep
                                && getResult.requestSummary.totalAffectedElements > 0
                                && VMComponent.class.isAssignableFrom(this.elementType)){
                            VMProcessHandler.findComponentMatch(this.input.getFirst(), triggerNextStep, this.isDeltaSync);
                        }
                    }
                    break;

                case MATCH_SVM:
                    // try to find a match via cpe and text
                    VMResult<VMMatch> componentMatchResult = syncHandler.findMatchByComponent(this.input.getFirst());
                    if (triggerNextStep
                            && componentMatchResult != null
                            && RequestStatus.SUCCESS.equals(componentMatchResult.requestSummary.requestStatus)
                            && componentMatchResult.elements != null
                            && !componentMatchResult.elements.isEmpty()
                            && componentMatchResult.requestSummary.totalAffectedElements > 0){
                        // re-queue for get Vulnerabilities
                        VMProcessHandler.getVulnerabilitiesByComponentId(this.input.getFirst(), SVMConstants.VULNERABILITIES_PER_COMPONENT_URL, triggerNextStep, this.isDeltaSync);
                    }
                    break;

                case MATCH_SW360:
                    // try to find a match via cpe and text
                    VMResult<String> releaseMatchResult = syncHandler.findMatchByRelease(this.input.getFirst());
                    if (triggerNextStep
                            && releaseMatchResult != null
                            && RequestStatus.SUCCESS.equals(releaseMatchResult.requestSummary.requestStatus)
                            && releaseMatchResult.elements != null
                            && !releaseMatchResult.elements.isEmpty()
                            && releaseMatchResult.requestSummary.totalAffectedElements > 0){
                        // re-queue for get Vulnerabilities
                        VMProcessHandler.getVulnerabilitiesByComponentIds(releaseMatchResult.elements, SVMConstants.VULNERABILITIES_PER_COMPONENT_URL, triggerNextStep, this.isDeltaSync);
                    }
                    break;

                case VULNERABILITIES:
                    VMResult vulResult = syncHandler.getVulnerabilitiesByComponentId(this.input.getFirst(), this.url);
                    if (triggerNextStep
                            && vulResult != null
                            && RequestStatus.SUCCESS.equals(vulResult.requestSummary.requestStatus)
                            && vulResult.elements != null
                            && !vulResult.elements.isEmpty()
                            && vulResult.requestSummary.totalAffectedElements > 0){
                        // get master data of the vulnerabilities
                        VMProcessHandler.getMasterData(Vulnerability.class, vulResult.elements, SVMConstants.VULNERABILITIES_URL, true, this.isDeltaSync);
                    }
                    break;

                case FINISH:
                    // finishing reporting
                    syncHandler.finishReport(this.input.getFirst());
                    break;

                default: throw new IllegalArgumentException("unknown task '"+task+"'. do not know what to do :( "+this.toString());
            }

            if (syncHandler != null){
                VMProcessHandler.giveSyncHandlerBack(syncHandler.getUuid());
            }
        } catch (Throwable e) {
            log.error(e.getMessage(), e);

            if (syncHandler != null){
                try {
                    VMProcessHandler.destroySyncHandler(syncHandler.getUuid());
                } catch (Throwable e1) {
                    log.error(e1.getMessage(), e1);
                }
            }
        }
        long runtime = new Date().getTime() - start;
        log.debug("Job finished ("+runtime+"ms) "+this.toString());
    }

    @Override
    public String toString() {
        return "VMProcessor{" +
                "elementType=" + elementType +
                ", input=" + input +
                ", task=" + task +
                ", url='" + url + '\'' +
                ", triggerNextStep=" + triggerNextStep +
                ", priority=" + priority +
                '}';
    }

    @Override
    public int compareTo(VMProcessor<T> tvmProcessor) {
        return this.priority - tvmProcessor.priority;
    }
}

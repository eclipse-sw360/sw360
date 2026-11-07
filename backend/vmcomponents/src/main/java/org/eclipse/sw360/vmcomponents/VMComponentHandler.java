/*
SPDX-FileCopyrightText: © 2022 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.vmcomponents;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.thrift.TBase;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.db.ComponentDatabaseHandler;
import org.eclipse.sw360.datahandler.db.VilocifyConnector;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMAction;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMComponent;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMComponentService;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMMatch;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMMatchState;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMMatchType;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMPriority;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMProcessReporting;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMProcessSyncType;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.ReleaseVulnerabilityRelation;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.Vulnerability;
import org.eclipse.sw360.vmcomponents.common.SVMConstants;
import org.eclipse.sw360.vmcomponents.common.SVMMapper;
import org.eclipse.sw360.vmcomponents.common.SVMUtils;
import org.eclipse.sw360.vmcomponents.db.VMDatabaseHandler;
import org.eclipse.sw360.vmcomponents.process.VMProcessHandler;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.log4j.Logger.getLogger;

/**
 * Implementation of the Thrift service
 *
 * @author stefan.jaeger@evosoft.com
 * @author alex.borodin@evosoft.com
 */
public class VMComponentHandler implements VMComponentService.Iface {

    private static final Logger log = getLogger(VMComponentHandler.class);
    private static final AtomicBoolean vilocifySyncRunning =
            new AtomicBoolean(false);
    private static final AtomicBoolean vilocifyMatchRunning =
            new AtomicBoolean(false);

    private final VMDatabaseHandler dbHandler;
    private final ComponentDatabaseHandler compHandler;


    public VMComponentHandler() throws IOException {
        dbHandler = new VMDatabaseHandler();
        compHandler = new ComponentDatabaseHandler(DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_DATABASE, DatabaseSettings.COUCH_DB_ATTACHMENTS);
    }

    @Override
    public List<VMProcessReporting> getAllProcesses(User user) throws TException {
        if (PermissionUtils.isAdmin(user)){
            return dbHandler.getAll(VMProcessReporting.class);
        }
        return Collections.emptyList();
    }

    @Override
    public List<VMMatch> getAllMatches(User user) throws TException {
        if (!PermissionUtils.isAdmin(user)){
            return Collections.emptyList();
        }
        return dbHandler.getAll(VMMatch.class);
    }

    @Override
    public RequestSummary synchronizeComponents() throws TException {
        if (VilocifyConnector.isVilocifyProviderActive()) {
            return synchronizeVilocifyComponentsAndVulnerabilities();
        }
        VMProcessHandler.cacheVendors(compHandler);

        // synchronize VMAction
        String actionStart = synchronizeElementType(VMAction.class, SVMConstants.ACTIONS_URL);
        log.info("Storing and getting master data of "+VMAction.class.getSimpleName()+" triggered. waiting for completion...");

        // synchronize VMPriority
        String prioStart = synchronizeElementType(VMPriority.class, SVMConstants.PRIORITIES_URL);
        log.info("Storing and getting master data of "+VMPriority.class.getSimpleName()+" triggered. waiting for completion...");

        // synchronize VMComponent
        String compStart = synchronizeElementType(VMComponent.class, SVMConstants.COMPONENTS_URL);
        log.info("Storing and getting master data of "+VMComponent.class.getSimpleName()+" triggered. waiting for completion...");

        // synchronize Vulnerability (bulk notifications)
        String vulnStart = synchronizeElementType(Vulnerability.class, SVMConstants.VULNERABILITIES_URL);
        log.info("Storing and getting master data of "+Vulnerability.class.getSimpleName()+" triggered. waiting for completion...");

        // triggerReporting
        VMProcessHandler.triggerReport(VMAction.class, actionStart);
        VMProcessHandler.triggerReport(VMPriority.class, prioStart);
        VMProcessHandler.triggerReport(VMComponent.class, compStart);
        VMProcessHandler.triggerReport(Vulnerability.class, vulnStart);

        return new RequestSummary(RequestStatus.SUCCESS);
    }

    /**
     * Synchronizes Vilocify components and vulnerability notifications using the same
     * delta (past {@code n} days) vs weekly full sync cadence as SVM, backed by
     * {@link VMProcessReporting}.
     *
     * <p>Executes asynchronously in a background daemon thread and returns immediately
     * (matching SVM's {@link #synchronizeComponents()} contract) so Thrift HTTP calls
     * never time out on large notification sets.
     */
    private RequestSummary synchronizeVilocifyComponentsAndVulnerabilities() {
        if (!vilocifySyncRunning.compareAndSet(false, true)) {
            log.info("Vilocify Sync: synchronization is already running in background; skipping duplicate trigger");
            return new RequestSummary(RequestStatus.SUCCESS);
        }

        Thread worker = new Thread(() -> {
            try {
                runVilocifyNotificationSyncJob();
            } catch (Exception e) {
                log.error("Vilocify Sync: unexpected error in background notification sync", e);
            } finally {
                vilocifySyncRunning.set(false);
            }
        }, "sw360-vilocify-notification-sync");
        worker.setDaemon(true);
        worker.start();

        return new RequestSummary(RequestStatus.SUCCESS);
    }

    private void runVilocifyNotificationSyncJob() {
        // 1a. Initialize Vilocify VMActions and VMPriorities if empty
        ensureVilocifyActionsAndPrioritiesInitialized();

        // 1b. Fetch and upsert all Vilocify notifications (delta or full)
        String elementType = "VilocifyNotification";
        VMProcessReporting lastProcess = dbHandler.getLastSuccessfulProcessByElementType(elementType);
        VMProcessReporting lastFullSync = dbHandler.getLastSuccessfulFullSyncByElementType(elementType);
        VMProcessSyncType syncType = determineSyncType(
                lastProcess, lastFullSync, SVMConstants.CLEANUP_FREQUENCY_DAYS, new Date());

        String modifiedAfter = null;
        if (syncType == VMProcessSyncType.DELTA && lastProcess != null && lastProcess.isSetEndDate()) {
            String rawModifiedAfter = SVMUtils.calculateModifiedAfter(
                    lastProcess.getEndDate(), SVMConstants.SVMSYNC_DELTA_OFFSET_DAYS);
            if (CommonUtils.isNotNullEmptyOrWhitespace(rawModifiedAfter)) {
                modifiedAfter = rawModifiedAfter.endsWith("Z") ? rawModifiedAfter : rawModifiedAfter + "Z";
            }
        }

        String startDate = SW360Utils.getCreatedOnTime();
        VMProcessReporting reporting = new VMProcessReporting(elementType, startDate)
                .setSyncType(syncType);
        dbHandler.add(reporting);

        log.info(String.format("Vilocify Sync [%s]: %s sync, last=%s, modified_after=%s",
                elementType,
                syncType == VMProcessSyncType.COMPLETE
                        ? "full (weekly)"
                        : "delta(" + SVMConstants.SVMSYNC_DELTA_OFFSET_DAYS + "d)",
                lastProcess != null ? lastProcess.getEndDate() : "none",
                modifiedAfter != null ? modifiedAfter : "none"));

        RequestStatus status = syncVilocifyNotificationsPageWise(
                modifiedAfter, syncType == VMProcessSyncType.COMPLETE);
        boolean syncSucceeded = RequestStatus.SUCCESS.equals(status);

        if (syncSucceeded) {
            syncSucceeded = updateExistingVilocifyVmComponents(modifiedAfter);
        }

        if (syncSucceeded) {
            reporting.setEndDate(SW360Utils.getCreatedOnTime());
            if (!RequestStatus.SUCCESS.equals(dbHandler.update(reporting))) {
                log.error("Vilocify Sync: failed to update successful process report");
            }
        } else {
            log.error("Vilocify Sync: process report " + reporting.getId()
                    + " is incomplete; leaving endDate unset");
        }

    }

    private static final Map<String, String> VILOCIFY_DEFAULT_ACTIONS = Map.of(
            "updateToFixedVersion", "Update to Fixed Version",
            "implementWorkaround", "Implement Workaround",
            "applyPatch", "Apply Patch",
            "followRecommendation", "Follow Recommendation",
            "waitForOfficialSolution", "Wait for Official Solution"
    );

    private record PriorityDef(String shortText, String longText) {
    }

    private static final Map<String, PriorityDef> VILOCIFY_DEFAULT_PRIORITIES = Map.of(
            "critical", new PriorityDef("critical",
                    "Typically used for remote attacks with critical impact which cause full compromise of the system. Investigate immediately and follow recommended action as soon as possible with urgency."),
            "major", new PriorityDef("major",
                    "Typically used for remote or local attacks with significant impact but no full compromise of system. Investigate as soon as possible and follow recommended action."),
            "minor", new PriorityDef("minor",
                    "Typically used for local attacks with minor impact to system. Follow the recommended action in a reasonable amount of time."),
            "informational", new PriorityDef("information only",
                    "Typically used for notifications that are not directly related to vulnerabilities but could have a security impact nonetheless, e.g. updates of trusted root CAs or time zone information."),
            "withdrawn", new PriorityDef("withdrawn",
                    "The vulnerability has been withdrawn either by the vendor or Siemens ProductCERT. It is no longer valid and can be disregarded.")
    );

    private void ensureVilocifyActionsAndPrioritiesInitialized() {
        List<VMAction> existingActions = dbHandler.getAll(VMAction.class);
        Set<String> knownActionVmids = new HashSet<>();
        if (existingActions != null) {
            for (VMAction a : existingActions) {
                if (a != null && CommonUtils.isNotNullEmptyOrWhitespace(a.getVmid())) {
                    knownActionVmids.add(a.getVmid());
                }
            }
        }
        for (Map.Entry<String, String> entry : VILOCIFY_DEFAULT_ACTIONS.entrySet()) {
            if (!knownActionVmids.contains(entry.getKey())) {
                VMAction action = new VMAction(entry.getKey())
                        .setText(entry.getValue())
                        .setLastUpdateDate(SW360Utils.getCreatedOnTime());
                dbHandler.add(action);
            }
        }

        List<VMPriority> existingPriorities = dbHandler.getAll(VMPriority.class);
        Set<String> knownPriorityVmids = new HashSet<>();
        if (existingPriorities != null) {
            for (VMPriority p : existingPriorities) {
                if (p != null && CommonUtils.isNotNullEmptyOrWhitespace(p.getVmid())) {
                    knownPriorityVmids.add(p.getVmid());
                }
            }
        }
        for (Map.Entry<String, PriorityDef> entry : VILOCIFY_DEFAULT_PRIORITIES.entrySet()) {
            if (!knownPriorityVmids.contains(entry.getKey())) {
                PriorityDef def = entry.getValue();
                VMPriority priority = new VMPriority(entry.getKey())
                        .setShortText(def.shortText())
                        .setLongText(def.longText())
                        .setLastUpdateDate(SW360Utils.getCreatedOnTime());
                dbHandler.add(priority);
            }
        }
    }

    private void applyPriorityAndActionMappings(
            Vulnerability vulnerability,
            Map<String, VMPriority> prioritiesByVmid,
            Map<String, VMAction> actionsByVmid) {
        if (CommonUtils.isNotNullEmptyOrWhitespace(vulnerability.getPriority())) {
            String rawPrio = vulnerability.getPriority().trim();
            VMPriority priority = prioritiesByVmid.get(rawPrio);
            if (priority != null) {
                vulnerability.setPriority(priority.getShortText());
                vulnerability.setPriorityText(priority.getLongText());
            }
        }
        if (CommonUtils.isNotNullEmptyOrWhitespace(vulnerability.getAction())) {
            String rawAction = vulnerability.getAction().trim();
            VMAction action = actionsByVmid.get(rawAction);
            if (action != null) {
                vulnerability.setAction(action.getText());
            }
        }
    }

    private boolean updateExistingVilocifyVmComponents(String modifiedAfter) {
        VilocifyConnector connector = new VilocifyConnector();
        if (!connector.isConfigured()) {
            return false;
        }
        List<VMComponent> existingComponents = dbHandler.getAll(VMComponent.class);
        if (existingComponents == null || existingComponents.isEmpty()) {
            return existingComponents != null;
        }
        Map<String, VMComponent> existingByVmid = new HashMap<>();
        for (VMComponent comp : existingComponents) {
            if (comp != null && CommonUtils.isNotNullEmptyOrWhitespace(comp.getVmid())) {
                existingByVmid.put(comp.getVmid().trim(), comp);
            }
        }
        if (existingByVmid.isEmpty()) {
            return true;
        }

        try {
            List<VMComponent> refreshed = connector.getComponentsAsVmComponentsByIds(
                    existingByVmid.keySet(), modifiedAfter);
            List<VMComponent> toBulkUpdate = new ArrayList<>();
            for (VMComponent updated : refreshed) {
                VMComponent existing = existingByVmid.get(updated.getVmid());
                if (existing != null) {
                    updated.setId(existing.getId());
                    updated.setRevision(existing.getRevision());
                    updated.setReceivedDate(existing.getReceivedDate());
                    toBulkUpdate.add(updated);
                }
            }
            if (!toBulkUpdate.isEmpty()) {
                if (!RequestStatus.SUCCESS.equals(dbHandler.add(VMComponent.class, toBulkUpdate))) {
                    return false;
                }
                log.info("Vilocify Sync: updated " + toBulkUpdate.size() + " existing VMComponents in sw360vm");
            }
            return true;
        } catch (Exception e) {
            log.warn("Vilocify Sync: failed updating existing VMComponents", e);
            return false;
        }
    }

    private boolean cleanupMissingVilocifyNotifications(
            Collection<Vulnerability> existingVulnerabilities,
            Set<String> currentNotificationIds) {
        List<ReleaseVulnerabilityRelation> relations = dbHandler.getAll(ReleaseVulnerabilityRelation.class);
        if (relations == null) {
            log.error("Vilocify Sync: could not load relations for full-sync cleanup");
            return false;
        }

        List<Vulnerability> staleNotifications = existingVulnerabilities.stream()
                .filter(vulnerability -> vulnerability != null
                        && CommonUtils.isNotNullEmptyOrWhitespace(vulnerability.getExternalId())
                        && vulnerability.isSetCveFurtherMetaDataPerSource()
                        && vulnerability.getCveFurtherMetaDataPerSource().containsKey("vilocify")
                        && !currentNotificationIds.contains(vulnerability.getExternalId().trim()))
                .toList();
        if (staleNotifications.isEmpty()) {
            return true;
        }

        Set<String> staleVulnerabilityIds = staleNotifications.stream()
                .map(Vulnerability::getId)
                .filter(CommonUtils::isNotNullEmptyOrWhitespace)
                .collect(java.util.stream.Collectors.toSet());
        boolean succeeded = true;
        for (ReleaseVulnerabilityRelation relation : relations) {
            if (relation != null && staleVulnerabilityIds.contains(relation.getVulnerabilityId())
                    && !RequestStatus.SUCCESS.equals(dbHandler.delete(relation))) {
                succeeded = false;
                log.error("Vilocify Sync: failed deleting relation for stale vulnerability "
                        + relation.getVulnerabilityId());
            }
        }
        if (!succeeded) {
            return false;
        }
        for (Vulnerability vulnerability : staleNotifications) {
            if (!RequestStatus.SUCCESS.equals(dbHandler.delete(vulnerability))) {
                succeeded = false;
                log.error("Vilocify Sync: failed deleting stale notification "
                        + vulnerability.getExternalId());
            }
        }
        return succeeded;
    }

    /**
     * Streams Vilocify notifications page-by-page (200 notifications per HTTP page)
     * and bulk-upserts each page of 200 notifications into {@code sw360vm} CouchDB
     * in a single {@code _bulk_docs} call via {@link VMDatabaseHandler}.
     */
    private RequestStatus syncVilocifyNotificationsPageWise(String modifiedAfter, boolean fullSync) {
        VilocifyConnector connector = new VilocifyConnector();
        if (!connector.isConfigured()) {
            log.info("Vilocify Sync: Vilocify connector is not configured; skipping notification sync");
            return RequestStatus.FAILURE;
        }

        Map<String, Vulnerability> existingByExternalId = new HashMap<>();
        List<Vulnerability> existingVulns = dbHandler.getAll(Vulnerability.class);
        if (existingVulns == null) {
            log.error("Vilocify Sync: could not load existing vulnerabilities from sw360vm");
            return RequestStatus.FAILURE;
        }
        for (Vulnerability v : existingVulns) {
            if (v != null && CommonUtils.isNotNullEmptyOrWhitespace(v.getExternalId())) {
                existingByExternalId.put(v.getExternalId().trim(), v);
            }
        }
        log.info("Vilocify Sync: pre-loaded " + existingByExternalId.size()
                + " existing vulnerabilities from sw360vm for bulk upsert");
        Set<String> currentNotificationIds = new HashSet<>();

        Map<String, VMPriority> prioritiesByVmid = new HashMap<>();
        List<VMPriority> priorities = dbHandler.getAll(VMPriority.class);
        if (priorities != null) {
            for (VMPriority p : priorities) {
                if (p != null && CommonUtils.isNotNullEmptyOrWhitespace(p.getVmid())) {
                    prioritiesByVmid.put(p.getVmid().trim(), p);
                }
            }
        }
        Map<String, VMAction> actionsByVmid = new HashMap<>();
        List<VMAction> actions = dbHandler.getAll(VMAction.class);
        if (actions != null) {
            for (VMAction a : actions) {
                if (a != null && CommonUtils.isNotNullEmptyOrWhitespace(a.getVmid())) {
                    actionsByVmid.put(a.getVmid().trim(), a);
                }
            }
        }

        int[] totalSaved = {0};
        boolean[] hasErrors = {false};
        try {
            connector.streamNotificationsAsSw360Vulnerabilities(modifiedAfter, pageBatch -> {
                for (Vulnerability incoming : pageBatch) {
                    applyPriorityAndActionMappings(incoming, prioritiesByVmid, actionsByVmid);
                    Vulnerability existing = existingByExternalId.get(incoming.getExternalId());
                    if (existing != null) {
                        incoming.setId(existing.getId());
                        incoming.setRevision(existing.getRevision());
                    }
                }
                RequestStatus batchStatus = dbHandler.add(Vulnerability.class, pageBatch);
                if (!RequestStatus.SUCCESS.equals(batchStatus)) {
                    hasErrors[0] = true;
                    log.error("Vilocify Sync: failed bulk-saving page of " + pageBatch.size() + " notifications");
                } else {
                    pageBatch.stream()
                            .map(Vulnerability::getExternalId)
                            .filter(CommonUtils::isNotNullEmptyOrWhitespace)
                            .map(String::trim)
                            .forEach(currentNotificationIds::add);
                    totalSaved[0] += pageBatch.size();
                    log.info("Vilocify Sync: bulk-saved page of " + pageBatch.size()
                            + " notifications (total saved: " + totalSaved[0] + ")");
                }
            });
        } catch (Exception e) {
            hasErrors[0] = true;
            log.error("Vilocify Sync: failed streaming notifications from Vilocify", e);
        }

        if (hasErrors[0]) {
            return RequestStatus.FAILURE;
        }
        if (fullSync && !cleanupMissingVilocifyNotifications(
                existingByExternalId.values(), currentNotificationIds)) {
            return RequestStatus.FAILURE;
        }
        return RequestStatus.SUCCESS;
    }

    /**
     * <p>Synchronize a single SVM element type. Decides between full sync (with
     * cleanup) and delta sync based on time elapsed since the last successful
     * complete sync.</p>
     * <p>Sync strategy:<ul>
     * <li>First run or no previous sync: full sync (no modified_after parameter).</li>
     * <li>{@code Elapsed since last complete sync >= CLEANUP_FREQUENCY_DAYS}: full sync to purge SVM-side deletions from local DB.</li>
     * <li>Otherwise: delta sync using modified_after = {@code lastEndDate - SVMSYNC_DELTA_OFFSET_DAYS}.</li>
     * </ul></p>
     */
    private <T extends TBase> String synchronizeElementType(Class<T> elementType, String url) {
        VMProcessReporting lastProcess = dbHandler.getLastSuccessfulProcessByElementType(elementType.getSimpleName());
        VMProcessReporting lastFullSync = dbHandler.getLastSuccessfulFullSyncByElementType(elementType.getSimpleName());
        VMProcessSyncType syncType = determineSyncType(
                lastProcess, lastFullSync, SVMConstants.CLEANUP_FREQUENCY_DAYS, new Date());
        String modifiedAfter = null;

        if (syncType == VMProcessSyncType.DELTA) {
            modifiedAfter = SVMUtils.calculateModifiedAfter(
                    lastProcess.getEndDate(), SVMConstants.SVMSYNC_DELTA_OFFSET_DAYS);
        }

        String startDate = SW360Utils.getCreatedOnTime();
        VMProcessReporting reporting = new VMProcessReporting(elementType.getSimpleName(), startDate)
                .setSyncType(syncType);
        dbHandler.add(reporting);

        String syncDescription = syncType == VMProcessSyncType.COMPLETE
                ? "full (cleanup)"
                : "delta(" + SVMConstants.SVMSYNC_DELTA_OFFSET_DAYS + "d)";
        log.info(String.format("SVM Sync [%s]: %s sync, last=%s, modified_after=%s",
            elementType.getSimpleName(), syncDescription,
            lastProcess != null ? lastProcess.getEndDate() : "none",
            modifiedAfter != null ? modifiedAfter : "none"));

        if (modifiedAfter != null) {
            VMProcessHandler.getElementIdsWithModifiedAfter(elementType, url, modifiedAfter, true);
        } else {
            VMProcessHandler.getElementIds(elementType, url, true);
        }
        return startDate;
    }

    /**
     * Select a full sync when there is no successful baseline, no known complete
     * sync, or the last complete sync is old enough to require cleanup.
     */
    static VMProcessSyncType determineSyncType(
            VMProcessReporting lastProcess, VMProcessReporting lastFullSync,
            int cleanupFrequencyDays, Date now
    ) {
        if (lastProcess == null || !lastProcess.isSetEndDate()
                || lastFullSync == null || !lastFullSync.isSetEndDate()) {
            return VMProcessSyncType.COMPLETE;
        }

        try {
            java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date lastDate = format.parse(lastFullSync.getEndDate());
            long diffMillis = now.getTime() - lastDate.getTime();
            long daysSinceLastFullSync = diffMillis / (1000 * 60 * 60 * 24);
            return daysSinceLastFullSync >= cleanupFrequencyDays
                    ? VMProcessSyncType.COMPLETE
                    : VMProcessSyncType.DELTA;
        } catch (ParseException e) {
            log.warn("Failed to parse last full sync end date: " + e.getMessage());
            return VMProcessSyncType.COMPLETE;
        }
    }

    @Override
    public RequestSummary triggerReverseMatch() throws TException {
        if (VilocifyConnector.isVilocifyProviderActive()) {
            if (!vilocifyMatchRunning.compareAndSet(false, true)) {
                log.info("VilocifyMatch: reverse match is already running in background; skipping duplicate trigger");
                return new RequestSummary(RequestStatus.SUCCESS);
            }
            Thread worker = new Thread(() -> {
                try {
                    runVilocifyReverseMatchAndTrackingFeedbackJob();
                } catch (Exception e) {
                    log.error("VilocifyMatch: unexpected error in background reverse match", e);
                } finally {
                    vilocifyMatchRunning.set(false);
                }
            }, "sw360-vilocify-reverse-match");
            worker.setDaemon(true);
            worker.start();
            return new RequestSummary(RequestStatus.SUCCESS);
        }
        Set<String> releaseIds = compHandler.getAllReleaseIds();
        if (releaseIds != null && !releaseIds.isEmpty()){
            for (String releaseId: releaseIds) {
                VMProcessHandler.findReleaseMatch(releaseId, true);
            }
        }
        log.info("Reverse match triggered for "+(releaseIds==null?0:releaseIds.size())+" releases. waiting for completion...");
        return new RequestSummary(RequestStatus.SUCCESS);
    }

    /**
     * Executes Job 2 ({@code svmmatchService}) for Vilocify:
     * <ol>
     *   <li>Finds all SW360 releases missing {@code com.vilocify.component.id} and attempts to
     *       match them via Vilocify API filters.</li>
     *   <li>If matched: upserts {@link VMComponent} in {@code sw360vm}, creates/updates
     *       {@link VMMatch} with {@link VMMatchState#ACCEPTED}, and stores
     *       {@code com.vilocify.component.id} in {@code release.externalIds}.</li>
     *   <li>If not matched: creates a {@code componentRequest} in Vilocify, stores the request ID
     *       in {@code release.additionalData["com.vilocify.component.request.id"]}, and creates
     *       a pending {@link VMMatch} with {@code vmComponentId=""} and
     *       {@code vmComponentVmid=requestId}.</li>
     *   <li>Maps vulnerabilities ({@link Vulnerability} in {@code sw360vm}) with mapped releases.</li>
     * </ol>
     */
    private void runVilocifyReverseMatchAndTrackingFeedbackJob() {
        VilocifyConnector connector = new VilocifyConnector();
        if (!connector.isConfigured()) {
            log.info("VilocifyMatch: Vilocify connector is not configured; skipping reverse match");
            return;
        }

        // Reconcile any releases that were already mapped by TrackingFeedback into VMComponent & VMMatch
        ensureVmComponentAndMatchForAlreadyMappedReleases(connector);

        Map<String, Release> releaseMap = compHandler.getAllReleasesIdMap();
        compHandler.fillVendors(releaseMap.values());
        Map<String, Component> componentMap = compHandler.getAllComponentsIdMap();

        List<Release> modifiedReleases = new ArrayList<>();
        for (Release release : releaseMap.values()) {
            if (release == null) {
                continue;
            }
            Map<String, String> externalIds = release.isSetExternalIds()
                    ? release.getExternalIds()
                    : Collections.emptyMap();
            String existingCompId = externalIds.get(SW360Constants.VILOCIFY_COMPONENT_ID);
            if (CommonUtils.isNotNullEmptyOrWhitespace(existingCompId)) {
                continue;
            }

            try {
                Optional<VMComponent> matchedVmCompOpt = connector.findComponentAsVmComponentByRelease(release);
                if (matchedVmCompOpt.isPresent()) {
                    VMComponent persistedVmComp = upsertVmComponentByVmid(matchedVmCompOpt.get());
                    compHandler.setVilocifyComponentIdOnReleaseInMemory(release, persistedVmComp.getVmid());
                    modifiedReleases.add(release);
                    upsertAcceptedVmMatchForRelease(release, persistedVmComp, componentMap);
                    log.info("VilocifyMatch: release " + release.getId()
                            + " matched to Vilocify component " + persistedVmComp.getVmid());
                } else {
                    Map<String, String> additionalData = release.isSetAdditionalData()
                            ? release.getAdditionalData()
                            : Collections.emptyMap();
                    String existingReqId = additionalData.get(SW360Constants.VILOCIFY_COMPONENT_REQUEST_ID);
                    if (CommonUtils.isNullEmptyOrWhitespace(existingReqId)) {
                        Optional<String> newReqIdOpt = connector.createComponentRequest(release);
                        if (newReqIdOpt.isPresent()) {
                            String requestId = newReqIdOpt.get();
                            compHandler.setVilocifyComponentRequestIdOnReleaseInMemory(release, requestId);
                            modifiedReleases.add(release);
                            upsertPendingVmMatchForComponentRequest(release, requestId, componentMap);
                            log.info("VilocifyMatch: created componentRequest " + requestId
                                    + " for unmapped release " + release.getId());
                        }
                    }
                }
            } catch (Exception e) {
                log.error("VilocifyMatch: failed processing release " + release.getId(), e);
            }
        }

        if (!modifiedReleases.isEmpty()) {
            compHandler.persistUpdatedReleases(modifiedReleases, "VilocifyMatch");
        }

        // 2d. Map vulnerabilities with mapped releases
        linkVilocifyNotificationsToMappedReleases();
    }

    /**
     * Resolves pending componentRequests (via {@link ComponentDatabaseHandler#resolvePendingVilocifyRequestsAndUpdateReleases()})
     * and updates {@link VMComponent} (queried by {@code byvmid}) and {@link VMMatch} in {@code sw360vm}.
     */
    private void ensureVmComponentAndMatchForAlreadyMappedReleases(VilocifyConnector connector) {
        Map<String, Release> releaseMap = compHandler.getAllReleasesIdMap();
        compHandler.fillVendors(releaseMap.values());
        Map<String, Component> componentMap = compHandler.getAllComponentsIdMap();

        for (Release release : releaseMap.values()) {
            if (release == null || !release.isSetExternalIds()) {
                continue;
            }
            String vmid = release.getExternalIds().get(SW360Constants.VILOCIFY_COMPONENT_ID);
            if (CommonUtils.isNullEmptyOrWhitespace(vmid)) {
                continue;
            }
            try {
                VMComponent vmComponent = dbHandler.getByVmId(VMComponent.class, vmid.trim());
                if (vmComponent == null) {
                    Optional<VMComponent> fetchedComp = connector.getComponentAsVmComponent(vmid.trim());
                    if (fetchedComp.isPresent()) {
                        vmComponent = upsertVmComponentByVmid(fetchedComp.get());
                    }
                }
                if (vmComponent != null) {
                    VMMatch pendingMatch = dbHandler.getMatchByIds(release.getId(), "");
                    if (pendingMatch != null) {
                        pendingMatch.setVmComponentId(vmComponent.getId());
                        pendingMatch.setState(VMMatchState.ACCEPTED);
                        SVMMapper.updateMatch(pendingMatch, vmComponent, release,
                                () -> componentMap.get(release.getComponentId()));
                        pendingMatch.setReleaseSvmId(vmComponent.getVmid());
                        dbHandler.update(pendingMatch);
                    } else {
                        upsertAcceptedVmMatchForRelease(release, vmComponent, componentMap);
                    }
                }
            } catch (Exception e) {
                log.warn("VilocifyMatch: could not ensure VMComponent/VMMatch for release " + release.getId(), e);
            }
        }
    }

    private VMComponent upsertVmComponentByVmid(VMComponent incoming) {
        VMComponent existing = dbHandler.getByVmId(VMComponent.class, incoming.getVmid());
        if (existing == null) {
            dbHandler.add(incoming);
            return dbHandler.getByVmId(VMComponent.class, incoming.getVmid());
        }
        incoming.setId(existing.getId());
        incoming.setRevision(existing.getRevision());
        incoming.setReceivedDate(existing.getReceivedDate());
        dbHandler.update(incoming);
        return incoming;
    }

    private void upsertAcceptedVmMatchForRelease(
            Release release, VMComponent vmComponent, Map<String, Component> componentMap) {
        if (vmComponent == null || CommonUtils.isNullEmptyOrWhitespace(vmComponent.getId())) {
            return;
        }
        VMMatch match = dbHandler.getMatchByIds(release.getId(), vmComponent.getId());
        if (match == null) {
            match = new VMMatch(vmComponent.getId(), release.getId(),
                    new HashSet<>(Collections.singleton(VMMatchType.SVM_ID)), VMMatchState.ACCEPTED);
        } else {
            match.setState(VMMatchState.ACCEPTED);
        }
        SVMMapper.updateMatch(match, vmComponent, release, () -> componentMap.get(release.getComponentId()));
        match.setReleaseSvmId(vmComponent.getVmid());
        if (CommonUtils.isNullEmptyOrWhitespace(match.getId())) {
            dbHandler.add(match);
        } else {
            dbHandler.update(match);
        }
    }

    private void upsertPendingVmMatchForComponentRequest(
            Release release, String requestId, Map<String, Component> componentMap) {
        VMMatch match = dbHandler.getMatchByIds(release.getId(), "");
        if (match == null) {
            match = new VMMatch("", release.getId(),
                    new HashSet<>(Collections.singleton(VMMatchType.SVM_ID)), VMMatchState.MATCHING_LEVEL_1);
        }
        SVMMapper.updateMatch(match, null, release, () -> componentMap.get(release.getComponentId()));
        match.setVmComponentId("");
        match.setVmComponentVmid(requestId);
        if (CommonUtils.isNullEmptyOrWhitespace(match.getId())) {
            dbHandler.add(match);
        } else {
            dbHandler.update(match);
        }
    }

    private void updatePendingVmMatchToAccepted(
            Release release, String requestId, VMComponent vmComponent, Map<String, Component> componentMap) {
        VMMatch pendingMatch = dbHandler.getMatchByIds(release.getId(), "");
        if (pendingMatch != null && requestId.equals(pendingMatch.getVmComponentVmid())) {
            pendingMatch.setVmComponentId(vmComponent.getId());
            pendingMatch.setState(VMMatchState.ACCEPTED);
            SVMMapper.updateMatch(pendingMatch, vmComponent, release,
                    () -> componentMap.get(release.getComponentId()));
            pendingMatch.setReleaseSvmId(vmComponent.getVmid());
            dbHandler.update(pendingMatch);
            return;
        }
        upsertAcceptedVmMatchForRelease(release, vmComponent, componentMap);
    }

    /**
     * Links mapped SW360 releases directly to their Vilocify notification {@link Vulnerability}
     * documents in {@code sw360vm} via {@link VMDatabaseHandler} (no Thrift round-trips),
     * and unlinks any notifications that are no longer associated with the component.
     */
    private void linkVilocifyNotificationsToMappedReleases() {
        VilocifyConnector connector = new VilocifyConnector();
        if (!connector.isConfigured()) {
            return;
        }
        Map<String, Release> releaseMap = compHandler.getAllReleasesIdMap();
        Map<String, List<Release>> releasesByComponentId = new HashMap<>();
        for (Release release : releaseMap.values()) {
            if (release != null && release.isSetExternalIds()) {
                String compId = release.getExternalIds().get(SW360Constants.VILOCIFY_COMPONENT_ID);
                if (CommonUtils.isNotNullEmptyOrWhitespace(compId)) {
                    releasesByComponentId.computeIfAbsent(compId.trim(), k -> new ArrayList<>()).add(release);
                }
            }
        }
        if (releasesByComponentId.isEmpty()) {
            return;
        }

        for (Map.Entry<String, List<Release>> entry : releasesByComponentId.entrySet()) {
            String componentId = entry.getKey();
            List<Release> targetReleases = entry.getValue();
            try {
                List<String> notificationIds = connector.getComponentNotificationIds(componentId);
                Set<String> activeVulnerabilityIds = new HashSet<>();

                for (String notificationId : notificationIds) {
                    Vulnerability vuln = dbHandler.getByExternalId(Vulnerability.class, notificationId);
                    if (vuln == null) {
                        Optional<Vulnerability> fetchedOpt =
                                connector.getNotificationAsSw360Vulnerability(notificationId);
                        if (fetchedOpt.isPresent()) {
                            vuln = fetchedOpt.get();
                            dbHandler.add(vuln);
                            vuln = dbHandler.getByExternalId(Vulnerability.class, notificationId);
                        }
                    } else if (!vuln.isSetAssignedExtComponentIds()
                            || vuln.getAssignedExtComponentIds().isEmpty()
                            || !vuln.isSetIsSetCvss()
                            || !vuln.isIsSetCvss()) {
                        connector.enrichNotificationVulnerabilityWithComponentsAndCvss(vuln);
                        dbHandler.update(vuln);
                    }
                    if (vuln == null || CommonUtils.isNullEmptyOrWhitespace(vuln.getId())) {
                        continue;
                    }
                    activeVulnerabilityIds.add(vuln.getId());
                    for (Release release : targetReleases) {
                        ReleaseVulnerabilityRelation relation =
                                dbHandler.getRelationByIds(release.getId(), vuln.getId());
                        if (relation == null) {
                            dbHandler.add(new ReleaseVulnerabilityRelation(release.getId(), vuln.getId()));
                            log.info("VilocifyMatch: linked notification " + notificationId
                                    + " (vulnId=" + vuln.getId() + ") to release " + release.getId());
                        }
                    }
                }

                // Prune stale relations if a component was removed from a notification
                for (Release release : targetReleases) {
                    List<ReleaseVulnerabilityRelation> existingRelations =
                            dbHandler.getRelationsByReleaseIds(Collections.singletonList(release.getId()));
                    if (existingRelations == null) {
                        continue;
                    }
                    for (ReleaseVulnerabilityRelation rel : existingRelations) {
                        if (rel != null
                                && CommonUtils.isNotNullEmptyOrWhitespace(rel.getVulnerabilityId())
                                && !activeVulnerabilityIds.contains(rel.getVulnerabilityId())) {
                            dbHandler.delete(rel);
                            log.info("VilocifyMatch: unlinked removed notification vulnId="
                                    + rel.getVulnerabilityId() + " from release " + release.getId());
                        }
                    }
                }
            } catch (Exception e) {
                log.error("VilocifyMatch: failed linking notifications for component " + componentId, e);
            }
        }
    }

    @Override
    public RequestSummary acceptMatch(User user, String matchId) throws TException {
        return setMatchState(user, matchId, VMMatchState.ACCEPTED);
    }

    @Override
    public RequestSummary declineMatch(User user, String matchId) throws TException {
        return setMatchState(user, matchId, VMMatchState.DECLINED);
    }

    private RequestSummary setMatchState(User user, String matchId, VMMatchState state){
        if (!PermissionUtils.isAdmin(user) || StringUtils.isEmpty(matchId)){
            return new RequestSummary(RequestStatus.FAILURE);
        }

        VMMatch match = dbHandler.getById(VMMatch.class, matchId);
        if (match == null){
            return new RequestSummary(RequestStatus.FAILURE);
        }
        match.setState(state);
        return new RequestSummary(dbHandler.update(match));
    }
}

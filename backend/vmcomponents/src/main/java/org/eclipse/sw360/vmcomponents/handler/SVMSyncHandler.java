/*
SPDX-FileCopyrightText: © 2022 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.vmcomponents.handler;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.thrift.TBase;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.db.ComponentDatabaseHandler;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.ReleaseVulnerabilityRelation;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.Vulnerability;
import org.eclipse.sw360.vmcomponents.common.SVMConstants;
import org.eclipse.sw360.vmcomponents.common.SVMMapper;
import org.eclipse.sw360.vmcomponents.common.SVMUtils;
import org.eclipse.sw360.vmcomponents.common.VMResult;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.*;
import org.eclipse.sw360.vmcomponents.db.VMDatabaseHandler;
import org.eclipse.sw360.vmcomponents.process.VMProcessHandler;
import org.eclipse.sw360.vulnerabilities.common.VulnerabilityMapper;
import org.jetbrains.annotations.NotNull;
import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Supplier;

import static org.apache.log4j.Logger.getLogger;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptySet;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotNull;

/**
 * mapping handler for JSON response from SVM service to pojos
 *
 * @author stefan.jaeger@evosoft.com
 */
public class SVMSyncHandler<T extends TBase> {

    private static final Logger log = getLogger(SVMSyncHandler.class);
    private static final String MATCH_KEY_SEPARATOR = "___";
    private static final ImmutableSet<VMMatchState> MATCH_STATES_WORTH_SAVING = ImmutableSet.of(VMMatchState.ACCEPTED, VMMatchState.DECLINED, VMMatchState.MATCHING_LEVEL_3);
    private static final ImmutableSet<VMMatchType> MATCH_TYPES_NAME_VERSION_VENDOR = ImmutableSet.of(VMMatchType.NAME_CR, VMMatchType.VERSION_CR, VMMatchType.VENDOR_CR);

    private final VMDatabaseHandler dbHandler;
    private ComponentDatabaseHandler compDB = null;
    private final Class<T> type;
    private final String uuid = UUID.randomUUID().toString();

    public  SVMSyncHandler(Class<T> type) throws MalformedURLException, SW360Exception {
        this(type, null);
    }

    private SVMSyncHandler(Class<T> type, VMDatabaseHandler dbHandler) throws MalformedURLException, SW360Exception {
        assertNotNull(type);
        this.type = type;
        if (dbHandler == null) {
            this.dbHandler = new VMDatabaseHandler();
        } else {
            this.dbHandler = dbHandler;
        }
    }

    public String getUuid() {
        return uuid;
    }

    public Class<T> getType() {
        return type;
    }

    public VMResult finishReport(String startTimeReporting){
        // get reporting element via start date
        VMProcessReporting reporting = dbHandler.getByCreationDate(VMProcessReporting.class, startTimeReporting);
        if (reporting == null){
            return new VMResult(SVMUtils.newRequestSummary(RequestStatus.FAILURE, 0, 0, "cannot find "+VMProcessReporting.class.getSimpleName()+" with startDate "+startTimeReporting));
        }

        // get last updated element for end reporting
        T lastUpdated = dbHandler.getLastUpdated(this.type);
        if (lastUpdated != null){
            if (VMComponent.class.isAssignableFrom(type))
                reporting.setEndDate(((VMComponent)lastUpdated).getLastUpdateDate());
            else if (VMAction.class.isAssignableFrom(type))
                reporting.setEndDate(((VMAction)lastUpdated).getLastUpdateDate());
            else if (VMPriority.class.isAssignableFrom(type))
                reporting.setEndDate(((VMPriority)lastUpdated).getLastUpdateDate());
            else
                throw new IllegalArgumentException("unknown type " + type.getSimpleName());
        } else {
            reporting.setEndDate(SW360Utils.getCreatedOnTime());
        }

        // calc processing time
        Date start = SW360Utils.getDateFromTimeString(reporting.getStartDate());
        Date end = SW360Utils.getDateFromTimeString(reporting.getEndDate());

        if (start != null && end != null && end.before(start)){
            reporting.setEndDate(SW360Utils.getCreatedOnTime());
            end = SW360Utils.getDateFromTimeString(reporting.getEndDate());
        }

        long processingTime = start == null || end == null ? 0 :end.getTime() - start.getTime();
        reporting.setProcessingSeconds((int) (processingTime / 1000));

        RequestStatus requestStatus = dbHandler.update(reporting);

        return new VMResult(SVMUtils.newRequestSummary(requestStatus, 1, 1, null));
    }

    public VMResult<T> syncDatabase(T newElement){
        if (newElement == null ){
            return new VMResult<>(SVMUtils.newRequestSummary(RequestStatus.SUCCESS, 0, 0, null));
        }

        T elementToUpdate;
        String vmid = SVMUtils.getVmid(newElement);
        T oldElement = dbHandler.getByVmId((Class<T>) newElement.getClass(), vmid);
        if (oldElement == null){
            RequestStatus requestStatus = dbHandler.add(newElement);
            String message = RequestStatus.FAILURE == requestStatus ? "failed to save new element ("+newElement.getClass()+")":null;
            return new VMResult<>(SVMUtils.newRequestSummary(requestStatus, 1, 1, message));
        }

        if (VMComponent.class.isAssignableFrom(newElement.getClass())) {
            elementToUpdate = (T) SVMMapper.updateComponent((VMComponent) oldElement, (VMComponent) newElement);
        } else if (VMAction.class.isAssignableFrom(newElement.getClass())) {
            elementToUpdate = (T) SVMMapper.updateAction((VMAction) oldElement, (VMAction) newElement);
        } else if (VMPriority.class.isAssignableFrom(newElement.getClass())) {
            elementToUpdate = (T) SVMMapper.updatePriority((VMPriority) oldElement, (VMPriority) newElement);
        } else if (Vulnerability.class.isAssignableFrom(newElement.getClass())) {
            elementToUpdate = (T) VulnerabilityMapper.updateVulnerability((Vulnerability) oldElement, (Vulnerability) newElement);
        } else {
            throw new IllegalArgumentException("unknown type " + newElement.getClass().getSimpleName());
        }

        if (!elementToUpdate.equals(oldElement)) {
            RequestStatus requestStatus = dbHandler.update(elementToUpdate);
            String message = RequestStatus.FAILURE == requestStatus ? "failed to save updated element ("+elementToUpdate.getClass()+")":null;
            return new VMResult<>(SVMUtils.newRequestSummary(requestStatus, 1, 1, message));
        }

        return new VMResult<>(SVMUtils.newRequestSummary(RequestStatus.SUCCESS, 0, 0, null));
    }

    public VMResult<T> storeNewElement(String vmid){
        T element = this.dbHandler.getByVmId(this.type, vmid);
        RequestStatus requestStatus = RequestStatus.SUCCESS;
        if (element == null){
            if (VMComponent.class.isAssignableFrom(type)){
                element = (T) new VMComponent(SW360Utils.getCreatedOnTime(), vmid);
            } else if (VMAction.class.isAssignableFrom(type)) {
                element = (T) new VMAction(vmid);
            } else if (VMPriority.class.isAssignableFrom(type)) {
                element = (T) new VMPriority(vmid);
            } else {
                throw new IllegalArgumentException("unknown type "+ this.type.getSimpleName());
            }
            requestStatus = dbHandler.add(element);
        }
        String message = "";
        int stored = 1;
        if (RequestStatus.FAILURE.equals(requestStatus)){
            message+="failed to save new element with vmid "+vmid;
            stored = 0;
        }
        return new VMResult<>(SVMUtils.newRequestSummary(requestStatus, 1, stored, message), element);
    }

    public VMResult<String> deleteMissingElements(List<String> newElementVmIds){
        if (newElementVmIds == null || newElementVmIds.isEmpty()){
            return new VMResult<>(SVMUtils.newRequestSummary(RequestStatus.FAILURE, 0, 0, "removing of missing elements aborted because new element list is null or empty!"));
        }
        Set<String> knownIds = dbHandler.getAllVmIds(this.type);
        Set<String> toBeRemoved = Sets.difference(nullToEmptySet(knownIds), Sets.newHashSet(newElementVmIds));

        int deleted = 0;
        List<String> toBeRemovedDBIds = new ArrayList<>();
        for (String elementVmId : toBeRemoved) {
            T element = dbHandler.getByVmId(this.type, elementVmId);
            if (element != null){
                toBeRemovedDBIds.add(SVMUtils.getId(element));
                RequestStatus delete = dbHandler.delete(element);
                if (RequestStatus.SUCCESS.equals(delete)){
                    deleted++;
                }
            }
        }
        return new VMResult<>(SVMUtils.newRequestSummary(RequestStatus.SUCCESS, toBeRemoved.size(), deleted, null), toBeRemovedDBIds);
    }

    public VMResult<String> cleanUpMatches(List<String> toBeCleanedComponentIds){
        if (toBeCleanedComponentIds == null || toBeCleanedComponentIds.isEmpty()){
            return new VMResult<>(SVMUtils.newRequestSummary(RequestStatus.SUCCESS, 0, 0, "match cleanup list is empty. nothing to do.."));
        }
        List<VMMatch> toBeCleanedMatches = dbHandler.getMatchesByComponentIds(toBeCleanedComponentIds);
        int deleted = 0;
        if (toBeCleanedMatches != null){
            for (VMMatch match: toBeCleanedMatches) {
                RequestStatus delete = dbHandler.delete(match);
                if (RequestStatus.SUCCESS.equals(delete)){
                    deleted++;
                }

            }
        }
        return new VMResult<>(SVMUtils.newRequestSummary(RequestStatus.SUCCESS, toBeCleanedComponentIds.size(), deleted, null), toBeCleanedComponentIds);
    }

    public VMResult<String> getSMVElementIds(String url){
        try {
            String idResponse = SVMUtils.prepareJSONRequestAndGetResponse(url);
            JsonArray ids = Jsoner.deserialize(idResponse, new JsonArray());

            if (ids == null || ids.isEmpty()){
                return null;
            }

            List<String> elementIds = new ArrayList<>(ids.size());
            for (Object id : ids) {
                elementIds.add(id.toString());
            }
            return new VMResult<>(SVMUtils.newRequestSummary(RequestStatus.SUCCESS, ids.size(), ids.size(), null), elementIds);
        } catch (Exception e) {
            String message = "Failed to get elements of type "+type.getSimpleName()+" from SMV: "+e.getMessage();
            log.error(message,e);
            return new VMResult<>(SVMUtils.newRequestSummary(RequestStatus.FAILURE, 0, 0, message));
        }
    }

    public VMResult getSMVElementMasterDataById(String elementId, String url){
        return getSMVElementMasterDataById(dbHandler.getById(type, elementId), url);
    }

    private VMResult<T> getSMVElementMasterDataById(T element, String url){
        if (element != null){
            try {
                url += "/" + SVMUtils.getVmid(element);
                String response = SVMUtils.prepareJSONRequestAndGetResponse(url);

                JsonObject jsonObject = Jsoner.deserialize(response, new JsonObject());

                if (VMComponent.class.isAssignableFrom(element.getClass()))
                    element = (T) SVMMapper.updateComponentByJSON((VMComponent) element, jsonObject);
                else if (VMAction.class.isAssignableFrom(element.getClass()))
                    element = (T) SVMMapper.updateActionByJSON((VMAction) element, jsonObject);
                else if (VMPriority.class.isAssignableFrom(element.getClass()))
                    element = (T) SVMMapper.updatePriorityByJSON((VMPriority) element, jsonObject);
                else if (Vulnerability.class.isAssignableFrom(element.getClass())){
                    element = (T) SVMMapper.updateVulnerabilityByJSON((Vulnerability) element, jsonObject);
                    element = (T) updateVulnerability((Vulnerability) element);
                } else
                    throw new IllegalArgumentException("unknown type "+ this.type.getSimpleName());

            } catch (Exception e){
                String message = "Failed to get master data for "+element.getClass().getSimpleName()+" "+SVMUtils.getVmid(element)+" from SMV";
                log.error(message,e);
                return new VMResult<>(SVMUtils.newRequestSummary(RequestStatus.FAILURE, 1, 0, message + ": " + e.getMessage()));
            }
        }
        return new VMResult<>(SVMUtils.newRequestSummary(RequestStatus.SUCCESS, 1, 1, null), element);
    }

    private Vulnerability updateVulnerability(Vulnerability vul){
        if (!StringUtils.isEmpty(vul.getPriority())){
            VMPriority priority = dbHandler.getByVmId(VMPriority.class, vul.getPriority());
            if (priority != null){
                vul.setPriority(vul.getPriority()+" - "+priority.getShortText());
                vul.setPriorityText(priority.getLongText());
            }
        }
        if (!StringUtils.isEmpty(vul.getAction())){
            VMAction action = dbHandler.getByVmId(VMAction.class, vul.getAction());
            if (action != null){
                vul.setAction(action.getText());
            }
        }
        return vul;
    }

    private void evaluateMatchState(VMMatch match, Set<VMMatchType> newMatchTypes) {
        if (newMatchTypes == null || VMMatchState.ACCEPTED.equals(match.getState())) {
            // null is nonsense and if it is ACCEPTED, don't forget it
            return;
        }
        Set<VMMatchType> matchTypes = match.isSetMatchTypes() ? match.getMatchTypes() : new HashSet<>();

        if (matchTypes.contains(VMMatchType.CPE)
                || matchTypes.contains(VMMatchType.SVM_ID)
                || matchTypes.containsAll(newMatchTypes)) {
            // no changes required, because no new information are available
            return;
        }

        // add new info
        matchTypes.addAll(newMatchTypes);
        match.setMatchTypes(matchTypes);

        // clarify state
        if (matchTypes.contains(VMMatchType.CPE) || matchTypes.contains(VMMatchType.SVM_ID)){
            // finish with ACCEPTED because CPE or SVM_ID match is strong enough
            match.setState(VMMatchState.ACCEPTED);
            return;
        }

        // check if at least one name AND vendor AND version check is fulfilled
        boolean nameMatched = matchTypes.contains(VMMatchType.NAME_CR) || matchTypes.contains(VMMatchType.NAME_RC);
        boolean vendorMatched = matchTypes.contains(VMMatchType.VENDOR_CR) || matchTypes.contains(VMMatchType.VENDOR_RC);
        boolean versionMatched = matchTypes.contains(VMMatchType.VERSION_CR) || matchTypes.contains(VMMatchType.VERSION_RC);

        int matchedPartCount = 0;
        if (nameMatched) matchedPartCount++;
        if (vendorMatched) matchedPartCount++;
        if (versionMatched) matchedPartCount++;

        // the text match requires an approval
        switch (matchedPartCount){
            case 1:
                match.setState(VMMatchState.MATCHING_LEVEL_1);
                break;
            case 2:
                match.setState(VMMatchState.MATCHING_LEVEL_2);
                break;
            case 3:
                match.setState(VMMatchState.MATCHING_LEVEL_3);
                break;
            default:
                throw new IllegalStateException("there is some error in the code :(, matches=" + matchedPartCount + " should not be possible...");
        }
    }

    private void evaluateDBMatches(String componentId, String releaseId, Set<VMMatchType> matchTypes, HashMap<String, VMMatch> knownMatches) {
        if (!StringUtils.isEmpty(releaseId) && !StringUtils.isEmpty(componentId)){
            VMMatch match = knownMatches.getOrDefault(getMatchKey(componentId, releaseId), dbHandler.getMatchByIds(releaseId, componentId));
            if (match == null){
                match = new VMMatch(componentId, releaseId, new HashSet<>(), null);
            }
            evaluateMatchState(match, matchTypes);
            knownMatches.put(getMatchKey(match.getVmComponentId(), match.getReleaseId()), match);
        }
    }

    @NotNull
    private String getMatchKey(String componentId, String releaseId) {
        return componentId + MATCH_KEY_SEPARATOR + releaseId;
    }

    private void evaluateDBMatches(VMComponent component, Set<String> releaseIds, Set<VMMatchType> matchTypes, HashMap<String, VMMatch> knownMatches) {
        if (releaseIds != null){
            for (String releaseId:releaseIds) {
                evaluateDBMatches(component.getId(), releaseId, matchTypes, knownMatches);
            }
        }
    }

    private void evaluateDBMatches(Release release, Set<String> componentIds, Set<VMMatchType> matchTypes, HashMap<String, VMMatch> knownMatches) {
        if (componentIds != null){
            for (String componentId:componentIds) {
                evaluateDBMatches(componentId, release.getId(), matchTypes, knownMatches);
            }
        }
    }

    private void initComponentDatabaseHandler() {
        if (compDB == null) {
            try {
                compDB = new ComponentDatabaseHandler(DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_DATABASE, DatabaseSettings.COUCH_DB_ATTACHMENTS);
            } catch (MalformedURLException e) {
                String message = "Failed to initialize " + ComponentDatabaseHandler.class.getSimpleName();
                log.error(message, e);
            }
        }
    }

    public VMResult<VMComponent> findMatchByComponent(String componentId){
        if (!VMComponent.class.isAssignableFrom(type)){
            return new VMResult<>(SVMUtils.newRequestSummary(RequestStatus.FAILURE, 0, 0, "no match possible for type " + this.type.getSimpleName()));
        }
        VMComponent component = dbHandler.getById(VMComponent.class, componentId);
        HashMap<String, VMMatch> knownMatches = new HashMap<>();
        if (component == null) {
            return new VMResult<>(SVMUtils.newRequestSummary(RequestStatus.SUCCESS, 0, 0, null), (VMComponent)null);
        }
        try {
            initComponentDatabaseHandler();
        } catch (RuntimeException e) {
            return new VMResult<>(SVMUtils.newRequestSummary(RequestStatus.FAILURE, 0, 0, e.getMessage()));
        }

        // match by CPE
        String svmCpe = component.getCpe();
        if (!StringUtils.isEmpty(svmCpe)) {
            Set<String> releaseIds = compDB.getReleaseIdsByCpeCaseInsensitive(svmCpe);
            evaluateDBMatches(component, releaseIds, Collections.singleton(VMMatchType.CPE), knownMatches);
        }
        // stop matching if the CPE matches
        if (knownMatches.size() > 0){
            return finalizeAndSuccess(component, knownMatches);
        }
        // match by SVM id
        String svmId = component.getVmid();
        if (!StringUtils.isEmpty(svmId)) {
            Set<String> releaseIds = compDB.getReleaseIdsBySvmId(svmId);
            evaluateDBMatches(component, releaseIds, Collections.singleton(VMMatchType.SVM_ID), knownMatches);
        }
        // stop matching if the SVM id matches
        if (knownMatches.size() > 0){
            return finalizeAndSuccess(component, knownMatches);
        }

        // match by name, version and vendor from svm component to sw360 release
        Set<String> matchedByName = Collections.emptySet();
        String svmName = component.getName();
        if (!StringUtils.isEmpty(svmName)) {
            matchedByName = compDB.getReleaseIdsByNamePrefixCaseInsensitive(svmName);
        }

        Set<String> matchedByVersion = Collections.emptySet();
        String svmVersion = component.getVersion();
        if (!StringUtils.isEmpty(svmVersion)) {
            matchedByVersion = compDB.getReleaseIdsByVersionPrefixCaseInsensitive(svmVersion);
        }

        Set<String> matchedByVendor = Collections.emptySet();
        String svmVendor = component.getVendor();
        if (!StringUtils.isEmpty(svmVendor)) {
            Set<String> vendorIds = compDB.getVendorIdsByNamePrefixCaseInsensitive(svmVendor);
            if (vendorIds != null && !vendorIds.isEmpty()) {
                matchedByVendor = compDB.getReleaseIdsByVendorIds(vendorIds);
            }
        }

        Set<String> matchedReleaseIds = Sets.intersection(Sets.intersection(matchedByName, matchedByVersion), matchedByVendor);
        evaluateDBMatches(component, matchedReleaseIds, MATCH_TYPES_NAME_VERSION_VENDOR, knownMatches);
        return finalizeAndSuccess(component, knownMatches);
    }

    @NotNull
    private VMResult<VMComponent> finalizeAndSuccess(VMComponent component, HashMap<String, VMMatch> knownMatches) {
        finalizeMatches(knownMatches.values());
        return new VMResult<>(SVMUtils.newRequestSummary(RequestStatus.SUCCESS, 1, knownMatches.size(), null), component);
    }

    public VMResult<String> findMatchByRelease(String releaseId){
        if (!Release.class.isAssignableFrom(type)){
            return new VMResult<>(SVMUtils.newRequestSummary(RequestStatus.FAILURE, 0, 0, "no match possible for type " + this.type.getSimpleName()));
        }
        if (!StringUtils.isEmpty(releaseId)) {
            try {
                initComponentDatabaseHandler();
            } catch (RuntimeException e) {
                return new VMResult<>(SVMUtils.newRequestSummary(RequestStatus.FAILURE, 0, 0, e.getMessage()));
            }
            Release release;
            try {
                release = compDB.getRelease(releaseId, null);
            } catch (SW360Exception e) {
                String message = "Failed to get release " + releaseId;
                log.error(message, e);
                return new VMResult<>(SVMUtils.newRequestSummary(RequestStatus.FAILURE, 0, 0, message + ": " + e.getMessage()));
            }
            int releases = 0;
            HashMap<String, VMMatch> knownMatches = new HashMap<>();
            if (release != null) {
                releases = 1;

                // match by CPE
                // CPE Match is not necessary for the reverse match because it is an exact match and is done via VMComponent match

                // match by name from sw360 release to svm component (reverse)
                Set<String> matchedByName = Collections.emptySet();
                String name = release.getName();
                if (!StringUtils.isEmpty(name)) {
                    matchedByName = dbHandler.getComponentIdsByNamePrefixCaseInsensitive(name);
                }

                // match by version from sw360 release to svm component (reverse)
                Set<String> matchedByVersion = Collections.emptySet();
                String version = release.getVersion();
                if (!StringUtils.isEmpty(version)) {
                    matchedByVersion = dbHandler.getComponentIdsByVersionPrefixCaseInsensitive(version);
                }

                // match by vendor from sw360 release to svm component (reverse)
                Set<String> matchedByVendor = Collections.emptySet();
                Vendor vendor = release.getVendor();
                if (vendor != null) {
                    String vendorName = vendor.getShortname();
                    if (!StringUtils.isEmpty(vendorName)) {
                        matchedByVendor = dbHandler.getComponentIdsByVendorPrefixCaseInsensitive(vendorName);
                    }
                    vendorName = vendor.getFullname();
                    if (!StringUtils.isEmpty(vendorName)) {
                        matchedByVendor.addAll(dbHandler.getComponentIdsByVendorPrefixCaseInsensitive(vendorName));
                    }
                }

                Set<String> matchedComponentIds = Sets.intersection(Sets.intersection(matchedByName, matchedByVersion), matchedByVendor);
                evaluateDBMatches(release, matchedComponentIds, MATCH_TYPES_NAME_VERSION_VENDOR, knownMatches);
            }
            finalizeMatches(knownMatches.values());
            List<String> componentIds = getComponentIdsOfMatches(knownMatches.values());
            return new VMResult<>(SVMUtils.newRequestSummary(RequestStatus.SUCCESS, releases, componentIds.size(), null), componentIds);
        }
        return new VMResult<>(SVMUtils.newRequestSummary(RequestStatus.SUCCESS, 0, 0, null), Collections.emptyList());
    }

    private List<String> getComponentIdsOfMatches(Collection<VMMatch> matches){
        if (matches == null || matches.isEmpty()){
            return Collections.emptyList();
        }
        HashSet<String> componentIds = new HashSet<>();
        for (VMMatch match: matches) {
            if (!StringUtils.isEmpty(match.getVmComponentId())){
                componentIds.add(match.getVmComponentId());
            }
        }
        return new ArrayList<>(componentIds);
    }

    private void finalizeMatches(Collection<VMMatch> matches){
        if (matches == null || matches.isEmpty()){
            return;
        }
        for (VMMatch match: matches) {
            if (isWorthSaving(match)){
                VMComponent vmComponent = null;
                if (!StringUtils.isEmpty(match.getVmComponentId())){
                    vmComponent = dbHandler.getById(VMComponent.class, match.getVmComponentId());
                }
                if (!StringUtils.isEmpty(match.getReleaseId())){
                    final Release release = loadRelease(match);
                    Supplier<Component> componentSupplier = getComponentSupplier(release);
                    SVMMapper.updateMatch(match, vmComponent, release, componentSupplier);
                } else {
                    SVMMapper.updateMatch(match, vmComponent, null, () -> null);
                }
                log.info(String.format("Saving match %s", match.toString()));
                if (StringUtils.isEmpty(match.getId())){
                    dbHandler.add(match);
                } else {
                    dbHandler.update(match);
                }
            } else {
                log.info(String.format("Match %s not worth saving", match.toString()));
            }
        }
    }

    @NotNull
    private Supplier<Component> getComponentSupplier(Release release) {
        return () -> {
                        Component component = null;
                        if (release != null && !StringUtils.isEmpty(release.getComponentId())) {
                            try {
                                component = compDB.getComponent(release.getComponentId(), null);
                            } catch (SW360Exception e) {
                                // no exception logging necessary because the component will later be shown as "NOT FOUND"
                            }
                        }
                        return component;
                    };
    }

    private Release loadRelease(VMMatch match) {
        Release release = null;
        try {
            release = compDB.getRelease(match.getReleaseId(), null, VMProcessHandler.getVendorCache());
        } catch (SW360Exception e) {
            // no exception logging necessary because the release will later be shown as "NOT FOUND"
        }
        return release;
    }

    private boolean isWorthSaving(VMMatch match) {
        return MATCH_STATES_WORTH_SAVING.contains(match.getState());
    }

    public VMResult<String> getVulnerabilitiesByComponentId(String componentId, String url){
        return getVulnerabilitiesByComponent(dbHandler.getById(VMComponent.class, componentId), url);
    }

    private VMResult<String> getVulnerabilitiesByComponent(VMComponent component, String url){
        if (component == null || StringUtils.isEmpty(url)){
            return new VMResult<>(SVMUtils.newRequestSummary(RequestStatus.FAILURE, 0, 0, "component/url cannot be empty.."));

        }
        List<VMMatch> matches = dbHandler.getMatchesByComponentIds(Collections.singletonList(component.getId()));
        if (matches == null || matches.isEmpty()){
            return new VMResult<>(SVMUtils.newRequestSummary(RequestStatus.SUCCESS, 1, 0, "no known matches for component: " + component.toString()));
        }
        // Counters for result
        Set<String> vulIds = new HashSet<>();

        for (VMMatch match: matches) {
            if (VMMatchState.ACCEPTED.equals(match.getState())){
                Set<String> vulVmIds = getVulIdsPerComponentVmId(component.getVmid(), url);
                if (vulVmIds == null || vulVmIds.isEmpty()) {
                    continue;
                }
                for (String vulVmId: vulVmIds) {
                    Vulnerability vulnerability = dbHandler.getByExternalId(Vulnerability.class, vulVmId);
                    if (vulnerability == null) {
                        vulnerability = new Vulnerability(vulVmId);
                        dbHandler.add(vulnerability);
                    }
                    vulIds.add(vulnerability.getId());
                    ReleaseVulnerabilityRelation relation = dbHandler.getRelationByIds(match.getReleaseId(), vulnerability.getId());
                    if (relation == null) {
                        relation = new ReleaseVulnerabilityRelation(match.releaseId, vulnerability.getId());
                        dbHandler.add(relation);
                    }
                }
            }
        }

        if (!matches.isEmpty() && vulIds.isEmpty()) {
            // Found matches but no vulns from SVM
            return new VMResult<>(SVMUtils.newRequestSummary(RequestStatus.SUCCESS, 1, 0, "got no vulnerabilities for component: " + component.toString()));
        }

        if (!vulIds.isEmpty()) {
            return new VMResult<>(SVMUtils.newRequestSummary(RequestStatus.SUCCESS, 1, vulIds.size(), null), new ArrayList<>(vulIds));
        }

        return new VMResult<>(SVMUtils.newRequestSummary(RequestStatus.SUCCESS, 1, 0, "no accepted matches found for component: " + component.toString()));
    }

    private Set<String> getVulIdsPerComponentVmId(String componentVmId, String url){
        if (!StringUtils.isEmpty(componentVmId)){
            try {
                url = url.replace(SVMConstants.COMPONENTS_ID_WILDCARD, componentVmId);
                String response = SVMUtils.prepareJSONRequestAndGetResponse(url);
                JsonArray ids = Jsoner.deserialize(response, new JsonArray());

                if (ids == null || ids.isEmpty()){
                    return Collections.emptySet();
                }

                Set<String> vulIds = new HashSet<>();
                for (Object id : ids) {
                    String vulId;
                    if (id instanceof JsonObject) {
                        Object vulIdObj = ((JsonObject) id).get(SVMConstants.VULNERABILITY_ID);
                        if (vulIdObj == null) {
                            continue;
                        }
                        vulId = vulIdObj.toString();
                    } else {
                        vulId = id.toString();
                    }
                    if (!StringUtils.isEmpty(vulId)){
                        vulIds.add(vulId);
                    }
                }
                return vulIds;

            } catch (IOException | RuntimeException | URISyntaxException e){
                String message = "Failed to get vulnerabilities for component "+componentVmId+" from SMV";
                log.error(message, e);
                return Collections.emptySet();
            }
        }
        return Collections.emptySet();
    }
}

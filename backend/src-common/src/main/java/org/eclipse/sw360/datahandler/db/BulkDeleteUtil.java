/*
 * Copyright (C) TOSHIBA CORPORATION, 2023. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.common.utils.BackendUtils;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.entitlement.ComponentModerator;
import org.eclipse.sw360.datahandler.entitlement.ReleaseModerator;
import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.Source;
import org.eclipse.sw360.datahandler.thrift.changelogs.Operation;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.BulkOperationNode;
import org.eclipse.sw360.datahandler.thrift.components.BulkOperationNodeType;
import org.eclipse.sw360.datahandler.thrift.components.BulkOperationResultState;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;

import com.cloudant.client.api.model.Document;
import com.cloudant.client.api.model.Response;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.common.SW360Assert.*;

/**
 * Provides a utility for the bulk delete function
 */
public class BulkDeleteUtil extends BackendUtils {

    private static final String CONFLICT_ERROR = "conflict";
    
    private static final Logger log = LogManager.getLogger(BulkDeleteUtil.class);

    private static final int LOOP_MAX = 100000;
    
    private ComponentDatabaseHandler componentDatabaseHandler;
    private ComponentRepository componentRepository;
    private ReleaseRepository releaseRepository;
    private ProjectRepository projectRepository;
    private ComponentModerator componentModerator;
    @SuppressWarnings("unused")
    private ReleaseModerator releaseModerator;
    private AttachmentConnector attachmentConnector;
    private AttachmentDatabaseHandler attachmentDatabaseHandler;
    private DatabaseHandlerUtil dbHandlerUtil;    
    
    private BulkDeleteUtilInspector inspector;
    
    public interface BulkDeleteUtilInspector {
        void checkVariables(Map<String, Release> releaseMap, Map<String, Component> componentMap,Map<String, Boolean> externalLinkMap, Map<String, List<String>> referencingReleaseIdsMap);
        void checkLoopState(int loopCount, Map<String, Release> releaseMap, Map<String, Component> componentMap, Map<String, BulkOperationResultState> resultStateMap);
        void checkLeafReleaseIdsInLoop(int loopCount, Set<String> leafReleaseIds);
        void checkUpdatedReferencingReleaseListInLoop(int loopCount, List<Release> updatedReferencingReleaseList);
        void checkUpdatedComponentListInLoop(int loopCount, List<Component> updatedComponentList);
        void checkDeletedReleaseListInLoop(int loopCount, List<Release> deletedReleaseList);
    }
    
    public BulkDeleteUtil(
            ComponentDatabaseHandler componentDatabaseHandler, 
            ComponentRepository componentRepository,  
            ReleaseRepository releaseRepository,
            ProjectRepository projectRepository,
            ComponentModerator componentModerator,
            ReleaseModerator releaseModerator,
            AttachmentConnector attachmentConnector,
            AttachmentDatabaseHandler attachmentDatabaseHandler,
            DatabaseHandlerUtil dbHandlerUtil
            ) {
        this.componentDatabaseHandler = componentDatabaseHandler;
        this.componentRepository = componentRepository;
        this.releaseRepository = releaseRepository;
        this.projectRepository = projectRepository;
        this.componentModerator = componentModerator;
        this.releaseModerator = releaseModerator;
        this.attachmentConnector = attachmentConnector; 
        this.attachmentDatabaseHandler = attachmentDatabaseHandler;
        this.dbHandlerUtil = dbHandlerUtil;
        this.inspector = null;
    }

    public BulkOperationNode deleteBulkRelease(String releaseId, User user, boolean isPreview) throws SW360Exception  {
        if (!BackendUtils.IS_BULK_RELEASE_DELETING_ENABLED) {
            throw fail(500, "Bulk release deleting feature is not enabled.");
        }
        if (!PermissionUtils.isAdmin(user)) {
            throw fail(403, "Failed to check the admin privilege.");
        }
        if (!PermissionUtils.IS_ADMIN_PRIVATE_ACCESS_ENABLED) {
            throw fail(500, "Admin private access is not enabled.");
        }
        
        Release release = releaseRepository.get(releaseId);
        assertNotNull(release, "No releases found to bulk delete!");
        
        //create a result state object
        Map<String, BulkOperationResultState> resultStateMap = new HashMap<String, BulkOperationResultState>();
        
        //create an all linked release map
        Map<String, Release> allLinkedReleaseMap = new HashMap<String, Release>();  
        getAllLinkedReleaseMap(release.getId(), allLinkedReleaseMap);
        Set<String> allLinkedReleaseIds = allLinkedReleaseMap.keySet();

        //create a deep copied release map for temporary data.
        Map<String, Release> workReleaseMap = getDuplicatedReleaseMap(allLinkedReleaseMap);

        //create an external flag map and an ids map of referencing releases
        Map<String, Boolean> externalLinkMap = new HashMap<String, Boolean>();
        Map<String, List<String>> referencingReleaseIdsMap = new HashMap<String, List<String>>();
        getExternalLinkMap(allLinkedReleaseMap.keySet(), externalLinkMap, referencingReleaseIdsMap);
        
        //update result state map for excluded releases
        for (String externalLinkMapId : externalLinkMap.keySet()) {
            if (externalLinkMap.get(externalLinkMapId)) {
                resultStateMap.put(externalLinkMapId, BulkOperationResultState.EXCLUDED);
            }
        }
        
        //create a linked component map
        Map<String, Component> allComponentMap = getComponenMap(allLinkedReleaseMap);
        Map<String, Component> workComponentMap = getDuplicatedComponentMap(allComponentMap);
        
        if (inspector != null) inspector.checkVariables(workReleaseMap, workComponentMap, externalLinkMap, referencingReleaseIdsMap);
        
        //delete linked releases
        boolean isCompleted = false;
        for (int loopCount=0; loopCount<LOOP_MAX; loopCount++) {
            
            if (inspector != null) inspector.checkLoopState(loopCount, workReleaseMap, workComponentMap, resultStateMap);
            
            //get leaf releases
            Set<String> leafReleaseIds = getLeafReleaseIds(workReleaseMap);
            if (inspector != null) inspector.checkLeafReleaseIdsInLoop(loopCount, leafReleaseIds);
           
            if (CommonUtils.isNullOrEmptyCollection(leafReleaseIds)) {
                isCompleted = true;
                break;
            }
            //exclude releases that have external link
            boolean hasExternalLink = false;
            for (String leafReleaseId : leafReleaseIds) {
                if (externalLinkMap.get(leafReleaseId)) {
                    hasExternalLink = true;
                    //remove the release from work tree
                    workReleaseMap.remove(leafReleaseId);
                    
                    //remove the release id from ReleaseIdToRelationship of reference releases
                    List<String> referencingReleaseIds = referencingReleaseIdsMap.get(leafReleaseId);
                    for (String referencingReleaseId : referencingReleaseIds) {
                        if (workReleaseMap.containsKey(referencingReleaseId)) {
                            //update the work tree
                            Release referencingRelease = workReleaseMap.get(referencingReleaseId);
                            Map<String, ReleaseRelationship> relationMap = referencingRelease.getReleaseIdToRelationship();
                            relationMap.remove(leafReleaseId);
                            if (!isPreview) {
                                releaseRepository.update(referencingRelease);
                            }
                        }
                    }
                }
            }
            if (hasExternalLink) {
                continue;
            }
            
            //filter releases that failed last time 
            Set<String> targetLeafReleaseIds = leafReleaseIds;
            Set<String> deletedLeafReleaseIds = new HashSet<String>();
            for (String targetLeafReleaseId : targetLeafReleaseIds) {
                if (resultStateMap.containsKey(targetLeafReleaseId)) {
                    BulkOperationResultState state = resultStateMap.get(targetLeafReleaseId); 
                    if (state  == BulkOperationResultState.FAILED || state == BulkOperationResultState.CONFLICTED) {
                        log.warn(String.format("Release %s is skipped because the last status was error.", targetLeafReleaseId));
                        continue;
                    }
                }
                deletedLeafReleaseIds.add(targetLeafReleaseId);
            }
            if (CommonUtils.isNullOrEmptyCollection(deletedLeafReleaseIds)) {
                isCompleted = true;
                log.warn("Could not find a release that can be deleted.");
                break;
            }

            //delete attachment
            if (!isPreview) {
                Set<String> filteredReleaseIds = new HashSet<String>();
                for (String deletedLeafReleaseId : deletedLeafReleaseIds) {
                    try {
                        Release deletedLeafRelease = workReleaseMap.get(deletedLeafReleaseId);
                        deleteReleaseAttachments(deletedLeafRelease);
                    } catch(Exception ex) {
                        filteredReleaseIds.add(deletedLeafReleaseId);
                        resultStateMap.put(deletedLeafReleaseId, BulkOperationResultState.FAILED);
                        log.error(String.format("failed to delete the release attachment, release id=%s", deletedLeafReleaseId), ex);
                    }
                }
                deletedLeafReleaseIds.removeAll(filteredReleaseIds);
            }
            
            //update referencing releases
            Map<String, Map<String, ReleaseRelationship>> previousRelationshipMap = new HashMap<String, Map<String, ReleaseRelationship>>();
            Set <String> updatedReferencingReleaseIds = new HashSet<String>();
            for (String deletedLeafReleaseId : deletedLeafReleaseIds) {
                //remove the release id from ReferencedReleaseIds of referencing releases
                List<String> referencingReleaseIds = referencingReleaseIdsMap.get(deletedLeafReleaseId);
                for (String referencingReleaseId : referencingReleaseIds) {
                    assertTrue(workReleaseMap.containsKey(referencingReleaseId));
                    Release referencingRelease = workReleaseMap.get(referencingReleaseId);
                    //backup current relationship
                    previousRelationshipMap.put(referencingReleaseId, getDuplicatedReleaseIdToRelationship(referencingRelease));
                    //update the work tree
                    Map<String, ReleaseRelationship> relationMap = referencingRelease.getReleaseIdToRelationship();
                    relationMap.remove(deletedLeafReleaseId);
                    updatedReferencingReleaseIds.add(referencingReleaseId);
                }
            }
            List<Release> updatedReferencingReleaseList = updatedReferencingReleaseIds.stream().map(id -> workReleaseMap.get(id)).collect(Collectors.toList());
            if (inspector != null) inspector.checkUpdatedReferencingReleaseListInLoop(loopCount, updatedReferencingReleaseList);
            Map<String, BulkOperationResultState> resultState;
            if (!isPreview) {
                resultState = updateBulkReleases(updatedReferencingReleaseList);
            } else {
                resultState = new HashMap<String, BulkOperationResultState>();
                for (Release updatedReferencingRelease : updatedReferencingReleaseList) {
                    resultState.put(updatedReferencingRelease.getId(), BulkOperationResultState.SUCCEEDED);
                }
            }
            for (String referencingReleaseId : resultState.keySet()) {
                BulkOperationResultState state = resultState.get(referencingReleaseId);
                if (state != BulkOperationResultState.SUCCEEDED) {
                    Map<String, ReleaseRelationship> referencedReleaseMap = previousRelationshipMap.get(referencingReleaseId);
                    //exclude from the list of candidates for deletion
                    for (String referencedReleaseId : referencedReleaseMap.keySet()) {
                        if (deletedLeafReleaseIds.contains(referencedReleaseId)) {
                            deletedLeafReleaseIds.remove(referencedReleaseId);
                            log.error(String.format("Failed to update the referencing release %s, so Release %s is excluded from bulk deletion.", referencingReleaseId, referencedReleaseId));
                            
                            //update the result state of this release
                            resultStateMap.put(referencedReleaseId, BulkOperationResultState.FAILED);
                        }
                    }
                    //restore work tree
                    Release referencingRelease = workReleaseMap.get(referencingReleaseId);
                    referencingRelease.setReleaseIdToRelationship(referencedReleaseMap);
                }
            }
            
            //update components
            Map<String, Set<String>> previousComponentReleaseIdsMap = new HashMap<String, Set<String>>();
            Set<String> updatedComponentIds = new HashSet<String>();
            for (String deletedLeafReleaseId : deletedLeafReleaseIds) {
                //get the component of this release
                Release deletedLeafRelease = workReleaseMap.get(deletedLeafReleaseId);
                String componentId = deletedLeafRelease.getComponentId();
                assertTrue(workComponentMap.containsKey(componentId));
                Component component = workComponentMap.get(componentId);
                //backup current relationship
                previousComponentReleaseIdsMap.put(componentId, getDuplicatedComponentReleaseIds(component));
                //remove the release id from ReleaseIds of the component
                Set<String> releaseIds = component.getReleaseIds();
                releaseIds.remove(deletedLeafReleaseId);
                updatedComponentIds.add(componentId);
            }
            List<Component> updatedComponentList = updatedComponentIds.stream().map(id -> workComponentMap.get(id)).collect(Collectors.toList());
            if (inspector != null) inspector.checkUpdatedComponentListInLoop(loopCount, updatedComponentList);
            
            if (!isPreview) {
                resultState = updateBulkComponent(updatedComponentList);
            } else {
                resultState = new HashMap<String, BulkOperationResultState>();
                for (Component updatedComponent : updatedComponentList) {
                    resultState.put(updatedComponent.getId(), BulkOperationResultState.SUCCEEDED);
                }
            }
            for (String componentId : resultState.keySet()) {
                BulkOperationResultState state = resultState.get(componentId);
                if (state != BulkOperationResultState.SUCCEEDED) {
                    assertTrue(workComponentMap.containsKey(componentId));
                    Component component = workComponentMap.get(componentId);
                    Set<String> componentReleaseIds = previousComponentReleaseIdsMap.get(componentId);
                    //exclude from the list of candidates for deletion
                    for (String componentReleaseId : componentReleaseIds) {
                        if (deletedLeafReleaseIds.contains(componentReleaseId)) {
                            deletedLeafReleaseIds.remove(componentReleaseId);	
                            log.error(String.format("Failed to update the component %s, so Release %s is excluded from bulk deletion.", componentId, componentReleaseId));
                            
                            //update the result state of this release
                            resultStateMap.put(componentReleaseId, BulkOperationResultState.FAILED);
                            //restore referencing releases
                            List<String> referencingReleaseIdList = referencingReleaseIdsMap.get(componentReleaseId);
                            for (String referencingReleaseId : referencingReleaseIdList) {
                                Release referencingRelease = workReleaseMap.get(referencingReleaseId);
                                referencingRelease.putToReleaseIdToRelationship(componentReleaseId, ReleaseRelationship.CONTAINED);
                                if (!isPreview) {
                                    releaseRepository.update(referencingRelease);
                                }
                            }
                        }
                    }
                    //restore work tree
                    component.setReleaseIds(componentReleaseIds);
                }
            }
            
            //delete releases in bulk
            List<Release> deletedReleaseList = deletedLeafReleaseIds.stream().map(id -> workReleaseMap.get(id)).collect(Collectors.toList());
            if (inspector != null) inspector.checkDeletedReleaseListInLoop(loopCount, deletedReleaseList);
            
            if (!isPreview) {
                resultState = deleteBulkRelease(deletedReleaseList);
            } else {
                resultState = new HashMap<String, BulkOperationResultState>();
                for (Release deletedRelease : deletedReleaseList) {
                    resultState.put(deletedRelease.getId(), BulkOperationResultState.SUCCEEDED);
                }
            }
            for (String deletedReleaseId : resultState.keySet()) {
                BulkOperationResultState state = resultState.get(deletedReleaseId);
                if (state == BulkOperationResultState.SUCCEEDED) {
                    resultStateMap.put(deletedReleaseId, BulkOperationResultState.SUCCEEDED);
                    //remove the release from work tree
                    workReleaseMap.remove(deletedReleaseId);
                } else {
                    log.error(String.format("Failed to delete the release %s", deletedReleaseId));
                    
                    resultStateMap.put(deletedReleaseId, state);
                    //restore component link
                    Release deletedRelease = workReleaseMap.get(deletedReleaseId);
                    Component component = workComponentMap.get(deletedRelease.getComponentId());
                    component.addToReleaseIds(deletedReleaseId);
                    if (!isPreview) {
                        componentRepository.update(component);
                    }
                    //restore referencing releases
                    List<String> referencingReleaseIdList = referencingReleaseIdsMap.get(deletedReleaseId);
                    for (String referencingReleaseId : referencingReleaseIdList) {
                        Release referencingRelease = workReleaseMap.get(referencingReleaseId);
                        referencingRelease.putToReleaseIdToRelationship(deletedReleaseId, ReleaseRelationship.CONTAINED);
                        if (!isPreview) {
                            releaseRepository.update(referencingRelease);
                        }
                    }
                }
            }
            
            //update modration request
            if (!isPreview) {
                for (String deletedLeafReleaseId : deletedLeafReleaseIds) {
                    BulkOperationResultState state = resultState.get(deletedLeafReleaseId);
                    if (state == BulkOperationResultState.SUCCEEDED) {
                        componentModerator.notifyModeratorOnDelete(deletedLeafReleaseId);
                    }
                }
            }
        }
        if (!isCompleted) {
            throw fail(500, "Unexpected loop processing was detected.");
        }

        //delete components have no release
        List<Component> deletedComponentList = new ArrayList<Component>();
        List<Component> updatedComponentList = new ArrayList<Component>();
        for (String componentId : workComponentMap.keySet()) {
            Component component = workComponentMap.get(componentId);
            Set<String> releaseIds = component.getReleaseIds();
            if (releaseIds.size() == 0) {
                deletedComponentList.add(component);
            } else {
                updatedComponentList.add(component);
            }
        }
        
        Map<String, BulkOperationResultState> resultState;
        if (!isPreview) {
            List<Component> bulkComponentList = new ArrayList<Component>();
            for (Component deletedComponent : deletedComponentList) {
                try {
                    attachmentConnector.deleteAttachments(deletedComponent.getAttachments());
                    attachmentDatabaseHandler.deleteUsagesBy(Source.componentId(deletedComponent.getId()));
                    bulkComponentList.add(deletedComponent);
                } catch (Exception ex) {
                    resultStateMap.put(deletedComponent.getId(), BulkOperationResultState.FAILED);
                    log.error(String.format("failed to delete the component attachment, component id=%s", deletedComponent.getId()), ex);
                }
            }
            resultState = deleteBulkComponent(bulkComponentList);
        } else {
            resultState = new HashMap<String, BulkOperationResultState>();
            for (Component deletedComponent : deletedComponentList) {
                resultState.put(deletedComponent.getId(), BulkOperationResultState.SUCCEEDED);
            }
        }
        for (String componentId : resultState.keySet()) {
            BulkOperationResultState state = resultState.get(componentId);
            if (state == BulkOperationResultState.SUCCEEDED) {
                resultStateMap.put(componentId, BulkOperationResultState.SUCCEEDED);
                workComponentMap.remove(componentId);
                if (!isPreview) {
                    componentModerator.notifyModeratorOnDelete(componentId);
                }
            } else {
                resultStateMap.put(componentId, state);
            }
        }
        
        //update ramained components
        if (!isPreview) {
            for (Component updatedComponent : updatedComponentList) {
                componentDatabaseHandler.updateReleaseDependentFieldsForComponentId(updatedComponent.getId(), user);
            }
        }
        for (String componentId : workComponentMap.keySet()) {
            if (!resultStateMap.containsKey(componentId)) {
                resultStateMap.put(componentId, BulkOperationResultState.EXCLUDED);
            }
        }
        
        //update result state for remained releases
        for (String linkedReleaseId : allLinkedReleaseIds) {
            if (!resultStateMap.containsKey(linkedReleaseId)) {
                if (workReleaseMap.containsKey(linkedReleaseId))  {
                    resultStateMap.put(linkedReleaseId, BulkOperationResultState.FAILED);
                } else {
                    log.error(String.format("The status of this release is indeterminate. id=%s", linkedReleaseId));
                }
            }
        }
        
        //add change log
        if (!isPreview) {
            // add release change logs
            for (String linkedReleaseId : allLinkedReleaseIds) {
                if (!workReleaseMap.containsKey(linkedReleaseId)) {
                    //deleted
                    Release oldRelease = allLinkedReleaseMap.get(linkedReleaseId);
                    dbHandlerUtil.addChangeLogs(null, oldRelease, user.getEmail(), Operation.DELETE, attachmentConnector,
                            Lists.newArrayList(), null, null);
                } else {
                    Release oldRelease = allLinkedReleaseMap.get(linkedReleaseId);
                    Release newRelease = workReleaseMap.get(linkedReleaseId);
                    if (!oldRelease.equals(newRelease)) {
                        //update
                        dbHandlerUtil.addChangeLogs(newRelease, oldRelease, user.getEmail(), Operation.UPDATE, attachmentConnector, 
                                Lists.newArrayList(), null, null);
                    }
                }
            }
            // add component change logs
            for (String componentId : allComponentMap.keySet()) {
                if (!workComponentMap.containsKey(componentId)) {
                    //deleted
                    Component oldComponent = allComponentMap.get(componentId);
                    dbHandlerUtil.addChangeLogs(null, oldComponent, user.getEmail(), Operation.DELETE, attachmentConnector,
                            Lists.newArrayList(), null, null);
                } else {
                    Component oldComponent = allComponentMap.get(componentId);
                    Component newComponent = workComponentMap.get(componentId);
                    if (!oldComponent.equals(newComponent)) {
                        //update
                        dbHandlerUtil.addChangeLogs(newComponent, oldComponent, user.getEmail(), Operation.UPDATE, attachmentConnector, 
                                Lists.newArrayList(), null, null);
                    }
                }
            }
        }
        
        //create result data
        BulkOperationNode rootNode = createBulkOperationNodeTree(
                releaseId, BulkOperationNodeType.RELEASE, null, allLinkedReleaseMap, allComponentMap, resultStateMap);
        
        return rootNode;
    }
    
    public Map<String, ReleaseRelationship> getDuplicatedReleaseIdToRelationship(Release release) throws SW360Exception {
        Map<String, ReleaseRelationship> relationship = release.getReleaseIdToRelationship(); 
        if (!CommonUtils.isNullOrEmptyMap(relationship)) {
            Map<String, ReleaseRelationship> newReleaseIdToRelationship = new HashMap<String, ReleaseRelationship>();
            for (String releaseId : relationship.keySet()){
                ReleaseRelationship releaseRelationship = relationship.get(releaseId);
                newReleaseIdToRelationship.put(releaseId, releaseRelationship);
            }
            return newReleaseIdToRelationship;
        } else {
            throw fail(500, "Unexpected ReleaseRelationship.");                
        }
    }
    
    public Set<String> getDuplicatedComponentReleaseIds(Component component) throws SW360Exception {
        Set<String> releaseIds = component.getReleaseIds(); 
        if (!CommonUtils.isNullOrEmptyCollection(releaseIds)) {
            return new HashSet<String>(releaseIds);
        } else {
            throw fail(500, "Unexpected ReleaseRelationship.");                
        }
    }
    
    public void deleteReleaseAttachments(Release release) throws SW360Exception {
        attachmentConnector.deleteAttachments(release.getAttachments());
        attachmentDatabaseHandler.deleteUsagesBy(Source.releaseId(release.getId()));
    }
        
    public Map<String, BulkOperationResultState> deleteBulkRelease(List<Release> releaseList) {
        Map<String, BulkOperationResultState> resultState = new HashMap<String, BulkOperationResultState>();
        
        List<Document> documentList = new ArrayList<Document>();
        for(Release release : releaseList) {
            Document document = new Document();
            document.setId(release.getId());
            document.setRevision(release.getRevision());
            document.setDeleted(true);
            documentList.add(document);
        }
        List<Response> responseList = releaseRepository.executeBulk(documentList);
        for (Response response : responseList) {
            String documentId = response.getId();
            String error = response.getError();
            if (CommonUtils.isNullEmptyOrWhitespace(error)) {
                resultState.put(documentId, BulkOperationResultState.SUCCEEDED);
            } else if (error.equals(CONFLICT_ERROR)) {
                resultState.put(documentId, BulkOperationResultState.CONFLICTED);
            } else {
                resultState.put(documentId, BulkOperationResultState.FAILED);
            }
        }
        return resultState;
    }

    public Map<String, BulkOperationResultState> updateBulkReleases(Collection<Release> collection) {
        Map<String, BulkOperationResultState> resultState = new HashMap<String, BulkOperationResultState>();
        List<Response> responseList = releaseRepository.executeBulk(collection);
        for (Response response : responseList) {
            String documentId = response.getId();
            String error = response.getError();
            if (CommonUtils.isNullEmptyOrWhitespace(error)) {
                resultState.put(documentId, BulkOperationResultState.SUCCEEDED);
            } else if (error.equals(CONFLICT_ERROR)) {
                resultState.put(documentId, BulkOperationResultState.CONFLICTED);
            } else {
                resultState.put(documentId, BulkOperationResultState.FAILED);
            }
        }
        return resultState;
    }

    public Map<String, BulkOperationResultState> deleteBulkComponent(List<Component> componentList) {
        Map<String, BulkOperationResultState> resultState = new HashMap<String, BulkOperationResultState>();
        
        List<Document> documentList = new ArrayList<Document>();
        for(Component component : componentList) {
            Document document = new Document();
            document.setId(component.getId());
            document.setRevision(component.getRevision());
            document.setDeleted(true);
            documentList.add(document);
        }
        List<Response> responseList = releaseRepository.executeBulk(documentList);
        for (Response response : responseList) {
            String documentId = response.getId();
            String error = response.getError();
            if (CommonUtils.isNullEmptyOrWhitespace(error)) {
                resultState.put(documentId, BulkOperationResultState.SUCCEEDED);
            } else if (error.equals(CONFLICT_ERROR)) {
                resultState.put(documentId, BulkOperationResultState.CONFLICTED);
            } else {
                resultState.put(documentId, BulkOperationResultState.FAILED);
            }
        }
        return resultState;
    }

    public Map<String, BulkOperationResultState> updateBulkComponent(Collection<Component> collection) {
        Map<String, BulkOperationResultState> resultState = new HashMap<String, BulkOperationResultState>();
        List<Response> responseList = componentRepository.executeBulk(collection);
        for (Response response : responseList) {
            String documentId = response.getId();
            String error = response.getError();
            if (CommonUtils.isNullEmptyOrWhitespace(error)) {
                resultState.put(documentId, BulkOperationResultState.SUCCEEDED);
            } else if (error.equals(CONFLICT_ERROR)) {
                resultState.put(documentId, BulkOperationResultState.CONFLICTED);
            } else {
                resultState.put(documentId, BulkOperationResultState.FAILED);
            }
        }
        return resultState;
    }
    
    public void getAllLinkedReleaseMap(String releaseId, Map<String, Release> outMap) throws SW360Exception {
        if (outMap.containsKey(releaseId)) {
            return;
        }
        Release release = releaseRepository.get(releaseId);
        assertNotNull(release, "Could not find release to update getAllLinkedReleaseMap!");
        outMap.put(release.getId(), release);
        if (CommonUtils.isNullOrEmptyMap(release.releaseIdToRelationship)) {
            return;
        }
        for (String linkedReleaseId : release.releaseIdToRelationship.keySet()) {
            getAllLinkedReleaseMap(linkedReleaseId, outMap);
        }
    }

    public Map<String, Release> getDuplicatedReleaseMap(Map<String, Release> releaseMap) {
        Map<String, Release> resultMap = new HashMap<String, Release>();
        for (Map.Entry<String, Release> entry : releaseMap.entrySet()) {
            String releaseId = entry.getKey();
            Release release = entry.getValue().deepCopy();
            resultMap.put(releaseId, release);
        }
        return resultMap;
    }
    
    public Map<String, Component> getDuplicatedComponentMap(Map<String, Component> componentMap) {
        Map<String, Component> resultMap = new HashMap<String, Component>();
        for (Map.Entry<String, Component> entry : componentMap.entrySet()) {
            String componentId = entry.getKey();
            Component component = entry.getValue().deepCopy();
            resultMap.put(componentId, component);
        }
        return resultMap;
    }    
    
    public void getExternalLinkMap(Set<String> allLinkedReleaseIds, Map<String, Boolean> outExternalFlagMap, Map<String, List<String>> outReferencingReleaseIdsMap) {
        Map<String, Boolean> cacheMap = new HashMap<String, Boolean>();
        for (String releaseId : allLinkedReleaseIds) {
            Boolean result = checkReleaseHasExternalLink(releaseId, allLinkedReleaseIds, outReferencingReleaseIdsMap, cacheMap);
            outExternalFlagMap.put(releaseId, result);
        }
    }
    
    public boolean checkReleaseHasExternalLink(String releaseId, Set<String> allLinkedReleaseIds, Map<String, List<String>> outReferencingReleaseIdsMap, Map<String, Boolean> cacheMap) {
        if (cacheMap.containsKey(releaseId)) {
            return cacheMap.get(releaseId);
        }
        List<Release> referencingReleaseList = releaseRepository.getReferencingReleases(releaseId);
        outReferencingReleaseIdsMap.put(releaseId, referencingReleaseList.stream().map(r -> r.getId()).collect(Collectors.toList()));
        
        Set<Project> referencingProjects = projectRepository.searchByReleaseId(releaseId);
        if (CommonUtils.isNotEmpty(referencingProjects)) {
            cacheMap.put(releaseId, true);
            return true;
        }
        
        if (CommonUtils.isNotEmpty(referencingReleaseList)) {
            for (Release referencingRelase : referencingReleaseList) {
                String referencingRelaseId = referencingRelase.getId();
                if (allLinkedReleaseIds.contains(referencingRelaseId)) {
                    if (checkReleaseHasExternalLink(referencingRelaseId, allLinkedReleaseIds, outReferencingReleaseIdsMap, cacheMap)) {
                        cacheMap.put(releaseId, true);
                        return true;
                    }
                } else {
                    cacheMap.put(releaseId, true);
                    return true;
                }
            }
        } else {
            cacheMap.put(releaseId, false);
            return false;
        }
        
        cacheMap.put(releaseId, false);
        return false;
    }
    
    public Map<String, Component> getComponenMap(Map<String, Release> allLinkedReleaseMap){
        Map<String, Component> resultMap = new HashMap<String, Component>();
        for (Release release : allLinkedReleaseMap.values()) {
            String componentId = release.getComponentId();
            if (!resultMap.containsKey(componentId)) {
                Component component = componentRepository.get(componentId);
                resultMap.put(componentId, component);
            }
        }
        return resultMap;
    }
    
    public Set<String> getLeafReleaseIds(Map<String, Release> releaseMap) {
        Set<String> resultList = new HashSet<String>();
        for (Release release : releaseMap.values()) {
            if (CommonUtils.isNullOrEmptyMap(release.releaseIdToRelationship)) {
                resultList.add(release.getId());
            }
        }
        return resultList;
    }
    
    public BulkOperationNode createBulkOperationNodeTree(
            String documentId, 
            BulkOperationNodeType type,
            String parentId,
            Map<String, Release> allLinkedReleaseMap, 
            Map<String, Component> componentMap, 
            Map<String, BulkOperationResultState> resultStateMap) throws SW360Exception {
        
        if (type == BulkOperationNodeType.RELEASE) {
            //create release node
            String releaseId = documentId;
            if (!allLinkedReleaseMap.containsKey(releaseId)) {
                throw fail(500, "Unexpected node specification. release id " + releaseId);
            }
            Release release = allLinkedReleaseMap.get(releaseId);
            BulkOperationNode releaseNode = new BulkOperationNode(
                    releaseId, release.getName(), release.getVersion(), BulkOperationNodeType.RELEASE);
            if (!resultStateMap.containsKey(releaseId)) {
                throw fail(500, "Could not find BulkOperationResultState. release id " + releaseId);
            }
            releaseNode.setState(resultStateMap.get(releaseId));
            ArrayList<BulkOperationNode> releaseChildList = new ArrayList<BulkOperationNode>();
            if (!CommonUtils.isNullOrEmptyMap(release.getReleaseIdToRelationship())) {
                for (String linkedReleaseId : release.getReleaseIdToRelationship().keySet()) {
                    BulkOperationNode childNode = createBulkOperationNodeTree(
                            linkedReleaseId, BulkOperationNodeType.RELEASE, releaseId, allLinkedReleaseMap, componentMap, resultStateMap);
                    releaseChildList.add(childNode);
                }
            }
            releaseNode.setChildList(releaseChildList);
            
            //create component node
            String componentId = release.getComponentId();
            if (!componentMap.containsKey(componentId)) {
                throw fail(500, "Could not find the component. release id " + releaseId);
            }
            Component component = componentMap.get(componentId);
            BulkOperationNode componentNode = new BulkOperationNode(
                    componentId, component.getName(), "", BulkOperationNodeType.COMPONENT);
            if (!resultStateMap.containsKey(componentId)) {
                throw fail(500, "Could not find BulkOperationResultState. component id " + componentId);
            }
            componentNode.setState(resultStateMap.get(componentId));
            ArrayList<BulkOperationNode> componentChildList = new ArrayList<BulkOperationNode>();
            componentChildList.add(releaseNode);
            componentNode.setChildList(componentChildList);
            componentNode.setParentId(parentId);
            releaseNode.setParentId(componentId);
            return componentNode;
            
        } else {
            throw fail(500, "Unsupported data type " + type.toString());
        }
    }
    
    public void setInspector(BulkDeleteUtilInspector inspector) {
        this.inspector = inspector;
    }
    
    public void unsetInspector() {
        this.inspector = null;
    }
    
}

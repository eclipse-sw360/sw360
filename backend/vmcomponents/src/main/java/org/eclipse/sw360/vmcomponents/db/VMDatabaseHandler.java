/*
SPDX-FileCopyrightText: © 2022 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.vmcomponents.db;

import com.ibm.cloud.cloudant.v1.Cloudant;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.thrift.TBase;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.vmcomponents.common.SVMMapper;
import org.eclipse.sw360.vulnerabilities.db.VulnerabilityDatabaseHandler;

import java.net.MalformedURLException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Class for accessing the CouchDB database
 *
 * @author stefan.jaeger@evosoft.com
 */
public class VMDatabaseHandler extends VulnerabilityDatabaseHandler {

    private static final Logger log = Logger.getLogger(VMDatabaseHandler.class);

    /**
     * Connection to the couchDB database
     */
    private VMComponentRepository compRepo;
    private VMActionRepository actionRepo;
    private VMPriorityRepository prioRepo;
    private VMProcessReportingRepository processRepo;
    private VMMatchRepository matchRepo;

    public VMDatabaseHandler() throws MalformedURLException {
        this(DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_VM);
    }

    public VMDatabaseHandler(Cloudant client, String dbName) throws MalformedURLException {
        super(client, dbName);
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(client, dbName);
        compRepo = new VMComponentRepository(db);
        actionRepo = new VMActionRepository(db);
        prioRepo = new VMPriorityRepository(db);
        processRepo = new VMProcessReportingRepository(db);
        matchRepo = new VMMatchRepository(db);
    }

    public <T extends TBase> RequestStatus add(T element){
        if (element == null){
            log.error("cannot add null element");
            return RequestStatus.FAILURE;
        }
        log.debug("adding element "+element.toString());
        try {
            if (VMComponent.class.isAssignableFrom(element.getClass()))
                compRepo.add((VMComponent) element);
            else if (VMAction.class.isAssignableFrom(element.getClass()))
                actionRepo.add((VMAction) element);
            else if (VMPriority.class.isAssignableFrom(element.getClass()))
                prioRepo.add((VMPriority) element);
            else if (VMProcessReporting.class.isAssignableFrom(element.getClass()))
                processRepo.add((VMProcessReporting) element);
            else if (VMMatch.class.isAssignableFrom(element.getClass()))
                matchRepo.add((VMMatch) element);
            else
                super.add(element);
            return RequestStatus.SUCCESS;
        } catch (Exception e) {
            log.error("error on adding "+element.getClass().getSimpleName()+": "+e.getMessage(), e);
            return RequestStatus.FAILURE;
        }
    }

    public <T extends TBase> RequestStatus add(Class<T> type, Collection<T> elements) {
        if (type == null || elements == null) {
            log.error("type/elements cannot be null");
            return RequestStatus.FAILURE;
        }
        try {
            log.debug("adding "+elements.size()+" elements via bulk");
            if (VMComponent.class.isAssignableFrom(type)) {
                compRepo.executeBulk(elements);
            } else if (VMAction.class.isAssignableFrom(type)) {
                actionRepo.executeBulk(elements);
            } else if (VMPriority.class.isAssignableFrom(type)) {
                prioRepo.executeBulk(elements);
            } else if (VMProcessReporting.class.isAssignableFrom(type)) {
                processRepo.executeBulk(elements);
            } else
                super.add(type, elements);

            log.debug("adding "+elements.size()+" elements via bulk finished");
            return RequestStatus.SUCCESS;
        } catch (Exception e) {
            log.error("error on bulk updating " + type.getSimpleName() + ": " + e.getMessage(), e);
            return RequestStatus.FAILURE;
        }
    }

    public <T extends TBase> RequestStatus update(T element){
        if (element == null){
            log.error("cannot update null element");
            return RequestStatus.FAILURE;
        }
        try {
            if (VMComponent.class.isAssignableFrom(element.getClass())){
                compRepo.update(SVMMapper.setLastUpdate((VMComponent) element));
            } else if (VMAction.class.isAssignableFrom(element.getClass())){
                actionRepo.update(SVMMapper.setLastUpdate((VMAction) element));
            } else if (VMPriority.class.isAssignableFrom(element.getClass())){
                prioRepo.update(SVMMapper.setLastUpdate((VMPriority) element));
            } else if (VMProcessReporting.class.isAssignableFrom(element.getClass())){
                processRepo.update((VMProcessReporting) element);
            } else if (VMMatch.class.isAssignableFrom(element.getClass())){
                matchRepo.update((VMMatch) element);
            } else
                super.update(element);

            return RequestStatus.SUCCESS;
        } catch (Exception e) {
            log.error("error on updating "+element.getClass().getSimpleName()+": "+e.getMessage(), e);
            return RequestStatus.FAILURE;
        }
    }

    public <T extends TBase> RequestStatus delete(T element){
        if (element == null){
            log.error("cannot remove null element");
            return RequestStatus.FAILURE;
        }
        try {
            if (VMComponent.class.isAssignableFrom(element.getClass()))
                compRepo.remove((VMComponent) element);
            else if (VMAction.class.isAssignableFrom(element.getClass()))
                actionRepo.remove((VMAction) element);
            else if (VMPriority.class.isAssignableFrom(element.getClass()))
                prioRepo.remove((VMPriority) element);
            else if (VMProcessReporting.class.isAssignableFrom(element.getClass()))
                processRepo.remove((VMProcessReporting) element);
            else if (VMMatch.class.isAssignableFrom(element.getClass()))
                matchRepo.remove((VMMatch) element);
            else
                super.delete(element);

            return RequestStatus.SUCCESS;
        } catch (Exception e) {
            log.error("error on removing "+element.getClass().getSimpleName()+": "+e.getMessage(), e);
            return RequestStatus.FAILURE;
        }
    }

    public <T extends TBase> List<T> getAll(Class<T> type){
        if (type == null){
            log.error("type cannot be null");
            return null;
        }
        if (VMComponent.class.isAssignableFrom(type))
            return (List<T>) compRepo.getAll();
        else if (VMAction.class.isAssignableFrom(type))
            return (List<T>) actionRepo.getAll();
        else if (VMPriority.class.isAssignableFrom(type))
            return (List<T>) prioRepo.getAll();
        else if (VMProcessReporting.class.isAssignableFrom(type))
            return (List<T>) processRepo.getAll();
        else if (VMMatch.class.isAssignableFrom(type))
            return (List<T>) matchRepo.getAll();
        else
            return super.getAll(type);
    }

    public <T extends TBase> Set<String> getAllIds(Class<T> type){
        if (type == null){
            log.error("type cannot be null");
            return null;
        }
        if (VMComponent.class.isAssignableFrom(type))
            return compRepo.getAllIds();
        else if (VMAction.class.isAssignableFrom(type))
            return actionRepo.getAllIds();
        else if (VMPriority.class.isAssignableFrom(type))
            return prioRepo.getAllIds();
        else if (VMProcessReporting.class.isAssignableFrom(type))
            return processRepo.getAllIds();
        else if (VMMatch.class.isAssignableFrom(type))
            return matchRepo.getAllIds();
        else
            return super.getAllIds(type);
    }

    public <T extends TBase> T getById(Class<T> type, String id){
        if (type == null || StringUtils.isEmpty(id)){
            log.error("type/id cannot be null "+type+" "+id);
            return null;
        }
        if (VMComponent.class.isAssignableFrom(type))
            return (T) compRepo.get(id);
        else if (VMAction.class.isAssignableFrom(type))
            return (T) actionRepo.get(id);
        else if (VMPriority.class.isAssignableFrom(type))
            return (T) prioRepo.get(id);
        else if (VMProcessReporting.class.isAssignableFrom(type))
            return (T) processRepo.get(id);
        else if (VMMatch.class.isAssignableFrom(type))
            return (T) matchRepo.get(id);
        else
            return super.getById(type, id);
    }

    public <T extends TBase> Set<String> getAllVmIds(Class<T> type){
        if (type == null){
            log.error("type cannot be null");
            return null;
        }
        if (VMComponent.class.isAssignableFrom(type))
            return compRepo.getAllVmids();
        else if (VMAction.class.isAssignableFrom(type))
            return actionRepo.getAllVmids();
        else if (VMPriority.class.isAssignableFrom(type))
            return prioRepo.getAllVmids();
        else
            return super.getAllExternalIds(type);
    }

    public VMMatch getMatchByIds(String releaseId, String vmComponentId){
        if (StringUtils.isEmpty(releaseId) || StringUtils.isEmpty(vmComponentId)){
            log.error("releaseId/vmComponentId cannot be null "+releaseId+" "+vmComponentId);
            return null;
        }
        return matchRepo.getMatchByIds(releaseId, vmComponentId);
    }

    public Set<String> getComponentIdsByNamePrefixCaseInsensitive(String namePrefix){
        return compRepo.getComponentByLowercaseNamePrefix(namePrefix);
    }

    public Set<String> getComponentIdsByVendorPrefixCaseInsensitive(String vendorPrefix){
        return compRepo.getComponentByLowercaseVendorPrefix(vendorPrefix);
    }

    public Set<String> getComponentIdsByVersionPrefixCaseInsensitive(String versionPrefix){
        return compRepo.getComponentByLowercaseVersionPrefix(versionPrefix);
    }

    public List<VMMatch> getMatchesByComponentIds(Collection<String> componentIds){
        if (componentIds == null || componentIds.isEmpty()){
            log.error("componentIds cannot be null/empty");
            return null;
        }
        return matchRepo.getMatchesByComponentIds(componentIds);
    }

    public <T extends TBase> T getByVmId(Class<T> type, String vmid){
        if (type == null || StringUtils.isEmpty(vmid)){
            log.error("type/vmid cannot be null "+type+" "+vmid);
            return null;
        }
        if (VMComponent.class.isAssignableFrom(type))
            return (T) compRepo.getComponentByVmid(vmid);
        else if (VMAction.class.isAssignableFrom(type))
            return (T) actionRepo.getActionByVmid(vmid);
        else if (VMPriority.class.isAssignableFrom(type))
            return (T) prioRepo.getPriorityByVmid(vmid);
        else
            return super.getByExternalId(type, vmid);
    }

    public <T extends TBase> T getByCreationDate(Class<T> type, String creationDate){
        if (type == null || StringUtils.isEmpty(creationDate)){
            log.error("type/creationDate cannot be null "+type+" "+creationDate);
            return null;
        }
        if (VMProcessReporting.class.isAssignableFrom(type))
            return (T) processRepo.getProcessReportingByStartDate(creationDate);
        else
            throw new IllegalArgumentException("unknown type "+ type.getSimpleName());

    }

    public VMProcessReporting getLastSuccessfulProcessByElementType(String elementType) {
        return processRepo.getLastSuccessfulProcessByElementType(elementType);
    }

    public <T extends TBase> T getLastUpdated(Class<T> type){
        if (type == null){
            log.error("type cannot be null");
            return null;
        }
        if (VMComponent.class.isAssignableFrom(type))
            return (T) compRepo.getComponentByLastUpdate(null);
        else if (VMAction.class.isAssignableFrom(type))
            return (T) actionRepo.getActionByLastUpdate(null);
        else if (VMPriority.class.isAssignableFrom(type))
            return (T) prioRepo.getPriorityByLastUpdate(null);
        else
            return super.getLastUpdated(type, 1).get(0);
    }

}

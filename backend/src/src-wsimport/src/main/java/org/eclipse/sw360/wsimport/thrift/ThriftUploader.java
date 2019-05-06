/*
 * Copyright (c) Bosch Software Innovations GmbH 2017.
 * With modifications by Verifa Oy, 2018-2019.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.wsimport.thrift;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonSyntaxException;
import org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship;
import org.eclipse.sw360.wsimport.domain.*;
import org.eclipse.sw360.wsimport.entitytranslation.WsLibraryToSw360ComponentTranslator;
import org.eclipse.sw360.wsimport.entitytranslation.WsLibraryToSw360ReleaseTranslator;
import org.eclipse.sw360.wsimport.entitytranslation.WsLicenseToSw360LicenseTranslator;
import org.eclipse.sw360.wsimport.entitytranslation.WsProjectToSw360ProjectTranslator;
import org.eclipse.sw360.wsimport.entitytranslation.helper.ReleaseRelation;
import org.eclipse.sw360.wsimport.rest.WsImportService;
import org.eclipse.sw360.wsimport.thrift.helper.ProjectImportError;
import org.eclipse.sw360.wsimport.thrift.helper.ProjectImportResult;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.importstatus.ImportStatus;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.projectimport.TokenCredentials;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.eclipse.sw360.wsimport.utility.TranslationConstants.UNKNOWN;

/**
 * @author: ksoranko@verifa.io
 */
public class ThriftUploader {

    private static final Logger LOGGER = Logger.getLogger(ThriftUploader.class);
    private final WsLibraryToSw360ComponentTranslator libraryToComponentTranslator = new WsLibraryToSw360ComponentTranslator();
    private final WsLibraryToSw360ReleaseTranslator libraryToReleaseTranslator = new WsLibraryToSw360ReleaseTranslator();
    private final WsLicenseToSw360LicenseTranslator licenseToLicenseTranslator = new WsLicenseToSw360LicenseTranslator();
    private final WsProjectToSw360ProjectTranslator projectToProjectTranslator = new WsProjectToSw360ProjectTranslator();

    private ThriftExchange thriftExchange;

    public ThriftUploader() {
        this.thriftExchange = new ThriftExchange();
    }

    private <T> Optional<String> searchExistingEntityId(Optional<List<T>> nomineesOpt, Function<T, String> idExtractor, String wsName, String sw360name) {
        return nomineesOpt.flatMap(
                nominees -> {
                    Optional<String> nomineeId = nominees.stream()
                            .findFirst()
                            .map(idExtractor);
                    if (nomineeId.isPresent()) {
                        LOGGER.info(wsName + " to import matches a " + sw360name + " with id: " + nomineeId.get());
                        nominees.stream()
                                .skip(1)
                                .forEach(n -> LOGGER.error(wsName + " to import would also match a " + sw360name + " with id: " + idExtractor.apply(n)));
                    }
                    return nomineeId;
                }
        );
    }

    protected ProjectImportResult createProject(WsProject wsProject, User sw360User, TokenCredentials tokenCredentials) throws TException, JsonSyntaxException {
        LOGGER.info("Try to import whitesource project: " + wsProject.getProjectName());
        LOGGER.info("Sw360-User: " + sw360User.email);

        LOGGER.info("projectName and token and id: " + wsProject.getProjectName() + " " + wsProject.getProjectToken() + " " + wsProject.getId());
        if (wsProject.getProjectName() == null || wsProject.getProjectToken() == null) {
            LOGGER.error("Unable to get project: " + wsProject.getProjectName() + " with token: " + wsProject.getProjectToken() + " from whitesource!");
            return new ProjectImportResult(ProjectImportError.PROJECT_NOT_FOUND);
        }

        if (thriftExchange.projectExists(wsProject.getId(), wsProject.getProjectName(), sw360User)) {
            LOGGER.error("Project already in database: " + wsProject.getProjectName());
            return new ProjectImportResult(ProjectImportError.PROJECT_ALREADY_EXISTS);
        }

        Project sw360Project = projectToProjectTranslator.apply(wsProject);
        Set<ReleaseRelation> releases = createReleases(wsProject, sw360User, tokenCredentials);
        sw360Project.setProjectResponsible(sw360User.getEmail());

        /*
         * TODO: Improve duplicate handling
         */
        Map<String, ProjectReleaseRelationship> releaseIdToUsage =
                releases.stream()
                    .collect(Collectors.toMap(
                            ReleaseRelation::getReleaseId,
                            ReleaseRelation::getProjectReleaseRelationship,
                            (projectReleaseRelationship1, projectReleaseRelationship2) -> {
                                LOGGER.info("--- Duplicate key found!");
                                LOGGER.info("--- 1: " + projectReleaseRelationship1.getReleaseRelation());
                                LOGGER.info("--- 2: " + projectReleaseRelationship2.getReleaseRelation());
                                return projectReleaseRelationship1;
                            }
                    ));
        sw360Project.setReleaseIdToUsage(releaseIdToUsage);
        String projectId = thriftExchange.addProject(sw360Project, sw360User);

        if(isNullOrEmpty(projectId)) {
            return new ProjectImportResult(ProjectImportError.OTHER);
        } else {
            return new ProjectImportResult(projectId);
        }
    }

    public ImportStatus importWsProjects(Collection<WsProject> wsProjects, User sw360User, TokenCredentials tokenCredentials) {
        List<String> successfulIds = new ArrayList<>();
        Map<String, String> failedIds = new HashMap<>();
        ImportStatus wsImportStatus = new ImportStatus().setRequestStatus(RequestStatus.SUCCESS);

        for (WsProject wsProject : wsProjects) {
            ProjectImportResult projectImportResult;
            try{
                projectImportResult = createProject(wsProject, sw360User, tokenCredentials);
            } catch (TException e){
                LOGGER.error("Error when creating the project", e);
                wsImportStatus.setRequestStatus(RequestStatus.FAILURE);
                return wsImportStatus;
            }
            if (projectImportResult.isSuccess()) {
                successfulIds.add(wsProject.getProjectName());
            } else {
                LOGGER.error("Could not import project with whitesource name: " + wsProject.getProjectName());
                failedIds.put(wsProject.getProjectName(), projectImportResult.getError().getText());
            }
        }
        return wsImportStatus
                .setFailedIds(failedIds)
                .setSuccessfulIds(successfulIds);
    }

    protected String getOrCreateLicenseId(WsLicense wsLicense, User sw360User) {
        LOGGER.info("Try to import whitesource License: " + wsLicense.getName());

        Optional<String> potentialLicenseId = searchExistingEntityId(thriftExchange.searchLicenseByWsName(wsLicense.getName()),
                License::getId,
                "License",
                "Licence");

        if (potentialLicenseId.isPresent()) {
            return potentialLicenseId.get();
        } else {
            License sw360License = licenseToLicenseTranslator.apply(wsLicense);
            String licenseId = thriftExchange.addLicense(sw360License, sw360User);
            LOGGER.info("Imported license: " + licenseId);
            return licenseId;
        }
    }

    private String getOrCreateComponent(WsLibrary wsLibrary, User sw360User) {
        LOGGER.info("Try to import whitesource Library: " + wsLibrary.getName() + ", version: " + wsLibrary.getVersion());

        String componentVersion = isNullOrEmpty(wsLibrary.getVersion()) ? UNKNOWN : wsLibrary.getVersion();
        Optional<String> potentialReleaseId = searchExistingEntityId(thriftExchange.searchReleaseByNameAndVersion(wsLibrary.getName(), componentVersion),
                Release::getId,
                "Library",
                "Release");
        if (potentialReleaseId.isPresent()) {
            return potentialReleaseId.get();
        }

        Release sw360Release = libraryToReleaseTranslator.apply(wsLibrary);
        sw360Release.setModerators(new HashSet<>());
        sw360Release.getModerators().add(sw360User.getEmail());
        Optional<String> potentialComponentId = searchExistingEntityId(thriftExchange.searchComponentByName(wsLibrary.getName()),
                Component::getId,
                "Library",
                "Component");

        String componentId;
        if (potentialComponentId.isPresent()) {
            componentId = potentialComponentId.get();
        } else {
            Component sw360Component = libraryToComponentTranslator.apply(wsLibrary);
            componentId = thriftExchange.addComponent(sw360Component, sw360User);
        }
        sw360Release.setComponentId(componentId);

        if (wsLibrary.getLicenses() == null) {
            sw360Release.setMainLicenseIds(Collections.singleton(UNKNOWN));
        } else {
            Set<String> mainLicenses = new HashSet<>();
            for (WsLicense wsLicense : wsLibrary.getLicenses()) {
                mainLicenses.add(getOrCreateLicenseId(wsLicense, sw360User));
            }
            sw360Release.setMainLicenseIds(mainLicenses);
        }

        return thriftExchange.addRelease(sw360Release, sw360User);
    }

    private ReleaseRelation createReleaseRelation(WsLibrary wsLibrary, User sw360User) {
        String releaseId = getOrCreateComponent(wsLibrary, sw360User);
        if (releaseId == null) {
            return null;
        } else {
            ReleaseRelationship releaseRelationship = ReleaseRelationship.UNKNOWN;
            return new ReleaseRelation(releaseId, releaseRelationship);
        }
    }

    private Set<ReleaseRelation> createReleases(WsProject wsProject, User sw360User, TokenCredentials tokenCredentials) {
        WsLibrary[] libraries = null;
        try {
            libraries =  new WsImportService().getProjectLicenses(wsProject.getProjectToken(), tokenCredentials);
        } catch (JsonSyntaxException jse) {
            LOGGER.error(jse);
        }
        List<WsLibrary> libraryList;
        if (libraries == null) {
            return ImmutableSet.of();
        } else {
            libraryList = new ArrayList<>(Arrays.asList(libraries));
        }
        Set<ReleaseRelation> releases = libraryList.stream()
                .map(c -> createReleaseRelation(c, sw360User))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (releases.size() != libraryList .size()) {
            LOGGER.warn("expected to get " + libraryList.size() + " different ids of releases but got " + releases.size());
        } else {
            LOGGER.info("The expected number of releases was imported or already found in database.");
        }

        return releases;
    }

}

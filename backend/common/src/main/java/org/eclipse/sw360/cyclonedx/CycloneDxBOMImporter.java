/*
 * Copyright Siemens Healthineers GmBH, 2023. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.cyclonedx;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.ExternalReference;
import org.cyclonedx.model.ExternalReference.Type;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.parsers.JsonParser;
import org.cyclonedx.parsers.Parser;
import org.cyclonedx.parsers.XmlParser;
import org.eclipse.sw360.commonIO.AttachmentFrontendUtils;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.common.ThriftEnumUtils;
import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.db.ComponentDatabaseHandler;
import org.eclipse.sw360.datahandler.db.PackageDatabaseHandler;
import org.eclipse.sw360.datahandler.db.ProjectDatabaseHandler;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestStatus;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.thrift.CycloneDxComponentType;
import org.eclipse.sw360.datahandler.thrift.MainlineState;
import org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.Visibility;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.attachments.CheckStatus;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.ComponentType;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.components.Repository;
import org.eclipse.sw360.datahandler.thrift.components.RepositoryType;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.datahandler.thrift.packages.PackageManager;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectType;
import org.eclipse.sw360.datahandler.thrift.users.User;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.github.jsonldjava.shaded.com.google.common.io.Files;
import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import com.google.common.net.MediaType;
import com.google.gson.Gson;

/**
 * CycloneDX BOM import implementation.
 * Supports both XML and JSON format of CycloneDX SBOM
 *
 * @author abdul.kapti@siemens-healthineers.com
 */
public class CycloneDxBOMImporter {
    private static final Logger log = LogManager.getLogger(CycloneDxBOMImporter.class);
    private static final String SCHEMA_PATTERN = ".+://(\\w*(?:[\\-@.\\\\s,_:/][/(.\\-)A-Za-z0-9]+)*)";
    private static final String DOT_GIT = ".git";
    private static final String SLASH = "/";
    private static final String DOT = ".";
    private static final String HYPHEN = "-";
    private static final String JOINER = "||";
    private static final String VCS_HTTP_REGEX = "^[^:]+://";
    private static final String COLON_REGEX = "[:,\\s]";
    private static final String HTTPS_SCHEME = "https://";
    private static final String COMP_CREATION_COUNT_KEY = "compCreationCount";
    private static final String COMP_REUSE_COUNT_KEY = "compReuseCount";
    private static final String REL_CREATION_COUNT_KEY = "relCreationCount";
    private static final String REL_REUSE_COUNT_KEY = "relReuseCount";
    private static final String PKG_CREATION_COUNT_KEY = "pkgCreationCount";
    private static final String PKG_REUSE_COUNT_KEY = "pkgReuseCount";
    private static final String DUPLICATE_COMPONENT = "dupComp";
    private static final String DUPLICATE_RELEASE = "dupRel";
    private static final String DUPLICATE_PACKAGE = "dupPkg";
    private static final String INVALID_COMPONENT = "invalidComp";
    private static final String INVALID_RELEASE = "invalidRel";
    private static final String INVALID_PACKAGE = "invalidPkg";
    private static final String PROJECT_ID = "projectId";
    private static final String PROJECT_NAME = "projectName";
    private static final boolean IS_PACKAGE_PORTLET_ENABLED = SW360Constants.IS_PACKAGE_PORTLET_ENABLED;
    private static final Predicate<ExternalReference.Type> typeFilter = type -> ExternalReference.Type.VCS.equals(type);

    private final ProjectDatabaseHandler projectDatabaseHandler;
    private final ComponentDatabaseHandler componentDatabaseHandler;
    private final PackageDatabaseHandler packageDatabaseHandler;
    private final User user;
    private final AttachmentConnector attachmentConnector;

    // Map of supported hosts and base URL formats
    private static final Map<String, String> VCS_HOSTS = Map.of(
            "github.com", "https://github.com/%s/%s",
            "gitlab.com", "https://gitlab.com/%s/%s",
            "bitbucket.org", "https://bitbucket.org/%s/%s",
            "cs.opensource.google", "https://cs.opensource.google/%s/%s",
            "go.googlesource.com", "https://go.googlesource.com/%s",
            "pypi.org", "https://pypi.org/project/%s"
    );

    public CycloneDxBOMImporter(ProjectDatabaseHandler projectDatabaseHandler, ComponentDatabaseHandler componentDatabaseHandler,
            PackageDatabaseHandler packageDatabaseHandler, AttachmentConnector attachmentConnector, User user) {
        this.projectDatabaseHandler = projectDatabaseHandler;
        this.componentDatabaseHandler = componentDatabaseHandler;
        this.packageDatabaseHandler = packageDatabaseHandler;
        this.attachmentConnector = attachmentConnector;
        this.user = user;
    }

    /**
     * Creating the Map of Sanitized VCS URLs to List of Component -> Grouping by VCS URLs
     * Sanitizing VCS URLs:
     *      git+https://github.com/microsoft/ApplicationInsights-JS.git/tree/master/shared/AppInsightsCommon
     *              --> microsoft.applicationinsights-js
     * @param components
     * @return Map<String, List<org.cyclonedx.model.Component>>
     */
    private Map<String, List<org.cyclonedx.model.Component>> getVcsToComponentMap(List<org.cyclonedx.model.Component> components) {
        return components.parallelStream().filter(Objects::nonNull)
                .flatMap(comp -> CommonUtils.nullToEmptyList(comp.getExternalReferences()).stream()
                        .filter(Objects::nonNull)
                        .filter(ref -> ExternalReference.Type.VCS.equals(ref.getType()))
                        .map(ExternalReference::getUrl)
                        .map(url -> sanitizeVCS(url.toLowerCase()))
                        .filter(url -> CommonUtils.isValidUrl(url))
                        .map(url -> new AbstractMap.SimpleEntry<>(url, comp)))
                .collect(Collectors.groupingBy(e -> e.getKey(),
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
    }

    @SuppressWarnings("unchecked")
    public RequestSummary importFromBOM(InputStream inputStream, AttachmentContent attachmentContent, String projectId, User user) {
        RequestSummary requestSummary = new RequestSummary();
        Map<String, String> messageMap = new HashMap<>();
        requestSummary.setRequestStatus(RequestStatus.FAILURE);
        String fileExtension = Files.getFileExtension(attachmentContent.getFilename());
        Parser parser;

        if (!SW360Utils.isUserAtleastDesiredRoleInPrimaryOrSecondaryGroup(user, SW360Constants.SBOM_IMPORT_EXPORT_ACCESS_USER_ROLE)) {
            log.warn("User does not have permission to import the SBOM: " + user.getEmail());
            requestSummary.setMessage(SW360Constants.SBOM_IMPORT_EXPORT_ACCESS_USER_ROLE.name());
            requestSummary.setRequestStatus(RequestStatus.ACCESS_DENIED);
            return requestSummary;
        }

        if (fileExtension.equalsIgnoreCase(SW360Constants.XML_FILE_EXTENSION)) {
            parser = new XmlParser();
        } else if (fileExtension.equalsIgnoreCase(SW360Constants.JSON_FILE_EXTENSION)) {
            parser = new JsonParser();
        } else {
            requestSummary.setMessage(String.format("Invalid file format <b>%s</b>. Only XML & JSON SBOM are supported by CycloneDX!", fileExtension));
            return requestSummary;
        }

        try {
            // parsing the input stream into CycloneDx org.cyclonedx.model.Bom
            Bom bom = parser.parse(IOUtils.toByteArray(inputStream));
            Metadata bomMetadata = bom.getMetadata();

            // Getting List of org.cyclonedx.model.Component from the Bom
            List<org.cyclonedx.model.Component> components = CommonUtils.nullToEmptyList(bom.getComponents());

            long vcsCount = getVcsToComponentMap(components).size();
            long componentsCount = components.size();
            org.cyclonedx.model.Component compMetadata = bomMetadata.getComponent();
            Map<String, List<org.cyclonedx.model.Component>> vcsToComponentMap = new HashMap<>();

            if (!IS_PACKAGE_PORTLET_ENABLED) {
                vcsToComponentMap.put("", components);
                requestSummary = importSbomAsProject(compMetadata, vcsToComponentMap, projectId, attachmentContent);
            } else {
                vcsToComponentMap = getVcsToComponentMap(components);
                if (componentsCount == vcsCount) {

                    requestSummary = importSbomAsProject(compMetadata, vcsToComponentMap, projectId, attachmentContent);
                } else if (componentsCount > vcsCount) {

                    requestSummary = importSbomAsProject(compMetadata, vcsToComponentMap, projectId, attachmentContent);

                    if (requestSummary.requestStatus.equals(RequestStatus.SUCCESS)) {

                        String jsonMessage = requestSummary.getMessage();
                        messageMap = new Gson().fromJson(jsonMessage, Map.class);
                        String projId = messageMap.get("projectId");

                        if (CommonUtils.isNullEmptyOrWhitespace(projId)) {
                            return requestSummary;
                        }
                        final Set<String> duplicatePackages = new HashSet<>();
                        final Set<String> componentsWithoutVcs = new HashSet<>();
                        final Set<String> invalidPackages = new HashSet<>();

                        Integer relReuseCount = Integer.valueOf(messageMap.get(REL_REUSE_COUNT_KEY));
                        Integer pkgReuseCount = Integer.valueOf(messageMap.get(PKG_REUSE_COUNT_KEY));
                        Integer pkgCreationCount = Integer.valueOf(messageMap.get(PKG_CREATION_COUNT_KEY));

                        String packages = messageMap.get(DUPLICATE_PACKAGE);
                        if (CommonUtils.isNotNullEmptyOrWhitespace(packages)) {
                            duplicatePackages.addAll(Arrays.asList(packages.split("\\|\\|")));
                            packages = "";
                        }
                        packages = messageMap.get(INVALID_PACKAGE);
                        if (CommonUtils.isNotNullEmptyOrWhitespace(packages)) {
                            invalidPackages.addAll(Arrays.asList(packages.split("\\|\\|")));
                            packages = "";
                        }
                        Project project = projectDatabaseHandler.getProjectById(projId, user);

                        for (org.cyclonedx.model.Component comp : components) {
                            if (CommonUtils.isNullOrEmptyCollection(comp.getExternalReferences())
                                    || comp.getExternalReferences().stream().map(ExternalReference::getType).filter(typeFilter).count() == 0
                                    || !containsComp(vcsToComponentMap, comp)) {

                                final var fullName = SW360Utils.getVersionedName(comp.getName(), comp.getVersion());
                                final var licenses = getLicenseFromBomComponent(comp);
                                final Package pkg = createPackage(comp, null, licenses);

                                if (pkg == null || CommonUtils.isNullEmptyOrWhitespace(pkg.getName()) || CommonUtils.isNullEmptyOrWhitespace(pkg.getVersion())
                                        || CommonUtils.isNullEmptyOrWhitespace(pkg.getPurl())) {
                                    invalidPackages.add(fullName);
                                    log.error(String.format("Invalid package '%s' found in SBoM, missing name or version or purl! ", fullName));
                                    continue;
                                }

                                try {
                                    AddDocumentRequestSummary pkgAddSummary = packageDatabaseHandler.addPackage(pkg, user);
                                    componentsWithoutVcs.add(fullName);

                                    if (CommonUtils.isNotNullEmptyOrWhitespace(pkgAddSummary.getId())) {
                                        pkg.setId(pkgAddSummary.getId());
                                        if (AddDocumentRequestStatus.DUPLICATE.equals(pkgAddSummary.getRequestStatus())) {
                                            if(!CommonUtils.nullToEmptySet(project.getPackageIds()).contains(pkgAddSummary.getId())){
                                                pkgReuseCount++;
                                            }
                                            Package dupPkg = packageDatabaseHandler.getPackageById(pkgAddSummary.getId());
                                            if (CommonUtils.isNotNullEmptyOrWhitespace(dupPkg.getReleaseId())) {
                                                if (!CommonUtils.nullToEmptyMap(project.getReleaseIdToUsage()).containsKey(dupPkg.getReleaseId())) {
                                                    project.putToReleaseIdToUsage(dupPkg.getReleaseId(), getDefaultRelation());
                                                    relReuseCount++;
                                                }
                                            }
                                        } else {
                                            pkgCreationCount++;
                                        }
                                    } else {
                                        // in case of more than 1 duplicate found, then return and show error message in UI.
                                        log.warn("found multiple packages: " + fullName);
                                        duplicatePackages.add(fullName);
                                        continue;
                                    }
                                    project.addToPackageIds(pkgAddSummary.getId());
                                } catch (SW360Exception e) {
                                    log.error("An error occured while creating/adding package from SBOM: " + e.getMessage());
                                    continue;
                                }
                            }
                        }

                        RequestStatus updateStatus = projectDatabaseHandler.updateProject(project, user);
                        if (RequestStatus.SUCCESS.equals(updateStatus)) {
                            log.info("linking packages to project successfull: " + projId);
                        }
                        // all components does not have VCS, so return & show appropriate error in UI
                        messageMap.put(INVALID_COMPONENT, String.join(JOINER, componentsWithoutVcs));
                        messageMap.put(INVALID_PACKAGE, String.join(JOINER, invalidPackages));
                        messageMap.put(DUPLICATE_PACKAGE, String.join(JOINER, duplicatePackages));
                        messageMap.put(SW360Constants.MESSAGE,
                                String.format("VCS information is missing for <b>%s</b> / <b>%s</b> Components!",
                                        componentsCount - vcsCount, componentsCount));
                        messageMap.put(REL_REUSE_COUNT_KEY, String.valueOf(relReuseCount));
                        messageMap.put(PKG_CREATION_COUNT_KEY, String.valueOf(pkgCreationCount));
                        messageMap.put(PKG_REUSE_COUNT_KEY, String.valueOf(pkgReuseCount));
                        requestSummary.setMessage(convertCollectionToJSONString(messageMap));
                    }
                } else {
                    requestSummary.setMessage(String.format(String.format(
                            "SBOM import aborted with error: Multiple vcs information found in compnents, vcs found: %s and total components: %s",
                            vcsCount, componentsCount)));
                    return requestSummary;
                }
            }

            if (RequestStatus.SUCCESS.equals(requestSummary.getRequestStatus())) {
                String jsonMessage = requestSummary.getMessage();
                messageMap = new Gson().fromJson(jsonMessage, Map.class);
                String projId = messageMap.get("projectId");
                Project project = projectDatabaseHandler.getProjectById(projId, user);
                try {
                    // link SBOM attachment to Project
                    if (attachmentContent != null) {
                        Attachment attachment = makeAttachmentFromContent(attachmentContent);
                        project.addToAttachments(attachment);
                    }
                    RequestStatus updateStatus = projectDatabaseHandler.updateProject(project, user, true);
                    if (RequestStatus.SUCCESS.equals(updateStatus)) {
                        log.info("SBOM attachment linked to project successfully: " + project.getId());
                    } else {
                        log.info("failed to link SBOM Import status attachment to project with status: " + updateStatus);
                    }
                    messageMap.put("result", requestSummary.getRequestStatus().toString());
                    messageMap.put("fileName", attachmentContent.getFilename());
                    final StringBuilder fileName = new StringBuilder(attachmentContent.getFilename())
                            .append("_ImportStatus_").append(SW360Utils.getCreatedOnTime().replaceAll(COLON_REGEX, HYPHEN)).append(DOT).append(SW360Constants.JSON_FILE_EXTENSION);
                    final InputStream inStream = IOUtils.toInputStream(convertCollectionToJSONString(messageMap), Charset.defaultCharset());
                    final AttachmentContent importResultAttachmentContent = makeAttachmentContent(fileName.toString(), MediaType.JSON_UTF_8.toString());
                    Attachment attachment = new AttachmentFrontendUtils().uploadAttachmentContent(importResultAttachmentContent, inStream, user);
                    attachment.setAttachmentType(AttachmentType.OTHER);
                    StringBuilder comment = new StringBuilder("Auto Generated: CycloneDX SBOM import result for: '").append(attachmentContent.getFilename()).append("'");
                    attachment.setCreatedComment(comment.toString());
                    attachment.setSha1(attachmentConnector.getSha1FromAttachmentContentId(importResultAttachmentContent.getId()));
                    project = projectDatabaseHandler.getProjectById(projId, user);
                    project.addToAttachments(attachment);
                    updateStatus = projectDatabaseHandler.updateProject(project, user, true);
                    if (RequestStatus.SUCCESS.equals(updateStatus)) {
                        log.info("SBOM Import status attachment linked to project successfully: " + project.getId());
                    } else {
                        log.info("failed to link SBOM Import status attachment to project with status: " + updateStatus);
                    }
                } catch (SW360Exception e) {
                    log.error("An error occured while updating project from SBOM: " + e.getMessage());
                    requestSummary.setMessage("An error occured while updating project during SBOM import, please delete the project and re-import SBOM!");
                    return requestSummary;
                }
            }

        } catch (IOException e) {
            log.error("IOException occured while importing CycloneDX SBoM: ", e);
            requestSummary.setMessage("IOException occured while importing CycloneDX SBoM: " + e.getMessage());
        } catch (ParseException e) {
            log.error("ParseException occured while importing CycloneDX SBoM: ", e);
            requestSummary.setMessage("ParseException occured while importing CycloneDX SBoM: " + e.getMessage());
        } catch (SW360Exception e) {
            log.error("SW360Exception occured while importing CycloneDX SBoM: ", e);
            requestSummary.setMessage("SW360Exception occured while importing CycloneDX SBoM: " + e.getMessage());
        } catch (Exception e) {
            log.error("Exception occured while importing CycloneDX SBoM: ", e);
            requestSummary.setMessage("An Exception occured while importing CycloneDX SBoM: " + e.getMessage());
        }
        return requestSummary;
    }

    public RequestSummary importSbomAsProject(org.cyclonedx.model.Component compMetadata,
            Map<String, List<org.cyclonedx.model.Component>> vcsToComponentMap, String projectId, AttachmentContent attachmentContent)
                    throws SW360Exception {
        final RequestSummary summary = new RequestSummary();
        summary.setRequestStatus(RequestStatus.FAILURE);

        Project project;
        AddDocumentRequestSummary projectAddSummary = new AddDocumentRequestSummary();
        AddDocumentRequestStatus addStatus = projectAddSummary.getRequestStatus();
        Map<String, String> messageMap = new HashMap<>();

        try {
            if (CommonUtils.isNotNullEmptyOrWhitespace(projectId)) {
                project = projectDatabaseHandler.getProjectById(projectId, user);
                Project sBomProject = createProject(compMetadata);
                if (!(sBomProject.getName().equalsIgnoreCase(project.getName()) && sBomProject.getVersion().equalsIgnoreCase(project.getVersion()))) {
                    log.warn("cannot import SBOM with different metadata information than the current project!");
                    summary.setRequestStatus(RequestStatus.FAILED_SANITY_CHECK);
                    messageMap.put(PROJECT_NAME, SW360Utils.getVersionedName(sBomProject.getName(), sBomProject.getVersion()));
                    summary.setMessage(convertCollectionToJSONString(messageMap));
                    return summary;
                }
                log.info("reusing existing project: " + projectId);
            } else {
                // Metadata component is used to created the project
                project = createProject(compMetadata);
                projectAddSummary = projectDatabaseHandler.addProject(project, user);
                addStatus = projectAddSummary.getRequestStatus();

                if (CommonUtils.isNotNullEmptyOrWhitespace(projectAddSummary.getId())) {
                    if (AddDocumentRequestStatus.SUCCESS.equals(addStatus)) {
                        project = projectDatabaseHandler.getProjectById(projectAddSummary.getId(), user);
                        log.info("project created successfully: " + projectAddSummary.getId());
                    } else if (AddDocumentRequestStatus.DUPLICATE.equals(addStatus)) {
                        log.warn("cannot import SBOM for an existing project from Project List / Home page - " + projectAddSummary.getId());
                        summary.setRequestStatus(getRequestStatusFromAddDocRequestStatus(addStatus));
                        messageMap.put(PROJECT_ID, projectAddSummary.getId());
                        messageMap.put(PROJECT_NAME, SW360Utils.getVersionedName(project.getName(), project.getVersion()));
                        summary.setMessage(convertCollectionToJSONString(messageMap));
                        return summary;
                    }
                } else {
                    summary.setRequestStatus(getRequestStatusFromAddDocRequestStatus(addStatus));
                    summary.setMessage("Invalid Projct metadata present in SBOM or Multiple project with same name and version is already present in SW360!");
                    return summary;
                }
            }
        } catch (SW360Exception e) {
            log.error("An error occured while importing project from SBOM: " + e.getMessage());
            summary.setMessage("An error occured while importing project from SBOM!");
            return summary;
        }

        if (IS_PACKAGE_PORTLET_ENABLED) {
            messageMap = importAllComponentsAsPackages(vcsToComponentMap, project);
        } else {
            messageMap = importAllComponentsAsReleases(vcsToComponentMap, project);
        }
        RequestStatus updateStatus = projectDatabaseHandler.updateProject(project, user, true);
        if (RequestStatus.SUCCESS.equals(updateStatus)) {
            log.info("project updated successfully: " + project.getId());
        } else {
            log.info("failed to update project with status: " + updateStatus);
        }
        summary.setMessage(convertCollectionToJSONString(messageMap));
        summary.setRequestStatus(RequestStatus.SUCCESS);
        return summary;
    }

    private Map<String, String> importAllComponentsAsReleases(Map<String, List<org.cyclonedx.model.Component>> vcsToComponentMap, Project project) {

        final var countMap = new HashMap<String, Integer>();
        final Set<String> duplicateComponents = new HashSet<>();
        final Set<String> duplicateReleases = new HashSet<>();
        final Set<String> invalidReleases = new HashSet<>();
        final Map<String, ProjectReleaseRelationship> releaseRelationMap = CommonUtils.isNullOrEmptyMap(project.getReleaseIdToUsage()) ? new HashMap<>() : project.getReleaseIdToUsage();
        countMap.put(COMP_CREATION_COUNT_KEY, 0); countMap.put(COMP_REUSE_COUNT_KEY, 0);
        countMap.put(REL_CREATION_COUNT_KEY, 0); countMap.put(REL_REUSE_COUNT_KEY, 0);
        int compCreationCount = 0, compReuseCount = 0, relCreationCount = 0, relReuseCount = 0;

        final List<org.cyclonedx.model.Component> components = vcsToComponentMap.get("");
        for (org.cyclonedx.model.Component bomComp : components) {
            Component comp = createComponent(bomComp);
            if (CommonUtils.isNullEmptyOrWhitespace(comp.getName()) ) {
                log.error("component name is not present in SBoM: " + project.getId());
                continue;
            }
            String relName = "";
            AddDocumentRequestSummary compAddSummary;
            try {
                compAddSummary = componentDatabaseHandler.addComponent(comp, user.getEmail());

                if (CommonUtils.isNotNullEmptyOrWhitespace(compAddSummary.getId())) {
                    comp.setId(compAddSummary.getId());
                    if (AddDocumentRequestStatus.SUCCESS.equals(compAddSummary.getRequestStatus())) {
                        compCreationCount++;
                    } else {
                        compReuseCount++;
                    }
                } else {
                    // in case of more than 1 duplicate found, then continue and show error message in UI.
                    log.warn("found multiple components: " + comp.getName());
                    duplicateComponents.add(comp.getName());
                    continue;
                }

                Release release = new Release();
                Set<String> licenses = getLicenseFromBomComponent(bomComp);
                release = createRelease(bomComp, comp, licenses);
                if (CommonUtils.isNullEmptyOrWhitespace(release.getVersion()) ) {
                    log.error("release version is not present in SBoM for component: " + comp.getName());
                    invalidReleases.add(comp.getName());
                    continue;
                }
                relName = SW360Utils.getVersionedName(release.getName(), release.getVersion());

                try {
                    AddDocumentRequestSummary relAddSummary = componentDatabaseHandler.addRelease(release, user);
                    if (CommonUtils.isNotNullEmptyOrWhitespace(relAddSummary.getId())) {
                        release.setId(relAddSummary.getId());
                        if (AddDocumentRequestStatus.SUCCESS.equals(relAddSummary.getRequestStatus())) {
                            relCreationCount++;
                        } else {
                            relReuseCount++;
                        }
                    } else {
                        // in case of more than 1 duplicate found, then continue and show error message in UI.
                        log.warn("found multiple releases: " + relName);
                        duplicateReleases.add(relName);
                        continue;
                    }
                    releaseRelationMap.putIfAbsent(release.getId(), getDefaultRelation());
                } catch (SW360Exception e) {
                    log.error("An error occured while creating/adding release from SBOM: " + e.getMessage());
                    continue;
                }

                // update components specific fields
                comp = componentDatabaseHandler.getComponent(compAddSummary.getId(), user);
                if (AddDocumentRequestStatus.SUCCESS.equals(compAddSummary.getRequestStatus()) && (null != bomComp.getType() && null == comp.getCdxComponentType())) {
                    comp.setCdxComponentType(getCdxComponentType(bomComp.getType()));
                }
                if (AddDocumentRequestStatus.SUCCESS.equals(compAddSummary.getRequestStatus()) && (CommonUtils.isNullEmptyOrWhitespace(comp.getDescription()) && CommonUtils.isNotNullEmptyOrWhitespace(bomComp.getDescription()))) {
                    comp.setDescription(bomComp.getDescription().trim());
                }
                if (CommonUtils.isNotEmpty(comp.getMainLicenseIds())) {
                    comp.getMainLicenseIds().addAll(licenses);
                } else {
                    comp.setMainLicenseIds(licenses);
                }
                for (ExternalReference extRef : CommonUtils.nullToEmptyList(bomComp.getExternalReferences())) {
                    if (Type.WEBSITE.equals(extRef.getType()) && CommonUtils.isNullEmptyOrWhitespace(comp.getHomepage())) {
                        comp.setHomepage(CommonUtils.nullToEmptyString(extRef.getUrl()));
                    } else if (Type.MAILING_LIST.equals(extRef.getType()) && CommonUtils.isNullEmptyOrWhitespace(comp.getMailinglist())) {
                        comp.setMailinglist(CommonUtils.nullToEmptyString(extRef.getUrl()));
                    } else if (Type.SUPPORT.equals(extRef.getType()) && CommonUtils.isNullEmptyOrWhitespace(comp.getWiki())) {
                        comp.setWiki(CommonUtils.nullToEmptyString(extRef.getUrl()));
                    }
                }

                RequestStatus updateStatus = componentDatabaseHandler.updateComponent(comp, user, true);
                if (RequestStatus.SUCCESS.equals(updateStatus)) {
                    log.info("updating component successfull: " + comp.getName());
                }
            } catch (SW360Exception e) {
                log.error("An error occured while creating/adding component from SBOM: " + e.getMessage());
                continue;
            }
        }

        project.setReleaseIdToUsage(releaseRelationMap);
        final Map<String, String> messageMap = new HashMap<>();
        messageMap.put(DUPLICATE_COMPONENT, String.join(JOINER, duplicateComponents));
        messageMap.put(DUPLICATE_RELEASE, String.join(JOINER, duplicateReleases));
        messageMap.put(INVALID_RELEASE, String.join(JOINER, invalidReleases));
        messageMap.put(PROJECT_ID, project.getId());
        messageMap.put(PROJECT_NAME, SW360Utils.getVersionedName(project.getName(), project.getVersion()));
        messageMap.put(COMP_CREATION_COUNT_KEY, String.valueOf(compCreationCount));
        messageMap.put(COMP_REUSE_COUNT_KEY, String.valueOf(compReuseCount));
        messageMap.put(REL_CREATION_COUNT_KEY, String.valueOf(relCreationCount));
        messageMap.put(REL_REUSE_COUNT_KEY, String.valueOf(relReuseCount));
        return messageMap;
    }

    private Map<String, String> importAllComponentsAsPackages(Map<String, List<org.cyclonedx.model.Component>> vcsToComponentMap, Project project) {

        final var countMap = new HashMap<String, Integer>();
        final Set<String> duplicateComponents = new HashSet<>();
        final Set<String> duplicateReleases = new HashSet<>();
        final Set<String> duplicatePackages = new HashSet<>();
        final Set<String> invalidReleases = new HashSet<>();
        final Set<String> invalidPackages = new HashSet<>();
        final Map<String, ProjectReleaseRelationship> releaseRelationMap = CommonUtils.isNullOrEmptyMap(project.getReleaseIdToUsage()) ? new HashMap<>() : project.getReleaseIdToUsage();
        countMap.put(REL_CREATION_COUNT_KEY, 0); countMap.put(REL_REUSE_COUNT_KEY, 0);
        countMap.put(PKG_CREATION_COUNT_KEY, 0); countMap.put(PKG_REUSE_COUNT_KEY, 0);
        int relCreationCount = 0, relReuseCount = 0, pkgCreationCount = 0, pkgReuseCount = 0;

        for (Map.Entry<String, List<org.cyclonedx.model.Component>> entry : vcsToComponentMap.entrySet()) {
            Component comp = createComponent(entry.getKey());
            List<org.cyclonedx.model.Component> componentsFromBom = entry.getValue();

            Release release = new Release();
            String relName = "";
            AddDocumentRequestSummary compAddSummary;
            try {
                Component dupCompByName = componentDatabaseHandler.getComponentByName(comp.getName());

                if (dupCompByName != null && (CommonUtils.isNotNullEmptyOrWhitespace(dupCompByName.getVcs())
                        && !(comp.getVcs().equalsIgnoreCase(dupCompByName.getVcs())))) {
                    comp.setName(getComponentNameFromVCS(entry.getKey(), true));
                }

                compAddSummary = componentDatabaseHandler.addComponent(comp, user.getEmail());

                if (CommonUtils.isNotNullEmptyOrWhitespace(compAddSummary.getId())) {
                    comp.setId(compAddSummary.getId());
                    String existingCompName = getComponetNameById(comp.getId(), user);
                    comp.setName(existingCompName);
                } else {
                    // in case of more than 1 duplicate found, then continue and show error message in UI.
                    log.warn("found multiple components: " + comp.getName());
                    duplicateComponents.add(comp.getName());
                    continue;
                }

                for (org.cyclonedx.model.Component bomComp : entry.getValue()) {
                    Set<String> licenses = getLicenseFromBomComponent(bomComp);
                    release = createRelease(bomComp.getVersion(), comp, licenses);
                    if (CommonUtils.isNullEmptyOrWhitespace(release.getVersion()) ) {
                        log.error("release version is not present in SBoM for component: " + comp.getName());
                        invalidReleases.add(comp.getName());
                        continue;
                    }
                    relName = SW360Utils.getVersionedName(release.getName(), release.getVersion());

                    try {
                        AddDocumentRequestSummary relAddSummary = componentDatabaseHandler.addRelease(release, user);

                        if (CommonUtils.isNotNullEmptyOrWhitespace(relAddSummary.getId())) {
                            release.setId(relAddSummary.getId());
                            if (AddDocumentRequestStatus.SUCCESS.equals(relAddSummary.getRequestStatus())) {
                                relCreationCount = releaseRelationMap.containsKey(release.getId()) ? relCreationCount : relCreationCount + 1;
                            } else {
                                relReuseCount = releaseRelationMap.containsKey(release.getId()) ? relReuseCount : relReuseCount + 1;
                            }
                        } else {
                            // in case of more than 1 duplicate found, then continue and show error message in UI.
                            log.warn("found multiple releases: " + relName);
                            duplicateReleases.add(relName);
                            continue;
                        }
                    } catch (SW360Exception e) {
                        log.error("An error occured while creating/adding release from SBOM: " + e.getMessage());
                        continue;
                    }

                    // update components specific fields
                    comp = componentDatabaseHandler.getComponent(compAddSummary.getId(), user);
                    if (AddDocumentRequestStatus.SUCCESS.equals(compAddSummary.getRequestStatus()) && (null != bomComp.getType() && null == comp.getCdxComponentType())) {
                        comp.setCdxComponentType(getCdxComponentType(bomComp.getType()));
                    }
                    if (AddDocumentRequestStatus.SUCCESS.equals(compAddSummary.getRequestStatus()) && (CommonUtils.isNullEmptyOrWhitespace(comp.getDescription()) && CommonUtils.isNotNullEmptyOrWhitespace(bomComp.getDescription()))) {
                        comp.setDescription(bomComp.getDescription().trim());
                    }
                    if (CommonUtils.isNotEmpty(comp.getMainLicenseIds())) {
                        comp.getMainLicenseIds().addAll(licenses);
                    } else {
                        comp.setMainLicenseIds(licenses);
                    }
                    if (CommonUtils.isNullEmptyOrWhitespace(comp.getVcs())) {
                        for (ExternalReference extRef : CommonUtils.nullToEmptyList(bomComp.getExternalReferences())) {
                            if (Type.VCS.equals(extRef.getType())) {
                                comp.setVcs(entry.getKey());
                            } else if (Type.MAILING_LIST.equals(extRef.getType())) {
                                comp.setMailinglist(CommonUtils.nullToEmptyString(extRef.getUrl()));
                            } else if (Type.SUPPORT.equals(extRef.getType())) {
                                comp.setWiki(CommonUtils.nullToEmptyString(extRef.getUrl()));
                            }
                        }
                    }

                    RequestStatus updateStatus = componentDatabaseHandler.updateComponent(comp, user, true);
                    if (RequestStatus.SUCCESS.equals(updateStatus)) {
                        log.info("updating component successfull: " + comp.getName());
                    }

                    releaseRelationMap.putIfAbsent(release.getId(), getDefaultRelation());
                    Package pkg = createPackage(bomComp, release, licenses);
                    String pkgName = pkg == null ? SW360Utils.getVersionedName(bomComp.getName(), bomComp.getVersion()) : SW360Utils.getVersionedName(pkg.getName(), pkg.getVersion());
                    if (pkg == null || CommonUtils.isNullEmptyOrWhitespace(pkg.getName()) || CommonUtils.isNullEmptyOrWhitespace(pkg.getVersion())
                            || CommonUtils.isNullEmptyOrWhitespace(pkg.getPurl())) {
                        invalidPackages.add(pkgName);
                        log.error(String.format("Invalid package '%s' found in SBoM, missing name or version or purl! ", pkgName));
                        continue;
                    }

                    try {
                        AddDocumentRequestSummary pkgAddSummary = packageDatabaseHandler.addPackage(pkg, user);
                        if (CommonUtils.isNotNullEmptyOrWhitespace(pkgAddSummary.getId())) {
                            pkg.setId(pkgAddSummary.getId());
                            if (AddDocumentRequestStatus.DUPLICATE.equals(pkgAddSummary.getRequestStatus())) {
                                Package dupPkg = packageDatabaseHandler.getPackageById(pkg.getId());
                                String dupPkgReleaseId = dupPkg.getReleaseId();
                                String releaseId = release.getId();
                                if (!releaseId.equals(dupPkgReleaseId) && CommonUtils.isNullEmptyOrWhitespace(dupPkgReleaseId)) {
                                    dupPkg.setReleaseId(releaseId);
                                    packageDatabaseHandler.updatePackage(dupPkg, user);
                                    log.error("Release Id of Package from BOM: '%s' and Database: '%s' is not equal!", releaseId, dupPkgReleaseId);
                                }
                                if(!CommonUtils.nullToEmptySet(project.getPackageIds()).contains(pkg.getId())){
                                    pkgReuseCount++;
                                }
                            } else {
                                pkgCreationCount++;
                            }
                        } else {
                            // in case of more than 1 duplicate found, then continue and show error message in UI.
                            log.warn("found multiple packages: " + pkgName);
                            duplicatePackages.add(pkgName);
                            continue;
                        }
                        project.addToPackageIds(pkg.getId());
                    } catch (SW360Exception e) {
                        log.error("An error occured while creating/adding package from SBOM: " + e.getMessage());
                        continue;
                    }
                }
            } catch (SW360Exception e) {
                log.error("An error occured while creating/adding component from SBOM: " + e.getMessage());
                continue;
            }
        }

        project.setReleaseIdToUsage(releaseRelationMap);
        final Map<String, String> messageMap = new HashMap<>();
        messageMap.put(DUPLICATE_COMPONENT, String.join(JOINER, duplicateComponents));
        messageMap.put(DUPLICATE_RELEASE, String.join(JOINER, duplicateReleases));
        messageMap.put(DUPLICATE_PACKAGE, String.join(JOINER, duplicatePackages));
        messageMap.put(INVALID_RELEASE, String.join(JOINER, invalidReleases));
        messageMap.put(INVALID_PACKAGE, String.join(JOINER, invalidPackages));
        messageMap.put(PROJECT_ID, project.getId());
        messageMap.put(PROJECT_NAME, SW360Utils.getVersionedName(project.getName(), project.getVersion()));
        messageMap.put(REL_CREATION_COUNT_KEY, String.valueOf(relCreationCount));
        messageMap.put(REL_REUSE_COUNT_KEY, String.valueOf(relReuseCount));
        messageMap.put(PKG_CREATION_COUNT_KEY, String.valueOf(pkgCreationCount));
        messageMap.put(PKG_REUSE_COUNT_KEY, String.valueOf(pkgReuseCount));
        return messageMap;
    }

    private Set<String> getLicenseFromBomComponent(org.cyclonedx.model.Component comp) {
        Set<String> licenses = new HashSet<>();
        if ((null != comp.getLicenseChoice() && CommonUtils.isNotEmpty(comp.getLicenseChoice().getLicenses()))) {
            licenses.addAll(comp.getLicenseChoice().getLicenses().stream()
                    .map(lic -> (null == lic.getId()) ? lic.getName() : lic.getId())
                    .filter(lic -> (null != lic && !SW360Constants.NO_ASSERTION.equalsIgnoreCase(lic)))
                    .collect(Collectors.toSet()));
        }
        if (null != comp.getEvidence() && null != comp.getEvidence().getLicenseChoice()
                && CommonUtils.isNotEmpty(comp.getEvidence().getLicenseChoice().getLicenses())) {
            licenses.addAll(comp.getEvidence().getLicenseChoice().getLicenses().stream()
                    .map(lic -> (null == lic.getId()) ? lic.getName() : lic.getId())
                    .filter(lic -> (null != lic && !SW360Constants.NO_ASSERTION.equalsIgnoreCase(lic)))
                    .collect(Collectors.toSet()));
        }
        return licenses;
    }

    private String convertCollectionToJSONString(Map<String, String> map) throws SW360Exception {
        try {
            JsonFactory factory = new JsonFactory();
            StringWriter jsonObjectWriter = new StringWriter();
            JsonGenerator jsonGenerator = factory.createGenerator(jsonObjectWriter);
            jsonGenerator.useDefaultPrettyPrinter();
            jsonGenerator.writeStartObject();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                jsonGenerator.writeStringField(entry.getKey(), entry.getValue());
            }
            jsonGenerator.writeEndObject();
            jsonGenerator.close();
            return jsonObjectWriter.toString();
        } catch (IOException e) {
            throw new SW360Exception("An exception occured while generating JSON info for BOM import! " + e.getMessage());
        }
    }

    private RequestStatus getRequestStatusFromAddDocRequestStatus(AddDocumentRequestStatus status) {
        switch (status) {
            case SUCCESS:
                return RequestStatus.SUCCESS;
            case DUPLICATE:
                return RequestStatus.DUPLICATE;
            case NAMINGERROR:
                return RequestStatus.NAMINGERROR;
            case INVALID_INPUT:
                return RequestStatus.INVALID_INPUT;
            default:
                return RequestStatus.FAILURE;
        }
    }

    private AttachmentContent makeAttachmentContent(String filename, String contentType) {
        AttachmentContent attachment = new AttachmentContent()
                .setContentType(contentType)
                .setFilename(filename)
                .setOnlyRemote(false);
        return makeAttachmentContent(attachment);
    }

    private AttachmentContent makeAttachmentContent(AttachmentContent content) {
        try {
            return new AttachmentFrontendUtils().makeAttachmentContent(content);
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }

    private Attachment makeAttachmentFromContent(AttachmentContent attachmentContent) {
        Attachment attachment = new Attachment();
        attachment.setAttachmentContentId(attachmentContent.getId());
        attachment.setAttachmentType(AttachmentType.SBOM);
        StringBuilder comment = new StringBuilder("Auto Generated: Used for importing CycloneDX SBOM.");
        attachment.setCreatedComment(comment.toString());
        attachment.setFilename(attachmentContent.getFilename());
        attachment.setCreatedOn(SW360Utils.getCreatedOn());
        attachment.setCreatedBy(user.getEmail());
        attachment.setCreatedTeam(user.getDepartment());
        attachment.setCheckStatus(CheckStatus.NOTCHECKED);
        return attachment;
    }

    private Project createProject(org.cyclonedx.model.Component compMetadata) {
        return new Project(CommonUtils.nullToEmptyString(compMetadata.getName()).trim())
                .setVersion(CommonUtils.nullToEmptyString(compMetadata.getVersion()).trim())
                .setDescription(CommonUtils.nullToEmptyString(compMetadata.getDescription()).trim()).setType(ThriftEnumUtils.enumToString(ProjectType.PRODUCT))
                .setBusinessUnit(user.getDepartment()).setVisbility(Visibility.EVERYONE);
    }

    // Create Component for Package
    private Component createComponent(String vcsUrl) {
        String name = getComponentNameFromVCS(vcsUrl, false);
        Component component = new Component();
        component.setName(CommonUtils.nullToEmptyString(name.trim()));
        component.setComponentType(ComponentType.OSS);
        component.setVcs(vcsUrl);

        return component;
    }

    private Component createComponent(org.cyclonedx.model.Component componentFromBom) {
        Component component = new Component();
        component.setName(CommonUtils.nullToEmptyString(componentFromBom.getName()).trim());
        component.setComponentType(ComponentType.OSS);
        if (null != componentFromBom.getType()) {
            component.setCdxComponentType(getCdxComponentType(componentFromBom.getType()));
        }

        for (ExternalReference extRef : CommonUtils.nullToEmptyList(componentFromBom.getExternalReferences())) {
            if (Type.WEBSITE.equals(extRef.getType())) {
                component.setHomepage(CommonUtils.nullToEmptyString(extRef.getUrl()));
            } else if (Type.MAILING_LIST.equals(extRef.getType())) {
                component.setMailinglist(CommonUtils.nullToEmptyString(extRef.getUrl()));
            } else if (Type.SUPPORT.equals(extRef.getType())) {
                component.setWiki(CommonUtils.nullToEmptyString(extRef.getUrl()));
            }
        }
        return component;
    }

    private CycloneDxComponentType getCdxComponentType(org.cyclonedx.model.Component.Type compType) {
        switch (compType) {
        case APPLICATION:
            return CycloneDxComponentType.APPLICATION;
        case CONTAINER:
            return CycloneDxComponentType.CONTAINER;
        case DEVICE:
            return CycloneDxComponentType.DEVICE;
        case FILE:
            return CycloneDxComponentType.FILE;
        case FIRMWARE:
            return CycloneDxComponentType.FIRMWARE;
        case FRAMEWORK:
            return CycloneDxComponentType.FRAMEWORK;
        case LIBRARY:
            return CycloneDxComponentType.LIBRARY;
        case OPERATING_SYSTEM:
            return CycloneDxComponentType.OPERATING_SYSTEM;
        default:
            return null;
        }
    }

    // Create Release for Package
    private Release createRelease(String version, Component component, Set<String> licenses) {
        Release release = new Release(component.getName(), CommonUtils.nullToEmptyString(version).trim(), component.getId());
        release.setCreatorDepartment(user.getDepartment());
        if (release.isSetMainLicenseIds()) {
            release.getMainLicenseIds().addAll(licenses);
        } else {
            release.setMainLicenseIds(licenses);
        }
        return release;
    }

    private Release createRelease(org.cyclonedx.model.Component componentFromBom, Component component, Set<String> licenses) {
        Release release = new Release(component.getName(), CommonUtils.nullToEmptyString(componentFromBom.getVersion()).trim(), component.getId());
        release.setCreatorDepartment(user.getDepartment());
        if (release.isSetMainLicenseIds()) {
            release.getMainLicenseIds().addAll(licenses);
        } else {
            release.setMainLicenseIds(licenses);
        }
        if (CommonUtils.isNotNullEmptyOrWhitespace(componentFromBom.getCpe())) {
            release.setCpeid(componentFromBom.getCpe());
        }
        if (CommonUtils.isNotNullEmptyOrWhitespace(componentFromBom.getPurl())) {
            String purl = componentFromBom.getPurl();
            try {
                purl = purl.toLowerCase().trim();
                new PackageURL(purl);
                Map<String, String> externalIds = new HashMap<>();
                externalIds.put(SW360Constants.PACKAGE_URL, purl);
                release.setExternalIds(externalIds);
            } catch (MalformedPackageURLException e) {
                log.error("Malformed PURL for component: " + componentFromBom.getName(), e);
            }
        }
        for (ExternalReference extRef : CommonUtils.nullToEmptyList(componentFromBom.getExternalReferences())) {
            if (Type.VCS.equals(extRef.getType())) {
                String repoUrl = CommonUtils.nullToEmptyString(StringUtils.removeEnd(extRef.getUrl(), DOT_GIT));
                Repository repo = new Repository(repoUrl);
                if (repoUrl.toLowerCase().contains("github")) {
                    repo.setRepositorytype(RepositoryType.GIT);
                } else if (repoUrl.toLowerCase().contains("svn")) {
                    repo.setRepositorytype(RepositoryType.SVN);
                }
                release.setRepository(repo);
            }
        }
        return release;
    }

    private static ProjectReleaseRelationship getDefaultRelation() {
        return new ProjectReleaseRelationship(ReleaseRelationship.UNKNOWN, MainlineState.OPEN);
    }

    private String getPackageName(PackageURL packageURL, org.cyclonedx.model.Component comp, String delimiter) {
        String name = CommonUtils.nullToEmptyString(packageURL.getName());
        String pkgManager = CommonUtils.isNullEmptyOrWhitespace(packageURL.getType()) ? "" : packageURL.getType();
        if (CommonUtils.isNotNullEmptyOrWhitespace(packageURL.getNamespace())) {
            return new StringBuilder(pkgManager).append(delimiter).append(packageURL.getNamespace()).append(delimiter).append(name).toString();
        } else if (CommonUtils.isNotNullEmptyOrWhitespace(comp.getGroup())) {
            return new StringBuilder(pkgManager).append(delimiter).append(comp.getGroup()).append(delimiter).append(name).toString();
        } else if (CommonUtils.isNotNullEmptyOrWhitespace(comp.getPublisher())) {
            //.replaceAll(CLEAN_PUBLISHER_REGEX, StringUtils.EMPTY) --> Use this to remove Publisher email id.
            return new StringBuilder(pkgManager).append(delimiter).append(comp.getPublisher()).append(delimiter).append(name).toString();
        } else {
            return pkgManager + delimiter + name;
        }
    }

    private Package createPackage(org.cyclonedx.model.Component componentFromBom, Release release, Set<String> licenses) {
        Package pckg = new Package();
        String purl = componentFromBom.getPurl();
        if (CommonUtils.isNotNullEmptyOrWhitespace(purl)) {
            try {
                purl = purl.toLowerCase().trim();
                PackageURL packageURL = new PackageURL(purl);
                pckg.setPurl(purl);
                String packageName = componentFromBom.getName();
                boolean isDuplicatePackageName = packageDatabaseHandler.getPackageByNameAndVersion(packageName, packageURL.getVersion()).size() > 0;
                if (isDuplicatePackageName) {
                    packageName = getPackageName(packageURL, componentFromBom, SLASH).trim();
                }
                pckg.setName(packageName);
                pckg.setVersion(packageURL.getVersion());
            } catch (MalformedPackageURLException e) {
                log.error("Malformed PURL for component: " + componentFromBom.getName(), e);
                return null;
            }
        } else {
            return null;
        }

        if (null == componentFromBom.getType()) {
            pckg.setPackageType(CycloneDxComponentType.LIBRARY);
        } else {
            pckg.setPackageType(getCdxComponentType(componentFromBom.getType()));
        }
        if (release != null && release.isSetId()) {
            pckg.setReleaseId(release.getId());
        }
        pckg.setDescription(CommonUtils.nullToEmptyString(componentFromBom.getDescription()).trim());

        for (ExternalReference ref : CommonUtils.nullToEmptyList(componentFromBom.getExternalReferences())) {
            if (ExternalReference.Type.WEBSITE.equals(ref.getType())) {
                pckg.setHomepageUrl(ref.getUrl());
            } else if (ExternalReference.Type.VCS.equals(ref.getType())) {
                pckg.setVcs(ref.getUrl().replaceAll(VCS_HTTP_REGEX, HTTPS_SCHEME));
            }
        }

        if (pckg.isSetLicenseIds()) {
            pckg.getLicenseIds().addAll(licenses);
        } else {
            pckg.setLicenseIds(licenses);
        }
        return pckg;
    }

    public String getComponetNameById(String id, User user) throws SW360Exception {
        Component comp = componentDatabaseHandler.getComponent(id, user);
        return comp.getName();
    }

    private String getComponentNameFromVCS(String vcsUrl, boolean isGetVendorandName) {
        String compName = vcsUrl.replaceAll(SCHEMA_PATTERN, "$1");
        String[] parts = compName.split("/");

        if (parts.length >= 2) {
            if (isGetVendorandName) {
                return String.join("/", Arrays.copyOfRange(parts, 1, parts.length));
            } else {
                return parts[parts.length - 1];
            }
        }
        return compName;
    }

    /*
     * Sanitize different repository URLS based on their defined schema
     */
    public String sanitizeVCS(String vcs) {
        for (String host : VCS_HOSTS.keySet()) {
            if (vcs.contains(host)) {
                return sanitizeVCSByHost(vcs, host);
            }
        }
        return vcs; // Return unchanged if no known host is found
    }

    private String sanitizeVCSByHost(String vcs, String host) {
        vcs = "https://" + vcs.substring(vcs.indexOf(host)).trim();

        try {
            URI uri = URI.create(vcs);
            String[] urlParts = uri.getPath().split("/");
            String formattedUrl = formatVCSUrl(host, urlParts);

            if (formattedUrl == null) {
                log.error("Invalid {} repository URL: {}", host, vcs);
                return vcs;
            }
            return formattedUrl.endsWith("/") ? formattedUrl.substring(0, formattedUrl.length() - 1) : formattedUrl;

        } catch (IllegalArgumentException e) {
            log.error("Invalid URL format: {}", vcs, e);
            return vcs;
        }
    }

    private String formatVCSUrl(String host, String[] urlParts) {
        String formattedUrl = null;

        switch (host) {
            case "github.com":
            case "bitbucket.org":
                if (urlParts.length >= 3) {
                    formattedUrl = String.format(VCS_HOSTS.get(host),
                            urlParts[1], urlParts[2].replaceAll("\\.git.*|#.*", ""));
                }
                break;

            case "gitlab.com":
                if (urlParts.length >= 2) {
                    // Join everything after the main host to get the full nested path
                    String repoPath = String.join("/", Arrays.copyOfRange(urlParts, 1, urlParts.length));

                    // Remove everything from the first occurrence of .git or #
                    repoPath = repoPath.replaceAll("\\.git.*|#.*", "");

                    formattedUrl = String.format(VCS_HOSTS.get(host), repoPath);
                }
                break;

            case "cs.opensource.google":
                if (urlParts.length >= 3) {
                    String thirdSegment = urlParts.length > 3 && !urlParts[3].isEmpty() && !urlParts[3].equals("+")
                            ? urlParts[3] : "";
                    formattedUrl = String.format(VCS_HOSTS.get(host), urlParts[1], urlParts[2], thirdSegment);
                }
                break;

            case "go.googlesource.com":
                if (urlParts.length >= 2) {
                    formattedUrl = String.format(VCS_HOSTS.get(host), urlParts[1]);
                }
                break;

            case "pypi.org":
                if (urlParts.length >= 3) {
                    formattedUrl = String.format(VCS_HOSTS.get(host), urlParts[2].replaceAll("\\.git.*|#.*", ""));
                }
                break;
        }

        return formattedUrl;
    }

    public static boolean containsComp(Map<String, List<org.cyclonedx.model.Component>> map, org.cyclonedx.model.Component element) {
        for (List<org.cyclonedx.model.Component> list : map.values()) {
            if (list.contains(element)) {
                return true;
            }
        }
        return false;
    }
}

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
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.cyclonedx.model.Property;
import org.cyclonedx.parsers.JsonParser;
import org.cyclonedx.parsers.Parser;
import org.cyclonedx.parsers.XmlParser;
import org.eclipse.sw360.common.utils.RepositoryURL;
import org.eclipse.sw360.commonIO.AttachmentFrontendUtils;
import org.eclipse.sw360.datahandler.common.*;
import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.db.ComponentDatabaseHandler;
import org.eclipse.sw360.datahandler.db.PackageDatabaseHandler;
import org.eclipse.sw360.datahandler.db.ProjectDatabaseHandler;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestStatus;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.thrift.CycloneDxComponentType;
import org.eclipse.sw360.datahandler.thrift.MainlineState;
import org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.ProjectPackageRelationship;
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
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectType;
import org.eclipse.sw360.datahandler.thrift.users.User;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import com.google.common.io.Files;
import com.google.common.net.MediaType;
import com.google.gson.Gson;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;

import static org.eclipse.sw360.common.utils.RepositoryURL.*;
import static org.eclipse.sw360.datahandler.common.SW360ConfigKeys.IS_PACKAGE_PORTLET_ENABLED;
import static org.eclipse.sw360.datahandler.common.SW360ConfigKeys.NON_PKG_MANAGED_COMPS_PROP;

/**
 * CycloneDX BOM import implementation.
 * Supports both XML and JSON format of CycloneDX SBOM
 *
 * @author abdul.kapti@siemens-healthineers.com
 */
public class CycloneDxBOMImporter {
    private static final Logger log = LogManager.getLogger(CycloneDxBOMImporter.class);
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
    private static final String NON_PKG_MANAGED_COMP_WITHOUT_VCS = "nonPkgManagedCompWithoutVCS";
    private static final String INVALID_PACKAGE = "invalidPkg";
    private static final String COMPONENT_IMPORT_ERROR_COUNT_KEY = "compImportErrorCount";
    private static final String PROJECT_ID = "projectId";
    private static final String PROJECT_NAME = "projectName";
    public static final String INVALID_VCS_COMPONENT = "invalidVcsComponent";
    private static final Predicate<ExternalReference.Type> typeFilter = Type.VCS::equals;

    private final ProjectDatabaseHandler projectDatabaseHandler;
    private final ComponentDatabaseHandler componentDatabaseHandler;
    private final PackageDatabaseHandler packageDatabaseHandler;
    private final User user;
    private final AttachmentConnector attachmentConnector;
    private final RepositoryURL repositoryURL;

    public CycloneDxBOMImporter(ProjectDatabaseHandler projectDatabaseHandler, ComponentDatabaseHandler componentDatabaseHandler,
            PackageDatabaseHandler packageDatabaseHandler, AttachmentConnector attachmentConnector, User user) {
        this.projectDatabaseHandler = projectDatabaseHandler;
        this.componentDatabaseHandler = componentDatabaseHandler;
        this.packageDatabaseHandler = packageDatabaseHandler;
        this.attachmentConnector = attachmentConnector;
        this.user = user;
        this.repositoryURL = new RepositoryURL();
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
        Stream<org.cyclonedx.model.Component> baseStream = components.parallelStream()
                .filter(Objects::nonNull);

        if (CommonUtils.isNotNullEmptyOrWhitespace(SW360Utils.readConfig(NON_PKG_MANAGED_COMPS_PROP, ""))) {
            baseStream = baseStream.filter(comp -> !isCompNonPkgManaged(comp));
        }

        return baseStream
                .flatMap(comp -> CommonUtils.nullToEmptyList(comp.getExternalReferences()).stream()
                        .filter(ref -> ExternalReference.Type.VCS.equals(ref.getType()))
                        .map(ExternalReference::getUrl)
                        .filter(CommonUtils::isNotNullEmptyOrWhitespace)
                        .map(url->repositoryURL.processURL(url.trim()))
                        .map(url -> new AbstractMap.SimpleEntry<>(url, comp)))
                .collect(Collectors.groupingBy(
                        AbstractMap.SimpleEntry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                ));
    }

    @SuppressWarnings("unchecked")
    public RequestSummary importFromBOM(InputStream inputStream, AttachmentContent attachmentContent, String projectId, User user, boolean doNotReplacePackageAndRelease) {
        RequestSummary requestSummary = new RequestSummary();
        Map<String, String> messageMap = new HashMap<>();
        requestSummary.setRequestStatus(RequestStatus.FAILURE);
        String fileExtension = Files.getFileExtension(attachmentContent.getFilename());
        Parser parser;

        final UserGroup allowedUserGroup = SW360Utils.readConfig(SW360ConfigKeys.SBOM_IMPORT_EXPORT_ACCESS_USER_ROLE, UserGroup.USER);

        if (!SW360Utils.isUserAtleastDesiredRoleInPrimaryOrSecondaryGroup(user, allowedUserGroup)) {
            log.warn("User does not have permission to import the SBOM: " + user.getEmail());
            requestSummary.setMessage(allowedUserGroup.name());
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

            if (!SW360Utils.readConfig(IS_PACKAGE_PORTLET_ENABLED, true)) {
                vcsToComponentMap.put("", components);
                requestSummary = importSbomAsProject(compMetadata, vcsToComponentMap, new ArrayList<>(), projectId, attachmentContent, doNotReplacePackageAndRelease);
            } else {
                vcsToComponentMap = getVcsToComponentMap(components);
                List<org.cyclonedx.model.Component> nonPkgManagedComponents = components.stream()
                        .filter(Objects::nonNull)
                        .filter(this::isCompNonPkgManaged)
                        .collect(Collectors.toList());

                if (componentsCount == vcsCount) {
                    requestSummary = importSbomAsProject(compMetadata, vcsToComponentMap, nonPkgManagedComponents, projectId, attachmentContent, doNotReplacePackageAndRelease);
                } else if (componentsCount > vcsCount) {
                    requestSummary = importSbomAsProject(compMetadata, vcsToComponentMap, nonPkgManagedComponents, projectId, attachmentContent, doNotReplacePackageAndRelease);

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
                        Map<String, ProjectPackageRelationship> linkedPackages =  CommonUtils.isNullOrEmptyMap(project.getPackageIds()) ? new HashMap<>() : project.getPackageIds();
                        for (org.cyclonedx.model.Component comp : components) {
                            if (CommonUtils.isNullOrEmptyCollection(comp.getExternalReferences())
                                    || comp.getExternalReferences().stream().map(ExternalReference::getType).filter(typeFilter).count() == 0
                                    || !containsComp(vcsToComponentMap, comp) && !isCompNonPkgManaged(comp)) {

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
                                            if (project.getPackageIds() != null && !CommonUtils.nullToEmptySet(project.getPackageIds().keySet()).contains(pkgAddSummary.getId())) {
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
                                    linkedPackages.put(pkgAddSummary.getId(), createPackageNode(project, user, ""));
                                    project.setPackageIds(linkedPackages);
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
                            "SBOM import aborted with error: Multiple vcs information found in components, vcs found: %s and total components: %s",
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
            Map<String, List<org.cyclonedx.model.Component>> vcsToComponentMap, List<org.cyclonedx.model.Component> nonPkgManagedComponents, String projectId, AttachmentContent attachmentContent, boolean doNotReplacePackageAndRelease)
                    throws SW360Exception {
        final RequestSummary summary = new RequestSummary();
        summary.setRequestStatus(RequestStatus.FAILURE);

        Project project;
        AddDocumentRequestSummary projectAddSummary = new AddDocumentRequestSummary();
        AddDocumentRequestStatus addStatus;
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
            summary.setRequestStatus(RequestStatus.FAILURE);
            return summary;
        }

        if (SW360Utils.readConfig(IS_PACKAGE_PORTLET_ENABLED, true)) {
            messageMap = importAllComponentsAsPackages(vcsToComponentMap, nonPkgManagedComponents, project, doNotReplacePackageAndRelease);
        } else {
            messageMap = importAllComponentsAsReleases(vcsToComponentMap, project);
        }
        RequestStatus updateStatus = projectDatabaseHandler.updateProject(project, user, true);
        if (RequestStatus.SUCCESS.equals(updateStatus)) {
            log.info("project updated successfully: " + project.getId());
        } else {
            log.error("Failed to update project: " + project.getId() + " with status: " + updateStatus);
            summary.setMessage("Failed to update project after SBOM import!");
            summary.setRequestStatus(RequestStatus.FAILURE);
            return summary;
        }

        int importErrorCount = 0;
        String errorCountStr = messageMap.get(COMPONENT_IMPORT_ERROR_COUNT_KEY);
        if (CommonUtils.isNotNullEmptyOrWhitespace(errorCountStr)) {
            importErrorCount = Integer.parseInt(errorCountStr);
        }

        summary.setMessage(convertCollectionToJSONString(messageMap));
        if (importErrorCount > 0) {
            log.warn("SBOM import completed with " + importErrorCount + " component error(s) for project: " + project.getId());
        }
        summary.setRequestStatus(RequestStatus.SUCCESS);
        return summary;
    }

    private Map<String, String> importAllComponentsAsReleases(Map<String, List<org.cyclonedx.model.Component>> vcsToComponentMap, Project project) {

        final var countMap = new HashMap<String, Integer>();
        final Set<String> duplicateComponents = new HashSet<>();
        final Set<String> duplicateReleases = new HashSet<>();
        final Set<String> invalidReleases = new HashSet<>();
        final Set<String> invalidVcsComponents = new HashSet<>();
        final Map<String, ProjectReleaseRelationship> releaseRelationMap = CommonUtils.isNullOrEmptyMap(project.getReleaseIdToUsage()) ? new HashMap<>() : project.getReleaseIdToUsage();
        countMap.put(COMP_CREATION_COUNT_KEY, 0); countMap.put(COMP_REUSE_COUNT_KEY, 0);
        countMap.put(REL_CREATION_COUNT_KEY, 0); countMap.put(REL_REUSE_COUNT_KEY, 0);
        int compCreationCount = 0, compReuseCount = 0, relCreationCount = 0, relReuseCount = 0;
        int compImportErrorCount = 0;

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
                } else if (AddDocumentRequestStatus.INVALID_INPUT.equals(compAddSummary.getRequestStatus())) {
                    log.warn("Invalid VCS URL for component: " + comp.getName());
                    invalidVcsComponents.add(comp.getName()+ " (" + comp.getVcs() + ")");
                    continue;
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
                    compImportErrorCount++;
                    continue;
                }

                // update components specific fields
                comp = componentDatabaseHandler.getComponent(compAddSummary.getId(), user);
                if (null != bomComp.getType() && null == comp.getCdxComponentType()) {
                    comp.setCdxComponentType(getCdxComponentType(bomComp.getType()));
                }
                StringBuilder description = new StringBuilder();
                if (CommonUtils.isNullEmptyOrWhitespace(comp.getDescription()) && CommonUtils.isNotNullEmptyOrWhitespace(bomComp.getDescription())) {
                    description.append(bomComp.getDescription().trim());
                } else if (CommonUtils.isNotNullEmptyOrWhitespace(bomComp.getDescription())) {
                    description.append(" || ").append(bomComp.getDescription().trim());
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
                comp.setDescription(description.toString());
                RequestStatus updateStatus = componentDatabaseHandler.updateComponent(comp, user, true);
                if (RequestStatus.SUCCESS.equals(updateStatus)) {
                    log.info("updating component successfull: " + comp.getName());
                } else {
                    log.error("Failed to update component: " + comp.getName() + " with status: " + updateStatus);
                }
            } catch (SW360Exception e) {
                log.error("An error occured while creating/adding component from SBOM: " + e.getMessage());
                compImportErrorCount++;
                continue;
            }
        }

        project.setReleaseIdToUsage(releaseRelationMap);
        final Map<String, String> messageMap = new HashMap<>();
        messageMap.put(DUPLICATE_COMPONENT, String.join(JOINER, duplicateComponents));
        messageMap.put(DUPLICATE_RELEASE, String.join(JOINER, duplicateReleases));
        messageMap.put(INVALID_RELEASE, String.join(JOINER, invalidReleases));
        messageMap.put(INVALID_VCS_COMPONENT, String.join(JOINER, invalidVcsComponents));
        messageMap.put(PROJECT_ID, project.getId());
        messageMap.put(PROJECT_NAME, SW360Utils.getVersionedName(project.getName(), project.getVersion()));
        messageMap.put(COMP_CREATION_COUNT_KEY, String.valueOf(compCreationCount));
        messageMap.put(COMP_REUSE_COUNT_KEY, String.valueOf(compReuseCount));
        messageMap.put(REL_CREATION_COUNT_KEY, String.valueOf(relCreationCount));
        messageMap.put(REL_REUSE_COUNT_KEY, String.valueOf(relReuseCount));
        messageMap.put(COMPONENT_IMPORT_ERROR_COUNT_KEY, String.valueOf(compImportErrorCount));
        return messageMap;
    }

    private Map<String, String> importAllComponentsAsPackages(Map<String, List<org.cyclonedx.model.Component>> vcsToComponentMap,
                                                              List<org.cyclonedx.model.Component> nonPkgManagedComponents, Project project,
                                                              boolean doNotReplacePackageAndRelease) {
        final var countMap = new HashMap<String, Integer>();
        final Set<String> duplicateComponents = new HashSet<>();
        final Set<String> duplicateReleases = new HashSet<>();
        final Set<String> duplicatePackages = new HashSet<>();
        final Set<String> invalidReleases = new HashSet<>();
        final Set<String> nonPkgManagedCompWithoutVCS = new HashSet<>();
        final Set<String> invalidPackages = new HashSet<>();
        final Set<String> invalidVcsComponents = new HashSet<>();
        final Map<String, ProjectReleaseRelationship> releaseRelationMap = CommonUtils.isNullOrEmptyMap(project.getReleaseIdToUsage()) ? new HashMap<>() : project.getReleaseIdToUsage();
        final Set<String> projectPkgIds = CommonUtils.isNullOrEmptyMap(project.getPackageIds()) ? new HashSet<>() : project.getPackageIds().keySet();
        countMap.put(REL_CREATION_COUNT_KEY, 0); countMap.put(REL_REUSE_COUNT_KEY, 0);
        countMap.put(PKG_CREATION_COUNT_KEY, 0); countMap.put(PKG_REUSE_COUNT_KEY, 0);
        int relCreationCount = 0, relReuseCount = 0, pkgCreationCount = 0, pkgReuseCount = 0;
        int compImportErrorCount = 0;

        if (!doNotReplacePackageAndRelease) {
            releaseRelationMap.clear();
            projectPkgIds.clear();
            log.info("Cleared existing releases and packages for project: " + project.getName());
        }
        Map<String, ProjectPackageRelationship> linkedPackages = CommonUtils.isNullOrEmptyMap(project.getPackageIds()) ? new HashMap<>() : project.getPackageIds();
        for (Map.Entry<String, List<org.cyclonedx.model.Component>> entry : vcsToComponentMap.entrySet()) {
            Component comp = createComponent(entry.getKey());

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
                } else if (AddDocumentRequestStatus.INVALID_INPUT.equals(compAddSummary.getRequestStatus())) {
                    log.warn("Invalid VCS URL for component: " + comp.getName());
                    invalidVcsComponents.add(comp.getName() + " (" + comp.getVcs() + ")");
                    continue;
                } else {
                    // in case of more than 1 duplicate found, then continue and show error message in UI.
                    log.warn("found multiple components: " + comp.getName());
                    duplicateComponents.add(comp.getName());
                    continue;
                }

                for (org.cyclonedx.model.Component bomComp : entry.getValue()) {
                    Set<String> licenses = getLicenseFromBomComponent(bomComp);
                    Release release = createRelease(bomComp.getVersion(), comp, licenses);
                    if (CommonUtils.isNullEmptyOrWhitespace(release.getVersion()) ) {
                        log.error("release version is not present in SBoM for component: " + comp.getName());
                        invalidReleases.add(comp.getName());
                        continue;
                    }
                    String relName = SW360Utils.getVersionedName(release.getName(), release.getVersion());

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
                        releaseRelationMap.putIfAbsent(release.getId(), getDefaultRelation());
                    } catch (SW360Exception e) {
                        log.error("An error occured while creating/adding release from SBOM: " + e.getMessage());
                        compImportErrorCount++;
                        continue;
                    }

                    // update components specific fields
                    comp = componentDatabaseHandler.getComponent(compAddSummary.getId(), user);
                    if (null != bomComp.getType() && null == comp.getCdxComponentType()) {
                        comp.setCdxComponentType(getCdxComponentType(bomComp.getType()));
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
                    } else {
                        log.error("Failed to update component: " + comp.getName() + " with status: " + updateStatus);
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
                                if(!CommonUtils.nullToEmptyMap(project.getPackageIds()).containsKey(pkg.getId())){
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
                        linkedPackages.put(pkgAddSummary.getId(), createPackageNode(project, user, ""));
                        project.setPackageIds(linkedPackages);
                    } catch (SW360Exception e) {
                        log.error("An error occured while creating/adding package from SBOM: " + e.getMessage());
                        compImportErrorCount++;
                        continue;
                    }
                }
            } catch (SW360Exception e) {
                log.error("An error occured while creating/adding component from SBOM: " + e.getMessage());
                compImportErrorCount++;
                continue;
            }
        }

        for(org.cyclonedx.model.Component bomComp: nonPkgManagedComponents) {
            if (CommonUtils.isNullEmptyOrWhitespace(bomComp.getName())) {
                log.error("component name is not present in SBoM: " + project.getId());
                continue;
            }
            boolean hasVCS = !CommonUtils.isNullOrEmptyCollection(bomComp.getExternalReferences())
                    && bomComp.getExternalReferences().stream()
                    .map(ExternalReference::getType)
                    .anyMatch(typeFilter);

            if (hasVCS) {
                Component comp = createComponent(bomComp);
                AddDocumentRequestSummary compAddSummary;
                try {
                    compAddSummary = componentDatabaseHandler.addComponent(comp, user.getEmail());

                    if (CommonUtils.isNotNullEmptyOrWhitespace(compAddSummary.getId())) {
                        comp.setId(compAddSummary.getId());
                    } else {
                        // in case of more than 1 duplicate found, then continue and show error message in UI.
                        log.warn("found multiple components: " + comp.getName());
                        duplicateComponents.add(comp.getName());
                        continue;
                    }

                    Set<String> licenses = getLicenseFromBomComponent(bomComp);
                    Release release = createRelease(bomComp, comp, licenses);
                    if (CommonUtils.isNullEmptyOrWhitespace(release.getVersion()) ) {
                        log.error("release version is not present in SBoM for component: " + comp.getName());
                        invalidReleases.add(comp.getName());
                        continue;
                    }

                    String relName = SW360Utils.getVersionedName(release.getName(), release.getVersion());
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
                        releaseRelationMap.putIfAbsent(release.getId(), getDefaultRelation());
                    } catch (SW360Exception e) {
                        log.error("An error occured while creating/adding release from SBOM: " + e.getMessage());
                        compImportErrorCount++;
                        continue;
                    }

                    // update components specific fields
                    comp = componentDatabaseHandler.getComponent(compAddSummary.getId(), user);
                    if (null != bomComp.getType() && null == comp.getCdxComponentType()) {
                        comp.setCdxComponentType(getCdxComponentType(bomComp.getType()));
                    }
                    if (CommonUtils.isNotEmpty(comp.getMainLicenseIds())) {
                        comp.getMainLicenseIds().addAll(licenses);
                    } else {
                        comp.setMainLicenseIds(licenses);
                    }
                    RequestStatus updateStatus = componentDatabaseHandler.updateComponent(comp, user, true);
                    if (RequestStatus.SUCCESS.equals(updateStatus)) {
                        log.info("updating component successfull: " + comp.getName());
                    } else {
                        log.error("Failed to update component: " + comp.getName() + " with status: " + updateStatus);
                    }

                } catch (SW360Exception e) {
                    log.error("An error occured while creating/adding component from SBOM: " + e.getMessage());
                    compImportErrorCount++;
                }
            } else {
                nonPkgManagedCompWithoutVCS.add(bomComp.getName());
            }
        }

        project.setReleaseIdToUsage(releaseRelationMap);
        final Map<String, String> messageMap = new HashMap<>();
        messageMap.put(DUPLICATE_COMPONENT, String.join(JOINER, duplicateComponents));
        messageMap.put(DUPLICATE_RELEASE, String.join(JOINER, duplicateReleases));
        messageMap.put(DUPLICATE_PACKAGE, String.join(JOINER, duplicatePackages));
        messageMap.put(INVALID_RELEASE, String.join(JOINER, invalidReleases));
        messageMap.put(NON_PKG_MANAGED_COMP_WITHOUT_VCS, String.join(JOINER, nonPkgManagedCompWithoutVCS));
        messageMap.put(INVALID_PACKAGE, String.join(JOINER, invalidPackages));
        messageMap.put(INVALID_VCS_COMPONENT, String.join(JOINER, invalidVcsComponents));
        messageMap.put(PROJECT_ID, project.getId());
        messageMap.put(PROJECT_NAME, SW360Utils.getVersionedName(project.getName(), project.getVersion()));
        messageMap.put(REL_CREATION_COUNT_KEY, String.valueOf(relCreationCount));
        messageMap.put(REL_REUSE_COUNT_KEY, String.valueOf(relReuseCount));
        messageMap.put(PKG_CREATION_COUNT_KEY, String.valueOf(pkgCreationCount));
        messageMap.put(PKG_REUSE_COUNT_KEY, String.valueOf(pkgReuseCount));
        messageMap.put(COMPONENT_IMPORT_ERROR_COUNT_KEY, String.valueOf(compImportErrorCount));
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
        if (compMetadata == null) {
            log.error("Component metadata is null. Unable to create project.");
            throw new IllegalArgumentException("Unable to create project due to incorrect metadata");
        }
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

        String compVCS = CommonUtils.nullToEmptyList(componentFromBom.getExternalReferences()).stream()
                .filter(extRef -> ExternalReference.Type.VCS.equals(extRef.getType()))
                .map(ExternalReference::getUrl)
                .findFirst()
                .orElse("");

        if(compVCS.equalsIgnoreCase("cots")){
            component.setComponentType(ComponentType.COTS);
        } else if(compVCS.equalsIgnoreCase("freeware")) {
            component.setComponentType(ComponentType.FREESOFTWARE);
        } else {
            component.setComponentType(ComponentType.OSS);
            component.setVcs(repositoryURL.processURL(compVCS));
        }

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


    public static boolean containsComp(Map<String, List<org.cyclonedx.model.Component>> map, org.cyclonedx.model.Component element) {
        for (List<org.cyclonedx.model.Component> list : map.values()) {
            if (list.contains(element)) {
                return true;
            }
        }
        return false;
    }

    public boolean isCompNonPkgManaged(org.cyclonedx.model.Component comp) {
        List<Property> properties = CommonUtils.nullToEmptyList(comp.getProperties());
        return (!properties.isEmpty() && properties.stream().anyMatch(prop -> SW360Utils.readConfig(NON_PKG_MANAGED_COMPS_PROP, "").equals(prop.getName()) && "true".equalsIgnoreCase(prop.getValue())));
    }
    public  ProjectPackageRelationship createPackageNode(Project project, User user, String comment) {
        ProjectPackageRelationship projectPackageRelationship = new ProjectPackageRelationship();
        projectPackageRelationship.setCreatedBy(user.getEmail());
        projectPackageRelationship.setCreatedOn(SW360Utils.getCreatedOn());
        projectPackageRelationship.setComment(comment);
        return projectPackageRelationship;
    }
}

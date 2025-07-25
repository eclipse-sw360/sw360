/*
 * Copyright Siemens AG, 2017-2018.
 * Copyright Bosch Software Innovations GmbH, 2017.
 * Part of the SW360 Portal Project.
 * Copyright Ritankar Saha<ritankar.saha786@gmail.com>, 2025. Part of the SW360 Portal Project.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.attachment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.sw360.commonIO.AttachmentFrontendUtils;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.common.Duration;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.Source;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.ThriftUtils;
import org.eclipse.sw360.datahandler.thrift.attachments.*;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.SPDXDocumentService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.core.ThriftServiceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.CollectionModel;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.eclipse.sw360.datahandler.common.CommonUtils.isNullEmptyOrWhitespace;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyList;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotNull;
import static org.eclipse.sw360.datahandler.common.WrappedException.wrapSW360Exception;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class Sw360AttachmentService {
    @Value("${sw360.thrift-server-url:http://localhost:8080}")
    private String thriftServerUrl;

    @Value("${sw360.couchdb-url:http://localhost:5984}")
    private String couchdbUrl;

    @NonNull
    private final RestControllerHelper restControllerHelper;

    private static final Logger log = LogManager.getLogger(Sw360AttachmentService.class);

    @NonNull
    private final ThriftServiceProvider<AttachmentService.Iface> thriftAttachmentServiceProvider;

    private final Duration downloadTimeout = Duration.durationOf(30, TimeUnit.SECONDS);
    private AttachmentConnector attachmentConnector;

    public List<AttachmentUsage> getAttachemntUsages(String projectId) throws TException {
        AttachmentService.Iface attachmentClient = getThriftAttachmentClient();
        return attachmentClient.getUsedAttachments(Source.projectId(projectId),
                UsageData.licenseInfo(new LicenseInfoUsage(Sets.newHashSet())));
    }

    public AttachmentInfo getAttachmentById(String id) throws TException {
        try {
            AttachmentService.Iface attachmentClient = getThriftAttachmentClient();
            List<Attachment> attachments = attachmentClient.getAttachmentsByIds(Collections.singleton(id));
            if (attachments.isEmpty()) {
                throw new ResourceNotFoundException("Attachment not found with ID: " + id);
            }
            return createAttachmentInfo(attachmentClient, attachments.get(0));
        } catch (TException e) {
            log.error("Error retrieving attachment by ID {}: {}", id, e.getMessage(), e);
            throw new TException("Failed to retrieve attachment: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error retrieving attachment by ID {}: {}", id, e.getMessage(), e);
            throw new TException("Internal error processing getAttachmentsByIds: " + e.getMessage());
        }
    }

    public List<AttachmentUsage> getAttachmentUseById(String id) throws TException {
        AttachmentService.Iface attachmentClient = getThriftAttachmentClient();
        return attachmentClient.getUsedAttachmentsById(id);
    }

    public List<AttachmentInfo> getAttachmentsBySha1(String sha1) throws TException {
        try {
            if (sha1 == null || sha1.trim().isEmpty()) {
                log.warn("Empty or null SHA1 provided");
                return Collections.emptyList();
            }
            
            AttachmentService.Iface attachmentClient = getThriftAttachmentClient();
            List<Attachment> attachments = attachmentClient.getAttachmentsBySha1s(Collections.singleton(sha1.trim()));
            return createAttachmentInfos(attachmentClient, attachments != null ? attachments : Collections.emptyList());
        } catch (TException e) {
            log.error("Error retrieving attachments by SHA1 {}: {}", sha1, e.getMessage(), e);
            throw new TException("Failed to retrieve attachments by SHA1: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error retrieving attachments by SHA1 {}: {}", sha1, e.getMessage(), e);
            throw new TException("Internal error processing getAttachmentsBySha1s: " + e.getMessage());
        }
    }

    public Map<String, List<AttachmentInfo>> getAttachmentsBySha1s(Collection<String> sha1s) throws TException {
        try {
            if (sha1s == null || sha1s.isEmpty()) {
                log.warn("Empty or null SHA1 collection provided");
                return Collections.emptyMap();
            }
            
            // Filter out null/empty SHA1s
            Set<String> validSha1s = sha1s.stream()
                    .filter(sha1 -> sha1 != null && !sha1.trim().isEmpty())
                    .map(String::trim)
                    .collect(Collectors.toSet());
                    
            if (validSha1s.isEmpty()) {
                log.warn("No valid SHA1s provided after filtering");
                return Collections.emptyMap();
            }
            
            AttachmentService.Iface attachmentClient = getThriftAttachmentClient();
            List<Attachment> attachments = attachmentClient.getAttachmentsBySha1s(validSha1s);
            
            // Group attachments by SHA1
            Map<String, List<AttachmentInfo>> result = new HashMap<>();
            for (String sha1 : validSha1s) {
                result.put(sha1, new ArrayList<>());
            }
            
            if (attachments != null) {
                for (Attachment attachment : attachments) {
                    String sha1 = attachment.getSha1();
                    if (sha1 != null && result.containsKey(sha1)) {
                        result.get(sha1).add(createAttachmentInfo(attachmentClient, attachment));
                    }
                }
            }
            
            return result;
        } catch (TException e) {
            log.error("Error retrieving attachments by SHA1s {}: {}", sha1s, e.getMessage(), e);
            throw new TException("Failed to retrieve attachments by SHA1s: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error retrieving attachments by SHA1s {}: {}", sha1s, e.getMessage(), e);
            throw new TException("Internal error processing getAttachmentsBySha1s: " + e.getMessage());
        }
    }

    /**
     * Filters the attachments that can be actually removed before a delete
     * attachments operation. This method can be called by controllers to
     * handle a request to delete attachments. For each attachment to be
     * deleted, it checks whether all criteria are fulfilled: The attachment
     * must belong to the given owner, it must not be in use by a project, and
     * its checked status must not be ACCEPTED. The resulting set contains all
     * the attachments that are safe to be removed.
     *
     * @param owner          the {@code Source} referencing the attachment owner
     * @param allAttachments the full set of attachments of the owner
     * @param idsToDelete    collection with the IDs of the attachments to delete
     * @return the filtered set with attachments that can be removed
     */
    public Set<Attachment> filterAttachmentsToRemove(Source owner, Set<Attachment> allAttachments,
                                                     Collection<String> idsToDelete) throws TException {
        Map<String, Attachment> knownAttachmentIds = allAttachments.stream()
                .collect(Collectors.toMap(Attachment::getAttachmentContentId, v -> v));
        AttachmentService.Iface attachmentService = getThriftAttachmentClient();

        return idsToDelete.stream()
                .map(knownAttachmentIds::get)
                .filter(Objects::nonNull)
                .filter(attachment -> canDeleteAttachment(attachmentService, owner, attachment))
                .collect(Collectors.toSet());
    }

    private static boolean canDeleteAttachment(AttachmentService.Iface attachmentService, Source owner,
                                               Attachment attachment) {
        String id = attachment.getAttachmentContentId();
        if (attachment.getCheckStatus() == CheckStatus.ACCEPTED) {
            log.warn("Attachment " + id + " must not be deleted as it is in status checked.");
            return false;
        }

        try {
            List<AttachmentUsage> usages = attachmentService.getAttachmentUsages(owner, id, null);
            if (usages.stream().anyMatch(usage -> usage.usedBy.isSetProjectId())) {
                log.warn("Attachment " + id + " must not be deleted as it is used by a project.");
                return false;
            }
            return true;
        } catch (TException e) {
            log.error("Could not check attachment usage for " + id);
            return false;
        }
    }

    private AttachmentInfo createAttachmentInfo(AttachmentService.Iface attachmentClient, Attachment attachment)
            throws TException {
        AttachmentInfo attachmentInfo = new AttachmentInfo(attachment);
        attachmentInfo.setOwner(attachmentClient
                .getAttachmentOwnersByIds(Collections.singleton(attachment.getAttachmentContentId())).get(0));
        return attachmentInfo;
    }

    private List<AttachmentInfo> createAttachmentInfos(AttachmentService.Iface attachmentClient,
            List<Attachment> attachments) throws TException {
        List<AttachmentInfo> attachmentInfos = new ArrayList<>();
        for (Attachment attachment : attachments) {
            attachmentInfos.add(createAttachmentInfo(attachmentClient, attachment));
        }
        return attachmentInfos;
    }

    public void downloadAttachmentWithContext(Object context, String attachmentId, HttpServletResponse response, User sw360User) {
        AttachmentContent attachmentContent = getAttachmentContent(attachmentId);

        String filename = attachmentContent.getFilename();
        String contentType = attachmentContent.getContentType();

        try (InputStream attachmentStream = getStreamToAttachments(Collections.singleton(attachmentContent), sw360User, context)) {
            response.setContentType(contentType);
            response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", filename));
            FileCopyUtils.copy(attachmentStream, response.getOutputStream());
        } catch (TException | IOException e) {
            log.error(e.getMessage());
        }
    }

    public void downloadAttachmentBundleWithContext (Object context, Set<Attachment> attachments, User user, HttpServletResponse response) throws TException, IOException {
        if (CommonUtils.isNullOrEmptyCollection(attachments)) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }
        List<File> files = new ArrayList<>();
        for (Attachment attachment : attachments) {
            AttachmentContent attachmentContent = getAttachmentContent(attachment.getAttachmentContentId());
            InputStream inputStream = getStreamToAttachments(Collections.singleton(attachmentContent), user, context);
            String fileType = getFileType(attachmentContent.getFilename());
            File sourceFile = saveAsTempFile(inputStream, attachment.getAttachmentContentId(), fileType);
            File file = renameFile(sourceFile, attachment.getFilename());
            files.add(file);
            FileUtils.delete(sourceFile);
        }
        response.setStatus(HttpServletResponse.SC_OK);
        response.addHeader("Content-Disposition", "attachment; filename=\"AttachmentBundle.zip\"");
        ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream());
        for (File file : files) {
            zipOutputStream.putNextEntry(new ZipEntry(file.getName()));
            FileInputStream fileInputStream = new FileInputStream(file);
            IOUtils.copy(fileInputStream, zipOutputStream);
            fileInputStream.close();
            zipOutputStream.closeEntry();
        }
        zipOutputStream.close();
    }

    public <T> InputStream getStreamToAttachments(Set<AttachmentContent> attachments, User sw360User, T context) throws IOException, TException {
        return new AttachmentFrontendUtils().getStreamToServeAFile(attachments, sw360User, context);
    }

    public Attachment uploadAttachment(MultipartFile file, Attachment newAttachment, User sw360User) throws IOException, TException {
        String fileName = file.getOriginalFilename(); // TODO: shouldn't the fileName be taken from newAttachment?
        String contentType = file.getContentType();
        final AttachmentContent attachmentContent = makeAttachmentContent(fileName, contentType);

        final AttachmentConnector attachmentConnector = getConnector();
        Attachment attachment = new AttachmentFrontendUtils().uploadAttachmentContent(attachmentContent, file.getInputStream(), sw360User);
        
        // Compute all checksums for the uploaded attachment
        attachmentConnector.setAllChecksumsForAttachment(attachment);

        AttachmentType attachmentType = newAttachment.getAttachmentType();
        if (attachmentType != null) {
            attachment.setAttachmentType(attachmentType);
        }

        CheckStatus checkStatus = newAttachment.getCheckStatus();
        if (checkStatus != null) {
            attachment.setCheckStatus(checkStatus);
        }

        String createdComment = newAttachment.getCreatedComment();
        if (createdComment != null) {
            attachment.setCreatedComment(createdComment);
        }

        if (checkStatus != null &&
                (checkStatus == CheckStatus.ACCEPTED || checkStatus == CheckStatus.REJECTED))
        {
            if (CommonUtils.isNotNullEmptyOrWhitespace(attachment.getCreatedBy())) {
                attachment.setCheckedBy(attachment.getCreatedBy());
            }

            if (CommonUtils.isNotNullEmptyOrWhitespace(attachment.getCreatedTeam())) {
                attachment.setCheckedTeam(attachment.getCreatedTeam());
            }
        }

        return attachment;
    }

    public Attachment addAttachment(MultipartFile file, User sw360User) throws IOException, TException {
        String fileName = file.getOriginalFilename();
        String contentType = file.getContentType();
        final AttachmentContent attachmentContent = makeAttachmentContent(fileName, contentType);
        final AttachmentConnector attachmentConnector = getConnector();
        Attachment attachment = new AttachmentFrontendUtils().uploadAttachmentContent(attachmentContent, file.getInputStream(), sw360User);
        
        // Compute all checksums for the uploaded attachment
        attachmentConnector.setAllChecksumsForAttachment(attachment);

        return attachment;
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

    public AttachmentContent getAttachmentContent(String id) {
        try {
            return new AttachmentFrontendUtils().getAttachmentContent(id);
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }

    public CollectionModel<EntityModel<Attachment>> getAttachmentResourcesFromList(User user, Set<Attachment> attachments, Source owner) throws TTransportException {
        Set<Attachment> attachmentsWithUsages = getAttachmentsWithUsages(user, attachments, owner);

        final List<EntityModel<Attachment>> attachmentResources = new ArrayList<>();
        if (CommonUtils.isNotEmpty(attachmentsWithUsages)) {
            for (final Attachment attachment : attachmentsWithUsages) {
                final EntityModel<Attachment> attachmentResource = EntityModel.of(attachment);
                attachmentResources.add(attachmentResource);
            }
        }
        return CollectionModel.of(attachmentResources);
    }

    public CollectionModel<EntityModel<Attachment>> getResourcesFromList(Set<Attachment> attachmentList) {
        final List<EntityModel<Attachment>> attachmentResources = new ArrayList<>();
        if (CommonUtils.isNotEmpty(attachmentList)) {
            for (final Attachment attachment : attachmentList) {
                final Attachment embeddedAttachment = restControllerHelper.convertToEmbeddedAttachment(attachment);
                final EntityModel<Attachment> attachmentResource = EntityModel.of(embeddedAttachment);
                attachmentResources.add(attachmentResource);
            }
        }
        return CollectionModel.of(attachmentResources);
    }

    public List<AttachmentUsage> getAllAttachmentUsage(String projectId) throws TException {
        AttachmentService.Iface attachmentClient = getThriftAttachmentClient();
        return attachmentClient.getUsedAttachments(Source.projectId(projectId), null);
    }

    public Attachment updateAttachment(Set<Attachment> attachments, Attachment newData, String attachmentId, User user) {
        if (CommonUtils.isNotEmpty(attachments)) {
            Optional<Attachment> matchingAttachment = attachments.stream()
                    .filter(att -> att.attachmentContentId.equals(attachmentId)).findFirst();
            if (matchingAttachment.isPresent()) {
                Attachment actualAtt = matchingAttachment.get();
                updateAttachment(actualAtt, newData, user);
                return actualAtt;
            }
        }
        throw new ResourceNotFoundException("Requested Attachment Not Found");
    }

    private AttachmentConnector getConnector() throws SW360Exception {
        if (attachmentConnector == null) makeConnector();
        return attachmentConnector;
    }

    private synchronized void makeConnector() throws SW360Exception {
        if (attachmentConnector == null) {
            try {
                attachmentConnector = new AttachmentConnector(DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_ATTACHMENTS, downloadTimeout);
            } catch (MalformedURLException e) {
                log.error("Invalid database address received...", e);
                throw new SW360Exception(e.getMessage());
            }
        }
    }

    private AttachmentService.Iface getThriftAttachmentClient() throws TTransportException {
        return thriftAttachmentServiceProvider.getService(thriftServerUrl);
    }

    private void updateAttachment(Attachment attachmentToUpdate, Attachment reqBodyAttachment, User user) {
        AttachmentType attachmentType = reqBodyAttachment.getAttachmentType();
        String createdComment = reqBodyAttachment.getCreatedComment();
        CheckStatus checkStatus = reqBodyAttachment.getCheckStatus();
        if (attachmentType != null) {
            attachmentToUpdate.setAttachmentType(attachmentType);
        }
        if (createdComment != null) {
            attachmentToUpdate.setCreatedComment(createdComment);
        }
        if (checkStatus != null) {
            attachmentToUpdate.setCheckStatus(checkStatus);
            String checkedComment = reqBodyAttachment.getCheckedComment();
            if (checkStatus != CheckStatus.NOTCHECKED) {
                if (checkedComment != null) {
                    attachmentToUpdate.setCheckedComment(checkedComment);
                }
                attachmentToUpdate.setCheckedBy(user.getEmail());
                attachmentToUpdate.setCheckedTeam(user.getDepartment());
                attachmentToUpdate.setCheckedOn(SW360Utils.getCreatedOn());
            } else {
                attachmentToUpdate.unsetCheckedBy();
                attachmentToUpdate.unsetCheckedTeam();
                attachmentToUpdate.setCheckedComment("");
                attachmentToUpdate.unsetCheckedOn();
            }
        }
    }

    public File saveAsTempFile(InputStream inputStream, String prefix, String suffix) throws IOException {
        final File tempFile = File.createTempFile(prefix, suffix);
        tempFile.deleteOnExit();
        // Set append to false, overwrite if file existed
        try (FileOutputStream outputStream = new FileOutputStream(tempFile, false)) {
            IOUtils.copy(inputStream, outputStream);
        }
        return tempFile;
    }

    private String getFileType(String fileName) {
        if (isNullEmptyOrWhitespace(fileName) || !fileName.contains(".")) {
            log.error("Can not get file type from file name - no file extension");
            return null;
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    private File renameFile(File sourceFile, String filename) throws IOException {
        String pathFile = sourceFile.getPath().substring(0,sourceFile.getPath().lastIndexOf("/"));
        StringBuilder newName = new StringBuilder(pathFile);
        newName.append("/");
        newName.append(filename);
        File file = new File(newName.toString());
        FileUtils.copyFile(sourceFile, file);
        return file;
    }

    private Set<Attachment> getAttachmentsWithUsages(User user, Set<Attachment> attachments, Source owner) throws TTransportException {
        Set<Attachment> atts = CommonUtils.nullToEmptySet(attachments);
        Set<String> attachmentContentIds = atts.stream().map(Attachment::getAttachmentContentId).collect(Collectors.toSet());
        Map<String, Long> restrictedProjectsCountsByContentId = getRestrictedProjectsCountsByContentId(attachmentContentIds, user, owner);

        for (Attachment attachment : atts) {
            try {
                List<AttachmentUsage> attachmentUsages = getAttachmentUseById(attachment.getAttachmentContentId());
                long numberProjectByAttachmentUsages = distinctProjectIdsFromAttachmentUsages(attachmentUsages).count();
                Set<ProjectUsage> projectUsages = getProjectAttachmentUsages(attachmentUsages, user);

                ProjectAttachmentUsage usage = new ProjectAttachmentUsage();
                usage.setVisible(numberProjectByAttachmentUsages);
                usage.setRestricted(restrictedProjectsCountsByContentId.getOrDefault(attachment.getAttachmentContentId(), 0L));
                usage.setProjectUsages(projectUsages);
                attachment.setProjectAttachmentUsage(usage);
            } catch (TException e) {
                log.error(e.getMessage());
            }
        }
        return atts;
    }

    private Map<String, Long> getRestrictedProjectsCountsByContentId(Set<String> attachmentContentIds, User user, Source owner) throws TTransportException {
        AttachmentService.Iface client = getThriftAttachmentClient();
        Map<String, Long> restrictedProjectsCountsByContentId = new HashMap<>();
        try {
            Map<String, List<AttachmentUsage>> attachmentUsagesByContentId =
                    client.getAttachmentsUsages(owner, attachmentContentIds, null)
                            .stream()
                            .collect(Collectors.groupingBy(AttachmentUsage::getAttachmentContentId));

            Map<String, List<Project>> usingProjectsByContentId = fetchUsingProjectsForAttachmentUsages(attachmentUsagesByContentId, user);
            restrictedProjectsCountsByContentId = countRestrictedProjectsByContentId(attachmentUsagesByContentId, usingProjectsByContentId);
        } catch (TException e) {
            log.error("Cannot load restricted projects counts by contentId", e);
        }
        return restrictedProjectsCountsByContentId;
    }

    private  Set<ProjectUsage> getProjectAttachmentUsages(List<AttachmentUsage> attachmentUsages, User user) {
        Set<ProjectUsage> projectUsages = new HashSet<>();
        attachmentUsages.stream().forEach(attachmentUsage -> {
            try {
                Project project = getThriftProjectClient().getProjectById(attachmentUsage.getUsedBy().getProjectId(), user);

                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(project.getName());
                stringBuilder.append("(");
                stringBuilder.append(project.getVersion());
                stringBuilder.append(")");

                ProjectUsage projectUsage = new ProjectUsage();
                projectUsage.setProjectId(attachmentUsage.getUsedBy().getProjectId());
                projectUsage.setProjectName(stringBuilder.toString());

                projectUsages.add(projectUsage);
            } catch (TException e) {
                log.error("Cannot load project name attachment usages", e);
            }
        });
        return projectUsages;
    }

    private Map<String, List<Project>> fetchUsingProjectsForAttachmentUsages(Map<String, List<AttachmentUsage>> attachmentUsagesByContentId, User user) throws TException {
        List<String> projectIds = attachmentUsagesByContentId.values().stream()
                .flatMap(Collection::stream)
                .map(AttachmentUsage::getUsedBy)
                .map(Source::getProjectId)
                .distinct().collect(Collectors.toList());

        List<Project> usingProjectsList = getThriftProjectClient().getProjectsById(projectIds, user);
        Map<String, Project> usingProjects = ThriftUtils.getIdMap(usingProjectsList);
        return Maps.transformValues(
                attachmentUsagesByContentId,
                attUsages -> distinctProjectIdsFromAttachmentUsages(attUsages)
                        .map(usingProjects::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
    }

    private ProjectService.Iface getThriftProjectClient() throws TTransportException {
        THttpClient thriftClient = new THttpClient(thriftServerUrl + "/projects/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        return new ProjectService.Client(protocol);
    }

    private SPDXDocumentService.Iface getThriftSpdxDocumentClient() {
        ThriftClients thriftClients = new ThriftClients();
        return thriftClients.makeSPDXClient();
    }

    private Stream<String> distinctProjectIdsFromAttachmentUsages (List<AttachmentUsage> usages){
        return nullToEmptyList(usages).stream()
                .map(AttachmentUsage::getUsedBy)
                .map(Source::getProjectId)
                .distinct();
    }

    private Map<String, Long> countRestrictedProjectsByContentId(Map<String, List<AttachmentUsage>> attachmentUsagesByContentId, Map<String, List<Project>> usingProjectsByContentId) {
        return attachmentUsagesByContentId.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> distinctProjectIdsFromAttachmentUsages(entry.getValue())
                                .count() - usingProjectsByContentId.get(entry.getKey()).size()
                ));
    }

    private AttachmentContent getAttachmentContentForId(String id) throws TException {
        AttachmentService.Iface attachmentClient = getThriftAttachmentClient();
        return attachmentClient.getAttachmentContentById(id);
    }

    public boolean isAttachmentContentExist(String id) {
        try {
            AttachmentContent attachmentContent = getAttachmentContentForId(id);
            return attachmentContent != null;
        } catch (ResourceNotFoundException | TException notFoundException) {
            log.error(notFoundException.getMessage());
            return false;
        }
    }

    public Set<Attachment> getAttachmentsFromRequest(Object attachmentData, ObjectMapper mapper) {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        if (null == attachmentData) {
            return null;
        }
        Set<Attachment> attachments = mapper.convertValue(attachmentData,
                mapper.getTypeFactory().constructCollectionType(LinkedHashSet.class, Attachment.class));
        return attachments.stream()
                .map(attachment -> wrapSW360Exception(() -> {
                    boolean isAttachmentExist = isAttachmentContentExist(attachment.getAttachmentContentId());
                    if (!isAttachmentExist) {
                        throw new ResourceNotFoundException("Attachment " + attachment.getAttachmentContentId() + " not found.");
                    }
                    fillCheckedAttachmentData(attachment, sw360User);
                    return attachment;
                }))
                .collect(Collectors.toSet());
    }

    public void fillCheckedAttachmentData(Attachment attachment, User user) throws SW360Exception {
        assertNotNull(attachment);
        if (attachment.getCheckStatus() == null) {
            attachment.setCheckStatus(CheckStatus.NOTCHECKED);
        }
        if (!CheckStatus.NOTCHECKED.equals(attachment.getCheckStatus())) {
            attachment.setCheckedBy(attachment.getCheckedBy() != null ? attachment.getCheckedBy() : user.getEmail());
            attachment.setCheckedTeam(attachment.getCheckedTeam() != null ? attachment.getCheckedTeam() : user.getDepartment());
            attachment.setCheckedOn(attachment.getCheckedOn() != null ? attachment.getCheckedOn() : SW360Utils.getCreatedOn());
        } else {
            attachment.unsetCheckedBy();
            attachment.unsetCheckedTeam();
            attachment.setCheckedComment("");
            attachment.unsetCheckedOn();
        }
    }

    public boolean isValidSbomFile(MultipartFile file, String type) throws TException {
        if (file == null || file.isEmpty()) {
            return false;
        }

        String fileExtension = getFileType(file.getOriginalFilename());
        if (isNullEmptyOrWhitespace(fileExtension)) {
            return false;
        }

        SPDXDocumentService.Iface spdxClient = getThriftSpdxDocumentClient();
        try (InputStream inputStream = file.getInputStream()) {
            ByteBuffer fileBuffer = ByteBuffer.wrap(inputStream.readAllBytes());
            return spdxClient.isValidSbomFile(fileBuffer, type, fileExtension);
        } catch (IOException e) {
            log.error("Error reading file", e);
            return false;
        }
    }

    /**
     * Validate attachment checksums against stored values
     */
    public boolean validateAttachmentChecksums(Attachment attachment) throws SW360Exception {
        final AttachmentConnector attachmentConnector = getConnector();
        return attachmentConnector.validateAttachmentChecksums(attachment);
    }

    /**
     * Recompute and update all checksums for an attachment
     */
    public void recomputeAttachmentChecksums(Attachment attachment) throws SW360Exception {
        final AttachmentConnector attachmentConnector = getConnector();
        attachmentConnector.setAllChecksumsForAttachment(attachment);
    }
}

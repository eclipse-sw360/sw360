/*
 * Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.attachment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.Source;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.component.ComponentController;
import org.eclipse.sw360.rest.resourceserver.component.Sw360ComponentService;
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.project.ProjectController;
import org.eclipse.sw360.rest.resourceserver.project.Sw360ProjectService;
import org.eclipse.sw360.rest.resourceserver.release.Sw360ReleaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@BasePathAwareController
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RestController
@SecurityRequirement(name = "tokenAuth")
@SecurityRequirement(name = "basic")
public class AttachmentController implements RepresentationModelProcessor<RepositoryLinksResource> {
    public static final String ATTACHMENTS_URL = "/attachments";

    @NonNull
    private final Sw360AttachmentService attachmentService;

    @NonNull
    private final Sw360ProjectService projectService;

    @NonNull
    private final Sw360ReleaseService releaseService;

    @NonNull
    private final Sw360ComponentService componentService;

    @NonNull
    private final RestControllerHelper restControllerHelper;

    @Operation(
            summary = "Get attachment information.",
            description = "Get attachment information.",
            tags = {"Attachments"}
    )
    @GetMapping(value = ATTACHMENTS_URL + "/{id}")
    public ResponseEntity<EntityModel<Attachment>> getAttachmentForId(
            @Parameter(description = "id of the attachment")
            @PathVariable("id") String id
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);
        AttachmentInfo attachmentInfo = attachmentService.getAttachmentById(id);
        HalResource<Attachment> attachmentResource = createHalAttachment(attachmentInfo, sw360User);
        return new ResponseEntity<>(attachmentResource, HttpStatus.OK);
    }

    @Operation(
            summary = "Get attachment information by sha1.",
            description = "Get attachment information by sha1 and the resource having it.",
            tags = {"Attachments"}
    )
    @GetMapping(value = ATTACHMENTS_URL)
    public ResponseEntity<CollectionModel<EntityModel<Attachment>>> getAttachments(
            @Parameter(description = "sha1 of the attachment", required = true)
            @RequestParam String sha1
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);
        List<AttachmentInfo> attachmentInfos = attachmentService.getAttachmentsBySha1(sha1);

        List<EntityModel<Attachment>> attachmentResources = new ArrayList<>();
        for (AttachmentInfo sw360Attachment : attachmentInfos) {
            HalResource<Attachment> attachmentResource = createHalAttachment(sw360Attachment, sw360User);
            attachmentResources.add(attachmentResource);
        }
        CollectionModel<EntityModel<Attachment>> resources;
        if (!attachmentResources.isEmpty()) {
            resources = CollectionModel.of(attachmentResources);
            return new ResponseEntity<>(resources, HttpStatus.OK);
        } else {
            return new ResponseEntity(attachmentResources, HttpStatus.NO_CONTENT);
        }
    }

    @Operation(
            summary = "Create attachment.",
            description = "Create an attachment.",
            tags = {"Attachments"}
    )
    @RequestMapping(value = ATTACHMENTS_URL , method = RequestMethod.POST, consumes = {MediaType.MULTIPART_MIXED_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<CollectionModel<EntityModel<Attachment>>> createAttachment(
            @Parameter(description = "List of files to attach",
                    schema = @Schema(
                            type = "string",
                            format = "binary",
                            description = "File to attach"
                    )
            )
            @RequestParam("files") List<MultipartFile> files
    ) throws TException, IOException {
        if (files == null || files.isEmpty()) {
            throw new RuntimeException("You must select at least one file for uploading");
        }
        final User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        List<EntityModel<Attachment>> attachments = new ArrayList<>();
        for (MultipartFile file: files) {
            Attachment attachment = attachmentService.addAttachment(file, sw360User);
            attachments.add(EntityModel.of(attachment));
        }
        CollectionModel<EntityModel<Attachment>> attachmentsResponse = CollectionModel.of(attachments);
        return new ResponseEntity<>(attachmentsResponse, HttpStatus.OK);
    }

    private HalResource<Attachment> createHalAttachment(AttachmentInfo attachmentInfo, User sw360User) throws TException {
        HalResource<Attachment> halAttachment = new HalResource<>(attachmentInfo.getAttachment());
        Source owner = attachmentInfo.getOwner();
        String attachmentId = attachmentInfo.getAttachment().getAttachmentContentId();
        Link downloadLink = null;

        switch (owner.getSetField()) {
            case PROJECT_ID:
                Project sw360Project = projectService.getProjectForUserById(owner.getProjectId(), sw360User);
                restControllerHelper.addEmbeddedProject(halAttachment, sw360Project, false);
                downloadLink = linkTo(ProjectController.class).slash("/api/projects/" + sw360Project.getId() + "/attachments/" + attachmentId).withRel("downloadLink");
                break;
            case COMPONENT_ID:
                Component sw360Component = componentService.getComponentForUserById(owner.getComponentId(), sw360User);
                restControllerHelper.addEmbeddedComponent(halAttachment, sw360Component);
                downloadLink = linkTo(ComponentController.class).slash("/api/components/" + sw360Component.getId() + "/attachments/" + attachmentId).withRel("downloadLink");
                break;
            case RELEASE_ID:
                Release sw360Release = releaseService.getReleaseForUserById(owner.getReleaseId(), sw360User);
                restControllerHelper.addEmbeddedRelease(halAttachment, sw360Release);
                downloadLink = linkTo(ComponentController.class).slash("/api/releases/" + sw360Release.getId() + "/attachments/" + attachmentId).withRel("downloadLink");
                break;
        }

        halAttachment.add(downloadLink);

        if (sw360User != null) {
            restControllerHelper.addEmbeddedUser(halAttachment, sw360User, "createdBy");
        }

        return halAttachment;
    }

    @Operation(
            summary = "Find duplicate attachments by checksum.",
            description = "Find duplicate attachments across the system using checksum matching.",
            tags = {"Attachments", "Reuse"}
    )
    @PostMapping(value = ATTACHMENTS_URL + "/duplicates", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> findDuplicateAttachments(
            @Parameter(description = "Request containing checksums to search for duplicates")
            @RequestBody DuplicateSearchRequest request
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);
        
        Map<String, Object> result = new HashMap<>();
        
        List<String> checksums = request.getChecksums();
        if (checksums == null || checksums.isEmpty()) {
            result.put("error", "No checksums provided");
            return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
        }
        
        try {
            // Find duplicates by searching attachments with provided checksums using bulk method
            Map<String, List<AttachmentInfo>> allResults = attachmentService.getAttachmentsBySha1s(checksums);
            Map<String, List<AttachmentInfo>> duplicates = new HashMap<>();
            
            // Filter to keep only checksums that have more than one attachment (duplicates)
            for (Map.Entry<String, List<AttachmentInfo>> entry : allResults.entrySet()) {
                if (entry.getValue().size() > 1) {
                    duplicates.put(entry.getKey(), entry.getValue());
                }
            }
            
            result.put("duplicates", duplicates);
            result.put("totalChecksums", checksums.size());
            result.put("duplicateCount", duplicates.size());
            result.put("searchedBy", sw360User.getEmail());
            result.put("timestamp", System.currentTimeMillis());
            
            return new ResponseEntity<>(result, HttpStatus.OK);
            
        } catch (TException e) {
            log.error("Thrift error finding duplicate attachments: {}", e.getMessage(), e);
            result.put("error", "Service error: " + e.getMessage());
            result.put("errorType", "SERVICE_ERROR");
            return new ResponseEntity<>(result, HttpStatus.SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            log.error("Unexpected error finding duplicate attachments: {}", e.getMessage(), e);
            result.put("error", "Failed to find duplicate attachments: " + e.getMessage());
            result.put("errorType", "INTERNAL_ERROR");
            return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Get reuse statistics for attachments.",
            description = "Get statistics about potential reuse of attachments based on checksums.",
            tags = {"Attachments", "Reuse"}
    )
    @GetMapping(value = ATTACHMENTS_URL + "/reuse/statistics", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getReuseStatistics(
            @Parameter(description = "List of checksums to analyze for reuse potential")
            @RequestParam(required = false) List<String> checksums,
            @Parameter(description = "Limit number of results")
            @RequestParam(defaultValue = "100") int limit
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);
        
        Map<String, Object> statistics = new HashMap<>();
        
        try {
            if (checksums != null && !checksums.isEmpty()) {
                // Analyze specific checksums using bulk method
                List<String> limitedChecksums = checksums.subList(0, Math.min(checksums.size(), limit));
                Map<String, List<AttachmentInfo>> allResults = attachmentService.getAttachmentsBySha1s(limitedChecksums);
                
                Map<String, Integer> reuseCount = new HashMap<>();
                int totalReusable = 0;
                
                for (Map.Entry<String, List<AttachmentInfo>> entry : allResults.entrySet()) {
                    if (entry.getValue().size() > 1) {
                        reuseCount.put(entry.getKey(), entry.getValue().size());
                        totalReusable++;
                    }
                }
                
                double reusePercentage = limitedChecksums.size() > 0 ? 
                        (double) totalReusable / limitedChecksums.size() * 100.0 : 0.0;
                
                statistics.put("analyzedChecksums", limitedChecksums.size());
                statistics.put("reusableChecksums", totalReusable);
                statistics.put("reusePercentage", Math.round(reusePercentage * 100.0) / 100.0);
                statistics.put("reuseDetails", reuseCount);
            } else {
                // General statistics
                statistics.put("message", "Provide checksums parameter for specific analysis");
                statistics.put("totalAnalyzed", 0);
                statistics.put("reusePercentage", 0.0);
            }
            
            statistics.put("generatedBy", sw360User.getEmail());
            statistics.put("timestamp", System.currentTimeMillis());
            statistics.put("maxLimit", limit);
            
            return new ResponseEntity<>(statistics, HttpStatus.OK);
            
        } catch (TException e) {
            log.error("Thrift error generating reuse statistics: {}", e.getMessage(), e);
            statistics.put("error", "Service error: " + e.getMessage());
            statistics.put("errorType", "SERVICE_ERROR");
            return new ResponseEntity<>(statistics, HttpStatus.SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            log.error("Unexpected error generating reuse statistics: {}", e.getMessage(), e);
            statistics.put("error", "Failed to generate reuse statistics: " + e.getMessage());
            statistics.put("errorType", "INTERNAL_ERROR");
            return new ResponseEntity<>(statistics, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Find potential reuse candidates for attachment.",
            description = "Find other attachments that could be reused based on checksum matching.",
            tags = {"Attachments", "Reuse"}
    )
    @GetMapping(value = ATTACHMENTS_URL + "/{id}/reuse/candidates", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> findReuseCandidates(
            @Parameter(description = "ID of the attachment to find reuse candidates for")
            @PathVariable("id") String attachmentId
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            AttachmentInfo attachmentInfo = attachmentService.getAttachmentById(attachmentId);
            Attachment attachment = attachmentInfo.getAttachment();
            
            if (attachment.getSha1() == null || attachment.getSha1().isEmpty()) {
                result.put("error", "Attachment has no SHA1 checksum for reuse analysis");
                return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
            }
            
            // Find all attachments with same checksum
            List<AttachmentInfo> candidates = attachmentService.getAttachmentsBySha1(attachment.getSha1());
            
            // Remove the original attachment from candidates
            candidates.removeIf(candidate -> 
                    candidate.getAttachment().getAttachmentContentId().equals(attachmentId));
            
            List<Map<String, Object>> candidateDetails = new ArrayList<>();
            for (AttachmentInfo candidate : candidates) {
                Map<String, Object> candidateDetail = new HashMap<>();
                candidateDetail.put("attachmentId", candidate.getAttachment().getAttachmentContentId());
                candidateDetail.put("filename", candidate.getAttachment().getFilename());
                candidateDetail.put("sha1", candidate.getAttachment().getSha1());
                candidateDetail.put("attachmentType", candidate.getAttachment().getAttachmentType());
                candidateDetail.put("createdBy", candidate.getAttachment().getCreatedBy());
                candidateDetail.put("createdOn", candidate.getAttachment().getCreatedOn());
                
                // Add owner information
                Source owner = candidate.getOwner();
                Map<String, Object> ownerInfo = new HashMap<>();
                switch (owner.getSetField()) {
                    case PROJECT_ID:
                        ownerInfo.put("type", "project");
                        ownerInfo.put("id", owner.getProjectId());
                        break;
                    case COMPONENT_ID:
                        ownerInfo.put("type", "component");
                        ownerInfo.put("id", owner.getComponentId());
                        break;
                    case RELEASE_ID:
                        ownerInfo.put("type", "release");
                        ownerInfo.put("id", owner.getReleaseId());
                        break;
                }
                candidateDetail.put("owner", ownerInfo);
                candidateDetails.add(candidateDetail);
            }
            
            result.put("originalAttachment", Map.of(
                    "id", attachmentId,
                    "filename", attachment.getFilename(),
                    "sha1", attachment.getSha1()
            ));
            result.put("candidates", candidateDetails);
            result.put("candidateCount", candidateDetails.size());
            result.put("hasReusePotential", !candidateDetails.isEmpty());
            result.put("analyzedBy", sw360User.getEmail());
            result.put("timestamp", System.currentTimeMillis());
            
            return new ResponseEntity<>(result, HttpStatus.OK);
            
        } catch (TException e) {
            log.error("Thrift error finding reuse candidates for attachment {}: {}", attachmentId, e.getMessage(), e);
            result.put("error", "Service error: " + e.getMessage());
            result.put("errorType", "SERVICE_ERROR");
            return new ResponseEntity<>(result, HttpStatus.SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            log.error("Unexpected error finding reuse candidates for attachment {}: {}", attachmentId, e.getMessage(), e);
            result.put("error", "Failed to find reuse candidates: " + e.getMessage());
            result.put("errorType", "INTERNAL_ERROR");
            return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
            summary = "Validate attachment reuse potential.",
            description = "Validate whether an attachment can be reused in a specific context.",
            tags = {"Attachments", "Reuse"}
    )
    @PostMapping(value = ATTACHMENTS_URL + "/{id}/reuse/validate", 
                 consumes = MediaType.APPLICATION_JSON_VALUE, 
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> validateAttachmentReuse(
            @Parameter(description = "ID of the attachment to validate for reuse")
            @PathVariable("id") String attachmentId,
            @Parameter(description = "Target context for reuse validation")
            @RequestBody ReuseValidationContext targetContext
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);
        
        Map<String, Object> validationResult = new HashMap<>();
        
        try {
            AttachmentInfo attachmentInfo = attachmentService.getAttachmentById(attachmentId);
            Attachment attachment = attachmentInfo.getAttachment();
            
            // Basic validation
            List<String> validationIssues = new ArrayList<>();
            List<String> warnings = new ArrayList<>();
            
            // Check if attachment has required metadata for reuse
            if (attachment.getSha1() == null || attachment.getSha1().isEmpty()) {
                validationIssues.add("Attachment missing SHA1 checksum required for reuse validation");
            }
            
            if (attachment.getFilename() == null || attachment.getFilename().isEmpty()) {
                warnings.add("Attachment missing filename - limited reuse validation possible");
            }
            
            // Check target context
            String targetType = targetContext.getType();
            String targetId = targetContext.getId();
            
            if (targetType == null || targetId == null) {
                validationIssues.add("Target context must specify 'type' and 'id'");
            }
            
            // Validate target context exists and user has access
            boolean targetAccessible = false;
            if ("project".equals(targetType)) {
                try {
                    projectService.getProjectForUserById(targetId, sw360User);
                    targetAccessible = true;
                } catch (Exception e) {
                    validationIssues.add("Target project not accessible or does not exist");
                }
            } else if ("component".equals(targetType)) {
                try {
                    componentService.getComponentForUserById(targetId, sw360User);
                    targetAccessible = true;
                } catch (Exception e) {
                    validationIssues.add("Target component not accessible or does not exist");
                }
            } else if ("release".equals(targetType)) {
                try {
                    releaseService.getReleaseForUserById(targetId, sw360User);
                    targetAccessible = true;
                } catch (Exception e) {
                    validationIssues.add("Target release not accessible or does not exist");
                }
            } else {
                validationIssues.add("Invalid target type. Must be 'project', 'component', or 'release'");
            }
            
            // Check for potential conflicts
            if (targetAccessible && attachment.getSha1() != null) {
                List<AttachmentInfo> existingInTarget = attachmentService.getAttachmentsBySha1(attachment.getSha1());
                boolean alreadyExists = existingInTarget.stream().anyMatch(existing -> {
                    Source owner = existing.getOwner();
                    switch (owner.getSetField()) {
                        case PROJECT_ID:
                            return "project".equals(targetType) && targetId.equals(owner.getProjectId());
                        case COMPONENT_ID:
                            return "component".equals(targetType) && targetId.equals(owner.getComponentId());
                        case RELEASE_ID:
                            return "release".equals(targetType) && targetId.equals(owner.getReleaseId());
                        default:
                            return false;
                    }
                });
                
                if (alreadyExists) {
                    warnings.add("Attachment with same checksum already exists in target");
                }
            }
            
            // Determine validation result
            boolean isValid = validationIssues.isEmpty();
            String recommendation;
            if (!isValid) {
                recommendation = "Cannot reuse - validation errors must be resolved";
            } else if (!warnings.isEmpty()) {
                recommendation = "Can reuse with warnings - review recommended";
            } else {
                recommendation = "Safe to reuse - no issues found";
            }
            
            validationResult.put("valid", isValid);
            validationResult.put("recommendation", recommendation);
            validationResult.put("validationIssues", validationIssues);
            validationResult.put("warnings", warnings);
            validationResult.put("attachment", Map.of(
                    "id", attachmentId,
                    "filename", attachment.getFilename(),
                    "sha1", attachment.getSha1()
            ));
            validationResult.put("targetContext", targetContext);
            validationResult.put("validatedBy", sw360User.getEmail());
            validationResult.put("timestamp", System.currentTimeMillis());
            
            return new ResponseEntity<>(validationResult, HttpStatus.OK);
            
        } catch (TException e) {
            log.error("Thrift error validating attachment reuse for {}: {}", attachmentId, e.getMessage(), e);
            validationResult.put("error", "Service error: " + e.getMessage());
            validationResult.put("errorType", "SERVICE_ERROR");
            return new ResponseEntity<>(validationResult, HttpStatus.SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            log.error("Unexpected error validating attachment reuse for {}: {}", attachmentId, e.getMessage(), e);
            validationResult.put("error", "Failed to validate attachment reuse: " + e.getMessage());
            validationResult.put("errorType", "INTERNAL_ERROR");
            return new ResponseEntity<>(validationResult, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        final WebMvcLinkBuilder controllerLinkBuilder = linkTo(AttachmentController.class);
        final Link attachments = Link.of(UriTemplate.of(controllerLinkBuilder.toUri().toString() + "/api/attachments{?sha1}"), "attachments");
        resource.add(attachments);
        return resource;
    }

    // Request DTOs
    
    @Schema(description = "Request for searching duplicate attachments")
    public static class DuplicateSearchRequest {
        @Schema(description = "List of checksums (SHA1) to search for duplicates", 
                example = "[\"a1b2c3d4e5f6789\", \"f6e5d4c3b2a1098\"]",
                required = true)
        private List<String> checksums;

        // Getters and setters
        public List<String> getChecksums() { return checksums; }
        public void setChecksums(List<String> checksums) { this.checksums = checksums; }
    }

    @Schema(description = "Target context for attachment reuse validation")
    public static class ReuseValidationContext {
        @Schema(description = "Type of target entity", 
                example = "project",
                allowableValues = {"project", "component", "release"},
                required = true)
        private String type;
        
        @Schema(description = "ID of the target entity", 
                example = "target-entity-id",
                required = true)
        private String id;

        // Getters and setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
    }
}

/*
 * Copyright Ritankar Saha<ritankar.saha786@gmail.com>, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.datahandler.services.reuse;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.db.ChecksumRepository;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.components.ClearingState;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Handles clearing decision inheritance and reuse logic.
 * This class manages the complex logic of determining when and how
 * clearing decisions can be inherited from one file to another based on FOSSology reuser agent capabilities.
 */
public class ClearingDecisionReuse {

    private static final Logger log = LoggerFactory.getLogger(ClearingDecisionReuse.class);

    private final ChecksumRepository checksumRepository;
    private final DuplicationDetectionService duplicationDetectionService;
    private final Map<String, InheritanceRecord> inheritanceCache;
    private final Set<String> conflictingLicenses;
    private final Map<String, Set<String>> organizationDomains;

    // Decision types that support reuse
    public enum ReuseDecisionType {
        IDENTICAL_FILE("File with identical checksum"),
        SIMILAR_FILE("File with similar content structure"),
        COMPONENT_LEVEL("Component-wide clearing decision"),
        LICENSE_CONCLUSION("Specific license identification"),
        COPYRIGHT_STATEMENT("Copyright ownership statement"),
        EXPORT_RESTRICTION("Export control classification");

        private final String description;
        ReuseDecisionType(String description) { this.description = description; }
        public String getDescription() { return description; }
    }

    // Reuse scope levels
    public enum ReuseScope {
        FILE_LEVEL(1, "Individual file"),
        COMPONENT_LEVEL(2, "Entire component"),
        PROJECT_LEVEL(3, "Project scope"),
        ORGANIZATION_LEVEL(4, "Organization-wide");

        private final int priority;
        private final String description;
        ReuseScope(int priority, String description) { 
            this.priority = priority; 
            this.description = description; 
        }
        public int getPriority() { return priority; }
        public String getDescription() { return description; }
    }

    // Decision confidence levels
    public enum ConfidenceLevel {
        HIGH(90, "Automatic inheritance recommended"),
        MEDIUM(60, "Manual review recommended"),
        LOW(30, "Manual verification required");

        private final int score;
        private final String recommendation;
        ConfidenceLevel(int score, String recommendation) { 
            this.score = score; 
            this.recommendation = recommendation; 
        }
        public int getScore() { return score; }
        public String getRecommendation() { return recommendation; }
    }

    /**
     * Data structure representing a clearing decision that can be reused
     */
    public static class ReuseableDecision {
        private String sourceReleaseId;
        private String sourceAttachmentId;
        private String checksum;
        private String checksumType;
        private ReuseDecisionType decisionType;
        private ReuseScope scope;
        private ConfidenceLevel confidence;
        private Map<String, Object> decisionData;
        private Date createdDate;
        private String createdBy;
        private String comment;
        private List<String> licenseIds;
        private List<String> copyrightStatements;
        private String clearingTeam;
        private String fossologyUploadId;
        private Map<String, String> additionalData;
        private ClearingState clearingState;
        private String organizationId;
        private boolean isApproved;
        private List<String> reviewerComments;

        public ReuseableDecision() {
            this.decisionData = new HashMap<>();
            this.licenseIds = new ArrayList<>();
            this.copyrightStatements = new ArrayList<>();
            this.additionalData = new HashMap<>();
            this.reviewerComments = new ArrayList<>();
            this.isApproved = false;
        }

        // Getters and setters
        public String getSourceReleaseId() { return sourceReleaseId; }
        public void setSourceReleaseId(String sourceReleaseId) { this.sourceReleaseId = sourceReleaseId; }

        public String getSourceAttachmentId() { return sourceAttachmentId; }
        public void setSourceAttachmentId(String sourceAttachmentId) { this.sourceAttachmentId = sourceAttachmentId; }

        public String getChecksum() { return checksum; }
        public void setChecksum(String checksum) { this.checksum = checksum; }

        public String getChecksumType() { return checksumType; }
        public void setChecksumType(String checksumType) { this.checksumType = checksumType; }

        public ReuseDecisionType getDecisionType() { return decisionType; }
        public void setDecisionType(ReuseDecisionType decisionType) { this.decisionType = decisionType; }

        public ReuseScope getScope() { return scope; }
        public void setScope(ReuseScope scope) { this.scope = scope; }

        public ConfidenceLevel getConfidence() { return confidence; }
        public void setConfidence(ConfidenceLevel confidence) { this.confidence = confidence; }

        public Map<String, Object> getDecisionData() { return decisionData; }
        public void setDecisionData(Map<String, Object> decisionData) { this.decisionData = decisionData; }

        public Date getCreatedDate() { return createdDate; }
        public void setCreatedDate(Date createdDate) { this.createdDate = createdDate; }

        public String getCreatedBy() { return createdBy; }
        public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }

        public List<String> getLicenseIds() { return licenseIds; }
        public void setLicenseIds(List<String> licenseIds) { this.licenseIds = licenseIds; }

        public List<String> getCopyrightStatements() { return copyrightStatements; }
        public void setCopyrightStatements(List<String> copyrightStatements) { this.copyrightStatements = copyrightStatements; }

        public String getClearingTeam() { return clearingTeam; }
        public void setClearingTeam(String clearingTeam) { this.clearingTeam = clearingTeam; }

        public String getFossologyUploadId() { return fossologyUploadId; }
        public void setFossologyUploadId(String fossologyUploadId) { this.fossologyUploadId = fossologyUploadId; }

        public Map<String, String> getAdditionalData() { return additionalData; }
        public void setAdditionalData(Map<String, String> additionalData) { this.additionalData = additionalData; }

        public ClearingState getClearingState() { return clearingState; }
        public void setClearingState(ClearingState clearingState) { this.clearingState = clearingState; }

        public String getOrganizationId() { return organizationId; }
        public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }

        public boolean isApproved() { return isApproved; }
        public void setApproved(boolean approved) { isApproved = approved; }

        public List<String> getReviewerComments() { return reviewerComments; }
        public void setReviewerComments(List<String> reviewerComments) { this.reviewerComments = reviewerComments; }
    }

    /**
     * Internal record for tracking inheritance operations
     */
    private static class InheritanceRecord {
        private final String targetReleaseId;
        private final String sourceReleaseId;
        private final String checksum;
        private final Date inheritedAt;
        private final String inheritedBy;
        private final ConfidenceLevel confidence;
        private final boolean successful;

        public InheritanceRecord(String targetReleaseId, String sourceReleaseId, String checksum, 
                               Date inheritedAt, String inheritedBy, ConfidenceLevel confidence, boolean successful) {
            this.targetReleaseId = targetReleaseId;
            this.sourceReleaseId = sourceReleaseId;
            this.checksum = checksum;
            this.inheritedAt = inheritedAt;
            this.inheritedBy = inheritedBy;
            this.confidence = confidence;
            this.successful = successful;
        }

        public String getKey() {
            return targetReleaseId + ":" + checksum;
        }
    }

    public ClearingDecisionReuse(ChecksumRepository checksumRepository) {
        this.checksumRepository = checksumRepository;
        this.duplicationDetectionService = new DuplicationDetectionService(checksumRepository);
        this.inheritanceCache = new ConcurrentHashMap<>();
        this.conflictingLicenses = initializeConflictingLicenses();
        this.organizationDomains = initializeOrganizationDomains();
    }

    /**
     * Find reuseable clearing decisions for a release based on file checksums and FOSSology analysis
     */
    public Map<String, List<ReuseableDecision>> findReuseableClearingDecisions(
            Release targetRelease, List<Release> sourceReleases, User user) {
        
        Map<String, List<ReuseableDecision>> reuseableDecisions = new HashMap<>();
        
        if (targetRelease.getAttachments() == null || targetRelease.getAttachments().isEmpty()) {
            log.debug("No attachments in target release {}", targetRelease.getId());
            return reuseableDecisions;
        }
        
        log.info("Finding reuseable clearing decisions for release {} from {} source releases", 
                 targetRelease.getId(), sourceReleases.size());
        
        // Build comprehensive checksum index from source releases with valid clearing decisions
        Map<String, List<ReuseableDecision>> checksumIndex = buildChecksumIndex(sourceReleases, user);
        
        // Process each attachment in target release
        for (Attachment targetAttachment : targetRelease.getAttachments()) {
            String checksum = targetAttachment.getSha1();
            if (CommonUtils.isNullEmptyOrWhitespace(checksum)) {
                continue;
            }
            
            // Check cache first
            String cacheKey = targetRelease.getId() + ":" + checksum;
            if (inheritanceCache.containsKey(cacheKey)) {
                InheritanceRecord cached = inheritanceCache.get(cacheKey);
                if (cached.successful && isRecentDecision(cached.inheritedAt)) {
                    log.debug("Using cached inheritance decision for checksum: {}", checksum);
                    continue;
                }
            }
            
            List<ReuseableDecision> matchingDecisions = findMatchingDecisions(
                    targetAttachment, checksumIndex, targetRelease, user);
            
            if (!matchingDecisions.isEmpty()) {
                // Apply additional filtering and ranking
                List<ReuseableDecision> filteredDecisions = applyAdvancedFiltering(
                        matchingDecisions, targetRelease, user);
                
                if (!filteredDecisions.isEmpty()) {
                    reuseableDecisions.put(checksum, filteredDecisions);
                    log.debug("Found {} reuseable decisions for attachment {} (checksum: {})", 
                             filteredDecisions.size(), targetAttachment.getFilename(), checksum);
                }
            }
        }
        
        log.info("Found reuseable decisions for {} out of {} attachments in release {}", 
                 reuseableDecisions.size(), targetRelease.getAttachments().size(), targetRelease.getId());
        
        return reuseableDecisions;
    }

    /**
     * Apply clearing decision inheritance with comprehensive validation and error handling
     */
    public Map<String, Object> applyClearingDecisionInheritance(
            Release targetRelease, Map<String, List<ReuseableDecision>> reuseDecisions, User user) {
        
        Map<String, Object> results = new HashMap<>();
        
        log.info("Applying clearing decision inheritance for release {} with {} decision groups", 
                 targetRelease.getId(), reuseDecisions.size());
        
        int totalDecisions = reuseDecisions.values().stream().mapToInt(List::size).sum();
        int appliedDecisions = 0;
        int skippedDecisions = 0;
        List<String> validationErrors = new ArrayList<>();
        Map<String, Object> inheritanceDetails = new HashMap<>();
        List<Map<String, Object>> auditTrail = new ArrayList<>();
        Map<String, Integer> errorCategories = new HashMap<>();
        
        for (Map.Entry<String, List<ReuseableDecision>> entry : reuseDecisions.entrySet()) {
            String checksum = entry.getKey();
            List<ReuseableDecision> decisions = entry.getValue();
            
            Map<String, Object> applicationResult = applyDecisionsForChecksum(
                    targetRelease, checksum, decisions, user);
            
            appliedDecisions += (Integer) applicationResult.getOrDefault("applied", 0);
            skippedDecisions += (Integer) applicationResult.getOrDefault("skipped", 0);
            
            @SuppressWarnings("unchecked")
            List<String> errors = (List<String>) applicationResult.get("errors");
            if (errors != null && !errors.isEmpty()) {
                validationErrors.addAll(errors);
                // Categorize errors
                for (String error : errors) {
                    String category = categorizeError(error);
                    errorCategories.merge(category, 1, Integer::sum);
                }
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> checksumAuditTrail = (List<Map<String, Object>>) applicationResult.get("auditTrail");
            if (checksumAuditTrail != null) {
                auditTrail.addAll(checksumAuditTrail);
            }
            
            inheritanceDetails.put(checksum, applicationResult);
        }
        
        double successRate = totalDecisions > 0 ? (double) appliedDecisions / totalDecisions * 100.0 : 0.0;
        String overallStatus = determineOverallStatus(appliedDecisions, skippedDecisions, validationErrors.size());
        
        results.put("status", overallStatus);
        results.put("releaseId", targetRelease.getId());
        results.put("releaseName", targetRelease.getName());
        results.put("releaseVersion", targetRelease.getVersion());
        results.put("totalDecisions", totalDecisions);
        results.put("appliedDecisions", appliedDecisions);
        results.put("skippedDecisions", skippedDecisions);
        results.put("successRate", Math.round(successRate * 100.0) / 100.0);
        results.put("validationErrors", validationErrors);
        results.put("errorCategories", errorCategories);
        results.put("inheritanceDetails", inheritanceDetails);
        results.put("auditTrail", auditTrail);
        results.put("timestamp", new Date());
        results.put("appliedBy", user.getEmail());
        results.put("appliedByDepartment", user.getDepartment());
        results.put("processingTimeMs", 0L); // Will be set by caller
        
        // Update clearing state if significant progress made
        if (successRate > 50.0) {
            results.put("recommendedClearingState", ClearingState.UNDER_CLEARING);
        }
        
        log.info("Applied clearing decision inheritance for release {}: {}/{} decisions applied ({:.1f}%), {} errors", 
                 targetRelease.getId(), appliedDecisions, totalDecisions, successRate, validationErrors.size());
        
        return results;
    }

    /**
     * Validate that a clearing decision can be inherited in the target context with comprehensive checks
     */
    public Map<String, Object> validateDecisionInheritance(
            ReuseableDecision decision, Release targetRelease, User user) {
        
        Map<String, Object> validation = new HashMap<>();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        ConfidenceLevel confidence = decision.getConfidence();
        Map<String, Object> validationMetrics = new HashMap<>();
        
        // Core validation checks
        validateRequiredFields(decision, errors);
        validateChecksumFormat(decision, errors);
        validateUserAuthorization(user, decision, targetRelease, errors, warnings);
        validateOrganizationContext(decision, targetRelease, user, warnings);
        validateTemporalConstraints(decision, warnings);
        validateLicenseCompatibility(decision, targetRelease, warnings, errors);
        validateDecisionTypeSpecific(decision, targetRelease, warnings, errors);
        validateFileTypeCompatibility(decision, targetRelease, warnings);
        validateClearingStateCompatibility(decision, targetRelease, warnings);
        
        // Advanced validation metrics
        validationMetrics.put("checksumValidation", errors.stream().noneMatch(e -> e.contains("checksum")));
        validationMetrics.put("licenseValidation", errors.stream().noneMatch(e -> e.contains("license")));
        validationMetrics.put("authorizationValidation", errors.stream().noneMatch(e -> e.contains("authorization")));
        validationMetrics.put("temporalValidation", warnings.stream().noneMatch(w -> w.contains("old")));
        
        // Adjust confidence based on validation results
        confidence = adjustConfidenceLevel(confidence, warnings.size(), errors.size());
        
        validation.put("valid", errors.isEmpty());
        validation.put("confidence", confidence);
        validation.put("confidenceScore", confidence.getScore());
        validation.put("warnings", warnings);
        validation.put("errors", errors);
        validation.put("recommendation", generateInheritanceRecommendation(confidence, warnings, errors));
        validation.put("riskLevel", calculateRiskLevel(confidence, warnings.size(), errors.size()));
        validation.put("validationMetrics", validationMetrics);
        validation.put("validatedBy", user.getEmail());
        validation.put("validationTimestamp", new Date());
        validation.put("requiresManualReview", confidence == ConfidenceLevel.LOW || errors.size() > 0);
        validation.put("canAutoApply", confidence == ConfidenceLevel.HIGH && errors.isEmpty() && warnings.size() <= 1);
        
        return validation;
    }

    /**
     * Create comprehensive inheritance audit trail entry with full context
     */
    public Map<String, Object> createInheritanceAuditTrail(
            ReuseableDecision decision, Release targetRelease, User user, Map<String, Object> validationResult) {
        
        Map<String, Object> auditEntry = new HashMap<>();
        
        auditEntry.put("action", "clearing_decision_inheritance");
        auditEntry.put("timestamp", new Date());
        auditEntry.put("userId", user.getId());
        auditEntry.put("userEmail", user.getEmail());
        auditEntry.put("userDepartment", user.getDepartment());
        auditEntry.put("userGroups", user.getUserGroup() != null ? Arrays.asList(user.getUserGroup()) : Collections.emptyList());
        auditEntry.put("targetReleaseId", targetRelease.getId());
        auditEntry.put("targetReleaseName", targetRelease.getName());
        auditEntry.put("targetReleaseVersion", targetRelease.getVersion());
        auditEntry.put("targetComponentId", targetRelease.getComponentId());
        auditEntry.put("sourceReleaseId", decision.getSourceReleaseId());
        auditEntry.put("sourceAttachmentId", decision.getSourceAttachmentId());
        auditEntry.put("checksum", decision.getChecksum());
        auditEntry.put("checksumType", decision.getChecksumType());
        auditEntry.put("decisionType", decision.getDecisionType().toString());
        auditEntry.put("decisionTypeDescription", decision.getDecisionType().getDescription());
        auditEntry.put("scope", decision.getScope().toString());
        auditEntry.put("scopeDescription", decision.getScope().getDescription());
        auditEntry.put("confidence", decision.getConfidence().toString());
        auditEntry.put("confidenceScore", decision.getConfidence().getScore());
        auditEntry.put("validationResult", validationResult);
        auditEntry.put("originalDecisionBy", decision.getCreatedBy());
        auditEntry.put("originalDecisionDate", decision.getCreatedDate());
        auditEntry.put("clearingTeam", decision.getClearingTeam());
        auditEntry.put("fossologyUploadId", decision.getFossologyUploadId());
        auditEntry.put("inheritanceMethod", "fossology_reuser_agent_v2");
        auditEntry.put("licenseIds", decision.getLicenseIds());
        auditEntry.put("copyrightStatements", decision.getCopyrightStatements());
        auditEntry.put("comment", decision.getComment());
        auditEntry.put("additionalData", decision.getAdditionalData());
        auditEntry.put("organizationId", decision.getOrganizationId());
        auditEntry.put("approved", decision.isApproved());
        auditEntry.put("reviewerComments", decision.getReviewerComments());
        auditEntry.put("clearingState", decision.getClearingState() != null ? decision.getClearingState().toString() : null);
        
        log.debug("Created comprehensive inheritance audit trail entry for decision inheritance from {} to {}", 
                 decision.getSourceReleaseId(), targetRelease.getId());
        
        return auditEntry;
    }

    // Private implementation methods

    private Map<String, List<ReuseableDecision>> buildChecksumIndex(List<Release> sourceReleases, User user) {
        Map<String, List<ReuseableDecision>> checksumIndex = new HashMap<>();
        
        for (Release release : sourceReleases) {
            if (release.getAttachments() != null && hasValidClearingState(release)) {
                for (Attachment attachment : release.getAttachments()) {
                    String checksum = attachment.getSha1();
                    if (!CommonUtils.isNullEmptyOrWhitespace(checksum) && hasValidClearingDecision(attachment, release)) {
                        ReuseableDecision decision = createReuseableDecisionFromAttachment(attachment, release, user);
                        checksumIndex.computeIfAbsent(checksum, k -> new ArrayList<>()).add(decision);
                    }
                }
            }
        }
        
        return checksumIndex;
    }

    private ReuseableDecision createReuseableDecisionFromAttachment(Attachment attachment, Release release, User user) {
        ReuseableDecision decision = new ReuseableDecision();
        decision.setSourceReleaseId(release.getId());
        decision.setSourceAttachmentId(attachment.getAttachmentContentId());
        decision.setChecksum(attachment.getSha1());
        decision.setChecksumType("sha1");
        decision.setDecisionType(ReuseDecisionType.IDENTICAL_FILE);
        decision.setScope(ReuseScope.FILE_LEVEL);
        decision.setConfidence(ConfidenceLevel.HIGH);
        decision.setCreatedDate(extractDecisionDate(attachment, release));
        decision.setCreatedBy(extractDecisionCreator(attachment, release));
        // decision.setClearingTeam(release.getClearingTeam());
        decision.setFossologyUploadId(attachment.getFossologyUploadId());
        decision.setClearingState(release.getClearingState());
        decision.setOrganizationId(extractOrganizationId(user));
        decision.setApproved(isDecisionApproved(release));
        
        extractClearingInformation(attachment, release, decision);
        populateDecisionMetadata(attachment, release, decision);
        
        return decision;
    }

    private List<ReuseableDecision> findMatchingDecisions(
            Attachment targetAttachment, Map<String, List<ReuseableDecision>> checksumIndex, 
            Release targetRelease, User user) {
        
        String checksum = targetAttachment.getSha1();
        List<ReuseableDecision> candidateDecisions = checksumIndex.getOrDefault(checksum, Collections.emptyList());
        
        return candidateDecisions.stream()
                .filter(decision -> isDecisionApplicable(decision, targetAttachment, targetRelease, user))
                .sorted(this::compareDecisionPriority)
                .collect(Collectors.toList());
    }

    private List<ReuseableDecision> applyAdvancedFiltering(
            List<ReuseableDecision> decisions, Release targetRelease, User user) {
        
        return decisions.stream()
                .filter(decision -> {
                    // Organization boundary check
                    if (!isOrganizationCompatible(decision, user)) {
                        return false;
                    }
                    
                    // License conflict check
                    if (hasLicenseConflicts(decision, targetRelease)) {
                        return false;
                    }
                    
                    // Temporal validity check
                    if (isDecisionExpired(decision)) {
                        return false;
                    }
                    
                    return true;
                })
                .limit(10) 
                .collect(Collectors.toList());
    }

    private boolean isDecisionApplicable(ReuseableDecision decision, Attachment targetAttachment, 
                                       Release targetRelease, User user) {
        
        // Basic confidence threshold
        if (decision.getConfidence() == ConfidenceLevel.LOW) {
            return false;
        }
        
        // File type compatibility
        if (!isFileTypeCompatible(decision, targetAttachment)) {
            return false;
        }
        
        // User authorization
        if (!hasUserPermission(user, decision)) {
            return false;
        }
        
        // Organizational policy compliance
        if (!isOrganizationPolicyCompliant(decision, targetRelease, user)) {
            return false;
        }
        
        return true;
    }

    private int compareDecisionPriority(ReuseableDecision a, ReuseableDecision b) {
        // Multi-factor priority comparison
        
        // Confidence level 
        int confidenceComparison = Integer.compare(b.getConfidence().getScore(), a.getConfidence().getScore());
        if (confidenceComparison != 0) {
            return confidenceComparison;
        }
        
        // Approval status 
        int approvalComparison = Boolean.compare(b.isApproved(), a.isApproved());
        if (approvalComparison != 0) {
            return approvalComparison;
        }
        
        //  Creation date 
        if (a.getCreatedDate() != null && b.getCreatedDate() != null) {
            int dateComparison = b.getCreatedDate().compareTo(a.getCreatedDate());
            if (dateComparison != 0) {
                return dateComparison;
            }
        }
        
        // Clearing team presence 
        boolean aHasTeam = !CommonUtils.isNullEmptyOrWhitespace(a.getClearingTeam());
        boolean bHasTeam = !CommonUtils.isNullEmptyOrWhitespace(b.getClearingTeam());
        int teamComparison = Boolean.compare(bHasTeam, aHasTeam);
        if (teamComparison != 0) {
            return teamComparison;
        }
        
        // License information completeness
        int aLicenseCount = a.getLicenseIds() != null ? a.getLicenseIds().size() : 0;
        int bLicenseCount = b.getLicenseIds() != null ? b.getLicenseIds().size() : 0;
        return Integer.compare(bLicenseCount, aLicenseCount);
    }

    private Map<String, Object> applyDecisionsForChecksum(
            Release targetRelease, String checksum, List<ReuseableDecision> decisions, User user) {
        
        Map<String, Object> result = new HashMap<>();
        int applied = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();
        List<Map<String, Object>> auditTrail = new ArrayList<>();
        List<String> processingLog = new ArrayList<>();
        
        for (ReuseableDecision decision : decisions) {
            processingLog.add("Processing decision from release: " + decision.getSourceReleaseId());
            
            Map<String, Object> validationResult = validateDecisionInheritance(decision, targetRelease, user);
            
            if ((Boolean) validationResult.get("valid")) {
                Map<String, Object> auditEntry = createInheritanceAuditTrail(decision, targetRelease, user, validationResult);
                auditTrail.add(auditEntry);
                
                boolean applicationSuccess = executeDecisionInheritance(decision, targetRelease, user);
                if (applicationSuccess) {
                    applied++;
                    processingLog.add("Successfully applied decision for checksum: " + checksum);
                    
                    // Cache successful inheritance
                    InheritanceRecord record = new InheritanceRecord(
                            targetRelease.getId(), decision.getSourceReleaseId(), checksum,
                            new Date(), user.getEmail(), decision.getConfidence(), true);
                    inheritanceCache.put(record.getKey(), record);
                    
                    log.debug("Successfully applied clearing decision inheritance for checksum: {}", checksum);
                    break; // Apply only the best match
                } else {
                    errors.add("Failed to execute decision inheritance for checksum: " + checksum);
                    skipped++;
                    processingLog.add("Failed to apply decision for checksum: " + checksum);
                }
            } else {
                @SuppressWarnings("unchecked")
                List<String> decisionErrors = (List<String>) validationResult.get("errors");
                if (decisionErrors != null) {
                    errors.addAll(decisionErrors);
                }
                skipped++;
                processingLog.add("Skipped decision due to validation errors: " + decisionErrors);
                log.debug("Skipped decision inheritance for checksum {} due to validation errors: {}", 
                         checksum, decisionErrors);
            }
        }
        
        result.put("checksum", checksum);
        result.put("applied", applied);
        result.put("skipped", skipped);
        result.put("errors", errors);
        result.put("auditTrail", auditTrail);
        result.put("processingLog", processingLog);
        result.put("candidateCount", decisions.size());
        
        return result;
    }

    private boolean executeDecisionInheritance(ReuseableDecision decision, Release targetRelease, User user) {
        try {
            // Create comprehensive inheritance record
            Map<String, Object> inheritanceRecord = createInheritanceRecord(decision, targetRelease, user);
            
            // Store inheritance record
            boolean stored = storeInheritanceRecord(inheritanceRecord);
            if (!stored) {
                log.error("Failed to store inheritance record for decision");
                return false;
            }
            
            // Update release clearing status if applicable
            boolean statusUpdated = updateClearingStatus(targetRelease, decision, user);
            if (!statusUpdated) {
                log.warn("Failed to update clearing status for release: {}", targetRelease.getId());
            }
            
            // Send notifications if configured
            sendInheritanceNotifications(decision, targetRelease, user);
            
            log.info("Successfully executed clearing decision inheritance from release {} to {} for checksum {}", 
                     decision.getSourceReleaseId(), targetRelease.getId(), decision.getChecksum());
            
            return true;
            
        } catch (Exception e) {
            log.error("Failed to execute decision inheritance for checksum {}: {}", 
                     decision.getChecksum(), e.getMessage(), e);
            return false;
        }
    }

    // Validation helper methods

    private void validateRequiredFields(ReuseableDecision decision, List<String> errors) {
        if (CommonUtils.isNullEmptyOrWhitespace(decision.getChecksum())) {
            errors.add("Missing required checksum");
        }
        if (decision.getDecisionType() == null) {
            errors.add("Missing required decision type");
        }
        if (decision.getConfidence() == null) {
            errors.add("Missing confidence level");
        }
    }

    private void validateChecksumFormat(ReuseableDecision decision, List<String> errors) {
        if (!CommonUtils.isNullEmptyOrWhitespace(decision.getChecksum())) {
            String checksumType = duplicationDetectionService.validateChecksumFormat(decision.getChecksum());
            if (checksumType == null) {
                errors.add("Invalid checksum format: " + decision.getChecksum());
            }
        }
    }

    private void validateUserAuthorization(User user, ReuseableDecision decision, Release targetRelease, 
                                         List<String> errors, List<String> warnings) {
        if (user.getUserGroup() == null) {
            warnings.add("User has no assigned group - limited authorization");
            return;
        }
        
        // Check specific permissions based on decision type
        switch (decision.getDecisionType()) {
            case LICENSE_CONCLUSION:
                if (user.getUserGroup() != UserGroup.CLEARING_ADMIN && user.getUserGroup() != UserGroup.CLEARING_EXPERT) {
                    errors.add("User not authorized to inherit license conclusions");
                }
                break;
            case EXPORT_RESTRICTION:
                if (user.getUserGroup() != UserGroup.CLEARING_ADMIN) {
                    errors.add("User not authorized to inherit export restriction decisions");
                }
                break;
            case COMPONENT_LEVEL:
                if (user.getUserGroup() == UserGroup.USER) {
                    warnings.add("Component-level inheritance requires elevated permissions");
                }
                break;
            case IDENTICAL_FILE:
        
                break;
            case SIMILAR_FILE:
                if (user.getUserGroup() == UserGroup.USER) {
                    warnings.add("Similar file inheritance may require review");
                }
                break;
            case COPYRIGHT_STATEMENT:
                if (user.getUserGroup() != UserGroup.CLEARING_ADMIN && user.getUserGroup() != UserGroup.CLEARING_EXPERT) {
                    warnings.add("Copyright statement inheritance requires clearing permissions");
                }
                break;
        }
    }

    private void validateOrganizationContext(ReuseableDecision decision, Release targetRelease, User user, List<String> warnings) {
        if (decision.getScope() == ReuseScope.ORGANIZATION_LEVEL) {
            String userOrg = extractOrganizationId(user);
            if (!Objects.equals(userOrg, decision.getOrganizationId())) {
                warnings.add("Cross-organization inheritance - verify policy compliance");
            }
        }
    }

    private void validateTemporalConstraints(ReuseableDecision decision, List<String> warnings) {
        if (decision.getCreatedDate() != null) {
            long daysSinceCreation = (System.currentTimeMillis() - decision.getCreatedDate().getTime()) / (1000 * 60 * 60 * 24);
            if (daysSinceCreation > 365) {
                warnings.add("Decision is older than 1 year - consider verification");
            }
            if (daysSinceCreation > 1095) { // 3 years
                warnings.add("Decision is older than 3 years - manual review strongly recommended");
            }
        }
    }

    private void validateLicenseCompatibility(ReuseableDecision decision, Release targetRelease, 
                                            List<String> warnings, List<String> errors) {
        if (decision.getLicenseIds() != null && !decision.getLicenseIds().isEmpty()) {
            List<String> incompatibleLicenses = findIncompatibleLicenses(decision.getLicenseIds(), targetRelease);
            if (!incompatibleLicenses.isEmpty()) {
                errors.add("Incompatible licenses detected: " + String.join(", ", incompatibleLicenses));
            }
            
            // Check for conflicting licenses within the decision
            List<String> conflicts = findLicenseConflicts(decision.getLicenseIds());
            if (!conflicts.isEmpty()) {
                warnings.add("License conflicts detected: " + String.join(", ", conflicts));
            }
        }
    }

    private void validateDecisionTypeSpecific(ReuseableDecision decision, Release targetRelease, 
                                            List<String> warnings, List<String> errors) {
        switch (decision.getDecisionType()) {
            case IDENTICAL_FILE:
                // High confidence validation
                break;
            case SIMILAR_FILE:
                warnings.add("Similar file decision requires manual verification");
                break;
            case COMPONENT_LEVEL:
                if (!isComponentLevelApplicable(decision, targetRelease)) {
                    errors.add("Component level decision not applicable to different component");
                }
                break;
            case LICENSE_CONCLUSION:
                if (decision.getLicenseIds().isEmpty()) {
                    errors.add("License conclusion decision missing license information");
                }
                break;
            case COPYRIGHT_STATEMENT:
                if (decision.getCopyrightStatements().isEmpty()) {
                    errors.add("Copyright decision missing copyright statements");
                }
                break;
            case EXPORT_RESTRICTION:
                warnings.add("Export restriction decision - verify applicability to target context");
                break;
        }
    }

    private void validateFileTypeCompatibility(ReuseableDecision decision, Release targetRelease, List<String> warnings) {
        Object sourceFilename = decision.getDecisionData().get("filename");
        if (sourceFilename != null && targetRelease.getAttachments() != null) {
            boolean hasCompatibleFile = targetRelease.getAttachments().stream()
                    .anyMatch(att -> isFileTypeCompatible(sourceFilename.toString(), att.getFilename()));
            if (!hasCompatibleFile) {
                warnings.add("No compatible file types found in target release");
            }
        }
    }

    private void validateClearingStateCompatibility(ReuseableDecision decision, Release targetRelease, List<String> warnings) {
        if (decision.getClearingState() != null && targetRelease.getClearingState() != null) {
            if (!isClearingStateCompatible(decision.getClearingState(), targetRelease.getClearingState())) {
                warnings.add("Clearing state mismatch - review inheritance applicability");
            }
        }
    }

    // Support methods

    private Set<String> initializeConflictingLicenses() {
        Set<String> conflicts = new HashSet<>();
        conflicts.add("GPL-2.0:MIT");
        conflicts.add("GPL-2.0:Apache-2.0");
        conflicts.add("GPL-3.0:MIT");
        conflicts.add("AGPL-3.0:Apache-2.0");
        return conflicts;
    }

    private Map<String, Set<String>> initializeOrganizationDomains() {
        Map<String, Set<String>> domains = new HashMap<>();
        // Initialize with common organizational domain patterns
        return domains;
    }

    private boolean hasValidClearingState(Release release) {
        return release.getClearingState() != null && 
               (release.getClearingState() == ClearingState.REPORT_AVAILABLE || 
                release.getClearingState() == ClearingState.APPROVED);
    }

    private boolean hasValidClearingDecision(Attachment attachment, Release release) {
        return !CommonUtils.isNullEmptyOrWhitespace(attachment.getSha1()) &&
               !CommonUtils.isNullEmptyOrWhitespace(attachment.getFossologyUploadId()) &&
               hasValidClearingState(release);
    }

    private Date extractDecisionDate(Attachment attachment, Release release) {
        if (release.getCreatedOn() != null) {
            try {
                return new Date(Long.parseLong(release.getCreatedOn()));
            } catch (NumberFormatException e) {
                log.debug("Invalid date format in release: {}", release.getCreatedOn());
            }
        }
        return new Date();
    }

    private String extractDecisionCreator(Attachment attachment, Release release) {
        return release.getCreatedBy() != null ? release.getCreatedBy() : "system";
    }

    private String extractOrganizationId(User user) {
        if (user.getEmail() != null) {
            String domain = user.getEmail().substring(user.getEmail().indexOf('@') + 1);
            String normalizedDomain = domain.toLowerCase();
            
            // Use organization domains mapping if available
            for (Map.Entry<String, Set<String>> entry : organizationDomains.entrySet()) {
                if (entry.getValue().contains(normalizedDomain)) {
                    return entry.getKey();
                }
            }
            
            return normalizedDomain;
        }
        return "unknown.org";
    }

    private boolean isDecisionApproved(Release release) {
        return release.getClearingState() == ClearingState.APPROVED;
    }

    private List<Attachment> findRelatedAttachmentsByChecksum(String checksum) {
        try {
            String checksumType = duplicationDetectionService.validateChecksumFormat(checksum);
            if (checksumType != null) {
                return checksumRepository.getAttachmentsByChecksum(checksum, checksumType);
            }
        } catch (Exception e) {
            log.warn("Error finding attachments by checksum {}: {}", checksum, e.getMessage());
        }
        return new ArrayList<>();
    }

    private void extractClearingInformation(Attachment attachment, Release release, ReuseableDecision decision) {
        if (release.getMainLicenseIds() != null) {
            decision.setLicenseIds(new ArrayList<>(release.getMainLicenseIds()));
        }
        
        decision.setComment("Inherited via FOSSology Reuser agent from " + 
                          release.getName() + " " + release.getVersion());
    }

    private void populateDecisionMetadata(Attachment attachment, Release release, ReuseableDecision decision) {
        Map<String, Object> decisionData = decision.getDecisionData();
        decisionData.put("filename", attachment.getFilename());
        decisionData.put("attachmentType", attachment.getAttachmentType() != null ? 
                attachment.getAttachmentType().toString() : "UNKNOWN");
        decisionData.put("sourceReleaseName", release.getName());
        decisionData.put("sourceReleaseVersion", release.getVersion());
        decisionData.put("sourceComponentId", release.getComponentId());
        decisionData.put("clearingState", release.getClearingState() != null ? 
                release.getClearingState().toString() : "UNKNOWN");
        decisionData.put("fossologyUploadId", attachment.getFossologyUploadId());
    }

    private ConfidenceLevel adjustConfidenceLevel(ConfidenceLevel original, int warningCount, int errorCount) {
        if (errorCount > 0) {
            return ConfidenceLevel.LOW;
        }
        if (warningCount > 2) {
            return original == ConfidenceLevel.HIGH ? ConfidenceLevel.MEDIUM : ConfidenceLevel.LOW;
        }
        if (warningCount > 0 && original == ConfidenceLevel.HIGH) {
            return ConfidenceLevel.MEDIUM;
        }
        return original;
    }

    private String generateInheritanceRecommendation(ConfidenceLevel confidence, List<String> warnings, List<String> errors) {
        if (!errors.isEmpty()) {
            return "Manual review required - validation errors must be resolved";
        }
        
        return confidence.getRecommendation() + 
               (warnings.isEmpty() ? "" : " (Note: " + warnings.size() + " warnings)");
    }

    private String calculateRiskLevel(ConfidenceLevel confidence, int warningCount, int errorCount) {
        if (errorCount > 0) return "HIGH";
        if (confidence == ConfidenceLevel.LOW || warningCount > 3) return "MEDIUM";
        if (warningCount > 0 || confidence == ConfidenceLevel.MEDIUM) return "LOW";
        return "MINIMAL";
    }

    private boolean isOrganizationCompatible(ReuseableDecision decision, User user) {
        String userOrg = extractOrganizationId(user);
        return decision.getOrganizationId() == null || userOrg.equals(decision.getOrganizationId());
    }

    private boolean hasLicenseConflicts(ReuseableDecision decision, Release targetRelease) {
        if (decision.getLicenseIds() == null || targetRelease.getMainLicenseIds() == null) {
            return false;
        }
        
        for (String sourceLicense : decision.getLicenseIds()) {
            for (String targetLicense : targetRelease.getMainLicenseIds()) {
                if (conflictingLicenses.contains(sourceLicense + ":" + targetLicense) ||
                    conflictingLicenses.contains(targetLicense + ":" + sourceLicense)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isDecisionExpired(ReuseableDecision decision) {
        if (decision.getCreatedDate() == null) return false;
        long daysSinceCreation = (System.currentTimeMillis() - decision.getCreatedDate().getTime()) / (1000 * 60 * 60 * 24);
        return daysSinceCreation > 1095; // 3 years
    }

    private boolean isFileTypeCompatible(ReuseableDecision decision, Attachment targetAttachment) {
        Object sourceFilename = decision.getDecisionData().get("filename");
        return isFileTypeCompatible(sourceFilename != null ? sourceFilename.toString() : null, 
                                  targetAttachment.getFilename());
    }

    private boolean isFileTypeCompatible(String sourceFilename, String targetFilename) {
        if (sourceFilename == null || targetFilename == null) {
            return true; // Assume compatible if filename not available
        }
        
        String sourceExt = getFileExtension(sourceFilename);
        String targetExt = getFileExtension(targetFilename);
        
        return sourceExt.equals(targetExt) || 
               (isArchiveType(sourceExt) && isArchiveType(targetExt)) ||
               (isSourceCodeType(sourceExt) && isSourceCodeType(targetExt));
    }

    private String getFileExtension(String filename) {
        if (filename == null) return "";
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot).toLowerCase();
        }
        return "";
    }

    private boolean isArchiveType(String extension) {
        Set<String> archiveTypes = Set.of(".zip", ".tar", ".gz", ".bz2", ".tgz", ".tar.gz", ".tar.bz2", ".7z", ".rar");
        return archiveTypes.contains(extension);
    }

    private boolean isSourceCodeType(String extension) {
        Set<String> sourceTypes = Set.of(".java", ".c", ".cpp", ".h", ".hpp", ".py", ".js", ".ts", ".go", ".rs");
        return sourceTypes.contains(extension);
    }

    private boolean hasUserPermission(User user, ReuseableDecision decision) {
        if (user.getUserGroup() == null) {
            return false;
        }
        
        switch (user.getUserGroup()) {
            case ADMIN:
            case CLEARING_ADMIN:
                return true;
            case CLEARING_EXPERT:
                return decision.getDecisionType() != ReuseDecisionType.EXPORT_RESTRICTION;
            case USER:
                return decision.getConfidence() == ConfidenceLevel.HIGH && 
                       decision.getDecisionType() == ReuseDecisionType.IDENTICAL_FILE;
            default:
                return false;
        }
    }

    private boolean isOrganizationPolicyCompliant(ReuseableDecision decision, Release targetRelease, User user) {
        // Check organizational policies for inheritance
        String userOrg = extractOrganizationId(user);
        return decision.getOrganizationId() == null || userOrg.equals(decision.getOrganizationId());
    }

    private boolean isComponentLevelApplicable(ReuseableDecision decision, Release targetRelease) {
        Object sourceComponentId = decision.getDecisionData().get("sourceComponentId");
        return sourceComponentId != null && sourceComponentId.equals(targetRelease.getComponentId());
    }

    private List<String> findIncompatibleLicenses(List<String> licenseIds, Release targetRelease) {
        List<String> incompatible = new ArrayList<>();
        if (targetRelease.getMainLicenseIds() != null) {
            for (String sourceLicense : licenseIds) {
                for (String targetLicense : targetRelease.getMainLicenseIds()) {
                    if (conflictingLicenses.contains(sourceLicense + ":" + targetLicense)) {
                        incompatible.add(sourceLicense + " conflicts with " + targetLicense);
                    }
                }
            }
        }
        return incompatible;
    }

    private List<String> findLicenseConflicts(List<String> licenseIds) {
        List<String> conflicts = new ArrayList<>();
        for (int i = 0; i < licenseIds.size(); i++) {
            for (int j = i + 1; j < licenseIds.size(); j++) {
                String pair = licenseIds.get(i) + ":" + licenseIds.get(j);
                if (conflictingLicenses.contains(pair)) {
                    conflicts.add(pair);
                }
            }
        }
        return conflicts;
    }

    private boolean isClearingStateCompatible(ClearingState source, ClearingState target) {
        // Define compatibility matrix for clearing states
        return source == target || 
               (source == ClearingState.APPROVED && target != ClearingState.NEW_CLEARING) ||
               (source == ClearingState.REPORT_AVAILABLE && target == ClearingState.UNDER_CLEARING);
    }

    private String categorizeError(String error) {
        if (error.contains("checksum")) return "CHECKSUM_ERROR";
        if (error.contains("license")) return "LICENSE_ERROR";
        if (error.contains("authorization")) return "AUTHORIZATION_ERROR";
        if (error.contains("organization")) return "ORGANIZATION_ERROR";
        if (error.contains("validation")) return "VALIDATION_ERROR";
        return "GENERAL_ERROR";
    }

    private String determineOverallStatus(int applied, int skipped, int errors) {
        if (errors == 0 && skipped == 0) return "SUCCESS";
        if (applied > 0 && errors == 0) return "PARTIAL_SUCCESS";
        if (applied > 0) return "COMPLETED_WITH_ERRORS";
        return "FAILED";
    }

    private boolean isRecentDecision(Date decisionDate) {
        long hoursSinceDecision = (System.currentTimeMillis() - decisionDate.getTime()) / (1000 * 60 * 60);
        return hoursSinceDecision < 24; // Consider decisions from last 24 hours as recent
    }

    private Map<String, Object> createInheritanceRecord(ReuseableDecision decision, Release targetRelease, User user) {
        Map<String, Object> record = new HashMap<>();
        record.put("id", UUID.randomUUID().toString());
        record.put("targetReleaseId", targetRelease.getId());
        record.put("sourceReleaseId", decision.getSourceReleaseId());
        record.put("checksum", decision.getChecksum());
        record.put("inheritedAt", new Date());
        record.put("inheritedBy", user.getEmail());
        record.put("decisionType", decision.getDecisionType().toString());
        record.put("confidence", decision.getConfidence().toString());
        record.put("licenseIds", decision.getLicenseIds());
        record.put("copyrightStatements", decision.getCopyrightStatements());
        record.put("fossologyUploadId", decision.getFossologyUploadId());
        record.put("organizationId", decision.getOrganizationId());
        record.put("status", "ACTIVE");
        return record;
    }

    private boolean storeInheritanceRecord(Map<String, Object> record) {
        // Integration point for database storage
        log.info("Storing inheritance record: {}", record.get("id"));
        return true;
    }

    private boolean updateClearingStatus(Release targetRelease, ReuseableDecision decision, User user) {
        // Integration point for updating release clearing status
        log.debug("Updating clearing status for release: {}", targetRelease.getId());
        return true;
    }

    private void sendInheritanceNotifications(ReuseableDecision decision, Release targetRelease, User user) {
        // Integration point for sending notifications
        log.debug("Sending inheritance notification for release: {}", targetRelease.getId());
    }
}
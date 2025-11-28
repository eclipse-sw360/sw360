/*
 * Copyright Ritankar Saha<ritankar.saha786@gmail.com>, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

include "users.thrift"
include "sw360.thrift"
include "attachments.thrift"
include "components.thrift"

namespace java org.eclipse.sw360.datahandler.thrift.reuse
namespace php sw360.thrift.reuse

typedef sw360.RequestStatus RequestStatus
typedef sw360.RequestSummary RequestSummary
typedef sw360.SW360Exception SW360Exception
typedef users.User User
typedef attachments.Attachment Attachment
typedef components.Release Release

enum ReuseDecisionType {
    IDENTICAL_FILE = 0,
    SIMILAR_FILE = 1,
    COMPONENT_LEVEL = 2,
    LICENSE_CONCLUSION = 3,
    COPYRIGHT_STATEMENT = 4,
    EXPORT_RESTRICTION = 5
}

enum ReuseScope {
    FILE_LEVEL = 0,
    COMPONENT_LEVEL = 1,
    PROJECT_LEVEL = 2,
    ORGANIZATION_LEVEL = 3
}

enum ConfidenceLevel {
    LOW = 0,
    MEDIUM = 1,
    HIGH = 2,
    VERIFIED = 3
}

enum ReuseStatus {
    PENDING = 0,
    APPLIED = 1,
    REJECTED = 2,
    EXPIRED = 3
}

struct ReuseableDecision {
    1: optional string id,
    2: optional string revision,
    3: optional string type = "reuseableDecision",

    // Core reuse information
    10: required string sourceReleaseId,
    11: required string sourceAttachmentId,
    12: optional string checksum,
    13: optional string checksumType,
    14: required ReuseDecisionType decisionType,
    15: required ReuseScope scope,
    16: required ConfidenceLevel confidence,

    // Decision metadata
    20: optional i64 createdDate,
    21: optional string createdBy,
    22: optional string clearingTeam,
    23: optional string fossologyUploadId,
    24: optional string organizationId,
    25: optional bool approved,

    // Clearing information
    30: optional list<string> licenseIds,
    31: optional string copyrightText,
    32: optional string exportRestrictionClass,
    33: optional string clearingComment,
    34: optional components.ClearingState clearingState,

    // Decision data (flexible key-value storage)
    40: optional map<string, string> decisionData,

    // Validation and expiry
    50: optional i64 validUntil,
    51: optional string validationComment,
    52: optional bool requiresReview,

    // Usage tracking
    60: optional i32 usageCount,
    61: optional i64 lastUsed,
    62: optional list<string> usedByReleases
}

struct ReuseStatistics {
    1: optional string id,
    2: optional string revision,
    3: optional string type = "reuseStatistics",

    // Basic statistics
    10: required string releaseId,
    11: required i32 totalFiles,
    12: required i32 reusableFiles,
    13: required double reusePercentage,
    14: required i64 analysisDate,

    // Detailed breakdowns
    20: optional map<string, i32> reuseByType,          // ReuseDecisionType -> count
    21: optional map<string, i32> reuseByConfidence,    // ConfidenceLevel -> count
    22: optional map<string, i32> reuseBySource,        // source release ID -> count
    23: optional map<string, double> licenseReuse,      // license ID -> reuse percentage

    // FOSSology integration
    30: optional list<string> fossologyUploadIds,
    31: optional map<string, i32> uploadReuseCounts,
    32: optional bool fossologyReuserEnabled,

    // Performance metrics
    40: optional i64 analysisTimeMs,
    41: optional string analysisVersion,
    42: optional string analyzedBy
}

struct ReuseCandidate {
    1: optional string id,
    2: optional string revision,
    3: optional string type = "reuseCandidate",

    // Candidate identification
    10: required string targetReleaseId,
    11: required string targetAttachmentId,
    12: required string sourceReleaseId,
    13: required string sourceAttachmentId,

    // Matching information
    20: required string checksum,
    21: required string checksumType,
    22: required double similarityScore,
    23: required ConfidenceLevel matchConfidence,

    // Reusable decision information
    30: optional ReuseableDecision availableDecision,
    31: optional list<string> compatibleLicenses,
    32: optional bool requiresApproval,

    // Discovery metadata
    40: optional i64 discoveredDate,
    41: optional string discoveryMethod,
    42: optional map<string, string> matchingMetadata
}

struct ReuseApplication {
    1: optional string id,
    2: optional string revision,
    3: optional string type = "reuseApplication",

    // Application context
    10: required string targetReleaseId,
    11: required string appliedBy,
    12: required i64 applicationDate,
    13: required ReuseStatus status,

    // Applied decisions
    20: required list<ReuseableDecision> appliedDecisions,
    21: optional i32 totalDecisionsApplied,
    22: optional i32 successfulApplications,
    23: optional i32 failedApplications,

    // Application options
    30: optional bool reuseMain,
    31: optional bool reuseEnhanced,
    32: optional bool reuseCopyright,
    33: optional bool reuseReport,

    // Result information
    40: optional string applicationComment,
    41: optional list<string> errors,
    42: optional list<string> warnings,
    43: optional map<string, string> applicationMetadata
}

struct ReuseConfiguration {
    1: optional string id,
    2: optional string revision,
    3: optional string type = "reuseConfiguration",

    // Configuration scope
    10: optional string organizationId,
    11: optional string projectId,
    12: optional string componentId,

    // Reuse policies
    20: optional bool enableAutomaticReuse,
    21: optional bool requireApprovalForReuse,
    22: optional i32 maxReuseAge,                       // in days
    23: optional double minimumConfidenceLevel,
    24: optional list<ReuseDecisionType> allowedTypes,

    // FOSSology integration
    30: optional bool enableFossologyReuser,
    31: optional bool autoApplyIdenticalFiles,
    32: optional bool requireFossologyValidation,

    // Notification settings
    40: optional bool notifyOnReuse,
    41: optional list<string> notificationEmails,
    42: optional bool generateReuseReports,

    // Configuration metadata
    50: optional string createdBy,
    51: optional i64 createdDate,
    52: optional string lastModifiedBy,
    53: optional i64 lastModifiedDate
}

service ReuseService {

    /**
     * Analyze reuse potential for a release
     */
    ReuseStatistics analyzeReusePotential(1: string releaseId, 2: User user) throws (1: SW360Exception exp);

    /**
     * Find reuse candidates for a release
     */
    list<ReuseCandidate> findReuseCandidates(1: string releaseId, 2: User user, 3: i32 maxResults) throws (1: SW360Exception exp);

    /**
     * Find reuse candidates for specific attachment
     */
    list<ReuseCandidate> findAttachmentReuseCandidates(1: string attachmentId, 2: User user) throws (1: SW360Exception exp);

    /**
     * Create a reuseable decision
     */
    ReuseableDecision createReuseableDecision(1: ReuseableDecision decision, 2: User user) throws (1: SW360Exception exp);

    /**
     * Get reuseable decision by ID
     */
    ReuseableDecision getReuseableDecision(1: string decisionId, 2: User user) throws (1: SW360Exception exp);

    /**
     * Update a reuseable decision
     */
    ReuseableDecision updateReuseableDecision(1: ReuseableDecision decision, 2: User user) throws (1: SW360Exception exp);

    /**
     * Delete a reuseable decision
     */
    RequestStatus deleteReuseableDecision(1: string decisionId, 2: User user) throws (1: SW360Exception exp);

    /**
     * Find reuseable decisions by checksum
     */
    list<ReuseableDecision> getReuseableDecisionsByChecksum(1: string checksum, 2: string checksumType, 3: User user) throws (1: SW360Exception exp);

    /**
     * Find reuseable decisions by source release
     */
    list<ReuseableDecision> getReuseableDecisionsBySourceRelease(1: string sourceReleaseId, 2: User user) throws (1: SW360Exception exp);

    /**
     * Apply reuse decisions to a release
     */
    ReuseApplication applyReuse(1: string targetReleaseId, 2: list<string> decisionIds, 3: User user, 4: map<string, string> options) throws (1: SW360Exception exp);

    /**
     * Apply FOSSology reuser agent
     */
    RequestStatus enableFossologyReuser(1: string releaseId, 2: i32 sourceUploadId, 3: User user) throws (1: SW360Exception exp);

    /**
     * Get reuse application status
     */
    ReuseApplication getReuseApplication(1: string applicationId, 2: User user) throws (1: SW360Exception exp);

    /**
     * Get reuse applications for a release
     */
    list<ReuseApplication> getReuseApplicationsByRelease(1: string releaseId, 2: User user) throws (1: SW360Exception exp);

    /**
     * Get comprehensive reuse statistics for multiple releases
     */
    map<string, ReuseStatistics> getComprehensiveReuseStatistics(1: list<string> releaseIds, 2: User user) throws (1: SW360Exception exp);

    /**
     * Validate reuse decision compatibility
     */
    list<string> validateReuseCompatibility(1: string targetReleaseId, 2: list<string> decisionIds, 3: User user) throws (1: SW360Exception exp);

    /**
     * Get reuse configuration for context
     */
    ReuseConfiguration getReuseConfiguration(1: string contextId, 2: string contextType, 3: User user) throws (1: SW360Exception exp);

    /**
     * Update reuse configuration
     */
    ReuseConfiguration updateReuseConfiguration(1: ReuseConfiguration configuration, 2: User user) throws (1: SW360Exception exp);

    /**
     * Generate reuse report for a release
     */
    string generateReuseReport(1: string releaseId, 2: User user, 3: string format) throws (1: SW360Exception exp);

    /**
     * Get reuse inheritance chain for a decision
     */
    list<ReuseableDecision> getReuseInheritanceChain(1: string decisionId, 2: User user) throws (1: SW360Exception exp);

    /**
     * Bulk create reuseable decisions
     */
    list<ReuseableDecision> createBulkReuseableDecisions(1: list<ReuseableDecision> decisions, 2: User user) throws (1: SW360Exception exp);

    /**
     * Search reuseable decisions
     */
    list<ReuseableDecision> searchReuseableDecisions(1: map<string, string> searchCriteria, 2: User user, 3: i32 limit) throws (1: SW360Exception exp);

    /**
     * Get reuse trends and analytics
     */
    map<string, string> getReuseAnalytics(1: string organizationId, 2: i64 startDate, 3: i64 endDate, 4: User user) throws (1: SW360Exception exp);
}
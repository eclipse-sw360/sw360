/*
 * Copyright Siemens AG, 2013-2015, 2019. Part of the SW360 Portal Project.
 * Copyright Ritankar Saha<ritankar.saha786@gmail.com>, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.fossology;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.FossologyUtils;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.thrift.ConfigContainer;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.attachments.*;
import org.eclipse.sw360.datahandler.thrift.components.*;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService.Iface;
import org.eclipse.sw360.datahandler.thrift.fossology.FossologyService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.fossology.config.FossologyRestConfig;
import org.eclipse.sw360.fossology.rest.FossologyRestClient;
import org.eclipse.sw360.fossology.rest.model.FossologyV2Models.CombinedUploadJobResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.eclipse.sw360.datahandler.common.SW360ConfigKeys.DISABLE_CLEARING_FOSSOLOGY_REPORT_DOWNLOAD;

/**
 * Implementation of the Thrift service with v2 API support.
 * Optimized for FOSSology v2 API endpoints with enhanced functionality.
 */
@Component
public class FossologyHandler implements FossologyService.Iface {

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final ThriftClients thriftClients;
    private final FossologyRestConfig fossologyRestConfig;
    private final FossologyRestClient fossologyRestClient;
    private final AttachmentConnector attachmentConnector;

    // v2 API status constants
    private static final String V2_STATUS_SUCCESS = "success";
    private static final String V2_STATUS_PROCESSING = "Processing";
    private static final String V2_STATUS_COMPLETED = "Completed";
    private static final String V2_STATUS_FAILED = "Failed";
    private static final String V2_STATUS_QUEUED = "Queued";

    boolean reportStep = false;

    public FossologyHandler(ThriftClients thriftClients, FossologyRestConfig fossologyRestConfig,
            FossologyRestClient fossologyRestClient, AttachmentConnector attachmentConnector) {
        this.thriftClients = thriftClients;
        this.fossologyRestConfig = fossologyRestConfig;
        this.fossologyRestClient = fossologyRestClient;
        this.attachmentConnector = attachmentConnector;
    }

    @Override
    public RequestStatus setFossologyConfig(ConfigContainer newConfig) throws TException {
        try {
            return fossologyRestConfig.update(newConfig).getConfigKeyToValues().equals(newConfig.getConfigKeyToValues())
                    ? RequestStatus.SUCCESS
                    : RequestStatus.FAILURE;
        } catch (IllegalStateException e) {
            throw new SW360Exception(e.getMessage()); // Convert to something Thrift allows
        }
    }

    @Override
    public ConfigContainer getFossologyConfig() throws TException {
        return fossologyRestConfig.get();
    }

    @Override
    public RequestStatus checkConnection() throws TException {
        return fossologyRestClient.checkConnection() ? RequestStatus.SUCCESS : RequestStatus.FAILURE;
    }

    @Override
    public RequestStatus markFossologyProcessOutdated(String releaseId, User user) throws TException {
        ExternalToolProcess fossologyProcess;

        Iface componentClient = thriftClients.makeComponentClient();
        Release release;
        try {
            release = componentClient.getReleaseById(releaseId, user);
            Set<ExternalToolProcess> fossologyProcesses = SW360Utils.getNotOutdatedExternalToolProcessesForTool(release,
                    ExternalTool.FOSSOLOGY);

            if (isIllegalStateFossologyProcesses(releaseId, fossologyProcesses)) {
                return RequestStatus.FAILURE;
            }

            if (fossologyProcesses.size() == 0) {
                log.info("No FOSSology process found for release with id {}, so nothing to set to OUTDATED.",
                        releaseId);
                return RequestStatus.SUCCESS;
            } else {
                // after illegal state check, we know exactly 1 fossology process
                fossologyProcess = fossologyProcesses.iterator().next();
                fossologyProcess.setProcessStatus(ExternalToolProcessStatus.OUTDATED);
                release.setClearingState(calculateCurrentClearingState(release, fossologyProcess));
                componentClient.updateReleaseFossology(release, user);
                return RequestStatus.SUCCESS;
            }
        } catch (TException e) {
            log.error("Could not set FOSSology process to status OUTDATED for release id " + releaseId
                    + " because of exceptions in components backend: ", e);
            return RequestStatus.FAILURE;
        }
    }

    @Override
    public ExternalToolProcess process(String releaseId, User user, String uploadDescription) throws TException {
        ExternalToolProcess fossologyProcess;

        Iface componentClient = thriftClients.makeComponentClient();
        Release release = componentClient.getReleaseById(releaseId, user);

        Set<ExternalToolProcess> fossologyProcesses = SW360Utils.getNotOutdatedExternalToolProcessesForTool(release,
                ExternalTool.FOSSOLOGY);
        if (isIllegalStateFossologyProcesses(releaseId, fossologyProcesses)) {
            return null;
        }

        Set<Attachment> sourceAttachments = componentClient.getSourceAttachments(release.getId());
        if (isIllegalStateSourceAttachments(releaseId, sourceAttachments)) {
            return null;
        }

        Attachment sourceAttachment = sourceAttachments.iterator().next();
        if (isIllegalStateProcessAndAttachments(fossologyProcesses, sourceAttachment)) {
            return null;
        }

        if (fossologyProcesses.size() == 0) {
            fossologyProcess = createFossologyProcess(release, user, sourceAttachment.getAttachmentContentId(),
                    sourceAttachment.getSha1());
        } else {
            // after illegal state check, we know exactly 1 fossology process
            fossologyProcess = fossologyProcesses.iterator().next();
        }

        FossologyUtils.ensureOrderOfProcessSteps(fossologyProcess);

        ExternalToolProcessStep furthestStep = fossologyProcess.getProcessSteps().getLast();

        // Handle different process steps using v2 API
        if (FossologyUtils.FOSSOLOGY_STEP_NAME_UPLOAD.equals(furthestStep.getStepName())) {
            handleUploadStepV2(componentClient, release, user, fossologyProcess, sourceAttachment, uploadDescription);
        } else if (FossologyUtils.FOSSOLOGY_STEP_NAME_SCAN.equals(furthestStep.getStepName())) {
            handleScanStepV2(componentClient, release, user, fossologyProcess);
        } else if(!SW360Utils.readConfig(DISABLE_CLEARING_FOSSOLOGY_REPORT_DOWNLOAD, false) && FossologyUtils.FOSSOLOGY_STEP_NAME_REPORT.equals(furthestStep.getStepName())) {
            handleReportStepV2(componentClient, release, user, fossologyProcess);
        } else if(reportStep && FossologyUtils.FOSSOLOGY_STEP_NAME_REPORT.equals(furthestStep.getStepName())) {
            handleReportStepV2(componentClient, release, user, fossologyProcess);
        }

        updateFossologyProcessInRelease(fossologyProcess, release, user, componentClient);

        return fossologyProcess;
    }

    /**
     * Start the upload to FOSSology and the scan jobs with it. Wait till the
     * upload is done and pass to scan started step with job id.
     */
    private void handleUploadStepV2(Iface componentClient, Release release, User user,
            ExternalToolProcess fossologyProcess, Attachment sourceAttachment, String uploadDescription) throws TException {
        ExternalToolProcessStep furthestStep = fossologyProcess.getProcessSteps()
                .getLast();

        switch (furthestStep.getStepStatus()) {
            case NEW:

                fossologyProcess.setProcessStatus(ExternalToolProcessStatus.IN_WORK);
                furthestStep.setStepStatus(ExternalToolProcessStatus.IN_WORK);
                updateFossologyProcessInRelease(fossologyProcess, release, user, componentClient);

                String attachmentFilename = sourceAttachment.getFilename();
                if (StringUtils.isEmpty(attachmentFilename)) {
                    attachmentFilename = "unknown-filename";
                }
                String attachmentContentId = sourceAttachment.getAttachmentContentId();
                AttachmentContent attachmentContent = attachmentConnector.getAttachmentContent(attachmentContentId);

                String shaValue = sourceAttachment.getSha1();

                // Check if file already exists using v2 API
                int existingUploadId = fossologyRestClient.getUploadId(shaValue, attachmentFilename);
                if (existingUploadId > -1) {
                    log.info("FILE ALREADY EXISTS with uploadId {}, marking upload as DONE and proceeding to scan check", existingUploadId);
                    furthestStep.setFinishedOn(Instant.now().toString());
                    furthestStep.setStepStatus(ExternalToolProcessStatus.DONE);
                    furthestStep.setProcessStepIdInTool(existingUploadId + "");
                    furthestStep.setResult(existingUploadId + "");
                } else {
                    // Upload file using v2 API with automatic scan scheduling
                    InputStream attachmentStream = attachmentConnector.getAttachmentStream(attachmentContent, user, release);
                    log.info("STARTING UPLOAD for file {}", attachmentFilename);
                    CombinedUploadJobResponse response = fossologyRestClient.uploadFileAndScan(
                        attachmentFilename, attachmentStream, uploadDescription);

                    if (response != null && V2_STATUS_SUCCESS.equals(response.getStatus())) {
                        int jobId = fossologyRestClient.getJobIdAfterScan(response.getUploadId());
                        if (jobId > 0) {
                            response.setJobId(jobId);
                        } else {
                            response.setMessage("Unable to find latest job id for upload " + response.getUploadId());
                            response.setStatus(V2_STATUS_FAILED);
                            furthestStep.setStepStatus(ExternalToolProcessStatus.NEW);
                            furthestStep.setResult(response.getMessage());
                            log.error("Unable to find latest job id for upload: {}", response.getMessage());
                        }
                    }
                    if (response != null && response.getUploadId() > 0 && response.getJobId() > 0) {
                        furthestStep.setFinishedOn(Instant.now().toString());
                        furthestStep.setStepStatus(ExternalToolProcessStatus.DONE);
                        furthestStep.setProcessStepIdInTool(response.getUploadId() + "");
                        furthestStep.setResult(response.getUploadId() + "");

                        log.info("UPLOAD SUCCESSFUL: uploadId={}", response.getUploadId());

                        ExternalToolProcessStep scanStep = createFossologyProcessStep(user, FossologyUtils.FOSSOLOGY_STEP_NAME_SCAN);
                        scanStep.setStepStatus(ExternalToolProcessStatus.IN_WORK);
                        scanStep.setProcessStepIdInTool(response.getJobId() + "");
                        fossologyProcess.addToProcessSteps(scanStep);
                        log.info("AUTO-SCAN STARTED: uploadId={}, jobId={}", response.getUploadId(), response.getJobId());
                    } else {
                        furthestStep.setStepStatus(ExternalToolProcessStatus.NEW);
                        furthestStep.setResult(response != null ? response.getMessage() : "Upload failed");
                        log.error("UPLOAD FAILED: {}", response != null ? response.getMessage() : "Unknown error");
                    }
                }
                break;
            case DONE:
                // Start scan if not already started automatically
                log.info("Upload completed, checking if scan needs to be started");
                fossologyProcess.addToProcessSteps(createFossologyProcessStep(user, FossologyUtils.FOSSOLOGY_STEP_NAME_SCAN));
                handleScanStepV2(componentClient, release, user, fossologyProcess);
                break;
            case IN_WORK:
                // Do nothing, upload should happen in another thread
                log.debug("Upload in progress...");
            default:
                // Do nothing, unknown status
        }
    }

    /**
     * Handle scan step using v2 API
     */
    private void handleScanStepV2(Iface componentClient,
                             Release release,
                             User user,
                             ExternalToolProcess fossologyProcess) throws TException {
        ExternalToolProcessStep furthestStep =
            fossologyProcess.getProcessSteps().getLast();
        String uploadId = SW360Utils
            .getExternalToolProcessStepOfFirstProcessForTool(release, ExternalTool.FOSSOLOGY,
                                                             FossologyUtils.FOSSOLOGY_STEP_NAME_UPLOAD)
            .getResult();

        switch (furthestStep.getStepStatus()) {
            case NEW:
                // Start scan using v2 API
                furthestStep.setStepStatus(ExternalToolProcessStatus.IN_WORK);
                updateFossologyProcessInRelease(fossologyProcess, release, user, componentClient);

                log.info("STARTING SCAN for uploadId: {}", uploadId);
                int jobId = fossologyRestClient.startScanning(Integer.parseInt(uploadId));
                if (jobId > -1) {
                    furthestStep.setProcessStepIdInTool(String.valueOf(jobId));
                    furthestStep.setResult(null);
                    log.info("SCAN STARTED successfully with jobId: {}", jobId);
                } else {
                    furthestStep.setStepStatus(ExternalToolProcessStatus.NEW);
                    furthestStep.setResult(String.valueOf(jobId));
                    log.error("FAILED to start scan job for uploadId: {}", uploadId);
                }
                break;

            case IN_WORK:
                // Query scan status using v2 API
                int scanningJobId = Integer.parseInt(furthestStep.getProcessStepIdInTool());
                Map<String, String> statusResponse = fossologyRestClient.checkScanStatus(scanningJobId);
                int status = scanStatusCodeV2(statusResponse);

                log.debug("Checking scan status for jobId {}: status={}", scanningJobId, statusResponse.get("status"));

                if (status > 0) {
                    // SCAN COMPLETED SUCCESSFULLY
                    furthestStep.setFinishedOn(Instant.now().toString());
                    furthestStep.setStepStatus(ExternalToolProcessStatus.DONE);
                    furthestStep.setResult(String.valueOf(status));

                    log.info("SCAN COMPLETED SUCCESSFULLY for jobId: {}", scanningJobId);

                    // AUTOMATICALLY TRIGGER REPORT GENERATION
                    boolean reportDisabled = SW360Utils.readConfig(DISABLE_CLEARING_FOSSOLOGY_REPORT_DOWNLOAD, false);
                    log.info("Report generation disabled config: {}", reportDisabled);

                    if (!reportDisabled) {
                        log.info("AUTOMATICALLY STARTING REPORT GENERATION after scan completion");

                        // Create report step immediately
                        ExternalToolProcessStep reportStep =
                            createFossologyProcessStep(user, FossologyUtils.FOSSOLOGY_STEP_NAME_REPORT);
                        fossologyProcess.addToProcessSteps(reportStep);

                        // Persist the NEW report step
                        updateFossologyProcessInRelease(fossologyProcess, release, user, componentClient);

                        // Start report generation in the same process cycle
                        handleReportStepV2(componentClient, release, user, fossologyProcess);

                        // Persist any IN_WORK or DONE updates from handleReportStepV2
                        updateFossologyProcessInRelease(fossologyProcess, release, user, componentClient);
                    }
                } else if (status == -1) {
                    // SCAN FAILED
                    furthestStep.setFinishedOn(Instant.now().toString());
                    furthestStep.setStepStatus(ExternalToolProcessStatus.DONE);
                    furthestStep.setResult(String.valueOf(status));
                    log.error("SCAN FAILED for jobId: {}", scanningJobId);
                } else {
                    // SCAN STILL IN PROGRESS
                    if (statusResponse.containsKey("eta")) {
                        log.debug("Scan in progress for jobId {}, ETA: {} seconds",
                                  scanningJobId, statusResponse.get("eta"));
                    }
                    // leave in_work for next cycle
                }
                break;

            case DONE:
                // Check if we need to start report generation
                boolean reportDisabled = SW360Utils.readConfig(DISABLE_CLEARING_FOSSOLOGY_REPORT_DOWNLOAD, false);
                log.info("Scan already completed, checking if report step needed. Report disabled: {}", reportDisabled);

                if (!reportDisabled) {
                    boolean hasReportStep = fossologyProcess.getProcessSteps().stream()
                        .anyMatch(step -> FossologyUtils.FOSSOLOGY_STEP_NAME_REPORT.equals(step.getStepName()));
                    if (!hasReportStep) {
                        log.info("Adding report step after scan completion for upload {}", uploadId);

                        ExternalToolProcessStep reportStep =
                            createFossologyProcessStep(user, FossologyUtils.FOSSOLOGY_STEP_NAME_REPORT);
                        fossologyProcess.addToProcessSteps(reportStep);

                        // Persist the NEW report step
                        updateFossologyProcessInRelease(fossologyProcess, release, user, componentClient);

                        // Kick off the report generation right away
                        handleReportStepV2(componentClient, release, user, fossologyProcess);

                        // Persist any IN_WORK or DONE updates from handleReportStepV2
                        updateFossologyProcessInRelease(fossologyProcess, release, user, componentClient);
                    }
                }
                break;

            default:
                log.warn("Unknown scan status: {}", furthestStep.getStepStatus());
        }
    }

    /**
     * Handle report step using v2 API - ENHANCED VERSION
     */
    private void handleReportStepV2(Iface componentClient, Release release, User user,
            ExternalToolProcess fossologyProcess) throws TException {
        ExternalToolProcessStep furthestStep = fossologyProcess.getProcessSteps()
                .getLast();

        switch (furthestStep.getStepStatus()) {
            case NEW:
                // Generate report using v2 API
                furthestStep.setStepStatus(ExternalToolProcessStatus.IN_WORK);
                updateFossologyProcessInRelease(fossologyProcess, release, user, componentClient);

                String uploadId = SW360Utils.getExternalToolProcessStepOfFirstProcessForTool(release,
                        ExternalTool.FOSSOLOGY, FossologyUtils.FOSSOLOGY_STEP_NAME_UPLOAD).getResult();

                log.info("STARTING REPORT GENERATION for uploadId: {}", uploadId);
                int reportId = fossologyRestClient.startReport(Integer.parseInt(uploadId));
                if (reportId > -1) {
                    furthestStep.setProcessStepIdInTool(reportId + "");
                    log.info("REPORT GENERATION STARTED with reportId: {} for uploadId: {}", reportId, uploadId);
                } else {
                    furthestStep.setStepStatus(ExternalToolProcessStatus.NEW);
                    furthestStep.setResult("Failed to start report generation");
                    log.error("FAILED to start report generation for uploadId: {}", uploadId);
                }
                break;
            case IN_WORK:
                // Try to download report using v2 API
                updateFossologyProcessInRelease(fossologyProcess, release, user, componentClient);
                int reportJobId = Integer.parseInt(furthestStep.getProcessStepIdInTool());
                log.debug(" Attempting to download report for reportId: {}", reportJobId);
                InputStream reportStream = fossologyRestClient.getReport(reportJobId);
                if (reportStream != null) {
                        String attachmentContentId = attachReportToRelease(componentClient, release, user, reportStream);
                        furthestStep.setFinishedOn(Instant.now().toString());
                        furthestStep.setResult(attachmentContentId);
                        fossologyProcess.setProcessStatus(ExternalToolProcessStatus.DONE);
                        furthestStep.setStepStatus(ExternalToolProcessStatus.DONE);
                        log.info("REPORT SUCCESSFULLY DOWNLOADED AND ATTACHED! ReportId: {}, AttachmentId: {}",
                                reportJobId, attachmentContentId);

                } else {
                    log.debug("Report not ready yet for reportId: {}, will retry later", reportJobId);
                    furthestStep.setStepStatus(ExternalToolProcessStatus.IN_WORK);
                }
                break;
            case DONE:
                // Do nothing, last step is already done
                log.debug("Report step already completed");
                break;
            default:
                // Do nothing, unknown status
                log.warn("Unknown report status: {}", furthestStep.getStepStatus());
        }
    }

    private void updateFossologyProcessInRelease(ExternalToolProcess fossologyProcess, Release release, User user,
            Iface componentClient) throws TException {
        // because our workflow in between might have taken some time, we have to
        // refetch the release to get the current version (as another thread might have
        // written changes to the release which results in a new version so that we
        // would get a conflict error on trying to write)
        release = componentClient.getReleaseById(release.getId(), user);
        Iterator<ExternalToolProcess> oldFossologyProcessIterator = SW360Utils
                .getNotOutdatedExternalToolProcessesForTool(release, ExternalTool.FOSSOLOGY).iterator();
        if (oldFossologyProcessIterator.hasNext()) {
            // we might be getting called in an unvalidated situation, so it might be
            // possible that there is no process yet.
            release.getExternalToolProcesses().remove(oldFossologyProcessIterator.next());
        }
        release.addToExternalToolProcesses(fossologyProcess);
        release.setClearingState(calculateCurrentClearingState(release, fossologyProcess));
        componentClient.updateReleaseFossology(release, user);
    }

    private ClearingState calculateCurrentClearingState(Release release, ExternalToolProcess fossologyProcess) {
        if (ClearingState.APPROVED.equals(release.getClearingState())) {
            // if the fossology process was not used for clearing, but a manual clearing
            // report was attached and approved, then we do not want to mess with the
            // clearing state!
            return ClearingState.APPROVED;
        }

        if (ExternalToolProcessStatus.OUTDATED.equals(fossologyProcess.getProcessStatus())) {
            // if the process is not valid any more, reset clearing state
            return ClearingState.NEW_CLEARING;
        }

        ClearingState result = ClearingState.NEW_CLEARING;
        ExternalToolProcessStep furthestStep = fossologyProcess.getProcessSteps()
                .get(fossologyProcess.getProcessSteps().size() - 1);

        if (FossologyUtils.FOSSOLOGY_STEP_NAME_UPLOAD.equals(furthestStep.getStepName())) {
            switch (furthestStep.getStepStatus()) {
            case IN_WORK:
            case DONE:
                result = ClearingState.SENT_TO_CLEARING_TOOL;
                break;
            case NEW:
            default:
                result = ClearingState.NEW_CLEARING;
            }
        } else if (FossologyUtils.FOSSOLOGY_STEP_NAME_SCAN.equals(furthestStep.getStepName())) {
            switch (furthestStep.getStepStatus()) {
            case IN_WORK:
            case DONE:
                result = ClearingState.SENT_TO_CLEARING_TOOL;
                break;
            case NEW:
            default:
                result = ClearingState.SENT_TO_CLEARING_TOOL;
            }
        } else if (FossologyUtils.FOSSOLOGY_STEP_NAME_REPORT.equals(furthestStep.getStepName())) {
            switch (furthestStep.getStepStatus()) {
            case DONE:
                result = ClearingState.REPORT_AVAILABLE;
                break;
            case IN_WORK:
            case NEW:
            default:
                result = ClearingState.UNDER_CLEARING;
            }
        }

        return result;
    }

    /**
     * Currently there is only one single fossology process allowed (since there can
     * only be one single source for a release that can be scanned), which is
     * checked by this method.
     */
    private boolean isIllegalStateFossologyProcesses(String releaseId, Set<ExternalToolProcess> fossologyProcess) {
        if (fossologyProcess.size() > 1) {
            log.error(
                    "There are more than 1 fossology processes at release with id {}, but only 1 is currently allowed.",
                    releaseId);
            return true;
        }

        return false;
    }

    /**
     * There can be only one single source attachment for a release, which is
     * checked by this method.
     */
    private boolean isIllegalStateSourceAttachments(String releaseId, Set<Attachment> sourceAttachments) {
        if (sourceAttachments.size() != 1) {
            log.error("There has to be exactly one source attachment at release with id {}, but there are {}.",
                    releaseId, sourceAttachments.size());
            return true;
        }

        return false;
    }

    /**
     * The current source attachment has to match the one used for the fossology
     * process up to now in contentid and hash, which is checked by this method.
     */
    private boolean isIllegalStateProcessAndAttachments(Set<ExternalToolProcess> fossologyProcesses,
            Attachment sourceAttachment) {
        return fossologyProcesses.stream().anyMatch(fp -> {
            return !Objects.equals(sourceAttachment.getAttachmentContentId(), fp.getAttachmentId())
                    || !Objects.equals(sourceAttachment.getSha1(), fp.getAttachmentHash());
        });
    }

    private ExternalToolProcess createFossologyProcess(Release release, User user, String attachmentId,
            String attachmentHash) {
        ExternalToolProcessStep etps = createFossologyProcessStep(user, FossologyUtils.FOSSOLOGY_STEP_NAME_UPLOAD);

        ExternalToolProcess etp = new ExternalToolProcess();
        etp.setExternalTool(ExternalTool.FOSSOLOGY);
        etp.setProcessStatus(ExternalToolProcessStatus.NEW);
        etp.setAttachmentId(attachmentId);
        etp.setAttachmentHash(attachmentHash);
        etp.addToProcessSteps(etps);

        release.addToExternalToolProcesses(etp);

        return etp;
    }

    private ExternalToolProcessStep createFossologyProcessStep(User user, String stepName) {
        ExternalToolProcessStep etps = new ExternalToolProcessStep();

        etps.setStepName(stepName);
        etps.setStartedOn(Instant.now().toString());
        etps.setStartedBy(user.getEmail());
        etps.setStartedByGroup(user.getDepartment());
        etps.setStepStatus(ExternalToolProcessStatus.NEW);

        return etps;
    }

    private String attachReportToRelease(Iface componentClient, Release release, User user, InputStream reportStream)
            throws TException {
        AttachmentService.Iface attachmentClient = thriftClients.makeAttachmentClient();

        // first create the content metadata object and save it to get an id from couch
        AttachmentContent attachmentContent = new AttachmentContent(createReportAttachmentName(release));
        attachmentContent.setContentType("text");
        attachmentContent = attachmentClient.makeAttachmentContent(attachmentContent);

        // then upload the real attachment content as _attachment to the metadata object
        attachmentConnector.uploadAttachment(attachmentContent, reportStream);

        // finally reference the attachment metadata object in a new attachment object
        // that is added to the release
        Attachment attachment = CommonUtils.getNewAttachment(user, attachmentContent.getId(),
                attachmentContent.getFilename());
        attachment.setAttachmentType(AttachmentType.INITIAL_SCAN_REPORT);
        attachment.setSha1(attachmentConnector.getSha1FromAttachmentContentId(attachmentContent.getId()));

        // get release again because it has been updated in the meantime so version
        // changed and update might otherwise result in update conflict
        release = componentClient.getReleaseById(release.getId(), user);
        release.addToAttachments(attachment);
        componentClient.updateRelease(release, user);

        return attachmentContent.getId();
    }

    private String createReportAttachmentName(Release release) {
        LocalDateTime now = LocalDateTime.now();
        return release.getName() + "-" + release.getVersion() + "-" + dateTimeFormatter.format(now) + "-SPDX.rdf";
    }

    @Override
    public RequestStatus triggerReportGenerationFossology(String releaseId, User user) throws TException {
        Iface componentClient = thriftClients.makeComponentClient();
        Release release = componentClient.getReleaseById(releaseId, user);
        Set<ExternalToolProcess> fossologyProcesses = SW360Utils.getNotOutdatedExternalToolProcessesForTool(release,
                ExternalTool.FOSSOLOGY);
        if (isIllegalStateFossologyProcesses(releaseId, fossologyProcesses)) {
            return RequestStatus.FAILURE;
        }
        if (fossologyProcesses.size() == 0) {
            log.info("No FOSSology process found for release with id {}.", releaseId);
            return RequestStatus.FAILURE;
        } else if (fossologyProcesses.size() == 1) {
            ExternalToolProcess extToolProcess = fossologyProcesses.iterator().next();
            if (extToolProcess.getProcessSteps().size() > 2) {
                extToolProcess.getProcessSteps().get(extToolProcess.getProcessSteps().size() - 1)
                        .setStepStatus(ExternalToolProcessStatus.NEW);
            } else if (extToolProcess.getProcessSteps().size() == 2 && SW360Utils.readConfig(DISABLE_CLEARING_FOSSOLOGY_REPORT_DOWNLOAD, false)) {
                extToolProcess.addToProcessSteps(createFossologyProcessStep(user, FossologyUtils.FOSSOLOGY_STEP_NAME_REPORT));
                reportStep = true;
            } else {
                log.info("Either the source of release with id {} is not yet uploaded or not yet scanned", releaseId);
                return RequestStatus.FAILURE;
            }
            handleReportStepV2(componentClient, release, user, extToolProcess);
            updateFossologyProcessInRelease(extToolProcess, release, user, componentClient);
            return RequestStatus.SUCCESS;
        }
        return RequestStatus.SUCCESS;
    }

    @Override
    public Map<String, String> checkUnpackStatus(int uploadId) throws TException {
        return fossologyRestClient.checkUnpackStatus(uploadId);
    }

    @Override
    public Map<String, String> checkScanStatus(int scanJobId) throws TException {
        return fossologyRestClient.checkScanStatus(scanJobId);
    }

    /**
     * Parse scan status code for v2 API
     */
    private int scanStatusCodeV2(Map<String, String> responseMap) {
        if (responseMap == null || responseMap.isEmpty())
            return -1;

        String status = responseMap.get("status");
        if (status == null) return -1;

        return switch (status) {
            case V2_STATUS_COMPLETED -> 1;
            case V2_STATUS_QUEUED, V2_STATUS_PROCESSING -> 0;
            default -> -1;
        };
    }
}

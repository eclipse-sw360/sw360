/*
 * Copyright Siemens AG, 2013-2015, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.fossology;

import org.eclipse.sw360.common.utils.BackendUtils;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.FossologyUtils;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.thrift.ConfigContainer;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.attachments.*;
import org.eclipse.sw360.datahandler.thrift.components.*;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService.Iface;
import org.eclipse.sw360.datahandler.thrift.fossology.FossologyService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.fossology.config.FossologyRestConfig;
import org.eclipse.sw360.fossology.rest.FossologyRestClient;

import org.apache.commons.lang3.StringUtils;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Implementation of the Thrift service. Offers a very simple interface where
 * clients can only trigger the {@link #process(String, User)} method for a
 * releaseId. The current state will be determined by checking the
 * {@link ExternalToolProcess} for {@link ExternalTool#FOSSOLOGY} and the next
 * step will be invoked.
 */
@Component
public class FossologyHandler implements FossologyService.Iface {

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final ThriftClients thriftClients;
    private final FossologyRestConfig fossologyRestConfig;
    private final FossologyRestClient fossologyRestClient;
    private final AttachmentConnector attachmentConnector;

    private static final String SCAN_RESPONSE_STATUS_VALUE_QUEUED = "Queued";
    private static final String SCAN_RESPONSE_STATUS_VALUE_PROCESSING = "Processing";
    private static final String SCAN_RESPONSE_STATUS_VALUE_COMPLETED = "Completed";
    private static final String SCAN_RESPONSE_STATUS_VALUE_FAILED = "Failed";
    boolean reportStep = false;

    @Autowired
    public FossologyHandler(ThriftClients thriftClients, FossologyRestConfig fossologyRestConfig,
            FossologyRestClient fossologyRestClient, AttachmentConnector attachmentConnector) {
        this.thriftClients = thriftClients;
        this.fossologyRestConfig = fossologyRestConfig;
        this.fossologyRestClient = fossologyRestClient;
        this.attachmentConnector = attachmentConnector;
    }

    @Override
    public RequestStatus setFossologyConfig(ConfigContainer newConfig) throws TException {
        return fossologyRestConfig.update(newConfig).getConfigKeyToValues().equals(newConfig.getConfigKeyToValues())
                ? RequestStatus.SUCCESS
                : RequestStatus.FAILURE;
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

        ExternalToolProcessStep furthestStep = fossologyProcess.getProcessSteps().get(fossologyProcess.getProcessSteps().size() - 1);
        if (FossologyUtils.FOSSOLOGY_STEP_NAME_UPLOAD.equals(furthestStep.getStepName())) {
            handleUploadStep(componentClient, release, user, fossologyProcess, sourceAttachment, uploadDescription);
        } else if (FossologyUtils.FOSSOLOGY_STEP_NAME_SCAN.equals(furthestStep.getStepName())) {
            handleScanStep(componentClient, release, user, fossologyProcess);
        } else if(!BackendUtils.DISABLE_CLEARING_FOSSOLOGY_REPORT_DOWNLOAD && FossologyUtils.FOSSOLOGY_STEP_NAME_REPORT.equals(furthestStep.getStepName())) {
            handleReportStep(componentClient, release, user, fossologyProcess);
        } else if(reportStep && FossologyUtils.FOSSOLOGY_STEP_NAME_REPORT.equals(furthestStep.getStepName())) {
            handleReportStep(componentClient, release, user, fossologyProcess);
        }

        updateFossologyProcessInRelease(fossologyProcess, release, user, componentClient);

        return fossologyProcess;
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

    private void handleUploadStep(Iface componentClient, Release release, User user,
            ExternalToolProcess fossologyProcess, Attachment sourceAttachment, String uploadDescription) throws TException {
        ExternalToolProcessStep furthestStep = fossologyProcess.getProcessSteps()
                .get(fossologyProcess.getProcessSteps().size() - 1);
        switch (furthestStep.getStepStatus()) {
        case NEW:
            // upload, set new state immediately to prevent other threads from doing the
            // same
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
            int lastUploadId = fossologyRestClient.getUploadId(shaValue, attachmentFilename);
            if (lastUploadId > -1) {
                furthestStep.setFinishedOn(Instant.now().toString());
                furthestStep.setStepStatus(ExternalToolProcessStatus.DONE);
                furthestStep.setProcessStepIdInTool(lastUploadId + "");
                furthestStep.setResult(lastUploadId + "");
            } else {
                InputStream attachmentStream = attachmentConnector.getAttachmentStream(attachmentContent, user, release);
                int uploadId = fossologyRestClient.uploadFile(attachmentFilename, attachmentStream, uploadDescription);
                if (uploadId > -1) {
                    furthestStep.setFinishedOn(Instant.now().toString());
                    furthestStep.setStepStatus(ExternalToolProcessStatus.DONE);
                    furthestStep.setProcessStepIdInTool(uploadId + "");
                    furthestStep.setResult(uploadId + "");
                } else {
                    furthestStep.setStepStatus(ExternalToolProcessStatus.NEW);
                    furthestStep.setResult(uploadId + "");
                }
            }
            break;
        case DONE:
            // start scan
            fossologyProcess.addToProcessSteps(createFossologyProcessStep(user, FossologyUtils.FOSSOLOGY_STEP_NAME_SCAN));
            handleScanStep(componentClient, release, user, fossologyProcess);
            break;
        case IN_WORK:
            // do nothing, upload should happen in another thread
        default:
            // do nothing, unknown status
        }
    }

    private void handleScanStep(Iface componentClient, Release release, User user,
            ExternalToolProcess fossologyProcess) throws TException {
        ExternalToolProcessStep furthestStep = fossologyProcess.getProcessSteps()
                .get(fossologyProcess.getProcessSteps().size() - 1);
        String uploadId = SW360Utils.getExternalToolProcessStepOfFirstProcessForTool(release, ExternalTool.FOSSOLOGY,
                FossologyUtils.FOSSOLOGY_STEP_NAME_UPLOAD).getResult();

        switch (furthestStep.getStepStatus()) {
        case NEW:
            // scan, set new state immediately to prevent other threads from doing the
            // same
            furthestStep.setStepStatus(ExternalToolProcessStatus.IN_WORK);
            updateFossologyProcessInRelease(fossologyProcess, release, user, componentClient);

            int jobId = fossologyRestClient.startScanning(Integer.valueOf(uploadId));
            if (jobId > -1) {
                furthestStep.setProcessStepIdInTool(jobId + "");
                furthestStep.setResult(null);
            } else {
                furthestStep.setStepStatus(ExternalToolProcessStatus.NEW);
                furthestStep.setResult(jobId + "");
            }
            break;
        case IN_WORK:
            // query state
            int scanningJobId = Integer.valueOf(furthestStep.getProcessStepIdInTool());
            int status = scanStatusCode(fossologyRestClient.checkScanStatus(scanningJobId));
            if (status > 0 || status == -1) {
                furthestStep.setFinishedOn(Instant.now().toString());
                furthestStep.setStepStatus(ExternalToolProcessStatus.DONE);
                furthestStep.setResult(status + "");
            } else {
                // empty on purpose: do nothing, just leave status in_work and wait for next
                // query
            }
            break;
        case DONE:
            // start report
            if(!BackendUtils.DISABLE_CLEARING_FOSSOLOGY_REPORT_DOWNLOAD) {
                fossologyProcess.addToProcessSteps(createFossologyProcessStep(user, FossologyUtils.FOSSOLOGY_STEP_NAME_REPORT));
                handleReportStep(componentClient, release, user, fossologyProcess);
            }
            break;
        default:
            // do nothing, unknown status
        }
    }

    private void handleReportStep(Iface componentClient, Release release, User user,
            ExternalToolProcess fossologyProcess) throws TException {
        ExternalToolProcessStep furthestStep = fossologyProcess.getProcessSteps()
                .get(fossologyProcess.getProcessSteps().size() - 1);

        switch (furthestStep.getStepStatus()) {
        case NEW:
            // generate report, set new state immediately to prevent other threads from
            // doing the same
            furthestStep.setStepStatus(ExternalToolProcessStatus.IN_WORK);
            updateFossologyProcessInRelease(fossologyProcess, release, user, componentClient);

            String uploadId = SW360Utils.getExternalToolProcessStepOfFirstProcessForTool(release,
                    ExternalTool.FOSSOLOGY, FossologyUtils.FOSSOLOGY_STEP_NAME_UPLOAD).getResult();
            int reportId = fossologyRestClient.startReport(Integer.valueOf(uploadId));
            if (reportId > -1) {
                furthestStep.setProcessStepIdInTool(reportId + "");
            } else {
                furthestStep.setStepStatus(ExternalToolProcessStatus.NEW);
            }
            break;
        case IN_WORK:
            // try to download report - since download might take a bit longer, we first set
            // the state to done so that no one else downloads the same report. if download
            // fails, we reset the state

            furthestStep.setStepStatus(ExternalToolProcessStatus.DONE);
            updateFossologyProcessInRelease(fossologyProcess, release, user, componentClient);

            int reportJobId = Integer.valueOf(furthestStep.getProcessStepIdInTool());
            InputStream reportStream = fossologyRestClient.getReport(Integer.valueOf(reportJobId));
            if (reportStream != null) {
                String attachmentContentId = attachReportToRelease(componentClient, release, user, reportStream);
                furthestStep.setFinishedOn(Instant.now().toString());
                furthestStep.setResult(attachmentContentId);
                fossologyProcess.setProcessStatus(ExternalToolProcessStatus.DONE);
            } else {
                furthestStep.setStepStatus(ExternalToolProcessStatus.IN_WORK);
            }
            break;
        case DONE:
            // do nothing, last step is already done
            break;
        default:
            // do nothing, unknown status
        }
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
            } else if (extToolProcess.getProcessSteps().size() == 2 && BackendUtils.DISABLE_CLEARING_FOSSOLOGY_REPORT_DOWNLOAD) {
                extToolProcess.addToProcessSteps(createFossologyProcessStep(user, FossologyUtils.FOSSOLOGY_STEP_NAME_REPORT));
                reportStep = true;
            } else {
                log.info("Either the source of release with id {} is not yet uploaded or not yet scanned", releaseId);
                return RequestStatus.FAILURE;
            }
            handleReportStep(componentClient, release, user, extToolProcess);
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
     * Checks the status of a report generation process.
     *
     * @param reportId the id of the report to check status for
     * @return the Map object containing status details
     */
    @Override
    public Map<String, String> checkReportGenerationStatus(int reportId) throws TException {
        return fossologyRestClient.checkReportGenerationStatus(reportId);
    }

    private int scanStatusCode(Map<String, String> responseMap) {
        if (responseMap == null || responseMap.isEmpty())
            return -1;

        String status = responseMap.get("status");
        switch (status) {
        case SCAN_RESPONSE_STATUS_VALUE_COMPLETED:
            return 1;
        case SCAN_RESPONSE_STATUS_VALUE_QUEUED:
        case SCAN_RESPONSE_STATUS_VALUE_PROCESSING:
            return 0;
        case SCAN_RESPONSE_STATUS_VALUE_FAILED:
        default:
            return -1;
        }
    }
}

/*
 * Copyright TOSHIBA CORPORATION, 2022. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2022. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db.spdx.document;

import com.ibm.cloud.cloudant.v1.Cloudant;

import org.apache.logging.log4j.core.util.UuidUtil;
import org.apache.thrift.TException;
import org.cyclonedx.parsers.JsonParser;
import org.cyclonedx.parsers.XmlParser;
import org.cyclonedx.model.Bom;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.DocumentCreationInformation;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageInformation;
import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestStatus;
import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.services.common.DocumentState;
import org.eclipse.sw360.datahandler.services.common.FieldAccessor;
import org.eclipse.sw360.datahandler.services.common.PojoFields;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.users.User;
import org.eclipse.sw360.datahandler.services.users.RequestedAction;
import org.eclipse.sw360.datahandler.services.spdx.Annotations;
import org.eclipse.sw360.datahandler.services.spdx.OtherLicensingInformationDetected;
import org.eclipse.sw360.datahandler.services.spdx.RelationshipsBetweenSPDXElements;
import org.eclipse.sw360.datahandler.services.spdx.SnippetInformation;
import org.eclipse.sw360.datahandler.services.spdx.SPDXDocument;
import org.eclipse.sw360.datahandler.services.components.Release;
import org.eclipse.sw360.common.utils.converter.components.ReleaseConverter;
import org.eclipse.sw360.datahandler.services.changelogs.*;
import org.eclipse.sw360.datahandler.db.ReleaseRepository;
import org.eclipse.sw360.datahandler.db.VendorRepository;
import org.eclipse.sw360.datahandler.db.DatabaseHandlerUtil;
import org.eclipse.sw360.datahandler.db.spdx.documentcreationinfo.SpdxDocumentCreationInfoDatabaseHandler;
import org.eclipse.sw360.datahandler.db.spdx.packageinfo.SpdxPackageInfoDatabaseHandler;
import org.eclipse.sw360.datahandler.entitlement.SpdxDocumentModerator;
import org.eclipse.sw360.common.utils.converter.spdx.DocumentCreationInformationConverter;
import org.eclipse.sw360.common.utils.converter.spdx.PackageInformationConverter;
import org.eclipse.sw360.common.utils.converter.spdx.SPDXDocumentConverter;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.util.*;

import com.google.common.collect.Lists;
import org.spdx.library.model.v2.SpdxDocument;
import org.spdx.tools.SpdxToolsHelper;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertIdUnset;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotEmpty;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotNull;
import static org.eclipse.sw360.datahandler.common.CommonUtils.*;
import static org.eclipse.sw360.datahandler.common.SW360Constants.TYPE_SPDX_DOCUMENT;
import static org.eclipse.sw360.datahandler.common.SW360Constants.JSON_FILE_EXTENSION;
import static org.eclipse.sw360.datahandler.common.SW360Constants.RDF_FILE_EXTENSION;
import static org.eclipse.sw360.datahandler.common.SW360Constants.SPDX_FILE_EXTENSION;
import static org.eclipse.sw360.datahandler.common.SW360Constants.XML_FILE_EXTENSION;
import static org.eclipse.sw360.datahandler.permissions.PermissionUtils.makePermission;

public class SpdxDocumentDatabaseHandler {

    private static final Logger log = LogManager.getLogger(SpdxDocumentDatabaseHandler.class);

    /**
     * Connection to the couchDB database
     */
    private final DatabaseConnectorCloudant db;
    private final DatabaseConnectorCloudant sw360db;
    private final DatabaseConnectorCloudant dbChangeLogs;

    private final SpdxDocumentRepository SPDXDocumentRepository;
    private final ReleaseRepository releaseRepository;
    private final VendorRepository vendorRepository;
    private DatabaseHandlerUtil dbHandlerUtil;
    private final SpdxDocumentModerator moderator;

    private final SpdxDocumentCreationInfoDatabaseHandler creationInfoDatabaseHandler;
    private final SpdxPackageInfoDatabaseHandler packageInfoDatabaseHandler;

    public SpdxDocumentDatabaseHandler(Cloudant client, String dbName) throws MalformedURLException {
        db = new DatabaseConnectorCloudant(client, dbName);

        // Create the repositories
        SPDXDocumentRepository = new SpdxDocumentRepository(db);

        sw360db = new DatabaseConnectorCloudant(client, DatabaseSettings.COUCH_DB_DATABASE);
        vendorRepository = new VendorRepository(sw360db);
        releaseRepository = new ReleaseRepository(sw360db, vendorRepository);
        // Create the moderator
        moderator = new SpdxDocumentModerator();
        // Create the changelogs
        dbChangeLogs = new DatabaseConnectorCloudant(client, DatabaseSettings.COUCH_DB_CHANGE_LOGS);
        this.dbHandlerUtil = new DatabaseHandlerUtil(dbChangeLogs);
        this.creationInfoDatabaseHandler = new SpdxDocumentCreationInfoDatabaseHandler(client, dbName);
        this.packageInfoDatabaseHandler = new SpdxPackageInfoDatabaseHandler(client, dbName);
    }

    public List<SPDXDocument> getSPDXDocumentSummary(User user) {
        List<SPDXDocument> spdxs = SPDXDocumentRepository.getSPDXDocumentSummary();
        return spdxs;
    }

    public SPDXDocument getSPDXDocumentById(String id, User user) throws SW360Exception {
        SPDXDocument spdx = SPDXDocumentRepository.get(id);
        assertNotNull(spdx, "Could not find SPDX Document by id: " + id);
        // Set permissions
        if (user != null) {
            makePermission(spdx, user).fillPermissions();
        }
        return spdx;
    }

    public SPDXDocument getSPDXDocumentForEdit(String id, User user) throws SW360Exception {
        List<org.eclipse.sw360.datahandler.services.moderation.ModerationRequest> moderationRequestsForDocumentId =
                moderator.getModerationRequestsForDocumentId(id);

        SPDXDocument spdx = getSPDXDocumentById(id, user);
        DocumentState documentState;

        if (moderationRequestsForDocumentId.isEmpty()) {
            documentState = new DocumentState().setOriginalDocument(true);
        } else {
            final String email = user.getEmail();
            Optional<org.eclipse.sw360.datahandler.services.moderation.ModerationRequest> moderationRequestOptional =
                    CommonUtils.getFirstModerationRequestOfUserPojo(moderationRequestsForDocumentId, email);
            if (moderationRequestOptional.isPresent()
                    && isInProgressOrPending(moderationRequestOptional.get())) {
                org.eclipse.sw360.datahandler.services.moderation.ModerationRequest moderationRequest =
                        moderationRequestOptional.get();
                spdx = moderator.updateSPDXDocumentFromModerationRequest(
                        spdx,
                        moderationRequest.getSpdxDocumentAdditions(),
                        moderationRequest.getSpdxDocumentDeletions());
                documentState = moderatedState(moderationRequestOptional.get());
            } else {
                documentState = new DocumentState().setOriginalDocument(true)
                        .setModerationState(moderationRequestsForDocumentId.get(0).getModerationState());
            }
        }
        makePermission(spdx, user).fillPermissions();
        spdx.setDocumentState(documentState);
        return spdx;
    }

    public AddDocumentRequestSummary addSPDXDocument(SPDXDocument spdx, User user) throws SW360Exception {
        AddDocumentRequestSummary requestSummary= new AddDocumentRequestSummary();
        assertNotNull(spdx, "SPDXDocument object cannot be null");
        assertIdUnset(spdx.getId());
        spdx.setType(TYPE_SPDX_DOCUMENT);
        spdx.setPermissions(null);
        String releaseId = spdx.getReleaseId();
        Release release = releaseRepository.get(releaseId);
        assertNotNull(release, "Could not find Release to add SPDX Document!");
        if (isNotNullEmptyOrWhitespace(release.getSpdxId())){
            log.error("SPDX Document id is not found release!");
            return requestSummary.setRequestStatus(AddDocumentRequestStatus.DUPLICATE)
                            .setId(release.getSpdxId());
        }
        spdx.setCreatedBy(user.getEmail());
        SPDXDocumentRepository.add(spdx);
        String spdxId = spdx.getId();
        release.setSpdxId(spdxId);
        releaseRepository.update(release);
        dbHandlerUtil.addChangeLogs(SPDXDocumentConverter.toThrift(spdx), null, user.getEmail(), Operation.CREATE, null, Lists.newArrayList(), null, null);
        return requestSummary.setRequestStatus(AddDocumentRequestStatus.SUCCESS)
                            .setId(spdx.getId());
    }

    public RequestStatus updateSPDXDocument(SPDXDocument spdx, User user) throws SW360Exception {
        prepareSpdxDocument(spdx);
        SPDXDocument actual = SPDXDocumentRepository.get(spdx.getId());
        assertNotNull(actual, "Could not find SPDX Document to update!");
        if (sizeOf(spdx.getSnippets()) < sizeOf(actual.getSnippets())) {
            updateSnippets(spdx);
        }
        if (sizeOf(spdx.getAnnotations()) < sizeOf(actual.getAnnotations())) {
            updateAnnotations(spdx);
        }
        if (sizeOf(spdx.getRelationships()) < sizeOf(actual.getRelationships())) {
            updateRelationships(spdx);
        }
        if (sizeOf(spdx.getOtherLicensingInformationDetecteds()) < sizeOf(actual.getOtherLicensingInformationDetecteds())) {
            updateOtherLicensingInformationDetecteds(spdx);
        }

        if (!makePermission(spdx, user).isActionAllowed(RequestedAction.WRITE)) {
            if (isChanged(actual, spdx)) {
                return moderator.updateSPDXDocument(spdx, user);
            } else {
                return RequestStatus.SUCCESS;
            }
        }
        SPDXDocumentRepository.update(spdx);
        dbHandlerUtil.addChangeLogs(SPDXDocumentConverter.toThrift(spdx), SPDXDocumentConverter.toThrift(actual), user.getEmail(), Operation.UPDATE, null, Lists.newArrayList(), null, null);
        return RequestStatus.SUCCESS;
    }

    public void updateOtherLicensingInformationDetecteds(SPDXDocument request) {
        if (request.getOtherLicensingInformationDetecteds() == null) {
            return;
        }
        List<OtherLicensingInformationDetected> otherLicensingInformationDetecteds = new ArrayList<>(request.getOtherLicensingInformationDetecteds());
        otherLicensingInformationDetecteds.sort(Comparator.comparing(item -> item.getIndex() == null ? 0 : item.getIndex()));
        for (int i = 0; i < otherLicensingInformationDetecteds.size() ; i++) {
            otherLicensingInformationDetecteds.get(i).setIndex(i);
        }
        request.setOtherLicensingInformationDetecteds(new HashSet<>(otherLicensingInformationDetecteds));
    }

    public void updateRelationships(SPDXDocument request) {
        if (request.getRelationships() == null) {
            return;
        }
        List<RelationshipsBetweenSPDXElements> relationshipsBetweenSPDXElements = new ArrayList<>(request.getRelationships());
        relationshipsBetweenSPDXElements.sort(Comparator.comparing(item -> item.getIndex() == null ? 0 : item.getIndex()));
        for (int i = 0; i<relationshipsBetweenSPDXElements.size() ; i++) {
            relationshipsBetweenSPDXElements.get(i).setIndex(i);
        }
        request.setRelationships(new HashSet<>(relationshipsBetweenSPDXElements));
    }

    public void updateAnnotations(SPDXDocument request) {
        if (request.getAnnotations() == null) {
            return;
        }
        List<Annotations> annotations = new ArrayList<>(request.getAnnotations());
        annotations.sort(Comparator.comparing(item -> item.getIndex() == null ? 0 : item.getIndex()));
        for (int i = 0; i<annotations.size() ; i++) {
            annotations.get(i).setIndex(i);
        }
        request.setAnnotations(new HashSet<>(annotations));
    }

    public void updateSnippets(SPDXDocument request) {
        if (request.getSnippets() == null) {
            return;
        }
        List<SnippetInformation> snippetInformations = new ArrayList<>(request.getSnippets());
        snippetInformations.sort(Comparator.comparing(item -> item.getIndex() == null ? 0 : item.getIndex()));
        for (int i = 0; i<snippetInformations.size() ; i++) {
            snippetInformations.get(i).setIndex(i);
        }
        request.setSnippets(new HashSet<>(snippetInformations));
    }

    public RequestStatus updateSPDXDocumentFromModerationRequest(SPDXDocument spdxAdditions, SPDXDocument spdxDeletions, User user) throws SW360Exception {
        try {
            SPDXDocument spdx = getSPDXDocumentById(spdxAdditions.getId(), user);
            spdx = moderator.updateSPDXDocumentFromModerationRequest(spdx, spdxAdditions, spdxDeletions);
            return updateSPDXDocument(spdx, user);
        } catch (SW360Exception e) {
            log.error("Could not get original SPDX Document when updating from moderation request.");
            return RequestStatus.FAILURE;
        }
    }

    public RequestStatus deleteSPDXDocument(String id, User user) throws SW360Exception {
        SPDXDocument spdx = SPDXDocumentRepository.get(id);
        assertNotNull(spdx, "Could not find SPDX Document to delete!");
        if (!makePermission(spdx, user).isActionAllowed(RequestedAction.WRITE)) {
            return moderator.deleteSPDXDocument(spdx, user);
        }
        Set<String> packageInfoIds = spdx.getSpdxPackageInfoIds();
        if (packageInfoIds != null) {
            for (String packageInfoId : packageInfoIds) {
                packageInfoDatabaseHandler.deletePackageInformation(packageInfoId, user);
            }
        }

        Set<String> fileInfoIds = spdx.getSpdxFileInfoIds();
        if (fileInfoIds != null) {
            return RequestStatus.IN_USE;
        }

        String documentCreationId = spdx.getSpdxDocumentCreationInfoId();
        if (documentCreationId != null) {
            creationInfoDatabaseHandler.deleteDocumentCreationInformation(documentCreationId, user);
        }

        spdx.setSpdxPackageInfoIds(null);
        spdx.setSpdxDocumentCreationInfoId(null);

        SPDXDocumentRepository.remove(id);
        String releaseId = spdx.getReleaseId();
        if (isNotNullEmptyOrWhitespace(releaseId)) {
            Release release = releaseRepository.get(releaseId);
            assertNotNull(release, "Could not remove SPDX Document ID in Release!");
            Release oldRelease = ReleaseConverter.fromThrift(ReleaseConverter.toThrift(release));
            release.setSpdxId(null);
            releaseRepository.update(release);
            dbHandlerUtil.addChangeLogs(ReleaseConverter.toThrift(release), ReleaseConverter.toThrift(oldRelease), user.getEmail(), Operation.UPDATE, null, Lists.newArrayList(), spdx.getId(), Operation.SPDX_DOCUMENT_DELETE);
        }
        return RequestStatus.SUCCESS;
    }

    private boolean isChanged(SPDXDocument actual, SPDXDocument update) {
        for (FieldAccessor<SPDXDocument> field : PojoFields.of(SPDXDocument.class)) {
            Object actualValue = field.get(actual);
            Object updateValue = field.get(update);
            if (actualValue == null && updateValue == null) {
                return false;
            } else if (updateValue != null && actualValue == null) {
                return true;
            } else if (updateValue == null && actualValue != null) {
                return true;
            } else if (!actualValue.equals(updateValue)) {
                return true;
            }
        }
        return false;
    }

    private static void prepareSpdxDocument(SPDXDocument spdx) throws SW360Exception {
        assertNotNull(spdx, "SPDXDocument object cannot be null");
        assertNotEmpty(spdx.getId());
        assertNotEmpty(spdx.getSpdxDocumentCreationInfoId());
        spdx.setType(TYPE_SPDX_DOCUMENT);
        spdx.setPermissions(null);
    }

    private static DocumentState moderatedState(
            org.eclipse.sw360.datahandler.services.moderation.ModerationRequest moderationRequest) {
        return new DocumentState().setOriginalDocument(false)
                .setModerationState(moderationRequest.getModerationState());
    }

    private static int sizeOf(Collection<?> values) {
        return values == null ? 0 : values.size();
    }

    public static void updateSPDX(User user, Release release) throws TException {
        String spdxDocumentId = "";
        String releaseId = release.getId();
        Set<String> moderators;
        if (CommonUtils.isNullOrEmptyCollection(release.getModerators())) {
            moderators = new HashSet<>();
        } else {
            moderators = release.getModerators();
        }

        SpdxDocumentDatabaseHandler spdxHandler;
        SpdxDocumentCreationInfoDatabaseHandler documentHandler;
        SpdxPackageInfoDatabaseHandler packageHandler;
        try {
            spdxHandler = new SpdxDocumentDatabaseHandler(
                    DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_SPDX);
            documentHandler = new SpdxDocumentCreationInfoDatabaseHandler(
                    DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_SPDX);
            packageHandler = new SpdxPackageInfoDatabaseHandler(
                    DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_SPDX);
        } catch (MalformedURLException e) {
            throw new TException(e);
        }

        // Add SPDXDocument
        SPDXDocument spdx = SPDXDocumentConverter.fromThrift(SW360Utils.generateSpdxDocument());
        spdx.setModerators(moderators);
        if (isNullOrEmpty(spdx.getReleaseId()) && !isNullOrEmpty(releaseId)) {
            spdx.setReleaseId(releaseId);
        }
        if (isNullOrEmpty(spdx.getId())) {
            spdx.setId(null);
            spdx.setRevision(null);
            spdxDocumentId = spdxHandler.addSPDXDocument(spdx, user).getId();
        } else {
            spdxHandler.updateSPDXDocument(spdx, user);
            spdxDocumentId = spdx.getId();
        }

        // Add DocumentCreationInformation
        DocumentCreationInformation document = SW360Utils.generateDocumentCreationInformation();
        document.setModerators(moderators);
        if (isNullOrEmpty(document.getSpdxDocumentId())) {
            document.setSpdxDocumentId(spdxDocumentId);
        }
        if (isNullOrEmpty(document.getId())) {
            document.unsetId();
            document.unsetRevision();
            documentHandler.addDocumentCreationInformation(
                    DocumentCreationInformationConverter.fromThrift(document), user);
        } else {
            documentHandler.updateDocumentCreationInformation(
                    DocumentCreationInformationConverter.fromThrift(document), user);
        }

        // Add PackageInformation
        PackageInformation packageInfo = SW360Utils.generatePackageInformation();
        packageInfo.setModerators(moderators);
        if (isNullOrEmpty(packageInfo.getSpdxDocumentId())) {
            packageInfo.setSpdxDocumentId(spdxDocumentId);
        }
        if (isNullOrEmpty(packageInfo.getId())) {
            packageInfo.unsetId();
            packageInfo.unsetRevision();
            packageHandler.addPackageInformation(PackageInformationConverter.fromThrift(packageInfo), user);
        } else {
            packageHandler.updatePackageInformation(PackageInformationConverter.fromThrift(packageInfo), user);
        }
    }

    public boolean isValidSbomFile(ByteBuffer file, String type, String extension) {
        final File tempFile;
        try {
            tempFile = File.createTempFile(UuidUtil.getTimeBasedUuid().toString(), "." + extension);
            tempFile.deleteOnExit();
            // Set append to false, overwrite if file existed
            try (FileOutputStream outputStream = new FileOutputStream(tempFile, false)) {
                outputStream.write(file.array());
            }
        } catch (IOException e) {
            log.error("Error creating temporary file: {}", e.getMessage());
            return false;
        }

        try {
            // SPDX formats
            if (type.equalsIgnoreCase("SPDX")) {

                if (extension.equalsIgnoreCase(SPDX_FILE_EXTENSION) || extension.equalsIgnoreCase(RDF_FILE_EXTENSION)) {

                    try {
                        SpdxDocument doc = SpdxToolsHelper.deserializeDocumentCompatV2(tempFile);

                        String specVersion = doc.getSpecVersion();
                        if (specVersion == null || specVersion.isEmpty()) {
                            log.debug("SPDX document missing SPDX specification version");
                            return false;
                        }

                        String documentUri = doc.getDocumentUri();
                        if (documentUri == null || documentUri.isEmpty()) {
                            log.debug("SPDX document missing required Document URI/namespace");
                            return false;
                        }

                        return true;

                    } catch (Exception e) {
                        log.debug("Not a valid SPDX file: {}", e.getMessage());
                        return false;
                    }
                }
            }
            // CycloneDX formats
            else if (type.equalsIgnoreCase("CycloneDX")) {

                if (extension.equalsIgnoreCase(XML_FILE_EXTENSION)) {
                    try {
                        XmlParser xmlParser = new XmlParser();
                        Bom bom = xmlParser.parse(tempFile);
                        return bom != null && bom.getMetadata() != null;
                    } catch (Exception e) {
                        log.debug("Not a valid CycloneDX XML file: {}", e.getMessage());
                        return false;
                    }
                } else if (extension.equalsIgnoreCase(JSON_FILE_EXTENSION)) {
                    try {
                        JsonParser jsonParser = new JsonParser();
                        Bom bom = jsonParser.parse(tempFile);
                        return bom != null && bom.getMetadata() != null;
                    } catch (Exception e) {
                        log.debug("Not a valid CycloneDX JSON file: {}", e.getMessage());
                        return false;
                    }
                }
            }
        } finally {
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
        return false;
    }
}

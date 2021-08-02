package org.eclipse.sw360.spdxdocumentcreationinfo.db;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.model.Response;
import com.google.common.collect.*;

import org.eclipse.sw360.common.utils.BackendUtils;
import org.eclipse.sw360.components.summary.SummaryType;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.common.Duration;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.common.ThriftEnumUtils;
import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.couchdb.AttachmentStreamConnector;
import org.eclipse.sw360.datahandler.entitlement.ComponentModerator;
import org.eclipse.sw360.datahandler.entitlement.ProjectModerator;
import org.eclipse.sw360.datahandler.entitlement.ReleaseModerator;
import org.eclipse.sw360.datahandler.permissions.DocumentPermissions;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.changelogs.ChangeLogs;
import org.eclipse.sw360.datahandler.thrift.changelogs.ChangedFields;
import org.eclipse.sw360.datahandler.thrift.changelogs.Operation;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationService;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.ProjectVulnerabilityRating;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.ReleaseVulnerabilityRelation;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityCheckStatus;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityService;
import org.eclipse.sw360.datahandler.thrift.spdxdocument.*;
import org.eclipse.sw360.datahandler.thrift.spdxdocument.SPDXDocument;
import org.eclipse.sw360.datahandler.thrift.spdxdocument.SPDXDocumentService;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.*;
import org.eclipse.sw360.mail.MailConstants;
import org.eclipse.sw360.mail.MailUtil;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.thrift.TException;
import org.eclipse.sw360.spdx.SpdxBOMImporter;
import org.eclipse.sw360.spdx.SpdxBOMImporterSink;
import org.jetbrains.annotations.NotNull;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.*;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static com.google.common.collect.Sets.newHashSet;
import static org.eclipse.sw360.datahandler.common.CommonUtils.*;
import static org.eclipse.sw360.datahandler.common.Duration.durationOf;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotNull;
import static org.eclipse.sw360.datahandler.common.SW360Assert.fail;
import static org.eclipse.sw360.datahandler.permissions.PermissionUtils.makePermission;
import static org.eclipse.sw360.datahandler.thrift.ThriftUtils.copyFields;
import static org.eclipse.sw360.datahandler.thrift.ThriftValidate.ensureEccInformationIsSet;
import static org.eclipse.sw360.datahandler.thrift.ThriftValidate.prepareComponents;
import static org.eclipse.sw360.datahandler.thrift.ThriftValidate.prepareReleases;

import org.eclipse.sw360.spdxdocumentcreationinfo.db.*;
import org.eclipse.sw360.spdxdocument.db.SpdxDocumentRepository;

public class SpdxDocumentCreationInfoDatabaseHandler {

    private static final Logger log = LogManager.getLogger(SpdxDocumentCreationInfoDatabaseHandler.class);

    /**
     * Connection to the couchDB database
     */
    private final DatabaseConnectorCloudant db;

    private final SpdxDocumentCreationInfoRepository SPDXDocumentCreationInfoRepository;
    private final SpdxDocumentRepository SPDXDocumentRepository;

    public SpdxDocumentCreationInfoDatabaseHandler(Supplier<CloudantClient> httpClient, String dbName) throws MalformedURLException {
        db = new DatabaseConnectorCloudant(httpClient, dbName);

        log.info("Create the SPDX Document Creation Information repositories ");
        // Create the repositories
        SPDXDocumentCreationInfoRepository = new SpdxDocumentCreationInfoRepository(db);
        SPDXDocumentRepository = new SpdxDocumentRepository(db);
        // Create the moderator
    }

    public List<DocumentCreationInformation> getDocumentCreationInformationSummary(User user) {
        List<DocumentCreationInformation> documentCreationInfos = SPDXDocumentCreationInfoRepository.getDocumentCreationInformationSummary();
        return documentCreationInfos;
    }

    public DocumentCreationInformation getDocumentCreationInformationById(String id, User user) throws SW360Exception {
        DocumentCreationInformation documentCreationInfo = SPDXDocumentCreationInfoRepository.get(id);
        return documentCreationInfo;
    }

    public AddDocumentRequestSummary addDocumentCreationInformation(DocumentCreationInformation documentCreationInfo, User user) throws SW360Exception {
        AddDocumentRequestSummary requestSummary= new AddDocumentRequestSummary();
        SPDXDocumentCreationInfoRepository.add(documentCreationInfo);
        String documentCreationInfoId = documentCreationInfo.getId();
        String spdxDocumentId = documentCreationInfo.getSpdxDocumentId();
        SPDXDocument spdxDocument = SPDXDocumentRepository.get(spdxDocumentId);
        spdxDocument.setSpdxDocumentCreationInfoId(spdxDocumentId);
        SPDXDocumentRepository.update(spdxDocument);
        return requestSummary.setRequestStatus(AddDocumentRequestStatus.SUCCESS)
                            .setId(documentCreationInfoId);
    }

    public RequestStatus updateDocumentCreationInformation(DocumentCreationInformation documentCreationInfo, User user) throws SW360Exception {
        DocumentCreationInformation actual = SPDXDocumentCreationInfoRepository.get(documentCreationInfo.getId());
        assertNotNull(actual, "Could not find SPDX Document Creation Information to update!");
        SPDXDocumentCreationInfoRepository.update(documentCreationInfo);
        return RequestStatus.SUCCESS;
    }

    public RequestStatus deleteDocumentCreationInformation(String id, User user) throws SW360Exception {
        DocumentCreationInformation documentCreationInfo = SPDXDocumentCreationInfoRepository.get(id);
        SPDXDocumentCreationInfoRepository.remove(documentCreationInfo);
        String spdxDocumentId = documentCreationInfo.getSpdxDocumentId();
        if (spdxDocumentId != null) {
            SPDXDocument spdxDocument = SPDXDocumentRepository.get(spdxDocumentId);
            spdxDocument.unsetSpdxDocumentCreationInfoId();
            SPDXDocumentRepository.update(spdxDocument);
        }
        return RequestStatus.SUCCESS;
    }

}
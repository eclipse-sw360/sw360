/*
 * Copyright Siemens AG, 2013-2019. Part of the SW360 Portal Project.
 * With modifications by Bosch Software Innovations GmbH, 2016.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db;

import com.ibm.cloud.cloudant.v1.Cloudant;
import com.ibm.cloud.cloudant.v1.model.DocumentResult;
import com.google.common.collect.*;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.sw360.commonIO.AttachmentFrontendUtils;
import org.eclipse.sw360.components.summary.SummaryType;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.common.Duration;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.couchdb.AttachmentStreamConnector;
import org.eclipse.sw360.datahandler.db.spdx.document.SpdxDocumentDatabaseHandler;
import org.eclipse.sw360.datahandler.entitlement.ComponentModerator;
import org.eclipse.sw360.datahandler.entitlement.ProjectModerator;
import org.eclipse.sw360.datahandler.entitlement.ReleaseModerator;
import org.eclipse.sw360.datahandler.permissions.DocumentPermissions;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestStatus;
import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.services.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentUsage;
import org.eclipse.sw360.datahandler.services.attachments.CheckStatus;
import org.eclipse.sw360.datahandler.services.changelogs.ChangeLogs;
import org.eclipse.sw360.datahandler.services.changelogs.ChangedFields;
import org.eclipse.sw360.datahandler.services.changelogs.Operation;
import org.eclipse.sw360.common.utils.converter.attachments.AttachmentConverter;
import org.eclipse.sw360.common.utils.converter.components.ClearingStateConverter;
import org.eclipse.sw360.common.utils.converter.components.ComponentConverter;
import org.eclipse.sw360.common.utils.converter.components.ComponentTypeConverter;
import org.eclipse.sw360.common.utils.converter.components.ReleaseConverter;
import org.eclipse.sw360.datahandler.services.components.Component;
import org.eclipse.sw360.datahandler.services.components.ComponentType;
import org.eclipse.sw360.datahandler.services.components.ClearingState;
import org.eclipse.sw360.datahandler.services.components.Release;
import org.eclipse.sw360.datahandler.services.components.ReleaseImmutableField;
import org.eclipse.sw360.datahandler.services.components.ClearingInformation;
import org.eclipse.sw360.datahandler.services.components.EccInformation;
import org.eclipse.sw360.datahandler.services.components.ECCStatus;
import org.eclipse.sw360.datahandler.services.components.COTSDetails;
import org.eclipse.sw360.datahandler.services.components.Repository;
import org.eclipse.sw360.datahandler.services.common.MainlineState;
import org.eclipse.sw360.datahandler.services.common.ReleaseRelationship;
import org.eclipse.sw360.datahandler.services.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.components.BulkOperationNode;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseNode;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseLink;
import org.eclipse.sw360.common.utils.converter.moderation.ModerationRequestConverter;
import org.eclipse.sw360.datahandler.moderation.ModerationClients;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.common.utils.converter.packages.PackageConverter;
import org.eclipse.sw360.common.utils.converter.projects.ProjectConverter;
import org.eclipse.sw360.common.utils.converter.common.DocumentStateConverter;
import org.eclipse.sw360.datahandler.services.common.DocumentState;
import org.eclipse.sw360.datahandler.services.common.ModerationState;
import org.eclipse.sw360.common.utils.converter.common.RequestStatusConverter;
import org.eclipse.sw360.common.utils.converter.common.RequestSummaryConverter;
import org.eclipse.sw360.common.utils.converter.users.RequestedActionConverter;
import org.eclipse.sw360.common.utils.converter.users.UserConverter;
import org.eclipse.sw360.datahandler.services.packages.Package;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.datahandler.services.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.ProjectVulnerabilityRating;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.ReleaseVulnerabilityRelation;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityCheckStatus;
import org.eclipse.sw360.vulnerabilities.db.VulnerabilityDatabaseHandler;
import org.eclipse.sw360.components.ComponentHandler;
import org.eclipse.sw360.components.ComponentHandlerThriftAdapter;
import org.eclipse.sw360.exporter.ComponentExporter;
import org.eclipse.sw360.mail.MailConstants;
import org.eclipse.sw360.mail.MailUtil;
import org.apache.logging.log4j.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.thrift.TException;
import org.eclipse.sw360.spdx.SpdxBOMImporter;
import org.eclipse.sw360.spdx.SpdxBOMImporterSink;
import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.spdx.core.InvalidSPDXAnalysisException;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.*;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static com.google.common.collect.Sets.newHashSet;
import static org.eclipse.sw360.datahandler.common.CommonUtils.*;
import static org.eclipse.sw360.datahandler.common.Duration.durationOf;
import static org.eclipse.sw360.datahandler.common.SW360Assert.*;
import static org.eclipse.sw360.datahandler.common.SW360ConfigKeys.*;
import static org.eclipse.sw360.datahandler.permissions.PermissionUtils.makePermission;

/**
 * Class for accessing Component information from the database
 *
 * @author cedric.bodet@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 * @author alex.borodin@evosoft.com
 * @author thomas.maier@evosoft.com
 */
public class ComponentDatabaseHandler extends AttachmentAwareDatabaseHandler {

    private static final Logger log = LogManager.getLogger(ComponentDatabaseHandler.class);
    private static final String ECC_AUTOSET_COMMENT = "automatically set";
    private static final String ECC_AUTOSET_VALUE = "N";
    private static final String DEFAULT_CATEGORY = "Default_Category";
    private static final String ECC_FIELDS_VALUE_RESET = "";
    private static final String NO_COMPONENT = "Don't have Component created!";
    private static final String NO_RELEASE = "Don't have Release created!";
    private static final List<String> listComponentName = new ArrayList<>();
    private static final Map<String, String> mapReleaseName = new HashMap<>();
    public static final List<String> formats = new ArrayList<>(Arrays.asList(SW360Constants.URL_FORMATS.split(",")));

    /**
     * Connection to the couchDB database
     */
    private final ComponentRepository componentRepository;
    private final ReleaseRepository releaseRepository;
    private final VendorRepository vendorRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final PackageRepository packageRepository;
    private PackageDatabaseHandler packageDatabaseHandler;
    private VulnerabilityDatabaseHandler vulnerabilityDatabaseHandler;
    private ProjectDatabaseHandler projectDatabaseHandlerForVuln;
    private DatabaseHandlerUtil dbHandlerUtil;
    private BulkDeleteUtil bulkDeleteUtil;

    private final AttachmentConnector attachmentConnector;
    private SvmConnector svmConnector;
    private final SpdxDocumentDatabaseHandler spdxDocumentDatabaseHandler;
    /**
     * Access to moderation
     */
    private final ComponentModerator moderator;
    private final ReleaseModerator releaseModerator;
    private final ProjectModerator projectModerator;

    private final MailUtil mailUtil = new MailUtil();

    public ComponentDatabaseHandler(Cloudant client, String dbName, String attachmentDbName, ComponentModerator moderator, ReleaseModerator releaseModerator, ProjectModerator projectModerator) throws MalformedURLException {
        super(client, dbName, attachmentDbName);
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(client, dbName);

        // Create the repositories
        vendorRepository = new VendorRepository(db);
        releaseRepository = new ReleaseRepository(db, vendorRepository);
        componentRepository = new ComponentRepository(db, releaseRepository, vendorRepository);
        projectRepository = new ProjectRepository(db);
        userRepository = new UserRepository(db);
        packageRepository = new PackageRepository(db);

        // Create the moderator
        this.moderator = moderator;
        this.releaseModerator = releaseModerator;
        this.projectModerator = projectModerator;

        // Create the attachment connector
        attachmentConnector = new AttachmentConnector(client, attachmentDbName, durationOf(30, TimeUnit.SECONDS));
        DatabaseConnectorCloudant dbChangeLogs = new DatabaseConnectorCloudant(client, DatabaseSettings.COUCH_DB_CHANGE_LOGS);
        this.dbHandlerUtil = new DatabaseHandlerUtil(dbChangeLogs);

        this.bulkDeleteUtil = new BulkDeleteUtil(this, componentRepository, releaseRepository, projectRepository, moderator, releaseModerator,
                attachmentConnector, attachmentDatabaseHandler, dbHandlerUtil);

        // Create the spdx document database handler
        this.spdxDocumentDatabaseHandler = new SpdxDocumentDatabaseHandler(client, DatabaseSettings.COUCH_DB_SPDX);
    }

    private PackageDatabaseHandler getPackageDatabaseHandler() throws SW360Exception {
        if (packageDatabaseHandler == null) {
            try {
                packageDatabaseHandler = new PackageDatabaseHandler(
                        DatabaseSettings.getConfiguredClient(),
                        DatabaseSettings.COUCH_DB_DATABASE,
                        DatabaseSettings.COUCH_DB_CHANGE_LOGS,
                        DatabaseSettings.COUCH_DB_ATTACHMENTS,
                        attachmentDatabaseHandler,
                        this);
            } catch (MalformedURLException e) {
                throw new RuntimeException("Failed to initialize PackageDatabaseHandler", e);
            }
        }
        return packageDatabaseHandler;
    }

    public ComponentDatabaseHandler(Cloudant client, String dbName, String changeLogsDbName, String attachmentDbName, ComponentModerator moderator, ReleaseModerator releaseModerator, ProjectModerator projectModerator) throws MalformedURLException {
        this(client, dbName, attachmentDbName, moderator, releaseModerator, projectModerator);
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(client, changeLogsDbName);
        this.dbHandlerUtil = new DatabaseHandlerUtil(db);
    }


    public ComponentDatabaseHandler(Cloudant client, String dbName, String attachmentDbName) throws MalformedURLException {
        this(client, dbName, attachmentDbName, new ComponentModerator(), new ReleaseModerator(), new ProjectModerator());
    }

    public ComponentDatabaseHandler(Cloudant client, String dbName, String changelogsDbName, String attachmentDbName) throws MalformedURLException {
        this(client, dbName, attachmentDbName, new ComponentModerator(), new ReleaseModerator(), new ProjectModerator());
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(client, changelogsDbName);
        this.dbHandlerUtil = new DatabaseHandlerUtil(db);
    }

    /** Ranks attachments the way {@code CommonUtils.CHECK_STATUS_COMPARATOR} does. */
    private static final Comparator<CheckStatus> CHECK_STATUS_COMPARATOR = Comparator.comparingInt(cs -> {
        switch (cs) {
            case ACCEPTED:
                return 2;
            case NOTCHECKED:
                return 1;
            case REJECTED:
                return 0;
        }
        throw new IllegalArgumentException("CheckStatus is unknown to this Comparator: " + cs.name());
    });

    private static Optional<Attachment> getBestClearingReport(Release release) {
        return bestAttachmentByCheckStatus(release,
                att -> att.getAttachmentType() == AttachmentType.CLEARING_REPORT
                        || att.getAttachmentType() == AttachmentType.COMPONENT_LICENSE_INFO_XML);
    }

    private static Optional<Attachment> getBestInternalUseScanReport(Release release) {
        return bestAttachmentByCheckStatus(release,
                att -> att.getAttachmentType() == AttachmentType.INTERNAL_USE_SCAN);
    }

    private static Optional<Attachment> bestAttachmentByCheckStatus(Release release, Predicate<Attachment> filter) {
        return nullToEmptySet(release.getAttachments()).stream()
                .filter(filter)
                .max(Comparator.comparing(Attachment::getCheckStatus, CHECK_STATUS_COMPARATOR));
    }

    private void autosetReleaseClearingState(Release releaseAfter, Release releaseBefore) {
        // If the clearing state was manually set to UNDER_CLEARING from an allowed
        // source state (NEW_CLEARING or REPORT_AVAILABLE), preserve the manual override
        // and skip the automatic recalculation based on attachments.
        ClearingState stateBefore = releaseBefore.getClearingState();
        ClearingState stateAfter = releaseAfter.getClearingState();
        if (stateAfter == ClearingState.UNDER_CLEARING
                && stateBefore != ClearingState.UNDER_CLEARING
                && (stateBefore == ClearingState.NEW_CLEARING || stateBefore == ClearingState.REPORT_AVAILABLE)) {
            return;
        }

        Optional<Attachment> oldBestCR = getBestClearingReport(releaseBefore);
        Optional<Attachment> newBestCR = getBestClearingReport(releaseAfter);

        Optional<Attachment> oldSecondBestCR = getBestInternalUseScanReport(releaseBefore);
        Optional<Attachment> newSecondBestCR = getBestInternalUseScanReport(releaseAfter);

        long isrCountAfter = evaluateClearingStateForScanAvailable(releaseAfter);
        if (isrCountAfter > 0) {
            releaseAfter.setClearingState(ClearingState.SCAN_AVAILABLE);
        }

        if (newSecondBestCR.isPresent()) {
            if (newSecondBestCR.get().getCheckStatus() == CheckStatus.ACCEPTED) {
                releaseAfter.setClearingState(ClearingState.INTERNAL_USE_SCAN_AVAILABLE);
            }
        } else {
            if (oldSecondBestCR.isPresent()) {
                releaseAfter.setClearingState(ClearingState.NEW_CLEARING);
            }
        }

        if (newBestCR.isPresent()) {
            if (newBestCR.get().getCheckStatus() == CheckStatus.ACCEPTED) {
                releaseAfter.setClearingState(ClearingState.APPROVED);
            } else {
                releaseAfter.setClearingState(ClearingState.REPORT_AVAILABLE);
            }
        } else {
            if (oldBestCR.isPresent()) {
                releaseAfter.setClearingState(ClearingState.NEW_CLEARING);
            }

            if (isrCountAfter > 0) {
                releaseAfter.setClearingState(ClearingState.SCAN_AVAILABLE);
            }

            if (newSecondBestCR.isPresent() &&  (newSecondBestCR.get().getCheckStatus() == CheckStatus.ACCEPTED)) {
                releaseAfter.setClearingState(ClearingState.INTERNAL_USE_SCAN_AVAILABLE);
            }
        }
    }


    private static void ensureEccInformationIsSet(Release release) {
        if (release.getEccInformation() == null) {
            release.setEccInformation(new EccInformation());
        }
        EccInformation eccInformation = release.getEccInformation();
        if (eccInformation.getEccStatus() == null) {
            eccInformation.setEccStatus(ECCStatus.OPEN);
        }
        // Initialize optional boolean fields to prevent null vs false comparison issues,
        // e.g. in hasChangesInEccFields
        if (eccInformation.getContainsCryptography() == null) {
            eccInformation.setContainsCryptography(false);
        }
    }

    /////////////////////
    // SUMMARY GETTERS //
    /////////////////////
    public List<Component> getComponentsShort(Set<String> ids) {
        return componentRepository.makeSummary(SummaryType.SHORT, ids);
    }

    public List<Component> getComponentSummary(User user) {
        return componentRepository.getComponentSummary(user);
    }

    public List<Component> getComponentSummaryForExport() {
        return componentRepository.getSummaryForExport();
    }

    public List<Component> getComponentDetailedSummaryForExport() {
        return componentRepository.getDetailedSummaryForExport();
    }

    public List<Release> getReleaseSummary() throws TException {
        List<Release> releases = releaseRepository.getReleaseSummary();
        releases.forEach(ComponentDatabaseHandler::ensureEccInformationIsSet);


        // todo: move filling out of department to ReleaseRepository/ReleaseSummary???
        Set<String> userIds = releases.stream().map(Release::getCreatedBy).collect(Collectors.toSet());
        Map<String, User> usersByEmail = ThriftUtils.getIdMap(
                userRepository.get(userIds).stream().map(UserConverter::toThrift).collect(Collectors.toList()));
        releases.forEach(release -> release.setCreatorDepartment(Optional
                .ofNullable(release.getCreatedBy())
                .map(usersByEmail::get)
                .map(User::getDepartment)
                .orElse(null)));
        return releases;
    }

    public List<Release> getAccessibleReleaseSummary(User user) throws TException {
        return getAccessibleReleaseList(getReleaseSummary(), user);
    }

    public List<Release> getRecentReleases() {
        return releaseRepository.getRecentReleases();
    }

    public List<Release> getRecentReleasesWithAccessibility(User user) {
        List<Release> releaseList = releaseRepository.getRecentReleases();
        for (Release release : releaseList) {
            makePermission(release, user).fillPermissions();
            for (RequestedAction action : RequestedAction.values()) {
                release.getPermissions().put(
                        RequestedActionConverter.fromThrift(action),
                        isReleaseActionAllowed(release, user, action));
            }
        }
        return releaseList;
    }

    public List<Component> getSubscribedComponents(String user) {
        return componentRepository.getSubscribedComponents(user);
    }

    public List<Release> getSubscribedReleases(String email) {
        return releaseRepository.getSubscribedReleases(email);
    }


    public List<Release> getReleasesFromVendorId(String id, User user) throws TException {
        return releaseRepository.getReleasesFromVendorId(id, user);
    }

    public List<Release> getReleasesFromVendorIds(Set<String> ids) {
        return releaseRepository.getReleasesFromVendorIds(ids);
    }

    public List<Release> getAccessibleReleasesFromVendorIds(Set<String> ids, User user) {
        return getAccessibleReleaseList(releaseRepository.getReleasesFromVendorIds(ids), user);
    }

    public Set<Release> getReleasesByVendorId(String vendorId) {
        return releaseRepository.getReleasesByVendorId(vendorId);
    }

    public List<Release> getReleasesFromComponentId(String id, User user) throws TException {
        return releaseRepository.getReleasesFromComponentId(id, user);
    }

    public List<Release> getReleasesFullDocsFromComponentId(String id, User user) throws TException {
        return releaseRepository.getReleasesFullDocsFromComponentId(id, user);
    }

    public Map<PaginationData, List<Release>> getReleasesFromComponentIdWithPagination(String id, User user, PaginationData pageData) throws TException {
        return releaseRepository.getReleasesFromComponentIdWithPagination(id, user, pageData);
    }

    public List<Component> getMyComponents(String user) {
        Collection<Component> myComponents = componentRepository.getMyComponents(user);

        return componentRepository.makeSummaryFromFullDocs(SummaryType.HOME, myComponents);
    }

    public List<Component> getSummaryForExport() {
        return componentRepository.getSummaryForExport();
    }

    ////////////////////////////
    // GET INDIVIDUAL OBJECTS //
    ////////////////////////////

    public void addSelectLogs(Component component, User user) {

        DatabaseHandlerUtil.addSelectLogs(ComponentConverter.toThrift(component), user.getEmail(), attachmentConnector);
    }
    public void addSelectLogs(Release release, User user) {

        DatabaseHandlerUtil.addSelectLogs(ReleaseConverter.toThrift(release), user.getEmail(), attachmentConnector);
    }

    public Component getComponent(String id, User user) throws SW360Exception {
        try {
            Component component = componentRepository.get(id);

            if (component == null) {
                log.error("Component not found in database. Component ID: {}", id);
                throw fail(404, "Could not fetch component from database! id=" + id);
            }

            component.setReleases(releaseRepository.makeSummaryWithPermissions(SummaryType.SUMMARY, component.getReleaseIds(), user));
            component.setReleaseIds(null);

            setMainLicenses(component);

            vendorRepository.fillVendor(component);

            makePermission(component, user).fillPermissions();

            return component;
        } catch (SW360Exception e) {
            log.error("Error fetching component. Component ID: {}, Error: {}", id, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error while fetching component. Component ID: {}, Error: {}", id, e.getMessage(), e);
            throw new SW360Exception("Failed to fetch component with id: " + id + ". " + e.getMessage());
        }
    }

    public Component getAccessibleComponent(String id, User user) throws SW360Exception {
        Component component = getComponent(id, user);
        Map<org.eclipse.sw360.datahandler.services.users.RequestedAction, Boolean> permissions = component.getPermissions();
        if (!Boolean.TRUE.equals(permissions.get(org.eclipse.sw360.datahandler.services.users.RequestedAction.READ))) {
            throw fail(403, "Could not fetch component because access is denied! id=" + id);
        }
        return component;
    }

    //Used by scheduled upload service to get releases of a component without user info
    public Release getRelease(String id) {
        return releaseRepository.get(id);
    }

    public Release getRelease(String id, User user) throws SW360Exception {
        return getRelease(id, user, null);
    }

    public Release getRelease(String id, User user, Map<String, Vendor> vendorCache) throws SW360Exception {
        Release release = releaseRepository.get(id);

        if (release == null) {
            throw fail(404, "Could not fetch release from database! id=" + id);
        }

        vendorRepository.fillVendor(release, vendorCache);
        // Set permissions
        if (user != null) {
            makePermission(release, user).fillPermissions();
        }

        ensureEccInformationIsSet(release);

        return release;
    }

    public Release getAccessibleRelease(String id, User user) throws SW360Exception {
        Release release = getRelease(id, user);
        if (!isReleaseActionAllowed(release, user, RequestedAction.READ)) {
            throw fail(403, "Could not access the release! id=" + id);
        }
        return release;
    }

    private void setMainLicenses(Component component) {
        if (component.getMainLicenseIds() == null && (component.getReleases() != null)) {
            Set<String> licenseIds = new HashSet<>();

            for (Release release : component.getReleases()) {
                licenseIds.addAll(nullToEmptySet(release.getMainLicenseIds()));
            }

            component.setMainLicenseIds(licenseIds);
        }
    }

    ////////////////////////////
    // ADD INDIVIDUAL OBJECTS //
    ////////////////////////////

    /**
     * Add new release to the database
     */
    public AddDocumentRequestSummary addComponent(Component component, String user) throws SW360Exception {
        if (isNotNullEmptyOrWhitespace(component.getVcs())) {
            String vcsUrl = component.getVcs();
            if (isDuplicateUsingVcs(vcsUrl, true)){
                final AddDocumentRequestSummary addDocumentRequestSummary = new AddDocumentRequestSummary()
                        .setRequestStatus(AddDocumentRequestStatus.DUPLICATE);
                Set<String> duplicates = componentRepository.getComponentIdsByVCS(component.getVcs(), true);
                if (duplicates.size() == 1) {
                    duplicates.forEach(addDocumentRequestSummary::setId);
                }
                return addDocumentRequestSummary;
            }
            if (!isValidUrl(vcsUrl)) {
                log.error("Invalid VCS URL: " + vcsUrl);
                return new AddDocumentRequestSummary().setRequestStatus(AddDocumentRequestStatus.INVALID_INPUT);
            }
        }

        if (component.getName().trim().length() == 0) {
            return new AddDocumentRequestSummary().setRequestStatus(AddDocumentRequestStatus.NAMINGERROR);
        } else {
            if (isDuplicate(component.getName(), true)) {
                final AddDocumentRequestSummary addDocumentRequestSummary = new AddDocumentRequestSummary()
                        .setRequestStatus(AddDocumentRequestStatus.DUPLICATE);
                Set<String> duplicates = componentRepository.getComponentIdsByName(component.getName(), true);
                if (duplicates.size() == 1) {
                    duplicates.forEach(addDocumentRequestSummary::setId);
                }
                return addDocumentRequestSummary;
            }
        }

        if (!isDependenciesExistInComponent(component)) {
            return new AddDocumentRequestSummary()
                    .setRequestStatus(AddDocumentRequestStatus.INVALID_INPUT);
        }

        removeLeadingTrailingWhitespace(component);
        Set<String> categories = component.getCategories();
        if (categories == null || categories.isEmpty()) {
            component.setCategories(ImmutableSet.of(DEFAULT_CATEGORY));
        }

        // Prepare the component
        prepareComponent(component);

        // Save creating user
        component.setCreatedBy(user);
        component.setCreatedOn(SW360Utils.getCreatedOn());

        // Add the component to the database and return ID
        componentRepository.add(component);
        sendMailNotificationsForNewComponent(component, user);
        dbHandlerUtil.addChangeLogs(ComponentConverter.toThrift(component), null, user, Operation.CREATE, attachmentConnector,
                Lists.newArrayList(), null, null);
        return new AddDocumentRequestSummary()
                .setRequestStatus(AddDocumentRequestStatus.SUCCESS)
                .setId(component.getId());
    }

    /**
     * Add a single new release to the database
     */
    public AddDocumentRequestSummary addRelease(Release release, User user) throws SW360Exception {
        removeLeadingTrailingWhitespace(release);
        String name = release.getName();
        String version = release.getVersion();
        if (name == null || name.isEmpty() || version == null || version.isEmpty()) {
            return new AddDocumentRequestSummary().setRequestStatus(AddDocumentRequestStatus.NAMINGERROR);
        }

        // Prepare the release and get underlying component ID
        prepareRelease(release);
        if(isDuplicate(release)) {
            final AddDocumentRequestSummary addDocumentRequestSummary = new AddDocumentRequestSummary()
                    .setRequestStatus(AddDocumentRequestStatus.DUPLICATE);
            List<Release> duplicates = releaseRepository.searchByNameAndVersion(release.getName(), release.getVersion(), true);
            if (duplicates.size() == 1) {
                duplicates.stream()
                        .map(Release::getId)
                        .forEach(addDocumentRequestSummary::setId);
            }
            return addDocumentRequestSummary;
        }

        if (!isDependenciesExistsInRelease(release)
                || verifyLinkedPackages(Collections.emptySet(), CommonUtils.nullToEmptySet(release.getPackageIds()), "") ) {
            return new AddDocumentRequestSummary()
                    .setRequestStatus(AddDocumentRequestStatus.INVALID_INPUT);
        } else if(!validSourceCodeDownloadUrl(release)){
            return new AddDocumentRequestSummary().setRequestStatus(AddDocumentRequestStatus.INVALID_SOURCE_CODE_URL);
        }

        // Block nested release linking if disabled by configuration
        if (!SW360Constants.ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP) {
            Map<String, ReleaseRelationship> releaseLinks = release.getReleaseIdToRelationship();
            if (!CommonUtils.isNullOrEmptyMap(releaseLinks)) {
                throw new SW360Exception(SW360Constants.PLEASE_ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP);
            }
        }

        String componentId = release.getComponentId();
        // Ensure that component exists
        Component component = componentRepository.get(componentId);
        assertNotNull(component);

        // Save creating user
        release.setCreatedBy(user.getEmail());
        release.setCreatedOn(SW360Utils.getCreatedOn());

        // Add default ECC options if download url is set
        autosetEccFieldsForReleaseWithDownloadUrl(release);

        // check for MainlineState change
        setMainlineState(release, user, null);
        if (release.getClearingState() == null) {
            release.setClearingState(ClearingState.NEW_CLEARING);
        }
        // Add release to database
        releaseRepository.add(release);
        if (SW360Utils.readConfig(SPDX_DOCUMENT_ENABLED, false)) {
            try {
                spdxDocumentDatabaseHandler.updateSPDX(user, release);
            } catch (TException ex) {
                log.error("Error updateSPDX "+ ex.getMessage());
            }
        }
        final String id = release.getId();

        // Update the underlying component
        { if (component.getReleaseIds() == null) { component.setReleaseIds(new HashSet<>()); } component.getReleaseIds().add(id); };

        if (component.getLanguages() == null) {
            component.setLanguages(new HashSet<String>());
        }
        if (component.getOperatingSystems() == null) {
            component.setOperatingSystems(new HashSet<String>());
        }
        if (component.getVendorNames() == null) {
            component.setVendorNames(new HashSet<String>());
        }
        if (component.getMainLicenseIds() == null) {
            component.setMainLicenseIds(new HashSet<String>());
        }
        Component oldComponent = ComponentConverter.fromThrift(ComponentConverter.toThrift(component));
        updateReleaseDependentFieldsForComponent(component, release);
        updateModifiedFields(component, user.getEmail());
        componentRepository.update(component);
        // update linked packages
        updateLinkedPackages(Collections.emptySet(), CommonUtils.nullToEmptySet(release.getPackageIds()), id, user);
        sendMailNotificationsForNewRelease(release, user.getEmail());
        dbHandlerUtil.addChangeLogs(ReleaseConverter.toThrift(release), null, user.getEmail(), Operation.CREATE, attachmentConnector,
                Lists.newArrayList(), null, null);
        dbHandlerUtil.addChangeLogs(ComponentConverter.toThrift(component), ComponentConverter.toThrift(oldComponent), user.getEmail(), Operation.UPDATE,
                attachmentConnector, Lists.newArrayList(), release.getId(), Operation.RELEASE_CREATE);
        return new AddDocumentRequestSummary()
                .setRequestStatus(AddDocumentRequestStatus.SUCCESS)
                .setId(id);
    }

    private boolean isDuplicate(Component component, boolean caseInsensitive){
        return isDuplicate(component.getName(), caseInsensitive);
    }

    private boolean isDuplicate(Release release){
        return isDuplicate(release.getName(), release.getVersion());
    }

    public boolean isDuplicate(String componentName, boolean caseInsensitive) {
        if (isNullEmptyOrWhitespace(componentName)) {
            return false;
        }
        Set<String> duplicates = componentRepository.getComponentIdsByName(componentName.trim(), caseInsensitive);
        return duplicates.size()>0;
    }

    private boolean isDuplicateUsingVcs(String vcsUrl, boolean caseInsensitive){
        if (isNullEmptyOrWhitespace(vcsUrl)) {
            return false;
        }
        Set<String> duplicates = componentRepository.getComponentIdsByVCS(vcsUrl, caseInsensitive);
        return duplicates.size()>0;
    }

    private boolean isDuplicate(String releaseName, String releaseVersion) {
        if (isNullEmptyOrWhitespace(releaseName)) {
            return false;
        }
        List<Release> duplicates = releaseRepository.searchByNameAndVersion(releaseName, releaseVersion, true);
        return duplicates.size()>0;
    }

    private void isDuplicateComponent(List<String> componentNames, boolean caseInsensitive) {
        for (String name : componentNames) {
            if(!isDuplicate(name, caseInsensitive))
                listComponentName.add(name);
        }
    }

    private void isDuplicateRelease(Map<String, String>  releases) {
        for (Map.Entry<String, String> release : releases.entrySet()) {
            if(!isDuplicate(release.getKey(), release.getValue()))
                mapReleaseName.put(release.getKey(), release.getValue());
        }
    }

    private void resetReleaseDependentFields(Component component) {
        component.setLanguages(new HashSet<String>());
        component.setOperatingSystems(new HashSet<String>());
        component.setVendorNames(new HashSet<String>());
        component.setMainLicenseIds(new HashSet<String>());
    }

    public void updateReleaseDependentFieldsForComponent(Component component, Release release) {
        if (release != null && component != null) {
            if (component.getLanguages() == null) {
                component.setLanguages(new HashSet<String>());
            }
            component.getLanguages().addAll(nullToEmptySet(release.getLanguages()));

            if (component.getOperatingSystems() == null) {
                component.setOperatingSystems(new HashSet<String>());
            }
            component.getOperatingSystems().addAll(nullToEmptySet(release.getOperatingSystems()));

            if(component.getSoftwarePlatforms() == null) {
                component.setSoftwarePlatforms(new HashSet<String>());
            }
            component.getSoftwarePlatforms().addAll(nullToEmptySet(release.getSoftwarePlatforms()));

            if (component.getVendorNames() == null) {
                component.setVendorNames(new HashSet<String>());
            }
            if (release.getVendor() != null)
                component.getVendorNames().add(release.getVendor().getShortname());
            else if (!isNullOrEmpty(release.getVendorId())) {
                Vendor vendor = getVendor(release.getVendorId());
                component.getVendorNames().add(vendor.getShortname());
            }

            if (component.getMainLicenseIds() == null) component.setMainLicenseIds(new HashSet<String>());
            if ((release.getMainLicenseIds() != null)) {
                component.getMainLicenseIds().addAll(release.getMainLicenseIds());
            }
        }
    }

    private Vendor getVendor(String vendorId) {
        return vendorRepository.get(vendorId);
    }

    ///////////////////////////////
    // UPDATE INDIVIDUAL OBJECTS //
    ///////////////////////////////
    public RequestStatus updateComponent(Component component, User user) throws SW360Exception {
        return updateComponent(component, user, false);
    }

    public RequestStatus updateComponent(Component component, User user, boolean forceUpdate) throws SW360Exception {
        removeLeadingTrailingWhitespace(component);
        String name = component.getName();
        String vcs = component.getVcs();

        if (name == null || name.isEmpty()) {
            return RequestStatus.NAMINGERROR;
        }

        if (isNotNullEmptyOrWhitespace(vcs)) {
            if (!CommonUtils.isValidUrl(vcs)) {
                log.error("Invalid VCS URL: " + vcs);
                return RequestStatus.INVALID_INPUT;
            }
        }

        Set<String> categories = component.getCategories();
        if (categories == null || categories.isEmpty()) {
            component.setCategories(ImmutableSet.of(DEFAULT_CATEGORY));
        }

        // Prepare component for database
        prepareComponent(component);

        // Get actual document for members that should not change
        Component actual = componentRepository.get(component.getId());
        assertNotNull(actual, "Could not find component to update!");
        DatabaseHandlerUtil.saveAttachmentInFileSystem(attachmentConnector, toThriftAttachments(actual.getAttachments()),
                toThriftAttachments(component.getAttachments()), user.getEmail(), component.getId());
        if (changeWouldResultInDuplicate(actual, component)) {
            return RequestStatus.DUPLICATE;
        } else if (duplicateAttachmentExist(component)) {
            return RequestStatus.DUPLICATE_ATTACHMENT;
        } else if (!isDependenciesExistInComponent(component)){
            return RequestStatus.INVALID_INPUT;
        } else if (makePermission(actual, user).isActionAllowed(RequestedAction.WRITE) || forceUpdate) {
            // Nested releases and attachments should not be updated by this method
            boolean isComponentNameChanged = false;
            if ((actual.getReleaseIds() != null)) {
                component.setReleaseIds(actual.getReleaseIds());
                isComponentNameChanged = !component.getName().equals(actual.getName());
            }

            copyImmutableFields(component, actual);
            component.setAttachments(fromThriftAttachments(getAllAttachmentsToKeep(toSource(actual), toThriftAttachments(actual.getAttachments()), toThriftAttachments(component.getAttachments()))));
            recomputeReleaseDependentFields(component, null);

            List<ChangeLogs> referenceDocLogList = new LinkedList<>();
            Set<Attachment> attachmentsAfter = component.getAttachments();
            Set<Attachment> attachmentsBefore = actual.getAttachments();
            DatabaseHandlerUtil.populateChangeLogsForAttachmentsDeleted(toThriftAttachments(attachmentsBefore), toThriftAttachments(attachmentsAfter),
                    referenceDocLogList, user.getEmail(), component.getId(), Operation.COMPONENT_UPDATE,
                    attachmentConnector, false);

            updateComponentInternal(component, actual, user);

            if (isComponentNameChanged) {
                updateComponentDependentFieldsForRelease(component,referenceDocLogList,user.getEmail());
            }

            if (component.getComponentType() != null && !component.getComponentType().equals(actual.getComponentType()) && !ComponentType.OSS.equals(component.getComponentType())) {
                updateEccStatusForRelease(component);
            }

            dbHandlerUtil.addChangeLogs(ComponentConverter.toThrift(component), ComponentConverter.toThrift(actual), user.getEmail(), Operation.UPDATE, attachmentConnector,
                    referenceDocLogList, null, null);
        } else {
            return RequestStatusConverter.fromThrift(
                    moderator.updateComponent(ComponentConverter.toThrift(component), user));
        }
        return RequestStatus.SUCCESS;

    }

    public Component getComponentByName(String name) {
        Set<String> components = componentRepository.getComponentIdsByName(name,true);
        if (components != null && components.size() == 1) {
            Component comp = componentRepository.get(components.iterator().next());
            return comp;
        } else {
            return null;
        }
    }

    public Map<PaginationData, List<Component>> searchComponentByNamePrefixPaginated(User user, String name, PaginationData pageData) {
        return componentRepository.searchComponentByNamePrefixPaginated(user, name, pageData);
    }

    public Map<PaginationData, List<Component>> searchComponentByExactNamePaginated(User user, String name, PaginationData pageData) {
        return componentRepository.searchComponentByExactNamePaginated(user, name, pageData);
    }

    public Map<PaginationData, List<Component>> searchComponentByExactValues(Map<String,Set<String>> subQueryRestrictions, User user, PaginationData pageData) {
        Map<PaginationData, List<Component>> resultMap = componentRepository.searchComponentByExactValues(subQueryRestrictions, user, pageData);
        List<Component> resultComponentList = resultMap.get(pageData);
        for (Component component : resultComponentList) {
            makePermission(component, user).fillPermissionsInOther(component);
        }
        return Collections.singletonMap(pageData, resultComponentList);
    }

    private boolean isDependenciesExistInComponent(Component component) {
        boolean isValidDependentIds = true;
        if ((component.getReleaseIds() != null)) {
            Set<String> releaseIds = component.getReleaseIds();
            isValidDependentIds = DatabaseHandlerUtil.isAllIdInSetExists(releaseIds, releaseRepository);
        }

        if (isValidDependentIds && (component.getDefaultVendorId() != null)) {
            isValidDependentIds = DatabaseHandlerUtil.isAllIdInSetExists(Sets.newHashSet(component.getDefaultVendorId()), vendorRepository);
        }
        return isValidDependentIds;
    }

    private boolean isDependenciesExistsInRelease(Release release) {
        boolean isValidDependentIds = true;
        if ((release.getComponentId() != null)) {
            String componentId = release.getComponentId();
            isValidDependentIds = DatabaseHandlerUtil.isAllIdInSetExists(Sets.newHashSet(componentId), componentRepository);
        }

        if (isValidDependentIds && (release.getReleaseIdToRelationship() != null)) {
            Set<String> releaseIds = release.getReleaseIdToRelationship().keySet();
            isValidDependentIds = DatabaseHandlerUtil.isAllIdInSetExists(Sets.newHashSet(releaseIds), releaseRepository);
        }

        if (isValidDependentIds && (release.getVendorId() != null)) {
            String vendorId = release.getVendorId();
            isValidDependentIds = DatabaseHandlerUtil.isAllIdInSetExists(Sets.newHashSet(vendorId), vendorRepository);
        }

        if (isValidDependentIds && (release.getPackageIds() != null)) {
            Set<String> pacakgeIds = release.getPackageIds();
            isValidDependentIds = DatabaseHandlerUtil.isAllIdInSetExists(pacakgeIds, packageRepository);
        }
        return isValidDependentIds;
    }

    private void updateComponentDependentFieldsForRelease(Component component, List<ChangeLogs> referenceDocLogList,
                                                          String userEdited) {
        String name = component.getName();
        for (Release release : releaseRepository.getReleasesFromComponentId(component.getId())) {
            ChangeLogs changeLog = DatabaseHandlerUtil.initChangeLogsObj(ReleaseConverter.toThrift(release), userEdited, component.getId(),
                    Operation.UPDATE, Operation.COMPONENT_UPDATE);
            Set<ChangedFields> changes = new HashSet<ChangedFields>();
            ChangedFields nameFields = new ChangedFields();
            nameFields.setFieldName("name");
            nameFields.setFieldValueOld(DatabaseHandlerUtil.convertObjectToJson(release.getName()));
            nameFields.setFieldValueNew(DatabaseHandlerUtil.convertObjectToJson(name));
            changes.add(nameFields);
            changeLog.setChanges(changes);
            release.setName(name);
            updateModifiedFields(release, userEdited);
            releaseRepository.update(release);
            referenceDocLogList.add(changeLog);
        }
    }

    private void updateEccStatusForRelease(Component component) {
        for (Release release : releaseRepository.getReleasesFromComponentId(component.getId())) {
            EccInformation eccInfo = release.getEccInformation();
            eccInfo.setEccStatus(ECCStatus.OPEN);
            eccInfo.setAl(ECC_FIELDS_VALUE_RESET);
            eccInfo.setEccn(ECC_FIELDS_VALUE_RESET);
            eccInfo.setEccComment(ECC_FIELDS_VALUE_RESET);
            releaseRepository.update(release);
        }
    }

    private boolean changeWouldResultInDuplicate(Component before, Component after) {
        String beforeVCS = isNullEmptyOrWhitespace(before.getVcs()) ? "" : before.getVcs().trim();
        String afterVCS = isNullEmptyOrWhitespace(after.getVcs()) ? "" : after.getVcs().trim();

        if (before.getName().trim().equalsIgnoreCase(after.getName().trim())) {
            if (beforeVCS.equalsIgnoreCase(afterVCS)) {
                return false;
            }
            return isDuplicateUsingVcs(afterVCS, true);
        }

        return isDuplicate(after, true);
    }

    private boolean duplicateAttachmentExist(Component component) {
        if(component.getAttachments() != null && !component.getAttachments().isEmpty()) {
            return AttachmentConnector.isDuplicateAttachment(toThriftAttachments(component.getAttachments()));
        }
        return false;
    }

    private void updateComponentInternal(Component updated, Component current, User user) {
        updateModifiedFields(updated, user.getEmail());
        // Update the database with the component
        componentRepository.update(updated);

        //clean up attachments in database
        attachmentConnector.deleteAttachmentDifference(toThriftAttachments(current.getAttachments()), toThriftAttachments(updated.getAttachments()));
        sendMailNotificationsForComponentUpdate(updated, user.getEmail());
    }

    private void prepareComponent(Component component) throws SW360Exception {
        if (CommonUtils.isNullEmptyOrWhitespace(component.getName())) {
            throw fail("component name cannot be empty");
        }
        component.setType(SW360Constants.TYPE_COMPONENT);
        component.setPermissions(null);
        component.setReleases(null);
        component.setDefaultVendor(null);

        setSha1ForAttachments(component.getAttachments());
    }

    /**
     * POJO counterpart of {@link AttachmentConnector#setSha1ForAttachments}. Mutates the given
     * attachments in place — converting to thrift first would compute the sha1 on throwaway copies
     * and the value would never reach the document that gets persisted.
     */
    private void setSha1ForAttachments(Set<Attachment> attachments) {
        for (Attachment attachment : nullToEmptySet(attachments)) {
            if (isNullOrEmpty(attachment.getSha1())) {
                attachment.setSha1(
                        attachmentConnector.getSha1FromAttachmentContentId(attachment.getAttachmentContentId()));
            }
        }
    }

    public RequestSummary updateComponents(Set<Component> components, User user) throws SW360Exception {
        return RequestSummaryConverter.fromThrift(
                RepositoryUtils.doBulk(prepareComponents(components), user, componentRepository));
    }


    public RequestStatus updateComponentFromAdditionsAndDeletions(Component componentAdditions, Component componentDeletions, User user){

        try {
            Component component = getComponent(componentAdditions.getId(), user);
            org.eclipse.sw360.datahandler.thrift.components.Component thriftComponent =
                    moderator.updateComponentFromModerationRequest(
                            ComponentConverter.toThrift(component),
                            ComponentConverter.toThrift(componentAdditions),
                            ComponentConverter.toThrift(componentDeletions));
            component = ComponentConverter.fromThrift(thriftComponent);
            return updateComponent(component, user);
        } catch (SW360Exception e) {
            log.error("Could not get original component when updating from moderation request.");
            return RequestStatus.FAILURE;
        }
    }


    public RequestStatus mergeComponents(String mergeTargetId, String mergeSourceId, Component mergeSelection,
                                         User sessionUser) throws TException {
        Component mergeTarget = getComponent(mergeTargetId, sessionUser);
        Component mergeSource = getComponent(mergeSourceId, sessionUser);
        Component mergeTargetOriginal = ComponentConverter.fromThrift(ComponentConverter.toThrift(mergeTarget));

        Set<String> srcComponentReleaseIds = nullToEmptyList(mergeSource.getReleases()).stream().map(Release::getId)
                .collect(Collectors.toSet());
        Set<String> targetComponentReleaseIds = nullToEmptyList(mergeTarget.getReleases()).stream().map(Release::getId)
                .collect(Collectors.toSet());
        Set<String> releaseIds = Stream.concat(targetComponentReleaseIds.stream(), srcComponentReleaseIds.stream())
                .collect(Collectors.toSet());

        long noOfReleasesNotAllowedToUpdate = getNoOfReleasesNotAllowedToUpdate(srcComponentReleaseIds, sessionUser);

        if (!makePermission(mergeTarget, sessionUser).isActionAllowed(RequestedAction.WRITE)
                || !makePermission(mergeSource, sessionUser).isActionAllowed(RequestedAction.WRITE)
                || !makePermission(mergeSource, sessionUser).isActionAllowed(RequestedAction.DELETE)
                || noOfReleasesNotAllowedToUpdate > 0) {
            return RequestStatus.ACCESS_DENIED;
        }

        if (isComponentUnderModeration(mergeTargetId) ||
                isComponentUnderModeration(mergeSourceId)){
            return RequestStatus.IN_USE;
        }

        try {
            // First merge everything into the new compontent which is mergable in one step (attachments, plain fields)
            mergePlainFields(mergeSelection, mergeTarget, mergeSource);
            mergeAttachments(mergeSelection, mergeTarget, mergeSource);
            transferReleases(releaseIds, mergeTarget, mergeSource);
            recomputeReleaseDependentFields(mergeTarget, null);

            // update target first. If updating source fails, no data is lost (but inconsistency might occur)
            updateComponentCompletely(mergeTarget, sessionUser);
            // now, update source (before deletion so that attachments and releases and
            // stuff that has been migrated will not be deleted by component deletion!)
            updateComponentCompletelyWithoutDeletingAttachment(mergeSource, sessionUser);
            // now update some release fields related to the component (e.g. id and name)
            updateReleasesAfterMerge(targetComponentReleaseIds, srcComponentReleaseIds, mergeSelection, mergeTarget,
                    sessionUser);

            // Finally we can delete the source component
            deleteComponent(mergeSourceId, sessionUser);

        } catch(Exception e) {
            log.error("Cannot merge component [" + mergeSource.getId() + "] into [" + mergeTarget.getId() + "]. Releases after merge: " + releaseIds, e);
            return RequestStatus.FAILURE;
        }
        dbHandlerUtil.addChangeLogs(ComponentConverter.toThrift(mergeTarget), ComponentConverter.toThrift(mergeTargetOriginal), sessionUser.getEmail(), Operation.UPDATE,
                attachmentConnector, Lists.newArrayList(), null, Operation.MERGE_COMPONENT);
        dbHandlerUtil.addChangeLogs(null, ComponentConverter.toThrift(mergeSource), sessionUser.getEmail(), Operation.DELETE, null,
                Lists.newArrayList(), mergeTargetId, Operation.MERGE_COMPONENT);
        return RequestStatus.SUCCESS;
    }

    private boolean isComponentUnderModeration(String componentSourceId) throws TException {
        List<org.eclipse.sw360.datahandler.services.moderation.ModerationRequest> sourceModerationRequests =
                ModerationClients.get().getModerationRequestByDocumentId(componentSourceId);
        return sourceModerationRequests.stream().anyMatch(CommonUtils::isInProgressOrPending);
    }

    private void mergePlainFields(Component mergeSelection, Component mergeTarget, Component mergeSource) {
        // First handle the creator of the component in a way, that the discarded creator will be on the
        // moderator list afterwards. There is nothing to do, if source and target author are the same
        if(!nullToEmpty(mergeTarget.getCreatedBy()).equals(mergeSource.getCreatedBy())) {
            if(nullToEmpty(mergeSelection.getCreatedBy()).equals(nullToEmpty(mergeTarget.getCreatedBy()))) {
                // creator of the target component should be retained. Add creator of source component to list of moderators.
                mergeTarget.setModerators(mergeSelection.getModerators());
                if(!isNullOrEmpty(mergeSource.getCreatedBy())) {
                    Set<String> moderators = new HashSet<>(nullToEmptySet(mergeTarget.getModerators()));
                    moderators.add(mergeSource.getCreatedBy());
                    mergeTarget.setModerators(moderators);
                }
            } else {
                // creator of the source component has been selected. Add creator of target component to list of moderators.

                // remember creator otherwise it is overwritten
                String creator = mergeTarget.getCreatedBy();

                // merge
                mergeTarget.setModerators(mergeSelection.getModerators());
                if(!isNullOrEmpty(mergeTarget.getCreatedBy())) {
                    Set<String> moderators = new HashSet<>(nullToEmptySet(mergeTarget.getModerators()));
                    moderators.add(mergeTarget.getCreatedBy());
                    mergeTarget.setModerators(moderators);
                }
            }
        }

        // Handle other fields
        copyIfSet(mergeSelection.getName(), mergeTarget::setName);
        copyIfSet(mergeSelection.getCreatedOn(), mergeTarget::setCreatedOn);
        copyIfSet(mergeSelection.getCreatedBy(), mergeTarget::setCreatedBy);
        copyIfSet(mergeSelection.getCategories(), mergeTarget::setCategories);
        copyIfSet(mergeSelection.getComponentType(), mergeTarget::setComponentType);
        copyIfSet(mergeSelection.getDefaultVendorId(), mergeTarget::setDefaultVendorId);
        copyIfSet(mergeSelection.getHomepage(), mergeTarget::setHomepage);
        copyIfSet(mergeSelection.getBlog(), mergeTarget::setBlog);
        copyIfSet(mergeSelection.getWiki(), mergeTarget::setWiki);
        copyIfSet(mergeSelection.getMailinglist(), mergeTarget::setMailinglist);
        copyIfSet(mergeSelection.getDescription(), mergeTarget::setDescription);
        copyIfSet(mergeSelection.getExternalIds(), mergeTarget::setExternalIds);
        copyIfSet(mergeSelection.getAdditionalData(), mergeTarget::setAdditionalData);
        copyIfSet(mergeSelection.getComponentOwner(), mergeTarget::setComponentOwner);
        copyIfSet(mergeSelection.getOwnerAccountingUnit(), mergeTarget::setOwnerAccountingUnit);
        copyIfSet(mergeSelection.getOwnerGroup(), mergeTarget::setOwnerGroup);
        copyIfSet(mergeSelection.getOwnerCountry(), mergeTarget::setOwnerCountry);
        copyIfSet(mergeSelection.getModerators(), mergeTarget::setModerators);
        copyIfSet(mergeSelection.getSubscribers(), mergeTarget::setSubscribers);
        copyIfSet(mergeSelection.getRoles(), mergeTarget::setRoles);
    }

    /**
     * POJO equivalent of {@code ThriftUtils.copyField}: assign only when the source value is
     * present, so an absent field on the source leaves the target untouched.
     */
    private static <T> void copyIfSet(T sourceValue, Consumer<T> targetSetter) {
        if (sourceValue != null) {
            targetSetter.accept(sourceValue);
        }
    }

    /**
     * POJO equivalent of {@code DatabaseHandlerUtil.trimStringFields}: trim only when the value is
     * present, so an absent field stays absent rather than becoming an empty string.
     */
    private static void trimIfSet(String value, Consumer<String> setter) {
        if (value != null) {
            setter.accept(value.trim());
        }
    }

    private void mergeAttachments(Component mergeSelection, Component mergeTarget, Component mergeSource) {
        // --- handle attachments (a bit more complicated)
        // prepare for no NPE
        if (mergeSource.getAttachments() == null) {
            mergeSource.setAttachments(new HashSet<>());
        }
        if (mergeTarget.getAttachments() == null) {
            mergeTarget.setAttachments(new HashSet<>());
        }
        if (mergeSelection.getAttachments() != null) {
            Set<String> attachmentIdsSelected = mergeSelection.getAttachments().stream()
                    .map(Attachment::getAttachmentContentId).collect(Collectors.toSet());
            // add new attachments from source
            Set<Attachment> attachmentsToAdd = new HashSet<>();
            mergeSource.getAttachments().forEach(a -> {
                if (attachmentIdsSelected.contains(a.getAttachmentContentId())) {
                    attachmentsToAdd.add(a);
                }
            });
            // remove moved attachments in source
            attachmentsToAdd.forEach(a -> {
                {
                Set<Attachment> targetAttachments = new HashSet<>(nullToEmptySet(mergeTarget.getAttachments()));
                targetAttachments.add(a);
                mergeTarget.setAttachments(targetAttachments);
            }
                mergeSource.getAttachments().remove(a);
            });
            // delete unchosen attachments from target
            Set<Attachment> attachmentsToDelete = new HashSet<>();
            mergeTarget.getAttachments().forEach(a -> {
                if (!attachmentIdsSelected.contains(a.getAttachmentContentId())) {
                    attachmentsToDelete.add(a);
                }
            });
            mergeTarget.getAttachments().removeAll(attachmentsToDelete);
        }
    }

    private void transferReleases(Set<String> releaseIds, Component mergeTarget, Component mergeSource) throws SW360Exception {
        // remove releaseids from source so that they don't get deleted on deletion of
        // source component later on (releases are not part of the component in couchdb,
        // only the ids)
        mergeSource.setReleaseIds(new HashSet<>());

        // only release ids are persisted, the list of release objects are joined so
        // there is no need to update that one
        releaseIds.forEach(rid -> {
                if (mergeTarget.getReleaseIds() == null) {
                    mergeTarget.setReleaseIds(new HashSet<>());
                }
                mergeTarget.getReleaseIds().add(rid);
            });
    }

    private void updateReleasesAfterMerge(Set<String> targetComponentReleaseIds, Set<String> srcComponentReleaseIds,
                                          Component mergeSelection, Component mergeTarget, User sessionUser) throws SW360Exception {
        final String userEmail = sessionUser.getEmail();
        // Change release name if appropriate
        List<Release> targetComponentReleases = getReleasesForClearingStateSummary(targetComponentReleaseIds);
        List<Release> srcComponentReleases = getReleasesForClearingStateSummary(srcComponentReleaseIds);
        Set<String> targetComponentReleaseVersions = targetComponentReleases.stream().map(Release::getVersion)
                .collect(Collectors.toSet());
        Set<Release> releases = Stream.concat(targetComponentReleases.stream(), srcComponentReleases.stream())
                .collect(Collectors.toSet());

        List<Release> releasesToUpdate = releases.stream()
                .filter( r -> {
                    return !(r.getComponentId().equals(mergeTarget.getId()) && r.getName().equals(mergeSelection.getName()));
                }).map(r -> {
                    Release releaseBefore = ReleaseConverter.fromThrift(ReleaseConverter.toThrift(r));
                    if (srcComponentReleases.contains(r) && targetComponentReleaseVersions.contains(r.getVersion())) {
                        r.setVersion(r.getVersion() + "_conflict (" + r.getId() + ")");
                    }
                    r.setComponentId(mergeTarget.getId());
                    r.setName(mergeSelection.getName());
                    updateModifiedFields(r, userEmail);
                    dbHandlerUtil.addChangeLogs(ReleaseConverter.toThrift(r), ReleaseConverter.toThrift(releaseBefore), userEmail, Operation.UPDATE,
                            attachmentConnector, Lists.newArrayList(), mergeTarget.getId(), Operation.MERGE_COMPONENT);
                    return r;
                }).collect(Collectors.toList());
        updateReleases(releasesToUpdate, sessionUser, true);
    }

    /**
     * The {{@link #updateComponent(Component, User)} does not change the given
     * component completely according to the user request. As we want to have
     * exactly the given component as a result, this method is really submitting the
     * given data to the persistence.
     */
    private void updateComponentCompletely(Component component, User user) throws SW360Exception {
        // Prepare component for database
        prepareComponent(component);

        Component actual = componentRepository.get(component.getId());
        assertNotNull(actual, "Could not find component to update!");

        updateComponentInternal(component, actual, user);

    }

    private void updateComponentCompletelyWithoutDeletingAttachment(Component component, User user) throws SW360Exception {
        // Prepare component for database
        prepareComponent(component);
        updateModifiedFields(component, user.getEmail());
        componentRepository.update(component);

        sendMailNotificationsForComponentUpdate(component, user.getEmail());
    }

    public RequestStatus updateRelease(Release release, User user, Set<ReleaseImmutableField> immutableFields) throws SW360Exception {
        return updateRelease(release, user, immutableFields, false);
    }

    public RequestStatus updateRelease(Release release, User user, Set<ReleaseImmutableField> immutableFields, boolean forceUpdate) throws SW360Exception {
        removeLeadingTrailingWhitespace(release);
        String name = release.getName();
        String version = release.getVersion();
        if (name == null || name.isEmpty() || version == null || version.isEmpty()) {
            return RequestStatus.NAMINGERROR;
        }

        // Prepare release for database
        prepareRelease(release);

        // Get actual document for members that should no change
        Release actual = releaseRepository.get(release.getId());
        assertNotNull(actual, "Could not find release to update");

        // Block nested release linking if disabled by configuration
        if (!SW360Constants.ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP) {
            Map<String, ReleaseRelationship> newLinks = release.getReleaseIdToRelationship();
            Map<String, ReleaseRelationship> existingLinks = actual.getReleaseIdToRelationship();
            boolean hadLinks = existingLinks != null && !existingLinks.isEmpty();
            boolean hasLinks = newLinks != null && !newLinks.isEmpty();
            if (hasLinks && !hadLinks) {
                throw new SW360Exception(SW360Constants.PLEASE_ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP);
            }
            if (hasLinks && !newLinks.equals(existingLinks)) {
                throw new SW360Exception(SW360Constants.PLEASE_ENABLE_FLEXIBLE_PROJECT_RELEASE_RELATIONSHIP);
            }
        }

        ensureEccInformationIsSet(actual);
        DatabaseHandlerUtil.saveAttachmentInFileSystem(attachmentConnector, toThriftAttachments(actual.getAttachments()),
                toThriftAttachments(release.getAttachments()), user.getEmail(), release.getId());

        // Use compareTo logic to detect if there are no changes
        if (hasNoChanges(actual, release)) {
            return RequestStatus.SUCCESS;
        } else if (duplicateAttachmentExist(release)) {
            return RequestStatus.DUPLICATE_ATTACHMENT;
        } else if (changeWouldResultInDuplicate(actual, release)) {
            return RequestStatus.DUPLICATE;
        } else if (!isDependenciesExistsInRelease(release)
                || verifyLinkedPackages(CommonUtils.nullToEmptySet(actual.getPackageIds()), CommonUtils.nullToEmptySet(release.getPackageIds()), release.getId())) {
            return RequestStatus.INVALID_INPUT;
        }else if(!validSourceCodeDownloadUrl(release)){
            return RequestStatus.INVALID_SOURCE_CODE_URL;
        }else {
            DocumentPermissions<Release> permissions = makePermission(actual, user);
            boolean hasChangesInEccFields = hasChangesInEccFields(release, actual);

            if ((hasChangesInEccFields && permissions.isActionAllowed(RequestedAction.WRITE_ECC))
                    || (!hasChangesInEccFields && permissions.isActionAllowed(RequestedAction.WRITE))
                    || forceUpdate) {

                if (!hasChangesInEccFields && hasEmptyEccFields(release)) {
                    autosetEccFieldsForReleaseWithDownloadUrl(release);
                }

                copyImmutableFields(release, actual, immutableFields);

                if (hasChangesInEccFields) {
                    autosetEccUpdaterInfo(release, user);
                }

                release.setAttachments(
                        fromThriftAttachments(getAllAttachmentsToKeep(toSource(actual), toThriftAttachments(actual.getAttachments()), toThriftAttachments(release.getAttachments()))));
                autosetReleaseClearingState(release, actual);

                List<ChangeLogs> referenceDocLogList = new LinkedList<>();
                Set<Attachment> attachmentsAfter = release.getAttachments();
                Set<Attachment> attachmentsBefore = actual.getAttachments();
                DatabaseHandlerUtil.populateChangeLogsForAttachmentsDeleted(toThriftAttachments(attachmentsBefore), toThriftAttachments(attachmentsAfter),
                        referenceDocLogList, user.getEmail(), release.getId(), Operation.RELEASE_UPDATE,
                        attachmentConnector, false);

                deleteAttachmentUsagesOfUnlinkedReleases(release, actual);
                // check for MainlineState change
                setMainlineState(release, user, actual);
                if (release.getClearingState() == null) {
                    release.setClearingState(ClearingState.NEW_CLEARING);
                }

                checkSuperAttachmentExists(release);
                updateModifiedFields(release, user.getEmail());
                releaseRepository.update(release);
                String componentId=release.getComponentId();
                Component oldComponent = componentRepository.get(componentId);
                Component updatedComponent = updateReleaseDependentFieldsForComponentId(componentId, user);
                // clean up attachments in database
                Set<String> idsToBeDeleted = attachmentConnector.getAttachentContentIdsToBeDeleted(
                        toThriftAttachments(nullToEmptySet(actual.getAttachments())),
                        toThriftAttachments(nullToEmptySet(release.getAttachments())));
                Set<String> idsInUse = attachmentDatabaseHandler.getAttachmentsByIds(idsToBeDeleted).stream()
                        .map(org.eclipse.sw360.datahandler.thrift.attachments.Attachment::getAttachmentContentId)
                        .collect(Collectors.toSet());
                attachmentConnector.deleteAttachmentsByIds(idsToBeDeleted.stream().filter(id->!idsInUse.contains(id)).collect(Collectors.toSet()));
                // update linked packages
                updateLinkedPackages(CommonUtils.nullToEmptySet(actual.getPackageIds()), CommonUtils.nullToEmptySet(release.getPackageIds()), release.getId(), user);
                sendMailNotificationsForReleaseUpdate(release, user.getEmail());
                dbHandlerUtil.addChangeLogs(ReleaseConverter.toThrift(release), ReleaseConverter.toThrift(actual), user.getEmail(), Operation.UPDATE,
                        attachmentConnector, referenceDocLogList, null, null);
                dbHandlerUtil.addChangeLogs(ComponentConverter.toThrift(updatedComponent), ComponentConverter.toThrift(oldComponent), user.getEmail(), Operation.UPDATE,
                        attachmentConnector, Lists.newArrayList(), release.getId(), Operation.RELEASE_UPDATE);
                Runnable clearingRequestRunnable = addCrCommentForAttachmentUpdatesInRelease(actual, CommonUtils.nullToEmptySet(release.getAttachments()), user);
                Thread crUpdateThread = new Thread(clearingRequestRunnable);
                crUpdateThread.start();
            } else {
                if (hasChangesInEccFields) {
                    return RequestStatusConverter.fromThrift(
                            releaseModerator.updateReleaseEccInfo(ReleaseConverter.toThrift(release), user));
                } else {
                    return RequestStatusConverter.fromThrift(
                            releaseModerator.updateRelease(ReleaseConverter.toThrift(release), user));
                }
            }

            return RequestStatus.SUCCESS;
        }
    }

    /*
     * 1) Ensures that the source code URL is not an ignored URL, i.e., perform a simple regex check
     * to ensure that the URL does not contain knows/ignored URLs.
     *
     * 2) Test whether the URL exists by checking if the server responds.This is done by
     * sending a HEAD request to the URL and checking the response code.
     */
    private boolean validSourceCodeDownloadUrl(Release release) {
        String sourceCodeUrl = release.getSourceCodeDownloadurl();
        if (StringUtils.isBlank(sourceCodeUrl)) {
            return true; // If empty or null, considered valid
        }
        try {
            // Don't check if the URL contains skipped domains
            String invalidUrlPattern = SW360Utils.readConfig(SKIP_DOMAINS_FOR_VALID_SOURCE_CODE, null);
            if (StringUtils.isNotBlank(invalidUrlPattern)) {
                Pattern pattern = Pattern.compile(invalidUrlPattern);
                if (pattern.matcher(sourceCodeUrl).find()) {
                    return true;
                }
            }
        } catch (PatternSyntaxException e) {
            log.error("Invalid regex pattern for SKIP_DOMAINS_FOR_VALID_SOURCE_CODE: {}", e.getMessage(), e);
            return false; // Consider invalid if the regex is malformed
        }

        return isValidURL(sourceCodeUrl);
    }

    private boolean isValidURL(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URI(url).toURL().openConnection();
            connection.setRequestMethod("HEAD");
            connection.setInstanceFollowRedirects(true); // Ensure redirects are followed
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            return connection.getResponseCode() == HttpURLConnection.HTTP_OK;
        } catch (IOException | URISyntaxException | IllegalArgumentException e) {
            log.error("Invalid or unreachable URL: {}", url, e);
            return false;
        }
    }


    private long evaluateClearingStateForScanAvailable(Release release) {
        return nullToEmptyCollection(release.getAttachments()).stream()
                .filter(att -> att.getAttachmentType() == AttachmentType.INITIAL_SCAN_REPORT).count();
    }

    private Runnable addCrCommentForAttachmentUpdatesInRelease(Release release, Set<Attachment> updatedAttachments, User user) {
        return () -> {
            Set<Attachment> originalAttachments = CommonUtils.nullToEmptySet(release.getAttachments());
            // collect the attachment Ids
            Set<String> originalAttachmentId = originalAttachments.stream().map(Attachment::getAttachmentContentId).collect(Collectors.toSet());
            Set<String> updatedAttachmentId = updatedAttachments.stream().map(Attachment::getAttachmentContentId).collect(Collectors.toSet());

            // check if attachments are updated
            if (!Objects.equals(originalAttachmentId, updatedAttachmentId)) {
                // fetch all the projects associated with this release and collect the Clearing request Ids
                final Set<org.eclipse.sw360.datahandler.services.projects.Project> usingProjects =
                        projectRepository.searchByReleaseId(release.getId());
                final Set<String> crIds = CommonUtils.nullToEmptySet(usingProjects).stream()
                        .filter(proj -> CommonUtils.isNotNullEmptyOrWhitespace(proj.getClearingRequestId()))
                        .map(org.eclipse.sw360.datahandler.services.projects.Project::getClearingRequestId)
                        .collect(Collectors.toSet());
                if (crIds.size() > 0) {
                    Set<String> added = Sets.difference(updatedAttachmentId, originalAttachmentId);
                    Set<String> removed = Sets.difference(originalAttachmentId, updatedAttachmentId);
                    StringBuilder commentText = new StringBuilder("Attachment(s) updated for the release: <b>")
                            .append(printReleaseFullname(release)).append("</b> (").append(release.getId()).append(")");
                    if (CommonUtils.isNotEmpty(added)) {
                        Set<String> attachmentNames = extractAttachmentNameWithType(updatedAttachments, added);
                        commentText.append(System.lineSeparator()).append("Added Attachments: ").append(SW360Utils.spaceJoiner.join(attachmentNames));
                    }
                    if (CommonUtils.isNotEmpty(removed)) {
                        Set<String> attachmentNames = extractAttachmentNameWithType(originalAttachments, removed);
                        commentText.append(System.lineSeparator()).append("Removed Attachments: ").append(SW360Utils.spaceJoiner.join(attachmentNames));
                    }
                    for (String cdId : crIds) {
                        Comment comment = new Comment().setText(commentText.toString()).setCommentedBy(user.getEmail()).setAutoGenerated(true);
                        projectModerator.addCommentToClearingRequest(cdId, comment, user);
                    }
                }
            }
        };
    }

    private Set<String> extractAttachmentNameWithType(Set<Attachment> attachments, Collection<String> filterCriteria) {
        return attachments.stream().filter(att -> filterCriteria.contains(att.getAttachmentContentId()))
                .map(att -> new StringBuilder(System.lineSeparator()).append("\t").append(att.getFilename()).append(DatabaseHandlerUtil.SEPARATOR)
                        .append(att.getAttachmentType() != null ? att.getAttachmentType().name() : "").toString())
                .collect(Collectors.toSet());
    }

    private void setMainlineState(Release updated, User user, Release current) {
        boolean isMainLineStateEnabledForUser = SW360Utils.readConfig(MAINLINE_STATE_ENABLED_FOR_USER, false);
        boolean isMainlineStateDisabled = !(isMainLineStateEnabledForUser
                || PermissionUtils.isUserAtLeast(UserGroup.CLEARING_ADMIN, user));

        if ((null == current || null == current.getMainlineState()) && isMainlineStateDisabled) {
            updated.setMainlineState(MainlineState.OPEN);
        } else if (isMainlineStateDisabled) {
            updated.setMainlineState(current.getMainlineState());
        }

        if (updated.getMainlineState() == null) {
            updated.setMainlineState(MainlineState.OPEN);
        }
    }

    private boolean changeWouldResultInDuplicate(Release before, Release after) {
        if (before.getName().equals(after.getName()) && ((before.getVersion() == null && after.getVersion() == null)
                || (before.getVersion() != null && before.getVersion().equals(after.getVersion())))) {
            // sth else was changed, not one of the duplication relevant properties
            return false;
        }

        return isDuplicate(after);
    }

    private boolean duplicateAttachmentExist(Release release) {
        if (release.getAttachments() != null && !release.getAttachments().isEmpty()) {
            return AttachmentConnector.isDuplicateAttachment(toThriftAttachments(release.getAttachments()));
        }
        return false;
    }

    private void deleteAttachmentUsagesOfUnlinkedReleases(Release updated, Release actual) throws SW360Exception {
        Source usedBy = Source.releaseId(updated.getId());
        Set<String> updatedLinkedReleaseIds = nullToEmptyMap(updated.getReleaseIdToRelationship()).keySet();
        Set<String> actualLinkedReleaseIds = nullToEmptyMap(actual.getReleaseIdToRelationship()).keySet();
        deleteAttachmentUsagesOfUnlinkedReleases(usedBy, updatedLinkedReleaseIds, actualLinkedReleaseIds);
    }

    /**
     * Check if there are no changes between two Release objects using compareTo logic
     * This is a GENERALIZED method that uses Thrift reflection to automatically
     * compare ALL fields - no need to manually add new fields when they're added to Release
     *
     * @param actual The current release from database
     * @param update The updated release from the user
     * @return true if there are NO changes, false if there are changes
     */
    private boolean hasNoChanges(Release actual, Release update) {
        org.eclipse.sw360.datahandler.thrift.components.Release actualThrift = ReleaseConverter.toThrift(actual);
        org.eclipse.sw360.datahandler.thrift.components.Release updateThrift = ReleaseConverter.toThrift(update);
        SW360Utils.setVendorId(actualThrift);
        SW360Utils.setVendorId(updateThrift);

        return compareThriftObjects(actualThrift, updateThrift, org.eclipse.sw360.datahandler.thrift.components.Release._Fields.values());
    }

    /**
     * Generalized method to compare two Thrift objects field by field
     * This automatically handles ALL fields using Thrift's reflection capabilities
     *
     * @param obj1 First Thrift object
     * @param obj2 Second Thrift object
     * @param fields Array of field enums (e.g., org.eclipse.sw360.datahandler.thrift.components.Release._Fields.values())
     * @return true if objects are equal (no changes), false if different
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T extends org.apache.thrift.TBase<T, F>, F extends org.apache.thrift.TFieldIdEnum>
            boolean compareThriftObjects(T obj1, T obj2, F[] fields) {

        if (obj1 == null && obj2 == null) {
            return true;
        }
        if (obj1 == null || obj2 == null) {
            return false;
        }

        // Iterate through all fields automatically using Thrift reflection
        for (F field : fields) {
            // Get field values using Thrift's getFieldValue
            Object value1 = obj1.isSet(field) ? obj1.getFieldValue(field) : null;
            Object value2 = obj2.isSet(field) ? obj2.getFieldValue(field) : null;

            // Compare field values
            if (!areFieldValuesEqual(value1, value2, field.getFieldName())) {
                return false;
            }
        }

        // All fields are equal
        log.debug("No changes detected - all fields are equal");
        return true;
    }

    /**
     * Compare two field values with proper null handling and type-specific comparison
     * Includes detailed logging to track which fields differ
     *
     * @param value1 First value
     * @param value2 Second value
     * @param fieldName Name of field (for logging)
     * @return true if values are equal, false otherwise
     */
    private boolean areFieldValuesEqual(Object value1, Object value2, String fieldName) {
        // Both null = equal
        if (value1 == null && value2 == null) {
            return true;
        }

        // One null, one not = not equal
        if (value1 == null || value2 == null) {
            return false;
        }

        // For Comparable types, use compareTo
        if (value1 instanceof Comparable && value2 instanceof Comparable &&
            value1.getClass().equals(value2.getClass())) {
            try {
                @SuppressWarnings("unchecked")
                int comparison = ((Comparable<Object>) value1).compareTo(value2);
                return comparison == 0;
            } catch (ClassCastException e) {
                // Fall back to equals if compareTo fails
                return Objects.equals(value1, value2);
            }
        }

        // For all other types (collections, maps, objects), use equals
        return Objects.equals(value1, value2);
    }

    public boolean hasChangesInEccFields(Release release, Release actual) {
        ensureEccInformationIsSet(release);
        ensureEccInformationIsSet(actual);
        EccInformation changed = release.getEccInformation();
        EccInformation original = actual.getEccInformation();
        return !Objects.equals(changed.getAl(), original.getAl())
                || !Objects.equals(changed.getEccn(), original.getEccn())
                || !Objects.equals(changed.getEccComment(), original.getEccComment())
                || !Objects.equals(changed.getEccStatus(), original.getEccStatus())
                || !Objects.equals(changed.getContainsCryptography(), original.getContainsCryptography());
    }

    public boolean hasEmptyEccFields(Release release) {
        EccInformation eccInformation = release.getEccInformation();
        return isNullEmptyOrWhitespace(eccInformation.getAl()) &&
                isNullEmptyOrWhitespace(eccInformation.getEccn()) &&
                isNullEmptyOrWhitespace(eccInformation.getEccComment()) &&
                (eccInformation.getEccStatus() == null || eccInformation.getEccStatus() == ECCStatus.OPEN);
    }

    private void autosetEccUpdaterInfo(Release release, User user) {
        ensureEccInformationIsSet(release);
        release.getEccInformation().setAssessmentDate(SW360Utils.getCreatedOn());
        release.getEccInformation().setAssessorContactPerson(user.getEmail());
        release.getEccInformation().setAssessorDepartment(user.getDepartment());
    }

    private void autosetEccFieldsForReleaseWithDownloadUrl(Release release) {
        // For unmodified OSS, ECC classification can be done automatically
        // This release has to be an OSS component and should have a valid Url address
        Component parentComponent = componentRepository.get(release.getComponentId());
        ComponentType compType = parentComponent.getComponentType();

        String url = release.getSourceCodeDownloadurl();
        if (!isNullOrEmpty(url) && ComponentType.OSS.equals(compType)) {
            if (CommonUtils.isValidUrl(url)) {
                ensureEccInformationIsSet(release);
                EccInformation eccInfo = release.getEccInformation();
                eccInfo.setAl(ECC_AUTOSET_VALUE);
                eccInfo.setEccn(ECC_AUTOSET_VALUE);
                eccInfo.setEccComment(ECC_AUTOSET_COMMENT);
                if (SW360Utils.readConfig(AUTO_SET_ECC_STATUS, false)) {
                    eccInfo.setEccStatus(ECCStatus.APPROVED);
                }
                eccInfo.setAssessmentDate(SW360Utils.getCreatedOn());
            } else {
                log.warn("Could not set ECC options for unmodified OSS because download url is not valid: " + url);
            }
        }
    }

    private void prepareRelease(Release release) throws SW360Exception {
        if (CommonUtils.isNullEmptyOrWhitespace(release.getName())) {
            throw fail("release name cannot be empty");
        }
        if (CommonUtils.isNullEmptyOrWhitespace(release.getVersion())) {
            throw fail("release version cannot be empty");
        }
        if (CommonUtils.isNullEmptyOrWhitespace(release.getComponentId())) {
            throw fail("release componentId cannot be empty");
        }
        release.setType(SW360Constants.TYPE_RELEASE);
        if (release.getVendor() != null && release.getVendor().getId() != null) {
            release.setVendorId(release.getVendor().getId());
        }
        ensureEccInformationIsSet(release);
        release.setPermissions(null);
        release.setVendor(null);
        release.setCreatorDepartment(null);

        setSha1ForAttachments(release.getAttachments());
    }


    /**
     * Bulk update tolerates invalid entries: an entry that fails validation is logged and dropped,
     * the rest of the batch is still processed. The resulting {@link RequestSummary} reports the
     * split. Do not let the exception escape — callers such as the SBOM import and the SVM sync
     * rely on partial success.
     */
    private List<Component> prepareComponents(Collection<Component> components) {
        List<Component> result = new ArrayList<>();
        for (Component component : components) {
            try {
                prepareComponent(component);
                result.add(component);
            } catch (SW360Exception e) {
                log.error("Skipping invalid component in bulk update: {}", e.getWhy(), e);
            }
        }
        return result;
    }

    /**
     * @see #prepareComponents(Collection)
     */
    private List<Release> prepareReleases(Collection<Release> releases) {
        List<Release> result = new ArrayList<>();
        for (Release release : releases) {
            try {
                prepareRelease(release);
                result.add(release);
            } catch (SW360Exception e) {
                log.error("Skipping invalid release in bulk update: {}", e.getWhy(), e);
            }
        }
        return result;
    }


    public RequestSummary updateReleases(Collection<Release> releases, User user, boolean allowUpdate) throws SW360Exception {
        List<Release> storedReleases = prepareReleases(releases);

        RequestSummary requestSummary = new RequestSummary();
        if (allowUpdate || PermissionUtils.isAdmin(user)) {
            // Prepare component for database
            final List<DocumentResult> documentOperationResults = componentRepository.executeBulk(storedReleases);

            if (!documentOperationResults.isEmpty()) {

                final List<Component> componentList = componentRepository.get(storedReleases
                        .stream()
                        .map(Release::getComponentId)
                        .collect(Collectors.toSet()));

                final Map<String, Component> componentsById = ThriftUtils.getIdMap(componentList);

                for (Release storedRelease : storedReleases) {
                    final Component component = componentsById.get(storedRelease.getComponentId());
                    { if (component.getReleaseIds() == null) { component.setReleaseIds(new HashSet<>()); }
                component.getReleaseIds().add(storedRelease.getId()); }
                    updateReleaseDependentFieldsForComponent(component, storedRelease);
                }

                updateComponents(newHashSet(componentList), user);
            }

            requestSummary.setTotalElements(storedReleases.size());
            requestSummary.setTotalAffectedElements(storedReleases.size() - documentOperationResults.size());

            requestSummary.setRequestStatus(RequestStatus.SUCCESS);
        } else {
            requestSummary.setRequestStatus(RequestStatus.ACCESS_DENIED);
        }
        return requestSummary;
    }

    public RequestSummary updateReleasesDirectly(Set<Release> releases, User user) throws SW360Exception {
        return RequestSummaryConverter.fromThrift(
                RepositoryUtils.doBulk(prepareReleases(releases), user, releaseRepository));
    }

    public RequestStatus updateReleaseFromAdditionsAndDeletions(Release releaseAdditions, Release releaseDeletions, User user) {

        try {
            Release release = getRelease(releaseAdditions.getId(), user);
            release = ReleaseConverter.fromThrift(releaseModerator.updateReleaseFromModerationRequest(
                    ReleaseConverter.toThrift(release),
                    ReleaseConverter.toThrift(releaseAdditions),
                    ReleaseConverter.toThrift(releaseDeletions)));
            return updateRelease(release, user, ReleaseImmutableField.DEFAULT);
        } catch (SW360Exception e) {
            log.error("Could not get original release when updating from moderation request.");
            return RequestStatus.FAILURE;
        }

    }

    public Component updateReleaseDependentFieldsForComponentId(String componentId, User user) {
        Component component = componentRepository.get(componentId);
        recomputeReleaseDependentFields(component, null);
        updateModifiedFields(component, user.getEmail());
        componentRepository.update(component);

        return component;
    }

    /**
     * return false if verification is successful
     * return true if verification is failed
     * verify existence of newly linked packageIds
     * verify all newly linked packages are orphan packages
     **/
    private boolean verifyLinkedPackages(Set<String> currentPackageIds, Set<String> updatedPackageIds, String releaseId) throws SW360Exception {
        Set<String> addedPackageIds = Sets.difference(updatedPackageIds, currentPackageIds);
        if (CommonUtils.isNotEmpty(addedPackageIds)) {
            try {
                long addedCount = addedPackageIds.size();
                List<Package> addedPackages = getPackageDatabaseHandler().getPackageByIds(addedPackageIds);
                Predicate<Package> orphanReleaseFilter = pkg -> CommonUtils.isNullEmptyOrWhitespace(pkg.getReleaseId());
                Predicate<Package> linkedReleaseFilter = pkg -> releaseId.equals(pkg.getReleaseId());
                long orphanCount = addedPackages.stream().filter(orphanReleaseFilter).count();
                long linkedCount = addedPackages.stream().filter(linkedReleaseFilter).count();
                if (CommonUtils.isNotNullEmptyOrWhitespace(releaseId) && addedCount != orphanCount) {
                    return addedCount != linkedCount;
                } else {
                    return addedCount != orphanCount;
                }
            } catch (org.eclipse.sw360.datahandler.services.common.SW360Exception e) {
                log.error(String.format("An error occurred while updating linked packages of release: %s", releaseId), e.getCause());
                return true;
            }
        }
        return false;
    }

    private void updateLinkedPackages(Set<String> currentPackageIds, Set<String> updatedPackageIds, String releaseId, User user) throws SW360Exception {
        Set<String> removedPacakgeIds = Sets.difference(currentPackageIds, updatedPackageIds);
        Set<String> addedPacakgeIds = Sets.difference(updatedPackageIds, currentPackageIds);
        try {
            if (CommonUtils.isNotEmpty(removedPacakgeIds)) {
                List<Package> removedPackages = packageRepository.get(removedPacakgeIds);
                for (Package pkg : removedPackages) {
                    String relId = pkg.getReleaseId();
                    // update the package, if it contains linked release Id
                    if (CommonUtils.isNotNullEmptyOrWhitespace(relId) && releaseId.equals(relId)) {
                        pkg.setReleaseId(null);
                        org.eclipse.sw360.datahandler.services.common.RequestStatus status =
                                getPackageDatabaseHandler().updatePackage(pkg, user);
                        log.info(String.format("Unlinked package <%s> from release <%s>, Unlinking status: <%s>", pkg.getId(), releaseId, status.name()));
                    }
                }
            }
            if (CommonUtils.isNotEmpty(addedPacakgeIds)) {
                List<Package> addedPackages = getPackageDatabaseHandler().getPackageByIds(addedPacakgeIds);
                for (Package pkg : addedPackages) {
                    String relId = pkg.getReleaseId();
                    // update only orphan packages
                    if (CommonUtils.isNullEmptyOrWhitespace(relId)) {
                        pkg.setReleaseId(releaseId);
                        org.eclipse.sw360.datahandler.services.common.RequestStatus status =
                                getPackageDatabaseHandler().updatePackage(pkg, user);
                        log.info(String.format("Linked package <%s> to release <%s>, Linking status: <%s>", pkg.getId(), releaseId, status.name()));
                    } else if (!relId.equals(releaseId)) {
                        log.warn(String.format("Linked-ReleasId <%s> in Package <%s>, and Linked-PackageId <%s> in Release <%s> association is incorrect",
                                relId, pkg.getId(), pkg.getId(), releaseId));
                    }
                }
            }
        } catch (org.eclipse.sw360.datahandler.services.common.SW360Exception e) {
            log.error(String.format("An error occurred while updating linked packages of release: %s", releaseId), e.getCause());
            throw new SW360Exception(e.getMessage());
        }
    }

    public void recomputeReleaseDependentFields(Component component, String skipThisReleaseId) {
        resetReleaseDependentFields(component);

        List<Release> releases = releaseRepository.get(component.getReleaseIds());
        for (Release containedRelease : releases) {
            if (containedRelease.getId().equals(skipThisReleaseId)) continue;
            updateReleaseDependentFieldsForComponent(component, containedRelease);
        }
    }

    public BulkOperationNode deleteBulkRelease(String releaseId, User user, boolean isPreview) throws SW360Exception  {
        return bulkDeleteUtil.deleteBulkRelease(releaseId, user, isPreview);
    }

    public BulkDeleteUtil getBulkDeleteUtil() {
        return bulkDeleteUtil;
    }

    public RequestStatus mergeReleases(String mergeTargetId, String mergeSourceId, Release mergeSelection,
                                       User sessionUser) throws TException {

        Release mergeTarget = getRelease(mergeTargetId, sessionUser);
        Release mergeSource = getRelease(mergeSourceId, sessionUser);
        Release mergeTargetOriginal = ReleaseConverter.fromThrift(ReleaseConverter.toThrift(mergeTarget));
        if (!makePermission(mergeTarget, sessionUser).isActionAllowed(RequestedAction.WRITE)
                || !makePermission(mergeSource, sessionUser).isActionAllowed(RequestedAction.WRITE)
                || !makePermission(mergeSource, sessionUser).isActionAllowed(RequestedAction.DELETE)) {
            return RequestStatus.ACCESS_DENIED;
        }
        if (isReleaseUnderModeration(mergeTargetId) ||
                isReleaseUnderModeration(mergeSourceId)){
            return RequestStatus.IN_USE;
        }
        try {
            // First merge everything into the new compontent which is mergable in one step (attachments, plain fields)
            mergeReleasePlainFields(mergeSelection, mergeTarget, mergeSource);
            mergeReleaseAttachments(mergeSelection, mergeTarget, mergeSource);
            checkSuperAttachmentExists(mergeTarget);
            checkSuperAttachmentExists(mergeSource);
            // update target first. If updating source fails, no data is lost (but inconsistency might occur)
            updateReleaseCompletely(mergeTarget, sessionUser, true, true, true);
            // now, update source (before deletion so that attachments and releases and
            // stuff that has been migrated will not be deleted by component deletion!)
            updateReleaseCompletely(mergeSource, sessionUser, false, false, false);

            // updating references to source release
            // it is important to migrate the attachment usages first otherwise they will be delete during project update
            updateReleaseReferencesInAttachmentUsages(mergeTargetId, mergeSourceId);
            updateReleaseReferencesInProjects(mergeTargetId, mergeSourceId, sessionUser);
            updateReleaseReferencesInReleases(mergeTargetId, mergeSourceId, sessionUser);
            updateReleaseReferencesInVulnerabilities(mergeTargetId, mergeSourceId, sessionUser);
            updateReleaseReferencesInProjectRatings(mergeTargetId, mergeSourceId, sessionUser);
            updateReleaseReferencesInPackages(mergeTargetId, mergeSourceId, sessionUser);

            // Finally we can delete the source component
            updateParentComponent(mergeSource, sessionUser);

            RequestStatus deleteStatus = deleteRelease(mergeSourceId, sessionUser);
            if (deleteStatus != RequestStatus.SUCCESS) {
                log.error("Failed to delete source release [{}] during merge. Status: {}", mergeSourceId, deleteStatus);
                return RequestStatus.FAILURE;
            }

        } catch(Exception e) {
            log.error("Cannot merge release [" + mergeSource.getId() + "] into [" + mergeTarget.getId() + "].", e);
            return RequestStatus.FAILURE;
        }

        dbHandlerUtil.addChangeLogs(ReleaseConverter.toThrift(mergeTarget), ReleaseConverter.toThrift(mergeTargetOriginal), sessionUser.getEmail(), Operation.UPDATE,
                attachmentConnector, Lists.newArrayList(), null, Operation.MERGE_RELEASE);
        dbHandlerUtil.addChangeLogs(null, ReleaseConverter.toThrift(mergeSource), sessionUser.getEmail(), Operation.DELETE, null,
                Lists.newArrayList(), mergeTargetId, Operation.MERGE_RELEASE);
        return RequestStatus.SUCCESS;
    }

    private boolean isReleaseUnderModeration(String releaseId) throws TException {
        List<org.eclipse.sw360.datahandler.services.moderation.ModerationRequest> moderationRequests =
                ModerationClients.get().getModerationRequestByDocumentId(releaseId);
        return moderationRequests.stream().anyMatch(CommonUtils::isInProgressOrPending);
    }

    private void mergeReleasePlainFields(Release mergeSelection, Release mergeTarget, Release mergeSource) {
        // First handle the creator of the release in a way, that the discarded creator will be on the
        // moderator list afterwards. There is nothing to do, if source and target author are the same
        if(!nullToEmpty(mergeTarget.getCreatedBy()).equals(mergeSource.getCreatedBy())) {
            if(nullToEmpty(mergeSelection.getCreatedBy()).equals(nullToEmpty(mergeTarget.getCreatedBy()))) {
                // creator of the target component should be retained. Add creator of source component to list of moderators.
                mergeTarget.setModerators(mergeSelection.getModerators());
                if(!isNullOrEmpty(mergeSource.getCreatedBy())) {
                    Set<String> moderators = new HashSet<>(nullToEmptySet(mergeTarget.getModerators()));
                    moderators.add(mergeSource.getCreatedBy());
                    mergeTarget.setModerators(moderators);
                }
            } else {
                // creator of the source component has been selected. Add creator of target component to list of moderators.
                mergeTarget.setModerators(mergeSelection.getModerators());
                if(!isNullOrEmpty(mergeTarget.getCreatedBy())) {
                    Set<String> moderators = new HashSet<>(nullToEmptySet(mergeTarget.getModerators()));
                    moderators.add(mergeTarget.getCreatedBy());
                    mergeTarget.setModerators(moderators);
                }
            }
        }

        // Handle default fields
        copyIfSet(mergeSelection.getVendorId(), mergeTarget::setVendorId);
        copyIfSet(mergeSelection.getName(), mergeTarget::setName);
        copyIfSet(mergeSelection.getVersion(), mergeTarget::setVersion);
        copyIfSet(mergeSelection.getLanguages(), mergeTarget::setLanguages);
        copyIfSet(mergeSelection.getOperatingSystems(), mergeTarget::setOperatingSystems);
        copyIfSet(mergeSelection.getCpeid(), mergeTarget::setCpeid);
        copyIfSet(mergeSelection.getSoftwarePlatforms(), mergeTarget::setSoftwarePlatforms);
        copyIfSet(mergeSelection.getReleaseDate(), mergeTarget::setReleaseDate);
        copyIfSet(mergeSelection.getMainLicenseIds(), mergeTarget::setMainLicenseIds);
        copyIfSet(mergeSelection.getSourceCodeDownloadurl(), mergeTarget::setSourceCodeDownloadurl);
        copyIfSet(mergeSelection.getBinaryDownloadurl(), mergeTarget::setBinaryDownloadurl);
        copyIfSet(mergeSelection.getMainlineState(), mergeTarget::setMainlineState);
        copyIfSet(mergeSelection.getCreatedOn(), mergeTarget::setCreatedOn);
        copyIfSet(mergeSelection.getCreatedBy(), mergeTarget::setCreatedBy);
        copyIfSet(mergeSelection.getContributors(), mergeTarget::setContributors);
        copyIfSet(mergeSelection.getModerators(), mergeTarget::setModerators);
        copyIfSet(mergeSelection.getSubscribers(), mergeTarget::setSubscribers);
        copyIfSet(mergeSelection.getRepository(), mergeTarget::setRepository);
        copyIfSet(mergeSelection.getRoles(), mergeTarget::setRoles);
        copyIfSet(mergeSelection.getExternalIds(), mergeTarget::setExternalIds);
        copyIfSet(mergeSelection.getAdditionalData(), mergeTarget::setAdditionalData);
        // defensive copy: the self link is removed from the target's map below
        copyIfSet(mergeSelection.getReleaseIdToRelationship(),
                relationships -> mergeTarget.setReleaseIdToRelationship(new HashMap<>(relationships)));

        // Remove self links
        if((mergeTarget.getReleaseIdToRelationship() != null)) {
            mergeTarget.getReleaseIdToRelationship().remove(mergeTarget.getId());
        }

        // Handle clearing information
        ClearingInformation selectedClearing = mergeSelection.getClearingInformation();
        ClearingInformation targetClearing = mergeTarget.getClearingInformation();
        if (selectedClearing != null && targetClearing != null) {
            copyIfSet(selectedClearing.getBinariesOriginalFromCommunity(), targetClearing::setBinariesOriginalFromCommunity);
            copyIfSet(selectedClearing.getBinariesSelfMade(), targetClearing::setBinariesSelfMade);
            copyIfSet(selectedClearing.getComponentLicenseInformation(), targetClearing::setComponentLicenseInformation);
            copyIfSet(selectedClearing.getSourceCodeDelivery(), targetClearing::setSourceCodeDelivery);
            copyIfSet(selectedClearing.getSourceCodeOriginalFromCommunity(), targetClearing::setSourceCodeOriginalFromCommunity);
            copyIfSet(selectedClearing.getSourceCodeToolMade(), targetClearing::setSourceCodeToolMade);
            copyIfSet(selectedClearing.getSourceCodeSelfMade(), targetClearing::setSourceCodeSelfMade);
            copyIfSet(selectedClearing.getScreenshotOfWebSite(), targetClearing::setScreenshotOfWebSite);
            copyIfSet(selectedClearing.getFinalizedLicenseScanReport(), targetClearing::setFinalizedLicenseScanReport);
            copyIfSet(selectedClearing.getLicenseScanReportResult(), targetClearing::setLicenseScanReportResult);
            copyIfSet(selectedClearing.getLegalEvaluation(), targetClearing::setLegalEvaluation);
            copyIfSet(selectedClearing.getLicenseAgreement(), targetClearing::setLicenseAgreement);
            copyIfSet(selectedClearing.getScanned(), targetClearing::setScanned);
            copyIfSet(selectedClearing.getComponentClearingReport(), targetClearing::setComponentClearingReport);
            copyIfSet(selectedClearing.getClearingStandard(), targetClearing::setClearingStandard);
            copyIfSet(selectedClearing.getExternalUrl(), targetClearing::setExternalUrl);
            copyIfSet(selectedClearing.getComment(), targetClearing::setComment);
            copyIfSet(selectedClearing.getRequestID(), targetClearing::setRequestID);
            copyIfSet(selectedClearing.getAdditionalRequestInfo(), targetClearing::setAdditionalRequestInfo);
            copyIfSet(selectedClearing.getProcStart(), targetClearing::setProcStart);
            copyIfSet(selectedClearing.getEvaluated(), targetClearing::setEvaluated);
            copyIfSet(selectedClearing.getExternalSupplierID(), targetClearing::setExternalSupplierID);
            copyIfSet(selectedClearing.getCountOfSecurityVn(), targetClearing::setCountOfSecurityVn);
        }

        // Handle ECC information
        EccInformation selectedEcc = mergeSelection.getEccInformation();
        EccInformation targetEcc = mergeTarget.getEccInformation();
        if (selectedEcc != null && targetEcc != null) {
            copyIfSet(selectedEcc.getEccStatus(), targetEcc::setEccStatus);
            copyIfSet(selectedEcc.getEccComment(), targetEcc::setEccComment);
            copyIfSet(selectedEcc.getAl(), targetEcc::setAl);
            copyIfSet(selectedEcc.getEccn(), targetEcc::setEccn);
            copyIfSet(selectedEcc.getMaterialIndexNumber(), targetEcc::setMaterialIndexNumber);
            copyIfSet(selectedEcc.getAssessorContactPerson(), targetEcc::setAssessorContactPerson);
            copyIfSet(selectedEcc.getAssessorDepartment(), targetEcc::setAssessorDepartment);
            copyIfSet(selectedEcc.getAssessmentDate(), targetEcc::setAssessmentDate);
        }

        // Handle COTS information
        COTSDetails selectedCots = mergeSelection.getCotsDetails();
        COTSDetails targetCots = mergeTarget.getCotsDetails();
        if (selectedCots != null && targetCots != null) {
            copyIfSet(selectedCots.getUsageRightAvailable(), targetCots::setUsageRightAvailable);
            copyIfSet(selectedCots.getCotsResponsible(), targetCots::setCotsResponsible);
            copyIfSet(selectedCots.getClearingDeadline(), targetCots::setClearingDeadline);
            copyIfSet(selectedCots.getLicenseClearingReportURL(), targetCots::setLicenseClearingReportURL);
            copyIfSet(selectedCots.getUsedLicense(), targetCots::setUsedLicense);
            copyIfSet(selectedCots.getContainsOSS(), targetCots::setContainsOSS);
            copyIfSet(selectedCots.getOssContractSigned(), targetCots::setOssContractSigned);
            copyIfSet(selectedCots.getOssInformationURL(), targetCots::setOssInformationURL);
            copyIfSet(selectedCots.getSourceCodeAvailable(), targetCots::setSourceCodeAvailable);
        }
    }

    private void mergeReleaseAttachments(Release mergeSelection, Release mergeTarget, Release mergeSource) {
        // --- handle attachments (a bit more complicated)
        // prepare for no NPE
        if (mergeSource.getAttachments() == null) {
            mergeSource.setAttachments(new HashSet<>());
        }
        if (mergeTarget.getAttachments() == null) {
            mergeTarget.setAttachments(new HashSet<>());
        }

        Set<String> attachmentIdsSelected = mergeSelection.getAttachments().stream()
                .map(Attachment::getAttachmentContentId).collect(Collectors.toSet());
        // add new attachments from source
        Set<Attachment> attachmentsToAdd = new HashSet<>();
        mergeSource.getAttachments().forEach(a -> {
            if (attachmentIdsSelected.contains(a.getAttachmentContentId())) {
                attachmentsToAdd.add(a);
            }
        });
        // remove moved attachments in source
        attachmentsToAdd.forEach(a -> {
            {
                Set<Attachment> targetAttachments = new HashSet<>(nullToEmptySet(mergeTarget.getAttachments()));
                targetAttachments.add(a);
                mergeTarget.setAttachments(targetAttachments);
            }
            mergeSource.getAttachments().remove(a);
        });
        // delete unchosen attachments from target
        Set<Attachment> attachmentsToDelete = new HashSet<>();
        mergeTarget.getAttachments().forEach(a -> {
            if (!attachmentIdsSelected.contains(a.getAttachmentContentId())) {
                attachmentsToDelete.add(a);
            }
        });
        mergeTarget.getAttachments().removeAll(attachmentsToDelete);
    }


    /**
     * The {{@link #updateRelease(Component, User, Iterable)} does not change the given
     * release completely according to the user request. As we want to have
     * exactly the given release as a result, this method is really submitting the
     * given data to the persistence.
     */
    private void updateReleaseCompletely(Release release, User user, boolean updateClearingState, boolean cleanup, boolean sendmail) throws SW360Exception {
        // Prepare component for database
        prepareRelease(release);

        Release actual = releaseRepository.get(release.getId());
        assertNotNull(actual, "Could not find release to update!");

        // Update the database with the release
        if(updateClearingState) {
            autosetReleaseClearingState(release, actual);
        }
        updateModifiedFields(release, user.getEmail());
        releaseRepository.update(release);

        //clean up attachments in database
        if(cleanup) {
            attachmentConnector.deleteAttachmentDifference(toThriftAttachments(actual.getAttachments()), toThriftAttachments(release.getAttachments()));
        }
        if(sendmail) {
            sendMailNotificationsForReleaseUpdate(release, user.getEmail());
        }
    }

    private void updateReleaseReferencesInProjects(String mergeTargetId, String mergeSourceId, User sessionUser) throws TException {
        ProjectDatabaseHandler projectDbHandler = getProjectDatabaseHandlerForVuln();

        final String userEmail = sessionUser.getEmail();
        Set<org.eclipse.sw360.datahandler.services.projects.Project> pojoProjects =
                projectRepository.searchByReleaseId(mergeSourceId);
        for (org.eclipse.sw360.datahandler.services.projects.Project project : pojoProjects) {
            // retrieve full document, other method only retrieves summary
            project = projectDbHandler.getProjectByIdIgnoringVisibility(project.getId());
            org.eclipse.sw360.datahandler.services.projects.Project projectBefore =
                    ProjectConverter.fromThrift(ProjectConverter.toThrift(project));
            org.eclipse.sw360.datahandler.services.common.ProjectReleaseRelationship relationship =
                    project.getReleaseIdToUsage().remove(mergeSourceId);
            // if the target release is also linked, keep this one, do not overwrite
            if (!project.getReleaseIdToUsage().containsKey(mergeTargetId)) {
                project.getReleaseIdToUsage().put(mergeTargetId, relationship);
            }
            project.setModifiedBy(userEmail);
            project.setModifiedOn(SW360Utils.getCreatedOn());
            projectDbHandler.updateProject(project, sessionUser, true);

            dbHandlerUtil.addChangeLogs(ProjectConverter.toThrift(project), ProjectConverter.toThrift(projectBefore),
                    userEmail, Operation.UPDATE,
                    attachmentConnector, Lists.newArrayList(), mergeTargetId, Operation.MERGE_RELEASE);
        }
    }

    private void updateReleaseReferencesInAttachmentUsages(String mergeTargetId, String mergeSourceId) throws TException {
        List<AttachmentUsage> usages = attachmentDatabaseHandler.getAttachmentUsagesByReleaseId(mergeSourceId);
        for(AttachmentUsage usage : usages) {
            if(usage.getOwner().isSetReleaseId() && usage.getOwner().getReleaseId().equals(mergeSourceId)) {
                usage.getOwner().setReleaseId(mergeTargetId);
            }
            if(usage.getUsedBy().isSetReleaseId() && usage.getUsedBy().getReleaseId().equals(mergeSourceId)) {
                usage.getUsedBy().setReleaseId(mergeTargetId);
            }
            attachmentDatabaseHandler.updateAttachmentUsage(usage);
        }
    }

    private void updateReleaseReferencesInReleases(String mergeTargetId, String mergeSourceId, User sessionUser) throws SW360Exception {
        List<Release> releases = getReferencingReleases(mergeSourceId);
        for(Release release : releases) {
            Release releaseBefore =ReleaseConverter.fromThrift(ReleaseConverter.toThrift(release));
            ReleaseRelationship relationship = release.getReleaseIdToRelationship().remove(mergeSourceId);
            // if the target release is also linked, keep this one, do not overwrite
            if(!release.getReleaseIdToRelationship().containsKey(mergeTargetId)) {
                release.getReleaseIdToRelationship().put(mergeTargetId, relationship);
            }
            updateReleaseCompletely(release, sessionUser, false, false, false);
            dbHandlerUtil.addChangeLogs(ReleaseConverter.toThrift(release), ReleaseConverter.toThrift(releaseBefore), sessionUser.getEmail(), Operation.UPDATE,
                    attachmentConnector, Lists.newArrayList(), mergeTargetId, Operation.MERGE_RELEASE);
        }
    }

    private void updateReleaseReferencesInVulnerabilities(String mergeTargetId, String mergeSourceId, User sessionUser) throws TException {
        VulnerabilityDatabaseHandler vulnerabilityHandler = getVulnerabilityDatabaseHandler();

        List<ReleaseVulnerabilityRelation> relations = vulnerabilityHandler.getReleaseVulnerabilityRelationsByReleaseId(mergeSourceId);
        for(ReleaseVulnerabilityRelation relation : relations) {
            if((relation.getReleaseId() != null) && relation.getReleaseId().equals(mergeSourceId)) {
                ReleaseVulnerabilityRelation relationBefore = relation.deepCopy();
                relation.setReleaseId(mergeTargetId);
                vulnerabilityHandler.update(relation);
                dbHandlerUtil.addChangeLogs(relation, relationBefore, sessionUser.getEmail(), Operation.UPDATE,
                        attachmentConnector, Lists.newArrayList(), mergeTargetId, Operation.MERGE_RELEASE);
            }
        }
    }

    private void updateReleaseReferencesInProjectRatings(String mergeTargetId, String mergeSourceId, User sessionUser) throws TException {
        ProjectDatabaseHandler projectHandler = getProjectDatabaseHandlerForVuln();

        List<ProjectVulnerabilityRating> ratings = projectHandler.getProjectVulnerabilityRatingsByReleaseId(mergeSourceId);
        for(ProjectVulnerabilityRating rating : ratings) {
            ProjectVulnerabilityRating ratingBefore = rating.deepCopy();
            for(Map<String, List<VulnerabilityCheckStatus>> map : rating.getVulnerabilityIdToReleaseIdToStatus().values()) {
                List<VulnerabilityCheckStatus> list = map.remove(mergeSourceId);
                // if the target release is also linked, keep this one, do not overwrite
                if(list != null && !map.containsKey(mergeTargetId)) {
                    map.put(mergeTargetId, list);
                }
            }
            projectHandler.updateProjectVulnerabilityRating(rating);
            dbHandlerUtil.addChangeLogs(rating, ratingBefore, sessionUser.getEmail(), Operation.UPDATE,
                    attachmentConnector, Lists.newArrayList(), mergeTargetId, Operation.MERGE_RELEASE);
        }
    }

    private VulnerabilityDatabaseHandler getVulnerabilityDatabaseHandler() {
        try {
            if (vulnerabilityDatabaseHandler == null) {
                vulnerabilityDatabaseHandler = new VulnerabilityDatabaseHandler();
            }
            return vulnerabilityDatabaseHandler;
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private ProjectDatabaseHandler getProjectDatabaseHandlerForVuln() {
        try {
            if (projectDatabaseHandlerForVuln == null) {
                projectDatabaseHandlerForVuln = new ProjectDatabaseHandler(
                        DatabaseSettings.getConfiguredClient(),
                        DatabaseSettings.COUCH_DB_DATABASE,
                        DatabaseSettings.COUCH_DB_ATTACHMENTS);
            }
            return projectDatabaseHandlerForVuln;
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateReleaseReferencesInPackages(String mergeTargetId, String mergeSourceId, User sessionUser) throws TException {
        Set<Package> packages = getPackageDatabaseHandler().getPackagesByReleaseId(mergeSourceId);
        Release mergeTarget = releaseRepository.get(mergeTargetId);
        for (Package pkg : packages) {
            org.eclipse.sw360.datahandler.thrift.packages.Package packageBefore = PackageConverter.toThrift(pkg);
            pkg.setReleaseId(mergeTargetId);
            getPackageDatabaseHandler().updatePackage(pkg, sessionUser);
            Set<String> packageIds = mergeTarget.getPackageIds();
            if (packageIds == null) {
                packageIds = new HashSet<>();
                mergeTarget.setPackageIds(packageIds);
            }
            packageIds.add(pkg.getId());
            dbHandlerUtil.addChangeLogs(PackageConverter.toThrift(pkg), packageBefore, sessionUser.getEmail(),
                    Operation.UPDATE, attachmentConnector, Lists.newArrayList(), mergeTargetId, Operation.MERGE_RELEASE);
        }
        // Update the merge target release with the migrated package IDs
        if (!packages.isEmpty()) {
            releaseRepository.update(mergeTarget);
        }

        // Remove package IDs from source release so deleteRelease does not block
        Release mergeSource = releaseRepository.get(mergeSourceId);
        if ((mergeSource.getPackageIds() != null)) {
            mergeSource.getPackageIds().clear();
            releaseRepository.update(mergeSource);
        }
    }

    private void updateParentComponent(Release release, User sessionUser) throws SW360Exception {
        Component component = getComponent(release.getComponentId(), sessionUser);
        Component componentBefore = ComponentConverter.fromThrift(ComponentConverter.toThrift(component));
        Set<String> releaseIds = nullToEmptyList(component.getReleases()).stream().map(Release::getId).collect(Collectors.toSet());
        releaseIds.remove(release.getId());
        component.setReleaseIds(releaseIds);

        recomputeReleaseDependentFields(component, null);
        updateComponentCompletely(component, sessionUser);

        dbHandlerUtil.addChangeLogs(ComponentConverter.toThrift(component), ComponentConverter.toThrift(componentBefore), sessionUser.getEmail(), Operation.UPDATE,
                attachmentConnector, Lists.newArrayList(), release.getId(), Operation.MERGE_RELEASE);
    }

    ///////////////////////////////
    // DELETE INDIVIDUAL OBJECTS //
    ///////////////////////////////
    public RequestStatus deleteComponent(String id, User user) throws SW360Exception {
        return deleteComponent(id, user, false);
    }

    public RequestStatus deleteComponent(String id, User user, boolean forceDelete) throws SW360Exception {
        Component component = new Component();
        try {
            component = componentRepository.get(id);
            assertNotNull(component);
        } catch (Exception e) {
            return RequestStatus.INVALID_INPUT;
        }

        final Set<String> releaseIds = component.getReleaseIds();
        if (!forceDelete && checkIfInUse(releaseIds)) return RequestStatus.IN_USE;


        if (makePermission(component, user).isActionAllowed(RequestedAction.DELETE) || forceDelete) {

            for (Release release : releaseRepository.get(nullToEmptySet(component.getReleaseIds()))) {
                component = removeReleaseAndCleanUp(release, user);
            }

            // Remove the component with attachments
            attachmentConnector.deleteAttachments(toThriftAttachments(component.getAttachments()));
            attachmentDatabaseHandler.deleteUsagesBy(Source.componentId(id));
            componentRepository.remove(component);
            moderator.notifyModeratorOnDelete(id);
            dbHandlerUtil.addChangeLogs(null, ComponentConverter.toThrift(component), user.getEmail(), Operation.DELETE, attachmentConnector,
                    Lists.newArrayList(), null, null);
            return RequestStatus.SUCCESS;
        } else {
            return RequestStatusConverter.fromThrift(
                    moderator.deleteComponent(ComponentConverter.toThrift(component), user));
        }
    }

    public boolean checkIfInUseComponent(String componentId) {
        Component component = componentRepository.get(componentId);
        return checkIfInUse(component);
    }

    public boolean checkIfInUse(Component component) {
        return checkIfInUse(component.getReleaseIds());
    }

    public boolean checkIfInUse(Set<String> releaseIds) {
        if (CommonUtils.isNullOrEmptyCollection(releaseIds)) {
            return false;
        }
        for (String releaseId : releaseIds) {
            if (checkIfInUse(releaseId)) {
                return true;
            }
        }
        return false;
    }

    public boolean checkIfInUse(String releaseId) {

        final Set<Component> usingComponents = componentRepository.getUsingComponents(releaseId);
        if (!CommonUtils.isNullOrEmptyCollection(usingComponents))
            return true;

        final Set<org.eclipse.sw360.datahandler.services.projects.Project> usingProjects =
                projectRepository.searchByReleaseId(releaseId);
        return !CommonUtils.isNullOrEmptyCollection(usingProjects);
    }

    private Component removeReleaseAndCleanUp(Release release, User user) throws SW360Exception {
        attachmentConnector.deleteAttachments(toThriftAttachments(release.getAttachments()));
        attachmentDatabaseHandler.deleteUsagesBy(Source.releaseId(release.getId()));

        Component component = updateReleaseDependentFieldsForComponentId(release.getComponentId(), user);

        //TODO notify using projects!?? Or stop if there are any

        moderator.notifyModeratorOnDelete(release.getId());
        releaseRepository.remove(release);

        return component;
    }

    public RequestStatus deleteRelease(String id, User user) throws SW360Exception {
        return deleteRelease(id, user, false);
    }

    public RequestStatus deleteRelease(String id, User user, boolean forceDelete) throws SW360Exception {
        Release release = releaseRepository.get(id);
        assertNotNull(release);

        if (!nullToEmptySet(release.getPackageIds()).isEmpty() || checkIfInUse(id)) return RequestStatus.IN_USE;

        if (makePermission(release, user).isActionAllowed(RequestedAction.DELETE) || forceDelete) {
            Component componentBefore = componentRepository.get(release.getComponentId());
            // Remove release id from component
            removeReleaseId(id, release.getComponentId());
            // Remove spdx if exist
            String spdxId = release.getSpdxId();
            if (CommonUtils.isNotNullEmptyOrWhitespace(spdxId)) {
                spdxDocumentDatabaseHandler.deleteSPDXDocument(spdxId, user);
                release = releaseRepository.get(id);
            }
            Component componentAfter = removeReleaseAndCleanUp(release, user);
            dbHandlerUtil.addChangeLogs(null, ReleaseConverter.toThrift(release), user.getEmail(), Operation.DELETE, attachmentConnector,
                    Lists.newArrayList(), null, null);
            dbHandlerUtil.addChangeLogs(ComponentConverter.toThrift(componentAfter), ComponentConverter.toThrift(componentBefore), user.getEmail(), Operation.UPDATE,
                    attachmentConnector, Lists.newArrayList(), release.getId(), Operation.RELEASE_DELETE);
            return RequestStatus.SUCCESS;
        } else {
            return RequestStatusConverter.fromThrift(
                    releaseModerator.deleteRelease(ReleaseConverter.toThrift(release), user));
        }
    }

    private void removeReleaseId(String releaseId, String componentId) throws SW360Exception {
        // Remove release id from component
        Component component = componentRepository.get(componentId);
        assertNotNull(component);
        recomputeReleaseDependentFields(component, releaseId);
        component.getReleaseIds().remove(releaseId);
        componentRepository.update(component);
    }

    /////////////////////
    // HELPER SERVICES //
    /////////////////////

    List<ReleaseLink> getLinkedReleases(org.eclipse.sw360.datahandler.services.projects.Project project, Deque<String> visitedIds) {
        return getLinkedReleases(project.getReleaseIdToUsage(), visitedIds);
    }

    List<ReleaseLink> getLinkedReleasesWithAccessibility(org.eclipse.sw360.datahandler.services.projects.Project project, Deque<String> visitedIds, User user) {
        List<ReleaseLink> releaseLinkList = getLinkedReleases(project.getReleaseIdToUsage(), visitedIds);
        if (!CommonUtils.isNullOrEmptyCollection(releaseLinkList)) {
            for (ReleaseLink releaseLink : releaseLinkList) {
                Release release = releaseRepository.get(releaseLink.getId());
                releaseLink.setAccessible(isReleaseActionAllowed(release, user, RequestedAction.READ));
            }
        }
        return releaseLinkList;
    }

    private List<ReleaseLink> getLinkedReleases(Map<String, ?> relations, Deque<String> visitedIds) {
        return iterateReleaseRelationShips(relations, null, visitedIds);
    }

    public List<ReleaseLink> getLinkedReleases(Map<String, ?> relations) {
        return getLinkedReleases(relations, new ArrayDeque<>());
    }

    public List<ReleaseLink> getLinkedReleasesWithAccessibility(Map<String, ?> relations, User user) {
        List<ReleaseLink> releaseLinkList = getLinkedReleases(relations, new ArrayDeque<>());
        if (!CommonUtils.isNullOrEmptyCollection(releaseLinkList)) {
            for (ReleaseLink releaseLink : releaseLinkList) {
                Release release = releaseRepository.get(releaseLink.getId());
                releaseLink.setAccessible(isReleaseActionAllowed(release, user, RequestedAction.READ));
            }
        }
        return releaseLinkList;
    }

    public boolean isReleaseActionAllowed(Release release, User user, RequestedAction action) {
        boolean isAllowed = false;
        switch (action) {
            case READ:
                boolean isComponentAccessible = false;
                String componentId = release.getComponentId();
                if (CommonUtils.isNotNullEmptyOrWhitespace(componentId)) {
                    Component component = componentRepository.get(componentId);
                    isComponentAccessible = makePermission(component, user).isActionAllowed(RequestedAction.READ);
                }
                isAllowed = isComponentAccessible && makePermission(release, user).isActionAllowed(RequestedAction.READ);
                break;

            default:
                isAllowed = makePermission(release, user).isActionAllowed(action);
                break;
        }
        return isAllowed;
    }

    public List<Release> getAllReleases() {
        return releaseRepository.getAll();
    }

    public List<Vendor> getAllVendors() {
        return vendorRepository.getAll();
    }

    public Map<String, Release> getAllReleasesIdMap() {
        final List<Release> releases = getAllReleases();
        return ThriftUtils.getIdMap(releases);
    }

    void fillVendors(Collection<Release> releases){
        releases.forEach(vendorRepository::fillVendor);
    }

    public Map<String, Component> getAllComponentsIdMap() {
        final List<Component> components = componentRepository.getAll();
        return ThriftUtils.getIdMap(components);
    }

    public List<Component> getAllComponentsWithVCS() {
        final List<Component> componentsWithVCS = componentRepository.getComponentsByVCS();
        final List<Component> components = new ArrayList<>();
        for (Component component : componentsWithVCS) {
            if (!CommonUtils.isNullOrEmptyCollection(component.getReleaseIds())) {
                components.add(component);
            }
        }
        return components;
    }

    @NotNull
    private List<ReleaseLink> iterateReleaseRelationShips(Map<String, ?> relations, String parentNodeId, Deque<String> visitedIds) {
        List<ReleaseLink> out = new ArrayList<>();

        for (Map.Entry<String, ?> entry : relations.entrySet()) {
            String id = entry.getKey();
            Optional<ReleaseLink> releaseLinkOptional = getFilledReleaseLink(id, entry.getValue(), parentNodeId, visitedIds);
            releaseLinkOptional.ifPresent(out::add);
        }
        out.sort(SW360Utils.RELEASE_LINK_COMPARATOR);
        return out;
    }

    private Optional<ReleaseLink> getFilledReleaseLink(String id, Object relation, String parentNodeId, Deque<String> visitedIds) {
        ReleaseLink releaseLink = null;
        if (!visitedIds.contains(id)) {
            visitedIds.push(id);
            Release release = releaseRepository.get(id);
            if (release != null) {
                releaseLink = createReleaseLink(release);
                fillValueFieldInReleaseLink(releaseLink, relation);
                releaseLink.setNodeId(generateNodeId(id));
                releaseLink.setParentNodeId(parentNodeId);
                if ((release.getMainLicenseIds() != null)) {
                    releaseLink.setLicenseIds(release.getMainLicenseIds());
                }
                if ((release.getOtherLicenseIds() != null)) {
                    releaseLink.setOtherLicenseIds(release.getOtherLicenseIds());
                }
            } else {
                log.error("Broken ReleaseLink in release with id: " + parentNodeId + ". Linked release with id " + id + " was not in the release cache");
            }
            visitedIds.pop();
        }
        return Optional.ofNullable(releaseLink);
    }


    private void fillValueFieldInReleaseLink(ReleaseLink releaseLink, Object relation) {
        if (relation instanceof org.eclipse.sw360.datahandler.services.common.ProjectReleaseRelationship pojoRel) {
            releaseLink.setReleaseRelationship(org.eclipse.sw360.common.utils.converter.common.EnumConverter.toThrift(
                    pojoRel.getReleaseRelation(), org.eclipse.sw360.datahandler.thrift.ReleaseRelationship.class));
            releaseLink.setMainlineState(org.eclipse.sw360.common.utils.converter.common.EnumConverter.toThrift(
                    pojoRel.getMainlineState(), org.eclipse.sw360.datahandler.thrift.MainlineState.class));
            releaseLink.setComment(pojoRel.getComment());
        } else if (relation instanceof ProjectReleaseRelationship) {
            ProjectReleaseRelationship rel = (ProjectReleaseRelationship) relation;
            releaseLink.setReleaseRelationship(rel.getReleaseRelation());
            releaseLink.setMainlineState(rel.getMainlineState());
            releaseLink.setComment(rel.getComment());
        } else if (relation instanceof org.eclipse.sw360.datahandler.services.common.ReleaseRelationship pojoReleaseRelation) {
            releaseLink.setReleaseRelationship(org.eclipse.sw360.common.utils.converter.common.EnumConverter.toThrift(
                    pojoReleaseRelation, org.eclipse.sw360.datahandler.thrift.ReleaseRelationship.class));
        } else if (relation instanceof org.eclipse.sw360.datahandler.thrift.ReleaseRelationship) {
            releaseLink.setReleaseRelationship((org.eclipse.sw360.datahandler.thrift.ReleaseRelationship) relation);
        } else {
            throw new IllegalArgumentException("Only ProjectReleaseRelationship or ReleaseRelationship is allowed as ReleaseLink's relation value");
        }
    }

    @NotNull
    private ReleaseLink createReleaseLink(Release release) {
        vendorRepository.fillVendor(release);
        String vendorName = (release.getVendor() != null) ? release.getVendor().getShortname() : "";
        ReleaseLink releaseLink = new ReleaseLink(release.getId(), vendorName, release.getName(), release.getVersion(), printReleaseFullname(release),
                !nullToEmptyMap(release.getReleaseIdToRelationship()).isEmpty());
        releaseLink
                .setClearingState(ClearingStateConverter.toThrift(release.getClearingState()))
                .setComponentType(
                        Optional.ofNullable(componentRepository.get(release.getComponentId()))
                                .map(Component::getComponentType)
                                .map(ComponentTypeConverter::toThrift)
                                .orElse(null));
        if (!nullToEmptySet(release.getAttachments()).isEmpty()) {
            releaseLink.setAttachments(release.getAttachments().stream()
                    .map(org.eclipse.sw360.common.utils.converter.attachments.AttachmentConverter::toThrift)
                    .collect(Collectors.toList()));
        }
        return releaseLink;
    }

    private String generateNodeId(String id) {
        return id == null ? null : id + "_" + UUID.randomUUID();
    }

    public List<Release> searchReleaseByNamePrefix(String name) {
        return releaseRepository.searchByNamePrefix(name);
    }

    public Map<PaginationData, List<Release>> searchReleaseByNamePaginated(String name, PaginationData pageData) {
        return releaseRepository.searchReleaseByNamePaginated(name, pageData);
    }

    public Map<PaginationData, List<Release>> searchAccessibleReleasesByText(ReleaseSearchHandler searchHandler, String searchText, User user, PaginationData pageData) {
        Map<PaginationData, List<Release>> searchResult = searchHandler.search(searchText, pageData);
        PaginationData respPageData = searchResult.keySet().iterator().next();
        List<Release> releaseList = searchResult.values().iterator().next();
        return Collections.singletonMap(respPageData, getAccessibleReleaseList(releaseList, user));
    }

    public Map<PaginationData, List<Release>> getAccessibleNewReleasesWithSrc(User user, PaginationData pageData) {
        Map<PaginationData, List<Release>> searchResult = releaseRepository.getAccessibleNewReleasesWithSrc(pageData);
        PaginationData respPageData = searchResult.keySet().iterator().next();
        List<Release> releaseList = searchResult.values().iterator().next();
        return Collections.singletonMap(respPageData, getAccessibleReleaseList(releaseList, user));
    }

    public List<Release> getReleases(Set<String> ids) {
        return releaseRepository.makeSummary(SummaryType.SHORT, ids);
    }

    // return release directly from db, without making summary.
    public List<Release> getReleasesByIds(Set<String> ids) {
        return CommonUtils.isNullOrEmptyCollection(ids) ? Lists.newArrayList() : releaseRepository.get(ids);
    }

    // return components directly from db, without making summary.
    public List<Component> getComponentsByIds(Set<String> ids) {
        return CommonUtils.isNullOrEmptyCollection(ids) ? Lists.newArrayList() : componentRepository.get(ids);
    }

    public List<Release> getAccessibleReleases(Set<String> ids, User user) {
        return getAccessibleReleaseList(releaseRepository.makeSummary(SummaryType.SHORT, ids), user);
    }

    public Map<PaginationData, List<Release>> getAccessibleReleasesWithPagination(User user, PaginationData pageData) throws TException {
        return releaseRepository.getAccessibleReleasesWithPagination(user, pageData);
    }

    private List<Release> getAccessibleReleaseList(List<Release> releaseList, User user) {
        List<Release> resultList = new ArrayList<Release>();
        for (Release release : releaseList) {
            if (isReleaseActionAllowed(release, user, RequestedAction.READ)) {
                resultList.add(release);
            }
        }
        return resultList;
    }

    public Set<Component> searchComponentsByExternalIds(Map<String, Set<String>> externalIds) {
        return componentRepository.searchByExternalIds(externalIds);
    }

    public Set<Release> searchReleasesByExternalIds(Map<String, Set<String>> externalIds) {
        return releaseRepository.searchByExternalIds(externalIds);
    }

    /**
     * Returns full documents straight from repository. Don't want this to get abused, that's why it's package-private.
     * Used for bulk-computing ReleaseClearingStateSummaries by ProjectDatabaseHandler.
     * The reason for this hack is that making summaries (like in getReleases()) takes way too long for a lot of
     * releases.
     */
    List<Release> getReleasesForClearingStateSummary(Set<String> ids) {
        return releaseRepository.get(ids);
    }

    public List<Release> getDetailedReleasesForExport(Set<String> ids) {
        return releaseRepository.makeSummary(SummaryType.DETAILED_EXPORT_SUMMARY, ids, true);
    }

    public List<String> getReleaseIdsFromComponentId(String id, User user) {
        return releaseRepository.getReleaseIdsFromComponentId(id,user);
    }

    public List<Release> getDetailedReleasesWithAccessibilityForExport(Set<String> ids, User user) {
        List<Release> releaseList = releaseRepository.makeSummary(SummaryType.DETAILED_EXPORT_SUMMARY, ids);
        for (Release release : releaseList) {
            makePermission(release, user).fillPermissions();
            for (RequestedAction action : RequestedAction.values()) {
                release.getPermissions().put(
                        RequestedActionConverter.fromThrift(action),
                        isReleaseActionAllowed(release, user, action));
            }
        }
        return releaseList;
    }

    public List<Release> getFullReleases(Set<String> ids) {
        return releaseRepository.makeSummary(SummaryType.SUMMARY, ids);
    }

    public Set<String> getReleaseIdsByVendorIds(Set<String> vendorIds){
        return releaseRepository.getReleaseIdsFromVendorIds(vendorIds);
    }

    public Set<String> getReleaseIdsBySvmId(String svmId){
        return releaseRepository.getReleaseIdsBySvmId(svmId);
    }

    public Set<String> getReleaseIdsByCpeCaseInsensitive(String cpeId){
        return releaseRepository.getReleaseByLowercaseCpe(cpeId);
    }

    public Set<String> getReleaseIdsByNamePrefixCaseInsensitive(String namePrefix){
        return releaseRepository.getReleaseByLowercaseNamePrefix(namePrefix);
    }

    public Set<String> getReleaseIdsByVersionPrefixCaseInsensitive(String versionPrefix){
        return releaseRepository.getReleaseByLowercaseVersionPrefix(versionPrefix);
    }

    public Set<String> getAllReleaseIds(){
        return releaseRepository.getAllIds();
    }

    public Set<String> getVendorIdsByNamePrefixCaseInsensitive(String namePrefix){
        Set<String> fullnameList = vendorRepository.getVendorByLowercaseFullnamePrefix(namePrefix);
        Set<String> shortnameList = vendorRepository.getVendorByLowercaseShortnamePrefix(namePrefix);

        if (fullnameList == null) return shortnameList;
        if (shortnameList == null) return fullnameList;
        // both lists available
        fullnameList.addAll(shortnameList);
        return fullnameList;
    }

    public List<Release> getReleasesWithPermissions(Set<String> ids, User user) {
        return releaseRepository.makeSummaryWithPermissions(SummaryType.SUMMARY, ids, user);
    }

    public RequestStatus subscribeComponent(String id, User user) throws SW360Exception {
        Component component = componentRepository.get(id);
        assertNotNull(component);

        Set<String> subscribers = component.getSubscribers();
        if (subscribers == null) {
            subscribers = new HashSet<>();
            component.setSubscribers(subscribers);
        }
        subscribers.add(user.getEmail());
        componentRepository.update(component);
        return RequestStatus.SUCCESS;
    }

    public RequestStatus subscribeRelease(String id, User user) throws SW360Exception {
        Release release = releaseRepository.get(id);
        assertNotNull(release);

        Set<String> subscribers = release.getSubscribers();
        if (subscribers == null) {
            subscribers = new HashSet<>();
            release.setSubscribers(subscribers);
        }
        subscribers.add(user.getEmail());
        releaseRepository.update(release);
        return RequestStatus.SUCCESS;
    }


    public RequestStatus unsubscribeComponent(String id, User user) throws SW360Exception {
        Component component = componentRepository.get(id);
        assertNotNull(component);

        Set<String> subscribers = component.getSubscribers();
        String email = user.getEmail();
        if (subscribers != null && email != null) {
            subscribers.remove(email);
            component.setSubscribers(subscribers);
        }

        componentRepository.update(component);
        return RequestStatus.SUCCESS;
    }

    public RequestStatus unsubscribeRelease(String id, User user) throws SW360Exception {
        Release release = releaseRepository.get(id);
        assertNotNull(release);

        Set<String> subscribers = release.getSubscribers();
        String email = user.getEmail();
        if (subscribers != null && email != null) {
            subscribers.remove(email);
            release.setSubscribers(subscribers);
        }
        releaseRepository.update(release);
        return RequestStatus.SUCCESS;
    }

    public Component getComponentForEdit(String id, User user) throws SW360Exception {
        List<org.eclipse.sw360.datahandler.services.moderation.ModerationRequest> moderationRequestsForDocumentId =
                moderator.getModerationRequestsForDocumentId(id);

        Component component = getComponent(id, user);
        DocumentState documentState;

        if (moderationRequestsForDocumentId.isEmpty()) {
            documentState = DocumentStateConverter.fromThrift(CommonUtils.getOriginalDocumentState());
        } else {
            final String email = user.getEmail();
            Optional<org.eclipse.sw360.datahandler.services.moderation.ModerationRequest> moderationRequestOptional =
                    CommonUtils.getFirstModerationRequestOfUserPojo(moderationRequestsForDocumentId, email);
            if (moderationRequestOptional.isPresent()
                    && isInProgressOrPending(moderationRequestOptional.get())){
                ModerationRequest moderationRequest = ModerationRequestConverter.toThrift(moderationRequestOptional.get());

                component = ComponentConverter.fromThrift(moderator.updateComponentFromModerationRequest(
                        ComponentConverter.toThrift(component),
                        moderationRequest.getComponentAdditions(),
                        moderationRequest.getComponentDeletions()));
                documentState = DocumentStateConverter.fromThrift(CommonUtils.getModeratedDocumentState(moderationRequestOptional.get()));
            } else {
                documentState = new DocumentState()
                        .setOriginalDocument(true)
                        .setModerationState(ModerationState.valueOf(
                                moderationRequestsForDocumentId.get(0).getModerationState().name()));
            }
        }
        makePermission(component, user).fillPermissions();
        component.setDocumentState(documentState);
        return component;
    }

    public Component getAccessibleComponentForEdit(String id, User user) throws SW360Exception {
        Component component = getComponentForEdit(id, user);
        Map<org.eclipse.sw360.datahandler.services.users.RequestedAction, Boolean> permissions = component.getPermissions();
        if (!Boolean.TRUE.equals(permissions.get(org.eclipse.sw360.datahandler.services.users.RequestedAction.READ))) {
            throw fail(403, "Could not fetch component for edit, because access is denied! id=" + id);
        }
        return component;
    }

    public Release getReleaseForEdit(String id, User user) throws SW360Exception {
        List<org.eclipse.sw360.datahandler.services.moderation.ModerationRequest> moderationRequestsForDocumentId =
                moderator.getModerationRequestsForDocumentId(id);

        Release release = getRelease(id, user);
        DocumentState documentState;

        if (moderationRequestsForDocumentId.isEmpty()) {
            documentState = DocumentStateConverter.fromThrift(CommonUtils.getOriginalDocumentState());
        } else {
            final String email = user.getEmail();
            Optional<org.eclipse.sw360.datahandler.services.moderation.ModerationRequest> moderationRequestOptional =
                    CommonUtils.getFirstModerationRequestOfUserPojo(moderationRequestsForDocumentId, email);
            if (moderationRequestOptional.isPresent()
                    && isInProgressOrPending(moderationRequestOptional.get())){
                ModerationRequest moderationRequest = ModerationRequestConverter.toThrift(moderationRequestOptional.get());

                release = ReleaseConverter.fromThrift(releaseModerator.updateReleaseFromModerationRequest(
                        ReleaseConverter.toThrift(release),
                        moderationRequest.getReleaseAdditions(),
                        moderationRequest.getReleaseDeletions()));
                documentState = DocumentStateConverter.fromThrift(CommonUtils.getModeratedDocumentState(moderationRequestOptional.get()));
            } else {
                documentState = new DocumentState()
                        .setOriginalDocument(true)
                        .setModerationState(ModerationState.valueOf(
                                moderationRequestsForDocumentId.get(0).getModerationState().name()));
            }
        }
        vendorRepository.fillVendor(release);
        makePermission(release, user).fillPermissions();
        release.setDocumentState(documentState);
        ensureEccInformationIsSet(release);
        return release;
    }

    public Release getAccessibleReleaseForEdit(String id, User user) throws SW360Exception {
        Release release = getReleaseForEdit(id, user);
        if (!isReleaseActionAllowed(release, user, RequestedAction.READ)) {
            throw fail(403, "Could not access the release for edit! id=" + id);
        }
        return release;
    }

    public String getCyclicLinkedReleasePath(Release release, User user) throws TException {
        return DatabaseHandlerUtil.getCyclicLinkedPath(release, this, user);
    }

    public List<Component> searchComponentByNameForExport(String name, boolean caseSensitive) {
        return componentRepository.searchComponentByName(name, caseSensitive);
    }

    public Set<Component> getUsingComponents(String releaseId) {
        return componentRepository.getUsingComponents(releaseId);
    }

    public Set<Component> getUsingComponentsWithAccessibility(String releaseId, User user) {
        Set<Component> componentSet = componentRepository.getUsingComponents(releaseId);
        for (Component component : componentSet) {
            makePermission(component, user).fillPermissions();
        }
        return componentSet;
    }

    public Set<Component> getUsingComponents(Set<String> releaseIds) {
        return componentRepository.getUsingComponents(releaseIds);
    }

    public Set<Component> getUsingComponentsWithAccessibility(Set<String> releaseIds, User user) {
        Set<Component> componentSet = componentRepository.getUsingComponents(releaseIds);
        for (Component component : componentSet) {
            makePermission(component, user).fillPermissions();
        }
        return componentSet;
    }

    public Set<Component> getComponentsByDefaultVendorId(String vendorId) {
        return componentRepository.getComponentsByDefaultVendorId(vendorId);
    }

    public Component getComponentForReportFromFossologyUploadId(String uploadId) {

        Component component = componentRepository.getComponentFromFossologyUploadId(uploadId);

        if (component != null) {
            if ((component.getReleaseIds() != null)) {
                // Convert Ids to release summary
                final Set<String> releaseIds = component.getReleaseIds();
                final List<Release> releases = nullToEmptyList(releaseRepository.get(releaseIds));
                for (Release release : releases) {
                    vendorRepository.fillVendor(release);
                }
                component.setReleases(releases);
                component.setReleaseIds(null);

                setMainLicenses(component);
            }
        }
        return component;
    }

    public Set<String> getusedAttachmentContentIds() {
        return componentRepository.getUsedAttachmentContents();
    }

    public Map<String, List<String>> getDuplicateComponents() {
        ListMultimap<String, String> componentIdentifierToComponentId = ArrayListMultimap.create();

        for (Component component : componentRepository.getAll()) {
            componentIdentifierToComponentId.put(printComponentName(component), component.getId());
        }
        return CommonUtils.getIdentifierToListOfDuplicates(componentIdentifierToComponentId);
    }

    public Map<String, List<String>> getDuplicateReleases() {
        ListMultimap<String, String> releaseIdentifierToReleaseId = ArrayListMultimap.create();

        for (Release release : getAllReleases()) {
            releaseIdentifierToReleaseId.put(printReleaseName(release), release.getId());
        }

        return CommonUtils.getIdentifierToListOfDuplicates(releaseIdentifierToReleaseId);
    }

    public Set<Attachment> getSourceAttachments(String releaseId) throws SW360Exception {
        Release release = assertNotNull(releaseRepository.get(releaseId));

        return nullToEmptySet(release.getAttachments())
                .stream()
                .filter(Objects::nonNull)
                .filter(input -> input.getAttachmentType() == AttachmentType.SOURCE)
                .collect(Collectors.toSet());
    }

    public Map<String,List<String>> getDuplicateReleaseSources() {
        ListMultimap<String, String> releaseIdentifierToReleaseId = ArrayListMultimap.create();

        for (Release release : getAllReleases()) {

            if((release.getAttachments() != null)) {
                for (Attachment attachment : release.getAttachments()) {
                    if (attachment.getAttachmentType() == AttachmentType.SOURCE)
                        releaseIdentifierToReleaseId.put(printReleaseName(release), release.getId());
                }
            }
        }

        return CommonUtils.getIdentifierToListOfDuplicates(releaseIdentifierToReleaseId);
    }

    public List<Component> getRecentComponentsSummary(int limit, User user) {
        return componentRepository.getRecentComponentsSummary(limit, user);
    }

    public List<Component> getAccessibleRecentComponentsSummary(int limit, User user) {
        List<Component> allComponentList = componentRepository.getRecentComponentsSummary(-1, user);
        List<Component> componentList = new ArrayList<Component>();
        int componentNumber = 0;
        for (Component component : allComponentList) {
            if (0 <= limit) {
                if (limit == componentNumber) {
                    break;
                }
            }
            if (makePermission(component, user).isActionAllowed(RequestedAction.READ)){
                componentList.add(component);
                componentNumber++;
            }
        }
        return componentList;
    }

    public int getTotalComponentsCount() {
        return componentRepository.getDocumentCount();
    }

    public int getAccessibleTotalComponentsCount(User user) {
        List<Component> componentList = getAccessibleRecentComponentsSummary(-1, user);
        return componentList.size();
    }

    public List<Release> getReferencingReleases(String releaseId) {
        return releaseRepository.getReferencingReleases(releaseId);
    }

    public RequestStatus splitComponent(Component srcComponent, Component targetComponent, User user)
            throws TException {
        Component srcComponentFromDB = getComponent(srcComponent.getId(), user);
        Component targetComponentFromDB = getComponent(targetComponent.getId(), user);

        if (!makePermission(targetComponentFromDB, user).isActionAllowed(RequestedAction.WRITE)
                || !makePermission(srcComponentFromDB, user).isActionAllowed(RequestedAction.WRITE)) {
            return RequestStatus.ACCESS_DENIED;
        }

        if (isComponentUnderModeration(targetComponent.getId()) || isComponentUnderModeration(srcComponent.getId())) {
            return RequestStatus.IN_USE;
        }

        Component srcComponentFromDBOriginal = ComponentConverter.fromThrift(ComponentConverter.toThrift(srcComponentFromDB));
        Component targetComponentFromDBOriginal = ComponentConverter.fromThrift(ComponentConverter.toThrift(targetComponentFromDB));

        boolean isAttachmentsModified = moveAttachmentFromSrcComponentToTargetComponent(srcComponent, targetComponent,
                srcComponentFromDB, targetComponentFromDB);
        boolean isUpdated = false;
        try {
            Set<String> srcComponentReleaseIdsAfter = nullToEmptyList(srcComponent.getReleases()).stream().map(Release::getId)
                    .collect(Collectors.toSet());
            Set<String> targetComponentReleaseIdsAfter = nullToEmptyList(targetComponent.getReleases()).stream().map(Release::getId)
                    .collect(Collectors.toSet());

            Set<String> targetComponentReleaseIdsBefore = nullToEmptyList(targetComponentFromDB.getReleases()).stream().map(Release::getId)
                    .collect(Collectors.toSet());
            Set<String> srcComponentReleaseIdsBefore = nullToEmptyList(srcComponentFromDB.getReleases()).stream().map(Release::getId)
                    .collect(Collectors.toSet());
            srcComponentFromDBOriginal.setReleaseIds(srcComponentReleaseIdsBefore);
            targetComponentFromDBOriginal.setReleaseIds(targetComponentReleaseIdsBefore);

            Set<String> srcComponentReleaseIdsMovedFromSrc = new HashSet<>(srcComponentReleaseIdsBefore);
            srcComponentReleaseIdsMovedFromSrc.removeAll(srcComponentReleaseIdsAfter);

            long noOfReleasesNotAllowedToUpdate = getNoOfReleasesNotAllowedToUpdate(srcComponentReleaseIdsMovedFromSrc, user);

            if (noOfReleasesNotAllowedToUpdate > 0) {
                return RequestStatus.ACCESS_DENIED;
            }

            if (isAttachmentsModified || CommonUtils.isNotEmpty(srcComponentReleaseIdsMovedFromSrc)) {
                targetComponentFromDB.setReleaseIds(targetComponentReleaseIdsAfter);
                srcComponentFromDB.setReleaseIds(srcComponentReleaseIdsAfter);

                recomputeReleaseDependentFields(targetComponentFromDB, null);
                recomputeReleaseDependentFields(srcComponentFromDB, null);
                targetComponentFromDB.setReleases(null);
                srcComponentFromDB.setReleases(null);
                updateModifiedFields(targetComponentFromDB, user.getEmail());
                componentRepository.update(targetComponentFromDB);
                updateModifiedFields(srcComponentFromDB, user.getEmail());
                componentRepository.update(srcComponentFromDB);

                updateReleaseAfterComponentSplit(srcComponentFromDBOriginal, targetComponentFromDBOriginal,
                        srcComponentReleaseIdsMovedFromSrc, targetComponentReleaseIdsBefore, user);
                isUpdated = true;
            }

        } catch (Exception e) {
            log.error("Cannot split component [" + srcComponent.getId() + "] into [" + targetComponent.getId() + "]",
                    e);
            return RequestStatus.FAILURE;
        }
        if (isUpdated) {
            sendMailNotificationsForComponentUpdate(targetComponentFromDB, user.getEmail());
            sendMailNotificationsForComponentUpdate(srcComponentFromDB, user.getEmail());
            dbHandlerUtil.addChangeLogs(ComponentConverter.toThrift(srcComponentFromDB), ComponentConverter.toThrift(srcComponentFromDBOriginal), user.getEmail(),
                    Operation.UPDATE, null, Lists.newArrayList(), null, Operation.SPLIT_COMPONENT);
            dbHandlerUtil.addChangeLogs(ComponentConverter.toThrift(targetComponentFromDB), ComponentConverter.toThrift(targetComponentFromDBOriginal), user.getEmail(),
                    Operation.UPDATE, null, Lists.newArrayList(), null,
                    Operation.SPLIT_COMPONENT);
        }
        return RequestStatus.SUCCESS;
    }

    private long getNoOfReleasesNotAllowedToUpdate(Set<String> releaseIds, User sessionUser) {
        return releaseIds.stream().map(relId -> {
                    try {
                        return getRelease(relId, sessionUser);
                    } catch (SW360Exception e) {
                        log.error("Error occurred while getting release. ", e);
                    }
                    return null;
                }).filter(rel -> rel == null || !makePermission(rel, sessionUser).isActionAllowed(RequestedAction.WRITE))
                .count();
    }

    private void sendMailNotificationsForNewComponent(Component component, String user) {
        mailUtil.sendMail(component.getComponentOwner(),
                MailConstants.SUBJECT_FOR_NEW_COMPONENT,
                MailConstants.TEXT_FOR_NEW_COMPONENT,
                SW360Constants.NOTIFICATION_CLASS_COMPONENT, "componentOwner",
                component.getName());
        mailUtil.sendMail(component.getModerators(), user,
                MailConstants.SUBJECT_FOR_NEW_COMPONENT,
                MailConstants.TEXT_FOR_NEW_COMPONENT,
                SW360Constants.NOTIFICATION_CLASS_COMPONENT, "moderators",
                component.getName());
        mailUtil.sendMail(component.getSubscribers(), user,
                MailConstants.SUBJECT_FOR_NEW_COMPONENT,
                MailConstants.TEXT_FOR_NEW_COMPONENT,
                SW360Constants.NOTIFICATION_CLASS_COMPONENT, "subscribers",
                component.getName());
        mailUtil.sendMail(SW360Utils.unionValues(component.getRoles()), user,
                MailConstants.SUBJECT_FOR_NEW_COMPONENT,
                MailConstants.TEXT_FOR_NEW_COMPONENT,
                SW360Constants.NOTIFICATION_CLASS_COMPONENT, "roles",
                component.getName());
    }

    private void sendMailNotificationsForComponentUpdate(Component component, String user) {
        mailUtil.sendMail(component.getCreatedBy(),
                MailConstants.SUBJECT_FOR_UPDATE_COMPONENT,
                MailConstants.TEXT_FOR_UPDATE_COMPONENT,
                SW360Constants.NOTIFICATION_CLASS_COMPONENT, "createdBy",
                component.getName());
        mailUtil.sendMail(component.getComponentOwner(),
                MailConstants.SUBJECT_FOR_UPDATE_COMPONENT,
                MailConstants.TEXT_FOR_UPDATE_COMPONENT,
                SW360Constants.NOTIFICATION_CLASS_COMPONENT, "componentOwner",
                component.getName());
        mailUtil.sendMail(component.getModerators(), user,
                MailConstants.SUBJECT_FOR_UPDATE_COMPONENT,
                MailConstants.TEXT_FOR_UPDATE_COMPONENT,
                SW360Constants.NOTIFICATION_CLASS_COMPONENT, "moderators",
                component.getName());
        mailUtil.sendMail(component.getSubscribers(), user,
                MailConstants.SUBJECT_FOR_UPDATE_COMPONENT,
                MailConstants.TEXT_FOR_UPDATE_COMPONENT,
                SW360Constants.NOTIFICATION_CLASS_COMPONENT, "subscribers",
                component.getName());
        mailUtil.sendMail(SW360Utils.unionValues(component.getRoles()), user,
                MailConstants.SUBJECT_FOR_UPDATE_COMPONENT,
                MailConstants.TEXT_FOR_UPDATE_COMPONENT,
                SW360Constants.NOTIFICATION_CLASS_COMPONENT, "roles",
                component.getName());
    }

    private void sendMailNotificationsForNewRelease(Release release, String user) {
        mailUtil.sendMail(release.getContributors(), user,
                MailConstants.SUBJECT_FOR_NEW_RELEASE,
                MailConstants.TEXT_FOR_NEW_RELEASE,
                SW360Constants.NOTIFICATION_CLASS_RELEASE, "contributors",
                release.getName(), release.getVersion());
        mailUtil.sendMail(release.getModerators(), user,
                MailConstants.SUBJECT_FOR_NEW_RELEASE,
                MailConstants.TEXT_FOR_NEW_RELEASE,
                SW360Constants.NOTIFICATION_CLASS_RELEASE, "moderators",
                release.getName(), release.getVersion());
        mailUtil.sendMail(release.getSubscribers(), user,
                MailConstants.SUBJECT_FOR_NEW_RELEASE,
                MailConstants.TEXT_FOR_NEW_RELEASE,
                SW360Constants.NOTIFICATION_CLASS_RELEASE, "subscribers",
                release.getName(), release.getVersion());
        mailUtil.sendMail(SW360Utils.unionValues(release.getRoles()), user,
                MailConstants.SUBJECT_FOR_NEW_RELEASE,
                MailConstants.TEXT_FOR_NEW_RELEASE,
                SW360Constants.NOTIFICATION_CLASS_RELEASE, "subscribers",
                release.getName(), release.getVersion());
    }

    private void sendMailNotificationsForReleaseUpdate(Release release, String user) {
        mailUtil.sendMail(release.getCreatedBy(),
                MailConstants.SUBJECT_FOR_UPDATE_RELEASE,
                MailConstants.TEXT_FOR_UPDATE_RELEASE,
                SW360Constants.NOTIFICATION_CLASS_RELEASE, "contributors",
                release.getName(), release.getVersion());
        mailUtil.sendMail(release.getContributors(), user,
                MailConstants.SUBJECT_FOR_UPDATE_RELEASE,
                MailConstants.TEXT_FOR_UPDATE_RELEASE,
                SW360Constants.NOTIFICATION_CLASS_RELEASE, "contributors",
                release.getName(), release.getVersion());
        mailUtil.sendMail(release.getModerators(), user,
                MailConstants.SUBJECT_FOR_UPDATE_RELEASE,
                MailConstants.TEXT_FOR_UPDATE_RELEASE,
                SW360Constants.NOTIFICATION_CLASS_RELEASE, "moderators",
                release.getName(), release.getVersion());
        mailUtil.sendMail(release.getSubscribers(), user,
                MailConstants.SUBJECT_FOR_UPDATE_RELEASE,
                MailConstants.TEXT_FOR_UPDATE_RELEASE,
                SW360Constants.NOTIFICATION_CLASS_RELEASE, "subscribers",
                release.getName(), release.getVersion());
        mailUtil.sendMail(SW360Utils.unionValues(release.getRoles()), user,
                MailConstants.SUBJECT_FOR_UPDATE_RELEASE,
                MailConstants.TEXT_FOR_UPDATE_RELEASE,
                SW360Constants.NOTIFICATION_CLASS_RELEASE, "subscribers",
                release.getName(), release.getVersion());
    }

    public RequestStatus updateReleasesWithSvmTrackingFeedback() {
        try {
            Map<String, Map<String, Object>> componentMappings = getSvmConnector().fetchComponentMappings();
            List<Release> releases = releaseRepository.getReleasesIgnoringNotFound(componentMappings.keySet());
            releases.forEach(r -> {
                Map<String, String> externalIds = (r.getExternalIds() != null) ? r.getExternalIds() : new HashMap<>();
                Map<String, String> additionalData = (r.getAdditionalData() != null) ? r.getAdditionalData() : new HashMap<>();

                Map<String, Object> releaseSVMData = componentMappings.get(r.getId());
                if (!CommonUtils.isNullOrEmptyMap(releaseSVMData)) {
                    Release originalReleaseData = ReleaseConverter.fromThrift(ReleaseConverter.toThrift(r));
                    Object svmComponentId = releaseSVMData.get(SW360Constants.SVM_COMPONENT_ID_KEY);
                    Object shortStatus = releaseSVMData.get(SW360Constants.SVM_SHORT_STATUS_KEY);
                    boolean isChanged = false;
                    if (svmComponentId != null) {
                        String previousValue = externalIds.get(SW360Constants.SVM_COMPONENT_ID);
                        if (previousValue == null || !previousValue.equals(svmComponentId.toString())) {
                            externalIds.put(SW360Constants.SVM_COMPONENT_ID, svmComponentId.toString());
                            r.setExternalIds(externalIds);
                            isChanged = true;
                        }
                    }

                    if (shortStatus != null && CommonUtils.isNotNullEmptyOrWhitespace(shortStatus.toString())) {
                        String previousValue = additionalData.get(SW360Constants.SVM_SHORT_STATUS);
                        if (previousValue == null || !previousValue.equals(shortStatus.toString())) {
                            additionalData.put(SW360Constants.SVM_SHORT_STATUS, shortStatus.toString());
                            r.setAdditionalData(additionalData);
                            isChanged = true;
                        }
                    }

                    if (isChanged) {
                        dbHandlerUtil.addChangeLogs(ReleaseConverter.toThrift(r), ReleaseConverter.toThrift(originalReleaseData), SW360Constants.SVM_SCHEDULER_EMAIL,
                                Operation.UPDATE, attachmentConnector, Lists.newArrayList(), null, null);
                    }
                }
            });
            List<DocumentResult> documentOperationResults = releaseRepository.executeBulk(releases);
            documentOperationResults = documentOperationResults.stream().filter(res -> res.getError() != null || !res.isOk())
                    .toList();
            if (documentOperationResults.isEmpty()) {
                log.info(String.format("SVMTF: updated %d releases", releases.size()));
            } else {
                log.error("SVMTF: Failed saving releases: " + documentOperationResults);
                return RequestStatus.FAILURE;
            }
        } catch (IOException | SW360Exception e) {
            log.error(e);
            return RequestStatus.FAILURE;
        }

        return RequestStatus.SUCCESS;
    }

    @NotNull
    private SvmConnector getSvmConnector() {
        if (svmConnector == null) {
            svmConnector = new SvmConnector();
        }
        return svmConnector;
    }

    public ComponentDatabaseHandler setSvmConnector(SvmConnector svmConnector) {
        this.svmConnector = svmConnector;
        return this;
    }

    public ImportBomRequestPreparation prepareImportBom(User user, String attachmentContentId) throws SW360Exception {
        final AttachmentContent attachmentContent = attachmentConnector.getAttachmentContent(attachmentContentId);
        final Duration timeout = Duration.durationOf(30, TimeUnit.SECONDS);
        try {
            final AttachmentStreamConnector attachmentStreamConnector = new AttachmentStreamConnector(timeout);
            try (final InputStream inputStream = attachmentStreamConnector.unsafeGetAttachmentStream(attachmentContent)) {
                final SpdxBOMImporterSink spdxBOMImporterSink = new SpdxBOMImporterSink(user, null, this);
                final SpdxBOMImporter spdxBOMImporter = new SpdxBOMImporter(spdxBOMImporterSink);

                String fileType = getFileType(attachmentContent.getFilename());
                final String ext = "." + fileType;
                final File sourceFile = DatabaseHandlerUtil.saveAsTempFile(inputStream, attachmentContentId, ext);

                ImportBomRequestPreparation importBomRequestPreparation = spdxBOMImporter.prepareImportSpdxBOMAsRelease(sourceFile);
                if (RequestStatus.SUCCESS.equals(importBomRequestPreparation.getRequestStatus())) {
                    List<String> componentsName = getComponentsName(importBomRequestPreparation.getComponentsName());
                    Map<String, String> releasesName = getReleasesName(importBomRequestPreparation.getReleasesName());
                    isDuplicateRelease(releasesName);
                    isDuplicateComponent(componentsName,true);
                    if (listComponentName.size() == 0 && mapReleaseName.size() == 0){
                        importBomRequestPreparation.setIsComponentDuplicate(true);
                        importBomRequestPreparation.setIsReleaseDuplicate(true);
                    }
                    else {
                        String componentName = "";
                        String releaseName = "";
                        if (listComponentName.isEmpty()){
                            componentName = NO_COMPONENT;
                        } else {
                            componentName = listComponentName.stream().collect(Collectors.joining(" , "));
                        }
                        if (mapReleaseName.isEmpty()){
                            releaseName = NO_RELEASE;
                        } else {
                            releaseName = mapReleaseName.keySet().stream().map(key -> key + " " + mapReleaseName.get(key)).collect(Collectors.joining(" , "));
                        }
                        listComponentName.clear();
                        mapReleaseName.clear();
                        importBomRequestPreparation.setComponentsName(componentName);
                        importBomRequestPreparation.setReleasesName(releaseName);
                        importBomRequestPreparation.setIsComponentDuplicate(false);
                        importBomRequestPreparation.setIsReleaseDuplicate(false);
                    }

                    importBomRequestPreparation.setMessage(sourceFile.getAbsolutePath());
                }

                return importBomRequestPreparation;
            }
        } catch (IOException | InvalidSPDXAnalysisException e) {
            throw new SW360Exception(e.getMessage());
        }
    }

    public RequestSummary importBomFromAttachmentContent(User user, String attachmentContentId) throws SW360Exception {
        final AttachmentContent attachmentContent = attachmentConnector.getAttachmentContent(attachmentContentId);
        final Duration timeout = Duration.durationOf(30, TimeUnit.SECONDS);
        try {
            final AttachmentStreamConnector attachmentStreamConnector = new AttachmentStreamConnector(timeout);
            try (final InputStream inputStream = attachmentStreamConnector.unsafeGetAttachmentStream(attachmentContent)) {
                final SpdxBOMImporterSink spdxBOMImporterSink = new SpdxBOMImporterSink(user, null, this);
                final SpdxBOMImporter spdxBOMImporter = new SpdxBOMImporter(spdxBOMImporterSink);
                return RequestSummaryConverter.fromThrift(
                        spdxBOMImporter.importSpdxBOMAsRelease(inputStream, attachmentContent, user));
            }
        } catch (IOException e) {
            throw new SW360Exception(e.getMessage());
        }
    }

    private String getFileType(String fileName) {
        if (isNullEmptyOrWhitespace(fileName) || !fileName.contains(".")) {
            log.error("Can not get file type from file name - no file extension");
            return null;
        }
        String ext = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        if ("xml".equals(ext)) {
            if (fileName.endsWith("rdf.xml")) {
                ext = "rdf";
            }
        }
        return ext;
    }

    private void removeLeadingTrailingWhitespace(Release release) {
        trimIfSet(release.getCpeid(), release::setCpeid);
        trimIfSet(release.getName(), release::setName);
        trimIfSet(release.getVersion(), release::setVersion);
        trimIfSet(release.getReleaseDate(), release::setReleaseDate);
        trimIfSet(release.getSourceCodeDownloadurl(), release::setSourceCodeDownloadurl);
        trimIfSet(release.getBinaryDownloadurl(), release::setBinaryDownloadurl);

        ClearingInformation clearingInformation = release.getClearingInformation();
        if (clearingInformation != null) {
            trimIfSet(clearingInformation.getScanned(), clearingInformation::setScanned);
            trimIfSet(clearingInformation.getClearingStandard(), clearingInformation::setClearingStandard);
            trimIfSet(clearingInformation.getExternalUrl(), clearingInformation::setExternalUrl);
            trimIfSet(clearingInformation.getComment(), clearingInformation::setComment);
            trimIfSet(clearingInformation.getRequestID(), clearingInformation::setRequestID);
            trimIfSet(clearingInformation.getAdditionalRequestInfo(), clearingInformation::setAdditionalRequestInfo);
            trimIfSet(clearingInformation.getExternalSupplierID(), clearingInformation::setExternalSupplierID);
            trimIfSet(clearingInformation.getEvaluated(), clearingInformation::setEvaluated);
            trimIfSet(clearingInformation.getProcStart(), clearingInformation::setProcStart);
        }

        COTSDetails cotsDetails = release.getCotsDetails();
        if (cotsDetails != null) {
            trimIfSet(cotsDetails.getUsedLicense(), cotsDetails::setUsedLicense);
            trimIfSet(cotsDetails.getLicenseClearingReportURL(), cotsDetails::setLicenseClearingReportURL);
            trimIfSet(cotsDetails.getOssInformationURL(), cotsDetails::setOssInformationURL);
        }

        EccInformation eccInformation = release.getEccInformation();
        if (eccInformation != null) {
            trimIfSet(eccInformation.getAl(), eccInformation::setAl);
            trimIfSet(eccInformation.getEccn(), eccInformation::setEccn);
            trimIfSet(eccInformation.getEccComment(), eccInformation::setEccComment);
            trimIfSet(eccInformation.getMaterialIndexNumber(), eccInformation::setMaterialIndexNumber);
        }

        Repository repository = release.getRepository();
        if (repository != null) {
            String url = repository.getUrl();
            if (url != null) {
                repository.setUrl(url.trim());
            }
        }

        release.setLanguages(DatabaseHandlerUtil.trimSetOfString(release.getLanguages()));

        release.setOperatingSystems(DatabaseHandlerUtil.trimSetOfString(release.getOperatingSystems()));

        release.setSoftwarePlatforms(DatabaseHandlerUtil.trimSetOfString(release.getSoftwarePlatforms()));

        release.setMainLicenseIds(DatabaseHandlerUtil.trimSetOfString(release.getMainLicenseIds()));

        release.setContributors(DatabaseHandlerUtil.trimSetOfString(release.getContributors()));

        release.setModerators(DatabaseHandlerUtil.trimSetOfString(release.getModerators()));

        release.setAttachments(DatabaseHandlerUtil.trimSetOfAttachement(
                release.getAttachments() == null ? Collections.emptySet()
                        : release.getAttachments().stream().map(AttachmentConverter::toThrift).collect(Collectors.toSet()))
                .stream().map(AttachmentConverter::fromThrift).collect(Collectors.toSet()));

        release.setRoles(DatabaseHandlerUtil.trimMapOfStringKeySetValue(release.getRoles()));

        release.setExternalIds(DatabaseHandlerUtil.trimMapOfStringKeyStringValue(release.getExternalIds()));

        release.setAdditionalData(DatabaseHandlerUtil.trimMapOfStringKeyStringValue(release.getAdditionalData()));
    }

    private void removeLeadingTrailingWhitespace(Component component) {
        trimIfSet(component.getName(), component::setName);
        trimIfSet(component.getDescription(), component::setDescription);
        trimIfSet(component.getComponentOwner(), component::setComponentOwner);
        trimIfSet(component.getOwnerAccountingUnit(), component::setOwnerAccountingUnit);
        trimIfSet(component.getOwnerGroup(), component::setOwnerGroup);
        trimIfSet(component.getOwnerCountry(), component::setOwnerCountry);
        trimIfSet(component.getHomepage(), component::setHomepage);
        trimIfSet(component.getMailinglist(), component::setMailinglist);
        trimIfSet(component.getWiki(), component::setWiki);
        trimIfSet(component.getBlog(), component::setBlog);

        component.setRoles(DatabaseHandlerUtil.trimMapOfStringKeySetValue(component.getRoles()));

        component.setExternalIds(DatabaseHandlerUtil.trimMapOfStringKeyStringValue(component.getExternalIds()));

        component.setAdditionalData(DatabaseHandlerUtil.trimMapOfStringKeyStringValue(component.getAdditionalData()));

        component.setCategories(DatabaseHandlerUtil.trimSetOfString(component.getCategories()));

        component.setAttachments(DatabaseHandlerUtil.trimSetOfAttachement(
                component.getAttachments() == null ? Collections.emptySet()
                        : component.getAttachments().stream().map(AttachmentConverter::toThrift).collect(Collectors.toSet()))
                .stream().map(AttachmentConverter::fromThrift).collect(Collectors.toSet()));

        component.setLanguages(DatabaseHandlerUtil.trimSetOfString(component.getLanguages()));

        component.setOperatingSystems(DatabaseHandlerUtil.trimSetOfString(component.getOperatingSystems()));
    }

    private boolean moveAttachmentFromSrcComponentToTargetComponent(Component srcComponent, Component targetComponent,
                                                                    Component srcComponentFromDB, Component targetComponentFromDB) {
        Set<String> srcComponentAttachmentIdsAfter = nullToEmptySet(srcComponent.getAttachments()).stream()
                .map(Attachment::getAttachmentContentId).collect(Collectors.toSet());
        Set<String> targetComponentAttachmentIdsAfter = nullToEmptySet(targetComponent.getAttachments()).stream()
                .map(Attachment::getAttachmentContentId).collect(Collectors.toSet());
        Map<String, Attachment> srcComponentAttachmentsMapBefore = nullToEmptySet(srcComponentFromDB.getAttachments())
                .stream().collect(Collectors.toMap(Attachment::getAttachmentContentId, Function.identity()));

        Set<Attachment> targetComponentAttachmentBefore = nullToEmptySet(targetComponentFromDB.getAttachments());
        Set<String> targetComponentAttachmentIdsBefore = targetComponentAttachmentBefore.stream()
                .map(Attachment::getAttachmentContentId).collect(Collectors.toSet());

        targetComponentAttachmentIdsAfter.removeAll(targetComponentAttachmentIdsBefore);
        if (CommonUtils.isNotEmpty(targetComponentAttachmentIdsAfter)) {
            targetComponentAttachmentIdsAfter.stream().forEach(movedAttachmentId -> targetComponentAttachmentBefore
                    .add(srcComponentAttachmentsMapBefore.get(movedAttachmentId)));
            targetComponentFromDB.setAttachments(targetComponentAttachmentBefore);

            Set<Attachment> srcComponentAttachmentFinal = srcComponentAttachmentsMapBefore.values().stream()
                    .filter(attachment -> srcComponentAttachmentIdsAfter.contains(attachment.getAttachmentContentId()))
                    .collect(Collectors.toSet());
            srcComponentFromDB.setAttachments(srcComponentAttachmentFinal);
            return true;
        }
        return false;
    }

    private void updateReleaseAfterComponentSplit(Component srcComponentFromDB, Component targetComponentFromDB,
                                                  Set<String> srcComponentReleaseIdsMovedFromSrc, Set<String> targetComponentReleaseIdsBefore, User user) throws SW360Exception {
        List<Release> targetComponentReleases = getReleasesForClearingStateSummary(targetComponentReleaseIdsBefore);
        List<Release> srcComponentReleasesMoved = getReleasesForClearingStateSummary(srcComponentReleaseIdsMovedFromSrc);
        Set<String> targetComponentReleaseVersions = targetComponentReleases.stream().map(Release::getVersion)
                .collect(Collectors.toSet());
        final String userEmail = user.getEmail();

        List<Release> releasesToUpdate = srcComponentReleasesMoved.stream().map(r -> {
            Release releaseBefore = ReleaseConverter.fromThrift(ReleaseConverter.toThrift(r));
            if (targetComponentReleaseVersions.contains(r.getVersion())) {
                StringBuilder conflictVersionBuilder = new StringBuilder(r.getVersion()).append("_conflict (")
                        .append(r.getId()).append(")");
                r.setVersion(conflictVersionBuilder.toString());
            }
            r.setComponentId(targetComponentFromDB.getId());
            r.setName(targetComponentFromDB.getName());
            updateModifiedFields(r, userEmail);
            dbHandlerUtil.addChangeLogs(ReleaseConverter.toThrift(r), ReleaseConverter.toThrift(releaseBefore), userEmail, Operation.UPDATE, attachmentConnector,
                    Lists.newArrayList(), srcComponentFromDB.getId(), Operation.SPLIT_COMPONENT);
            return r;
        }).collect(Collectors.toList());
        updateReleases(releasesToUpdate, user, true);
    }

    public Map<PaginationData, List<Component>> getRecentComponentsSummaryWithPagination(User user,
                                                                                         PaginationData pageData) {
        return componentRepository.getRecentComponentsSummary(user, pageData);
    }

    private void checkSuperAttachmentExists(Release release) {
        if (CommonUtils.isNotEmpty(release.getAttachments())) {
            Set<String> attachmentContentIds = release.getAttachments().stream()
                    .map(Attachment::getAttachmentContentId).collect(Collectors.toSet());
            release.getAttachments().stream()
                    .filter(att -> CommonUtils.isNotNullEmptyOrWhitespace(att.getAttachmentContentId()))
                    .forEach(att -> {
                        if (!attachmentContentIds.contains(att.getSuperAttachmentId())) {
                            att.setSuperAttachmentFilename(null);
                            att.setSuperAttachmentId(null);
                        }
                    });
        }
    }

    public void sendExportSpreadsheetSuccessMail(String url, String recepient) throws TException {
        mailUtil.sendMail(recepient, MailConstants.SUBJECT_SPREADSHEET_EXPORT_SUCCESS,
                MailConstants.TEXT_SPREADSHEET_EXPORT_SUCCESS, SW360Constants.NOTIFICATION_CLASS_COMPONENT, "", false,
                "component", url);
    }

    public List<String> getComponentsName(String components) {
        String[] parts = components.split(",");
        return Arrays.asList(parts);
    }

    public Map<String, String> getReleasesName(String releases) {
        Map<String,String> releaseNames= new HashMap<>();
        String[] parts = releases.split(",");
        for (int i = 0; i < parts.length; i++) {
            String[] releaseName = parts[i].split(" ");
            releaseNames.put(releaseName[0], releaseName[1]);
        }
        return releaseNames;
    }

    public String getComponentReportInEmail(User user,boolean extendedByReleases) throws TException {
        try {
            List<Component> componentlist = getRecentComponentsSummary(-1, user);
            ComponentExporter exporter = getComponentExporterObject(componentlist,user, extendedByReleases);
            return exporter.makeExcelExportForProject(
                    componentlist.stream().map(ComponentConverter::toThrift).collect(Collectors.toList()), user);
        }catch (IOException e) {
            throw new SW360Exception(e.getMessage());
        }
    }

    private ComponentExporter getComponentExporterObject(List<Component> componentList ,User user,
                                                         boolean extendedByRelease) throws SW360Exception {
        try {
            List<org.eclipse.sw360.datahandler.thrift.components.Component> thriftComponents = componentList.stream()
                    .map(ComponentConverter::toThrift).collect(Collectors.toList());
            return new ComponentExporter(new ComponentHandlerThriftAdapter(new ComponentHandler()), thriftComponents, user, extendedByRelease);
        } catch (IOException e) {
            throw new SW360Exception("Error creating ComponentHandler: " + e.getMessage());
        }
    }

    public ByteBuffer downloadExcel(User user,boolean extendedByReleases,String token) throws SW360Exception {
        try {
            ComponentExporter exporter = new ComponentExporter(new ComponentHandlerThriftAdapter(new ComponentHandler()), user,
                    extendedByReleases);
            InputStream stream = exporter.downloadExcelSheet(token);
            return ByteBuffer.wrap(IOUtils.toByteArray(stream));
        } catch (IOException e) {
            throw new SW360Exception(e.getMessage());
        }
    }

    public ByteBuffer getComponentReportDataStream(User user, boolean extendedByReleases) throws TException{
        try {
            List<Component> componentlist = getRecentComponentsSummary(-1, user);
            ComponentExporter exporter = getComponentExporterObject(componentlist, user, extendedByReleases);
            List<org.eclipse.sw360.datahandler.thrift.components.Component> thriftComponentList = componentlist.stream()
                    .map(ComponentConverter::toThrift).collect(Collectors.toList());
            InputStream stream = exporter.makeExcelExport(thriftComponentList);
            return ByteBuffer.wrap(IOUtils.toByteArray(stream));
        }catch (IOException e) {
            throw new SW360Exception(e.getMessage());
        }
    }

    public List<Release> getReleaseByIds(List<String> ids) {
        return releaseRepository.getFullDocsByListIds(SummaryType.SHORT, ids);
    }

    public List<ReleaseNode> getReleaseRelationNetworkOfRelease(Release release, User user) {
        ReleaseNode dependencyNetwork = new ReleaseNode(release.getId());
        getReleaseNodes(dependencyNetwork, user);
        return Collections.singletonList(dependencyNetwork);
    }

    private ReleaseNode getReleaseNodes(ReleaseNode releaseNode, User user) {
        Release releaseById = null;
        try {
            releaseById = getAccessibleRelease(releaseNode.getReleaseId(), user);
            List<Release> releaseList = new ArrayList<>();
            if (releaseById.getReleaseIdToRelationship() != null) {
                releaseList = getAccessibleReleases(releaseById.getReleaseIdToRelationship().keySet(), user);
            }
            List<ReleaseNode> linkedReleasesJSON = new ArrayList<>();
            releaseNode.setMainlineState(MainlineState.OPEN.toString());
            releaseNode.setReleaseRelationship(ReleaseRelationship.CONTAINED.toString());
            releaseNode.setCreateOn(SW360Utils.getCreatedOn());
            releaseNode.setCreateBy(user.getEmail());
            releaseNode.setComment("");
            for (Release release : releaseList) {
                ReleaseNode node = new ReleaseNode(release.getId());
                node.setMainlineState(MainlineState.OPEN.toString());
                node.setReleaseRelationship(ReleaseRelationship.CONTAINED.toString());
                node.setComment("");
                node.setCreateOn(SW360Utils.getCreatedOn());
                node.setCreateBy(user.getEmail());
                linkedReleasesJSON.add(getReleaseNodes(node, user));
            }
            releaseNode.setReleaseLink(linkedReleasesJSON);

        } catch (TException e) {
            log.error("Error when get Release: " + releaseNode.getReleaseId());
        }
        return releaseNode;
    }

    public RequestStatus uploadSourceCodeAttachmentToReleases() {
        List<Component> components = getAllComponentsWithVCS();
        Set<String> releasesWithoutSRC = new HashSet<>();
        Set<String> updateReleases = new HashSet<>();
        log.info(String.format("SRC Upload: Found %d components with VCS", components.size()));

        components.forEach(c -> {
            String VCS = c.getVcs();
            // Add more domains in the future and include the download logic accordingly
            if (VCS.toLowerCase().contains("github.com")) {
                for (String r_id : c.getReleaseIds()) {
                    boolean isUploaded = false;
                    Release r = getRelease(r_id);

                    if (r.getClearingState() == ClearingState.NEW_CLEARING) {
                        List<Attachment> sourceAttachments = (r.getAttachments() != null) ? r.getAttachments().stream()
                                .filter(attachment -> AttachmentType.SOURCE.equals(attachment.getAttachmentType()))
                                .collect(Collectors.toList()) : Collections.emptyList();

                        if (sourceAttachments.size() == 0) {
                            releasesWithoutSRC.add(r.getId());
                            String version = r.getVersion();
                            Release originalReleaseData = ReleaseConverter.fromThrift(ReleaseConverter.toThrift(r));
                            log.info(String.format("SRC Upload: %s %s", c.getVcs(), version));

                            for (String format : formats) {
                                String downloadURL = String.format(format, c.getVcs(), version);
                                if (isValidURL(downloadURL)) {
                                    try {
                                        String destinationDirectory = SW360Constants.SRC_ATTACHMENT_DOWNLOAD_LOCATION;
                                        File file = downloadFile(downloadURL, destinationDirectory);
                                        Attachment attachment = new Attachment()
                                                .setAttachmentType(AttachmentType.SOURCE);
                                        Set<Attachment> src_attachment = new HashSet<>();
                                        src_attachment.add(uploadAttachment(file, attachment));
                                        r.setAttachments(src_attachment);
                                        r.setSourceCodeDownloadurl(downloadURL);
                                        releaseRepository.update(r);
                                        isUploaded = true;
                                        updateReleases.add(r.getId());
                                        // Delete the SRC zip file after the release is updated
                                        file.delete();
                                        break;
                                    } catch (IOException | URISyntaxException | IllegalArgumentException | TException e) {
                                        log.error(
                                                "SRC Upload: Error while downloading the source code zip file for release:"
                                                        + r.getId() + " " + e);
                                    } catch (Exception e) {
                                        log.error("An exception occurred while uploading source:" + r.getId() + " " + e);
                                    }
                                }
                            }
                            if (isUploaded) {
                                dbHandlerUtil.addChangeLogs(ReleaseConverter.toThrift(r), ReleaseConverter.toThrift(originalReleaseData),
                                        SW360Constants.SRC_ATTACHMENT_UPLOADER_EMAIL, Operation.UPDATE,
                                        attachmentConnector, Lists.newArrayList(), null, null);
                            }
                        }
                    }
                }
            }
        });
        if (updateReleases.size() == releasesWithoutSRC.size()) {
            log.info(String.format("SRC Upload: updated %d releases", updateReleases.size()));
            return RequestStatus.SUCCESS;
        } else {
            log.error("SRC Upload: Failed to upload SRC attachments for releases: "
                    + Sets.difference(releasesWithoutSRC, updateReleases));
            return RequestStatus.FAILURE;
        }
    }



    public File downloadFile(String url, String destinationDirectory) throws IOException, URISyntaxException {
        URL fileUrl = new URI(url).toURL();
        String regex = ".*/([^/]+)/archive/refs/tags/(?:v)?(.*).zip$";
        String fileName = url.replaceAll(regex, "$1-$2.zip");
        Path destinationPath = Paths.get(destinationDirectory, fileName.replace("/","-"));
        try (InputStream in = fileUrl.openStream()) {
            Files.copy(in, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        }
        return destinationPath.toFile();
    }

    public Attachment uploadAttachment(File file, Attachment newAttachment) throws IOException, TException {
        String fileName = file.getName();
        String contentType = "application/zip";
        final AttachmentContent attachmentContent = makeAttachmentContent(fileName, contentType);
        Attachment attachment;
        try (FileInputStream inputStream = new FileInputStream(file)) {
            AttachmentFrontendUtils frontendUtils = new AttachmentFrontendUtils(
                    new AttachmentDatabaseHandlerMetadataOperations(attachmentDatabaseHandler));
            attachment = AttachmentConverter.fromThrift(frontendUtils.uploadAttachmentContent(attachmentContent, inputStream, null));
        }
        attachment.setSha1(attachmentConnector.getSha1FromAttachmentContentId(attachmentContent.getId()));
        attachment.setAttachmentType(AttachmentType.SOURCE);
        attachment.setCheckStatus(CheckStatus.NOTCHECKED);
        attachment.setCreatedComment("Uploaded by the SW360 scheduled service based on the VCS url of the component");
        attachment.setCreatedBy(SW360Constants.SRC_ATTACHMENT_UPLOADER_EMAIL);
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
            return new AttachmentFrontendUtils(
                    new AttachmentDatabaseHandlerMetadataOperations(attachmentDatabaseHandler))
                    .makeAttachmentContent(content);
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }
    private static Set<org.eclipse.sw360.datahandler.thrift.attachments.Attachment> toThriftAttachments(
            Set<Attachment> attachments) {
        if (attachments == null) {
            return null;
        }
        return attachments.stream().map(org.eclipse.sw360.common.utils.converter.attachments.AttachmentConverter::toThrift)
                .collect(java.util.stream.Collectors.toSet());
    }

    private static Set<Attachment> fromThriftAttachments(
            Set<org.eclipse.sw360.datahandler.thrift.attachments.Attachment> attachments) {
        if (attachments == null) {
            return null;
        }
        return attachments.stream().map(org.eclipse.sw360.common.utils.converter.attachments.AttachmentConverter::fromThrift)
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * Restores the fields that must never be overwritten by a client payload.
     * POJO equivalent of {@code copyFields(actual, component, ThriftUtils.IMMUTABLE_OF_COMPONENT)}.
     */
    private static void copyImmutableFields(Component component, Component actual) {
        copyIfSet(actual.getCreatedBy(), component::setCreatedBy);
        copyIfSet(actual.getCreatedOn(), component::setCreatedOn);
    }

    /**
     * Restores the fields that must never be overwritten by a client payload.
     *
     * @see ReleaseImmutableField#DEFAULT
     * @see ReleaseImmutableField#FOR_FOSSOLOGY
     */
    private static void copyImmutableFields(Release release, Release actual,
            Set<ReleaseImmutableField> immutableFields) {
        for (ReleaseImmutableField field : immutableFields) {
            switch (field) {
                case CREATED_BY -> copyIfSet(actual.getCreatedBy(), release::setCreatedBy);
                case CREATED_ON -> copyIfSet(actual.getCreatedOn(), release::setCreatedOn);
                case EXTERNAL_TOOL_PROCESSES ->
                        copyIfSet(actual.getExternalToolProcesses(), release::setExternalToolProcesses);
            }
        }
    }

    private static String printComponentName(Component component) {
        if (component == null || isNullOrEmpty(component.getName())) {
            return "New Component";
        }
        return component.getName();
    }

    private static String printReleaseName(Release release) {
        if (release == null || isNullOrEmpty(release.getName())) {
            return "New Release";
        }
        return SW360Utils.getVersionedName(release.getName(), release.getVersion());
    }

    private static String printReleaseFullname(Release release) {
        if (release == null || isNullOrEmpty(release.getName())) {
            return "New Release";
        }
        String vendorName = release.getVendor() != null ? release.getVendor().getShortname() : null;
        return SW360Utils.getReleaseFullname(vendorName, release.getName(), release.getVersion());
    }


}

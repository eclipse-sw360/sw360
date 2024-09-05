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

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.model.Response;
import com.google.common.collect.*;

import org.eclipse.sw360.common.utils.BackendUtils;
import org.eclipse.sw360.commonIO.AttachmentFrontendUtils;
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
import org.eclipse.sw360.datahandler.db.spdx.document.SpdxDocumentDatabaseHandler;
import org.eclipse.sw360.datahandler.entitlement.ComponentModerator;
import org.eclipse.sw360.datahandler.entitlement.ProjectModerator;
import org.eclipse.sw360.datahandler.entitlement.ReleaseModerator;
import org.eclipse.sw360.datahandler.permissions.DocumentPermissions;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentService;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentUsage;
import org.eclipse.sw360.datahandler.thrift.attachments.CheckStatus;
import org.eclipse.sw360.datahandler.thrift.changelogs.ChangeLogs;
import org.eclipse.sw360.datahandler.thrift.changelogs.ChangedFields;
import org.eclipse.sw360.datahandler.thrift.changelogs.Operation;
import org.eclipse.sw360.datahandler.thrift.components.*;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationService;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.datahandler.thrift.packages.PackageService;
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
import org.eclipse.sw360.exporter.ComponentExporter;
import org.eclipse.sw360.mail.MailConstants;
import org.eclipse.sw360.mail.MailUtil;
import org.apache.logging.log4j.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.thrift.TException;
import org.eclipse.sw360.spdx.SpdxBOMImporter;
import org.eclipse.sw360.spdx.SpdxBOMImporterSink;
import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.spdx.library.InvalidSPDXAnalysisException;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
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

    public static final List<EccInformation._Fields> ECC_FIELDS = Arrays.asList(EccInformation._Fields.ECC_STATUS, EccInformation._Fields.AL, EccInformation._Fields.ECCN, EccInformation._Fields.MATERIAL_INDEX_NUMBER, EccInformation._Fields.ECC_COMMENT);

    private final MailUtil mailUtil = new MailUtil();
    private static final ImmutableList<Component._Fields> listOfStringFieldsInCompToTrim = ImmutableList.of(
            Component._Fields.NAME, Component._Fields.DESCRIPTION, Component._Fields.COMPONENT_OWNER,
            Component._Fields.OWNER_ACCOUNTING_UNIT, Component._Fields.OWNER_GROUP, Component._Fields.OWNER_COUNTRY,
            Component._Fields.HOMEPAGE, Component._Fields.MAILINGLIST, Component._Fields.WIKI, Component._Fields.BLOG);
    private static final ImmutableList<Release._Fields> listOfStringFieldsInReleaseToTrim = ImmutableList.of(
            Release._Fields.CPEID, Release._Fields.NAME, Release._Fields.VERSION, Release._Fields.RELEASE_DATE,
            Release._Fields.SOURCE_CODE_DOWNLOADURL, Release._Fields.BINARY_DOWNLOADURL);
    private static final ImmutableList<COTSDetails._Fields> listOfStringFieldsInCOTSDetailsToTrim = ImmutableList.of(
            COTSDetails._Fields.USED_LICENSE, COTSDetails._Fields.LICENSE_CLEARING_REPORT_URL,
            COTSDetails._Fields.OSS_INFORMATION_URL);
    private static final ImmutableList<EccInformation._Fields> listOfStringFieldsInEccInformationToTrim = ImmutableList
            .of(EccInformation._Fields.AL, EccInformation._Fields.ECCN, EccInformation._Fields.ECC_COMMENT,
                    EccInformation._Fields.MATERIAL_INDEX_NUMBER);
    private static final ImmutableList<ClearingInformation._Fields> listOfStringFieldsInClearingInformationToTrim = ImmutableList
            .of(ClearingInformation._Fields.SCANNED, ClearingInformation._Fields.CLEARING_STANDARD,
                    ClearingInformation._Fields.EXTERNAL_URL, ClearingInformation._Fields.COMMENT,
                    ClearingInformation._Fields.REQUEST_ID, ClearingInformation._Fields.ADDITIONAL_REQUEST_INFO,
                    ClearingInformation._Fields.EXTERNAL_SUPPLIER_ID, ClearingInformation._Fields.EVALUATED,
                    ClearingInformation._Fields.PROC_START);

    public ComponentDatabaseHandler(Supplier<CloudantClient> httpClient, String dbName, String attachmentDbName, ComponentModerator moderator, ReleaseModerator releaseModerator, ProjectModerator projectModerator) throws MalformedURLException {
        super(httpClient, dbName, attachmentDbName);
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(httpClient, dbName);

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
        attachmentConnector = new AttachmentConnector(httpClient, attachmentDbName, durationOf(30, TimeUnit.SECONDS));
        DatabaseConnectorCloudant dbChangeLogs = new DatabaseConnectorCloudant(httpClient, DatabaseSettings.COUCH_DB_CHANGE_LOGS);
        this.dbHandlerUtil = new DatabaseHandlerUtil(dbChangeLogs);

        this.bulkDeleteUtil = new BulkDeleteUtil(this, componentRepository, releaseRepository, projectRepository, moderator, releaseModerator,
                attachmentConnector, attachmentDatabaseHandler, dbHandlerUtil);

        // Create the spdx document database handler
        this.spdxDocumentDatabaseHandler = new SpdxDocumentDatabaseHandler(httpClient, DatabaseSettings.COUCH_DB_SPDX);
    }

    public ComponentDatabaseHandler(Supplier<CloudantClient> httpClient, String dbName, String changeLogsDbName, String attachmentDbName, ComponentModerator moderator, ReleaseModerator releaseModerator, ProjectModerator projectModerator) throws MalformedURLException {
        this(httpClient, dbName, attachmentDbName, moderator, releaseModerator, projectModerator);
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(httpClient, changeLogsDbName);
        this.dbHandlerUtil = new DatabaseHandlerUtil(db);
    }


    public ComponentDatabaseHandler(Supplier<CloudantClient> supplier, String dbName, String attachmentDbName) throws MalformedURLException {
        this(supplier, dbName, attachmentDbName, new ComponentModerator(), new ReleaseModerator(), new ProjectModerator());
    }

    public ComponentDatabaseHandler(Supplier<CloudantClient> supplier, String dbName, String changelogsDbName, String attachmentDbName) throws MalformedURLException {
        this(supplier, dbName, attachmentDbName, new ComponentModerator(), new ReleaseModerator(), new ProjectModerator());
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(supplier, changelogsDbName);
        this.dbHandlerUtil = new DatabaseHandlerUtil(db);
    }

    public ComponentDatabaseHandler(Supplier<CloudantClient> httpClient, String dbName, String changeLogsDbName, String attachmentDbName, ThriftClients thriftClients) throws MalformedURLException {
        this(httpClient, dbName, attachmentDbName, new ComponentModerator(thriftClients), new ReleaseModerator(thriftClients), new ProjectModerator(thriftClients));
    }

    private void autosetReleaseClearingState(Release releaseAfter, Release releaseBefore) {
        Optional<Attachment> oldBestCR = getBestClearingReport(releaseBefore);
        Optional<Attachment> newBestCR = getBestClearingReport(releaseAfter);

        Optional<Attachment> oldSecondBestCR = getBestInternalUseScanReport(releaseBefore);
        Optional<Attachment> newSecondBestCR = getBestInternalUseScanReport(releaseAfter);

        long isrCountAfter = evaluateClearingStateForScanAvailable(releaseAfter);
        if (isrCountAfter > 0) {
            releaseAfter.setClearingState(ClearingState.SCAN_AVAILABLE);
        } else {
            releaseAfter.setClearingState(ClearingState.NEW_CLEARING);
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

            if (newSecondBestCR.isPresent() &&  (newSecondBestCR.get().getCheckStatus() == CheckStatus.ACCEPTED)) {
                    releaseAfter.setClearingState(ClearingState.INTERNAL_USE_SCAN_AVAILABLE);
            }

            if (isrCountAfter > 0) {
                releaseAfter.setClearingState(ClearingState.SCAN_AVAILABLE);
            }
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
        releases.forEach(ThriftValidate::ensureEccInformationIsSet);


        // todo: move filling out of department to ReleaseRepository/ReleaseSummary???
        Set<String> userIds = releases.stream().map(Release::getCreatedBy).collect(Collectors.toSet());
        Map<String, User> usersByEmail = ThriftUtils.getIdMap(userRepository.get(userIds));
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
            release.setPermissions(makePermission(release, user).getPermissionMap());
            for (RequestedAction action : RequestedAction.values()) {
                release.getPermissions().put(action, isReleaseActionAllowed(release, user, action));
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

        DatabaseHandlerUtil.addSelectLogs(component, user.getEmail(), attachmentConnector);
    }
    public void addSelectLogs(Release release, User user) {

        DatabaseHandlerUtil.addSelectLogs(release, user.getEmail(), attachmentConnector);
    }

    public Component getComponent(String id, User user) throws SW360Exception {
        Component component = componentRepository.get(id);

        if (component == null) {
            throw fail("Could not fetch component from database! id=" + id);
        }

        // Convert Ids to release summary
        component.setReleases(releaseRepository.makeSummaryWithPermissions(SummaryType.SUMMARY, component.releaseIds, user));
        component.unsetReleaseIds();

        setMainLicenses(component);

        vendorRepository.fillVendor(component);

        // Set permissions
        makePermission(component, user).fillPermissions();

        return component;
    }

    public Component getAccessibleComponent(String id, User user) throws SW360Exception {
        Component component = getComponent(id, user);
        Map<RequestedAction, Boolean> permissions = component.getPermissions();
        if (!permissions.get(RequestedAction.READ)) {
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
        if (!component.isSetMainLicenseIds() && component.isSetReleases()) {
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
        if(isDuplicateUsingVcs(component, true)){
            final AddDocumentRequestSummary addDocumentRequestSummary = new AddDocumentRequestSummary()
                    .setRequestStatus(AddDocumentRequestStatus.DUPLICATE);
            Set<String> duplicates = componentRepository.getComponentIdsByVCS(component.getVcs(), true);
            if (duplicates.size() == 1) {
                duplicates.forEach(addDocumentRequestSummary::setId);
            }
            return addDocumentRequestSummary;

        }else if(isDuplicate(component, true)) {
            final AddDocumentRequestSummary addDocumentRequestSummary = new AddDocumentRequestSummary()
                    .setRequestStatus(AddDocumentRequestStatus.DUPLICATE);
            Set<String> duplicates = componentRepository.getComponentIdsByName(component.getName(), true);
            if (duplicates.size() == 1) {
                duplicates.forEach(addDocumentRequestSummary::setId);
            }
            return addDocumentRequestSummary;
        }
        if(component.getName().trim().length() == 0) {
            return new AddDocumentRequestSummary()
                    .setRequestStatus(AddDocumentRequestStatus.NAMINGERROR);
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
        dbHandlerUtil.addChangeLogs(component, null, user, Operation.CREATE, attachmentConnector,
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
                || verifyLinkedPackages(Collections.emptySet(), CommonUtils.nullToEmptySet(release.getPackageIds()), "")) {
            return new AddDocumentRequestSummary()
                    .setRequestStatus(AddDocumentRequestStatus.INVALID_INPUT);
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
        if (SW360Constants.SPDX_DOCUMENT_ENABLED) {
            try {
                spdxDocumentDatabaseHandler.updateSPDX(user, release, true);
            } catch (TException ex) {
                log.error("Error updateSPDX "+ ex.getMessage());
            }
        }
        final String id = release.getId();

        // Update the underlying component
        component.addToReleaseIds(id);

        if (!component.isSetLanguages()) {
            component.setLanguages(new HashSet<String>());
        }
        if (!component.isSetOperatingSystems()) {
            component.setOperatingSystems(new HashSet<String>());
        }
        if (!component.isSetVendorNames()) {
            component.setVendorNames(new HashSet<String>());
        }
        if (!component.isSetMainLicenseIds()) {
            component.setMainLicenseIds(new HashSet<String>());
        }
        Component oldComponent = component.deepCopy();
        updateReleaseDependentFieldsForComponent(component, release);
        updateModifiedFields(component, user.getEmail());
        componentRepository.update(component);
        // update linked packages
        updateLinkedPackages(Collections.emptySet(), CommonUtils.nullToEmptySet(release.getPackageIds()), id, user);
        sendMailNotificationsForNewRelease(release, user.getEmail());
        dbHandlerUtil.addChangeLogs(release, null, user.getEmail(), Operation.CREATE, attachmentConnector,
                Lists.newArrayList(), null, null);
        dbHandlerUtil.addChangeLogs(component, oldComponent, user.getEmail(), Operation.UPDATE,
                attachmentConnector, Lists.newArrayList(), release.getId(), Operation.RELEASE_CREATE);
        return new AddDocumentRequestSummary()
                .setRequestStatus(AddDocumentRequestStatus.SUCCESS)
                .setId(id);
    }

    private boolean isDuplicate(Component component, boolean caseInsenstive){
        return isDuplicate(component.getName(), caseInsenstive);
    }

    private boolean isDuplicate(Release release){
        return isDuplicate(release.getName(), release.getVersion());
    }

    private boolean isDuplicate(String componentName, boolean caseInsenstive) {
        if (isNullEmptyOrWhitespace(componentName)) {
            return false;
        }
        Set<String> duplicates = componentRepository.getComponentIdsByName(componentName, caseInsenstive);
        return duplicates.size()>0;
    }

    private boolean isDuplicateUsingVcs(Component component, boolean caseInsenstive){
        return isDuplicateUsingVcs(component.getVcs(), caseInsenstive);
    }

    private boolean isDuplicateUsingVcs(String vcsUrl, boolean caseInsenstive){
        if (isNullEmptyOrWhitespace(vcsUrl)) {
            return false;
        }
        Set<String> duplicates = componentRepository.getComponentIdsByVCS(vcsUrl, caseInsenstive);
        return duplicates.size()>0;
    }

    private boolean isDuplicate(String releaseName, String releaseVersion) {
        if (isNullEmptyOrWhitespace(releaseName)) {
            return false;
        }
        List<Release> duplicates = releaseRepository.searchByNameAndVersion(releaseName, releaseVersion, true);
        return duplicates.size()>0;
    }

    private void isDuplicateComponent(List<String> componentNames, boolean caseInsenstive) {
        for (String name : componentNames) {
            if(!isDuplicate(name, caseInsenstive))
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
            if (!component.isSetLanguages()) {
                component.setLanguages(new HashSet<String>());
            }
            component.languages.addAll(nullToEmptySet(release.languages));

            if (!component.isSetOperatingSystems()) {
                component.setOperatingSystems(new HashSet<String>());
            }
            component.operatingSystems.addAll(nullToEmptySet(release.operatingSystems));

            if(!component.isSetSoftwarePlatforms()) {
                component.setSoftwarePlatforms(new HashSet<String>());
            }
            component.softwarePlatforms.addAll(nullToEmptySet(release.softwarePlatforms));

            if (!component.isSetVendorNames()) {
                component.setVendorNames(new HashSet<String>());
            }
            if (release.vendor != null)
                component.vendorNames.add(release.vendor.getShortname());
            else if (!isNullOrEmpty(release.vendorId)) {
                Vendor vendor = getVendor(release.vendorId);
                component.vendorNames.add(vendor.getShortname());
            }

            if (!component.isSetMainLicenseIds()) component.setMainLicenseIds(new HashSet<String>());
            if (release.isSetMainLicenseIds()) {
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
        if (name == null || name.isEmpty()) {
            return RequestStatus.NAMINGERROR;
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
        DatabaseHandlerUtil.saveAttachmentInFileSystem(attachmentConnector, actual.getAttachments(),
                component.getAttachments(), user.getEmail(), component.getId());
        if (changeWouldResultInDuplicate(actual, component)) {
            return RequestStatus.DUPLICATE;
        } else if (duplicateAttachmentExist(component)) {
            return RequestStatus.DUPLICATE_ATTACHMENT;
        } else if (!isDependenciesExistInComponent(component)){
            return RequestStatus.INVALID_INPUT;
        } else if (makePermission(actual, user).isActionAllowed(RequestedAction.WRITE) || forceUpdate) {
            // Nested releases and attachments should not be updated by this method
            boolean isComponentNameChanged = false;
            if (actual.isSetReleaseIds()) {
                component.setReleaseIds(actual.getReleaseIds());
                isComponentNameChanged = !component.getName().equals(actual.getName());
            }

            copyFields(actual, component, ThriftUtils.IMMUTABLE_OF_COMPONENT);
            component.setAttachments(getAllAttachmentsToKeep(toSource(actual), actual.getAttachments(), component.getAttachments()));
            recomputeReleaseDependentFields(component, null);

            List<ChangeLogs> referenceDocLogList = new LinkedList<>();
            Set<Attachment> attachmentsAfter = component.getAttachments();
            Set<Attachment> attachmentsBefore = actual.getAttachments();
            DatabaseHandlerUtil.populateChangeLogsForAttachmentsDeleted(attachmentsBefore, attachmentsAfter,
                    referenceDocLogList, user.getEmail(), component.getId(), Operation.COMPONENT_UPDATE,
                    attachmentConnector, false);

            updateComponentInternal(component, actual, user);

            if (isComponentNameChanged) {
                updateComponentDependentFieldsForRelease(component,referenceDocLogList,user.getEmail());
            }

            if (component.getComponentType() != null && !component.getComponentType().equals(actual.getComponentType()) && !ComponentType.OSS.equals(component.getComponentType())) {
                updateEccStatusForRelease(component);
            }

            dbHandlerUtil.addChangeLogs(component, actual, user.getEmail(), Operation.UPDATE, attachmentConnector,
                    referenceDocLogList, null, null);
        } else {
            return moderator.updateComponent(component, user);
        }
        return RequestStatus.SUCCESS;

    }

    private boolean isDependenciesExistInComponent(Component component) {
        boolean isValidDependentIds = true;
        if (component.isSetReleaseIds()) {
            Set<String> releaseIds = component.getReleaseIds();
            isValidDependentIds = DatabaseHandlerUtil.isAllIdInSetExists(releaseIds, releaseRepository);
        }

        if (isValidDependentIds && component.isSetDefaultVendorId()) {
            isValidDependentIds = DatabaseHandlerUtil.isAllIdInSetExists(Sets.newHashSet(component.getDefaultVendorId()), vendorRepository);
        }
        return isValidDependentIds;
    }

    private boolean isDependenciesExistsInRelease(Release release) {
        boolean isValidDependentIds = true;
        if (release.isSetComponentId()) {
            String componentId = release.getComponentId();
            isValidDependentIds = DatabaseHandlerUtil.isAllIdInSetExists(Sets.newHashSet(componentId), componentRepository);
        }

        if (isValidDependentIds && release.isSetReleaseIdToRelationship()) {
            Set<String> releaseIds = release.getReleaseIdToRelationship().keySet();
            isValidDependentIds = DatabaseHandlerUtil.isAllIdInSetExists(Sets.newHashSet(releaseIds), releaseRepository);
        }

        if (isValidDependentIds && release.isSetVendorId()) {
            String vendorId = release.getVendorId();
            isValidDependentIds = DatabaseHandlerUtil.isAllIdInSetExists(Sets.newHashSet(vendorId), vendorRepository);
        }

        if (isValidDependentIds && release.isSetPackageIds()) {
            Set<String> pacakgeIds = release.getPackageIds();
            isValidDependentIds = DatabaseHandlerUtil.isAllIdInSetExists(pacakgeIds, packageRepository);
        }
        return isValidDependentIds;
    }

    private void updateComponentDependentFieldsForRelease(Component component, List<ChangeLogs> referenceDocLogList,
            String userEdited) {
        String name = component.getName();
        for (Release release : releaseRepository.getReleasesFromComponentId(component.getId())) {
            ChangeLogs changeLog = DatabaseHandlerUtil.initChangeLogsObj(release, userEdited, component.getId(),
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
        if (before.getName().equals(after.getName())) {
            // sth else was changed, not one of the duplication relevant properties
            return false;
        }

        return isDuplicate(after, false);
    }

    private boolean duplicateAttachmentExist(Component component) {
        if(component.attachments != null && !component.attachments.isEmpty()) {
            return AttachmentConnector.isDuplicateAttachment(component.attachments);
        }
        return false;
    }


    private void updateComponentInternal(Component updated, Component current, User user) {
        updateModifiedFields(updated, user.getEmail());
        // Update the database with the component
        componentRepository.update(updated);

        //clean up attachments in database
        attachmentConnector.deleteAttachmentDifference(current.getAttachments(), updated.getAttachments());
        sendMailNotificationsForComponentUpdate(updated, user.getEmail());
    }

    private void prepareComponent(Component component) throws SW360Exception {
        // Prepare component for database
        ThriftValidate.prepareComponent(component);

        //add sha1 to attachments if necessary
        if(component.isSetAttachments()) {
            attachmentConnector.setSha1ForAttachments(component.getAttachments());
        }
    }

    public RequestSummary updateComponents(Set<Component> components, User user) throws SW360Exception {
        return RepositoryUtils.doBulk(prepareComponents(components), user, componentRepository);
    }


    public RequestStatus updateComponentFromAdditionsAndDeletions(Component componentAdditions, Component componentDeletions, User user){

        try {
            Component component= getComponent(componentAdditions.getId(), user);
            component = moderator.updateComponentFromModerationRequest(component, componentAdditions, componentDeletions);
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
        Component mergeTargetOriginal = mergeTarget.deepCopy();

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
        dbHandlerUtil.addChangeLogs(mergeTarget, mergeTargetOriginal, sessionUser.getEmail(), Operation.UPDATE,
                attachmentConnector, Lists.newArrayList(), null, Operation.MERGE_COMPONENT);
        dbHandlerUtil.addChangeLogs(null, mergeSource, sessionUser.getEmail(), Operation.DELETE, null,
                Lists.newArrayList(), mergeTargetId, Operation.MERGE_COMPONENT);
        return RequestStatus.SUCCESS;
    }

    private boolean isComponentUnderModeration(String componentSourceId) throws TException {
        ModerationService.Iface moderationClient = new ThriftClients().makeModerationClient();
        List<ModerationRequest> sourceModerationRequests = moderationClient.getModerationRequestByDocumentId(componentSourceId);
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
                    mergeTarget.addToModerators(mergeSource.getCreatedBy());
                }
            } else {
                // creator of the source component has been selected. Add creator of target component to list of moderators.

                // remember creator otherwise it is overwritten
                String creator = mergeTarget.getCreatedBy();

                // merge
                mergeTarget.setModerators(mergeSelection.getModerators());
                if(!isNullOrEmpty(mergeTarget.getCreatedBy())) {
                    mergeTarget.addToModerators(mergeTarget.getCreatedBy());
                }
            }
        }

        // Handle other fields
        copyFields(mergeSelection, mergeTarget, ImmutableSet.<Component._Fields>builder()
                .add(Component._Fields.NAME)
                .add(Component._Fields.CREATED_ON)
                .add(Component._Fields.CREATED_BY)
                .add(Component._Fields.CATEGORIES)
                .add(Component._Fields.COMPONENT_TYPE)
                .add(Component._Fields.DEFAULT_VENDOR_ID)
                .add(Component._Fields.HOMEPAGE)
                .add(Component._Fields.BLOG)
                .add(Component._Fields.WIKI)
                .add(Component._Fields.MAILINGLIST)
                .add(Component._Fields.DESCRIPTION)
                .add(Component._Fields.EXTERNAL_IDS)
                .add(Component._Fields.ADDITIONAL_DATA)
                .add(Component._Fields.COMPONENT_OWNER)
                .add(Component._Fields.OWNER_ACCOUNTING_UNIT)
                .add(Component._Fields.OWNER_GROUP)
                .add(Component._Fields.OWNER_COUNTRY)
                .add(Component._Fields.MODERATORS)
                .add(Component._Fields.SUBSCRIBERS)
                .add(Component._Fields.ROLES)
                .build());
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
	            mergeTarget.addToAttachments(a);
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
        releaseIds.forEach(mergeTarget::addToReleaseIds);
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
                Release releaseBefore = r.deepCopy();
                if (srcComponentReleases.contains(r) && targetComponentReleaseVersions.contains(r.getVersion())) {
                        r.setVersion(r.getVersion() + "_conflict (" + r.getId() + ")");
                }
                r.setComponentId(mergeTarget.getId());
                r.setName(mergeSelection.getName());
                updateModifiedFields(r, userEmail);
                dbHandlerUtil.addChangeLogs(r, releaseBefore, userEmail, Operation.UPDATE,
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

    public RequestStatus updateRelease(Release release, User user, Iterable<Release._Fields> immutableFields) throws SW360Exception {
        return updateRelease(release, user, immutableFields, false);
    }

    public RequestStatus updateRelease(Release release, User user, Iterable<Release._Fields> immutableFields, boolean forceUpdate) throws SW360Exception {
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
        DatabaseHandlerUtil.saveAttachmentInFileSystem(attachmentConnector, actual.getAttachments(),
                release.getAttachments(), user.getEmail(), release.getId());
        if (actual.equals(release)) {
            return RequestStatus.SUCCESS;
        } else if (duplicateAttachmentExist(release)) {
            return RequestStatus.DUPLICATE_ATTACHMENT;
        } else if (changeWouldResultInDuplicate(actual, release)) {
            return RequestStatus.DUPLICATE;
        } else if (!isDependenciesExistsInRelease(release)
                || verifyLinkedPackages(CommonUtils.nullToEmptySet(actual.getPackageIds()), CommonUtils.nullToEmptySet(release.getPackageIds()), release.getId())) {
            return RequestStatus.INVALID_INPUT;
        } else {
            DocumentPermissions<Release> permissions = makePermission(actual, user);
            boolean hasChangesInEccFields = hasChangesInEccFields(release, actual);

            if ((hasChangesInEccFields && permissions.isActionAllowed(RequestedAction.WRITE_ECC))
                    || (!hasChangesInEccFields && permissions.isActionAllowed(RequestedAction.WRITE))
                    || forceUpdate) {

                if (!hasChangesInEccFields && hasEmptyEccFields(release)) {
                    autosetEccFieldsForReleaseWithDownloadUrl(release);
                }

                copyFields(actual, release, immutableFields);

                autosetReleaseClearingState(release, actual);
                if (hasChangesInEccFields) {
                    autosetEccUpdaterInfo(release, user);
                }
                release.setAttachments(
                        getAllAttachmentsToKeep(toSource(actual), actual.getAttachments(), release.getAttachments()));

                List<ChangeLogs> referenceDocLogList = new LinkedList<>();
                Set<Attachment> attachmentsAfter = release.getAttachments();
                Set<Attachment> attachmentsBefore = actual.getAttachments();
                DatabaseHandlerUtil.populateChangeLogsForAttachmentsDeleted(attachmentsBefore, attachmentsAfter,
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
                attachmentConnector.deleteAttachmentDifference(nullToEmptySet(actual.getAttachments()),
                        nullToEmptySet(release.getAttachments()));
                // update linked packages
                updateLinkedPackages(CommonUtils.nullToEmptySet(actual.getPackageIds()), CommonUtils.nullToEmptySet(release.getPackageIds()), release.getId(), user);
                sendMailNotificationsForReleaseUpdate(release, user.getEmail());
                dbHandlerUtil.addChangeLogs(release, actual, user.getEmail(), Operation.UPDATE,
                        attachmentConnector, referenceDocLogList, null, null);
                dbHandlerUtil.addChangeLogs(updatedComponent, oldComponent, user.getEmail(), Operation.UPDATE,
                        attachmentConnector, Lists.newArrayList(), release.getId(), Operation.RELEASE_UPDATE);
                Runnable clearingRequestRunnable = addCrCommentForAttachmentUpdatesInRelease(actual, CommonUtils.nullToEmptySet(release.getAttachments()), user);
                Thread crUpdateThread = new Thread(clearingRequestRunnable);
                crUpdateThread.start();
            } else {
                if (hasChangesInEccFields) {
                    return releaseModerator.updateReleaseEccInfo(release, user);
                } else {
                    return releaseModerator.updateRelease(release, user);
                }
            }

            return RequestStatus.SUCCESS;
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
            if (!originalAttachmentId.equals(updatedAttachmentId)) {
                // fetch all the projects associated with this release and collect the Clearing request Ids
                final Set<Project> usingProjects = projectRepository.searchByReleaseId(release.getId());
                final Set<String> crIds = CommonUtils.nullToEmptySet(usingProjects).stream()
                        .filter(proj -> CommonUtils.isNotNullEmptyOrWhitespace(proj.getClearingRequestId()))
                        .map(Project::getClearingRequestId).collect(Collectors.toSet());
                if (crIds.size() > 0) {
                    Set<String> added = Sets.difference(updatedAttachmentId, originalAttachmentId);
                    Set<String> removed = Sets.difference(originalAttachmentId, updatedAttachmentId);
                    StringBuilder commentText = new StringBuilder("Attachment(s) updated for the release: <b>")
                            .append(SW360Utils.printFullname(release)).append("</b> (").append(release.getId()).append(")");
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
                        .append(ThriftEnumUtils.enumToShortString(att.getAttachmentType())).toString())
                .collect(Collectors.toSet());
    }

    private void setMainlineState(Release updated, User user, Release current) {
        boolean isMainlineStateDisabled = !(BackendUtils.MAINLINE_STATE_ENABLED_FOR_USER
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
        if (release.attachments != null && !release.attachments.isEmpty()) {
            return AttachmentConnector.isDuplicateAttachment(release.attachments);
        }
        return false;
    }

    private void deleteAttachmentUsagesOfUnlinkedReleases(Release updated, Release actual) throws SW360Exception {
        Source usedBy = Source.releaseId(updated.getId());
        Set<String> updatedLinkedReleaseIds = nullToEmptyMap(updated.getReleaseIdToRelationship()).keySet();
        Set<String> actualLinkedReleaseIds = nullToEmptyMap(actual.getReleaseIdToRelationship()).keySet();
        deleteAttachmentUsagesOfUnlinkedReleases(usedBy, updatedLinkedReleaseIds, actualLinkedReleaseIds);
    }

    public boolean hasChangesInEccFields(Release release, Release actual) {
        ensureEccInformationIsSet(release);
        ensureEccInformationIsSet(actual);
        Function<EccInformation._Fields, Boolean> fieldChanged = f -> {
            Object changedValue = release.getEccInformation().getFieldValue(f);
            Object originalValue = actual.getEccInformation().getFieldValue(f);

            return !((changedValue == originalValue)
                    || (changedValue != null && changedValue.equals(originalValue))
                    || ("".equals(changedValue) && originalValue == null)
                    || (changedValue == null && "".equals(originalValue)));
        };
        return ECC_FIELDS
                .stream().map(fieldChanged)
                .reduce(false, Boolean::logicalOr);
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
                if (DatabaseHandlerUtil.AUTO_SET_ECC_STATUS) {
                    eccInfo.setEccStatus(ECCStatus.APPROVED);
                }
                eccInfo.setAssessmentDate(SW360Utils.getCreatedOn());
            } else {
                log.warn("Could not set ECC options for unmodified OSS because download url is not valid: " + url);
            }
        }
    }

    private void prepareRelease(Release release) throws SW360Exception {
        // Prepare release for database
        ThriftValidate.prepareRelease(release);

        //add sha1 to attachments if necessary
        if(release.isSetAttachments()) {
            attachmentConnector.setSha1ForAttachments(release.getAttachments());
        }
    }

    public RequestSummary updateReleases(Collection<Release> releases, User user, boolean allowUpdate) throws SW360Exception {
        List<Release> storedReleases = prepareReleases(releases);

        RequestSummary requestSummary = new RequestSummary();
        if (allowUpdate || PermissionUtils.isAdmin(user)) {
            // Prepare component for database
            final List<Response> documentOperationResults = componentRepository.executeBulk(storedReleases);

            if (!documentOperationResults.isEmpty()) {

                final List<Component> componentList = componentRepository.get(storedReleases
                        .stream()
                        .map(Release::getComponentId)
                        .collect(Collectors.toSet()));

                final Map<String, Component> componentsById = ThriftUtils.getIdMap(componentList);

                for (Release storedRelease : storedReleases) {
                    final Component component = componentsById.get(storedRelease.getComponentId());
                    component.addToReleaseIds(storedRelease.getId());
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
        return RepositoryUtils.doBulk(prepareReleases(releases), user, releaseRepository);
    }

    public RequestStatus updateReleaseFromAdditionsAndDeletions(Release releaseAdditions, Release releaseDeletions, User user) {

        try {
            Release release = getRelease(releaseAdditions.getId(), user);
            release = releaseModerator.updateReleaseFromModerationRequest(release, releaseAdditions, releaseDeletions);
            return updateRelease(release, user, ThriftUtils.IMMUTABLE_OF_RELEASE);
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
        PackageService.Iface packageClient = new ThriftClients().makePackageClient();
        if (CommonUtils.isNotEmpty(addedPackageIds)) {
            try {
                long addedCount = addedPackageIds.size();
                List<Package> addedPackages = packageClient.getPackageByIds(addedPackageIds);
                Predicate<Package> orphanReleaseFilter = pkg -> CommonUtils.isNullEmptyOrWhitespace(pkg.getReleaseId());
                Predicate<Package> linkedReleaseFilter = pkg -> releaseId.equals(pkg.getReleaseId());
                long orphanCount = addedPackages.stream().filter(orphanReleaseFilter).count();
                long linkedCount = addedPackages.stream().filter(linkedReleaseFilter).count();
                if (CommonUtils.isNotNullEmptyOrWhitespace(releaseId) && addedCount != orphanCount) {
                    return addedCount != linkedCount;
                } else {
                    return addedCount != orphanCount;
                }
            } catch (TException e) {
                log.error(String.format("An error occured while updating linked packages of release: %s", releaseId), e.getCause());
                return true;
            }
        }
        return false;
    }

    private void updateLinkedPackages(Set<String> currentPackageIds, Set<String> updatedPackageIds, String releaseId, User user) throws SW360Exception {
        Set<String> removedPacakgeIds = Sets.difference(currentPackageIds, updatedPackageIds);
        Set<String> addedPacakgeIds = Sets.difference(updatedPackageIds, currentPackageIds);
        PackageService.Iface packageClient = new ThriftClients().makePackageClient();
        try {
            if (CommonUtils.isNotEmpty(removedPacakgeIds)) {
                List<Package> removedPackages = packageRepository.get(removedPacakgeIds);
                for (Package pkg : removedPackages) {
                    String relId = pkg.getReleaseId();
                    // update the package, if it contains linked release Id
                    if (CommonUtils.isNotNullEmptyOrWhitespace(relId) && releaseId.equals(relId)) {
                        pkg.unsetReleaseId();
                        RequestStatus status = packageClient.updatePackage(pkg, user);
                        log.info(String.format("Unlinked package <%s> from release <%s>, Unlinking status: <%s>", pkg.getId(), releaseId, status.name()));
                    }
                }
            }
            if (CommonUtils.isNotEmpty(addedPacakgeIds)) {
                List<Package> addedPackages = packageClient.getPackageByIds(addedPacakgeIds);
                for (Package pkg : addedPackages) {
                    String relId = pkg.getReleaseId();
                    // update only orphan packages
                    if (CommonUtils.isNullEmptyOrWhitespace(relId)) {
                        pkg.setReleaseId(releaseId);
                        RequestStatus status = packageClient.updatePackage(pkg, user);
                        log.info(String.format("Linked package <%s> to release <%s>, Linking status: <%s>", pkg.getId(), releaseId, status.name()));
                    } else if (!relId.equals(releaseId)) {
                        log.warn(String.format("Linked-ReleasId <%s> in Package <%s>, and Linked-PackageId <%s> in Release <%s> association is incorrect",
                                relId, pkg.getId(), pkg.getId(), releaseId));
                    }
                }
            }
        } catch (TException e) {
            log.error(String.format("An error occured while updating linked packages of release: %s", releaseId), e.getCause());
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
        Release mergeTargetOriginal = mergeTarget.deepCopy();
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

            // Finally we can delete the source component
            updateParentComponent(mergeSource, sessionUser);

            deleteRelease(mergeSourceId, sessionUser);

        } catch(Exception e) {
            log.error("Cannot merge release [" + mergeSource.getId() + "] into [" + mergeTarget.getId() + "].", e);
            return RequestStatus.FAILURE;
        }

        dbHandlerUtil.addChangeLogs(mergeTarget, mergeTargetOriginal, sessionUser.getEmail(), Operation.UPDATE,
                attachmentConnector, Lists.newArrayList(), null, Operation.MERGE_RELEASE);
        dbHandlerUtil.addChangeLogs(null, mergeSource, sessionUser.getEmail(), Operation.DELETE, null,
                Lists.newArrayList(), mergeTargetId, Operation.MERGE_RELEASE);
        return RequestStatus.SUCCESS;
    }

    private boolean isReleaseUnderModeration(String releaseId) throws TException {
        ModerationService.Iface moderationClient = new ThriftClients().makeModerationClient();
        List<ModerationRequest> moderationRequests = moderationClient.getModerationRequestByDocumentId(releaseId);
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
                    mergeTarget.addToModerators(mergeSource.getCreatedBy());
                }
            } else {
                // creator of the source component has been selected. Add creator of target component to list of moderators.
                mergeTarget.setModerators(mergeSelection.getModerators());
                if(!isNullOrEmpty(mergeTarget.getCreatedBy())) {
                    mergeTarget.addToModerators(mergeTarget.getCreatedBy());
                }
            }
        }

        // Handle default fields
        copyFields(mergeSelection, mergeTarget, ImmutableSet.<Release._Fields>builder()
            .add(Release._Fields.VENDOR_ID)
            .add(Release._Fields.NAME)
            .add(Release._Fields.VERSION)
            .add(Release._Fields.LANGUAGES)
            .add(Release._Fields.OPERATING_SYSTEMS)
            .add(Release._Fields.CPEID)
            .add(Release._Fields.SOFTWARE_PLATFORMS)
            .add(Release._Fields.RELEASE_DATE)
            .add(Release._Fields.MAIN_LICENSE_IDS)
            .add(Release._Fields.SOURCE_CODE_DOWNLOADURL)
            .add(Release._Fields.BINARY_DOWNLOADURL)
            .add(Release._Fields.MAINLINE_STATE)
            .add(Release._Fields.CREATED_ON)
            .add(Release._Fields.CREATED_BY)
            .add(Release._Fields.CONTRIBUTORS)
            .add(Release._Fields.MODERATORS)
            .add(Release._Fields.SUBSCRIBERS)
            .add(Release._Fields.REPOSITORY)
            .add(Release._Fields.ROLES)
            .add(Release._Fields.EXTERNAL_IDS)
            .add(Release._Fields.ADDITIONAL_DATA)
            .add(Release._Fields.RELEASE_ID_TO_RELATIONSHIP)
            .build());

        // Remove self links
        if(mergeTarget.isSetReleaseIdToRelationship()) {
            mergeTarget.getReleaseIdToRelationship().remove(mergeTarget.getId());
        }

        // Handle clearing information
        copyFields(mergeSelection.getClearingInformation(), mergeTarget.getClearingInformation(), ImmutableSet.<ClearingInformation._Fields>builder()
            .add(ClearingInformation._Fields.BINARIES_ORIGINAL_FROM_COMMUNITY)
            .add(ClearingInformation._Fields.BINARIES_SELF_MADE)
            .add(ClearingInformation._Fields.COMPONENT_LICENSE_INFORMATION)
            .add(ClearingInformation._Fields.SOURCE_CODE_DELIVERY)
            .add(ClearingInformation._Fields.SOURCE_CODE_ORIGINAL_FROM_COMMUNITY)
            .add(ClearingInformation._Fields.SOURCE_CODE_TOOL_MADE)
            .add(ClearingInformation._Fields.SOURCE_CODE_SELF_MADE)
            .add(ClearingInformation._Fields.SCREENSHOT_OF_WEB_SITE)
            .add(ClearingInformation._Fields.FINALIZED_LICENSE_SCAN_REPORT)
            .add(ClearingInformation._Fields.LICENSE_SCAN_REPORT_RESULT)
            .add(ClearingInformation._Fields.LEGAL_EVALUATION)
            .add(ClearingInformation._Fields.LICENSE_AGREEMENT)
            .add(ClearingInformation._Fields.SCANNED)
            .add(ClearingInformation._Fields.COMPONENT_CLEARING_REPORT)
            .add(ClearingInformation._Fields.CLEARING_STANDARD)
            .add(ClearingInformation._Fields.EXTERNAL_URL)
            .add(ClearingInformation._Fields.COMMENT)
            .add(ClearingInformation._Fields.REQUEST_ID)
            .add(ClearingInformation._Fields.ADDITIONAL_REQUEST_INFO)
            .add(ClearingInformation._Fields.PROC_START)
            .add(ClearingInformation._Fields.EVALUATED)
            .add(ClearingInformation._Fields.EXTERNAL_SUPPLIER_ID)
            .add(ClearingInformation._Fields.COUNT_OF_SECURITY_VN)
            .build());

        // Handle ECC information
        copyFields(mergeSelection.getEccInformation(), mergeTarget.getEccInformation(), ImmutableSet.<EccInformation._Fields>builder()
            .add(EccInformation._Fields.ECC_STATUS)
            .add(EccInformation._Fields.ECC_COMMENT)
            .add(EccInformation._Fields.AL)
            .add(EccInformation._Fields.ECCN)
            .add(EccInformation._Fields.MATERIAL_INDEX_NUMBER)
            .add(EccInformation._Fields.ASSESSOR_CONTACT_PERSON)
            .add(EccInformation._Fields.ASSESSOR_DEPARTMENT)
            .add(EccInformation._Fields.ASSESSMENT_DATE)
            .build());

        // Handle COTS information
        copyFields(mergeSelection.getCotsDetails(), mergeTarget.getCotsDetails(), ImmutableSet.<COTSDetails._Fields>builder()
            .add(COTSDetails._Fields.USAGE_RIGHT_AVAILABLE)
            .add(COTSDetails._Fields.COTS_RESPONSIBLE)
            .add(COTSDetails._Fields.CLEARING_DEADLINE)
            .add(COTSDetails._Fields.LICENSE_CLEARING_REPORT_URL)
            .add(COTSDetails._Fields.USED_LICENSE)
            .add(COTSDetails._Fields.CONTAINS_OSS)
            .add(COTSDetails._Fields.OSS_CONTRACT_SIGNED)
            .add(COTSDetails._Fields.OSS_INFORMATION_URL)
            .add(COTSDetails._Fields.SOURCE_CODE_AVAILABLE)
            .build());
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
            mergeTarget.addToAttachments(a);
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
            attachmentConnector.deleteAttachmentDifference(actual.getAttachments(), release.getAttachments());
        }
        if(sendmail) {
            sendMailNotificationsForReleaseUpdate(release, user.getEmail());
        }
    }

    private void updateReleaseReferencesInProjects(String mergeTargetId, String mergeSourceId, User sessionUser) throws TException {
        ProjectService.Iface projectClient = new ThriftClients().makeProjectClient();

        final String userEmail = sessionUser.getEmail();
        Set<Project> projects = projectClient.searchByReleaseId(mergeSourceId, sessionUser);
        for(Project project : projects) {
            // retrieve full document, other method only retrieves summary
            project = projectClient.getProjectById(project.getId(), sessionUser);
            Project projectBefore=project.deepCopy();
            ProjectReleaseRelationship relationship = project.getReleaseIdToUsage().remove(mergeSourceId);
            // if the target release is also linked, keep this one, do not overwrite
            if(!project.getReleaseIdToUsage().containsKey(mergeTargetId)) {
                project.putToReleaseIdToUsage(mergeTargetId, relationship);
            }
            updateModifiedFields(project, userEmail);
            projectClient.updateProject(project, sessionUser);

            dbHandlerUtil.addChangeLogs(project, projectBefore, userEmail, Operation.UPDATE,
                    attachmentConnector, Lists.newArrayList(), mergeTargetId, Operation.MERGE_RELEASE);
        }
    }

    private void updateReleaseReferencesInAttachmentUsages(String mergeTargetId, String mergeSourceId) throws TException {
        AttachmentService.Iface attachmentClient = new ThriftClients().makeAttachmentClient();

        List<AttachmentUsage> usages = attachmentClient.getAttachmentUsagesByReleaseId(mergeSourceId);
        for(AttachmentUsage usage : usages) {
            if(usage.getOwner().isSetReleaseId() && usage.getOwner().getReleaseId().equals(mergeSourceId)) {
                usage.getOwner().setReleaseId(mergeTargetId);
            }
            if(usage.getUsedBy().isSetReleaseId() && usage.getUsedBy().getReleaseId().equals(mergeSourceId)) {
                usage.getUsedBy().setReleaseId(mergeTargetId);
            }
            attachmentClient.updateAttachmentUsage(usage);
        }
    }

    private void updateReleaseReferencesInReleases(String mergeTargetId, String mergeSourceId, User sessionUser) throws SW360Exception {
        List<Release> releases = getReferencingReleases(mergeSourceId);
        for(Release release : releases) {
            Release releaseBefore =release.deepCopy();
            ReleaseRelationship relationship = release.getReleaseIdToRelationship().remove(mergeSourceId);
            // if the target release is also linked, keep this one, do not overwrite
            if(!release.getReleaseIdToRelationship().containsKey(mergeTargetId)) {
                release.putToReleaseIdToRelationship(mergeTargetId, relationship);
            }
            updateReleaseCompletely(release, sessionUser, false, false, false);
            dbHandlerUtil.addChangeLogs(release, releaseBefore, sessionUser.getEmail(), Operation.UPDATE,
                    attachmentConnector, Lists.newArrayList(), mergeTargetId, Operation.MERGE_RELEASE);
        }
    }

    private void updateReleaseReferencesInVulnerabilities(String mergeTargetId, String mergeSourceId, User sessionUser) throws TException {
        VulnerabilityService.Iface vulnerabilityService = new ThriftClients().makeVulnerabilityClient();

        List<ReleaseVulnerabilityRelation> relations = vulnerabilityService.getReleaseVulnerabilityRelationsByReleaseId(mergeSourceId, sessionUser);
        for(ReleaseVulnerabilityRelation relation : relations) {
            if(relation.isSetReleaseId() && relation.getReleaseId().equals(mergeSourceId)) {
                ReleaseVulnerabilityRelation relationBefore = relation.deepCopy();
                relation.setReleaseId(mergeTargetId);
                vulnerabilityService.updateReleaseVulnerabilityRelation(relation, sessionUser);
                dbHandlerUtil.addChangeLogs(relation, relationBefore, sessionUser.getEmail(), Operation.UPDATE,
                        attachmentConnector, Lists.newArrayList(), mergeTargetId, Operation.MERGE_RELEASE);
            }
        }
    }

    private void updateReleaseReferencesInProjectRatings(String mergeTargetId, String mergeSourceId, User sessionUser) throws TException {
        VulnerabilityService.Iface vulnerabilityService = new ThriftClients().makeVulnerabilityClient();

        List<ProjectVulnerabilityRating> ratings = vulnerabilityService.getProjectVulnerabilityRatingsByReleaseId(mergeSourceId, sessionUser);
        for(ProjectVulnerabilityRating rating : ratings) {
            ProjectVulnerabilityRating ratingBefore = rating.deepCopy();
            for(Map<String, List<VulnerabilityCheckStatus>> map : rating.getVulnerabilityIdToReleaseIdToStatus().values()) {
                List<VulnerabilityCheckStatus> list = map.remove(mergeSourceId);
                // if the target release is also linked, keep this one, do not overwrite
                if(list != null && !map.containsKey(mergeTargetId)) {
                    map.put(mergeTargetId, list);
                }
            }
            vulnerabilityService.updateProjectVulnerabilityRating(rating, sessionUser);
            dbHandlerUtil.addChangeLogs(rating, ratingBefore, sessionUser.getEmail(), Operation.UPDATE,
                    attachmentConnector, Lists.newArrayList(), mergeTargetId, Operation.MERGE_RELEASE);
        }
    }

    private void updateParentComponent(Release release, User sessionUser) throws SW360Exception {
        Component component = getComponent(release.getComponentId(), sessionUser);
        Component componentBefore = component.deepCopy();
        Set<String> releaseIds = nullToEmptyList(component.getReleases()).stream().map(Release::getId).collect(Collectors.toSet());
        releaseIds.remove(release.getId());
        component.setReleaseIds(releaseIds);

        recomputeReleaseDependentFields(component, null);
        updateComponentCompletely(component, sessionUser);

        dbHandlerUtil.addChangeLogs(component, componentBefore, sessionUser.getEmail(), Operation.UPDATE,
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
        if (releaseIds!=null && releaseIds.size()>0) return RequestStatus.IN_USE;
        if (checkIfInUse(releaseIds)) return RequestStatus.IN_USE;


        if (makePermission(component, user).isActionAllowed(RequestedAction.DELETE) || forceDelete) {

            for (Release release : releaseRepository.get(nullToEmptySet(component.releaseIds))) {
                component = removeReleaseAndCleanUp(release, user);
            }

            // Remove the component with attachments
            attachmentConnector.deleteAttachments(component.getAttachments());
            attachmentDatabaseHandler.deleteUsagesBy(Source.componentId(id));
            componentRepository.remove(component);
            moderator.notifyModeratorOnDelete(id);
            dbHandlerUtil.addChangeLogs(null, component, user.getEmail(), Operation.DELETE, attachmentConnector,
                    Lists.newArrayList(), null, null);
            return RequestStatus.SUCCESS;
        } else {
            return moderator.deleteComponent(component, user);
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
        if (releaseIds != null && releaseIds.size() > 0) {
            final Set<Component> usingComponents = componentRepository.getUsingComponents(releaseIds);
            if (usingComponents.size() > 0)
                return true;

            final Set<Project> usingProjects = projectRepository.searchByReleaseId(releaseIds);
            if (usingProjects.size() > 0)
                return true;
        }
        return false;
    }

    public boolean checkIfInUse(String releaseId) {

        final Set<Component> usingComponents = componentRepository.getUsingComponents(releaseId);
        if (usingComponents.size() > 0)
            return true;

        final Set<Project> usingProjects = projectRepository.searchByReleaseId(releaseId);
        return (usingProjects.size() > 0);
    }

    private Component removeReleaseAndCleanUp(Release release, User user) throws SW360Exception {
        attachmentConnector.deleteAttachments(release.getAttachments());
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

        if (release.getPackageIdsSize() > 0 || checkIfInUse(id)) return RequestStatus.IN_USE;

        if (makePermission(release, user).isActionAllowed(RequestedAction.DELETE) || forceDelete) {
            Component componentBefore = componentRepository.get(release.getComponentId());
            // Remove release id from component
            removeReleaseId(id, release.componentId);
            // Remove spdx if exist
            String spdxId = release.getSpdxId();
            if (CommonUtils.isNotNullEmptyOrWhitespace(spdxId)) {
                spdxDocumentDatabaseHandler.deleteSPDXDocument(spdxId, user);
                release = releaseRepository.get(id);
            }
            Component componentAfter = removeReleaseAndCleanUp(release, user);
            dbHandlerUtil.addChangeLogs(null, release, user.getEmail(), Operation.DELETE, attachmentConnector,
                    Lists.newArrayList(), null, null);
            dbHandlerUtil.addChangeLogs(componentAfter, componentBefore, user.getEmail(), Operation.UPDATE,
                    attachmentConnector, Lists.newArrayList(), release.getId(), Operation.RELEASE_DELETE);
            return RequestStatus.SUCCESS;
        } else {
            return releaseModerator.deleteRelease(release, user);
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

    List<ReleaseLink> getLinkedReleases(Project project, Deque<String> visitedIds) {
        return getLinkedReleases(project.getReleaseIdToUsage(), visitedIds);
    }

    List<ReleaseLink> getLinkedReleasesWithAccessibility(Project project, Deque<String> visitedIds, User user) {
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
        final List<Component> components = componentRepository.getComponentsByVCS();
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
                if (release.isSetMainLicenseIds()) {
                    releaseLink.setLicenseIds(release.getMainLicenseIds());
                }
                if (release.isSetOtherLicenseIds()) {
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
        if (relation instanceof ProjectReleaseRelationship) {
            ProjectReleaseRelationship rel = (ProjectReleaseRelationship) relation;
            releaseLink.setReleaseRelationship(rel.getReleaseRelation());
            releaseLink.setMainlineState(rel.getMainlineState());
            releaseLink.setComment(rel.getComment());
        } else if (relation instanceof ReleaseRelationship) {
            releaseLink.setReleaseRelationship((ReleaseRelationship) relation);
        } else {
            throw new IllegalArgumentException("Only ProjectReleaseRelationship or ReleaseRelationship is allowed as ReleaseLink's relation value");
        }
    }

    @NotNull
    private ReleaseLink createReleaseLink(Release release) {
        vendorRepository.fillVendor(release);
        String vendorName = release.isSetVendor() ? release.getVendor().getShortname() : "";
        ReleaseLink releaseLink = new ReleaseLink(release.id, vendorName, release.name, release.version, SW360Utils.printFullname(release),
                 !nullToEmptyMap(release.getReleaseIdToRelationship()).isEmpty());
        releaseLink
                .setClearingState(release.getClearingState())
                .setComponentType(
                        Optional.ofNullable(componentRepository.get(release.getComponentId()))
                                .map(Component::getComponentType)
                                .orElse(null));
        if (!nullToEmptySet(release.getAttachments()).isEmpty()) {
            releaseLink.setAttachments(Lists.newArrayList(release.getAttachments()));
        }
        return releaseLink;
    }

    private String generateNodeId(String id) {
        return id == null ? null : id + "_" + UUID.randomUUID();
    }

    public List<Release> searchReleaseByNamePrefix(String name) {
        return releaseRepository.searchByNamePrefix(name);
    }

    public List<Release> searchAccessibleReleasesByText(ReleaseSearchHandler searchHandler, String searchText, User user){
        return getAccessibleReleaseList(searchHandler.search(searchText), user);
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
            release.setPermissions(makePermission(release, user).getPermissionMap());
            for (RequestedAction action : RequestedAction.values()) {
                release.getPermissions().put(action, isReleaseActionAllowed(release, user, action));
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

        component.addToSubscribers(user.getEmail());
        componentRepository.update(component);
        return RequestStatus.SUCCESS;
    }

    public RequestStatus subscribeRelease(String id, User user) throws SW360Exception {
        Release release = releaseRepository.get(id);
        assertNotNull(release);

        release.addToSubscribers(user.getEmail());
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
        List<ModerationRequest> moderationRequestsForDocumentId = moderator.getModerationRequestsForDocumentId(id);

        Component component = getComponent(id, user);
        DocumentState documentState;

        if (moderationRequestsForDocumentId.isEmpty()) {
            documentState = CommonUtils.getOriginalDocumentState();
        } else {
            final String email = user.getEmail();
            Optional<ModerationRequest> moderationRequestOptional = CommonUtils.getFirstModerationRequestOfUser(moderationRequestsForDocumentId, email);
            if (moderationRequestOptional.isPresent()
                    && isInProgressOrPending(moderationRequestOptional.get())){
                ModerationRequest moderationRequest = moderationRequestOptional.get();

                component = moderator.updateComponentFromModerationRequest(
                        component,
                        moderationRequest.getComponentAdditions(),
                        moderationRequest.getComponentDeletions());
                documentState = CommonUtils.getModeratedDocumentState(moderationRequest);
            } else {
                documentState = new DocumentState().setIsOriginalDocument(true).setModerationState(moderationRequestsForDocumentId.get(0).getModerationState());
            }
        }
        component.setPermissions(makePermission(component, user).getPermissionMap());
        component.setDocumentState(documentState);
        return component;
    }

    public Component getAccessibleComponentForEdit(String id, User user) throws SW360Exception {
        Component component = getComponentForEdit(id, user);
        Map<RequestedAction, Boolean> permissions = component.getPermissions();
        if (!permissions.get(RequestedAction.READ)) {
            throw fail(403, "Could not fetch component for edit, because access is denied! id=" + id);
        }
        return component;
    }

    public Release getReleaseForEdit(String id, User user) throws SW360Exception {
        List<ModerationRequest> moderationRequestsForDocumentId = moderator.getModerationRequestsForDocumentId(id);

        Release release = getRelease(id, user);
        DocumentState documentState;

        if (moderationRequestsForDocumentId.isEmpty()) {
            documentState = CommonUtils.getOriginalDocumentState();
        } else {
            final String email = user.getEmail();
            Optional<ModerationRequest> moderationRequestOptional = CommonUtils.getFirstModerationRequestOfUser(moderationRequestsForDocumentId, email);
            if (moderationRequestOptional.isPresent()
                    && isInProgressOrPending(moderationRequestOptional.get())){
                ModerationRequest moderationRequest = moderationRequestOptional.get();

                release = releaseModerator.updateReleaseFromModerationRequest(
                        release,
                        moderationRequest.getReleaseAdditions(),
                        moderationRequest.getReleaseDeletions());
                documentState = CommonUtils.getModeratedDocumentState(moderationRequest);
            } else {
                documentState = new DocumentState().setIsOriginalDocument(true).setModerationState(moderationRequestsForDocumentId.get(0).getModerationState());
            }
        }
        vendorRepository.fillVendor(release);
        release.setPermissions(makePermission(release, user).getPermissionMap());
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
            if (component.isSetReleaseIds()) {
                // Convert Ids to release summary
                final Set<String> releaseIds = component.getReleaseIds();
                final List<Release> releases = nullToEmptyList(releaseRepository.get(releaseIds));
                for (Release release : releases) {
                    vendorRepository.fillVendor(release);
                }
                component.setReleases(releases);
                component.unsetReleaseIds();

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
            componentIdentifierToComponentId.put(SW360Utils.printName(component), component.getId());
        }
        return CommonUtils.getIdentifierToListOfDuplicates(componentIdentifierToComponentId);
    }

    public Map<String, List<String>> getDuplicateReleases() {
        ListMultimap<String, String> releaseIdentifierToReleaseId = ArrayListMultimap.create();

        for (Release release : getAllReleases()) {
            releaseIdentifierToReleaseId.put(SW360Utils.printName(release), release.getId());
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

            if(release.isSetAttachments()) {
                for (Attachment attachment : release.getAttachments()) {
                    if (attachment.getAttachmentType() == AttachmentType.SOURCE)
                        releaseIdentifierToReleaseId.put(SW360Utils.printName(release), release.getId());
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

        Component srcComponentFromDBOriginal = srcComponentFromDB.deepCopy();
        Component targetComponentFromDBOriginal = targetComponentFromDB.deepCopy();

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
                targetComponentFromDB.unsetReleases();
                srcComponentFromDB.unsetReleases();
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
            dbHandlerUtil.addChangeLogs(srcComponentFromDB, srcComponentFromDBOriginal, user.getEmail(),
                    Operation.UPDATE, null, Lists.newArrayList(), null, Operation.SPLIT_COMPONENT);
            dbHandlerUtil.addChangeLogs(targetComponentFromDB, targetComponentFromDBOriginal, user.getEmail(),
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
                log.error("Error occured while getting release. ", e);
            }
            return null;
        }).filter(rel -> rel == null || !makePermission(rel, sessionUser).isActionAllowed(RequestedAction.WRITE))
                .count();
    }

    private void sendMailNotificationsForNewComponent(Component component, String user) {
        mailUtil.sendMail(component.getComponentOwner(),
                MailConstants.SUBJECT_FOR_NEW_COMPONENT,
                MailConstants.TEXT_FOR_NEW_COMPONENT,
                SW360Constants.NOTIFICATION_CLASS_COMPONENT, Component._Fields.COMPONENT_OWNER.toString(),
                component.getName());
        mailUtil.sendMail(component.getModerators(), user,
                MailConstants.SUBJECT_FOR_NEW_COMPONENT,
                MailConstants.TEXT_FOR_NEW_COMPONENT,
                SW360Constants.NOTIFICATION_CLASS_COMPONENT, Component._Fields.MODERATORS.toString(),
                component.getName());
        mailUtil.sendMail(component.getSubscribers(), user,
                MailConstants.SUBJECT_FOR_NEW_COMPONENT,
                MailConstants.TEXT_FOR_NEW_COMPONENT,
                SW360Constants.NOTIFICATION_CLASS_COMPONENT, Component._Fields.SUBSCRIBERS.toString(),
                component.getName());
        mailUtil.sendMail(SW360Utils.unionValues(component.getRoles()), user,
                MailConstants.SUBJECT_FOR_NEW_COMPONENT,
                MailConstants.TEXT_FOR_NEW_COMPONENT,
                SW360Constants.NOTIFICATION_CLASS_COMPONENT, Component._Fields.ROLES.toString(),
                component.getName());
    }

    private void sendMailNotificationsForComponentUpdate(Component component, String user) {
        mailUtil.sendMail(component.getCreatedBy(),
                MailConstants.SUBJECT_FOR_UPDATE_COMPONENT,
                MailConstants.TEXT_FOR_UPDATE_COMPONENT,
                SW360Constants.NOTIFICATION_CLASS_COMPONENT, Component._Fields.CREATED_BY.toString(),
                component.getName());
        mailUtil.sendMail(component.getComponentOwner(),
                MailConstants.SUBJECT_FOR_UPDATE_COMPONENT,
                MailConstants.TEXT_FOR_UPDATE_COMPONENT,
                SW360Constants.NOTIFICATION_CLASS_COMPONENT, Component._Fields.COMPONENT_OWNER.toString(),
                component.getName());
        mailUtil.sendMail(component.getModerators(), user,
                MailConstants.SUBJECT_FOR_UPDATE_COMPONENT,
                MailConstants.TEXT_FOR_UPDATE_COMPONENT,
                SW360Constants.NOTIFICATION_CLASS_COMPONENT, Component._Fields.MODERATORS.toString(),
                component.getName());
        mailUtil.sendMail(component.getSubscribers(), user,
                MailConstants.SUBJECT_FOR_UPDATE_COMPONENT,
                MailConstants.TEXT_FOR_UPDATE_COMPONENT,
                SW360Constants.NOTIFICATION_CLASS_COMPONENT, Component._Fields.SUBSCRIBERS.toString(),
                component.getName());
        mailUtil.sendMail(SW360Utils.unionValues(component.getRoles()), user,
                MailConstants.SUBJECT_FOR_UPDATE_COMPONENT,
                MailConstants.TEXT_FOR_UPDATE_COMPONENT,
                SW360Constants.NOTIFICATION_CLASS_COMPONENT, Component._Fields.ROLES.toString(),
                component.getName());
    }

    private void sendMailNotificationsForNewRelease(Release release, String user) {
        mailUtil.sendMail(release.getContributors(), user,
                MailConstants.SUBJECT_FOR_NEW_RELEASE,
                MailConstants.TEXT_FOR_NEW_RELEASE,
                SW360Constants.NOTIFICATION_CLASS_RELEASE, Release._Fields.CONTRIBUTORS.toString(),
                release.getName(), release.getVersion());
        mailUtil.sendMail(release.getModerators(), user,
                MailConstants.SUBJECT_FOR_NEW_RELEASE,
                MailConstants.TEXT_FOR_NEW_RELEASE,
                SW360Constants.NOTIFICATION_CLASS_RELEASE, Release._Fields.MODERATORS.toString(),
                release.getName(), release.getVersion());
        mailUtil.sendMail(release.getSubscribers(), user,
                MailConstants.SUBJECT_FOR_NEW_RELEASE,
                MailConstants.TEXT_FOR_NEW_RELEASE,
                SW360Constants.NOTIFICATION_CLASS_RELEASE, Release._Fields.SUBSCRIBERS.toString(),
                release.getName(), release.getVersion());
        mailUtil.sendMail(SW360Utils.unionValues(release.getRoles()), user,
                MailConstants.SUBJECT_FOR_NEW_RELEASE,
                MailConstants.TEXT_FOR_NEW_RELEASE,
                SW360Constants.NOTIFICATION_CLASS_RELEASE, Release._Fields.SUBSCRIBERS.toString(),
                release.getName(), release.getVersion());
    }

    private void sendMailNotificationsForReleaseUpdate(Release release, String user) {
        mailUtil.sendMail(release.getCreatedBy(),
                MailConstants.SUBJECT_FOR_UPDATE_RELEASE,
                MailConstants.TEXT_FOR_UPDATE_RELEASE,
                SW360Constants.NOTIFICATION_CLASS_RELEASE, Release._Fields.CONTRIBUTORS.toString(),
                release.getName(), release.getVersion());
        mailUtil.sendMail(release.getContributors(), user,
                MailConstants.SUBJECT_FOR_UPDATE_RELEASE,
                MailConstants.TEXT_FOR_UPDATE_RELEASE,
                SW360Constants.NOTIFICATION_CLASS_RELEASE, Release._Fields.CONTRIBUTORS.toString(),
                release.getName(), release.getVersion());
        mailUtil.sendMail(release.getModerators(), user,
                MailConstants.SUBJECT_FOR_UPDATE_RELEASE,
                MailConstants.TEXT_FOR_UPDATE_RELEASE,
                SW360Constants.NOTIFICATION_CLASS_RELEASE, Release._Fields.MODERATORS.toString(),
                release.getName(), release.getVersion());
        mailUtil.sendMail(release.getSubscribers(), user,
                MailConstants.SUBJECT_FOR_UPDATE_RELEASE,
                MailConstants.TEXT_FOR_UPDATE_RELEASE,
                SW360Constants.NOTIFICATION_CLASS_RELEASE, Release._Fields.SUBSCRIBERS.toString(),
                release.getName(), release.getVersion());
        mailUtil.sendMail(SW360Utils.unionValues(release.getRoles()), user,
                MailConstants.SUBJECT_FOR_UPDATE_RELEASE,
                MailConstants.TEXT_FOR_UPDATE_RELEASE,
                SW360Constants.NOTIFICATION_CLASS_RELEASE, Release._Fields.SUBSCRIBERS.toString(),
                release.getName(), release.getVersion());
    }

    public RequestStatus updateReleasesWithSvmTrackingFeedback() {
        try {
            Map<String, Map<String, Object>> componentMappings = getSvmConnector().fetchComponentMappings();
            List<Release> releases = releaseRepository.getReleasesIgnoringNotFound(componentMappings.keySet());
            releases.forEach(r -> {
                Map<String, String> externalIds = r.isSetExternalIds() ? r.getExternalIds() : new HashMap<>();
                Map<String, String> additionalData = r.isSetAdditionalData() ? r.getAdditionalData() : new HashMap<>();

                Map<String, Object> releaseSVMData = componentMappings.get(r.getId());
                if (!CommonUtils.isNullOrEmptyMap(releaseSVMData)) {
                    Release originalReleaseData = r.deepCopy();
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
                        dbHandlerUtil.addChangeLogs(r, originalReleaseData, SW360Constants.SVM_SCHEDULER_EMAIL,
                                Operation.UPDATE, attachmentConnector, Lists.newArrayList(), null, null);
                    }
                }
            });
            List<Response> documentOperationResults = releaseRepository.executeBulk(releases);
            documentOperationResults = documentOperationResults.stream().filter(res -> res.getError() != null || res.getStatusCode() != HttpStatus.SC_CREATED)
                    .collect(Collectors.toList());
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
                return spdxBOMImporter.importSpdxBOMAsRelease(inputStream, attachmentContent);
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
        DatabaseHandlerUtil.trimStringFields(release, listOfStringFieldsInReleaseToTrim);

        ClearingInformation clearingInformation = release.getClearingInformation();
        if (clearingInformation != null) {
            DatabaseHandlerUtil.trimStringFields(clearingInformation, listOfStringFieldsInClearingInformationToTrim);
        }

        COTSDetails cotsDetails = release.getCotsDetails();
        if (cotsDetails != null) {
            DatabaseHandlerUtil.trimStringFields(cotsDetails, listOfStringFieldsInCOTSDetailsToTrim);
        }

        EccInformation eccInformation = release.getEccInformation();
        if (eccInformation != null) {
            DatabaseHandlerUtil.trimStringFields(eccInformation, listOfStringFieldsInEccInformationToTrim);
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

        release.setAttachments(DatabaseHandlerUtil.trimSetOfAttachement(release.getAttachments()));

        release.setRoles(DatabaseHandlerUtil.trimMapOfStringKeySetValue(release.getRoles()));

        release.setExternalIds(DatabaseHandlerUtil.trimMapOfStringKeyStringValue(release.getExternalIds()));

        release.setAdditionalData(DatabaseHandlerUtil.trimMapOfStringKeyStringValue(release.getAdditionalData()));
    }

    private void removeLeadingTrailingWhitespace(Component component) {
        DatabaseHandlerUtil.trimStringFields(component, listOfStringFieldsInCompToTrim);

        component.setRoles(DatabaseHandlerUtil.trimMapOfStringKeySetValue(component.getRoles()));

        component.setExternalIds(DatabaseHandlerUtil.trimMapOfStringKeyStringValue(component.getExternalIds()));

        component.setAdditionalData(DatabaseHandlerUtil.trimMapOfStringKeyStringValue(component.getAdditionalData()));

        component.setCategories(DatabaseHandlerUtil.trimSetOfString(component.getCategories()));

        component.setAttachments(DatabaseHandlerUtil.trimSetOfAttachement(component.getAttachments()));

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
            Release releaseBefore = r.deepCopy();
            if (targetComponentReleaseVersions.contains(r.getVersion())) {
                StringBuilder conflictVersionBuilder = new StringBuilder(r.getVersion()).append("_conflict (")
                        .append(r.getId()).append(")");
                r.setVersion(conflictVersionBuilder.toString());
            }
            r.setComponentId(targetComponentFromDB.getId());
            r.setName(targetComponentFromDB.getName());
            updateModifiedFields(r, userEmail);
            dbHandlerUtil.addChangeLogs(r, releaseBefore, userEmail, Operation.UPDATE, attachmentConnector,
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
                            att.unsetSuperAttachmentFilename();
                            att.unsetSuperAttachmentId();
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
             return exporter.makeExcelExportForProject(componentlist, user);
          }catch (IOException e) {
             throw new SW360Exception(e.getMessage());
       }
    }

    private ComponentExporter getComponentExporterObject(List<Component> componentList ,User user,
            boolean extendedByRelease) throws SW360Exception {
    	ThriftClients thriftClients = new ThriftClients();
        return new ComponentExporter(thriftClients.makeComponentClient(), componentList, user,extendedByRelease);
    }

    public ByteBuffer downloadExcel(User user,boolean extendedByReleases,String token) throws SW360Exception {
        try {
        	ThriftClients thriftClients = new ThriftClients();
        	ComponentExporter exporter = new ComponentExporter(thriftClients.makeComponentClient(), user,
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
            InputStream stream = exporter.makeExcelExport(componentlist);
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
            log.info(String.format("SRC Upload: %s %s", c.getId(), VCS));
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
                            Release originalReleaseData = r.deepCopy();

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
                                    } catch (IOException | TException e) {
                                        log.error(
                                                "SRC Upload: Error while downloading the source code zip file for release:"
                                                        + r.getId() + " " + e);
                                    }
                                }
                            }
                            if (isUploaded) {
                                dbHandlerUtil.addChangeLogs(r, originalReleaseData,
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

    private boolean isValidURL(String url) {
        try {
            URL urlObj = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
            connection.setRequestMethod("HEAD");
            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                return true;
            } else {
                return false;
            }
        } catch (IOException e) {
            log.error("Error while checking the validity of the URL " + url, e);
            return false;
        }
    }

    public File downloadFile(String url, String destinationDirectory) throws IOException {
        URL fileUrl = new URL(url);
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
        FileInputStream inputStream = new FileInputStream(file);
        Attachment attachment = new AttachmentFrontendUtils().uploadAttachmentContent(attachmentContent, inputStream, null);

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
            return new AttachmentFrontendUtils().makeAttachmentContent(content);
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }
}

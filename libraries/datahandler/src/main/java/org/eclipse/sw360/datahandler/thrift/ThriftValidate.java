package org.eclipse.sw360.datahandler.thrift;

import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.components.*;
import org.eclipse.sw360.datahandler.thrift.licenses.*;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectClearingState;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.DocumentCreationInformation;
import org.eclipse.sw360.datahandler.thrift.projects.ObligationList;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.SPDXDocument;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageInformation;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.common.SW360Assert.*;
import static org.eclipse.sw360.datahandler.common.SW360Constants.*;
import static org.eclipse.sw360.datahandler.common.SW360Utils.newDefaultEccInformation;

/**
 * Utility class to validate the data before inserting it in the database.
 * It ensures that the type is set correctly on the object (for easier parsing).
 */
public class ThriftValidate {

    private static final Pattern LICENSE_ID_PATTERN = Pattern.compile("[A-Za-z0-9\\-.+]*");

    private ThriftValidate() {
        // Utility class with only static functions
    }

    /**
     * Prepares an Obligation object for database insertion.
     *
     * @param oblig The Obligation to prepare
     * @throws SW360Exception if validation fails
     */
    public static void prepareTodo(Obligation oblig) throws SW360Exception {
        assertNotNull(oblig, "Obligation cannot be null");
        assertNotEmpty(oblig.getText(), "Obligation text cannot be empty");
        assertNotNull(oblig.getTitle(), "Obligation title cannot be null");
        assertNotNull(oblig.getObligationLevel(), "Obligation level cannot be null");

        oblig.setWhitelist(oblig.isSetWhitelist() ? oblig.getWhitelist() : Collections.emptySet());
        oblig.setType(TYPE_OBLIGATION);
    }

    /**
     * Prepares a LicenseType object for database insertion.
     *
     * @param licenseType The LicenseType to prepare
     * @throws SW360Exception if validation fails
     */
    public static void prepareLicenseType(LicenseType licenseType) throws SW360Exception {
        assertNotNull(licenseType, "LicenseType cannot be null");
        assertNotEmpty(licenseType.getLicenseType(), "LicenseType cannot be empty");
        licenseType.setType(TYPE_LICENSETYPE);
    }

    /**
     * Prepares an ObligationElement object for database insertion.
     *
     * @param obligationElement The ObligationElement to prepare
     * @throws SW360Exception if validation fails
     */
    public static void prepareObligationElement(ObligationElement obligationElement) throws SW360Exception {
        assertNotNull(obligationElement, "ObligationElement cannot be null");
        obligationElement.setType(TYPE_OBLIGATIONELEMENT);
    }

    /**
     * Prepares an ObligationNode object for database insertion.
     *
     * @param obligationNode The ObligationNode to prepare
     * @throws SW360Exception if validation fails
     */
    public static void prepareObligationNode(ObligationNode obligationNode) throws SW360Exception {
        assertNotNull(obligationNode, "ObligationNode cannot be null");
        obligationNode.setType(TYPE_OBLIGATIONNODE);
    }

    /**
     * Prepares a License object for database insertion.
     *
     * @param license The License to prepare
     * @throws SW360Exception if validation fails
     */
    public static void prepareLicense(License license) throws SW360Exception {
        assertNotNull(license, "License cannot be null");
        assertNotEmpty(license.getId(), "License ID cannot be empty");
        assertNotEmpty(license.getFullname(), "License full name cannot be empty");

        if (license.isSetLicenseType() && !license.isSetLicenseTypeDatabaseId()) {
            license.setLicenseTypeDatabaseId(license.getLicenseType().getId());
        }
        license.unsetLicenseType();

        if (license.isSetObligations() && license.isSetObligationDatabaseIds()) {
            license.getObligations().forEach(oblig -> license.addToObligationDatabaseIds(oblig.getId()));
        }
        license.unsetObligations();

        license.setType(TYPE_LICENSE);
        license.unsetPermissions();
    }

    /**
     * Prepares a User object for database insertion.
     *
     * @param user The User to prepare
     * @throws SW360Exception if validation fails
     */
    public static void prepareUser(User user) throws SW360Exception {
        assertNotEmpty(user.getEmail(), "User email cannot be empty");
        user.setType(TYPE_USER);
        user.unsetCommentMadeDuringModerationRequest();
    }

    /**
     * Prepares a Vendor object for database insertion.
     *
     * @param vendor The Vendor to prepare
     * @throws SW360Exception if validation fails
     */
    public static void prepareVendor(Vendor vendor) throws SW360Exception {
        assertNotEmpty(vendor.getShortname(), "Vendor short name cannot be empty");
        assertNotEmpty(vendor.getFullname(), "Vendor full name cannot be empty");
        assertValidUrl(vendor.getUrl());
        vendor.setType(TYPE_VENDOR);
    }

    /**
     * Prepares a Component object for database insertion.
     *
     * @param component The Component to prepare
     * @throws SW360Exception if validation fails
     */
    public static void prepareComponent(Component component) throws SW360Exception {
        assertNotEmpty(component.getName(), "Component name cannot be empty");
        component.setType(TYPE_COMPONENT);
        component.unsetPermissions();
        component.unsetReleases();
        component.unsetDefaultVendor();
    }

    /**
     * Prepares a list of Component objects for database insertion.
     *
     * @param components The Collection of Components to prepare
     * @return List of prepared Components
     * @throws SW360Exception if validation fails
     */
    public static List<Component> prepareComponents(Collection<Component> components) throws SW360Exception {
        if (components == null) {
            return Collections.emptyList();
        }

        return components.stream()
                .map(component -> {
                    try {
                        prepareComponent(component);
                        return component;
                    } catch (SW360Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Prepares a Package object for database insertion.
     *
     * @param pkg The Package to prepare
     * @throws SW360Exception if validation fails
     */
    public static void preparePackage(Package pkg) throws SW360Exception {
        assertNotNull(pkg, "Package cannot be null");
        assertNotEmpty(pkg.getName(), "Package name cannot be empty");
        assertNotEmpty(pkg.getVersion(), "Package version cannot be empty");
        assertNotEmpty(pkg.getPurl(), "Package PURL cannot be empty");
        assertNotNull(pkg.getPackageManager(), "Package manager cannot be null");
        assertNotEmpty(pkg.getPackageManager().name(), "Package manager name cannot be empty");
        assertNotNull(pkg.getPackageType(), "Package type cannot be null");
        assertNotEmpty(pkg.getPackageType().name(), "Package type name cannot be empty");
        pkg.unsetRelease();
        pkg.setType(TYPE_PACKAGE);
    }

    /**
     * Prepares a Release object for database insertion.
     *
     * @param release The Release to prepare
     * @throws SW360Exception if validation fails
     */
    public static void prepareRelease(Release release) throws SW360Exception {
        assertNotEmpty(release.getName(), "Release name cannot be empty");
        assertNotEmpty(release.getVersion(), "Release version cannot be empty");
        assertNotEmpty(release.getComponentId(), "Release component ID cannot be empty");

        release.setType(TYPE_RELEASE);

        if (release.isSetVendor()) {
            release.setVendorId(release.getVendor().getId());
        }

        ensureEccInformationIsSet(release);

        release.unsetPermissions();
        release.unsetVendor();
        release.unsetCreatorDepartment();
    }

    /**
     * Ensures that ECC information is set for a Release.
     *
     * @param release The Release to update
     * @return The updated Release
     */
    public static Release ensureEccInformationIsSet(Release release) {
        EccInformation eccInformation = release.isSetEccInformation()
                ? release.getEccInformation()
                : newDefaultEccInformation();

        if (!eccInformation.isSetEccStatus()) {
            eccInformation.setEccStatus(ECCStatus.OPEN);
        }

        release.setEccInformation(eccInformation);
        return release;
    }

    /**
     * Prepares a list of Release objects for database insertion.
     *
     * @param releases The Collection of Releases to prepare
     * @return List of prepared Releases
     * @throws SW360Exception if validation fails
     */
    public static List<Release> prepareReleases(Collection<Release> releases) throws SW360Exception {
        if (releases == null) {
            return Collections.emptyList();
        }

        return releases.stream()
                .map(release -> {
                    try {
                        prepareRelease(release);
                        return release;
                    } catch (SW360Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Validates an AttachmentContent object.
     *
     * @param attachment The AttachmentContent to validate
     * @throws SW360Exception if validation fails
     */
    public static void validateAttachment(AttachmentContent attachment) throws SW360Exception {
        assertNotNull(attachment, "Attachment cannot be null");
        if (attachment.isOnlyRemote()) {
            assertValidUrl(attachment.getRemoteUrl());
        }
        attachment.setType(TYPE_ATTACHMENT);
    }

    /**
     * Validates a new License object.
     *
     * @param license The License to validate
     * @throws SW360Exception if validation fails
     */
    public static void validateNewLicense(License license) throws SW360Exception {
        assertId(license.getShortname());
        assertTrue(LICENSE_ID_PATTERN.matcher(license.getShortname()).matches(), "License ID must match the pattern");
        if (license.isSetId()) {
            validateLicenseIdMatch(license);
        }
    }

    /**
     * Validates an existing License object.
     *
     * @param license The License to validate
     * @throws SW360Exception if validation fails
     */
    public static void validateExistingLicense(License license) throws SW360Exception {
        validateLicenseIdMatch(license);
    }

    private static void validateLicenseIdMatch(License license) throws SW360Exception {
        assertEquals(license.getId(), license.getShortname(), "License short name must be equal to license ID");
    }

    /**
     * Prepares a Project object for database insertion.
     *
     * @param project The Project to prepare
     * @throws SW360Exception if validation fails
     */
    public static void prepareProject(Project project) throws SW360Exception {
        assertNotEmpty(project.getName(), "Project name cannot be empty");
        project.setType(TYPE_PROJECT);
        if (!project.isSetClearingState()) {
            project.setClearingState(ProjectClearingState.OPEN);
        }
        project.unsetPermissions();
        project.unsetReleaseClearingStateSummary();
    }

    /**
     * Prepares a ProjectObligation object for database insertion.
     *
     * @param obligation The ObligationList to prepare
     * @throws SW360Exception if validation fails
     */
    public static void prepareProjectObligation(ObligationList obligation) throws SW360Exception {
        assertId(obligation.getProjectId());
        assertNotNull(obligation.getLinkedObligationStatus(), "Linked obligation status cannot be null");
        assertNotEmpty(obligation.getLinkedObligationStatus().keySet(), "Linked obligations cannot be empty");
        obligation.setType(TYPE_PROJECT_OBLIGATION);
    }

    /**
     * Prepares an SPDXDocument object for database insertion.
     *
     * @param spdx The SPDXDocument to prepare
     * @throws SW360Exception if validation fails
     */
    public static void prepareSPDXDocument(SPDXDocument spdx) throws SW360Exception {
        assertNotNull(spdx, "SPDX Document cannot be null");
        spdx.setType(TYPE_SPDX_DOCUMENT);
        spdx.unsetPermissions();
    }

    /**
     * Prepares a DocumentCreationInformation object for database insertion.
     *
     * @param documentCreationInfo The DocumentCreationInformation to prepare
     * @throws SW360Exception if validation fails
     */
    public static void prepareSpdxDocumentCreationInfo(DocumentCreationInformation documentCreationInfo) throws SW360Exception {
        assertNotNull(documentCreationInfo, "Document Creation Information cannot be null");
        documentCreationInfo.setType(TYPE_SPDX_DOCUMENT_CREATION_INFO);
        documentCreationInfo.unsetPermissions();
    }

    /**
     * Prepares a PackageInformation object for database insertion.
     *
     * @param packageInfo The PackageInformation to prepare
     * @throws SW360Exception if validation fails
     */
    public static void prepareSpdxPackageInfo(PackageInformation packageInfo) throws SW360Exception {
        assertNotNull(packageInfo, "Package Information cannot be null");
        packageInfo.setType(TYPE_SPDX_PACKAGE_INFO);
        packageInfo.unsetPermissions();
    }
}

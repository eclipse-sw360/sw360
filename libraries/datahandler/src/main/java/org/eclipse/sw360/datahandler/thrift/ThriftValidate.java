/*
 * Copyright Siemens AG, 2014-2018. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.thrift;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;

import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.components.*;
import org.eclipse.sw360.datahandler.thrift.licenses.*;
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
import java.util.regex.Pattern;

import static com.google.common.base.Predicates.notNull;
import static org.eclipse.sw360.datahandler.common.SW360Assert.*;
import static org.eclipse.sw360.datahandler.common.SW360Constants.*;
import static org.eclipse.sw360.datahandler.common.SW360Utils.newDefaultEccInformation;

/**
 * Utility class to validate the data before inserting it in the database.
 * In particular, in ensure the that the type is set correctly on the object (for easier parsing)
 *
 * @author cedric.bodet@tngtech.com
 */
public class ThriftValidate {

    private static final Pattern LICENSE_ID_PATTERN = Pattern.compile("[A-Za-z0-9\\-.+]*");

    private ThriftValidate() {
        // Utility class with only static functions
    }

    public static void prepareTodo(Obligation oblig) throws SW360Exception {
        // Check required fields
        assertNotNull(oblig);
        assertNotEmpty(oblig.getText());
        assertNotNull(oblig.getTitle());
        assertNotNull(oblig.getObligationLevel());

        if (oblig.whitelist == null) {
            oblig.setWhitelist(Collections.emptySet());
        }

        // Check type
        oblig.setType(TYPE_OBLIGATION);
    }

    public static void prepareLicenseType(LicenseType licenseType) throws SW360Exception {
        // Check required fields
        assertNotNull(licenseType);
        assertNotEmpty(licenseType.getLicenseType());

        // Check type
        licenseType.setType(TYPE_LICENSETYPE);
    }

    public static void prepareObligationElement(ObligationElement obligationElement) throws SW360Exception {
        // Check required fields
        assertNotNull(obligationElement);
        // Check type
        obligationElement.setType(TYPE_OBLIGATIONELEMENT);
    }

    public static void prepareObligationNode(ObligationNode obligationNode) throws SW360Exception {
        // Check required fields
        assertNotNull(obligationNode);
        // Check type
        obligationNode.setType(TYPE_OBLIGATIONNODE);
    }

    public static void prepareLicense(License license) throws SW360Exception {
        // Check required fields
        assertNotNull(license);
        assertNotEmpty(license.getId());
        assertNotEmpty(license.getFullname());

        if (license.isSetLicenseType() && !license.isSetLicenseTypeDatabaseId()) {
            license.setLicenseTypeDatabaseId(license.getLicenseType().getId());
        }
        license.unsetLicenseType();

        if (license.isSetObligations() && license.isSetObligationDatabaseIds()) {
            for (Obligation oblig : license.getObligations()) {
                license.addToObligationDatabaseIds(oblig.getId());
            }
        }
        license.unsetObligations();

        // Check type
        license.setType(TYPE_LICENSE);

        // Unset optionals
        license.unsetPermissions();
    }


    public static void prepareUser(User user) throws SW360Exception {
        // Check required fields
        assertNotEmpty(user.getEmail());
        // Set type
        user.setType(TYPE_USER);
        // guarantee that `CommentMadeDuringModerationRequest` is never stored in the database as this is intended to be a transient field
        user.unsetCommentMadeDuringModerationRequest();
    }

    public static void prepareVendor(Vendor vendor) throws SW360Exception {
        // Check required fields
        assertNotEmpty(vendor.getShortname(), "vendor short name cannot be empty!");
        assertNotEmpty(vendor.getFullname(), "vendor full name cannot be empty!");
        assertValidUrl(vendor.getUrl());

        // Check type
        vendor.setType(TYPE_VENDOR);
    }

    public static void prepareComponent(Component component) throws SW360Exception {
        // Check required fields
        assertNotEmpty(component.getName());

        // Check type
        component.setType(TYPE_COMPONENT);

        // Unset optionals
        component.unsetPermissions();

        // Unset fields that do not make sense
        component.unsetReleases();
        component.unsetDefaultVendor();
        // we might want to have a logic someday that the defaultVendorId will be set
        // from defaultVendor if the latter one is set and the former one not - but
        // until now the thought is that clients need to make sure that the
        // defaultVendorId is set and that the component contains the vendor itself is
        // just convenience
    }

    public static List<Component> prepareComponents(Collection<Component> components) throws SW360Exception {

        return FluentIterable.from(components).transform(new Function<Component, Component>() {
            @Override
            public Component apply(Component input) {
                try {
                    prepareComponent(input);
                } catch (SW360Exception e) {
                    return null;
                }
                return input;
            }
        }).filter(notNull()).toList();

    }

    public static void prepareRelease(Release release) throws SW360Exception {
        // Check required fields
        assertNotEmpty(release.getName());
        assertNotEmpty(release.getVersion());
        assertNotEmpty(release.getComponentId());

        // Check type
        release.setType(TYPE_RELEASE);

        // Save vendor ID, not object
        if (release.isSetVendor()) {
            release.vendorId = release.vendor.id;
        }

        ensureEccInformationIsSet(release);

        // Unset optionals
        release.unsetPermissions();
        release.unsetVendor();
        release.unsetCreatorDepartment();
    }

    public static Release ensureEccInformationIsSet(Release release) {
        EccInformation eccInformation = release.isSetEccInformation() ? release.getEccInformation() : newDefaultEccInformation();
        if (!eccInformation.isSetEccStatus()){
            eccInformation.setEccStatus(ECCStatus.OPEN);
        }
        release.setEccInformation(eccInformation);
        return release;
    }

    public static List<Release> prepareReleases(Collection<Release> components) throws SW360Exception {

        return FluentIterable.from(components).transform(new Function<Release, Release>() {
            @Override
            public Release apply(Release input) {
                try {
                    prepareRelease(input);
                } catch (SW360Exception e) {
                    return null;
                }
                return input;
            }
        }).filter(notNull()).toList();

    }

    public static void validateAttachment(AttachmentContent attachment) throws SW360Exception {
        assertNotNull(attachment);
        if (attachment.isOnlyRemote()) {
            String remoteUrl = attachment.getRemoteUrl();
            assertValidUrl(remoteUrl);
        }

        attachment.setType(TYPE_ATTACHMENT);
    }

    public static void validateNewLicense(License license) throws SW360Exception {
        assertId(license.getShortname());
        assertTrue(LICENSE_ID_PATTERN.matcher(license.getShortname()).matches());
        if (license.isSetId()) {
            validateLicenseIdMatch(license);
        }
    }

    public static void validateExistingLicense(License license) throws SW360Exception {
        validateLicenseIdMatch(license);
    }

    private static void validateLicenseIdMatch(License license) throws SW360Exception {
        String message = "license short name must be equal to license id";
        assertEquals(license.getId(), license.getShortname(), message);
    }

    public static void prepareProject(Project project) throws SW360Exception {
        assertNotEmpty(project.getName());
        project.setType(TYPE_PROJECT);
        if (!project.isSetClearingState()) {
            project.setClearingState(ProjectClearingState.OPEN);
        }

        // Unset temporary fields
        project.unsetPermissions();
        project.unsetReleaseClearingStateSummary();
    }

    public static void prepareProjectObligation(ObligationList obligation) throws SW360Exception {
        assertId(obligation.getProjectId());
        assertNotNull(obligation.getLinkedObligationStatus());
        assertNotEmpty(obligation.getLinkedObligationStatus().keySet(), "linked obligtions cannot be empty");
        obligation.setType(TYPE_PROJECT_OBLIGATION);
    }

    public static void prepareSPDXDocument(SPDXDocument spdx) throws SW360Exception {
        // Check required fields

        // Check type
        spdx.setType(TYPE_SPDX_DOCUMENT);
        // Unset temporary fields
        spdx.unsetPermissions();
    }

    public static void prepareSpdxDocumentCreationInfo(DocumentCreationInformation documentCreationInfo) throws SW360Exception {
        // Check required fields

        // Check type
        documentCreationInfo.setType(TYPE_SPDX_DOCUMENT_CREATION_INFO);
        // Unset temporary fields
        documentCreationInfo.unsetPermissions();
}

    public static void prepareSpdxPackageInfo(PackageInformation packageInfo) throws SW360Exception {
        // Check type
        packageInfo.setType(TYPE_SPDX_PACKAGE_INFO);
        // Unset temporary fields
        packageInfo.unsetPermissions();
    }
}

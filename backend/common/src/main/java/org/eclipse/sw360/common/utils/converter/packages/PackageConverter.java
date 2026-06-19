/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.packages;

import org.eclipse.sw360.datahandler.services.packages.Package;
import org.eclipse.sw360.common.utils.converter.common.EnumConverter;
import org.eclipse.sw360.common.utils.converter.common.ThriftCollectionConverter;

public final class PackageConverter {

    private PackageConverter() {}

    public static Package fromThrift(org.eclipse.sw360.datahandler.thrift.packages.Package thrift) {
        if (thrift == null) {
            return null;
        }
        Package pojo = new Package();
        if (thrift.isSetCreatedBy()) {
            pojo.setCreatedBy(thrift.getCreatedBy());
        }
        if (thrift.isSetCreatedOn()) {
            pojo.setCreatedOn(thrift.getCreatedOn());
        }
        if (thrift.isSetDescription()) {
            pojo.setDescription(thrift.getDescription());
        }
        if (thrift.isSetHash()) {
            pojo.setHash(thrift.getHash());
        }
        if (thrift.isSetHomepageUrl()) {
            pojo.setHomepageUrl(thrift.getHomepageUrl());
        }
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetLicenseIds()) {
            pojo.setLicenseIds(ThriftCollectionConverter.mapSet(thrift.getLicenseIds(), e -> e));
        }
        if (thrift.isSetModifiedBy()) {
            pojo.setModifiedBy(thrift.getModifiedBy());
        }
        if (thrift.isSetModifiedOn()) {
            pojo.setModifiedOn(thrift.getModifiedOn());
        }
        if (thrift.isSetName()) {
            pojo.setName(thrift.getName());
        }
        if (thrift.isSetPackageManager()) {
            pojo.setPackageManager(EnumConverter.fromThrift(thrift.getPackageManager(), org.eclipse.sw360.datahandler.services.packages.PackageManager.class));
        }
        if (thrift.isSetPackageType()) {
            pojo.setPackageType(EnumConverter.fromThrift(thrift.getPackageType(), org.eclipse.sw360.datahandler.services.common.CycloneDxComponentType.class));
        }
        if (thrift.isSetPurl()) {
            pojo.setPurl(thrift.getPurl());
        }
        if (thrift.isSetRelease()) {
            pojo.setRelease(org.eclipse.sw360.common.utils.converter.components.ReleaseConverter.fromThrift(thrift.getRelease()));
        }
        if (thrift.isSetReleaseId()) {
            pojo.setReleaseId(thrift.getReleaseId());
        }
        if (thrift.isSetRevision()) {
            pojo.setRevision(thrift.getRevision());
        }
        if (thrift.isSetType()) {
            pojo.setType(thrift.getType());
        }
        if (thrift.isSetVcs()) {
            pojo.setVcs(thrift.getVcs());
        }
        if (thrift.isSetVendor()) {
            pojo.setVendor(org.eclipse.sw360.common.utils.converter.vendors.VendorConverter.fromThrift(thrift.getVendor()));
        }
        if (thrift.isSetVendorId()) {
            pojo.setVendorId(thrift.getVendorId());
        }
        if (thrift.isSetVersion()) {
            pojo.setVersion(thrift.getVersion());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.packages.Package toThrift(Package pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.packages.Package thrift = new org.eclipse.sw360.datahandler.thrift.packages.Package();
        if (pojo.getCreatedBy() != null) {
            thrift.setCreatedBy(pojo.getCreatedBy());
        }
        if (pojo.getCreatedOn() != null) {
            thrift.setCreatedOn(pojo.getCreatedOn());
        }
        if (pojo.getDescription() != null) {
            thrift.setDescription(pojo.getDescription());
        }
        if (pojo.getHash() != null) {
            thrift.setHash(pojo.getHash());
        }
        if (pojo.getHomepageUrl() != null) {
            thrift.setHomepageUrl(pojo.getHomepageUrl());
        }
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getLicenseIds() != null) {
            thrift.setLicenseIds(ThriftCollectionConverter.mapSet(pojo.getLicenseIds(), e -> e));
        }
        if (pojo.getModifiedBy() != null) {
            thrift.setModifiedBy(pojo.getModifiedBy());
        }
        if (pojo.getModifiedOn() != null) {
            thrift.setModifiedOn(pojo.getModifiedOn());
        }
        if (pojo.getName() != null) {
            thrift.setName(pojo.getName());
        }
        if (pojo.getPackageManager() != null) {
            thrift.setPackageManager(EnumConverter.toThrift(pojo.getPackageManager(), org.eclipse.sw360.datahandler.thrift.packages.PackageManager.class));
        }
        if (pojo.getPackageType() != null) {
            thrift.setPackageType(EnumConverter.toThrift(pojo.getPackageType(), org.eclipse.sw360.datahandler.thrift.CycloneDxComponentType.class));
        }
        if (pojo.getPurl() != null) {
            thrift.setPurl(pojo.getPurl());
        }
        if (pojo.getRelease() != null) {
            thrift.setRelease(org.eclipse.sw360.common.utils.converter.components.ReleaseConverter.toThrift(pojo.getRelease()));
        }
        if (pojo.getReleaseId() != null) {
            thrift.setReleaseId(pojo.getReleaseId());
        }
        if (pojo.getRevision() != null) {
            thrift.setRevision(pojo.getRevision());
        }
        if (pojo.getType() != null) {
            thrift.setType(pojo.getType());
        }
        if (pojo.getVcs() != null) {
            thrift.setVcs(pojo.getVcs());
        }
        if (pojo.getVendor() != null) {
            thrift.setVendor(org.eclipse.sw360.common.utils.converter.vendors.VendorConverter.toThrift(pojo.getVendor()));
        }
        if (pojo.getVendorId() != null) {
            thrift.setVendorId(pojo.getVendorId());
        }
        if (pojo.getVersion() != null) {
            thrift.setVersion(pojo.getVersion());
        }
        return thrift;
    }
}

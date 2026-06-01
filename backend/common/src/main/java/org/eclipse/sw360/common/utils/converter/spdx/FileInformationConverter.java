/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.spdx;

import org.eclipse.sw360.datahandler.services.spdx.FileInformation;
import org.eclipse.sw360.common.utils.converter.common.ThriftCollectionConverter;

public final class FileInformationConverter {

    private FileInformationConverter() {}

    public static FileInformation fromThrift(org.eclipse.sw360.datahandler.thrift.spdx.fileinformation.FileInformation thrift) {
        if (thrift == null) {
            return null;
        }
        FileInformation pojo = new FileInformation();
        if (thrift.isSetAnnotations()) {
            pojo.setAnnotations(ThriftCollectionConverter.mapSet(thrift.getAnnotations(), e -> org.eclipse.sw360.common.utils.converter.spdx.AnnotationsConverter.fromThrift(e)));
        }
        if (thrift.isSetChecksums()) {
            pojo.setChecksums(ThriftCollectionConverter.mapSet(thrift.getChecksums(), e -> org.eclipse.sw360.common.utils.converter.spdx.CheckSumConverter.fromThrift(e)));
        }
        if (thrift.isSetCopyrightText()) {
            pojo.setCopyrightText(thrift.getCopyrightText());
        }
        if (thrift.isSetFileAttributionText()) {
            pojo.setFileAttributionText(ThriftCollectionConverter.mapSet(thrift.getFileAttributionText(), e -> e));
        }
        if (thrift.isSetFileComment()) {
            pojo.setFileComment(thrift.getFileComment());
        }
        if (thrift.isSetFileContributors()) {
            pojo.setFileContributors(ThriftCollectionConverter.mapSet(thrift.getFileContributors(), e -> e));
        }
        if (thrift.isSetFileName()) {
            pojo.setFileName(thrift.getFileName());
        }
        if (thrift.isSetFileTypes()) {
            pojo.setFileTypes(ThriftCollectionConverter.mapSet(thrift.getFileTypes(), e -> e));
        }
        if (thrift.isSetHasExtractedLicensingInfos()) {
            pojo.setHasExtractedLicensingInfos(ThriftCollectionConverter.mapSet(thrift.getHasExtractedLicensingInfos(), e -> org.eclipse.sw360.common.utils.converter.spdx.OtherLicensingInformationDetectedConverter.fromThrift(e)));
        }
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetLicenseComments()) {
            pojo.setLicenseComments(thrift.getLicenseComments());
        }
        if (thrift.isSetLicenseConcluded()) {
            pojo.setLicenseConcluded(thrift.getLicenseConcluded());
        }
        if (thrift.isSetLicenseInfoInFiles()) {
            pojo.setLicenseInfoInFiles(ThriftCollectionConverter.mapSet(thrift.getLicenseInfoInFiles(), e -> e));
        }
        if (thrift.isSetNoticeText()) {
            pojo.setNoticeText(thrift.getNoticeText());
        }
        if (thrift.isSetRevision()) {
            pojo.setRevision(thrift.getRevision());
        }
        if (thrift.isSetSnippetInformation()) {
            pojo.setSnippetInformation(ThriftCollectionConverter.mapSet(thrift.getSnippetInformation(), e -> org.eclipse.sw360.common.utils.converter.spdx.SnippetInformationConverter.fromThrift(e)));
        }
        if (thrift.isSetType()) {
            pojo.setType(thrift.getType());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.spdx.fileinformation.FileInformation toThrift(FileInformation pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.spdx.fileinformation.FileInformation thrift = new org.eclipse.sw360.datahandler.thrift.spdx.fileinformation.FileInformation();
        if (pojo.getAnnotations() != null) {
            thrift.setAnnotations(ThriftCollectionConverter.mapSet(pojo.getAnnotations(), e -> org.eclipse.sw360.common.utils.converter.spdx.AnnotationsConverter.toThrift(e)));
        }
        if (pojo.getChecksums() != null) {
            thrift.setChecksums(ThriftCollectionConverter.mapSet(pojo.getChecksums(), e -> org.eclipse.sw360.common.utils.converter.spdx.CheckSumConverter.toThrift(e)));
        }
        if (pojo.getCopyrightText() != null) {
            thrift.setCopyrightText(pojo.getCopyrightText());
        }
        if (pojo.getFileAttributionText() != null) {
            thrift.setFileAttributionText(ThriftCollectionConverter.mapSet(pojo.getFileAttributionText(), e -> e));
        }
        if (pojo.getFileComment() != null) {
            thrift.setFileComment(pojo.getFileComment());
        }
        if (pojo.getFileContributors() != null) {
            thrift.setFileContributors(ThriftCollectionConverter.mapSet(pojo.getFileContributors(), e -> e));
        }
        if (pojo.getFileName() != null) {
            thrift.setFileName(pojo.getFileName());
        }
        if (pojo.getFileTypes() != null) {
            thrift.setFileTypes(ThriftCollectionConverter.mapSet(pojo.getFileTypes(), e -> e));
        }
        if (pojo.getHasExtractedLicensingInfos() != null) {
            thrift.setHasExtractedLicensingInfos(ThriftCollectionConverter.mapSet(pojo.getHasExtractedLicensingInfos(), e -> org.eclipse.sw360.common.utils.converter.spdx.OtherLicensingInformationDetectedConverter.toThrift(e)));
        }
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getLicenseComments() != null) {
            thrift.setLicenseComments(pojo.getLicenseComments());
        }
        if (pojo.getLicenseConcluded() != null) {
            thrift.setLicenseConcluded(pojo.getLicenseConcluded());
        }
        if (pojo.getLicenseInfoInFiles() != null) {
            thrift.setLicenseInfoInFiles(ThriftCollectionConverter.mapSet(pojo.getLicenseInfoInFiles(), e -> e));
        }
        if (pojo.getNoticeText() != null) {
            thrift.setNoticeText(pojo.getNoticeText());
        }
        if (pojo.getRevision() != null) {
            thrift.setRevision(pojo.getRevision());
        }
        if (pojo.getSnippetInformation() != null) {
            thrift.setSnippetInformation(ThriftCollectionConverter.mapSet(pojo.getSnippetInformation(), e -> org.eclipse.sw360.common.utils.converter.spdx.SnippetInformationConverter.toThrift(e)));
        }
        if (pojo.getType() != null) {
            thrift.setType(pojo.getType());
        }
        return thrift;
    }
}

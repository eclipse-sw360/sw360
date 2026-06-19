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

import org.eclipse.sw360.datahandler.services.spdx.SnippetInformation;
import org.eclipse.sw360.common.utils.converter.common.ThriftCollectionConverter;

public final class SnippetInformationConverter {

    private SnippetInformationConverter() {}

    public static SnippetInformation fromThrift(org.eclipse.sw360.datahandler.thrift.spdx.snippetinformation.SnippetInformation thrift) {
        if (thrift == null) {
            return null;
        }
        SnippetInformation pojo = new SnippetInformation();
        if (thrift.isSetComment()) {
            pojo.setComment(thrift.getComment());
        }
        if (thrift.isSetCopyrightText()) {
            pojo.setCopyrightText(thrift.getCopyrightText());
        }
        if (thrift.isSetIndex()) {
            pojo.setIndex(thrift.getIndex());
        }
        if (thrift.isSetLicenseComments()) {
            pojo.setLicenseComments(thrift.getLicenseComments());
        }
        if (thrift.isSetLicenseConcluded()) {
            pojo.setLicenseConcluded(thrift.getLicenseConcluded());
        }
        if (thrift.isSetLicenseInfoInSnippets()) {
            pojo.setLicenseInfoInSnippets(ThriftCollectionConverter.mapSet(thrift.getLicenseInfoInSnippets(), e -> e));
        }
        if (thrift.isSetName()) {
            pojo.setName(thrift.getName());
        }
        if (thrift.isSetSnippetAttributionText()) {
            pojo.setSnippetAttributionText(thrift.getSnippetAttributionText());
        }
        if (thrift.isSetSnippetFromFile()) {
            pojo.setSnippetFromFile(thrift.getSnippetFromFile());
        }
        if (thrift.isSetSnippetRanges()) {
            pojo.setSnippetRanges(ThriftCollectionConverter.mapSet(thrift.getSnippetRanges(), e -> org.eclipse.sw360.common.utils.converter.spdx.SnippetRangeConverter.fromThrift(e)));
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.spdx.snippetinformation.SnippetInformation toThrift(SnippetInformation pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.spdx.snippetinformation.SnippetInformation thrift = new org.eclipse.sw360.datahandler.thrift.spdx.snippetinformation.SnippetInformation();
        if (pojo.getComment() != null) {
            thrift.setComment(pojo.getComment());
        }
        if (pojo.getCopyrightText() != null) {
            thrift.setCopyrightText(pojo.getCopyrightText());
        }
        if (pojo.getIndex() != null) {
            thrift.setIndex(pojo.getIndex());
        }
        if (pojo.getLicenseComments() != null) {
            thrift.setLicenseComments(pojo.getLicenseComments());
        }
        if (pojo.getLicenseConcluded() != null) {
            thrift.setLicenseConcluded(pojo.getLicenseConcluded());
        }
        if (pojo.getLicenseInfoInSnippets() != null) {
            thrift.setLicenseInfoInSnippets(ThriftCollectionConverter.mapSet(pojo.getLicenseInfoInSnippets(), e -> e));
        }
        if (pojo.getName() != null) {
            thrift.setName(pojo.getName());
        }
        if (pojo.getSnippetAttributionText() != null) {
            thrift.setSnippetAttributionText(pojo.getSnippetAttributionText());
        }
        if (pojo.getSnippetFromFile() != null) {
            thrift.setSnippetFromFile(pojo.getSnippetFromFile());
        }
        if (pojo.getSnippetRanges() != null) {
            thrift.setSnippetRanges(ThriftCollectionConverter.mapSet(pojo.getSnippetRanges(), e -> org.eclipse.sw360.common.utils.converter.spdx.SnippetRangeConverter.toThrift(e)));
        }
        return thrift;
    }
}
